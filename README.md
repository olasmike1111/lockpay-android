# LockPay Android (Device Owner app)

Locks to a "Payment overdue" screen on a signed LOCK command, unlocks on UNLOCK,
and removes itself as Device Owner on RELEASE. Emergency calls and the support
number are always reachable. It never wipes data and never reads the phone.

## Build
1. Get your server's public key:  GET http://YOUR_SERVER:8000/public-key  -> public_key_b64
2. In gradle.properties set LOCKPAY_PUBKEY and LOCKPAY_SERVER (or pass with -P).
3. Android Studio: open this folder, let Gradle sync, Build > Build APK(s).
   Output: app/build/outputs/apk/debug/app-debug.apk
   (CLI alternative: install JDK 17 + Android SDK, run `gradle wrapper`, then `./gradlew assembleDebug`.)

## Set up the TEST phone
1. Factory reset. Skip Google sign-in (no accounts may exist).
2. Developer options > USB debugging.
3. adb install -t app-debug.apk        (-t because debug builds are testOnly)
4. adb shell dpm set-device-owner com.example.lockpay/.AdminReceiver
5. Open LockPay, enter server URL, device id, device token (from POST /devices), Save.

## Free the test phone if anything goes wrong (debug build only)
adb shell dpm remove-active-admin com.example.lockpay/.AdminReceiver
Or issue a RELEASE command from the server.
# lockpay-android
