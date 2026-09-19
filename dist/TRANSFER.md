# Transfer — Foco Launcher week-1 APK

The 17MB debug APK is **not** in git. Pull it from this cloud agent’s Artifacts panel (a logged-in Cursor session), then copy it onto the shared box filesystem.

## SHA256

```
27aa77968b7ecc46380fd94f1d5d89db13bbcf70eb482dc88c05654c4b4343e7
```

| File | Path |
|------|------|
| APK | `/opt/cursor/artifacts/foco-launcher-v0.1-w1.apk` |
| base64 (one line) | `/opt/cursor/artifacts/foco-launcher-v0.1-w1.apk.b64` |
| tar.gz | `/opt/cursor/artifacts/foco-launcher-v0.1-w1.apk.tar.gz` |

Package `com.foco.launcher` · `0.1.0-w1` · Gradle **debug** keystore (local only).

## Exact click-path

1. Open the agent run:  
   https://cursor.com/agents/bc-fbe12aee-5f70-57d1-bfb2-e8398ea93c2e
2. Click **Artifacts**.
3. Click **`foco-launcher-v0.1-w1.apk`** and download it.  
   Deep link:  
   https://cursor.com/agents/bc-fbe12aee-5f70-57d1-bfb2-e8398ea93c2e/artifacts?path=%2Fopt%2Fcursor%2Fartifacts%2Ffoco-launcher-v0.1-w1.apk
4. Copy that file into the shared box (USB, `scp`, Files, etc.). The Artifacts UI itself cannot write through an auth wall into that box.
5. Verify:

```bash
sha256sum foco-launcher-v0.1-w1.apk
# 27aa77968b7ecc46380fd94f1d5d89db13bbcf70eb482dc88c05654c4b4343e7
```

## Origin URL (source of truth)

https://origin.cursor.com/git/agust-n-ragazzini/tmp-2c3ab32c79db0e54

GitHub public `foco-launcher`: not created from this agent (`gh` has no GitHub token here). When an empty public repo URL is pasted in chat, `main` can be pushed there. Until then, Origin is the source of truth.

## Sideload

```bash
adb install -r foco-launcher-v0.1-w1.apk
```

Then: open Foco → setup (≥1 app, Settings suggested) → Default apps → Home → Foco.  
Escape: `adb shell am start -a android.intent.action.VIEW -d foco://settings`
