# Auditoría performance y seguridad — Foco Launcher 0.7.1

Branch: `cursor/home-drag-pause-v070-6930` (PR #6). App `com.foco.launcher`.
versionName se mantiene `0.7.1`. versionCode pasa de 11 a **12** para que este build pise el APK anterior firmado con la misma clave.
No se sube un APK aparte. El artefacto oficial sigue siendo el `assembleDebug` de esta línea (features de 0.7.1 + estos fixes).

Verificación local de esta revisión: `:app:testDebugUnitTest` (108 tests, 0 fallos) y `:app:assembleDebug`. El APK debug resultante pesa 10,4 MB (`10 930 523` bytes), `versionCode=12`, `versionName=0.7.1`. `aapt dump badging` confirma un solo `uses-permission` de la app: `RECEIVE_BOOT_COMPLETED`. En los dex no aparecen `ZoomOutMap` ni `AcUnit` (íconos que trae `material-icons-extended`), ni `foco-debug`, ni `storePassword`, ni `QUERY_ALL_PACKAGES`.

## Conteos

| Severidad | Encontrados | Corregidos en este branch | Diferidos |
| --- | ---: | ---: | ---: |
| P0 | 0 | 0 | 0 |
| P1 | 9 | 7 | 2 |
| P2 | 8 | 1 | 7 |

## Diferidos de alto riesgo

Estos dos P1 no se tocaron porque el arreglo cambia la instalación o el track de build, no un detalle interno.

1. **Clave debug pública** (`signing/foco-debug.keystore`, alias `foco-debug`, passwords `android` en `app/build.gradle.kts` y `README.md`). Cualquiera con el repo puede firmar un update de `com.foco.launcher`. En Android el grant del Notification Listener sobrevive a un update con la **misma** firma. Rotar la clave obliga a desinstalar, perder el grant y volver a conceder el listener. Borrar el archivo del árbol no lo saca del historial de git. No hay track de release separado en este branch.
2. **APK debug debuggable.** El manifest mergeado del `assembleDebug` tiene `android:debuggable=true`. Con `adb` y `run-as` se lee el DataStore, y un debugger adjunto al proceso ve las notificaciones que el listener ya recibe en memoria. Apagar `isDebuggable` en el build type `debug` rompe el flujo de depuración de esta misma línea, y un build type nuevo sería el track paralelo que se pidió no crear. El fix real va junto con una clave que no esté en el repo.

R8/`isMinifyEnabled` sigue en `false` en debug y release (`app/build.gradle.kts`). Prenderlo sin una pasada en dispositivo puede romper kotlinx.serialization y Compose. Queda diferido por el mismo motivo: no es un fix de una línea y no hay device en esta corrida.

## Hallazgos corregidos

### 1. El catálogo completo pisaba el home y guardaba íconos enormes

- Área: perf. Severidad: **P1**. Estado: **corregido**.
- Evidencia: `PackageRegistry.warmCatalog` publicaba todas las apps launcher en `launchables`, que `HomeViewModel` combina. `Drawable.toBitmapCached` se quedaba con el bitmap intrínseco (adaptive icons de varios cientos de px, ARGB_8888). `SettingsActivity.onResume` llamaba `refreshIfPackagesChanged()` e `invalidate()`, y el invalidate redecodificaba todo.
- Impacto: después del primer frame el home se recomponía con la lista completa, y el proceso retenía un bitmap grande por app instalada aunque la whitelist sea corta. Cada vuelta a Ajustes repetía el decode.
- Fix: `homeApps` es solo la whitelist y es lo que lee el home. `launchables` sigue siendo el catálogo para Ajustes. El scan del catálogo corre con `THREAD_PRIORITY_BACKGROUND` y restaura la prioridad del thread del pool. Los bitmaps se bajan a 192 px de lado mayor (48 dp en xxxhdpi). Si `lastUpdateTime != 0`, el sello reutiliza el bitmap. Ajustes solo llama `invalidate()`.

### 2. El drag recomponía toda la grilla en cada movimiento

- Área: perf. Severidad: **P1**. Estado: **corregido**.
- Evidencia: `HomeDragState.session` era un `mutableState` que se copiaba en cada `move()`. `AppCell` leía `drag.session`. `DragGhost` y `MiniIcon` llamaban `asImageBitmap()` en cada recomposición. `move()` hacía `tiles.values.toList()` y recalculaba los bounds de sección en cada evento.
- Impacto: con el dedo abajo, cada tile visible (highlight, alpha, ImageBitmap) se recomponía aunque el target no cambiara. El gesto y el dwell no cambian: el commit sigue usando `GroupDrag.commit` con `MIN_OVERLAP` y `COMMIT_DWELL_MS`.
- Fix: `chrome` (target, kind, overlap armado, tile levantado) se publica solo cuando eso cambia. `ghost` lo lee únicamente `HomeDragGhost`. El snapshot de tiles/grids se rearma solo si el layout cambió. El `ImageBitmap` del ghost y de los mini íconos queda en `remember`.

### 3. El santoral se parseaba en el main thread

- Área: perf. Severidad: **P1**. Estado: **corregido**.
- Evidencia: el `item` del reloj en `HomeScreen` hacía `assets.open("santoral_1962.json")` + `Santoral1962.parse` dentro de `remember`. El asset pesa 78 281 bytes.
- Impacto: el primer frame del home pagaba el decode JSON en el hilo de UI. El asset no es un problema de tamaño de APK.
- Fix: `FocoApp` llama `Santoral1962.load` en `Dispatchers.Default` junto con el warmup de íconos. Si el peek todavía es null cuando compone el reloj, queda el parse de antes, así la línea no desaparece.

### 4. Batería y próxima alarma se leían durante la composición

- Área: perf. Severidad: **P1**. Estado: **corregido**.
- Evidencia: `HomeClock` llamaba `readGlance()` en el cuerpo composable. `HomeGlance.line` usa `BatteryManager` y `AlarmManager.nextAlarmClock` (y a veces el sticky `ACTION_BATTERY_CHANGED`). El home se recompone con prefs, íconos de Trabajo y el minuto del reloj.
- Impacto: binders en el main thread en cada recomposición del reloj, no solo una vez por minuto.
- Fix: `HomeGlance.refresh` corre al arranque en background. El reloj pinta `peek()` y refresca en un `LaunchedEffect` sobre `Dispatchers.Default` cuando cambia el minuto. El orden sigue siendo hora → fecha → santoral → glance.

### 5. El estado del home y de Ajustes se armaba en el main thread

- Área: perf. Severidad: **P1**. Estado: **corregido**.
- Evidencia: `HomeViewModel.buildUi` llama `LaunchController.isDefaultHome` (`resolveActivity` de `CATEGORY_HOME`) y `NlsStatus.isGranted` (`NotificationManager` + `Settings.Secure`). `SettingsViewModel.buildState` suma `SuggestedApps.settingsPackage` y `UserProfileHelper.hasWorkProfile`. El `collect` estaba en `viewModelScope` (Main).
- Impacto: cada resume, cada cambio de prefs y cada refresh de Trabajo pegaban esos binders en el frame.
- Fix: ambos `collect` corren en `Dispatchers.Default`. El `StateFlow` sigue siendo el que Compose lee en main.

### 6. Cada notificación resolvía el dialer por PackageManager

- Área: perf. Severidad: **P1**. Estado: **corregido**.
- Evidencia: `NotificationPolicyEngine.shouldSuppress` → `SuggestedApps.isPhone` → `resolveActivity(ACTION_DIAL)` + fallbacks, en el callback del listener (main).
- Impacto: con el listener conectado el proceso vive en background. Cada `onNotificationPosted` hacía varias idas al PackageManager. El resultado (si es el dialer) casi no cambia.
- Fix: cache de 60 s del paquete de teléfono y del de Ajustes, invalidada en `PackageRegistry.onPackageEvent`. Un cambio de dialer por defecto sin alta/baja de paquete puede tardar hasta un minuto en verse. Las llamadas siguen exentas por categoría `call` aunque el paquete todavía no coincida.

### 7. `material-icons-extended` en el APK de release y de debug

- Área: perf. Severidad: **P1**. Estado: **corregido**.
- Evidencia: `gradle/libs.versions.toml` dependía de `material-icons-extended`. La UI usa seis vectores: `MoreVert`, `ArrowBack`, `Add`, `KeyboardArrowUp`, `KeyboardArrowDown`, `Delete`.
- Impacto: el artefacto extended mete miles de íconos en el dex. Debug no corre R8, así que el APK de sideload se los comía enteros.
- Fix: `material-icons-core`. Compila contra el BOM `2024.10.01`. En el APK de esta corrida no están `ZoomOutMap` ni `AcUnit`.

### 8. Log de perfiles de Trabajo en release

- Área: sec. Severidad: **P2**. Estado: **corregido**.
- Evidencia: `WorkCatalog.query` hacía `Log.i` con la cantidad de perfiles y `Log.w` con el stack si `getProfiles` / `getActivityList` fallaban, sin mirar `BuildConfig.DEBUG`. `PackageRegistry` ya logueaba solo en debug.
- Impacto: logcat de un build release describe perfiles que no son el del usuario. No loguea títulos ni texto de notificaciones.
- Fix: los tres logs quedan detrás de `BuildConfig.DEBUG`.

## Diferidos (no son el corte de alto riesgo de arriba)

### 9. `SettingsActivity` exportada con `foco://settings`

- Área: sec. Severidad: **P2**. Estado: **diferido**.
- Evidencia: `AndroidManifest.xml`, `SettingsActivity` `exported=true`, `VIEW` + scheme `foco` host `settings`. `intentDest` honra `EXTRA_DEST` (`edit`, `add`, `avisos`, `nls`).
- Impacto: otra app puede abrir Ajustes. El deep link sin extra cae en la pantalla principal. Con el extra puede abrir el onboarding del listener o el editor. Nada se concede solo: el grant del listener y la whitelist siguen pidiendo tap. Cerrar el export o exigir un token rompe el escape `foco://settings` documentado para adb.
- Fix propuesto, cuando se quiera: no honrar `EXTRA_DEST` si el intent no trae un token que solo arma `SettingsActivity.intent()`.

### 10. La lista de paquetes de sistema exentos es corta

- Área: sec. Severidad: **P2**. Estado: **diferido**.
- Evidencia: `NotificationPolicy.isProtectedSystemOrOem` cubre `android`, `android.*`, `com.android.*`, `com.google.android.apps.security*`, `com.google.android.permissioncontroller`, `com.motorola.android.*`, `com.motorola.ccc`. Antes de eso se perdonan call, alarm, navigation, transport, MediaStyle y el dialer (`NotificationPolicy.shouldSuppress`).
- Impacto: con «Pausar avisos» o el filtro, un aviso de sistema de Samsung/Xiaomi que no caiga en esas categorías se cancela. Ampliar la lista sin dispositivos OEM puede dejar pasar ruido o cancelar de más.
- Fix propuesto: medir en los equipos reales y agregar prefijos, sin cambiar el orden de las excepciones.

### 11. Prefs en claro

- Área: sec. Severidad: **P2**. Estado: **diferido**.
- Evidencia: `PrefsStore` guarda un JSON en DataStore `foco_launcher_prefs` (whitelist, `allowNotif`, pausas, grupos). No hay tokens ni cuerpos de notificación. `allowBackup=false` está en el manifest mergeado. `bioEnabled` no se usa en el launch: `BiometricGate.ENABLED_IN_LAUNCH_PATH` es `false`.
- Impacto: otra app no lee ese archivo. Sí lo lee `run-as` mientras el APK sea debuggable (hallazgo 2 de alto riesgo). Cifrar implica migrar el archivo; un fallo deja la whitelist vacía o el filtro en un estado distinto.
- Fix propuesto: hacerlo junto con un APK no debuggable, con migración que falle dejando el JSON anterior intacto.

### 12. `WorkCatalog.refresh()` en cada `onResume` del home

- Área: perf. Severidad: **P2**. Estado: **diferido**.
- Evidencia: `HomeViewModel.onResume` llama `workCatalog.refresh()`, `registry.refreshIfPackagesChanged()` y `NlsStatus.requestRebind`. El rebind ya está limitado por `NlsRebindGate` (1,5 s). El catálogo de Trabajo no decodifica íconos en `query()` (`LaunchpadGuardTest`).
- Impacto: volver al home relista actividades de otros perfiles. El callback de `LauncherApps` ya refresca en altas y bajas. Un throttle puede esconder un alta si el callback del OEM no dispara.
- Fix propuesto: no refrescar si el último query tiene menos de unos segundos y no hubo callback, recién después de medirlo.

### 13. El reloj sigue dormido de a un minuto con la activity en pausa

- Área: perf. Severidad: **P2**. Estado: **diferido**.
- Evidencia: `HomeClock` tiene `while (true)` + `delay` hasta el próximo minuto. El `LaunchedEffect` vive mientras el composable está en el árbol. El home no se destruye al abrir otra app.
- Impacto: un wakeup por minuto en background. No es un ticker de frames. Atarlo a `Lifecycle.State.STARTED` cambia el momento en que la hora se actualiza al volver; es chico, pero es comportamiento visible. No se tocó.

### 14. R8 apagado

- Área: perf. Severidad: **P2**. Estado: **diferido, alto riesgo de implementarlo ahora**.
- Evidencia: `isMinifyEnabled = false` en debug y release. El APK debug de esta corrida tiene 8 `classes*.dex` (sin comprimir, `classes.dex` ~18 MB y `classes8.dex` ~7,5 MB). Compose y Kotlin dominan eso; el santoral son 78 KB.
- Fix propuesto: minify solo en un build que se pruebe en un teléfono, con reglas para `@Serializable`. No en este debug.

### 15. `ProfileInstallReceiver` de AndroidX

- Área: sec. Severidad: **P2**. Estado: **diferido**.
- Evidencia: el manifest mergeado exporta `androidx.profileinstaller.ProfileInstallReceiver` con `android:permission="android.permission.DUMP"`. No es código de Foco.
- Impacto: solo un caller con `DUMP` (shell) puede invocarlo. Sacarlo desactiva el profile installer y puede empeorar el cold start.

## Sin hallazgo

- **Permisos.** Pedido por Foco: solo `RECEIVE_BOOT_COMPLETED` (normal, no dangerous). No hay `INTERNET`, `QUERY_ALL_PACKAGES`, ni permisos de notificación policy / device admin. El permiso `com.foco.launcher.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` lo agrega AndroidX con `protectionLevel=signature`.
- **`<queries>`.** MAIN/LAUNCHER, DIAL, sms, IMAGE_CAPTURE y pantallas de Ajustes. No es un permiso de visibilidad total.
- **Backup.** `android:allowBackup=false` en el manifest mergeado. No hay `BackupAgent` ni `usesCleartextTraffic`. Sin red, cleartext no aplica.
- **Boot.** `BootCompletedReceiver` está exportado con `android:permission="RECEIVE_BOOT_COMPLETED"`. Otra app no puede falsificar el broadcast. Solo calienta íconos de la whitelist (`warmHomeIcons`), no el catálogo.
- **Paquetes.** `PackageChangeReceiver` está `exported=false`. No auto-whitelistea altas. Una baja sí saca el paquete de la lista.
- **Accessibility.** No hay `AccessibilityService` en el fuente ni en el manifest. Las cadenas `AccessibilityService` del dex son nombres del compilador de Compose y descriptores del framework.
- **Notification Listener.** `FocoNotificationListener` está exportado con `BIND_NOTIFICATION_LISTENER_SERVICE` (solo el sistema bindea). Cancela con `cancelNotification(sbn.key)`, nunca `cancelAllNotifications`. No hay inbox. `onNotificationPosted` falla abierto si `startupReady` es false o si `shouldSuppress` tira. Perfil de trabajo u otro user: `UserProfileHelper` compara `sbn.user` con `Process.myUserHandle()`. Esas notificaciones se cancelan solo si `workSectionPaused` o `notificationsPaused`, y después de perdonar sistema, call, alarm, navigation, transport, media y dialer. El texto de la notificación no se loguea ni se persiste.
- **Biometría.** El launch no muestra prompt mientras `ENABLED_IN_LAUNCH_PATH` es false.
- **Secrets en el APK.** Las passwords viven en Gradle y en el README, no en el dex de este build.
- **Reloj en home.** No hay `Timer` ni delay de frame. El período es el resto hasta el minuto, acotado entre 250 ms y 60 s.
- **Íconos de Trabajo.** `WorkCatalog.query` no llama `getIcon`. El decode es por celda (`ensureIcon`). El guard de `LaunchpadGuardTest` sigue verde.
- **Leaks de home revisados.** `FocoNotificationListener.active` se limpia en `onListenerDisconnected` con `compareAndSet`. Los `CoroutineScope` de `FocoApp`, `PackageRegistry`, `WorkCatalog` y `NotificationPolicyEngine` son de proceso, no de Activity. El callback de `LauncherApps` está registrado en la Application. `CompositionLocalProvider` del tema solo provee `LocalContentColor`. No hay cache estática de bitmaps fuera de los `StateFlow` del registry (ahora la del home es la whitelist; el catálogo sigue en memoria cuando Ajustes lo cargó, pero con el tope de 192 px).

## Qué no cambió de producto

Drag entre secciones, dwell, pausa de Trabajo, pausa de avisos, santoral 1962, glance de batería/alarma, nombres solo opt-in, y el escape `foco://settings`. No hay `AppWidgetHost`. No hay escritura de quiet mode ni de interruption filter.
