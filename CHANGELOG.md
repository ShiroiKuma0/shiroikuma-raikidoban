# Changelog

Release history of **白い熊 雷起動盤**, 白い熊's fork of
[Lightning Launcher](https://github.com/pierrehebert/LightningLauncher). Upstream keeps no changelog
file, so this one carries the fork's history alone; should upstream ever add theirs, it goes **below**
this block and is never edited or reordered.

Entries are per-release deltas — only the first release, `14.3.1`, lists everything the fork had
accumulated up to that point. Every release is built on the TrianguloY `developer` line of upstream
Lightning Launcher eXtreme 14.3.

---

## 白い熊 雷起動盤 14.3.7+004 — 2026-09-05

The three hardening items named under **Known gaps** in `14.3.7+002`, from the revision of the
sister-app contract that landed after that build was made. All three sit in the data door — the
`ContentProvider` path 白い熊 応用管理 uses — and none of them touch the Export / Import window.

**Still signed with the release key introduced in `14.3.7+003`**, so if you are coming from
`14.3.7+002` or earlier the crossing is the same one-time uninstall: export through Export / Import
first, uninstall, install, import. Coming from `+002` you can skip `+003` entirely — it is one
uninstall, not two.

### One progress sender, not two

There were two implementations of the same §3 watchdog: one in `StateExportReceiver` for the
broadcast door, one inside `AutomationDataService` for the provider. The contract is explicit that an
app which already has a §1 sender **parameterises that one on the correlation-id extra rather than
writing a second**, because two implementations of the same watchdog drift, and the one that drifts
is always the one nobody is looking at.

So the second is gone. Both doors now use `AutomationProgress`, which is told which extras the id
belongs under: the broadcast door passes `reply_id`, the provider door passes **both** `job_id` and
`reply_id` carrying the same value, so one progress reader on the caller's side serves both. The
terminal reply carries both names for the same reason.

- **And with it, the heartbeat — the half that actually mattered.** §3 wants a message at least every
  30 s *even when the numbers have not moved*, because a caller presumes an app silent for two
  minutes to be dead and fails its slot. `RkbExport` only calls back when a whole category is
  finished, and one category here can be every item icon or every desktop wallpaper. A daemon thread
  now re-sends the last line every 20 s. **The broadcast export gains this too**, having never had it.
- The broadcast door also now sends `item` — the id of the category the count refers to — so the
  caller highlights the right row instead of guessing from a bare number.

### The descriptor has an owner from the moment it leaves the map

Taking the caller's descriptor out of the hand-over map and calling `startForeground` now happen
inside **one** `try`/`finally`. That call can be refused: a service started from a binder call is a
*background* start, and API 31+ can refuse it outright unless the app is exempt from battery
optimisation — which, on a phone where this app is not exempt, makes that the **ordinary** path
rather than an exotic one. Before, the throw left the caller's file open with no reply ever sent, so
the caller could neither checksum nor encrypt it and was told nothing about why.

### A large import is spooled to disk, not into memory

`RkbExport` gains file-based paths — `categoriesIn(File)` and `importZip(Context, File, Set)` —
behind a small internal source abstraction, so the Export / Import window's existing `byte[]` path is
untouched. The automation import now streams the descriptor into a cache file, validates it there,
applies it, and deletes the spool in a `finally`.

Reading the whole archive before writing anything is unchanged and deliberate: a partial read that
failed half way would import half an archive, and a half-restored launcher is worse than one that
refused. Only the bound moves, from RAM to disk — and this app's backup carries every item icon and
every desktop wallpaper, so it is precisely the archive that gets big enough for that to matter.

### Not covered

The import still has no per-category progress, because `importZip` has no callback to report
through. It sends one line and then heartbeats, which keeps the caller from giving up but moves no
progress bar.

## 白い熊 雷起動盤 14.3.7+003 — 2026-09-04

**This release changes the app's signing identity, so it cannot install over any earlier build.**
Every APK this repo had ever produced — `14.3.7+002` included — was signed with the Android **debug**
key. From here it is signed with its own release key.

> **Upgrading from `14.3.7+002` or earlier requires an uninstall, which takes the launcher's data.**
> Export through the Export / Import window first (白い熊 UI page), uninstall, install this build,
> then import. Widget *bindings* cannot survive regardless — the widget host dies with the app, so
> every `appWidgetId` is dead on reinstall. 自由作業盤 widgets are offered a **one-tap re-init** at
> startup that rebinds them and re-pushes their names and pull-templates; any other app's widgets
> have to be re-added by hand.

### Why it was debug-signed, and why that is the interesting part

The signing block in `app/llx/app/build.gradle` was guarded on a `signing.properties` **project
property** — which is set nowhere in this repo, and never has been. Both arms of the guard were
always false, so the block never ran and Gradle fell back to the debug certificate. The build then
reported success and wrote a file named exactly like a release, **indistinguishable from a properly
signed one by every check except `apksigner`**.

That shape is worse than an obviously wrong line, because the guard *reads* as defensive programming:
it checks that the property exists **and** that the file exists, so it looks like someone thought
about it. A guard that degrades silently buys the reviewer's confidence and spends it on nothing.

### What replaces it

- **The key data lives in a gitignored `app/llx/signing.properties`** (`sp.storeFile` / `sp.keyAlias` /
  `sp.storePassword` / `sp.keyPassword`); `-Psigning.properties=<path>` is still honoured as an
  override. The keystore itself is outside the repo, backed up with its password.
- **A missing key now FAILS the build**, with a message naming the keystore and where its password is
  recorded. The check is scoped to the task graph, so `clean` and `tasks` still work without it.
- **`buildApk` verifies the artefact it just wrote**, running `apksigner` over it and refusing to ship
  anything carrying `CN=Android Debug`; on success it prints the real signer. This is the check whose
  absence let the debug-signed build through, and it now runs on every build.
- **The `debug` build type carries the release key too**, deliberately — `buildApk` ships that build
  type, so signing only `release` would have left the one artefact actually installed as the one still
  signed with the debug certificate.

### The key

RSA 4096, valid 10 000 days, `CN=shiroikuma raikidoban, O=shiroikuma, C=JP`, alias `raikidoban` —
the same pattern as every sister app's. Its SHA-256 is
`e876d1d80d04905cb2a263b76618ded9749a50c0ef3e9f2dede1e15bd54b8129`; anything claiming to be
白い熊 雷起動盤 that does not present it is not this app.

*(No functional change to the launcher itself: the code is `14.3.7+002` plus the build change.)*

## 白い熊 雷起動盤 14.3.7+002 — 2026-09-04

The sister-app automation contract moves to **v2**, and the reason is the clean phone: 白い熊 応用管理
restores apps *and their data* onto a wiped device, where nothing has been configured and nobody has
pasted anything. A gate that only works once the phone is already set up is no gate for setting one
up. So the switch now ships on, the token becomes opt-in, and a second, identified door is opened for
the data itself.

### The gate — a switch that is ON, and a token that is OFF

- **`automation_enabled` now defaults to on**, and a new **`automation_require_token` defaults to
  off**. The master switch stays rather than being removed, because it is the only way to close this
  app off again, and a feature that can be turned on but never off is one that cannot be retreated
  from.
- **A token sent to this app while it is not asking for one is ignored, never refused.** Tokens live
  in task arguments and workspace variables that outlive the setting they were pasted for; refusing
  one would turn "a switch was turned off" into "half the batch mysteriously fails".
- **One gate, in one place**: `AutomationAuth.refuse()` returns either `null` or the exact `ERROR:`
  line, and every entry point — `EXPORT_STATE`, `LIST_CATEGORIES`, `CANCEL_EXPORT` and the new
  provider — calls it. Two checks written out at each entry point is how "automation disabled" and
  "bad token" drift apart. The constant-time compare stays for when the token *is* required.
- **The 白い熊 UI page gains 「認可トークンを使う？」** directly under the automation switch, and the
  token row is now drawn **only while that switch is on** — a 48-character secret sitting under an
  off switch invites pasting it somewhere it will do nothing. Both rows stay inside the
  Export / Import section rather than becoming a section of their own, because this is a backup
  feature. New strings in English, Japanese, Czech and Russian.

### The data door — a provider, a verified caller, and a file descriptor

A new `net.pierrox.lightning_launcher.automation` package, sitting **alongside** the existing
broadcast receiver rather than replacing it.

- **`AutomationProvider`** — an exported `ContentProvider` at `shiroikuma.raikidoban.automation`
  answering `describe` / `export` / `import` / `cancel` in the same `OK:` / `ERROR:` grammar the
  broadcast contract already uses, so a caller has one vocabulary rather than two. It exists because
  **a broadcast cannot tell you who sent it** — and since the caller supplies the destination an
  export is written into, "no idea who is asking" would mean any app on the phone could harvest the
  launcher's entire configuration. **Refusals are returned, never thrown**: an exception across a
  binder reaches the caller as a stack trace, which is useless to read and tells a misbehaving caller
  rather more than it should.
- **`AutomationCallers`** — the caller is checked three ways, each because the one before it is not
  enough: an **exact package name** from a two-entry map (never a prefix — package names are not a
  namespace anyone owns, so any sideloaded app can satisfy `shiroikuma.*`), the **uid the kernel
  reports** via `getPackagesForUid`, and a **pinned SHA-256 signing certificate**. The pin closes the
  real gap: whichever caller package is absent from the device is a name anyone can take, and a clean
  phone is precisely such a device. Includes the API 26–27 `GET_SIGNATURES` fallback, without which
  the door would refuse every caller on an older phone.
- **`AutomationDataService`** — a foreground service where the work actually runs, holding a
  **partial wakelock** because EMUI force-releases a backgrounded app's and the archive otherwise
  stops part-way with no crash, no ANR and no log. The payload moves through a **caller-supplied
  `ParcelFileDescriptor`** — not a path and not a URI, because the destination is renamed on commit,
  encrypted per known file and checksummed per known file, so a file dropped in by this app would be
  moved out from under it, left in plaintext inside an encrypted backup, and unverified. The
  descriptor is **duplicated before it leaves the provider call** (the original belongs to the binder
  transaction and closes when `call()` returns) and closed in a `finally`. Bytes are **counted as
  they are written** rather than stat'ed afterwards, since the destination may be a pipe.
- **`import` exists only behind the provider.** It never gets a broadcast action: an import
  overwrites the desktops, and the export receiver is exported with no permission, so an import there
  would let any app on the phone wipe the home screen.
- **`AutomationJobs`** — process-local cancellation flags, never persisted. A persisted "running"
  flag survives the crash that stranded it and wedges the app for good.

### Manifest and discovery

- **The `<queries>` element existed but named neither caller.** That is worse than it sounds: as well
  as `setPackage` on a reply broadcast failing **silently** on Android 11+, `getPackageInfo` and
  `getPackagesForUid` are themselves visibility-filtered, so an invisible caller fails the *identity*
  check as "signature unreadable". Both 白い熊 応用管理 and 白い熊 自由作業盤 are named now.
- **Three `shiroikuma.automation.*` `<meta-data>` entries** (contract 2, format 1, min\_format 1) let
  a caller read this app's capabilities **without waking it** — necessary because a frozen package
  cannot be asked anything. Note that `aapt2` stores `android:value="2"` as an **integer**: a caller
  must read these with `getInt`, and `getString` returns null.
- **`foregroundServiceType` is `dataSync`, not the contract's `specialUse`** — this module compiles
  against SDK 33 and `specialUse` is an API 34 literal that `aapt2` rejects outright.
- **`WAKE_LOCK`** added for the data service.

### Unchanged on purpose

The launcher's **outgoing** automation calls to 自由作業盤 (`SET_WIDGET_NAME`, `GET_WIDGET_BINDING`,
`GET_TASK_TARGET_PACKAGE`, `QUERY_STATUS`) are untouched. They carry no token at all — they are gated
the other way, by the `com.opentasker.permission.AUTOMATION` this app holds — so there was nothing to
strip, and an app whose owner switches the token requirement back on stays reachable.

## 白い熊 雷起動盤 14.3.7+001 — 2026-08-11

Every text the app never painted by hand was white — starting with the radio labels of the desktop's
**Screen orientation** picker, yellow ring around a white word.

- **One net for unpainted text: `UiTheme.paintUnstyledText(View, UiSlot)`.** Nothing in the fork
  overrides `android:textColorPrimary` (`values-night` sets `colorForeground` only), so every platform
  layout the code does not colour itself — `setItems` / `setSingleChoiceItems` rows,
  `simple_list_item_1`, `?textAppearanceLarge`, an `EditText` — drew white on the black chrome. The
  walk repaints every `TextView` still carrying a platform default (white or a light grey; deliberate
  colours are chromatic or black and are left alone), never touches typefaces — icon fonts, font
  previews and the monospace editor keep their faces — and hooks every `AdapterView` with a hierarchy
  listener, because list rows are built after a dialog is shown and again on each recycle.
- **Every dialog is covered at once**: `UiDialogStyler.style()` runs the walk over the whole dialog
  before the DIALOG_* slots, which still win on the views they own. So the orientation, language and
  font-weight pickers, the backup-action chooser, the import-backup picker, the fold-matrix menu, the
  script token list, "go to page", the variable picker, the file and font pickers, and the slider
  dialog's value, unit and ± buttons all turn yellow.
- **Surfaces that are not dialogs paint themselves**: the "List of actions" window (rows, summaries and
  the drag / delete glyphs — and it no longer shows its dialog bare), the Backup/restore archive list
  and its empty label, and the Shortcuts and Style-chooser lists.
- **Spinner dropdowns get `UiSpinnerAdapter`** — they live in their own window, out of the walk's
  reach: script editor, script picker (script and target), image picker (source, page, icon pack,
  package).
- **No version is ever built twice.** `BUILD_NUMBER=0` used to mean "a published build, no `+N`
  suffix", so the first build on top of the released `14.3.7` re-emitted that release's own APK name
  and its versionCode `3370000`. The counter is now always present, always zero-padded to three digits
  (`14.3.7+001`), and floors at 1 — a stray `0` means "the next build is `+001`" — and publishing tags
  the padded counter instead of a bare version.

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
