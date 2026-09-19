# Semana 2a NLS — qué se despachó vs arch §0

Techo: `docs/foco-launcher-arch-nls-v0.md` §0 / §9. Notes: `docs/foco-launcher-android-nls-notes-v0.md`. UX: `docs/foco-launcher-ux-nls-onboarding-v0.md`.

APK: `foco-launcher-v0.2-nls.apk` · versionName **0.2.0** · versionCode **2**

SHA256:

```
7457448c07da8dbc534b9836332070e6072e424cb81837172064f0d8d6747dad
```

| Arch §0 | Estado |
|---------|--------|
| Work profile: no administrar / no romper | Hecho — cero DPM / Device Admin / disable work |
| Allowlist notifs ≠ whitelist home (dos stores) | Hecho — `foco_launcher_prefs` vs `foco_notification_allowlist` |
| Work: TODAS las notifs PASS | Hecho — `UserProfileHelper.isWorkOrOtherProfile(sbn.user)` primera rama |
| Personal: cancel si package ∉ notificationAllowlist | Hecho — `NotificationPolicy.shouldSuppress` |
| UI Avisos editable; seed 1× opcional desde home | Hecho — al prender el filtro + default ON al agregar a home |
| `nlsFilterEnabled`; no gatea ROLE_HOME | Hecho |
| CALL / ALARM / NAVIGATION / MediaStyle / OEM panel | Hecho — nunca cancel |
| Sin inbox / Accessibility / QUERY_ALL_PACKAGES | Hecho |
| Bio | **OUT** (s2b HOLD) — `BiometricGate.ENABLED_IN_LAUNCH_PATH = false` |
| Copy honesto work + permiso NLS | Hecho — Camila ES-AR |

Detección work: `sbn.user` vs `Process.myUserHandle()`. Package name **no** decide work vs personal.
