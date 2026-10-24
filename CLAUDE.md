# Respiro — repo rules

Android TV app that debloats the TV it runs on, over the TV's own ADB, using a built-in database of known devices.

## Safety rules (not negotiable)

These come from debloating real TVs. Breaking one can leave a TV that doesn't boot or can't reach the network.

- **Preinstalled apps are disabled, never uninstalled.** Switch them off with `pm disable-user --user 0`; every
  change is recorded and reversible from the app. No root, nothing flashed.
- **Apps the user installed are uninstalled** (`pm uninstall`), never disabled: when the plan or the uninstall list
  includes one, after the user saw it named as "will be uninstalled" or confirmed it one by one. Only packages in
  `pm list packages -3`, never Respiro itself or the app that is Home. Not undoable from the app; say so.
- **Recovering space is allowed only for preinstalled packages Respiro switched off:** once one is confirmed off, it
  may remove the app's updates (`pm uninstall-system-updates`, the factory version stays) and clear its data
  (`pm clear`), then confirm it is still off. Turning it back on gives the factory version. Never for a kept,
  protected or user-installed app, and never before the disable succeeded.
- **Clearing caches is allowed for every app** (`pm trim-caches`, Free up space): it only drops `cache`/`code_cache`
  folders that apps rebuild, never app data, downloads or settings.
- **Back up first.** Save the package list, the disabled list and the relevant settings before the first change.
- **Never disable Android's core packages:** the permission controller, package installer, ext services/shared,
  module metadata (`com.android.*` and `com.google.android.*`), `com.google.android.overlay.modules*`,
  `com.android.providers.*` (calendar, contacts, user dictionary and blocked numbers aside), `com.android.systemui`,
  `com.android.shell`, `com.android.phone`, `com.android.settings`, `com.android.tv.settings`, `android`. Enforce
  this in code, for every list, with no override.
- **Never disable what the TV needs to work:** the TV/Live TV app and input services (HDMI, tuner), the input
  picker, remote and Bluetooth pairing, the on-screen keyboard, Play Store and Play Services, and the package that
  owns the saved Wi-Fi network (check `dumpsys wifi` before disabling a setup wizard).
- **Never start a factory or test package** (`*factory*`, `*fac.menu*`, `mediatek.factorymenu.ui`, …), not even
  to inspect it. On some boards that flips a flag the shell can't undo.
- **Never disable the running launcher** unless another one is confirmed as Home.
- **Ask before anything people use:** streaming apps, the voice assistant, Chromecast, Miracast, firmware updates.
- **Disable in small batches** and check the TV between them. Some firmware re-enables packages at boot; record
  that instead of fighting it.
- **Remind the user to switch ADB off** when finished.

## Device database

- JSON files in `app/src/main/assets/devices/`, layered `common/` → `vendors/` → `models/<vendor>/<model>.json`.
  The format is in that folder's README.md; keep it in sync with any change to the format.
- One model per file, added through a pull request. Only add a model that was tested on a real TV, including a
  reboot, and say in `tested.notes` what wasn't tested.
- `keep` wins over `disable` in every layer. Core Android packages are protected in code, never by the data.
- Never store identifiers of a person's TV: serial numbers, MACs, Wi-Fi names, IPs.

## Workflow

1. Before starting a feature, fetch the main branch: `git fetch origin`.
2. Work on every feature in its own git worktree, branched from `origin/HEAD`. Never commit directly on `main`.
3. Verify the change compiles: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew -q :app:compileDebugKotlin`
   (Gradle rejects newer JDKs; `local.properties` is gitignored, copy it from the main checkout).
4. Commit, push the branch, and open the PR with `gh pr create` against `main`.
5. Keep README.md in sync with behaviour in the same change.
6. Always commit: commit and push right after every finished change, however small. Never leave finished work
   uncommitted.

## Releases

- Push a tag `vX.Y.Z` on `main`: CI tests, builds with the private release key and publishes the release with
  `respiro.apk`, which the README's install link points to. Bump `versionCode` and `versionName` first.
- The release key lives only in repository secrets (`RESPIRO_KEYSTORE_BASE64`, `RESPIRO_KEYSTORE_PASSWORD`,
  `RESPIRO_KEY_ALIAS`, `RESPIRO_KEY_PASSWORD`) and an offline backup. Never commit it: Respiro holds an ADB key the
  TV trusts, and whoever can sign an update inherits it. `app/dev.keystore` is public and signs test builds only.

## Testing on a TV

- Use a release build: debug builds of a Compose app start several times slower on a 1 GB TV.
- Before pressing keys or opening anything on a TV, check what it is showing
  (`dumpsys activity activities | grep mFocusedApp`); someone may be watching. Put it back when done.
- `input keyevent` takes one key per call; check that a screenshot changed before trusting it (byte-identical
  captures are stale frames).

## Screenshots in PRs

A PR that changes the UI carries a screenshot in its description.

1. Capture from the TV or emulator: `adb exec-out screencap -p > <screen>.png`.
2. Cover anything personal first: Wi-Fi names, account names, IPs.
3. Push it to the orphan `pr-assets` branch under `<pr-branch>/<screen>.png` (`git worktree add ../respiro-pr-assets
   pr-assets`). Never merge or rebase that branch.
4. Embed it with its raw.githubusercontent.com URL on the `pr-assets` branch.
