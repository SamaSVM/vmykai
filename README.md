# vmykai

An Android app for tracking electricity outage ("GPV" / blackout) schedules in Ukraine. The app fetches the schedule for the user's queue, keeps it synced in the background, shows upcoming power status changes, and surfaces the information on the home screen via widgets.

## Features

- Outage schedule sync in the background (`SyncWorker`)
- Push notifications before upcoming power status changes (`NotificationScheduler`, `OutageNotificationWorker`)
- Home screen widgets in full and compact sizes (`OutageWidget`, `OutageWidgetCompact`)
- Queue/region picker during onboarding (`QueuePickerScreen`)
- Local schedule caching via DataStore Preferences

## Tech stack

- Kotlin 2.0.21
- Android Gradle Plugin 8.12.0
- Jetpack Compose (BOM 2024.09.00, Material 3)
- Glance (app widgets)
- WorkManager (background sync and notifications)
- DataStore Preferences (local storage)
- kotlinx.serialization

## Requirements

- JDK 11
- Android SDK with `compileSdk` / `targetSdk` 36, `minSdk` 35

## Project structure

```
app/src/main/java/com/example/vmykai/
├── data/           # Schedule repository, models, local cache and remote sources
├── notifications/  # Notification scheduling and content for status changes
├── sync/           # Background schedule synchronization (WorkManager)
├── widget/         # Home screen widgets (Glance)
└── ui/             # Compose screens, components and theme
```

## Building and running

```bash
./gradlew assembleDebug   # build a debug APK
./gradlew test            # run unit tests
```

Alternatively, open the project in Android Studio and run the `app` configuration on an emulator or device.
