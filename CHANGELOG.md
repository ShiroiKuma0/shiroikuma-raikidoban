# Changelog

Release history of **白い熊 雷起動盤**, 白い熊's fork of
[Lightning Launcher](https://github.com/pierrehebert/LightningLauncher). Upstream keeps no changelog
file, so this one carries the fork's history alone; should upstream ever add theirs, it goes **below**
this block and is never edited or reordered.

Entries are per-release deltas — only the first release, `14.3.1`, lists everything the fork had
accumulated up to that point. Every release is built on the TrianguloY `developer` line of upstream
Lightning Launcher eXtreme 14.3.

---

## 白い熊 雷起動盤 14.3.7+023 — 2026-11-27

The backup-directory texts say **directory**, and two of them are no longer English-only.

**Still the release key from `14.3.7+003`**, so it installs over `+021`. Build `+022` was a test
build and was not released.

### Backup screen and Export / Import

- The texts about the backup directory say “directory” in English — none selected, no longer
  accessible, “choose one first”, “Not set — tap to choose a directory”, and the re-grant text —
  and the matching word in Japanese (ディレクトリ), Czech (adresář) and Russian (каталог), the
  Export / Import panel's directory texts included.
- “No backup directory selected” no longer says “Use the menu”: it points at the
  “Select backup directory” pill on the screen.
- The “none selected” and “no longer accessible” texts now have Japanese, Czech and Russian.

### Under the hood

- `BackupFolder` and `SafFolder` are renamed `BackupDirectory` and `SafDirectory`, together with
  their methods, constants and string names. The stored preference key keeps its value, so the
  chosen backup directory survives the update and older backups restore as before.

---

## 白い熊 雷起動盤 14.3.7+021 — 2026-11-27

The backup dialog gets a pill to **choose the backup directory**, and names the directory in use.

**Still the release key from `14.3.7+003`**, so it installs over `+020`.

### Backup screen

- A fifth regular pill in the top row, **“Select backup directory”** (「バックアップ先を選択」),
  opens the directory picker. Until now that entry existed only in the screen's options menu, and
  the screen has no toolbar to open a menu from — the directory could be chosen only through the
  prompt shown while none was set.
- Under the pills a dim-gold line with a directory icon names the backup directory in use
  (「バックアップ先: …」). It is hidden while none is set.
- The string now has Japanese, Czech and Russian, and the English one says “directory”.

---

## 白い熊 雷起動盤 14.3.7+020 — 2026-11-27

The **“Select an action”** chooser becomes a folding list with a chip index, and the **backup
screen** becomes a framed dialog with its list in a box of its own.

**Still the release key from `14.3.7+003`**, so it installs over `+016`. Builds `+017` to `+019` were
test builds and were not released.

### Select an action (「動作を選択」)

- The one long flat list is now **folding groups under a chip index**: a wrapping row of small
  chips on top, one per group, each with the group's icon; a tap on a chip unfolds its group and
  scrolls to it. The chips are not draggable here.
- “Nothing” and 「貼り付け」 stay above the groups; then the seven groups — launch and apps,
  navigation, menu and status bar, folders, edition, external, advanced — each with a bold,
  underlined heading, its icon, and › when folded or ⌄ when open.
- Every group starts folded. The fold state is remembered between openings, in the UI page's own
  preferences, so it travels in the export.
- **Every action has its own icon, coloured by what it does**, in place of one icon per group; the
  icons stand in one column at the dialog's edge and the labels share one edge. The empty gutter of
  the old preference-row layout is gone.
- A **Cancel** pill at the bottom of the dialog.
- The group names now have Japanese and Russian, and “Show app shortcuts” has Japanese, Russian and
  Czech.

### Backup screen

- The screen is an **almost-screen-size dialog**: a panel in the configurable dialog frame, with
  the dimmed launcher around it. A tap outside the panel, the Back key and a new **Close** pill
  close it.
- A heading, 「バックアップと復元」, with its icon.
- The four actions are **regular pills that hug their label**, in a wrapping row, each with a
  role-coloured icon inside and a › where it opens something. They use the regular text font at
  15 sp — no longer the Buttons font and size of the UI page, which made them fill half the screen
  and cut their labels. “Create a backup” is the thick, bold one; “Prune old backups” stays red.
- An explanation line under the pills, in dim gold with a left rule.
- The list of backups has its own heading with the count, and sits in a **framed box that scrolls
  by itself**; everything above it stays in place.
- Each backup is one row: date and time in bold, the file name beneath in dim gold, a › at the
  right, and a “newest” mark on the first. A file whose name carries no date stamp shows its name
  in bold and its date beneath.
- “Load an external file” now has Japanese, Czech and Russian.

### Colours

- The defaults of four menu-icon roles move to the values the black-yellow look settled on
  2026-11-23: connect **heavy green** `#00A550` (was pure green), message **heavy blue** `#0066FF`,
  contact **light blue** `#00B0FF` (was cyan), block **rose** `#FF3D7F` (was magenta). A colour set
  on the UI page is kept.

---

## 白い熊 雷起動盤 14.3.7+016 — 2026-11-20

The settings bubble gains a row that opens the **「白い熊 雷起動盤 UI」** page directly.

**Still the release key from `14.3.7+003`**, so it installs over `+015`.

### Menus

- The settings bubble — the one behind the sliders glyph, with the desktop's (or folder's, or app
  drawer's) settings, 「白い熊 雷起動盤」 and Android's settings — has a fourth row,
  **「白い熊 雷起動盤 UI」**, between the launcher's own settings and Android's. It opens the UI
  appearance page in one tap; before, that page was reachable only from the Lightning menu or from
  the top of the launcher's settings.
- The row is a full-width pill with the same palette icon as its twin in the Lightning menu.

---

## 白い熊 雷起動盤 14.3.7+015 — 2026-11-20

The whole app now wears the settled black-yellow shapes — **pills, outlined switches and icons
coloured by what a row does** — and the backup screen can **prune** old backups.

**Still the release key from `14.3.7+003`**, so it installs over `+014`. Not yet checked on the device.

### Prune old backups

- A fourth button on the backup screen, **“Prune old backups”** (「古いバックアップを削除」), deletes
  every backup in the backup directory **except the newest one**.
- Only full backups count: a template (an `.lla` archive with a manifest) and any other file in the
  directory are never touched, so the newest backup stays even when a template is newer.
- Nothing is deleted before a confirmation that gives the number to delete and names the backup that
  is kept; a flash then reports how many were deleted.

### Buttons

- Every button with a text label is an **outlined pill, never filled**: 3 dp and bold for the main
  action of its block (Backup, OK in the colour picker and the cropper, Export, Restart now),
  1.25 dp for every other, and red when it deletes (Prune, Delete script).
- Covered: the backup screen, fold matrix, image cropper and picker, script editor, colour picker,
  the Export / Import panel, the desktop picker's OK, the live-wallpaper placeholder and the buttons
  inside dialogs' own layouts.
- The 「ボタン」 group's border-width and corner sliders now only **override** the pill: they read
  “pill” until moved, and a long-press on the row gives the pill back.
- Left as they are, on purpose: icon-glyph tool buttons, the translucent yellow edit bars, the
  bubble's glyph strip and the geometry box.

### Dialogs

- The button bar of **every** dialog is pills — the positive button thick and bold, the others
  regular.
- Twelve dialogs that delete, reset or revoke show a **red** confirm pill: remove widget, clear
  gesture, reset page, delete desktop, delete archive, prune backups, delete script, delete style,
  delete font, delete file (both forms) and regenerate token.
- Lists of **actions** are full-width icon pills, with a › on rows that open something and red on
  rows that delete: the backup long-press list, the fold-matrix cell menu, “app not installed” and
  the 自由作業盤 paste chooser. Lists of pure choices (language, weight, fonts) stay plain.
- The colour picker has the bordered panel every other dialog already had.

### Switches

- An on/off setting is the **outlined switch**: a 2 dp round outline with a solid dot, right and
  accent when on, left and an opaque dim yellow when off — never filled, never a white ring.
- Covered: the UI page, every checkbox preference in Customize and the item editor, the script
  editor's options, the search and apply-template options and every “don't show again”.
- Selection marks stay checkboxes: the export categories, the desktop selection and the “custom”
  mark on Customize rows.

### Menus

- Every row of every bubble menu — the Lightning menu, item menus in and out of edit mode, the
  drawer menus, script and plugin rows — is a **full-width pill with its icon inside**, a › when it
  opens a submenu or a screen, and red for Delete and Uninstall.
- The icon is **coloured by what the row does**: green runs / launches / adds, blue edits, cyan is
  about an item or app, orange copies / moves / exports, magenta locks / hides / stops, red deletes,
  the accent views and configures.
- The ⋮ overflow of Lightning Settings and Customize follows the same look.
- A menu **no longer scrolls while the screen has room**: opened from an item low on the screen it
  slides up by what is missing instead.
- The rules under the menu title and the glyph strip are the clear 1 dp darker-accent divider.

### Lists

- Row icons, coloured by role, on: the “Select action” chooser (by category), the Gestures overview
  (its ✕ is red), the Lightning Settings root, every Customize and item-editor row (by kind: colour,
  number, choice, switch, text, gesture, binding, or a row that opens something), the backup
  archives, the style and shortcut lists and the move-to picker.

### 「白い熊 雷起動盤 UI」 page

- A thick **Lightning settings ›** pill at the top opens the launcher's own settings.
- A **chip index** under it: one small chip per section. A tap unfolds the section and scrolls to
  it; a long-press drags the chip, the chips reorder live under the finger and the sections follow
  on drop.
- **Every section folds** (› folded, ⌄ open) and starts folded. Section order and fold state are
  remembered and travel in the export.
- A new **Maintenance** section holds **Reset section order**.
- Action rows are pills that hug their label, with the explanation beneath in dim gold: Export /
  Import…, Copy token, Regenerate (red). Switch rows have a bold label, a dim-gold description and
  a thin dim-gold rule down their left edge. Every row has an icon; rows are only as tall as their
  content.
- New settable colours: in Foundation **Explanations (dim gold)**, **Switch, off** and **Delete /
  destructive**; in a new **Icon colours** section the five icon roles. The red of a missing backup
  directory now follows the delete colour instead of a fixed value.

### Packaging

- `shiroikuma-raikidoban_14.3.7+015_arm64-v8a.apk`, versionCode `3370015`, arm64-v8a, signed with the
  fork's release key.

---

## 白い熊 雷起動盤 14.3.7+014 — 2026-10-31

Two things: the launcher tasks that 白い熊 自由作業盤 creates can now be **pasted** instead of picked,
and the **desktop manager** shows the real wallpaper and lets every desktop's navigation be seen and
set in one place.

**Still the release key from `14.3.7+003`**, so it installs over `+008`.

### 「貼り付け」 — paste 自由作業盤 launcher tasks

- 自由作業盤's 「起動作業を作る」 (from its build `+111`) puts the tasks it just created on the
  clipboard. **「貼り付け」**, the new first item of the add dialog (BUILT-INS → Icon & Text, clipboard
  icon), adds them as shortcuts — no trip through 「白い熊 自由作業盤 ショートカット」 and the project
  tree.
- Each shortcut is **the same as a manual pick**: 自由作業盤 returns it directly for the task name, so
  icon, label and the “App info” target are all the task's own.
- **Several tasks** are added one after another, each one cell to the right of the touched spot; the
  last one is selected for editing.
- **In the action chooser** 「貼り付け」 sits right under 何もしない (wherever shortcuts are offered).
  A gesture runs one shortcut, so several pasted tasks are offered as a list to pick from.
- A task name that no longer exists is reported once, in a black-yellow flash, and leaves nothing
  half-added behind; a clipboard without a 自由作業盤 task list says
  「クリップボードに自由作業盤のタスクがありません」. Only a clip labelled as 自由作業盤's task list is
  ever read, and the clipboard is never cleared.

### Desktop manager: the live wallpaper shows through

- The screen used to paint `WallpaperManager.getDrawable()` as its background — a static image, never
  the **live wallpaper**. It now uses the same transparent window as the home screen, so whatever
  wallpaper is set, live included, moves underneath. The desktop pickers that use the same screen
  benefit too.

### Desktop manager: per-desktop and global navigation

- In edit mode, under every desktop: its **swipe left, swipe right, back and home** action. A desktop's
  own setting is bright (a target desktop is shown by name); one that falls back to the global action
  is dimmed and marked 「（全体）」.
- **Above the desktops**, a row of the same four **global** actions — 「全体のナビゲーション」 — the
  ones every desktop without its own setting uses.
- **Tapping any of them explains it in full**: the complete action with its detail (target desktop and
  position, shortcut, script…); for a global one, which desktops use it and which override it and with
  what; for a desktop, whether its own action runs or the global one.
- **And sets it**: 「デスクトップ X へ移動」 for every desktop (the current one ticked), 「その他の動作…」
  for the full action editor — any action at all — and, on a desktop, 「全体の設定を使う」 to hand it back
  to the global action. Changes are saved at once.
- The gesture name is set small and the action gets up to four lines, so long action names stay
  readable.

---

## 白い熊 雷起動盤 14.3.7+008 — 2026-10-11

Pulling the **Huawei Multi-Window dock** out from the screen edge and dragging an app into a split
crashed the launcher every time — 「白い熊 雷起動盤」が繰り返し停止しています — while the split itself
went through. With Huawei Home as launcher there was no crash.

**Still the release key from `14.3.7+003`**, so it installs over `+006`; from `14.3.7+002` or earlier
the crossing is the same one-time uninstall as before.

### Other apps' drags are ignored

- The dock's drag is **global**: the system delivers its start to every visible window, the
  launcher's desktop included. The desktop then tried to rewrite the drag's coordinates through two
  hidden `DragEvent` fields that Android blocks for this app, never found them, and used them anyway —
  a `NullPointerException` in `ItemLayout.dispatchDragEvent`.
- The desktop now handles **only its own drags** — an app shortcut dragged out of a long-press
  bubble. Any other drag passes straight through and is declined the moment it starts, so it can
  never be taken for a shortcut drop either (that would have crashed one step later).
- The hidden-field lookup is attempted **once**; where it is blocked, it is never touched again.
- The **fold matrix editor** accepted every drag as well, and a drop from another app would have
  moved a desktop using leftover state. Its own drags are now marked, and anything unmarked is
  ignored.

### Dragging an app shortcut onto the desktop works again

- On this phone the same blocked lookup meant that dragging an app shortcut out of a bubble onto
  the desktop was **already crashing the same way** — the feature was broken, not merely imprecise.
- Where the hidden fields are unavailable, the desktop now converts the drag position into page
  coordinates itself, for the drop and for the edge auto-scroll alike, so a shortcut lands where it
  is let go, on a zoomed or scrolled desktop too.

---

## 白い熊 雷起動盤 14.3.7+006 — 2026-09-07

Restoring onto a new phone landed on **"No backup folder selected"**, however complete the rest of
the restore had been. The cause was not subtle: the `settings` category shipped `config`, `system`,
`state` and `statistics` and nothing else, so the app's SharedPreferences — where the Backup/restore
folder is kept — were in no backup at all, and neither was the Export / Import directory.

**Still the release key from `14.3.7+003`**, so from `14.3.7+002` or earlier the crossing is the same
one-time uninstall — export through Export / Import, uninstall, install, import — and coming from
`+002` this is a single uninstall, not one per release.

### The app's own preferences now travel

- `settings` carries a top-level **`prefs.json`**: the app's default preferences and the
  export-directory preferences, written as the same type-tagged dump `ui.json` already used and keyed
  by logical name, so renaming a preferences file later cannot orphan an existing archive.
- Both backup folders are in it — the Backup/restore screen's and the Export / Import window's.
- Older archives, which have no such entry, import exactly as before; an older build simply skips one
  that has it.
- The automation token stays out of every backup, deliberately: a backup travels, and a secret
  should not.

### A folder is a Uri **plus** a grant, and only the Uri can travel

- A folder picked through the system file picker is two things: the Uri, which is ordinary app data,
  and the **persisted grant** behind it, which the platform issues to one install on one device and
  drops when that install goes away. No backup can carry the grant — not this app's, not a
  whole-data one — and nothing can ask for one covering a folder the picker never handed to this
  install.
- Importing therefore **re-takes the grant when the platform still holds one**, which makes a restore
  onto the same install seamless where it previously lost the folder outright.
- When the grant is gone, the folder is kept **only if nothing usable is configured on this phone**.
  A pointer carried from another device can never evict a folder that currently works — landing on a
  phone that already has one, the archive's dead folder would otherwise break the very next export.

### Remembered folders, instead of "not set"

- A folder restored without its grant is now **named on screen** with a short line saying access was
  lost with the install, rather than reported as though it had never been chosen — on the
  Backup/restore screen (whose message is now tappable), in the Export / Import window's folder box,
  and on the UI page's directory row.
- Every folder picker opens **at the remembered folder**, so restoring access is one tap in the
  folder already on screen instead of a walk back down the tree.
- New strings in English, Japanese, Czech and Russian.

## 白い熊 雷起動盤 14.3.7+005 — 2026-09-05

Closes the one item `14.3.7+004` left open: a restore driven through the data door could only send a
single line and then beat, because `importZip` had no callback to report through. However long it
ran, no progress row on the caller's side moved.

**Still the release key from `14.3.7+003`**, so from `14.3.7+002` or earlier the crossing is the same
one-time uninstall — export through Export / Import, uninstall, install, import — and coming from
`+002` this is a single uninstall, not one per release.

### Per-category progress while restoring

- **Real boundaries, not sampling.** The archive is written category by category, so the entry walk
  crosses each category exactly once; every crossing is a genuine boundary. Each reports the
  **position of the category being restored** — the contract's rule for a count of categories — and a
  final message with `current == total` closes the run out whatever the walk happened to end on.
- **The denominator is what will actually be restored**: asked for **and** carried by the archive, in
  the order the export wrote them, never the whole catalogue. That is the number the caller builds
  its row list from, so sending the catalogue size would leave rows that could never tick.
- **Still one progress sender.** The import only learns its category list after reading the archive,
  so `AutomationProgress` gained `setCategories()` rather than the import building a second sender
  for its second phase. It starts on the default list, says 「Reading the backup…」 while spooling —
  where it genuinely has no count of its own, and the heartbeat is what keeps the caller from giving
  up — then switches to real numbers once the restore set is known.
- New string in English, Japanese, Czech and Russian.

The Export / Import window's own import path is untouched: the three-argument `importZip` remains and
passes no progress.

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
