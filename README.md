# NextClass

A precision class-routine app for Android. Import your university timetable as a PDF or image, let Gemini extract the sessions, review them, and get a home screen widget that lists your whole day with the current or next class highlighted.

Built for a small private group of students. Each person brings their own free Gemini API key. There is no backend server.

## Features

- **Routine import.** Pick a PDF or image (JPG/PNG) of your timetable. PDFs are rendered page by page at high resolution with `PdfRenderer`. An optional section or group filter (for example `Section A, Group A1`) isolates your classes from multi-section timetables.
- **AI extraction.** The pages are sent to the Gemini API (`gemini-3.8-flash`, falling back to `gemini-2.5-flash`) with a strict JSON response schema. Merged lab cells span from the first start time to the last end time, and break rows are ignored.
- **Review before saving.** Parsed classes are grouped by day. Tap a row to edit room or time, swipe to delete a wrong entry, or add a class manually.
- **Full-day widget.** The home screen widget lists every class of the day:
  - the ongoing class, or the next one when nothing is running, is highlighted with a vermilion accent
  - classes that already finished are faded
  - once the last class is over, the widget shows the next scheduled day
- **Compact widget.** A one-row widget, "NextClass (compact)", that shows only the ongoing or next class.
- **Widget theme.** Follow the phone, or force light or dark, in Settings.
- **12-hour time.** Times show as `09:00 AM` everywhere, and you can type times as `9:00 AM`, `2:30 pm` or `14:30`.
- **One-day changes.** Tap a class on the home screen to cancel it for one date or use a different room that day. The widget, the hero card and the reminder follow the change, and it resets by itself afterwards.
- **Free-time gaps.** Gaps of 20 minutes or more between classes show as `2H 10M FREE`.
- **Course list.** Upload a photo or PDF of your course codes and titles. Titles are filled into every class with a matching code.
- **Share and backup.** Share your routine as a code message for friends, save a backup file, and restore from a backup or a pasted code.
- **One notification per day.** A single reminder for the first class of the day, with a configurable lead time (15, 30 or 45 minutes). No reminders for later classes.
- **No background polling.** The widget refreshes through one exact alarm set for the next start or end time.
- **Private by design.** The API key is stored with `EncryptedSharedPreferences` and never leaves the device except in requests to Google.

## Design

Swiss-grid, utility-first. Warm off-white (`#F8F8F6`) in light mode and true obsidian (`#0E0E10`) in dark mode, flat surfaces with 1dp hairline borders, and a single Signal Vermilion accent reserved for live and highlighted states. Times and countdowns use tabular figures. There are no gradients, no emojis and no nested shadowed cards.

## Tech stack

| Area | Library |
| --- | --- |
| UI | Jetpack Compose, Material 3 |
| Widget | Jetpack Glance (`glance-appwidget`) |
| Storage | Room with KSP |
| Key storage | AndroidX Security Crypto |
| Networking | Retrofit, OkHttp, Kotlinx Serialization |
| Scheduling | AlarmManager, WorkManager, boot receiver |
| Build | Gradle (Kotlin DSL), version catalog, GitHub Actions |

Requirements: JDK 17, Android SDK 35, minimum Android 8.0 (API 26).

## Project structure

```
app/src/main/java/com/example/nextclass/
  data/local      Room entity, DAO, database, encrypted key storage, preferences
  data/remote     Gemini API service, DTOs, PDF/image parsing repository
  domain          Models, current-or-next-class resolver, daily alarm use case
  ui              Theme, components, screens (home, import, courses, settings), ViewModel
  widget          Glance widget, receiver, update helper
  worker          Alarm helper, notification and boot receivers, daily worker
```

## Build

### On GitHub (no Android Studio needed)

1. Create a repository and push this project to the `main` branch.
2. Open the **Actions** tab and wait for **Build Android APK** to finish. You can also start it manually with **Run workflow**.
3. Open the finished run and download **NextClass-Debug-APK** from the artifacts section.
4. Unzip it and install `app-debug.apk` on your phone.

### Locally

```
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. No API key or secret is needed to compile.

### Optional: signed release build

Set these environment variables before running `./gradlew assembleRelease`:

```
NEXTCLASS_KEYSTORE_PATH
NEXTCLASS_KEYSTORE_PASSWORD
NEXTCLASS_KEY_ALIAS
NEXTCLASS_KEY_PASSWORD
```

Never commit a keystore. `*.jks` and `*.keystore` are already in `.gitignore`.

## First run

1. Get a free API key from [Google AI Studio](https://aistudio.google.com/app/apikey).
2. Open NextClass, go to **Settings**, paste the key and tap **Verify Key**.
3. Tap **Import Routine**, choose your timetable PDF or image, optionally enter your section or group, and start the scan.
4. Check the parsed classes, fix anything wrong, and save.
5. Long-press your home screen, add the **NextClass** widget, and resize it to fit your day.
6. Allow notifications and exact alarms when Android asks, so the widget updates on time and the first-class reminder fires.

## Permissions

| Permission | Why |
| --- | --- |
| `INTERNET` | Send the routine to the Gemini API |
| `POST_NOTIFICATIONS` | First-class reminder |
| `SCHEDULE_EXACT_ALARM` | Update the widget and send the reminder at the exact minute |
| `RECEIVE_BOOT_COMPLETED` | Rebuild alarms after a reboot, update or clock change |

## Troubleshooting

- **Widget does not change at class time.** Allow exact alarms for NextClass in system settings, and exclude the app from aggressive battery optimization if your phone vendor kills background alarms.
- **Scan returns nothing or the wrong classes.** Use a sharper image or the original PDF, and add a section or group filter. You can fix any row on the review screen.
- **Key verification fails.** Check that the key starts with `AIza`, that it is active in Google AI Studio, and that the phone is online.
- **Build fails on GitHub.** Open the failed run, copy the Gradle error from the **Build Debug APK** step, and fix the dependency or code it points at.

## License

Private project for personal and group use.
