# API Manager

A lightweight, offline-first Android vault for the API endpoints and secret keys you use for your own projects.

No accounts. No ads. No analytics. No Google Play services. No Firebase. Your keys are encrypted on-device and only leave the device when **you** export a backup or start a device-to-device transfer.

## Features

- **Local encrypted vault** — AES-256-GCM encryption with a key held in the Android Keystore.
- **PIN / biometric lock** — optional 4–6 digit PIN, fingerprint, or both.
- **Themes** — Light, Dark, and Auto (follow system) from the settings menu.
- **Expiry tracking** — optional `DD-MM-YYYY` expiry date per key; unset means never expires. Expired keys are highlighted.
- **Trash with auto-purge** — deleting an endpoint moves it to 🗑️ Trash, where it can be restored for 14 days. After 14 days it is removed automatically. You can also empty Trash manually.
- **Duplicate detection** — multiple keys for the same site are badged as duplicates and extra entries are auto-named `1`, `2`, `3`, ....
- **Copy endpoint URL** — one tap copies the base URL for use in custom apps and scripts.
- **Local ZIP backups** — export/import an encrypted ZIP through the Android file picker. You control where it goes (drive, USB, your own cloud, etc.).
- **QR peer-to-peer sync** — split a backup into QR chunks and scan them on another device. A one-time transfer code encrypts the keys; works fully offline.
- **QR + LAN peer-to-peer sync** — show a one-time QR with a random token and transfer code; the other device downloads the encrypted backup directly over your Wi-Fi/LAN. No internet server involved.
- **Google AI Mode / Brave AI search** — opens the selected AI search engine directly in your browser (using `udm=50` or `summary=1`) with an endpoint-focused query. No search backend is hosted by this app.
- **Direct provider links** — OpenAI, Anthropic, Gemini, and OpenRouter key-management pages are available in About so you can manage keys yourself in a browser.

## What the app does / does not do

| Area | Behaviour |
| --- | --- |
| Storage | Room database on-device. API keys stored as AES-GCM ciphertext with a 12-byte random IV. |
| Trash | Deleted entries are kept for 14 days, then purged automatically. Manual "Empty Trash" is also available. |
| Accounts | None. There is no sign-in, no Google account, no cloud account. |
| Network | Only for optional LAN device-to-device transfer and links opened in your browser. Web search uses your browser (Google AI Mode or Brave AI). LAN transfer uses plain HTTP on your local network with a one-time random token; the client rejects non-private IP addresses. |
| Telemetry | None. No analytics, crash reporting, ads, or trackers. |
| Backups | ZIP contains encrypted keys only. It does **not** contain your PIN hash, salt, or keystore key. |

> **Backup restore note:** because the AES key is non-exportable and bound to the Android Keystore, a ZIP backup is intended to be restored on the same app install/device (for example after reinstall). For moving to a new phone, use **QR** or **LAN** transfer while you still have the old device. Those transfers generate a one-time code: keys are encrypted with it before leaving the old device, then decrypted and re-encrypted with the new device's Keystore.

## Build

### One-click (Windows)

Double-click `build-apk.cmd`, or run:

```powershell
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1
```

### Manual

Set the signing values in your own shell. Never commit real passwords or keystores.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:STORE_PASSWORD='<your-keystore-password>'
$env:KEY_PASSWORD='<your-key-password>'
$env:KEYSTORE_PATH='C:\path\to\api-manager\my-upload-key.jks'
& '.\gradlew.bat' assembleRelease --no-daemon
```

`build-apk.ps1` prompts for the keystore password if `STORE_PASSWORD` is not set. `*.jks` and `*.keystore` are gitignored; only the standard Android `debug.keystore` is committed.

Release output:

```text
app/build/outputs/apk/release/app-release.apk
```

Debug builds use the committed `debug.keystore` and can be built with `gradle assembleDebug`.

## Publishing targets

This project is intended for GitHub, IzzyOnDroid, and F-Droid. It is **not** intended for Google Play.

- GitHub: <https://github.com/arvind-ranga/api-manager>
- IzzyOnDroid / F-Droid metadata: `fastlane/metadata/android/` (short/full description, changelog, icon). The repository-side YAML for IzzyOnDroid is maintained by their maintainers after submission.

## Support

- Ko-fi: <https://ko-fi.com/arvindranga>
- GitHub Sponsors: <https://github.com/sponsors/arvind-ranga>
- UPI: `electriclegend@yescred`

## Secrets in this repository

- No API keys, release keystores, `.env` files, `local.properties`, `google-services.json`, or cloud credentials are committed.
- Release signing reads `STORE_PASSWORD` / `KEY_PASSWORD` from environment variables; `build-apk.ps1` prompts securely when they are absent.
- `*.jks` and `*.keystore` are gitignored. The only committed key file is the standard, non-secret Android `debug.keystore` used for debug builds.

## License

MIT. See [LICENSE](LICENSE).
