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

## Pins
Tap **Pin this location** to save where you're standing, then walk away: a small
coloured arrow on the rim of the compass (app, widget and floating bubble) points
back to it, corrected for magnetic vs. true north. The arrow always stays until
you delete the pin; within ~15 m a coloured ring also lights up around the dial
and the phone vibrates. Names only appear in the app.
To see pins on the **widget**, set the app's location permission to
"Allow all the time" (Android only allows widget taps to use location that way).

## Radar
Switch to the **Radar** tab for an offline, to-scale view: you in the middle, pins as
coloured dots (with names) at their real distance and direction. Tap to change zoom,
long-press to switch between heading-up and north-up. No map data or internet needed.

## Styles
**Settings** (button at the bottom of the main screen) switches the compass between
Modern (default), Operation, Gothic, Pirate, Old Western and Pixel Art. The style applies to the
app, the floating compass and the widget.

Fonts bundled (SIL Open Font License 1.1): UnifrakturCook (Gothic), Pirata One (Pirate),
Rye (Old Western), Silkscreen (Pixel Art), Share Tech Mono (Operation).
