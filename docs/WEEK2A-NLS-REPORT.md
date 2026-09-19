# Semana 2a NLS — qué se despachó vs arch §0 + Nico review

Techo: `docs/foco-launcher-arch-nls-v0.md` §0 / §9. Notes: `docs/foco-launcher-android-nls-notes-v0.md`. UX: `docs/foco-launcher-ux-nls-onboarding-v0.md`.

APK: `foco-launcher-v0.2-nls.apk` · versionName **0.2.0** · versionCode **2**

SHA256:

```
69e911be5b9174613777f93f103f886d489784f8cff3d52266b6803d88c5e821
```

## Nico review (GO 2a)

| # | Criterio | Estado |
|---|----------|--------|
| 1 | Work `UserHandle` (`sbn.user != Process.myUserHandle()`) → **nunca cancel** | Hecho — primera rama en `NotificationPolicy.shouldSuppress` |
| 2 | Personal ∉ `notificationAllowlist` → cancel | Hecho — `notificationAllowlist = entries.filter { allowNotif }` |
| 3 | Preserve ALWAYS: CALL + ALARM + **NAVIGATION** + MediaStyle (+ system/OEM) | Hecho — también TRANSPORT / dialer / `EXTRA_MEDIA_SESSION` |
| 4 | NLS post-setup + toggle; **no** gatea HOME / default launcher | Hecho — setup no pide NLS; `LauncherActivity` HOME independiente |
| 5 | Allowlist vacía + filtro ON = cancel personal (salvo preserve) | Hecho — `package !in emptySet()` |
| 6 | Camila onboarding/settings UX | Hecho — 1 pantalla + CTA Ajustes + Ahora no; master + lista **solo home personal** + card work; footer |
| 7 | No DPM / Accessibility / inbox | Hecho — cero Device Admin, cero A11y, `onNotificationRemoved` no-op |

Detección work: `sbn.user` vs `Process.myUserHandle()`. Package name **no** decide work vs personal. `hasWorkProfile` es solo UI (card Camila); cero toggles work.

Modelo DataStore (Nico / FORGE): `allowNotif` en `WhitelistEntry` (default true al agregar a home). `nlsFilterEnabled` en `LauncherPrefs`. `notificationAllowlist` es **derivado**, no un set paralelo editable con apps fuera del home.

| Arch §0 | Estado |
|---------|--------|
| Work profile: no administrar / no romper | Hecho |
| Allowlist notifs ≠ whitelist home (conceptual) | Hecho — mismo entry, `allowNotif=false` ⇒ icono sí, aviso no |
| Work: TODAS las notifs PASS | Hecho |
| Personal: cancel si package ∉ notificationAllowlist | Hecho |
| UI Avisos = home personal; default ON | Hecho |
| `nlsFilterEnabled`; no gatea ROLE_HOME | Hecho |
| CALL / ALARM / NAVIGATION / MediaStyle / OEM panel | Hecho |
| Sin inbox / Accessibility / QUERY_ALL_PACKAGES | Hecho |
| Bio | **OUT** (s2b HOLD) — `BiometricGate.ENABLED_IN_LAUNCH_PATH = false` |
| Copy honesto work + permiso NLS | Hecho — Camila ES-AR |

Feature no se presenta como activa sin grant (`nls_inactive` + CTA). Skip no rompe el launcher.
