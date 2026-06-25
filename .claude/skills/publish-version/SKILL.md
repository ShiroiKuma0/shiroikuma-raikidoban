---
name: publish-version
description: Publish the latest built 白い熊 雷起動盤 (shiroikuma.raikidoban) APK as a GitHub release/version — create a git tag WITHOUT a leading "v", a GitHub Release with the APK attached and a very specific changelog, refresh the README (fork-of intro + major-feature sections), and ensure the GitHub default branch is `main` so the repository landing page lands on our custom build. Use whenever 白い熊 says "publish", "/publish-version", "publish a version", "publish the latest build", "cut a release", "make a GitHub release/version", or "tag a release" for this LightningLauncher fork.
---

# Publish the latest build as a GitHub version/release

Takes the most recent APK already built into `~/tmp/` and publishes it as a GitHub Release on `ShiroiKuma0/shiroikuma-raikidoban`: a no-`v` tag, the APK as a download, a specific changelog, a refreshed README, and the default branch set to `main`.

This is an **outward-facing, public** action. 白い熊 invoking `/publish-version` (or asking to publish) IS the go-ahead — proceed. If you reach this skill without an explicit ask, confirm first. Never delete or overwrite an existing release/tag for the same version without asking.

## Prerequisites

- `gh` is authenticated as `ShiroiKuma0` (`gh auth status`). It needs network — run `gh` (and `git push`) with `dangerouslyDisableSandbox: true`.
- There is a built APK in `~/tmp/` named `shiroikuma-raikidoban_<VERSION>_arm64-v8a.apk` (produced by the `build-apk` skill). If not, build first.
- `main` is committed and pushed — the release tag must sit on a commit that exists on the remote.

## Steps

1. **Pick the build + version.** `ls -t ~/tmp/shiroikuma-raikidoban_*.apk | head -1`. Parse `<VERSION>` out of the filename (`shiroikuma-raikidoban_<VERSION>_arm64-v8a.apk`), e.g. `14.3.0+82`. That `<VERSION>` is the tag **and** the release title.
   - This publishes the latest build *as-is*, including a dev `+N` suffix. For a clean public version with no `+N`, first cut a `BUILD_NUMBER=0` build (set `VERSION_NAME`, `BUILD_NUMBER=0` in `app/llx/gradle.properties` → build via the `build-apk` skill), then publish that.
   - Confirm `git status` is clean and `git rev-parse main` is pushed (`git log origin/main..main` is empty). If not, stop and tell 白い熊.

2. **TAG — never a leading `v`.** The tag is exactly `<VERSION>` (e.g. `14.3.0+82`, `14.3.1`). This is a hard rule for every release. `gh release create` creates the tag at `main`'s current HEAD; don't hand-prefix it.

3. **Compile the changelog — be very specific; list everything.** Do NOT summarize away detail.
   - **First release:** enumerate *everything the fork adds over stock Lightning Launcher*, grouped by area (appearance/theming, fold matrix, gestures overview, 自由作業盤 integration, languages, backups, themed pickers, packaging/side-by-side, fixes). Walk `git log --no-merges --reverse` over the fork's own commits (from the `Re-brand to shiroikuma.raikidoban` commit onward) so nothing is missed.
   - **Later releases:** list every change since the previous tag — `git log --no-merges <prev-tag>..HEAD` — rewritten as specific, user-facing bullets.
   - Write it to a temp file (under the scratchpad) for `--notes-file`.

4. **Refresh `README.md` on `main`.** Keep the existing structure (centered title + icon, the "A fork of [Lightning Launcher] with major additions: …" line, "installs side-by-side", the **Latest release: `<VERSION>`** link to `/releases/latest`, the per-feature `##` sections, the "Built on Lightning Launcher" lineage/credits, and the "Building" section). Update the latest-version line to `<VERSION>`, and add a feature section for anything newly shipped. Commit (`README.md` only) and push to `main`. (No Claude attribution in the commit — see the repo `CLAUDE.md`.)

5. **Ensure the GitHub default branch is `main`** so the repo landing page shows our custom build (the upstream fork leaves it on `developer`): `gh repo edit ShiroiKuma0/shiroikuma-raikidoban --default-branch main`. Idempotent — safe to run every time.

6. **Create the release with the APK attached:**
   ```
   gh release create <VERSION> \
     ~/tmp/shiroikuma-raikidoban_<VERSION>_arm64-v8a.apk \
     --title "<VERSION>" --notes-file <changelog-file> --latest
   ```
   The APK is debug-signed (sideload-installable, not a Play release) — fine for these personal releases. If a release for `<VERSION>` already exists, ask before `--clobber`/recreating.

7. **Verify + report.** `gh release view <VERSION>` and report the release URL to 白い熊.

## Rules (read every time)

- **Tags never carry a leading `v`** — `14.3.0+82`, not `v14.3.0+82`.
- The changelog is exhaustive and specific, not a summary.
- Publishing is public/outward-facing: proceed on 白い熊's ask, otherwise confirm; never silently overwrite an existing version.
- Run `gh` / `git push` outside the sandbox (`dangerouslyDisableSandbox: true`).
