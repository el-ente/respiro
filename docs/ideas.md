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

## Experience

- **Explain each package** in plain words, with "what you lose if you switch it off".
- **Presets**: Minimal (only telemetry and ads), Recommended, Maximum.
- **A short "how to turn on debugging" video or illustrated guide** per brand (menus differ).
- **Share link to Daiji** for people who also want a cleaner home screen.
