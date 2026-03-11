# Media3 Shortform Feed Performance/Reliability Demo

This project demonstrates a short-form feed stack with two data sources:

- **Local seed:** hardcoded JSON asset (`app/src/main/assets/feed.json`)
- **Remote + cache:** Retrofit fetch with Room-backed metadata persistence

## What is implemented

- Kotlin + Jetpack Compose vertical feed UI
- `LazyColumn` feed with one active item tracked by scroll index
- One shared `Media3 ExoPlayer` instance
- Auto-activation for active item
- Local hardcoded JSON seed (`app/src/main/assets/feed.json`)
- Remote JSON fetch with Retrofit
- Room cache (`videos` table) for metadata persistence
- Cached metadata first, then remote refresh
- Simple retry path on failure
- Player debug overlay with item + playback state
- In-app pull-to-refresh interaction (`onPullToRefresh`, UI refresh indicator)
- Playback position persistence across process restarts (SharedPreferences-backed store)
- Macrobenchmark scenarios scaffolded under `app/src/androidTest`
- Basic playback tests + visibility selection tests in unit test suite
- Like button toggle persisted in local interaction store
- Simulated bad-network mode to force remote fetch errors for reliability testing

## Project structure

```text
app/src/main/java/com/example/shortformvideofeed/
  MainActivity.kt
  app/AppContainer.kt
  domain/
    model/VideoItem.kt
    repository/FeedRepository.kt
    usecase/ObserveFeedUseCase.kt
    usecase/RefreshFeedUseCase.kt
  data/
    local/
      AppDatabase.kt
      FeedLocalJsonDataSource.kt
      VideoDao.kt
      VideoEntity.kt
    remote/
      FeedApi.kt
      FeedDto.kt
      FeedRemoteDataSource.kt
    mapper/
      VideoMapper.kt
    repository/FeedRepositoryImpl.kt
  player/FeedPlayerManager.kt
  ui/feed/
    FeedScreen.kt
    FeedUiState.kt
    FeedViewModel.kt
  ui/theme/Theme.kt
```

## Current behavior

- Active item follows scroll visibility and activates its player.
- Playback state is resumed from saved positions when returning to an item.
- Refresh calls trigger both remote feed refresh and paged feed refresh.
- Preload strategy supports:
  - `NEXT_1` (default)
  - `NEXT_2`
  - `OFF`

## Verification

- Unit tests: `./gradlew testDebugUnitTest --no-daemon`
- Build: `./gradlew assembleDebug --no-daemon`

## Quick run

1. Open in Android Studio.
2. Sync Gradle.
3. Run the `app` configuration on an emulator/device.

## Remote endpoint configuration

Edit `FEED_REMOTE_URL` in:

- `app/build.gradle.kts`

It should point at a JSON array matching:

```json
[
  {
    "id": "1",
    "title": "Sample clip",
    "videoUrl": "...",
    "thumbnailUrl": "...",
    "durationMs": 12000
  }
]
```
