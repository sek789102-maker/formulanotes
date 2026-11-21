#!/usr/bin/env bash
# Installs the debug APK on the CI emulator, walks through a few real flows with
# screenshots, runs a random-tap stress test, and fails on any crash.
set -u
APK="$1"; OUT="$2"; PKG=com.sek789102.jotwai; ACT="$PKG/.MainActivity"
mkdir -p "$OUT"
shot() { sleep "${2:-3}"; adb exec-out screencap -p > "$OUT/$1.png"; }
adb wait-for-device
adb install -r "$APK" > "$OUT/install.txt" 2>&1 || { cat "$OUT/install.txt"; exit 1; }
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb shell appops set $PKG SCHEDULE_EXACT_ALARM allow || true
adb logcat -c

adb shell am start -n $ACT > /dev/null; shot 01-home-empty 8
adb shell am start -n $ACT -a com.sek789102.jotwai.NEW_CHECKLIST > /dev/null; shot 02-new-checklist 4
adb shell input text "Milk"; adb shell input keyevent 66; adb shell input text "Eggs"; adb shell input keyevent 66; adb shell input text "Bread"
shot 03-checklist-typed 2
adb shell input keyevent 4; adb shell input keyevent 4; shot 04-home-one-note 3
adb shell "am start -n $ACT -a android.intent.action.SEND -t text/plain --es android.intent.extra.SUBJECT 'Meeting notes' --es android.intent.extra.TEXT 'Budget review and hiring plan'" > /dev/null
shot 05-shared-text-note 4
adb shell input keyevent 4; adb shell input keyevent 4; shot 06-home-two-notes 3
adb shell am start -n $ACT -a com.sek789102.jotwai.NEW_NOTE > /dev/null; shot 07-new-text-note 4
adb shell input text "Ideas"; adb shell input keyevent 66; adb shell input text "-%sfirst"; adb shell input keyevent 66; adb shell input text "second"
shot 08-text-note-list-continues 2
adb shell input keyevent 4; adb shell input keyevent 4; shot 09-home-three-notes 3

# Stress: random taps/swipes inside the app only.
adb shell monkey -p $PKG --pct-syskeys 0 --throttle 120 -s 7 400 > "$OUT/monkey.txt" 2>&1 || true
sleep 3
adb shell am start -n $ACT > /dev/null; shot 10-after-monkey 4

adb shell dumpsys package $PKG | grep -E "granted=|SCHEDULE_EXACT_ALARM" > "$OUT/permissions.txt" || true
adb logcat -d > "$OUT/logcat.txt"
if grep -E "FATAL EXCEPTION|ANR in $PKG" "$OUT/logcat.txt" | tee "$OUT/crashes.txt" | grep -q .; then
  grep -A40 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -120
  exit 1
fi
adb shell pidof $PKG > /dev/null || { echo "app not running"; exit 1; }
echo "OK: installed, flows ran, survived monkey test"
