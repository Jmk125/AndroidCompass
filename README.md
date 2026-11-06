# Android Compass

A digital compass app plus a 2×2 home screen widget.

- **App:** rotating dial with degree readout and cardinal direction (magnetic north).
- **Widget:** tap it to switch it on; it updates live (~5×/sec) while on, and
  switches itself off when you lock the screen, tap again, or after 5 minutes
  (Android doesn't let widgets read sensors on their own, so a short-lived
  foreground service does it — you'll see a notification while it's live).

## Build
Open in Android Studio and run, or `./gradlew assembleDebug`
(APK at `app/build/outputs/apk/debug/app-debug.apk`). Pushes also build the
APK in GitHub Actions — download it from the run's artifacts.

## Add the widget
Use the **Add home screen widget** button in the app, or long-press the home
screen → Widgets → Compass.
