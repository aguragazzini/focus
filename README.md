# Foco Launcher

HOME launcher de lista blanca para Android. Kotlin + Jetpack Compose. Sin backend, sin Accessibility, sin NotificationListener.

Package: `com.foco.launcher`  
Versión: `0.1.0-w1` (semana 1: se puede vivir, **sin** BiometricPrompt)

Solo ves las apps que elegís. El resto no aparece.

## Requisitos para buildear

- JDK 17+ (probado con 21)
- Android SDK **compileSdk / targetSdk 34**, minSdk **26**
- Variable `ANDROID_HOME` o un `local.properties` local (ese archivo no se commitea)

```bash
export ANDROID_HOME="$HOME/android-sdk"
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:assembleDebug
```

El APK de debug lo firma Gradle con el keystore de debug **local** (`~/.android/debug.keystore`). No hay keystores ni secretos en este repo.

```
app/build/outputs/apk/debug/app-debug.apk
```

Copia de entrega (mismo binario): `foco-launcher-v0.1-w1.apk`.

SHA256 de la entrega week-1:

```
27aa77968b7ecc46380fd94f1d5d89db13bbcf70eb482dc88c05654c4b4343e7
```

## Sideload en Motorola (Edge 30 y modelos cercanos)

1. En el teléfono: **Ajustes → Seguridad** (o **Privacidad**) → permitir **orígenes desconocidos** / instalar apps desconocidas para el navegador o Archivos, según el OEM.
2. Copiá el APK (`adb install -r app-debug.apk` o abrirlo desde Archivos).
3. Abrí **Foco**. Completá el setup: elegí al menos una app. **Ajustes** viene sugerido; el CTA para ser inicio queda bloqueado hasta que Ajustes esté en la lista (escape hatch).
4. En **Usar Foco como inicio**, tocá **Elegir como inicio**. Eso abre el picker del sistema (`Settings.ACTION_HOME_SETTINGS`, con fallback a apps predeterminadas).
5. En Motorola: **Ajustes → Apps → Apps predeterminadas → App de inicio** (o **Default apps → Home**) y elegí **Foco**.
6. Al tocar Inicio / gesto de home, deberías ver solo la lista blanca.

Si Foco todavía no es el inicio, el home muestra: `Todavía no es el inicio del teléfono.`

### Escape si algo sale mal

- Engranaje fijo en el home → Ajustes de Foco (sin huella).
- Menú ⋮ → Editar apps / Ajustes / Ajustes del teléfono.
- Long-press en el área vacía del home → **Ajustes del sistema** (`Settings.ACTION_SETTINGS`), sin huella.
- Deep link:

```bash
adb shell am start -a android.intent.action.VIEW -d foco://settings
```

Si el OEM no resuelve el scheme, forzalo:

```bash
adb shell am start -a android.intent.action.VIEW -d foco://settings -n com.foco.launcher/.settings.SettingsActivity
```

- En Ajustes de Foco: **Volver al launcher anterior** abre otra vez el picker de inicio.
- Último recurso: modo seguro, desinstalar `com.foco.launcher`, o **Apps predeterminadas → Inicio**.

## Qué hay en semana 1

- Un solo módulo Gradle `:app` (`core/`, `registry/`, `security/` stub, `settings/`).
- Home Compose: solo whitelist (vacía al instalar).
- Tap → `getLaunchIntentForPackage` / component explícito. **Sin bio.**
- DataStore: `WhitelistEntry(packageName, order, bioEnabled=true)` y `LauncherPrefs(setupDone, biometricGlobalEnabled, entries)`.
- `PackageChangeReceiver`: invalida cache; **nunca** auto-agrega; desinstalación limpia huérfanos.
- `<queries>` MAIN/LAUNCHER. No `QUERY_ALL_PACKAGES`.

## Fuera de semana 1

BiometricPrompt, NotificationListener, Accessibility, multi-module Gradle, Play Store.

Informe semana 1 vs checklist: [`docs/WEEK1-REPORT.md`](docs/WEEK1-REPORT.md).  
Publicar en GitHub: [`PUSH.md`](PUSH.md).

## Documentos canónicos

En `docs/`: arquitectura v1 (techo), notes Android (checklist), OK Andrés semana 1, UX v1 (copy ES-AR), decisiones de producto, spec funcional.

## Licencia

Sideload. Sin backend. Sin cuenta.
