# dist/

Entrega actual (NLS 0.2.0): `foco-launcher-v0.2-nls.apk`

SHA256:

```
69e911be5b9174613777f93f103f886d489784f8cff3d52266b6803d88c5e821
```

## Download from the cloud agent Artifacts panel

1. Open the agent run: [cursor.com/agents/bc-fbe12aee-5f70-57d1-bfb2-e8398ea93c2e](https://cursor.com/agents/bc-fbe12aee-5f70-57d1-bfb2-e8398ea93c2e)
2. Open **Artifacts**.
3. Download `foco-launcher-v0.2-nls.apk`  
   Direct path: [artifacts?path=/opt/cursor/artifacts/foco-launcher-v0.2-nls.apk](https://cursor.com/agents/bc-fbe12aee-5f70-57d1-bfb2-e8398ea93c2e/artifacts?path=%2Fopt%2Fcursor%2Fartifacts%2Ffoco-launcher-v0.2-nls.apk)
4. Verify:

```bash
sha256sum foco-launcher-v0.2-nls.apk
# expect 69e911be5b9174613777f93f103f886d489784f8cff3d52266b6803d88c5e821
```

If the Artifacts UI hits an auth wall from another machine, copy the file locally from a session that is already logged into Cursor, then drop it into the shared box.

## Rebuild instead of downloading

```bash
export ANDROID_HOME="$HOME/android-sdk"
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk dist/foco-launcher-v0.2-nls.apk
```

A rebuild on another machine produces a **different** debug signature (local debug.keystore) and a different SHA256. For the hash above, use the artifact from this run.

## Source

Origin (source of truth until a public GitHub URL is pasted):  
https://origin.cursor.com/git/agust-n-ragazzini/tmp-2c3ab32c79db0e54
