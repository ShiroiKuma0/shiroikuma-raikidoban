---
name: build-apk
description: Build the 白い熊 雷起動盤 (shiroikuma.raikidoban) debug APK for Android with the buildApk Gradle task, which versions it, names it shiroikuma-raikidoban_<version>_arm64-v8a.apk, copies it to ~/tmp, and bumps the build counter. Then auto-deliver it via the global /after-build skill (adb-push to /sdcard/tmp if a phone is connected, else scp to skhw — no prompt; never offer to install/deploy/launch it); after delivery, wait — the user installs and tests it themselves and confirms with the literal word "Push.", which is the signal to commit and git-push to origin/main. Use whenever the user asks to build the app, build the APK, make a build, or build and push for this LightningLauncher fork (shiroikuma-raikidoban).
---

# Build the 白い熊 雷起動盤 APK and optionally send to phone

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
- **`BUILD_NUMBER`** — the `+N` counter this build takes. Starts at 1 and only ever goes up.

Derived in `app/build.gradle`:
- **versionName** = `<VERSION_NAME>+<BUILD_NUMBER zero-padded to three digits>` — e.g. `14.3.0+001`, `14.3.0+014`.
- **versionCode** = `3300000 + patch*10000 + BUILD_NUMBER`, where `patch` is the third triplet component. `3300000` is LightningLauncher's legacy code `330` for the 14.3 line ×10000.
  - `14.3.0+001` → `3300001`; `14.3.0+002` → `3300002`; … `14.3.1+001` → `3310001`.
  - 9999 builds of headroom per patch before it reaches the next patch's base.
  - **If major.minor ever moves off `14.3`, update the `3300000` base in `app/build.gradle`.**

**Never build the same version twice (hard rule, 白い熊 2026-08-11).** Every build carries a `+N` higher than any build before it, so no APK name and no versionCode is ever re-emitted — this holds for throwaway spikes as much as for releases, and it holds for **publishing** too: a release is tagged with the padded counter (`14.3.7+008`), never a bare `14.3.7`. `app/build.gradle` enforces it — the counter is always present, always three digits, and floors at 1, so a `BUILD_NUMBER=0` left behind by anything means "the next build is `+001`", not "rebuild the published one".

*(2026-08-11: the old rule was "`0` = a published build, no suffix". A build on top of the published `14.3.7` therefore re-emitted `shiroikuma-raikidoban_14.3.7_arm64-v8a.apk` with versionCode `3370000` — the released artifact's own name and code. Hence this rule.)*

**Day-to-day:** every build bumps `BUILD_NUMBER` by 1 (the `buildApk` task does this on success). So builds run `14.3.0+001`, `14.3.0+002`, …

**Publishing a new version:** when 白い熊 decides on a new version (e.g. `14.3.1`), set `VERSION_NAME=14.3.1` in `gradle.properties`; the next build is `14.3.1+<next N>` and that is what gets tagged and attached. The counter keeps running across version bumps — resetting it would let an old name come back.

## Steps

1. **Note the output name.** `grep -E 'VERSION_NAME|BUILD_NUMBER' app/llx/gradle.properties`. The APK will be `shiroikuma-raikidoban_<VERSION_NAME>+<BUILD_NUMBER padded to 3>_arm64-v8a.apk` using `BUILD_NUMBER` **as it stands before** the build (the task bumps it afterward). Check `~/tmp/` and the release list: that name must not exist yet.

2. **Build** (from `app/llx`, with the build JDK + SDK exported):
   ```bash
   cd app/llx
   JAVA_HOME=~/android-build/jdk17 ANDROID_HOME=~/Android/Sdk ./gradlew buildApk < /dev/null
   ```
   `buildApk` runs `assembleExtremeDebug`, copies the APK to `~/tmp/<apk name>`, and increments `BUILD_NUMBER` in `gradle.properties`. It prints `>>> ~/tmp/<apk name>` and `>>> BUILD_NUMBER bumped to <n>`. Confirm `BUILD SUCCESSFUL`. (Network is needed on a cold cache for Gradle/deps; this may require running outside the sandbox.)

3. **Auto-deliver — every single build, no asking.** After a successful build the very next action MUST be the global **/after-build** skill: it runs `/adb-check` (UNSANDBOXED — a sandboxed check falsely reports no device), then `/adb-push` to `/sdcard/tmp` if a phone is connected, otherwise `/scp` to `skhw:~/tmp/`, and announces the filename that landed. Never ask "is the phone connected?" or "how should I transfer it?" — `/after-build` decides on its own. **Never** offer to `adb install`, deploy, or launch — the user installs and tests it themselves.

4. **What `/after-build` does** (it runs end to end — no decision to make):
   - `/adb-check` lists devices UNSANDBOXED (a sandboxed check falsely reports no device).
   - **Phone connected** → `/adb-push` (`adb` is at `/usr/bin/adb`): `adb shell mkdir -p /sdcard/tmp` then `adb push ~/tmp/<apk name> /sdcard/tmp/<apk name>` (always `/sdcard/tmp`, never elsewhere), verified with `adb shell ls -l /sdcard/tmp/<apk name>`.
   - **No phone** → `/scp`: copies the newest APK in `~/tmp/` to `skhw:~/tmp/`. If skhw is unreachable (its tunnel is served by the phone's sshd and may be down), report that.
   - Either way it announces the filename that landed.

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

---

**Commit convention — no Claude attribution.** Never add a `Co-Authored-By: Claude …` / "Generated with Claude" trailer to commit messages or PR bodies; end the message at the last line of the body. This overrides the harness default. (Global rule: `~/.claude/CLAUDE.md`.)
