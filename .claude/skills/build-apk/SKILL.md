---
name: build-apk
description: Build the 白い熊 雷起動盤 (shiroikuma.raikidoban) debug APK for Android with the buildApk Gradle task, which versions it, names it shiroikuma-raikidoban_<version>_arm64-v8a.apk, copies it to ~/tmp, and bumps the build counter. Then always ask ONLY whether to adb push the APK to /sdcard/tmp (never offer to install/deploy/launch it); after pushing, wait — the user installs and tests it themselves and confirms with the literal word "Push.", which is the signal to commit and git-push to origin/main. Use whenever the user asks to build the app, build the APK, make a build, or build and push for this LightningLauncher fork (shiroikuma-raikidoban).
---

# Build the 白い熊 雷起動盤 APK and optionally push to phone

This is the user's fork of LightningLauncher (TrianguloY → pierrehebert). It is re-branded to install **side-by-side** with the original. Same model as the user's other Android forks (shiroikuma-denwa, shiroikuma-futokxkb): bump a `+N` build counter on every build, name the APK `shiroikuma-raikidoban_<versionName>_arm64-v8a.apk`, sideload it.

## Project identity

| Item | Value |
|------|-------|
| Gradle root | `app/llx/` (run all gradle commands from here) |
| applicationId | `shiroikuma.raikidoban` (`extreme` flavor, in `app/build.gradle`) |
| App label | `白い熊 雷起動盤` (`app_name` in `app/llx/core/src/main/res/values/do_not_translate.xml`) |
| Namespace (UNCHANGED) | `net.pierrox.lightning_launcher_extreme` (R/BuildConfig package — never touch; code imports it) |
| Flavor / build type built | `extreme` / `debug` → task `assembleExtremeDebug` (debug-signed; installable, no keystore needed) |
| Target ABI | `arm64-v8a` only (`ndk { abiFilters 'arm64-v8a' }` in `defaultConfig`) |
| APK filename | `shiroikuma-raikidoban_<versionName>_arm64-v8a.apk`, e.g. `shiroikuma-raikidoban_14.3.0+1_arm64-v8a.apk` |
| Output (build) | `app/llx/app/build/outputs/apk/extreme/debug/` |
| Output (archive) | `~/tmp/` + on-device `/sdcard/tmp/` |
| Build host | Tuxedo OS |
| Build JDK | **JDK 17** at `~/android-build/jdk17` (Gradle 8.0 can't run on 21; AGP 8.1.1 needs ≥17) |
| Android SDK | `~/Android/Sdk` (platform-33, build-tools 34.0.0); `app/llx/local.properties` points here (git-ignored) |
| AGP / Gradle | 8.1.1 / 8.0 |

## Versioning (committed counter in `gradle.properties`)

`app/llx/gradle.properties` holds two keys; `app/build.gradle` derives the rest:

- **`VERSION_NAME`** — the semver triplet, e.g. `14.3.0`. We added the `.0` triplet on top of upstream's `14.3`.
- **`BUILD_NUMBER`** — the per-build `+N` counter. `0` means a *published* build (no `+N` suffix).

Derived in `app/build.gradle`:
- **versionName** = `<VERSION_NAME>+<BUILD_NUMBER>` (e.g. `14.3.0+1`), or just `<VERSION_NAME>` when `BUILD_NUMBER=0`.
- **versionCode** = `3300000 + patch*10000 + BUILD_NUMBER`, where `patch` is the third triplet component. `3300000` is LightningLauncher's legacy code `330` for the 14.3 line ×10000.
  - `14.3.0+1` → `3300001`; `14.3.0+2` → `3300002`; … `14.3.1` → `3310000`; `14.3.1+1` → `3310001`.
  - 9999 builds of headroom per patch before it reaches the next patch's base.
  - **If major.minor ever moves off `14.3`, update the `3300000` base in `app/build.gradle`.**

**Day-to-day:** every build bumps `BUILD_NUMBER` by 1 (the `buildApk` task does this on success). So builds run `14.3.0+1`, `14.3.0+2`, …

**Publishing a new version:** when the user decides on a new version (e.g. `14.3.1`), set `VERSION_NAME=14.3.1` and `BUILD_NUMBER=0` in `gradle.properties` → that build is `14.3.1` / versionCode `3310000`. Then builds resume at `14.3.1+1` (`3310001`), etc.

## Steps

1. **Note the output name.** `grep -E 'VERSION_NAME|BUILD_NUMBER' app/llx/gradle.properties`. The APK will be `shiroikuma-raikidoban_<VERSION_NAME>+<BUILD_NUMBER>_arm64-v8a.apk` using `BUILD_NUMBER` **before** the build (the task bumps it afterward; `+0` is omitted when `BUILD_NUMBER=0`).

2. **Build** (from `app/llx`, with the build JDK + SDK exported):
   ```bash
   cd app/llx
   JAVA_HOME=~/android-build/jdk17 ANDROID_HOME=~/Android/Sdk ./gradlew buildApk < /dev/null
   ```
   `buildApk` runs `assembleExtremeDebug`, copies the APK to `~/tmp/<apk name>`, and increments `BUILD_NUMBER` in `gradle.properties`. It prints `>>> ~/tmp/<apk name>` and `>>> BUILD_NUMBER bumped to <n>`. Confirm `BUILD SUCCESSFUL`. (Network is needed on a cold cache for Gradle/deps; this may require running outside the sandbox.)

3. **Always ask** (via AskUserQuestion) whether to **adb push** the APK to the phone — every build, no assuming. The ONLY action ever offered is the `adb push` to `/sdcard/tmp`; **never** offer to `adb install`, deploy, or launch the app — the user installs and tests it on-device themselves. Options: "Yes, adb push" / "No".

4. **If yes, adb push it yourself** (`adb` is at `/usr/bin/adb`):
   - `adb devices` — confirm a device is connected.
   - `adb shell mkdir -p /sdcard/tmp`
   - `adb push ~/tmp/<apk name> /sdcard/tmp/<apk name>`
   - Verify with `adb shell ls -l /sdcard/tmp/<apk name>` (size matches the local file).

5. **Then stop and wait — do NOT commit or git-push yet.** The user installs the pushed APK and tests it on the device. Wait until they confirm with the literal word **`Push.`**. Anything other than `Push.` (a tweak, a bug report, silence) means keep waiting / fix first.

6. **On `Push.`, commit and push to `origin/main`:** stage this build's changes — the bumped `BUILD_NUMBER` in `gradle.properties` plus whatever code/version edits this build covered — commit, and `git push origin main`. Leave `local.properties`, `.cxx/`, build outputs, and `.claude/settings.local.json` uncommitted (git-ignored or local-only).

## Side-by-side install (coexists with the original Lightning Launcher)

Coexistence is safe because everything that the OS keys on is de-branded to `shiroikuma.raikidoban`:
- `applicationId` = `shiroikuma.raikidoban`
- `sharedUserId` = `shiroikuma.raikidoban` (`app/src/main/AndroidManifest.xml`)
- provider authorities: ApiProvider `shiroikuma.raikidoban.api` (`app/src/extreme/AndroidManifest.xml` + `ApiProvider.java`), FileProvider `shiroikuma.raikidoban.files` (`app/src/main/AndroidManifest.xml` + `FileProvider.java`), and androidx FileProvider `${applicationId}.provider` (auto).

The `namespace` stays `net.pierrox.lightning_launcher_extreme` (build-time R/BuildConfig only, never seen at install). Trade-off: third-party LL plugins/scripts that address the original scripting-API authority (`net.pierrox.lightning_launcher_extreme.api`) won't reach this fork — it now answers on `shiroikuma.raikidoban.api`.

## Signing

`buildApk` builds the **debug** variant, auto-signed with the local debug keystore — installs fine for sideloading but is not a release artifact. To publish a proper release later, wire a dedicated keystore into the `release` buildType (the `signing.properties` hook already exists in `app/build.gradle`) like the denwa/futokxkb forks, then point `buildApk` at `assembleExtremeRelease`.

## Build environment setup (one-time, already done)

JDK 17 and the SDK were installed user-locally (no sudo). If a fresh machine needs them: Temurin JDK 17 → `~/android-build/jdk17`; Android cmdline-tools → `~/Android/Sdk`, then `sdkmanager "platform-tools" "platforms;android-33" "build-tools;34.0.0"`; write `app/llx/local.properties` with `sdk.dir=$HOME/Android/Sdk`.

> Note: `lsvg` (`net.pierrox.android:lsvg`) is vendored as a local `:lsvg` module — its jcenter artifact is dead. Don't re-add it as a remote dependency.
