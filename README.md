# Foco Launcher

HOME launcher de lista blanca para Android. Kotlin + Jetpack Compose. Sin backend, sin Accessibility.

Package: `com.foco.launcher`  
Versión: `0.8.0` (versionCode 14)

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

El APK de debug lo firma `signing/foco-debug.keystore` (alias `foco-debug`, passwords `android`). Esa clave se generó para el repo en 0.7.0. Si `adb install -r` responde `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, desinstalá la build anterior y volvé a instalar. A partir de esta clave, las builds de debug del repo se pisan entre sí.

El reloj muestra el misal de 1962 y el Novus Ordo, en `santoral_1962.json` y `santoral_novus.json`. Las etiquetas son «1962» y «Novus». El inicio son cuatro páginas: Reloj, Personal, Comida, Trabajo. Personal es la página de llegada.

```
app/build/outputs/apk/debug/app-debug.apk
```

Copia de entrega week-1 (histórico): `foco-launcher-v0.1-w1.apk`.  
Entrega actual (NLS): `foco-launcher-v0.2-nls.apk` — ver Artifacts / `dist/TRANSFER.md`.

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

### Filtro de avisos (semana 2a)

No hace falta para ser launcher. En **Ajustes → Notificaciones** prendé **Silenciar otras notificaciones**. Android pide acceso a notificaciones; en la lista, activá **Foco**.

En Motorola: **Ajustes → Notificaciones → acceso / apps con acceso**.

El filtro es del **perfil personal**. Las notificaciones del **perfil de trabajo no se tocan** (`UserHandle`). Llamadas, alarmas, navegación y media en curso no se silencian. Sin el permiso del sistema, el filtro no se presenta como activo.

Apps en el home avisan por defecto (`allowNotif=true`). Podés apagar el switch por app: el ícono sigue; el aviso no. Apps que no están en Foco no avisan.

## Qué hay en 0.2.0 (semana 2a)

- Todo lo de semana 1, más `FocoNotificationListener` + `NotificationPolicy.shouldSuppress`.
- `allowNotif` en cada entry del home. `notificationAllowlist` **derivado** (`entries.filter { allowNotif }`).
- Flag `nlsFilterEnabled`. **No** gatea ROLE_HOME ni el picker de inicio.
- Settings **Notificaciones** + onboarding NLS (copy ES-AR Camila). Lista = solo apps del home personal.
- Work profile: PASS always (`sbn.user` / UserHandle). Sin Device Admin / DPM / Accessibility. Sin inbox. Sin bio.

## Fuera de este corte

BiometricPrompt (s2b HOLD), Accessibility, Device Admin, inbox de notifs, multi-module Gradle, Play Store.

Informe NLS vs arch §0: [`docs/WEEK2A-NLS-REPORT.md`](docs/WEEK2A-NLS-REPORT.md).  
Informe semana 1: [`docs/WEEK1-REPORT.md`](docs/WEEK1-REPORT.md).

Espejo público previsto: [github.com/aguragazzini/focus](https://github.com/aguragazzini/focus). Origin (Cursor) sigue siendo la fuente de verdad; este VM no pudo pushear a GitHub (sin auth). Ver [`PUSH.md`](PUSH.md).

## Documentos canónicos

En `docs/`: arquitectura v1, delta NLS v0, notes NLS, UX onboarding NLS, UX v1, decisiones, spec funcional.

## Licencia

Sideload. Sin backend. Sin cuenta.
