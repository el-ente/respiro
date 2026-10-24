# Respiro — repo rules

Android TV app that debloats the TV it runs on, over the TV's own ADB, using a built-in database of known devices.

## Safety rules (not negotiable)

These come from debloating real TVs. Breaking one can leave a TV that doesn't boot or can't reach the network.

- **Disable, never uninstall.** Only `pm disable-user --user 0`. No `pm uninstall`, no root, nothing flashed.
  Every change must be recorded and reversible from the app.
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

- Lives in code, one entry per tested model: how to recognise it (`ro.product.*` props, build id), packages that
  are safe to disable (grouped: telemetry, ads/content, background services, preloaded apps), packages to keep
  and why, and known firmware quirks.
- Only add an entry that was tested on a real device, including a reboot. Note what was not tested.
- Never store identifiers of a person's device: serial numbers, MACs, Wi-Fi names, IPs.

## Workflow

- Work on each feature in its own branch; never commit directly to `main`.
- Keep README in sync with behaviour in the same change.
