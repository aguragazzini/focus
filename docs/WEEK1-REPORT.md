# Semana 1 — qué se despachó vs checklist Nico §1 (items 1–10)

Techo: `docs/foco-launcher-arquitectura-v1.md`. Checklist: `docs/foco-launcher-android-notes-v1.md` §1. GO: `docs/foco-launcher-arch-ok-semana1.md`. Copy: `docs/foco-launcher-ux-v1.md`.

Alcance de este APK: **semana 1 solamente**. Sin BiometricPrompt UI. Sin Accessibility. Sin NotificationListener. Repo de cero (no Capacitor / foco-moto-v3).

| # | Item (notes §1) | Estado | Dónde |
|---|-----------------|--------|--------|
| 1 | Proyecto Compose, `applicationId` / namespace `com.foco.launcher`, un solo `:app` | Hecho | `settings.gradle.kts` (`include(":app")` only), `app/build.gradle.kts` |
| 2 | Packages `core`, `registry`, `settings` + `security` stub | Hecho | `app/src/main/java/com/foco/launcher/{core,registry,settings,security}` |
| 3 | `LauncherActivity` MAIN+HOME+DEFAULT+LAUNCHER; `singleTask`, `stateNotNeeded`, `excludeFromRecents`, `clearTaskOnLaunch`, theme launcher, portrait | Hecho | `AndroidManifest.xml` → `.core.LauncherActivity` |
| 4 | `<queries>` MAIN/LAUNCHER; **no** `QUERY_ALL_PACKAGES` | Hecho | Manifest `<queries>` (más DIAL / SMS / IMAGE_CAPTURE / SETTINGS para sugeridas OEM) |
| 5 | Home Compose: grid solo whitelist; empty state; engranaje fijo → `SettingsActivity` | Hecho | `core/HomeScreen.kt` — ⋮ Editar / Ajustes / Ajustes del teléfono |
| 6 | Registry: launchables via PM; cache label/icon/component; `PackageChangeReceiver` ADDED/REMOVED/REPLACED invalida y limpia huérfanos; **nunca** auto-agrega | Hecho | `registry/PackageRegistry.kt`, `PackageChangeReceiver.kt` |
| 7 | DataStore `LauncherPrefs` + `WhitelistEntry` — CRUD + reorder | Hecho | `registry/Models.kt`, `PrefsStore.kt`, `WhitelistMutations.kt`; editor en Settings |
| 8 | `SetupActivity` (exported false): Teléfono, Mensajes, Cámara, Ajustes sugeridos; ≥1 para Continuar; CTA default **bloqueado** hasta Ajustes en la lista | Hecho | `settings/SetupActivity.kt` + `SetupScreen.kt`. Bio global UI = semana 2 |
| 9 | CTA default: `Settings.ACTION_HOME_SETTINGS` (fallback `ACTION_MANAGE_DEFAULT_APPS_SETTINGS`); no RoleManager-only | Hecho | `core/LaunchController.openHomePicker` |
| 10 | Tap → `resolveLaunchIntent` → `startActivity` **sin** bio | Hecho | `core/LaunchController.openApp`; `security/BiometricGate.ENABLED_IN_LAUNCH_PATH = false` |

## Extra anti-brick (notes §3 + Andrés OK)

- Engranaje fijo → Ajustes Foco, sin bio.
- Long-press vacío → `Settings.ACTION_SETTINGS` (sistema), sin bio.
- Deep link `foco://settings` (`SettingsActivity` exported + VIEW).
- Botón **Volver al launcher anterior** → HOME_SETTINGS.
- Setup antes de pedir ser default.
- `allowBackup=false`. Cero servicios foreground.

## Modelo persistido

```kotlin
WhitelistEntry(packageName, order, bioEnabled = true) // Teléfono sugerido: false
LauncherPrefs(setupDone, biometricGlobalEnabled = true, entries)
```

`bioEnabled` se guarda (default true al agregar) pero **se ignora en el launch path de semana 1**.

## minSdk / target

minSdk **26**, targetSdk **34**, compileSdk **34**, portrait lock. Subir a 29 queda para después del primer sideload (`getprop ro.product.model` + `ro.build.version.sdk`).

## No incluido (a propósito)

- BiometricPrompt / androidx.biometric
- Banner RoleManager avanzado (hay banner seco si no es default)
- Toggle bio per-app en UI
- NotificationListener, Accessibility
- Multi-module Gradle
- `QUERY_ALL_PACKAGES`
- App drawer completo
- Auto-whitelist de apps nuevas
- Play Store / backend

## Cómo obtener el APK

```bash
./gradlew :app:assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

Entrega (debug, firmado con keystore debug de Gradle):

- `/opt/cursor/artifacts/foco-launcher-v0.1-w1.apk`
- `/opt/cursor/artifacts/foco-launcher-v0.1-w1.apk.b64` (base64 de una sola línea)
- `/opt/cursor/artifacts/foco-launcher-v0.1-w1.apk.tar.gz`

SHA256 (`foco-launcher-v0.1-w1.apk`):

```
27aa77968b7ecc46380fd94f1d5d89db13bbcf70eb482dc88c05654c4b4343e7
```

Rebuild local: `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.

## Sideload Motorola

Ver README: Default apps → Home, setup primero, deep link `adb shell am start -a android.intent.action.VIEW -d foco://settings`.

Item notes §1 “Sideload en Edge 30; ≥4–6 h como HOME” **no se puede cerrar desde este entorno** (no hay el teléfono). El APK es instalable; QA en device de Agus.
