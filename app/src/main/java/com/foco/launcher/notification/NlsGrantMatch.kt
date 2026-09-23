package com.foco.launcher.notification

/**
 * Parses `Settings.Secure.ENABLED_NOTIFICATION_LISTENERS` (colon-separated).
 * The platform stores either the flattened or the short component form.
 */
object NlsGrantMatch {
    fun listed(raw: String?, packageName: String, className: String): Boolean {
        if (raw.isNullOrBlank() || packageName.isBlank() || className.isBlank()) return false
        val full = "$packageName/$className"
        val shortClass = if (className.startsWith(packageName) &&
            className.length > packageName.length &&
            className[packageName.length] == '.'
        ) {
            className.substring(packageName.length)
        } else {
            className
        }
        val short = "$packageName/$shortClass"
        return raw.split(':').any { entry ->
            val token = entry.trim()
            token.equals(full, ignoreCase = true) || token.equals(short, ignoreCase = true)
        }
    }
}

/** Avoids a tight rebind loop if the platform disconnects the listener immediately. */
object NlsRebindGate {
    const val GAP_MS = 1_500L

    fun allow(nowElapsedMs: Long, lastElapsedMs: Long): Boolean {
        return nowElapsedMs - lastElapsedMs >= GAP_MS
    }
}
