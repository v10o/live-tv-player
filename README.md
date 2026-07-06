# IPTV Player for Android TV

Modern IPTV player built with Kotlin, Jetpack Compose for TV, and ExoPlayer.

## Features

- Xtream Codes API support
- M3U/M3U8 playlist support
- HLS and MPEG-TS stream playback
- Channel categories and search
- Favorites and watch history
- Resume playback from last position
- D-pad navigation optimized

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose for TV (Material 3)
- **Player**: ExoPlayer (Media3)
- **Network**: Ktor
- **Database**: Room
- **DI**: Hilt
- **Architecture**: MVVM + Repository pattern

## Project Structure

```
app/src/main/java/com/iptvplayer/tv/
├── data/
│   ├── api/              # API services (Xtream, M3U parser)
│   ├── model/            # Data models
│   ├── repository/       # Data repository
│   └── local/            # Room database
├── player/               # ExoPlayer wrapper
├── ui/
│   ├── screens/          # App screens
│   │   ├── home/         # Playlist selection
│   │   ├── channels/     # Channel browser
│   │   ├── player/       # Video player
│   │   └── settings/     # App settings
│   ├── components/       # Reusable UI components
│   └── theme/            # App theme
├── navigation/           # Navigation setup
└── di/                   # Dependency injection
```

## Setup

1. Open project in Android Studio (Ladybug or newer)
2. Sync Gradle files
3. Create Android TV emulator or connect physical device
4. Run the app

## Build

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install on device
./gradlew installDebug
```

## Usage

1. Add playlist (Xtream Codes or M3U URL)
2. Browse channels by category
3. Select channel to play
4. Use D-pad for player controls:
   - Center/Enter: Play/Pause
   - Left/Right: Seek ±10s
   - Back: Exit player

## License

MIT
