# Delta arquitectura — NotificationListener en Foco Launcher v0

**De:** Andrés · Arquitectura  
**Pedido:** FORGE / Agus GO — NLS **ahora** (ya no v0.2)  
**Fecha:** 2026-09-19  
**Base:** `foco-launcher-arquitectura-v1.md` · OK semana 1  
**Device QA:** Motorola Edge 30  
**Update:** 2026-09-19 — work profile intocable + allowlist notifs **separada**

---

## 0) Delta crítico — work profile + notifs (Agus)

### Reglas de producto

1. **Perfil de trabajo: NO administrar / NO romper.** Foco no toca apps ni políticas work.
2. **Allowlist de notificaciones ≠ whitelist del home** (dos stores).
3. **Default work:** **TODAS** las notifs del work profile **pasan**.
4. **Personal:** cancelar salvo apps en `notificationAllowlist` (editable; seed opcional desde home).
5. Usuario elige de qué apps quiere avisos (UI de allowlist notifs).

### Detección work vs personal

| Señal | Uso |
|-------|-----|
| `sbn.user` / `StatusBarNotification.getUser()` → `UserHandle` | Primera rama en `onNotificationPosted` |
| `UserManager` + managed profile APIs | Confirmar perfil managed/work |
| `Process.myUserHandle()` vs `sbn.user` | Distinto + managed → **PASS** |
| Package name solo | **Insuficiente** — app dual comparte package; discrimina **UserHandle** |
| `LauncherApps` por `UserHandle` | UI home **solo** personal; no gestionar grid work |

```text
shouldSuppress(sbn):
  if protectedSystemOrOem(sbn): return false
  if callAlarmMediaException(sbn): return false
  if isWorkProfile(sbn.user): return false  // SIEMPRE
  return sbn.packageName !in notificationAllowlist  // personal only
```

NLS multi-user: el listener puede ver posts work; **branch UserHandle antes** de allowlist.

### Qué NO hacer

- `DevicePolicyManager` / Device Admin / Profile Owner / Device Owner
- Disable, hide, force-stop o quiet-mode de apps work
- Cancelar notifs work por default o por lista
- Unificar home+work en un solo grid en v0
- Accessibility para “cerrar” work apps

### DataStore

```text
homeWhitelist: List<{packageName, order, bioEnabled}>  // iconos home personal
notificationAllowlist: Set<packageName>                // avisos personal only
nlsFilterEnabled: Boolean
// seed 1× opcional: notificationAllowlist ← homeWhitelist.packages
```

Settings: pantalla **“Avisos”** (allowlist notifs) separada de **“Apps en home”**.

---

## 1) Cambio de scope

| Antes | Ahora (v0) |
|-------|------------|
| NLS = v0.2+ | **NLS in-scope v0** |
| Semana 2 ≈ solo bio | Semana 2 = **NLS primero**, bio después (o en paralelo si sobra) |

**Modelo producto (cerrado) — ver también §0:**
- **Personal +** package **∉ notificationAllowlist** → cancel (best-effort).
- **Personal +** package **∈ notificationAllowlist** → pasar.
- **Work profile** → **pasar siempre**.
- Home whitelist **independiente** (qué iconos ves); notifs allowlist **independiente** (qué te avisa).
- Sin inbox / stats / gamificación / ranking UI.
- Onboarding **honesto** NLS + mención: *“Las notificaciones del perfil de trabajo no las tocamos.”*
- Local-first; **no** `QUERY_ALL_PACKAGES`.

**Límite honesto:** no es MDM ni admin del work profile. Revoco NLS / OEM raro / notifs sistema → puede colarse ruido en personal. Copy acorde.

---

## 2) Componentes

| Pieza | Rol |
|-------|-----|
| `FocoNotificationListener` | `NotificationListenerService` — único lugar que cancela |
| `NotificationPolicy` | `shouldSuppress(sbn): Boolean` — UserHandle work→false; personal→∉ notificationAllowlist; + excepciones §4 |
| `HomeWhitelistStore` | DataStore — iconos home (personal) |
| `NotificationAllowlistStore` | DataStore — **separado**; editable en Settings “Avisos” |
| `UserProfileHelper` | `isWork(UserHandle)` vía UserManager; wrappers LauncherApps para UI personal |
| `NlsStatus` | Helper: `isEnabled()`, Intent a settings de notification listener |
| Settings UI | Toggle filtro + CTA permiso + editor **allowlist avisos** (≠ editor home) |
| Setup / post-HOME | Paso opcional **después** de ser launcher default (no bloquear ROLE_HOME por NLS) |

**Gradle semana 1:** sigue **un solo `:app`**. Clases en `com.foco.launcher.notification` (o `security`). No módulo nuevo.

**Manifest (delta):**

```xml
<service
    android:name=".notification.FocoNotificationListener"
    android:exported="true"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
  <intent-filter>
    <action android:name="android.service.notification.NotificationListenerService" />
  </intent-filter>
</service>
```

Sin permisos `QUERY_ALL_PACKAGES`. Sin inbox activities.

---

## 3) Ciclo de vida del listener

```
User habilita Foco en Ajustes → Apps de notificaciones / Notification access
  → sistema bind FocoNotificationListener
  → onListenerConnected()
       · refrescar snapshot notificationAllowlist + flags
       · opcional: getActiveNotifications() — cancel solo personal que fallen policy (nunca work)
  → onNotificationPosted(sbn)
       · if policy.shouldSuppress(sbn) → cancelNotification(sbn.key)
       · else no-op
  → onNotificationRemoved — no-op (no inbox)
  → onListenerDisconnected()
       · NlsStatus = off; banner permiso
  → notificationAllowlist change
       · invalidar cache policy; opcional re-scan active (solo personal)
```

**Hilos:** binder thread → cancel rápido; allowlist en memoria. NLS puede recibir eventos de **ambos** users; **branch por UserHandle primero**.

**No** foreground service. **No** WorkManager. Si el proceso muere, el sistema rebind cuando pueda; UI no finge que filtra si `!isEnabled()`.

---

## 4) Ranking / ongoing / media / llamadas

Regla default: **suprimir por package**, no por texto.

| Tipo | Acción v0 | Motivo |
|------|-----------|--------|
| Apps personales ∉ **notificationAllowlist**, notif “normal” | **Cancel** | Core del producto |
| Apps ∈ **notificationAllowlist** | **Pasar** | Incluye IG si el user la puso en Avisos |
| `CATEGORY_CALL` / call-style / `ongoing` de dialer | **Nunca cancelar** si package es Teléfono/dialer **o** category CALL | Seguridad / no romper llamadas |
| Media / `MediaStyle` / `FOREGROUND_SERVICE` media | **No cancelar** si category `CATEGORY_TRANSPORT` o MediaStyle | Evitar cortar Spotify mid-track por error de lista |
| `CATEGORY_NAVIGATION` / nav ongoing | **Nunca cancelar** | Maps en ruta |
| `CATEGORY_ALARM` / `CATEGORY_EVENT` urgentes | **No cancelar** si category ALARM (y opcional EVENT) | Despertador / alarmas sistema |
| Ongoing genérico de app ∉ notificationAllowlist | **Cancel** (salvo CALL/ALARM/MediaStyle arriba) | Ej. “Instagram está ejecutándose” — ok matar |
| Group summary | Cancel child keys; summary suele caer solo | Probar en Moto |
| Notificaciones de `com.android.*` / `com.motorola.*` / `android` | **Allowlist sistema fija** (no cancelar) | OEM / shade / USB / batería |
| Ranking (`NotificationListenerService.Ranking`) | **No usar** para UI; no reordenar shade | Sin inbox / sin “prioridad Foco” |

**Allowlist sistema (código, no UI):** packages que empiezan por `android.`, `com.android.`, `com.google.android.apps.security`, dialer detectado, + `com.motorola.` cuidadosamente (no “Motorola App” bloat — solo lo necesario para no romper panel). Preferir **category-based exceptions** (CALL/ALARM/MediaStyle) antes que lista enorme OEM.

Si dialer ∉ notificationAllowlist / home pero suena llamada: **igual no cancelar** CALL. Igual NAVIGATION y MediaStyle. Documentar: *“Llamadas, alarmas, navegación y media en curso no las silenciamos.”*

---

## 5) Recovery si revocan el permiso

1. `onListenerDisconnected` + check al `onResume` de Home/Settings vía `NotificationManager.isNotificationListenerAccessGranted` (API 27+ / compat).  
2. Banner persistente no agobiante: “Filtro de avisos off” → CTA `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`.  
3. **No** crash; Home + stores siguen.  
4. **No** auto-loop de intents a settings más de 1× por sesión.  
5. Sideload README (1 línea): cómo volver a dar acceso en Motorola (Ajustes → Notificaciones → Acceso / apps de escucha).  
6. Toggle “Filtrar notificaciones” en Settings Foco: si off, listener conectado pero `policy` no-op (o unbind mental: `suppressEnabled=false` en DataStore). Si on y sin permiso → CTA.

Revocar ≠ desinstalar launcher. ROLE_HOME independiente de NLS.

---

## 6) Orden vs bio (semana 2)

**Recomendación arquitectura: NLS antes que bio.**

| Orden | Por qué |
|-------|---------|
| Semana 1 | Sin cambio: HOME + whitelist + open + Settings escape (**sin** NLS obligatorio para done) |
| Semana 2a (primero) | **NLS** + onboarding permiso + policy + QA Moto Edge 30 |
| Semana 2b | **Bio** por app (como OK arch previo) |

**Motivo:** el GO de Agus prioriza menos distracción real (avisos de apps ocultas). Bio solo cubre taps en Foco; NLS cubre el shade todo el día. Si el tiempo aprieta, **ship NLS y deslizá bio** antes que lo inverso.

**Paralelo OK** si Mateo + Nico parten: uno NLS, uno bio — sin acoplar releases. Un solo APK sideload al final de semana 2 con ambos si entra; feature flags DataStore: `nlsFilterEnabled`, bio ya previsto.

**Setup:** no gatear ROLE_HOME con NLS. Pedir NLS **después** de “ya soy home”, en Settings o card post-setup. Evita fatiga de permisos en el momento frágil de cambiar launcher.

---

## 7) Qué NO hacer en este delta

- Inbox / historial / “resumen diario”  
- Filtrar por palabra clave / ML  
- Reordenar ranking del sistema  
- Cancelar CALL/ALARM/MediaStyle  
- `QUERY_ALL_PACKAGES`  
- Accessibility “por las dudas”  
- Prometer 0 notificaciones de apps ocultas al 100%  
- Administrar / disable / quiet work profile  
- DevicePolicyManager / Device Admin  
- Cancelar notifs work por default  

---

## 8) Criterio done NLS v0

1. Personal + app ∉ notificationAllowlist → cancel.  
2. Personal + app ∈ notificationAllowlist → visible.  
3. Work profile notif → **siempre** visible (aunque package no esté en home).  
4. Home whitelist ≠ notifs allowlist (UI separada; seed opcional).  
5. Llamada/alarma/media exceptions intactas.  
6. Revoco NLS → banner; Home sigue; work apps intocadas.  
7. Sin Device Policy / disable work. Sin inbox.

**GO** al delta (ver §9 ACK).

---

## 9) Arch ACK — re-GO (Mateo HOLD → GO)

**Estado:** RE-GO. Supersede cualquier mención vieja a “una sola WhitelistStore = Home”. Producto locked = §0 (FORGE §0 APPROVE).

### Respuestas a Nico (§9 notes)

| # | Pedido | Arch OK |
|---|--------|---------|
| 1 | ¿GO NLS inmediato (rompe NLS=v0.2 de arch v1)? | **GO** — v0 semana **2a** |
| 2 | shouldPreserve CALL+ALARM+NAVIGATION siempre? ¿Media ongoing cancel si ∉ allowlist? | **CALL + ALARM + NAVIGATION + MediaStyle/ongoing media → siempre PASS** (nunca cancel). No cancelar media por ∉ allowlist |
| 3 | ¿NLS solo post-setup+toggle o también con allowlist vacía? | **Solo post-setup + toggle/permiso.** No gatea ROLE_HOME. Si filtro ON y `notificationAllowlist` vacía → cancel **todas** personal salvo preserves (comportamiento correcto) |
| 4 | ¿Package `notification/` en `:app` OK? | **OK** — single `:app`, package `com.foco.launcher.notification` |
| 5 | ¿NLS antes que bio? | **Sí** — 2a NLS, 2b bio |

### Policy canónica (copiar a código)

```text
shouldSuppress(sbn):
  if isWorkProfile(sbn.user): return false          // UserHandle ≠ my → PASS
  if protectedSystemOrOem(sbn): return false
  if category in {CALL, ALARM, NAVIGATION}: return false
  if MediaStyle or CATEGORY_TRANSPORT ongoing: return false
  if not nlsFilterEnabled: return false
  return sbn.packageName !in notificationAllowlist  // personal only
```

Stores: `HomeWhitelistStore` ≠ `NotificationAllowlistStore`. Seed opcional home→notifs. Sin DevicePolicy.

**Mateo: GO implementación 2a** bajo este techo.
