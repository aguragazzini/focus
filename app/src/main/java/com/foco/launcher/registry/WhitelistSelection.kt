package com.foco.launcher.registry

object WhitelistSelection {
    /**
     * Setup continue always keeps system Settings when that package resolved,
     * so a finished setup cannot persist an empty whitelist.
     */
    fun withSettings(selected: Set<String>, settingsPackage: String?): Set<String> {
        if (settingsPackage.isNullOrBlank()) return selected
        return selected + settingsPackage
    }
}
