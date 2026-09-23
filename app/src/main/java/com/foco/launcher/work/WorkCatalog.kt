package com.foco.launcher.work

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
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
import java.util.concurrent.atomic.AtomicInteger

data class WorkHomeState(
    val loaded: Boolean = false,
    val hasWorkProfile: Boolean = false,
    val apps: List<WorkApp> = emptyList(),
    val loadFailed: Boolean = false,
    /** True/false from isQuietModeEnabled. Null means the read was not available. */
    val quietEnabled: Boolean? = null,
    val refreshing: Boolean = false,
)

fun WorkHomeState.snapshot(): WorkSnapshot {
    return WorkSnapshot(
        profile = hasWorkProfile,
        failed = loadFailed,
        quiet = quietEnabled,
        keys = apps.map { it.key },
    )
}

/**
 * Every launchable activity on each non-personal profile from [LauncherApps.getProfiles].
 * No whitelist, no dedupe. Icons are decoded only when a cell asks for them.
 */
class WorkCatalog(context: Context) {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val gate = Mutex()
    private val density = appContext.resources.displayMetrics.densityDpi
    private val generation = AtomicInteger(0)

    private val infoByKey = HashMap<String, LauncherActivityInfo>()
    private val inFlight = HashMap<String, Long>()
    private val iconEpochByKey = HashMap<String, Long>()

    private val _state = MutableStateFlow(WorkHomeState())
    val state: StateFlow<WorkHomeState> = _state.asStateFlow()

    private val _icons = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val icons: StateFlow<Map<String, Bitmap>> = _icons.asStateFlow()

    private val _iconEpoch = MutableStateFlow(0L)
    val iconEpoch: StateFlow<Long> = _iconEpoch.asStateFlow()

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

    fun refresh(manual: Boolean = false, onDone: ((before: WorkHomeState, after: WorkHomeState) -> Unit)? = null) {
        val gen = generation.incrementAndGet()
        scope.launch {
            val before = _state.value
            if (manual) {
                gate.withLock {
                    if (gen == generation.get()) {
                        _state.value = _state.value.copy(refreshing = true)
                    }
                }
            }
            val result = withContext(Dispatchers.Default) { query() }
            var published: WorkHomeState? = null
            gate.withLock {
                if (gen != generation.get()) return@withLock
                infoByKey.clear()
                infoByKey.putAll(result.infos)
                val next = result.state.copy(loaded = true, refreshing = false)
                _state.value = next
                val keep = next.apps.map { it.key }.toSet()
                _icons.value = _icons.value.filterKeys { it in keep }
                iconEpochByKey.keys.retainAll(keep)
                _iconEpoch.value = _iconEpoch.value + 1
                published = next
            }
            val after = published
            if (after != null) onDone?.invoke(before, after)
        }
    }

    /** Decode one icon off the main thread. Safe to call for every visible cell. */
    fun ensureIcon(key: String) {
        scope.launch {
            val job = gate.withLock { claimIcon(key) } ?: return@launch
            val bitmap = withContext(Dispatchers.Default) { decodeIcon(job.info) }
            gate.withLock { publishIcon(key, job.epoch, bitmap) }
        }
    }

    private fun claimIcon(key: String): IconJob? {
        val epoch = _iconEpoch.value
        if (iconEpochByKey[key] == epoch && _icons.value.containsKey(key)) return null
        if (inFlight[key] == epoch) return null
        val info = infoByKey[key] ?: return null
        inFlight[key] = epoch
        return IconJob(info, epoch)
    }

    private fun publishIcon(key: String, epoch: Long, bitmap: Bitmap?) {
        if (inFlight[key] == epoch) inFlight.remove(key)
        if (bitmap == null || epoch != _iconEpoch.value) return
        if (!infoByKey.containsKey(key)) return
        _icons.value = _icons.value + (key to bitmap)
        iconEpochByKey[key] = epoch
    }

    private fun decodeIcon(info: LauncherActivityInfo): Bitmap? {
        return runCatching { info.getIcon(density).toBitmapCached() }.getOrNull()
    }

    /**
     * Metadata only. Does not call getIcon or decode bitmaps.
     */
    private fun query(): QueryResult {
        val apps = launcherApps
            ?: return QueryResult(
                WorkHomeState(loaded = true, hasWorkProfile = false, loadFailed = true),
                emptyMap(),
            )
        val profilesResult = runCatching {
            apps.profiles.filter { it != Process.myUserHandle() }
        }
        if (profilesResult.isFailure) {
            Log.w(TAG, "getProfiles failed", profilesResult.exceptionOrNull())
            return QueryResult(
                WorkHomeState(loaded = true, hasWorkProfile = false, loadFailed = true),
                emptyMap(),
            )
        }
        val workUsers = profilesResult.getOrThrow()
        if (workUsers.isEmpty()) {
            return QueryResult(
                WorkHomeState(loaded = true, hasWorkProfile = false, loadFailed = false),
                emptyMap(),
            )
        }
        if (workUsers.size > 1) {
            Log.i(TAG, "non-personal profiles=${workUsers.size}; listing every launchable")
        }
        val quietReadings = ArrayList<Boolean?>(workUsers.size)
        val built = ArrayList<WorkApp>()
        val infos = HashMap<String, LauncherActivityInfo>()
        var anyFailure = false
        var anySuccess = false
        for (user in workUsers) {
            quietReadings += QuietMode.read(userManager, user)
            val listed = runCatching { apps.getActivityList(null, user) }
            if (listed.isFailure) {
                anyFailure = true
                Log.w(TAG, "getActivityList failed", listed.exceptionOrNull())
                continue
            }
            anySuccess = true
            for (info in listed.getOrThrow()) {
                val app = toWorkApp(info, user)
                built += app
                infos[app.key] = info
            }
        }
        val failed = anyFailure && !anySuccess
        val shown = if (failed) {
            emptyList()
        } else {
            WorkCatalogRules.presentAll(
                activities = built,
                label = { it.label },
                packageName = { it.packageName },
                className = { it.className },
            )
        }
        val keptInfos = if (failed) emptyMap() else infos
        return QueryResult(
            WorkHomeState(
                loaded = true,
                hasWorkProfile = true,
                apps = shown,
                loadFailed = failed,
                quietEnabled = WorkCatalogRules.combineQuiet(quietReadings),
            ),
            keptInfos,
        )
    }

    private fun toWorkApp(info: LauncherActivityInfo, user: UserHandle): WorkApp {
        val component = info.componentName
        val label = info.label?.toString().orEmpty().ifBlank { component.packageName }
        val serial = runCatching { userManager?.getSerialNumberForUser(user) }.getOrNull() ?: 0L
        return WorkApp(
            key = "$serial:${component.flattenToShortString()}",
            packageName = component.packageName,
            className = component.className,
            label = label,
            user = user,
            component = component,
        )
    }

    private data class QueryResult(
        val state: WorkHomeState,
        val infos: Map<String, LauncherActivityInfo>,
    )

    private data class IconJob(
        val info: LauncherActivityInfo,
        val epoch: Long,
    )

    private companion object {
        const val TAG = "FocoWork"
    }
}
