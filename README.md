# Hermes Android App

A Kotlin + Jetpack Compose Android client for Hermes Agent, talking to the dashboard
directly over your **local network** (plain HTTP/WebSocket, no tunnels, no TLS).

## Features

- **Local-network only**: connects straight to the Hermes dashboard on your LAN
- **Authentication flow**: password login (basic provider) → session cookies → ws-ticket → WebSocket
- **Session management**: create, list, and delete chat sessions with Room persistence
- **Real-time streaming**: message deltas, tool events (start/progress/complete/error), gateway ready
- **Messenger-style UI**: chat bubbles with timestamps, role-based colours
- **Settings**: local IP address, credentials, auto-connect toggle, persisted in DataStore
- **Offline-first**: Room database for sessions/messages, DataStore for settings
- **GitHub Actions**: automated debug APK builds on push/PR

## Architecture

```
app/
├── data/
│   ├── db/           # Room database (entities, DAOs, database)
│   ├── model/        # Serialization models (JSON-RPC, events, domain models)
│   ├── preferences/  # DataStore for settings
│   └── repository/   # Repository layer
├── di/               # Koin dependency injection modules
├── network/          # HTTP client (auth, ticket) + WebSocket client
├── ui/
│   ├── chat/         # Chat screen with streaming bubbles
│   ├── login/        # Login screen
│   ├── sessionlist/  # Session list screen
│   ├── settings/     # Settings screen
│   └── theme/        # Material3 theming
└── MainActivity.kt   # Navigation and screen routing
```

## Prerequisites

- Android Studio Ladybug (2024.2.1) or later, JDK 17
- Android SDK 35 (compileSdk), minSdk 26, targetSdk 34
- Hermes dashboard listening on `0.0.0.0:9119` with basic auth enabled

## Setup

### 1. Start the Hermes dashboard on your LAN

```bash
# Bind on all interfaces so the phone can reach it
hermes dashboard --host 0.0.0.0 --port 9119
```

Enable basic auth in the dashboard (this example uses `asarel` / `asarel123`).
Confirm it is reachable from another device on the same network:

```bash
curl -u asarel:asarel123 http://192.168.1.100:9119/login
```

Find the host's LAN IP:

- **Linux**: `ip route get 1 | awk '{print $7}'`
- **macOS**: `ipconfig getifaddr en0`
- **Windows**: `ipconfig | findstr IPv4`

Example: `192.168.1.100`

Both the phone and the Hermes host must be on the same Wi-Fi/LAN, and the host
firewall must allow TCP 9119.

### 2. Build

```bash
./gradlew assembleDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`

### 3. Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or copy the APK to the phone and install it manually (allow unknown sources).

### 4. First launch

1. Open the app.
2. Enter the **local IP address** (e.g. `192.168.1.100`), username and password.
3. Tap **Connect**. The app logs in over HTTP, fetches a WebSocket ticket, and opens
   `ws://<ip>:9119/api/ws`.
4. Create a session and start chatting.

No protocol, port, or path needs to be typed into the app — only the IP.

## Settings screen

Gear icon in the session list:

- **Local IP Address** — LAN IP of the Hermes host (e.g. `192.168.1.100`)
- **Username** / **Password** — dashboard basic-auth credentials
- **Auto-connect** — log in automatically on launch
- **Connection status** — live indicator, plus a Disconnect button

## Network protocol

Plain HTTP + plain WebSocket on the LAN (the manifest sets
`android:usesCleartextTraffic="true"` so Android permits cleartext to private IPs):

1. `POST http://{ip}:9119/auth/password-login`
   Body: `{"provider":"basic","username":"…","password":"…"}`
   Response: `{"ok":true,"next":"/"}` + `Set-Cookie: hermes_session_at=…`, `hermes_session_rt=…`
2. `POST http://{ip}:9119/api/auth/ws-ticket` with the `Cookie` header
   Response: `{"ticket":"…","ttl_seconds":30}`
3. `ws://{ip}:9119/api/ws?ticket=<ticket>` — no TLS, no subprotocol required

The client reconnects up to 5 times with linear backoff if the socket drops.

### JSON-RPC methods

| Method | Direction | Description |
|--------|-----------|-------------|
| `session.create` | Client→Server | Create new session |
| `prompt.submit` | Client→Server | Send user prompt |
| `message.delta` | Server→Client | Streaming text delta |
| `tool.start` | Server→Client | Tool invocation started |
| `tool.progress` | Server→Client | Tool progress update |
| `tool.complete` | Server→Client | Tool completed with result |
| `tool.error` | Server→Client | Tool failed |
| `gateway.ready` | Server→Client | Session ready |
| `approval.request` | Server→Client | Approval needed |
| `error` | Server→Client | Error notification |

## Security notes

This client is intentionally LAN-only and unencrypted. Anyone on the same network can
sniff the traffic, and the settings store keeps the password in app-private DataStore
(not encrypted). Do not expose port 9119 to the internet; if you ever need remote
access, put the dashboard behind a TLS-terminating reverse proxy and switch the client
back to `https`/`wss`.

## Release build

```bash
./gradlew assembleRelease
```

Unsigned APK at `app/build/outputs/apk/release/app-release.apk`.

## GitHub Actions

`.github/workflows/build-debug-apk.yml` builds a debug APK on push/PR, uploads it as an
artifact (30-day retention), creates a GitHub Release on version tags, and runs lint
and unit tests.

## Dependencies

| Category | Libraries |
|----------|-----------|
| UI | Jetpack Compose, Material3, material-icons-extended |
| Database | Room (KSP), DataStore Preferences |
| Networking | OkHttp 4.12 (HTTP + WebSocket) |
| Serialization | Kotlinx Serialization JSON |
| DI | Koin 3.5 |
| Async | Kotlinx Coroutines, Kotlinx Datetime |

## License

MIT License - see LICENSE file for details.
