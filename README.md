<div align="center">

<img src="app/llx/app/src/main/res/drawable-hdpi/ic_launcher.png" width="96" alt="白い熊 雷起動盤 app icon" />

# 白い熊 雷起動盤

**The most customizable Android home screen — re-themed black-and-yellow, made fold-aware, fully self-contained, and restorable onto a wiped phone.**

A fork of [Lightning Launcher](https://github.com/pierrehebert/LightningLauncher) with **major additions**: a launcher-wide font / colour / border theming system, a native tri-fold desktop matrix, an at-a-glance gesture editor, bundled in-app languages, backups that work on modern Android, a headless export/import door that lets the whole desktop be restored onto a clean phone, and tight integration with 白い熊's own [自由作業盤](https://github.com/ShiroiKuma0/shiroikuma-jiyusagyoban) automation app.

Installs **side-by-side** with the original Lightning Launcher (package `shiroikuma.raikidoban`).

**📥 Latest release: [`14.3.7+004`](https://github.com/ShiroiKuma0/shiroikuma-raikidoban/releases/latest)** — [all releases & APK downloads »](https://github.com/ShiroiKuma0/shiroikuma-raikidoban/releases) · [changelog »](CHANGELOG.md)

*(雷起動盤 — “raikidōban”, the “lightning launch board”.)*

</div>

---

## 🎨 「白い熊 雷起動盤 UI」 — theme every piece of chrome

A single appearance screen at the top of Lightning Settings recolours and re-fonts **every** surface of the launcher — bubble menus, dialogs, the Settings & Customize pages, toolbars, push buttons and the in-editor geometry box. Pick a global chrome font (or import your own `.ttf` / `.otf`), set colours per slot with a foundation cascade so one change ripples everywhere, and tune **border width + corner roundness** on the dialog, menu, button and geometry panels. Out of the box it's a crisp **black background with yellow text and borders**.

## ⚡ Black-and-yellow, everywhere

Every visible element follows the theme — including toasts and progress dialogs, re-routed through a custom `Flash` helper so nothing ever pops up white-on-grey. **Every dialog** wears the yellow frame, confirmations and pickers alike: a single styler is the only way this launcher shows one, so nothing can slip out unthemed. And **not one line of text is left in the platform's white**: any text no code paints by hand — the rows of a chooser, an input field, a spinner's dropdown — is caught and repainted, without ever disturbing an icon font or a font preview. The launcher is **permanently in its dark palette** — it never follows the phone's light mode, and never shows stock's white-and-orange chrome, right from the first launch on a fresh install. Even the launcher icon and the in-editor toolbar were restyled to match.

## 📐 Fold-aware desktop matrix (for the tri-fold)

Built for the Huawei Mate XT and other foldables: a native **fold → desktop** selector that switches to the right desktop for the current fold width — replacing the fragile external scripting it used to need. Lay it out visually in a **drag-and-drop grid of desktop miniatures**.

## ✋ Gestures overview

Long-tap any desktop or item and open **「ジェスチャー」** to see every event-action binding — tap, long-tap, the four swipes, lifecycle hooks — in one themed list, each row showing what it actually launches. View, edit or clear any binding from there; inherited (global-default) actions are marked and coloured distinctly.

## 🤖 白い熊 自由作業盤 integration

Tight, headless integration with 白い熊's own automation app, **[自由作業盤 (jiyusagyoban)](https://github.com/ShiroiKuma0/shiroikuma-jiyusagyoban)** — the in-house successor to Tasker. Drop a **自由作業盤 task shortcut** straight from the item / action pickers, and have widget names survive crashes and restores through a cross-app re-init handshake (no accessibility hacks needed). Such shortcuts are labelled 「白い熊 自由作業盤: …」 throughout, and their “App info” / “App store” / “Uninstall” entries resolve the app the *task* opens rather than 自由作業盤 itself.

When you shut 自由作業盤 down from its own menu, the launcher **notices and says so**: every task shortcut is drawn faded, and tapping one explains 「白い熊 自由作業盤 は停止中」 and opens 自由作業盤 instead of firing into the void — a dead tap that used to be indistinguishable from a working one.

## 🌐 Self-contained, in-app languages

English, **Japanese, Czech and Russian** are bundled right into the app — no external language pack required. Switch language from inside the app (it restarts cleanly to apply), the fork's own custom strings included.

## 💾 Backups that work on modern Android

Restores the ability to **create** backups on Android 13 (targetSdk 33), which scoped storage had blocked, by writing to a folder you choose through the system file picker. Also stops a restore from wiping your system wallpaper.

## 📤 Export / Import — one zip, ticked by category

A dedicated Export / Import window at the top of the UI page backs up **everything settable in the launcher** into a single timestamped `.zip`: the appearance theme and imported fonts, launcher settings, the fold matrix, desktops and items (with item icons and desktop wallpapers as separate sub-options), scripts, item styles and script variables. Tick what to carry, pick a backup folder once, and import restores just the parts you tick — merged, never wiped. The same export runs **headlessly** on request, so [自由作業盤](https://github.com/ShiroiKuma0/shiroikuma-jiyusagyoban) can back this app up alongside every other sister app in one run — announcing which parts start ticked rather than letting the caller guess, and **stopping cleanly** when told to: a cancelled export unwinds at the next entry, deletes its half-written archive, and leaves the backup folder exactly as it found it. It answers out of the box, with an authorization token available as an **opt-in** extra rather than a thing you must paste before anything works.

## 🔐 Restorable onto a clean phone

A second, **identified** door lets [応用管理](https://github.com/ShiroiKuma0/shiroikuma-oyokanri) back the launcher up *with its data* and put it back on a wiped device — the desktop layout, the widget bindings, the theme and the fold matrix all returning together, which without root is otherwise the one thing a phone migration loses. A caller here is not taken at its word: it must present an **exact package name**, a uid that agrees with the kernel, and a **signing certificate matching a pinned hash** — because the app that is *missing* from a fresh phone is precisely the name anyone could take. The backup itself travels through a file handle the caller opens, so the launcher writes bytes and never touches the backup folder, and the restore side of it exists **only** behind that check, never on an open broadcast.

## 🧰 Themed pickers & dialogs

Black-and-yellow replacements for the framework choosers: the shortcut-app picker, the “Select action” chooser, a redesigned **“app not installed”** dialog (re-point the icon, or jump to F-Droid / Aurora Store), a colour picker with recent-colour swatches, and a fully configurable geometry box with a live preview.

---

## Built on Lightning Launcher

This is a personal fork of **[Lightning Launcher](https://github.com/pierrehebert/LightningLauncher)** by Pierre Hébert, continued on the [TrianguloY](https://github.com/TrianguloY/LightningLauncher) `developer` line — a famously fast, light and *extremely* customizable home screen. All upstream credit belongs to Pierre and the Lightning Launcher contributors; see the upstream repository for the canonical source, issues and licence.

The fork re-brands its `applicationId` to `shiroikuma.raikidoban` so it installs **alongside** the original (the build-time `namespace` is left unchanged, so the original install keeps answering its own scripting API). It is built for personal sideloading and signed with its own release key — so a build can be updated over the last one, and an APK claiming to be 雷起動盤 can be told from one that is not.

## Building

```
cd app/llx
JAVA_HOME=<jdk17> ANDROID_HOME=<android-sdk> ./gradlew assembleExtremeDebug
```

This produces an arm64-v8a APK under `app/llx/app/build/outputs/apk/extreme/debug/` — the `debug` build *type*, but signed with the release key like every other artefact here. Day-to-day builds use the in-repo `build-apk` skill / `buildApk` Gradle task, which versions the APK, verifies its signer and archives it.

Signing needs a gitignored `app/llx/signing.properties` (`sp.storeFile` / `sp.keyAlias` / `sp.storePassword` / `sp.keyPassword`). Without it the build **fails** rather than quietly falling back to the Android debug key.
