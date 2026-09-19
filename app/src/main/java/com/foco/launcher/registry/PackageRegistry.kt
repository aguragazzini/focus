package com.foco.launcher.registry

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

    private val _launchables = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val launchables: StateFlow<List<LaunchableApp>> = _launchables.asStateFlow()

    init {
        scope.launch { reload() }
    }

    fun invalidate() {
        scope.launch { reload() }
    }

    /**
     * Lightweight resume check: reload only if the installed launcher-package set changed.
     * Does not poll; called from Activity.onResume as OEM PACKAGE_* broadcasts can be flaky.
     */
    fun refreshIfPackagesChanged() {
        scope.launch {
            val current = peekLauncherPackages()
            val cachedPkgs = cache.get().map { it.packageName }.toSet()
            if (current != cachedPkgs) {
                reload()
            }
        }
    }

    suspend fun allLaunchables(): List<LaunchableApp> {
        val hit = cache.get()
        if (hit.isNotEmpty()) return hit
        return reload()
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
        val apps = allLaunchables().associateBy { it.packageName }
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
                            prefs.copy(entries = WhitelistMutations.remove(prefs.entries, packageName))
                        }
                    }
                }
            }
            Intent.ACTION_PACKAGE_REPLACED -> invalidate()
            else -> invalidate()
        }
    }

    private suspend fun reload(): List<LaunchableApp> = withContext(Dispatchers.Default) {
        val loaded = queryLaunchables()
        cache.set(loaded)
        _launchables.value = loaded
        loaded
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
        return resolved.mapNotNull { ri ->
            val info = ri.activityInfo ?: return@mapNotNull null
            val packageName = info.packageName ?: return@mapNotNull null
            if (packageName == appContext.packageName) return@mapNotNull null
            val label = ri.loadLabel(pm)?.toString().orEmpty().ifBlank { packageName }
            val icon = runCatching {
                ri.loadIcon(pm)?.toBitmapCached() ?: pm.getApplicationIcon(packageName).toBitmapCached()
            }.getOrNull() ?: return@mapNotNull null
            val component = ComponentName(packageName, info.name)
            LaunchableApp(packageName, label, icon, component)
        }.sortedBy { it.label.lowercase() }
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
