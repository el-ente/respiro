# Device database

One JSON file per tested TV model, built on shared layers:

```
common/    what holds on any Android TV with Google services
vendors/   what holds across one maker's sets (mostly what to keep)
models/    one file per tested model: how to recognise it and what to do on it
```

A model `extends` common and vendor files; entries in the model add to theirs. Android's core packages are
protected in code, not here, so no file can disable them.

## Model file

```json
{
  "id": "tcl/c06-2k-gb",
  "name": "TCL 43\" Smart TV (C06_2K_GB)",
  "match": { "ro.product.manufacturer": "TCL", "ro.product.device": "C06" },
  "firmware": { "android": "11", "sdk": 30, "build": "AR2101" },
  "extends": ["common/google-tv", "vendors/tcl"],
  "disable": [{ "package": "…", "group": "telemetry|ads|services|preloaded", "reason": "…" }],
  "ask": [{ "package": "…", "category": "streaming|assistant|cast|updates", "label": "…" }],
  "keep": [{ "package": "…", "reason": "…" }],
  "never_start": ["…"],
  "quirks": {
    "reenabled_at_boot": ["…"],
    "remote_keys": [{ "scancode": 204, "button": "…" }],
    "notes": ["…"]
  },
  "tested": { "date": "YYYY-MM-DD", "reboot": true, "notes": "…" }
}
```

- **match:** system properties that must all be equal, always including `ro.product.device`. Respiro picks the model
  with the most matching properties; no two models may have the same match.
- **disable:** safe to switch off on this model; Respiro does it without asking.
- **ask:** things people use; Respiro asks first and disables only what the user doesn't tick. If one layer disables
  a package and another asks about it, Respiro asks.
- **keep:** must stay on, with the reason. Respiro never disables these, even if a layer lists them under disable.
- **never_start:** packages Respiro must not open, not even to inspect them (factory menus).
- **tested:** every model is tested on a real TV, including a reboot. Say what wasn't tested.

Every package must be a plain package name (letters, digits, `_`, dots); ids are unique, and a model's id never
starts with `common/` or `vendors/`.

Never store anything that identifies one person's TV: serial numbers, MAC addresses, Wi-Fi names, IP addresses.

## Adding a device

Open a pull request with the model file (and any vendor file it needs). Respiro's device report gives the
properties and package list to start from.
