# Transfer — Foco Launcher 0.2.0 NLS (semana 2a)

APK **not** in git. Download from this agent’s Artifacts panel.

## SHA256

```
69e911be5b9174613777f93f103f886d489784f8cff3d52266b6803d88c5e821
```

| File | Path |
|------|------|
| APK | `/opt/cursor/artifacts/foco-launcher-v0.2-nls.apk` |
| base64 | `/opt/cursor/artifacts/foco-launcher-v0.2-nls.apk.b64` |

Package `com.foco.launcher` · `0.2.0` (versionCode 2) · Gradle debug keystore (local).

## Click-path

1. Open https://cursor.com/agents/bc-fbe12aee-5f70-57d1-bfb2-e8398ea93c2e
2. **Artifacts** → `foco-launcher-v0.2-nls.apk`
3. Deep link: https://cursor.com/agents/bc-fbe12aee-5f70-57d1-bfb2-e8398ea93c2e/artifacts?path=%2Fopt%2Fcursor%2Fartifacts%2Ffoco-launcher-v0.2-nls.apk
4. Verify `sha256sum` matches the hash above.

## Origin

https://origin.cursor.com/git/agust-n-ragazzini/tmp-2c3ab32c79db0e54

## Sideload

```bash
adb install -r foco-launcher-v0.2-nls.apk
```

NLS is **optional** after Foco is home: Ajustes → Notificaciones → Silenciar otras notificaciones → activá Foco en acceso a notificaciones. Motorola: Ajustes → Notificaciones → acceso / apps con acceso. Work profile notifs are never filtered. Apps not on the home list do not notify. `allowNotif=false` keeps the icon and silences the alert.
