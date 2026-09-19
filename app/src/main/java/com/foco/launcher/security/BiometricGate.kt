package com.foco.launcher.security

/**
 * BiometricPrompt gate — semana 2.
 *
 * Semana 1: el launch path en [com.foco.launcher.core] ignora bio.
 * No hay UI, no hay androidx.biometric, no se almacena biometría.
 *
 * `WhitelistEntry.bioEnabled` se persiste (default true al agregar;
 * Teléfono sugerido false) para no migrar datos en s2.
 */
object BiometricGate {
    const val ENABLED_IN_LAUNCH_PATH: Boolean = false
}
