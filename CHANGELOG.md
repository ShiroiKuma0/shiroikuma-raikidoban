# Changelog

Release history of **白い熊 雷起動盤**, 白い熊's fork of
[Lightning Launcher](https://github.com/pierrehebert/LightningLauncher). Upstream keeps no changelog
file, so this one carries the fork's history alone; should upstream ever add theirs, it goes **below**
this block and is never edited or reordered.

Entries are per-release deltas — only the first release, `14.3.1`, lists everything the fork had
accumulated up to that point. Every release is built on the TrianguloY `developer` line of upstream
Lightning Launcher eXtreme 14.3.

---

## 白い熊 雷起動盤 14.3.7 — 2026-08-10

A fresh install had almost none of the fork's look — white bubble menus, a white hint box, grey body
text, an orange status bar and an orange all-apps action bar.

- **Black-and-yellow from the first launch.** The whole palette lives in `values-night`, and
  `ResourceWrapperActivity` chose night vs. light from `SystemConfig.appStyle`, whose default was
  `LIGHT` — a configured install had `DARK` on disk and looked right, a fresh one fell into stock's
  light chrome. Night mode is now unconditional: `AppCompatDelegate.MODE_NIGHT_YES` in
  `attachBaseContext`, plus `UI_MODE_NIGHT_YES` forced into the base `Configuration` by
  `UiConfig.applyUiConfiguration` (renamed from `applyStoredLocale`) for activities and `LLApp` alike.
- **The "Application style: Light/Dark" preference is removed**, and the `AppDialog` /
  `AppLightNoActionBar` styles move off hard-coded `Theme.AppCompat.Light.*` to `DayNight`.
- **The accent becomes `#FFFF00`** — `UiSlot.ACCENT`, `@color/color_primary`, `@color/color_accent`.
  The fresh-install user-menu folder, which was painted in the accent itself, goes black with the
  accent on its top rule.
- **The edit-mode toolbar turns translucent yellow**, keeping its black button text.
- **The all-apps action bar follows the `TOOLBARS` slots** (`TOOLBAR_BG` / `TOOLBAR_TEXT` /
  `TOOLBAR_ICON`) instead of the stock deep orange; the shared `ab_bg` fallback is black.
- **Hint cells** take `MENU_ACCENT` and `MENU_TEXT` instead of the platform greys.
- **A fresh install hides the status bar** on the desktop and app drawer, black rather than orange when
  shown; `PageConfig.statusBarHide` stays `false` so existing desktops are untouched.
- **Android 15's compatibility dialog is cleared**: the APK is no longer debuggable (use
  `-PdebuggableApk` when `adb shell run-as` is needed), and `libll.so` links with
  `-Wl,-z,max-page-size=16384` so its `LOAD` segments are 16 KB aligned.

## 白い熊 雷起動盤 14.3.6 — 2026-08-01

- **Every dialog wears the yellow frame.** `UiDialogStyler` moved into the core module and became the
  only way the app shows a dialog: `styleOnShow` / `show` / `stylePanel` replaced hand-rolled
  `OnShowListener`s at ~40 framework dialogs across 23 files that had silently stayed unstyled.
- `style()` takes a `Dialog` and resolves its views by id, so AppCompat's `AlertDialog` is themed too.
- The fork's own `AlertDialog` subclasses style themselves in `onStart()`.
- The last six framework `ProgressDialog`s became `ThemedProgressDialog`, which gained `create()` and
  `setMessage()`.
- The build counter is zero-padded to three digits, so builds sort in order.

## 白い熊 雷起動盤 14.3.5 — 2026-07-31

- **A stopped 自由作業盤 is visible, not silent.** `JiyuTaskTarget.isJiyuStopped` probes the sister app's
  ordered `QUERY_STATUS` broadcast; while it is stopped, task shortcuts are drawn faded (35 % of the
  configured alpha, via a new `ItemView.DimPredicate`) and a tap explains 「白い熊 自由作業盤 は停止中」 and
  opens the app instead of firing into the void. No answer means "not stopped".
- The headless export **can be cancelled** — it unwinds at the next entry, deletes its half-written
  archive and leaves the backup folder as it found it — and **announces its own defaults**, so the
  caller no longer has to guess which categories start ticked.

## 白い熊 雷起動盤 14.3.4 — 2026-07-25

- **Export / Import — one zip, ticked by category.** A window at the top of the 「白い熊 雷起動盤 UI」 page
  backs up everything settable in the launcher into a single timestamped
  `shiroikuma-raikidoban_<yyyy-MM-dd_HH-mm-ss>.zip`: appearance and imported fonts, launcher settings,
  the fold matrix, desktops and items (item icons and desktop wallpapers as sub-options), scripts,
  item styles and script variables.
- Import is a **merge, never a wipe**; a restart is offered so restored pages are re-read.
- A **backup folder** chosen once through the system file picker and persisted across reboots, showing
  the newest backup's date — red until one exists.
- A `manifest.json` and data-dir-relative paths make the archive a plain, auditable copy.
- The same export runs **headlessly** on a token-gated intent, so 自由作業盤 can back the launcher up
  alongside every sister app. Neither the folder nor the token ever travels inside a backup.

## 白い熊 雷起動盤 14.3.3 — 2026-07-06

- **Gesture-binding repair.** A gesture bound to a dead `LAUNCH_APP` / `LAUNCH_SHORTCUT` intent used to
  open the generic "app not installed" dialog, which rewrote the *swiped icon's own tap intent* and
  left the gesture broken. The engine now reports the failing event-action binding itself
  (`Screen.onEventActionLaunchError`), and the dialog writes the replacement back into that exact
  binding — deep-cloning the action chain, since `modifyItemConfig()`'s copy-on-write is shallow.
- The dialog's F-Droid / Aurora Store entries point at the dead intent's package, not the item's.
- Bindings that cannot be located get an honest flash rather than a dialog that would repair the wrong
  thing.
- **自由作業盤 pull-widget recovery**: the pull-source template is stored on the item tag
  `rkb.jiyuWidgetTemplate`, so it rides along inside `.lla` backups.

## 白い熊 雷起動盤 14.3.2 — 2026-06-26

- **In-app rebrand.** Every user-visible *Lightning* / *Lightning Launcher* / *Lightning Launcher
  eXtreme* / 「ライトニング(ランチャー)」 mention, and the *LL* / *LLX* abbreviations, became
  **白い熊 雷起動盤** across English, 日本語, Čeština and Русский.
- Deliberately unchanged so nothing breaks or gets misattributed: code identifiers, package names, the
  `Lightning` scripting-API object, real upstream URLs, the `SET_WALLPAPER_HINTS` constant, the
  backup/template filename strings, the third-party "LL Direct Call" plugin name, and the historical
  translator changelog.

## 白い熊 雷起動盤 14.3.1 — 2026-06-25

First public release — everything built on top of stock Lightning Launcher up to that point.

- **「白い熊 雷起動盤 UI」**, a launcher-wide appearance config at the top of Lightning Settings: per-slot
  colours with a foundation cascade (FOUNDATION / MENUS / DIALOGS / SETTINGS / TOOLBARS / BUTTONS /
  GEOMETRY), a global chrome font with `.ttf` / `.otf` import and per-slot family / weight / size, and
  configurable border width + corner radius on the dialog, menu, button and geometry panels. Applied
  per render point, not through a resource override.
- **Black-and-yellow everywhere**: a yellow-on-black dark mode, every `Toast.makeText` re-routed through
  a themed `Flash` (app, scripting API and a script-scope `ScriptToast` shim), themed backup/restore
  progress, and a restyled launcher icon.
- **Fold-aware desktop matrix** for the tri-fold: native fold-width → desktop selection replacing the
  external scripting it needed before, laid out in a drag-and-drop grid of desktop miniatures.
- **Gestures overview**: long-tap any desktop or item to see every event-action binding in one themed
  list, with inherited actions marked, and view / edit / clear each from there.
- **Self-contained languages**: English, 日本語, Čeština and Русский bundled in-app, switchable from
  inside the app, with the external language pack retired.
- **白い熊 自由作業盤 integration**: task shortcuts from the item and action pickers, cross-app widget-name
  re-init that survives crashes and restores, 「白い熊 自由作業盤: …」 labelling, and app info / store /
  uninstall entries that resolve the app the *task* opens.
- **Backups that work on modern Android**: creating a backup works again under targetSdk 33 by writing
  through the system file picker, and a restore no longer wipes the system wallpaper.
- **Themed pickers and dialogs**: the shortcut-app picker, the "Select action" chooser, a redesigned
  "app not installed" dialog, a colour picker with recent-colour swatches, and a configurable geometry
  box with live preview.
