# On-device verification

- Device: sdk_gphone64_x86_64, Android 15 (API 35)
- Date: 2026-11-20T17:32Z

## 1. Integration tests (debug build, real Android storage/plugins)
- ❌ integration_test failed (see integration_test.log)

## 2. Release build (R8) functional check across a process restart
- ✅ release smoke APK installed
- ✅ phase 1: 10 checks passed
- ℹ️ notificationsAllowed=true exact=true tz=Etc/UTC
- ✅ scheduled notification delivered by Android
- ✅ phase 2 (after restart): 9 checks passed
- ✅ no Java/Kotlin crash in release smoke build

## 3. Production release APK launch
- ✅ production release APK installed
- ✅ app running 10 s after launch (pid 4820)
- ✅ no crash / Flutter error in logcat
- ℹ️ cold start TotalTime: 1062
- ✅ relaunch after process death OK

**Result: 1 CHECK(S) FAILED**
