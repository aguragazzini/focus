# Foco Launcher — Arquitectura Android v1

**De:** Andrés · Arquitectura  
**Fecha:** 2026-09-19  
**Inputs:** `launcher-android-spec-v0.md` · `foco-launcher-decisions-2026-09-19.md`  
**Stack locked:** Kotlin · Jetpack Compose · HOME launcher · PackageManager · DataStore · BiometricPrompt  
**Package:** `com.foco.launcher`  
**Distribución v0.1:** sideload · sin backend · sin Accessibility · NotificationListener = v0.2+  

---

## 0. Veredicto

**GO** al MVP 0.1 como launcher whitelist + bio en taps del launcher.

**Honestidad dura — intents externos:** un launcher **no puede** interceptar de forma fiable aperturas iniciadas fuera de él (notificación → Instagram, Chrome → `intent://`, share sheet, deep link, `startActivity` desde otra app). Eso lo resuelve el **ActivityManager** del sistema, no el HOME. En v0.1 protegemos **solo** aperturas desde nuestra UI. Bloqueo de intents indirectos = **fuera** (eval v0.3+ con mecanismos distintos; no fingir en v0.1).

**No reusar** `foco-moto-v3` / Capacitor / Accessibility como base. Repo/app **de cero**.

---

## 1. Módulos (packages internos)

```
com.foco.launcher
├── core/          # Home UI, grid, launch orchestration
├── registry/      # PackageManager + whitelist DataStore + package changes
├── security/      # BiometricPrompt gate
└── settings/      # Whitelist editor, bio toggles, “set default launcher”
```

| Módulo | Responsabilidad | Depende de |
|--------|-----------------|------------|
| **core** | `LauncherActivity` Compose home; lista ordenada; tap → Security → Intent launch; empty state | registry, security |
| **registry** | Enumerar launchables; CRUD whitelist; orden; cache iconos/labels; `PackageChangeReceiver` | DataStore, PM |
| **security** | `BiometricPrompt` por app; flag `bioEnabled`; fallback cancel = no open; sin almacenar biometría | AndroidX Biometric |
| **settings** | UI selección apps, reorder, bio per-app, CTA “Usar como inicio”, escape a Ajustes sistema | registry, security |

**Fuera de v0.1 (stubs no):** `notification/`, `recommendation/`, `intentguard/`.

Filosofía: un solo módulo Gradle app (`:app`). Subpackages, no multi-module Gradle en semana 1 (overhead). Multi-module solo si el repo crece post-kill-check 30d.

---

## 2. Componentes Android

| Componente | Tipo | Rol |
|------------|------|-----|
| `LauncherActivity` | Activity | `MAIN` + `HOME` + `DEFAULT`; home whitelist |
| `SettingsActivity` | Activity | Config; **exported** con deep link interno `foco://settings` para escape |
| `SetupActivity` | Activity | Primera corrida: elegir mínimos **antes** de pedir default launcher |
| `PackageChangeReceiver` | BroadcastReceiver | `PACKAGE_ADDED/REMOVED/REPLACED` → invalidar cache; **no** auto-agregar a whitelist |
| *(no)* Service foreground | — | v0.1 no necesita servicio residente |
| *(no)* AccessibilityService | — | prohibido v0.1 |
| *(no)* NotificationListenerService | — | v0.2+ |

**Launch path (único protegido en v0.1):**

```
Tap icono (core)
  → si bioEnabled[pkg] → BiometricPrompt (security)
  → success → registry.resolveLaunchIntent(pkg) → startActivity
  → fail/cancel → no-op (sin drama UI)
```

---

## 3. Manifest mínimo

```xml
<manifest package="com.foco.launcher">
  <!-- Android 11+: ver apps con launcher intent -->
  <queries>
    <intent>
      <action android:name="android.intent.action.MAIN" />
      <category android:name="android.intent.category.LAUNCHER" />
    </intent>
  </queries>

  <!-- Ideal 0 permisos dangerous en v0.1.
       NO QUERY_ALL_PACKAGES salvo que queries no alcance en OEM de prueba. -->

  <application
      android:label="Foco"
      android:icon="@mipmap/ic_launcher"
      android:allowBackup="false"
      android:supportsRtl="true">

    <activity
        android:name=".core.LauncherActivity"
        android:exported="true"
        android:launchMode="singleTask"
        android:stateNotNeeded="true"
        android:theme="@style/Theme.Foco.Launcher"
        android:excludeFromRecents="true">
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>

    <activity
        android:name=".settings.SetupActivity"
        android:exported="false"
        android:theme="@style/Theme.Foco" />

    <activity
        android:name=".settings.SettingsActivity"
        android:exported="true"
        android:theme="@style/Theme.Foco">
      <!-- Escape hatch: no depender solo del home vacío -->
      <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:scheme="foco" android:host="settings" />
      </intent-filter>
    </activity>

    <receiver
        android:name=".registry.PackageChangeReceiver"
        android:exported="false">
      <intent-filter>
        <action android:name="android.intent.action.PACKAGE_ADDED" />
        <action android:name="android.intent.action.PACKAGE_REMOVED" />
        <action android:name="android.intent.action.PACKAGE_REPLACED" />
        <data android:scheme="package" />
      </intent-filter>
    </receiver>
  </application>
</manifest>
```

**Nota OEM:** si en Motorola Edge 30 `queries` no lista alguna app, medir antes de pedir `QUERY_ALL_PACKAGES` (Play lo odia; sideload ok pero es olor a overreach).

---

## 4. Modelo de datos (DataStore)

```kotlin
// Fuente de verdad local — proto o preferences JSON
data class WhitelistEntry(
  val packageName: String,
  val order: Int,
  val bioEnabled: Boolean = true,  // default ON al agregar (decisión 19/09)
)

data class LauncherPrefs(
  val setupComplete: Boolean = false,
  val biometricGlobalEnabled: Boolean = true, // master; per-app puede off
  val entries: List<WhitelistEntry> = emptyList(),
)
```

Reglas:
- **Cero** entries al instalar → home vacío hasta setup.  
- Al **agregar** app: `bioEnabled = true`; excepción explícita (Teléfono sugerido off).  
- App desinstalada: receiver limpia entry huérfana.  
- App nueva instalada: **no** entra sola.  
- No cloud, no cuenta, `allowBackup=false`.

Cache en memoria (registry): `LaunchableApp(packageName, label, icon: Bitmap/ImageBitmap, launchIntent component)`. Refresh on package change + pull-to-refresh en settings.

---

## 5. Flujo setup → default launcher

```
Install APK (sideload)
  → primera apertura: SetupActivity (no Launcher aún como default)
  → Paso 1: “Elegí apps mínimas” 
       sugeridas (checkboxes, user confirma): Teléfono, Mensajes, Cámara, Ajustes sistema
       + aviso: Ajustes del launcher siempre accesible
  → Paso 2: bio global on/off (default on)
  → Paso 3: CTA “Poner Foco como pantalla de inicio”
       → Intent(Settings.ACTION_HOME_SETTINGS) o ACTION_MANAGE_DEFAULT_APPS
       → User elige Foco en picker OEM
  → setupComplete=true → LauncherActivity
  → Home solo whitelist

Escape anti-brick:
  - SettingsActivity siempre alcanzable desde home (ícono engranaje fijo, NO cuenta como “app whitelist”)
  - o long-press vacío → Settings
  - o `foco://settings`
  - Botón “Volver al launcher anterior”: HOME_SETTINGS otra vez
```

**No** pedir ser default hasta haber ≥1 app de comunicación o confirmación explícita “sé que el home puede quedar casi vacío”.

---

## 6. Riesgos Motorola / OEM (Edge 30 y familia)

| Riesgo | Impacto | Mitigación v0.1 |
|--------|---------|-----------------|
| Picker HOME confuso / “launcher de sistema” vs terceros | No setea default | Copy + deep link a `HOME_SETTINGS`; QA manual checklist |
| Gestos / botón Home vuelven al launcher stock un día | Pérdida de hábito | Re-detectar `RoleManager.ROLE_HOME` / `getDefaultActivity` al resume; banner “Foco no es el inicio” |
| BiometricPrompt flaky / sin huella | No abre apps | Fallback: device credential (PIN) vía `setAllowedAuthenticators`; si nada, toggle bio off en settings |
| `PACKAGE_*` broadcasts limitados | Whitelist stale | También refresh al onResume de Settings/Launcher |
| Iconos adaptive / themed icons | Feos o null | Usar `getApplicationIcon`; placeholder si falla |
| Batería / “apps en segundo plano” | Irrelevante si no hay service | No inventar keep-alive |
| Recientes muestran Launcher | Ruido | `excludeFromRecents=true` en launcher activity |

---

## 7. Plan módulos — semana 1 vs 2

### Semana 1 — “Se puede vivir” (sin bio)

1. Proyecto Android vacío Compose · package `com.foco.launcher`  
2. Manifest HOME + LauncherActivity minimal (grid vacía + engrane)  
3. Registry: listar launchables via PM + `queries`  
4. DataStore whitelist CRUD + reorder básico  
5. SetupActivity mínimos + CTA default launcher  
6. Tap → `startActivity` **sin** bio  
7. PackageChangeReceiver cleanup  
8. Sideload APK en Motorola; usar ≥4–6 h como HOME  

**Exit semana 1:** Agus puede setear Foco default, ver solo whitelist, abrir apps, entrar a settings, no brick.

### Semana 2 — Bio + uso diario

1. Security: BiometricPrompt en launch path; default on por entry  
2. Settings: toggle bio global + per-app (Teléfono excepción UI)  
3. Estabilidad: empty states, “no es default”, crash-free  
4. Performance: cache icons, no work en main al dibujar home  
5. Hardening menor: `allowBackup=false`, sin logs de packages sensibles  

**Exit semana 2:** bio en cada tap (salvo excepciones); 2–3 días seguidos como launcher principal sin volver al stock por bronca.

### Después (no estimar ahora)

v0.2 NotificationListener opcional · v0.3 investigación intents indirectos (ver §8) · recomendaciones nunca en critical path.

---

## 8. Límites de intents externos (leer antes de prometer)

| Origen de apertura | ¿Launcher puede exigir bio / bloquear? | v0.1 |
|--------------------|----------------------------------------|------|
| Tap en Foco home | **Sí** | GO |
| App drawer Foco | No hay drawer | — |
| Notificación de app | **No** (sistema → app) | Fuera |
| Deep link / App Link | **No** | Fuera |
| Share / chooser | **No** (chooser sistema) | Fuera |
| Otra app `startActivity` | **No** | Fuera |
| Widget otra app | **No** | Fuera |
| ADB `am start` | **No** | OK |

Cualquier “bloqueo universal” requiere **otra superficie** (Accessibility, Device Owner/MDM, role especial) — consciente fuera de v0.1 y lejos del APK Accessibility viejo. Documentar en settings: *“La biometría cubre lo que abrís desde Foco. No cubre notificaciones ni links.”*

---

## 9. Qué NO hacer

- Reusar código/UI Capacitor `foco-moto-v3` / Accessibility / VPN como base  
- NotificationListener en v0.1  
- Auto-agregar apps nuevas a whitelist  
- App drawer “todas las apps” (rompe el producto)  
- Backend, analytics cloud, cuenta, MELI  
- `QUERY_ALL_PACKAGES` de entrada  
- Foreground service / Accessibility “por si acaso”  
- Prometer bloqueo de intents indirectos en v0.1  
- Multi-module Gradle / Clean Architecture ceremonial en semana 1  
- Play Store / monetización  

---

## 10. Criterio de éxito arquitectura (para Nico)

Mateo/Nico puede buildear semana 1 **sin inventar infra**: este doc = techo.  
Agus entiende: bio = taps Foco; notificaciones/links siguen existiendo; kill personal 30 días manda.

**Siguiente:** FORGE despacha a Nico · Android Launcher con este techo; cero código de Andrés.
