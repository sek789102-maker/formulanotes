#!/usr/bin/env bash
# Installs the debug APK on the CI emulator, launches it, and checks it does not crash.
set -u
APK="$1"; OUT="$2"; PKG=com.sek789102.formulabook
mkdir -p "$OUT"
adb wait-for-device
adb install -r "$APK" > "$OUT/install.txt" 2>&1 || { cat "$OUT/install.txt"; exit 1; }
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb logcat -c
adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 > /dev/null
sleep 12
adb exec-out screencap -p > "$OUT/launch.png"
# Exercise the UI a little (taps/swipes only inside the app), then check it is still alive.
adb shell monkey -p $PKG --pct-syskeys 0 --throttle 150 -s 42 300 > "$OUT/monkey.txt" 2>&1 || true
sleep 3
adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 > /dev/null
sleep 5
adb exec-out screencap -p > "$OUT/after-monkey.png"
adb shell dumpsys package $PKG | grep -E "SCHEDULE_EXACT_ALARM|POST_NOTIFICATIONS" > "$OUT/permissions.txt" || true
adb logcat -d > "$OUT/logcat.txt"
if grep -E "FATAL EXCEPTION|ANR in $PKG" "$OUT/logcat.txt" | tee "$OUT/crashes.txt" | grep -q .; then
  grep -A30 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -80
  exit 1
fi
adb shell pidof $PKG > /dev/null || { echo "app not running"; exit 1; }
echo "OK: installed, launched, survived monkey test"
