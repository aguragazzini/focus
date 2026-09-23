package com.foco.launcher.work

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.foco.launcher.registry.toBitmapCached
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class WorkHomeState(
    val loaded: Boolean = false,
    val hasWorkProfile: Boolean = false,
    val apps: List<WorkApp> = emptyList(),
)

/**
 * Every launchable activity on each non-personal profile returned by [LauncherApps.getProfiles].
 * Those profiles are the work profiles the personal user can launch. No whitelist.
 */
class WorkCatalog(context: Context) {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val gate = Mutex()
    private val density = appContext.resources.displayMetrics.densityDpi

    private val _state = MutableStateFlow(WorkHomeState())
    val state: StateFlow<WorkHomeState> = _state.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackagesAvailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean,
        ) = refresh()

        override fun onPackagesUnavailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean,
        ) = refresh()

        override fun onPackagesSuspended(packageNames: Array<out String>?, user: UserHandle?) = refresh()

        override fun onPackagesUnsuspended(packageNames: Array<out String>?, user: UserHandle?) = refresh()
    }

    init {
        if (launcherApps != null) {
            runCatching {
                launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
            }
        }
        refresh()
    }

    fun refresh() {
        scope.launch { reload() }
    }

    private suspend fun reload() {
        val next = withContext(Dispatchers.Default) { query() }
        gate.withLock { _state.value = next }
    }

    private fun query(): WorkHomeState {
        val apps = launcherApps ?: return WorkHomeState(loaded = true, hasWorkProfile = false)
        val workUsers = runCatching {
            apps.profiles.filter { it != Process.myUserHandle() }
        }.getOrDefault(emptyList())
        if (workUsers.isEmpty()) {
            return WorkHomeState(loaded = true, hasWorkProfile = false)
        }
        val raw = workUsers.flatMap { user ->
            val listed = runCatching { apps.getActivityList(null, user) }.getOrNull().orEmpty()
            listed.map { info -> toWorkApp(info, user) }
        }
        val shown = WorkCatalogRules.presentAll(
            activities = raw,
            label = { it.label },
            packageName = { it.packageName },
            className = { it.className },
        )
        return WorkHomeState(loaded = true, hasWorkProfile = true, apps = shown)
    }

    private fun toWorkApp(info: android.content.pm.LauncherActivityInfo, user: UserHandle): WorkApp {
        val component = info.componentName
        val label = info.label?.toString().orEmpty().ifBlank { component.packageName }
        val icon = runCatching { info.getIcon(density).toBitmapCached() }.getOrNull() ?: placeholderIcon()
        val serial = userManager?.getSerialNumberForUser(user) ?: 0L
        return WorkApp(
            key = "$serial:${component.flattenToShortString()}",
            packageName = component.packageName,
            className = component.className,
            label = label,
            icon = icon,
            user = user,
            component = component,
        )
    }

    private fun placeholderIcon(): Bitmap {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
}
