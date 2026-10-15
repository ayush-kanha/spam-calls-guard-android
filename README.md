# Prefix Guard — Android spam call blocker

A minimal, offline Android app that rejects incoming calls when the caller number starts with one of your saved digit prefixes. No account, internet permission, ads, or analytics.

## Requirements
- Android 10 (API 29) or newer.
- Android Studio with Android SDK Platform 35 and a compatible JDK (Android Studio's bundled JDK is fine).

## Build and install
1. Unzip this project on a computer with Android Studio.
2. Open the `PrefixGuard` folder in Android Studio and let Gradle sync.
3. Connect your Android phone with USB debugging enabled, or select Build > Build Bundle(s) / APK(s) > Build APK(s).
4. Install the generated APK on your phone.
5. Open **Prefix Guard** and tap **Enable call screening**. Confirm Android's system prompt to make it your call-screening app.
6. Add the starting digits that you want blocked, e.g. `140` or `98765`.
7. Keep Prefix Guard selected as the system call-screening app.

## Behavior and limitations
- Matching incoming calls are disallowed and rejected by Android's call-screening service, with call log and missed-call notification skipped where Android honors those flags.
- Non-matching calls are allowed normally.
- Prefixes are stored locally in app-private preferences; they are not uploaded.
- The Android OS and phone manufacturer control call screening. The app cannot guarantee identical behavior on every device or with every carrier/VoIP call.
- Private/hidden caller IDs cannot be matched against a digit prefix.
- The screening service has to respond promptly; this implementation performs only a local prefix lookup.

## Project notes
- Package: `com.prefixguard.app`
- Min SDK: 29 (Android 10)
- Target/compile SDK: 35
- No runtime phone-state or contacts permissions are requested; Android grants the screening callback through the call-screening role.
