package com.foco.launcher.work

import android.os.UserHandle
import android.os.UserManager

/**
 * Reads quiet mode only through [UserManager.isQuietModeEnabled].
 * A security failure or any other throw becomes unknown (null). Callers must not
 * draw S3 / "Pausa" from null, and must not ask the system to pause or resume a profile.
 */
internal object QuietMode {
    fun read(userManager: UserManager?, user: UserHandle): Boolean? {
        if (userManager == null) return null
        val result = try {
            Result.success(userManager.isQuietModeEnabled(user))
        } catch (thrown: Throwable) {
            Result.failure(thrown)
        }
        return interpret(result)
    }

    fun interpret(result: Result<Boolean>): Boolean? = result.getOrNull()
}
