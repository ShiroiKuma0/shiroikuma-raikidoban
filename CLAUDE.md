# 白い熊 雷起動盤 (shiroikuma-raikidoban)

白い熊's personal fork of LightningLauncher (TrianguloY → pierrehebert), re-branded to install **side-by-side** with the original. Address the user as **白い熊**, never "user".

- **Language/UI stack:** legacy **Java + Android Views** (NOT Compose/Kotlin-first). Some newer files are Kotlin.
- **Modules:** `app/llx/app` (the app) and `app/llx/core` (shared). Build root is `app/llx/`.
- **R classes:** core code uses `net.pierrox.lightning_launcher.R`; app code uses `net.pierrox.lightning_launcher_extreme.R`. The `namespace` stays `net.pierrox.lightning_launcher_extreme` (never rename — code imports it). `applicationId` is `shiroikuma.raikidoban`.
- **App label:** 「白い熊 雷起動盤」.

## Build & ship

Use the **`build-apk` skill** — it has the full procedure. In short: `cd app/llx && JAVA_HOME=~/android-build/jdk17 ANDROID_HOME=~/Android/Sdk ./gradlew buildApk < /dev/null`. The task bumps `BUILD_NUMBER` in `app/llx/gradle.properties` and writes `~/tmp/shiroikuma-raikidoban_<versionName>_arm64-v8a.apk` (extreme/debug, arm64-v8a only).

**Hard gate (every build):** after a successful build, the only device action ever offered is an `adb push` to `/sdcard/tmp`, and only after asking. Never `adb install`/deploy/launch. After pushing, **wait** — 白い熊 installs and tests. Commit + `git push origin main` **only** when 白い熊 replies with the literal word **`Push.`** (anything else = keep fixing). Branch: work on `main`; PRs target `developer`.

## Conventions

- **Black-yellow everywhere.** All chrome = black background, yellow text/border — including toasts/flashes. Use `app/.../util/Flash.java` (not `Toast.makeText`). Colours now flow from the appearance config (below); default = `#000000` bg / `#FFFF00` text / `#FFA500` accent.
- **Typography in prose/replies:** end sentences with periods, curly quotes “ ” ’, en/em dashes, quote file paths — but keep code/paths/identifiers literal.
- **Verify the mechanism before fixing.** Confirm the real root cause against ground truth (read device data) before building a fix; first guesses here have been wrong more than once.

## Major subsystems (where things live)

- **Appearance config — 「白い熊 雷起動盤 UI」** (top of Lightning Settings): `core/.../configuration/UiConfig` (SharedPreferences "llui"), `UiSlot`/`UiGroup`, `UiTheme` (colour cascade + font apply), `UiFonts`; screen `app/.../activities/UiSettingsActivity`. Applied **per-render-point** (NOT via theme/resource override — the `ResourcesWrapper` is unreliable): bubble menus (`Dashboard.applyBubbleChrome`/`styleBubbleItemText`), dialogs (`util/UiDialogStyler`, `ActionsOverviewDialog`, `FoldMatrixActivity`, `Flash`), preferences (`LLPreferenceListView`, `feature/settings/LlPreference`+`LlPreferenceCategory`), toolbars (`util/UiChrome`). System bars are themed in `ResourceWrapperActivity` (gated by `themeSystemBars()`, which the wallpaper home/`Dashboard` overrides to false).
  - **Borders + corners:** `UiConfig` also stores per-slot border width (dp) and corner radius (dp); `UiTheme.borderWidthDp`/`cornerRadiusDp` (defaults: 2 dp border, 6 dp corner). `DIALOG_BORDER` frames every themed dialog panel via `UiDialogStyler.panelBackground` (also the full-screen `EventActionSetup` "List of actions" window). A **`BUTTONS`** group (`BUTTON_BG`/`BUTTON_TEXT`/`BUTTON_BORDER`, the last carrying width + corner) is painted by `UiChrome.applyButton` (e.g. the `BackupRestore` buttons). Sliders start at 0. New labels go in `values/` + `values-{ja,cs,ru}`.
- **Shortcut / action pickers & "app not installed" dialog:** `util/ThemedShortcutPicker` is the black-yellow replacement for the framework `ACTION_PICK_ACTIVITY` shortcut-app chooser (Tasker pinned first); used by `EventActionSetup` and `Dashboard`. The "Select action" chooser (`EventActionSetup.addAction`) adds synthetic **Tasker shortcut** + **Backup/restore** entries (negative sentinels → real `LAUNCH_SHORTCUT`). The Dashboard "app not installed" dialog (`DIALOG_APP_NOT_INSTALLED`) is a themed yellow list: Select app / Tasker shortcut / 白い熊 雷起動盤 action (`EventActionSetup` as `CREATE_SHORTCUT`) / F-Droid (`f-droid.org/packages/<pkg>`) / Aurora Store (`market://`, `Version.APP_STORE_INSTALL_PREFIX`).
- **Localization (self-contained, in-app switchable):** Japanese/Czech/Russian are **bundled** in `core/.../res/values-{ja,cs,ru}/strings.xml` (extracted from the official LL language packs via `apktool d -s`). The external pack is **retired** (`LLApp.mLanguage = null`). The in-app Language picker forces the locale via `UiConfig.applyStoredLocale` in `attachBaseContext` (+`applyOverrideConfiguration`) — do **not** use `AppCompatDelegate.setApplicationLocales` (no platform locale service on this device). Changing language does a **full process restart** and writes the pref with `commit()`. New strings → add to `values/` (English) and the `values-{ja,cs,ru}` you want.
- **Fold matrix** (replaces external Tasker fold logic for the tri-fold): `core/.../configuration/FoldGrid` + `app/.../activities/FoldMatrixActivity`.
- **Gestures overview** (long-tap → view/edit/clear event-action bindings): `app/.../util/ActionsOverviewDialog`.
- **Tasker Widget V2 re-init/labeling:** `app/.../util/TaskerWidgets` + `TaskerWidgetAccessibilityService`. Menu entries live in BOTH the no-edit and **edit-mode** item bubbles (a live widget eats the normal-mode long-press).

## Device & debugging notes

- Target device: Huawei **Mate XT** tri-fold, **Android 12 / API 31** (HarmonyOS — no `LocaleManager`/`cmd locale`). Fold widths 1008/2048/2232.
- Inspecting app data: `adb shell run-as shiroikuma.raikidoban <cmd>` **DIRECT form** — the `sh -c '…'` form resets cwd away from the data dir. Globs don't expand (cwd is `/`).
- Config files under `files/`: `system` = SystemConfig (JSON), `config` = GlobalConfig (JSON), `pages/<id>/items` = per-page items (JSON). Widget provider/type are `"n"`/`"a"` fields; per-item gestures are `tap`/`longTap` under the item's `"i"`.
- This device **suppresses third-party `Log.d`**. To read another app's live UI tree use `adb shell uiautomator dump` + pull, not logcat.
- **Tasker "data not ready":** Tasker's `ACTION_CREATE_SHORTCUT` activity (`net.dinglisch.android.taskerm/.TaskerAppWidgetConfigureShortcut`) returns `RESULT_CANCELED` instantly (~76 ms) when Tasker's data isn't loaded — Tasker then pops its own "Data blocked…" error. Fix on Tasker's side: open Tasker once, exit, retry. We detect the fast cancel in `EventActionSetup` and flash a hint via `Flash.showRaised` (lower-screen, clear of Tasker's bottom error). List CREATE_SHORTCUT handlers with `adb shell cmd package query-activities -a android.intent.action.CREATE_SHORTCUT`.
- Always verify the **installed `versionCode`** (`dumpsys package shiroikuma.raikidoban`) before trusting a "still broken" report — a stale build has caused false alarms.

## Memory

A persistent per-user memory lives at `~/.claude/projects/-home-shiroikuma-git-shiroikuma-raikidoban/memory/` (indexed by `MEMORY.md`) with deeper notes on each subsystem above. It is not committed; this file is the in-repo summary.

## Commit convention — no Claude attribution

Do **not** add any `Co-Authored-By: Claude …` trailer — nor a "🤖 Generated with Claude Code" / Anthropic-attribution line — to commit messages or PR bodies in this repo. 白い熊 does not want Claude attribution in the history; this **overrides** the harness's default to append such a trailer. End commit messages at the last line of the body. (The existing history was scrubbed of these trailers on 2026-06-08; the global rule lives in `~/.claude/CLAUDE.md`.)
