#!/usr/bin/env bash
# Store screenshots: installs the debug APK, loads sample notes (debug-only receiver)
# and captures the main screens. Elements are found by their test tags.
set -u
APK="$1"; OUT="$2"; PKG=com.sek789102.jotwai; ACT="$PKG/.MainActivity"
mkdir -p "$OUT"
adb uninstall $PKG > /dev/null 2>&1 || true
adb install -r "$APK" > /dev/null || exit 1
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb shell appops set $PKG SCHEDULE_EXACT_ALARM allow || true
adb shell settings put system time_12_24 24 || true

tap_tag() {
  for _ in 1 2 3; do
    adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
    adb pull /sdcard/ui.xml /tmp/ui.xml > /dev/null 2>&1
    xy=$(python3 - "$1" <<'PY'
import re, sys
xml = open('/tmp/ui.xml', encoding='utf-8').read()
m = re.search(r'resource-id="%s"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"' % re.escape(sys.argv[1]), xml)
if m:
    a, b, c, d = map(int, m.groups()); print((a + c) // 2, (b + d) // 2)
PY
)
    if [ -n "$xy" ]; then adb shell input tap $xy; return 0; fi
    sleep 1
  done
  echo "tag not found: $1"; return 1
}
shot() { sleep "${2:-2}"; adb exec-out screencap -p > "$OUT/$1.png"; }
open_note() { adb shell am start -n $ACT -a com.sek789102.jotwai.OPEN_NOTE -d "jotwai://note/$1" --el note_id "$1" > /dev/null; }

home() { adb shell input keyevent 4; sleep 1; adb shell am start -n $ACT > /dev/null; sleep 2; }
adb shell cmd uimode night no
adb shell am start -n $ACT > /dev/null; sleep 6
adb shell am broadcast -n "$PKG/.DebugSeedReceiver" -a com.sek789102.jotwai.DEBUG_SEED > /dev/null; sleep 3
adb shell am force-stop $PKG; adb shell cmd statusbar collapse; adb shell am start -n $ACT > /dev/null
shot 1-home 6

open_note 2; sleep 3; adb shell input keyevent 111; tap_tag editor_format; shot 2-formatted-note 2
tap_tag editor_reminder; shot 5-reminder 2; adb shell input keyevent 4; sleep 1
tap_tag editor_color; shot 6-colors 2; adb shell input keyevent 4; sleep 1
home

open_note 1; sleep 3; adb shell input keyevent 111; shot 3-checklist 2
home

tap_tag open_drawer; shot 4-drawer 2; adb shell input keyevent 4; sleep 1
adb shell am start -n $ACT > /dev/null; sleep 1

tap_tag search_input; adb shell input text "tom"; sleep 1; adb shell input keyevent 111; shot 7-search 2
tap_tag search_clear; sleep 1

adb shell cmd uimode night yes; sleep 3
adb shell am force-stop $PKG; adb shell cmd statusbar collapse; adb shell am start -n $ACT > /dev/null
shot 8-home-dark 6
adb shell cmd uimode night no
echo "screenshots done"
