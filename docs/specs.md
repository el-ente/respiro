# Respiro — specs

## Goal

Make a slow, ad-filled Android TV feel new again, for people who have never used a terminal. Everything
cleanyourtv does with a computer and an AI agent, Respiro does on the TV with a reviewed database.

## Users

- **Owner of a cheap or operator TV/box** (TCL, Hisense, Flow, …): slow menus, promo rows, apps they never use.
  Not technical; follows on-screen steps; uses the remote only.
- **Contributor**: someone technical who adds a new TV to the database through a pull request.

## What the user does

1. Turns on developer options and USB / network debugging (the app shows how, with pictures, before it can connect).
2. Opens Respiro and accepts "Allow USB debugging?" with "Always allow".
3. Sees their TV recognised and a plan: what will be switched off, grouped and explained in plain words.
4. Ticks what they still use from the "ask" list (streaming apps, assistant, Chromecast, updates).
5. Confirms. Respiro backs up, switches packages off in small batches and shows progress.
6. Sees the result (packages off, memory freed) and a reminder to turn debugging off again.
7. Can come back any time to see what's off and turn anything back on, one item or everything.

## Requirements

### Connection
- Connect to the TV's own ADB on `localhost:5555` with Respiro's own key; run commands as `shell`.
- Tell the user exactly what to do when it can't: debugging off, prompt not accepted, prompt stuck (ask to restart
  the TV), port closed.

### Recognising the TV
- Read `ro.product.*` and build properties and pick the model file whose `match` fits best.
- Unknown TV: apply only the `common` layer's `disable` list, say so clearly, and offer the device report.

### The plan
- Merge the model's layers: `disable` and `ask` from all of them; `keep` wins over everything.
- Skip packages that aren't installed or are already off.
- Never include Android's core packages, whatever the data says (enforced in code).
- Never start a `never_start` package, not even to read its name.

### Applying
- Back up first: package list, disabled list, relevant settings; keep it in the app.
- `pm disable-user --user 0` only, one package per command, in batches; re-read state after each batch.
- Record every change so it can be undone.
- Before disabling a setup wizard, check it doesn't own the saved Wi-Fi network.

### Undo
- A list of everything Respiro switched off, with "Turn back on" per item and "Undo everything".

### After
- Show what changed and the memory freed.
- Remind the user to switch USB / network debugging off.

## Device database

- JSON in `app/src/main/assets/devices/`, format in that folder's README. New TVs arrive by pull request, tested
  on a real TV including a reboot.
- A unit test validates every file: JSON, `extends` targets exist, no core package in `disable`, no duplicates.

## Constraints

- Android TV 8.0+ (minSdk 26), 32-bit ARM, 1 GB RAM: a release build, no heavy UI.
- D-pad only. Large text, readable from the sofa.
- English and Spanish from the start.
- No accounts, no analytics. Network only for the TV's own ADB (and, later, database updates).

## Not in scope

- Replacing the launcher, themes, button mapping (that's Daiji).
- Uninstalling, rooting, flashing.
- Debloating another TV over the network.
