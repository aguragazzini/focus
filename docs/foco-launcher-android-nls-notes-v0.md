# Foco Launcher — NotificationListenerService (NLS) notes v0

**Autor:** Nico · Android Launcher  
**Rev:** 2026-09-19 — detección work/managed + `allowNotif` + UX Camila  
**Techos:** `foco-launcher-arch-nls-v0.md` §0 · `foco-launcher-ux-nls-onboarding-v0.md` · `foco-launcher-nls-scope-v0.md`  
**Device:** Motorola Edge 30 · minSdk **26** (→29 post-getprop) · targetSdk **34**  
**Estado:** notes listas. **Mateo HOLD** hasta GO FORGE post-patch (FORGE 2026-09-19).

Orden: semana **2a NLS → 2b bio**. NLS **no** gatea ROLE_HOME. Single `:app`. Package `com.foco.launcher.notification`.

---

## 0. Modelo (Andrés + Camila + FORGE)

| Eje | Regla |
|-----|--------|
| Work / managed profile | **PASS siempre** — no cancel, no DevicePolicy, no UI de admin |
| Personal | Cancel salvo spare **y** app en home con `allowNotif=true` |
| Apps personal ∉ home | No avisan (no aparecen en lista “Apps que pueden avisar”) |
| Home app + `allowNotif=false` | Icono sí; notif **no** |
| Sin inbox / ranking UI | Sí |
| Master `nlsFilterEnabled` | OFF → no-op aunque listener bound |

**DataStore (implementación):**

```kotlin
data class WhitelistEntry(
  val packageName: String,
  val order: Int,
  val bioEnabled: Boolean = true,
  val allowNotif: Boolean = true, // Camila/FORGE: default ON al agregar a home
)

data class LauncherPrefs(
  val setupDone: Boolean = false,
  val entries: List<WhitelistEntry> = emptyList(), // home personal only
  val nlsFilterEnabled: Boolean = false,
  // Opcional cache/derivado (Andrés “notificationAllowlist”):
  // notificationAllowlist = entries.filter { it.allowNotif }.map { it.packageName }.toSet()
)
```

UI Settings “Notificaciones” = Camila: master + lista **solo** apps home personal con switch `allowNotif` + bloque work si `hasWorkProfile`.

---

## 1. Detección work / managed profile (PATCH FORGE)

**Primera rama** en `onNotificationPosted` — **antes** de allowlist. Package name solo es **insuficiente** (dual apps).

### 1.1 Señales

| API | Uso |
|-----|-----|
| `sbn.user` / `StatusBarNotification.getUser()` → `UserHandle` | User de la notif |
| `Process.myUserHandle()` | User donde corre Foco (personal) |
| `UserManager.userProfiles` / `getUserProfiles()` | Lista perfiles del usuario |
| `UserManager.isManagedProfile` (API 30+ context) / `UserManager.isManagedProfile()` legacy | ¿El *current* es managed? — para UI `hasWorkProfile` |
| `UserManager.isUserRunning` | Opcional |

### 1.2 Código recomendado (sin hidden API)

```kotlin
object WorkProfileNotifs {
  /** true ⇒ NLS must PASS (never cancel). */
  fun isWorkOrOtherProfile(sbn: StatusBarNotification): Boolean {
    val notifUser = sbn.user
    val myUser = Process.myUserHandle()
    if (notifUser == myUser) return false
    // Different UserHandle than Foco process ⇒ work/other profile → PASS
    return true
  }

  fun hasWorkProfile(context: Context): Boolean {
    val um = context.getSystemService(UserManager::class.java) ?: return false
    val profiles = um.userProfiles // List<UserHandle>
    val my = Process.myUserHandle()
    if (profiles.size <= 1) return false
    // API 30+:
    return profiles.any { uh ->
      uh != my && runCatching {
        // isManagedProfile() without args = current user only.
        // Compare via UserManager.getUserProperties (API 34) or:
        context.createContextAsUser(uh, 0).getSystemService(UserManager::class.java)
          ?.isManagedProfile == true
      }.getOrDefault(uh != my) // if createContextAsUser fails, still treat extra profiles as work-like → PASS notifs
    }
  }
}
```

**Regla dura:** si `isWorkOrOtherProfile(sbn)` → **nunca** `cancelNotification`. Default PASS aunque arch futuro diga opt-in (hoy no hay opt-in work).

**UI:** `hasWorkProfile` solo para mostrar el card Camila (“Foco no gestiona el perfil de trabajo…”). Cero toggles work.

### 1.3 `LauncherApps` (home)

v0: home lista **solo** personal (`Process.myUserHandle()`). No grid work. Si más adelante: `LauncherApps.getActivityList(pkg, userHandle)` — fuera de NLS.

### 1.4 Badges

Cancelar notif personal puede bajar badge — OK. No tocar badges work.

---

## 2. Policy personal (`shouldSuppress`)

```text
shouldSuppress(sbn):
  if isWorkOrOtherProfile(sbn): return false          // FORGE/Andrés
  if !nlsFilterEnabled || !listenerGranted: return false
  if protectedSystemOrOem(sbn): return false          // Andrés allowlist sistema
  if callAlarmMediaException(sbn): return false       // CALL / ALARM / MediaStyle
  val entry = homeEntries.find { it.packageName == sbn.packageName }
  if (entry == null): return true                     // fuera de home → cancel
  if (!entry.allowNotif): return true                 // home pero mute notif
  return false                                        // allowNotif → pass
```

Equivalente Andrés: `notificationAllowlist = { pkg | entry.allowNotif }`; suppress iff personal && pkg ∉ that set (tras spares).

Default al agregar a home: `allowNotif = true` (Camila).

---

## 3. Manifest

```xml
<service
    android:name=".notification.FocoNotificationListener"
    android:exported="true"
    android:label="@string/nls_service_label"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
  <intent-filter>
    <action android:name="android.service.notification.NotificationListenerService" />
  </intent-filter>
</service>
```

Sin Device Admin / Accessibility / `QUERY_ALL_PACKAGES`.

---

## 4. Permiso + UX (Camila)

- Toggle settings: **“Silenciar otras notificaciones”**.  
- ON sin grant → pantalla onboarding Camila → `ACTION_NOTIFICATION_LISTENER_SETTINGS`.  
- Sub: **“Perfil personal · work sin tocar”**.  
- Pedir **después** de HOME; no gate setup launcher.  
- Copy work obligatorio si `hasWorkProfile`.  
- Strings: ver `foco-launcher-ux-nls-onboarding-v0.md` copy bank.

Detección grant: `NotificationManager.isNotificationListenerAccessGranted(ComponentName)` (API 27+) o parse `Settings.Secure.ENABLED_NOTIFICATION_LISTENERS`.

---

## 5. cancel vs snooze / spares

- **`cancelNotification(sbn.key)`** default.  
- No snooze.  
- Spare: `CATEGORY_CALL`, `CATEGORY_ALARM`, MediaStyle / `CATEGORY_TRANSPORT`, packages `android.` / `com.android.` / OEM panel necesario (Andrés §4).  
- `onListenerConnected`: opcional scrub `getActiveNotifications()` con misma policy (skip work handles).

---

## 6. Edge 30 testing (multi-user)

### Personal

- [ ] Home app `allowNotif=true` → notif pasa  
- [ ] Home app `allowNotif=false` → cancel  
- [ ] App ∉ home → cancel  
- [ ] CALL/ALARM/MediaStyle spare  
- [ ] Revoco NLS → banner Camila; Home OK  

### Work (obligatorio Agus)

- [ ] Detectar `hasWorkProfile`  
- [ ] Notif work (`sbn.user != myUser`) **permanece** con master ON  
- [ ] Ningún control UX silencia work  
- [ ] Home sin iconos work OK  

### Reportar

```
model= sdk=
userProfiles_count=
work_notif_userHandle_differs=yes
personal_allowNotif_off_cancels=yes
```

---

## 7. Alineación Camila (checklist impl)

- [ ] 1 pantalla onboarding NLS; CTA Ajustes; “Ahora no”  
- [ ] Settings → Notificaciones: master + lista personal + card work  
- [ ] Default `allowNotif=true` al whitelistear  
- [ ] Footer: “Las apps que no están en Foco no avisan.”  
- [ ] Feature no se muestra “activa” sin permiso sistema  

---

## 8. HOLD / GO

| Actor | Estado |
|-------|--------|
| Andrés | Arch §0 GO (work PASS + allowlist ≠ home conceptual; Camila = `allowNotif` en entry) |
| Nico notes | Este doc (rev detección) |
| Mateo | **HOLD** hasta GO FORGE explícito post-patch |
| Semana 2a | NLS; 2b bio |

---

*Patch detección UserHandle/UserManager 2026-09-19. Policy = home + allowNotif (FORGE) ≡ notificationAllowlist derivado (Andrés).*
