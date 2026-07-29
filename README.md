# TalkBridge Android Client

> Real-time spoken-language translation on Android — powered by a dedicated [TalkBridge server](https://github.com/Julian-Hank/TalkBridge-Server).

---

## Overview

TalkBridge is a live translation app built with **Kotlin** and **Jetpack Compose**. It captures spoken audio, streams it to a TalkBridge backend server via WebSocket, and plays back the translated speech in real time. The client is deliberately lightweight — all heavy computation (STT, translation, TTS) runs on the server.

---

## Screens

The app has four main screens accessible via a bottom navigation bar, plus additional sub-screens:

| Screen | Description |
|---|---|
| **Home** (Live Translate) | Real-time translation via WebSocket. Select source/target language, tap start, speak — translated audio plays back automatically. |
| **Translate** | Text-based translation. Type in the source field; translation appears after a 500ms debounce delay. |
| **Transcribe** | Record audio locally, then send it to the server for transcription. Supports pause/resume, waveform visualization, and auto language detection. Saves results to local history. |
| **Settings** | Configure a custom server IP address, persisted via DataStore. |
| **Language Select** | Shared screen for picking source or target language, reachable from Home, Translate, and Transcribe. Shows recently used languages on the Home flow. |
| **Transcription Detail** | View a saved transcription item. |
| **Transcription History** | Browse and delete saved transcription items. |
| **BLE Connect** | Connect to a BLE Device to send the transcribed / translated text to |


---

## Features

- **Real-time translation** via persistent WebSocket connection with connection state tracking (`NOT_CONNECTED` → `CONNECTING` → `CONNECTED` → `READY`)
- **Live audio streaming** using `AudioRecorder` — PCM 16-bit mono at 16kHz
- **TTS audio playback** of translated speech received as binary frames from the server
- **Bluetooth headphone detection** — suppresses local audio overlap during TTS playback; skipped when Bluetooth A2DP is active
- **Text translation** via HTTP POST with 500ms debounce on input
- **Audio transcription** via HTTP POST with SSE streaming response — shows estimated processing time and a progress bar
- **Waveform visualization** — RMS-based amplitude bars computed from PCM 16-bit chunks (50 bars max, updated every 4 chunks)
- **Transcription recording controls** — start, pause, resume, stop, delete, finish
- **Auto language detection** toggle on the Transcribe screen
- **Language selection** with recently used languages (persisted, up to 4 entries)
- **Custom server IP** configurable in Settings, persisted via DataStore
- **Local history** for transcription results stored in a Room database
- **BLE connect** to connect BLE Device 

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose + Material 3 |
| Navigation | Jetpack Navigation Compose |
| Networking | OkHttp (WebSocket + HTTP) |
| Local Database | Room |
| Preferences | DataStore (`Preferences`) |
| Architecture | MVVM — `AndroidViewModel`, `StateFlow`, `MutableStateFlow` |

---

## Project Structure

```
com.talkbridge.livetranslator/
├── data/
│   ├── TalkBridgeClient.kt           # WebSocket & HTTP client
│   ├── TalkBridgeDatabase.kt         # Room database definition
│   ├── Language.kt                   # Language enum
│   ├── AppContainer.kt               # Dependency injection container
│   ├── ConnectivityObserver.kt       # Checks if Wifi is available
│   ├── audio/
│   │   ├── AudioRecorder.kt          # Mic capture (PCM 16-bit, 16kHz)
│   │   └── AudioOutputManager.kt     # TTS audio playback
│   ├── ble/
│   │   ├── BLEConnectManager.kt      
│   │   └── BleService.kt     
│   ├── local/
│   │   ├── dao/
│   │   │   ├── TranscriptionItemDao.kt
│   │   │   └── TranslationHistoryItemDao.kt
│   │   └── entity/
│   │       ├── TranscriptionItem.kt
│   │       ├── TranslationHistoryItem.kt
│   │       └── LocalDateConverter.kt
│   └── repository/
│       ├── TranscriptionItemsRepository.kt
│       ├── TranslationHistoryItemsRepository.kt
│       └── UserPreferencesRepository.kt  # IP address, language prefs, recent languages
├── ui/
│   ├── AppViewModelProvider.kt
│   ├── common/
│   │   └── LanguageSelect.kt         # Shared language picker screen
│   ├── facetoface/
│   ├── home/                         # Live translation screen
│   ├── introduction/                 # unfinished introduction screen
│   ├── navigation/
│   │   ├── NavigationDestination.kt
│   │   └── TalkBridgeNavGraph.kt
│   ├── settings/
│   ├── theme/
│   ├── transcribe/                   # Recording + transcription screen + history
│   └── translate/                    # Text translation screen
├── MainActivity.kt
├── TalkBridgeApp.kt                  # Top app bar + bottom nav bar composables
└── TalkBridgeApplication.kt
```

---

## Setup

### Prerequisites

- Android Studio (latest stable)
- Android device or emulator running API 26+
- A running [TalkBridge server](https://github.com/Julian-Hank/TalkBridge-Server) reachable on your network

### Server IP Configuration

The default fallback IP in `TalkBridgeClient.kt` is `192.168.68.57` on Port `8080`. To use a different server, enter its IP address and Port in the app's **Settings** screen. The value is persisted via DataStore and used automatically for all WebSocket and HTTP requests.

### Permissions Required

- `RECORD_AUDIO` — for microphone access in the Home and Transcribe screens

### Build & Run

1. Clone the repository
2. Open the `TalkBridgeLive` project in Android Studio
3. Connect an Android device or start an emulator
4. Run

---

## WebSocket Protocol (Home Screen)

On connection, the client sends an init message:

```json
{
  "source_lang": "de",
  "target_lang": "en"
}
```

The server responds in sequence:

| Message | Type | Description |
|---|---|---|
| `{"type": "connected"}` | JSON | Connection acknowledged |
| `{"type": "ready"}` | JSON | Session initialized — recording starts automatically |
| `{"type": "final", "text": "..."}` | JSON | Final translated text |
| *(binary frames)* | bytes | TTS audio played back immediately via `AudioOutputManager` |

The client streams raw PCM 16-bit audio chunks as binary WebSocket frames. Audio sending is suppressed while TTS is playing back locally, unless Bluetooth A2DP is active.

---

## HTTP Endpoints Used

### `POST /transcript` — Transcribe screen

Sends a recorded WAV file. Response is an SSE stream.

**Request:** `multipart/form-data`
- `lang` — language code (`de`, `en`, ...) or `auto` for automatic detection
- `audio` — WAV file (PCM 16-bit, 16kHz)

**SSE Response:**
```
data: {"type": "estimated_time", "seconds": 3}
data: {"type": "transcript", "text": "Hallo, wie geht es dir?"}
```

The UI shows a progress bar animating over the estimated time, then saves the result to Room on completion. (Progress bar may be wrong since estimated time is send by the server and depends on the hardware of the server)

### `POST /translate` — Translate screen

**Request:** `multipart/form-data`
- `text`, `source_lang`, `target_lang`

**Response:**
```json
{ "translated": "Hello, how are you?" }
```

---

## Supported Languages

Dutch, English, French, German, Italian, Polish, Portuguese, Russian, Spanish, Swedish, Turkish, Ukrainian

---


## License

This project was developed as part of a **Jugend Forscht** youth science competition entry.
