# dist/

The week-1 debug APK is **not** in git (17MB, no GitHub LFS). Build it, or download it from the cloud agent Artifacts panel and verify the hash.

## SHA256

```
27aa77968b7ecc46380fd94f1d5d89db13bbcf70eb482dc88c05654c4b4343e7
```

File name: `foco-launcher-v0.1-w1.apk`  
Package: `com.foco.launcher` · version `0.1.0-w1`  
Signed with the **local** Gradle debug keystore (`~/.android/debug.keystore`). No keystore is in this repo.

## Download from the cloud agent Artifacts panel

1. Open the week-1 agent run: [cursor.com/agents/bc-fbe12aee-5f70-57d1-bfb2-e8398ea93c2e](https://cursor.com/agents/bc-fbe12aee-5f70-57d1-bfb2-e8398ea93c2e)
2. Open **Artifacts** (panel / tab on that run).
3. Download `foco-launcher-v0.1-w1.apk`  
   Direct path on the run: [artifacts?path=/opt/cursor/artifacts/foco-launcher-v0.1-w1.apk](https://cursor.com/agents/bc-fbe12aee-5f70-57d1-bfb2-e8398ea93c2e/artifacts?path=%2Fopt%2Fcursor%2Fartifacts%2Ffoco-launcher-v0.1-w1.apk)
4. Also available: `.apk.b64` (one-line base64) and `.apk.tar.gz`.
5. Verify:

```bash
sha256sum foco-launcher-v0.1-w1.apk
# expect 27aa77968b7ecc46380fd94f1d5d89db13bbcf70eb482dc88c05654c4b4343e7
```

If the Artifacts UI hits an auth wall from another machine, copy the file locally from a session that is already logged into Cursor, then drop it into the shared box.

## Rebuild instead of downloading

```bash
export ANDROID_HOME="$HOME/android-sdk"
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk dist/foco-launcher-v0.1-w1.apk
```

A rebuild on another machine produces a **different** debug signature (local debug.keystore) and a different SHA256. For the hash above, use the artifact from this run.

## Source

Origin (source of truth until a public GitHub URL is pasted):  
https://origin.cursor.com/git/agust-n-ragazzini/tmp-2c3ab32c79db0e54
