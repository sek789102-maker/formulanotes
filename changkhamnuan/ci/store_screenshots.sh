#!/usr/bin/env bash
# Store screenshots with sample data (debug-only receiver fills prices, shop and one job).
set -u
APK="$1"; OUT="$2"; PKG=com.sek789102.changkhamnuan; ACT="$PKG/.MainActivity"
mkdir -p "$OUT"
source "$(dirname "$0")/ui_helpers.sh"
adb uninstall $PKG > /dev/null 2>&1 || true
adb install -r "$APK" > /dev/null || exit 1
adb shell cmd uimode night no
adb shell am start -n $ACT > /dev/null; sleep 6
adb shell am broadcast -n "$PKG/.DebugSeedReceiver" -a com.sek789102.changkhamnuan.DEBUG_SEED > /dev/null; sleep 3
adb shell am force-stop $PKG; adb shell cmd statusbar collapse; adb shell am start -n $ACT > /dev/null
shot 1-calculators 6
tap_tag calc_tiles; sleep 2; adb shell input swipe 540 1500 540 500 300; shot 2-tiles-result 2
adb shell input keyevent 4; sleep 1
tap_tag calc_bricks; sleep 2; adb shell input swipe 540 1500 540 400 300; shot 3-bricks-result 2
adb shell input keyevent 4; sleep 1
tap_tag tab_projects; sleep 2; shot 4-projects 1
adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1; adb pull /sdcard/ui.xml /tmp/ui.xml > /dev/null 2>&1
PID=$(python3 -c "import re;print((re.findall(r'resource-id=\"project_(\d+)\"', open('/tmp/ui.xml',encoding='utf-8').read()) or ['1'])[0])")
tap_tag "project_$PID"; sleep 2; shot 5-project 1
adb shell input swipe 540 1600 540 300 300; sleep 1; adb shell input swipe 540 1600 540 300 300; shot 6-totals 1
tap_tag share_image; sleep 5; shot 7-share 1
adb shell input keyevent 4; sleep 1; adb shell input keyevent 4; sleep 1
adb shell am start -n $ACT > /dev/null; sleep 1
tap_tag tab_prices; sleep 2; shot 8-prices 1
tap_tag tab_more; sleep 1; tap_tag nav_converter; sleep 1; shot 9-converter 1
adb shell input keyevent 4; sleep 1
# The quote image itself, as sent to customers
adb shell "run-as $PKG sh -c 'ls cache/quotes'" > "$OUT/quote-files.txt" 2>&1 || true
Q=$(adb shell "run-as $PKG sh -c 'ls cache/quotes/*.png'" 2>/dev/null | tr -d '\r' | head -1)
[ -n "$Q" ] && adb exec-out run-as $PKG cat "$Q" > "$OUT/quote-sample.png"
echo "screenshots done"
