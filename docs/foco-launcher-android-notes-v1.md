# Foco Launcher — notas Android nativas v1 (cerrado)

**Autor:** Nico · Android Launcher  
**Fecha:** 2026-09-19  
**Estado:** alineado a arquitectura v1 + **OK semana 1** (`foco-launcher-arch-ok-semana1.md`). Checklist de implementación; no rival de arquitectura.  
**Package:** `com.foco.launcher`  
**Fuentes:** `launcher-android-spec-v0.md` · `foco-launcher-decisions-2026-09-19.md` · `foco-launcher-arquitectura-v1.md`  
**Dispositivo de prueba:** Motorola Edge 30 (OEM notes §6 arquitectura)

**Techo:** un solo módulo Gradle `:app`. Packages internos `core` / `registry` / `security` / `settings`. Sin multi-module Gradle en semana 1. Sin Accessibility, sin NotificationListener, sin Capacitor/`foco-moto-v3`.

---

## 1) Checklist implementación HOME launcher

Orden = plan Andrés §7. Semana 1 = sin bio; semana 2 = bio.

### Semana 1 — “Se puede vivir”

- [ ] Proyecto Compose vacío, `applicationId` / namespace `com.foco.launcher`, un solo `:app`.
- [ ] Packages: `core`, `registry`, `settings` (security stub vacío o ausente hasta s2).
- [ ] `LauncherActivity` (`MAIN` + `HOME` + `DEFAULT` + `LAUNCHER`): `singleTask`, `stateNotNeeded`, `excludeFromRecents`, theme launcher.
- [ ] Manifest `<queries>` con `MAIN`/`LAUNCHER` — **no** `QUERY_ALL_PACKAGES` hasta medir en Edge 30.
- [ ] Home Compose: grid solo whitelist; empty state claro; **engranaje fijo** → `SettingsActivity` (no es ítem de whitelist).
- [ ] `registry`: listar launchables vía PackageManager; cache label/icon/component; `PackageChangeReceiver` (`ADDED`/`REMOVED`/`REPLACED`) invalida cache y limpia huérfanos — **nunca** auto-agrega.
- [ ] DataStore `LauncherPrefs` + `WhitelistEntry` (ver §2) — CRUD + reorder.
- [ ] `SetupActivity` (exported false): mínimas sugeridas (Teléfono, Mensajes, Cámara, Ajustes sistema) con confirmación; bio global UI puede quedar para s2; CTA default solo después (Andrés §5: ≥1 app de comunicación **o** confirmación explícita “sé que el home puede quedar casi vacío”).
- [ ] CTA default: `Settings.ACTION_HOME_SETTINGS` (o `ACTION_MANAGE_DEFAULT_APPS`) — picker OEM; no inventar RoleManager-only.
- [ ] Tap → `registry.resolveLaunchIntent(pkg)` → `startActivity` **sin** bio.
- [ ] `allowBackup=false`. Cero servicios foreground.
- [ ] Sideload en Edge 30; ≥4–6 h como HOME.

**Exit s1:** default launcher, solo whitelist, abre apps, settings alcanzable, no brick.

### Semana 2 — Bio + uso diario

- [ ] Package `security`: BiometricPrompt en launch path.
- [ ] Toggle bio global + per-app; Teléfono excepción UI sugerida off.
- [ ] Banner si Foco no es default (`RoleManager` / default activity al resume).
- [ ] Cache icons off-main; empty states; crash-free 2–3 días.
- [ ] Copy en settings: bio solo cubre taps desde Foco (Andrés §8).

### Prohibido (Andrés §9)

App drawer completo · auto-whitelist · Accessibility · NotificationListener s1 · `QUERY_ALL_PACKAGES` de entrada · multi-module Gradle ceremonial · prometer bloqueo de intents externos.

---

## 2) BiometricPrompt por app

Alineado a arquitectura §2 / §4. Solo semana 2.

### Modelo (nombres de Andrés)

```kotlin
data class WhitelistEntry(
  val packageName: String,
  val order: Int,
  val bioEnabled: Boolean = true, // default ON al agregar
)

data class LauncherPrefs(
  val setupComplete: Boolean = false,
  val biometricGlobalEnabled: Boolean = true,
  val entries: List<WhitelistEntry> = emptyList(),
)
```

### Launch path (único protegido en v0.1)

```
Tap (core)
  → si biometricGlobalEnabled && entry.bioEnabled → BiometricPrompt (security)
  → success → resolveLaunchIntent → startActivity
  → fail/cancel → no-op
```

### Reglas

- Al agregar app: `bioEnabled = true`; Teléfono sugerido `false`.
- AndroidX Biometric; no almacenar biometría.
- Fallback: device credential (PIN) vía `setAllowedAuthenticators` (Andrés §6). Si no hay authenticator: no abrir + camino a desactivar bio en settings.
- **No** protege notificación / deep link / share / otra app (tabla Andrés §8). Decirlo en UI.

---

## 3) Cómo evitar “quedar sin Settings”

Implementación = anti-brick Andrés §5 (reemplaza propuesta previa de “Settings del sistema forzado en whitelist”).

### Obligatorio v0.1

1. **Engranaje fijo en home** → `SettingsActivity` de Foco — no cuenta como app whitelist; no se puede “sacar del grid” accidentalmente.
2. **Long-press en área vacía** → mismo Settings.
3. **Deep link** `foco://settings` (`SettingsActivity` exported + intent-filter VIEW) — escape vía adb / browser / nota de sideload.
4. Botón **“Volver al launcher anterior”** → otra vez `ACTION_HOME_SETTINGS`.
5. Setup **antes** de pedir ser default; no CTA HOME hasta condición Andrés §5.
6. Ajustes del **sistema**: sugeridos en setup; apertura preferida con `Settings.ACTION_SETTINGS` (no hardcodear component OEM). Bio sobre Ajustes sistema: si está en whitelist, preferir `bioEnabled=false` o no exigir bio fail-closed en v0.1.

### Recuperación documentada (README sideload)

Safe Mode / desinstalar `com.foco.launcher` / Default apps → Home. Último recurso si el usuario se encierra.

---

## 4) minSdk / targetSdk (Motorola Edge 30)

Edge 30 salió con **Android 12 (API 31)**; muchos siguen en 12–14 vía updates.

| Campo | Valor cerrado | Nota |
|--------|----------------|------|
| **minSdk** | **26** al scaffold → **29** si el Moto ≥ Android 10 | Andrés OK 2026-09-19: no cerrar 29 hasta pegar model/API del sideload. Edge 30 debería quedar en 29. |
| **targetSdk** | **34** | Sideload 2026; subir a 35 solo tras smoke en el device de Agus. |
| **compileSdk** | **34** (o 35 = target) | — |

Tras primer sideload: `getprop ro.product.model` + `ro.build.version.sdk` → subir minSdk a 29 si aplica.

**Portrait:** default `android:screenOrientation="portrait"` en Home hasta que Camila diga libre (Andrés OK §3).

`QUERY_ALL_PACKAGES`: solo si `<queries>` falla midiendo apps reales en ese Edge 30.

---

## 5) Estructura de módulos Gradle

**Cerrado por Andrés §1:** un solo `:app`. Subpackages, no multi-module.

```
foco-launcher/
  settings.gradle.kts          # include(":app") only
  gradle/libs.versions.toml
  app/
    src/main/java/com/foco/launcher/
      core/       # LauncherActivity, grid, launch orchestration
      registry/   # PM, DataStore whitelist, PackageChangeReceiver, cache
      security/   # BiometricPrompt gate (semana 2)
      settings/   # SetupActivity, SettingsActivity, toggles, CTA default
    AndroidManifest.xml         # techo = arquitectura §3
```

| Package | Rol |
|---------|-----|
| core | Home; tap → security → intent |
| registry | Launchables + whitelist + package changes |
| security | Bio gate; cancel = no open |
| settings | Editor whitelist, bio, set default, escape |

**No crear** Gradle modules `notification` / `recommendation` / `intentguard`.  
**No** Hilt multi-módulo. DI manual / simples factories OK.

Stack: Kotlin, Compose, DataStore, androidx.biometric (s2), coroutines. Sin red, sin Firebase, sin WebView.

---

## Diff vs notes previas (para Mateo)

| Antes (Nico draft) | Ahora (techo Andrés) |
|--------------------|----------------------|
| Multi-module `core/model\|data\|security` | Un solo `:app` + packages internos |
| `biometricRequired` | `bioEnabled` + `biometricGlobalEnabled` |
| Gate duro “Settings sistema en whitelist” | Engranaje fijo + `foco://settings` + long-press |
| RoleManager enfatizado | `ACTION_HOME_SETTINGS` / default apps OEM |
| minSdk 26 genérico | **29** / target **34**, Edge 30 |
| clearTaskOnLaunch sugerido | Seguir manifest Andrés (sin ese attr salvo que QA lo pida) |

---

## Limitaciones honestas (repetir en settings)

1. Bio = solo taps desde Foco.  
2. Notificaciones, links, share, otras apps: el sistema abre sin pasar por nosotros.  
3. Bloqueo universal = otra superficie (Accessibility/MDM) — fuera v0.1.  
4. Kill personal 30 días manda (decisiones producto).

---

**Cierre:** notes v1 cerradas contra arquitectura v1. Mateo implementa contra `foco-launcher-arquitectura-v1.md`; este archivo = checklist operativo. Próxima edición solo si Agus/Mateo reportan OEM break en Edge 30.
