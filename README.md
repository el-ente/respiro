# Respiro

*Dale un respiro a tu tele.* Give your TV a breather.

Respiro is an Android TV app that debloats the TV it runs on. It recognises the model, shows what the maker
preinstalled (telemetry, ads, promo rows, duplicate services) and switches it off in one go, safely and reversibly.

## How it works

- **Runs on the TV itself.** No computer needed. Respiro connects to the TV's own ADB (Wireless debugging or
  network ADB on the same device) to get the permissions an ordinary app doesn't have.
- **Knows the device.** A database of tested TV models, built into the app, says which packages are safe to switch
  off on each one, which must stay, and which quirks the firmware has.
- **Disables, never uninstalls.** Every change is `pm disable-user --user 0`, and Respiro keeps a record, so any of
  it can be undone from the app.
- **Asks about what you use.** Streaming apps, the voice assistant, Chromecast and firmware updates are your call.
  Respiro asks before touching them.

## Before you start

ADB must be on:

1. Settings → Device Preferences → About → press OK on **Build** seven times.
2. Back to Device Preferences → **Developer options** → turn on **USB debugging** (and **Network debugging** or
   **Wireless debugging** if your TV has it).
3. Open Respiro and accept the "Allow debugging?" prompt.

When you're done, Respiro reminds you to switch debugging off again: network ADB left open lets other devices on
your Wi-Fi control the TV.

## Status

Nothing is built yet. This repository holds the plan.
