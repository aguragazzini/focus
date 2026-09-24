package com.foco.launcher.registry

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log
import com.foco.launcher.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

class PackageRegistry(
    context: Context,
    private val prefsStore: PrefsStore,
) {
    private val appContext = context.applicationContext
    private val pm: PackageManager = appContext.packageManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cache = AtomicReference<List<LaunchableApp>>(emptyList())
    private val homeMutex = Mutex()
    private val publishMutex = Mutex()

    private val _launchables = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val launchables: StateFlow<List<LaunchableApp>> = _launchables.asStateFlow()

    /** True once whitelist icons are published. Does not wait for the full catalog. */
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _catalogLoaded = MutableStateFlow(false)

    init {
        scope.launch {
            warmHomeIcons()
            warmCatalog(force = false)
        }
    }

    /**
     * Loads icons for the current whitelist only, off the caller thread.
     * Safe to call from boot; a full-catalog scan is a separate step.
     */
    suspend fun warmHomeIcons() {
        if (_loaded.value) return
        homeMutex.withLock {
            if (_loaded.value) return@withLock
            val prefs = prefsStore.prefs.first()
            val listed = withContext(Dispatchers.Default) {
                loadListed(prefs.entries.map { it.packageName })
            }
            publishHome(listed)
        }
    }

    fun invalidate() {
        scope.launch { warmCatalog(force = true) }
    }

    /**
     * Lightweight resume check: reload only if the installed launcher-package set changed.
     * Does not poll; called from Activity.onResume as OEM PACKAGE_* broadcasts can be flaky.
     */
    fun refreshIfPackagesChanged() {
        scope.launch {
            if (!_catalogLoaded.value) return@launch
            val current = peekLauncherPackages()
            val cachedPkgs = cache.get().map { it.packageName }.toSet()
            if (current != cachedPkgs) {
                warmCatalog(force = true)
            }
        }
    }

    suspend fun allLaunchables(): List<LaunchableApp> {
        if (!_catalogLoaded.value) warmCatalog(force = false)
        return cache.get()
    }

    fun resolveLaunchIntent(packageName: String): Intent? {
        val launch = pm.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            launch.addCategory(Intent.CATEGORY_LAUNCHER)
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return launch
        }
        val component = cache.get().find { it.packageName == packageName }?.component
            ?: return null
        return Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    suspend fun visibleApps(prefs: LauncherPrefs): List<LaunchableApp> {
        if (!_loaded.value) warmHomeIcons()
        val apps = cache.get().associateBy { it.packageName }
        return prefs.entries
            .sortedBy { it.order }
            .mapNotNull { apps[it.packageName] }
    }

    suspend fun pruneUninstalled() {
        val installed = allLaunchables().map { it.packageName }.toSet()
        val prefs = prefsStore.prefs.first()
        val pruned = WhitelistMutations.pruneOrphans(prefs.entries, installed)
        if (pruned != prefs.entries) {
            prefsStore.setEntries(pruned)
        }
    }

    /**
     * New installs never enter the whitelist. Uninstalls drop the orphan entry.
     * Replaces only invalidate the icon/label cache.
     */
    fun onPackageEvent(action: String?, packageName: String?, replacing: Boolean) {
        if (packageName.isNullOrBlank()) {
            invalidate()
            return
        }
        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                if (!replacing) {
                    log("package added; not auto-whitelisted: $packageName")
                }
                invalidate()
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                invalidate()
                if (!replacing) {
                    scope.launch {
                        prefsStore.update { prefs ->
                            prefs.withEntries(WhitelistMutations.remove(prefs.entries, packageName))
                        }
                    }
                }
            }
            Intent.ACTION_PACKAGE_REPLACED -> invalidate()
            else -> invalidate()
        }
    }

    private suspend fun warmCatalog(force: Boolean) {
        if (!force && _catalogLoaded.value) return
        val loaded = withContext(Dispatchers.Default) { queryLaunchables() }
        publishMutex.withLock {
            if (!force && _catalogLoaded.value) return@withLock
            cache.set(loaded)
            _launchables.value = loaded
            _catalogLoaded.value = true
            _loaded.value = true
        }
    }

    private suspend fun publishHome(apps: List<LaunchableApp>) {
        publishMutex.withLock {
            if (_catalogLoaded.value) {
                _loaded.value = true
                return@withLock
            }
            cache.set(apps)
            _launchables.value = apps
            _loaded.value = true
        }
    }

    private fun loadListed(packageNames: List<String>): List<LaunchableApp> {
        val seen = LinkedHashSet<String>()
        val out = ArrayList<LaunchableApp>(packageNames.size)
        for (packageName in packageNames) {
            if (!seen.add(packageName)) continue
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(packageName)
            val match = pm.queryIntentActivities(intent, 0).firstOrNull() ?: continue
            toLaunchable(match)?.let { out.add(it) }
        }
        return out
    }

    private fun peekLauncherPackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
    }

    private fun queryLaunchables(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, 0)
        return resolved.mapNotNull { toLaunchable(it) }.sortedBy { it.label.lowercase() }
    }

    private fun toLaunchable(ri: ResolveInfo): LaunchableApp? {
        val info = ri.activityInfo ?: return null
        val packageName = info.packageName ?: return null
        if (packageName == appContext.packageName) return null
        val label = ri.loadLabel(pm)?.toString().orEmpty().ifBlank { packageName }
        val icon = runCatching {
            ri.loadIcon(pm)?.toBitmapCached() ?: pm.getApplicationIcon(packageName).toBitmapCached()
        }.getOrNull() ?: return null
        val component = ComponentName(packageName, info.name)
        return LaunchableApp(packageName, label, icon, component)
    }

    private fun log(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    private companion object {
        const val TAG = "FocoRegistry"
    }
}
