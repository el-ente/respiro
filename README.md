# Respiro

*Dale un respiro a tu tele.* Give your TV a breather.

Respiro is an Android TV app that debloats the TV it runs on. It recognises the model, shows what the maker
preinstalled (telemetry, ads, promo rows, duplicate services) and switches it off in one go, safely and reversibly.

## How it works

- **Runs on the TV itself.** No computer needed. Respiro connects to the TV's own ADB (Wireless debugging or
  network ADB on the same device) to get the permissions an ordinary app doesn't have.
- **Knows the device.** A database of tested TV models, built into the app, says which packages are safe to switch
  off on each one, which must stay, and which quirks the firmware has.
- **Switches off what came with the TV.** Every change is `pm disable-user --user 0`, and Respiro keeps a record, so
  any of it can be undone from the app.
- **Uninstalls apps you installed, when you choose.** Biggest first, one at a time with a confirmation, or the ones
  you said you don't use. These can't be undone from Respiro; the Play Store has them.
- **Gives the space back.** Once a preinstalled app is off, Respiro removes its updates and clears its data; the
  factory version stays, so turning it back on works (the Play Store updates it again). Apps you installed keep
  their data.
- **Asks about what you use.** Streaming apps, the voice assistant, Chromecast and firmware updates are your call.
  Respiro asks before touching them.

## Supported TVs

| TV | Android | Tested |
|---|---|---|
| TCL 43" Smart TV (C06_2K_GB) | 11 | 2026-10-24 |
| Flow SoundBox (Sagemcom, Telecom Argentina) | 11 | 2026-10-24 |

Each one is a file in [`app/src/main/assets/devices/`](app/src/main/assets/devices/README.md). To add yours, choose
**Save device report** in Respiro: it writes `Download/respiro-<device>.txt` with a model file to start from and the
TV's preinstalled packages (never serial numbers or the apps you installed). Attach it to an issue, or turn it into a
pull request; that folder's README describes the format.

## Install

On the TV, install **Downloader** (by AFTVnews) from the Play Store, open it and enter:

```
https://github.com/el-ente/respiro/releases/latest/download/respiro.apk
```

Allow Downloader to install apps when Android asks, then open Respiro from your apps.

## Before you start

ADB must be on:

1. Settings → Device Preferences → About → press OK on **Build** seven times.
2. Back to Device Preferences → **Developer options** → turn on **USB debugging** (and **Network debugging** or
   **Wireless debugging** if your TV has it).
3. Open Respiro and accept the "Allow debugging?" prompt.

When you're done, Respiro reminds you to switch debugging off again: network ADB left open lets other devices on
your Wi-Fi control the TV.

## Status

Early, but it works end to end on the TCL 43" C06_2K_GB (Android 11): Respiro connects to the TV's own ADB,
recognises the TV, shows the plan (only what is installed and still on), asks about the apps people use, backs up,
switches the rest off in checked batches, and lists what it switched off so any of it can be turned back on.

Known so far:

- The TV must have network ADB on port 5555. Google TV sets that only offer Wireless debugging need pairing first;
  not supported yet.
- If an earlier authorization prompt is still pending (even one that never showed), Android skips new ones until
  the TV restarts. Restart the TV and open Respiro again.
