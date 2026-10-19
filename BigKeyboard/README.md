# Big Keyboard (for car head units)

Large-key on-screen keyboard (QWERTY + numbers + symbols). Key height is adjustable in the app.

## Build the APK (no PC setup needed)
1. Create a free GitHub account and a new repository.
2. Upload everything in this folder (keep the folder structure, including the hidden `.github` folder).
3. Open the repo's "Actions" tab -> "Build APK" -> "Run workflow".
4. When it finishes, download the `BigKeyboard-apk` artifact (a zip containing `app-debug.apk`).

Or: open this folder in Android Studio and choose Build > Build APK(s).

## Install in the car
Copy `app-debug.apk` to a USB stick and install it with the car's file manager/APK installer.

1. Open "Big Keyboard" and tap "1. Enable keyboard", then turn it on.
2. Tap "2. Switch to Big Keyboard" and choose it.
3. Use the slider to set the key height, and test in the text field.

If the car has no Settings screen, with adb over USB/Wi-Fi:
    adb shell ime enable com.example.bigkeyboard/.BigKeyboardService
    adb shell ime set com.example.bigkeyboard/.BigKeyboardService
