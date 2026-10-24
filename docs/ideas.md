# Respiro — ideas

Not planned yet. Move an idea to specs.md when it's decided.

## Connection

- **Wireless debugging pairing** for Google TV sets that don't offer network ADB on 5555 (pairing code entered with
  the remote).
- **Detect a stuck authorization prompt** (connection hangs with no dialog) and walk the user through a restart.
- **Close ADB for the user** at the end: turn network debugging off and confirm port 5555 refuses connections.

## Database

- **Download the latest database** from the repo's `main` at start, with the bundled copy as fallback; core
  protection stays in code so a bad file can't break a TV.
- **Device report**: properties, package list and the user's choices, exported for an issue; contributors turn it
  into a PR.
- **Community tested count**: how many people applied a model's list without undoing it.

## Debloat

- **Ad-blocking Private DNS** as an optional step (`dns.adguard-dns.com`), warning that it doesn't block YouTube ads
  and bypasses a router's own DNS filtering.
- **Screensaver swap** before disabling a heavy photo screensaver (the TCL's `backdrop` uses about 105 MB).
- **Firmware re-enables packages at boot**: show them as "comes back on restart" instead of failing.
- **Before/after numbers**: free RAM, boot time, number of background processes.

## Apps

- **App uninstaller**: list the apps the user installed themselves (not preinstalled), with size and last used,
  and remove the ones they pick. Preinstalled packages stay disable-only, as the safety rules require.
- **Clear cache**, any time: trim every app's cache (`pm trim-caches`) and clear the data of apps Respiro switched
  off, showing the space freed. On the TCL this took free storage from 704 MB to 955 MB.
- **Startup apps**: list the apps that start at boot (`BOOT_COMPLETED` receivers) and stop the ones not needed.
- **Auto-update off**: stop the Play Store from updating preinstalled bloat back to life.
- **Storage report**: what takes the space (apps, data, cache, downloads), before deciding what to clear.

## Maintenance

- **Scheduled restart**: a weekly restart at night, for 1 GB TVs that slow down after days on.
- **Health check**: free RAM, free storage and firmware age, summed up as a simple score.

## TV settings

- **Picture fixes**: turn off motion smoothing and eco dimming through known settings keys, per model in the
  database. Vendor-specific and untested; only with keys verified on a real TV.

## Experience

- **Explain each package** in plain words, with "what you lose if you switch it off".
- **Presets**: Minimal (only telemetry and ads), Recommended, Maximum.
- **A short "how to turn on debugging" video or illustrated guide** per brand (menus differ).
- **Kids mode**: switch chosen apps off behind a PIN, so they disappear from the TV until a parent turns them back
  on. Respiro isn't a launcher, so it hides apps by disabling them, not by changing Home.
- **Share link to Daiji** for people who also want a cleaner home screen.
