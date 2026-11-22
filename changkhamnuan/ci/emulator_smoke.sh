#!/usr/bin/env bash
# Installs the (test-signed, minified) release APK, uses each calculator, adds a result to a
# new job, opens the quote share sheet, runs a random-tap test, and fails on any crash.
set -u
APK="$1"; OUT="$2"; PKG=com.sek789102.changkhamnuan; ACT="$PKG/.MainActivity"
mkdir -p "$OUT"
source "$(dirname "$0")/ui_helpers.sh"
adb wait-for-device
adb install -r "$APK" > "$OUT/install.txt" 2>&1 || { cat "$OUT/install.txt"; exit 1; }
adb logcat -c
adb shell am start -n $ACT > /dev/null; sleep 8; shot 01-home
tap_tag tab_prices; sleep 2; shot 01b-prices 1
tap_tag tab_more; sleep 2; shot 01c-more 1
tap_tag tab_projects; sleep 2; shot 01d-projects-empty 1
tap_tag tab_calc; sleep 2

for c in concrete footing piles stairs rebar rebar_grid steel_section formwork excavation fill cutfill retaining road drain tanks slope bricks plaster drywall paint tiles flooring ceiling roof excavation fill yard fence pipes; do
  if tap_tag_s "calc_$c" top; then sleep 2; shot "02-calc-$c" 1; adb shell input keyevent 4; sleep 1; fi
done

tap_tag tab_calc; sleep 1; tap_tag_s calc_bricks top; sleep 2
tap_tag in_length; adb shell input keyevent 123; for _ in 1 2 3; do adb shell input keyevent 67; done; adb shell input text "6"; adb shell input keyevent 111
tap_tag add_to_project; sleep 2; shot 03-add-dialog 1
tap_tag new_project_name; adb shell input text "Kitchen"; adb shell input keyevent 111
tap_tag create_and_add; sleep 3; shot 04-project 2
tap_tag tool_stages; sleep 2; tap_tag preset_0; sleep 1; tap_tag dialog_confirm; sleep 2; shot 04b-stages 1
tap_tag stage_invoice_1; sleep 1; tap_tag doc_image; sleep 4; shot 04e-stage-invoice 1; adb shell input keyevent 4; sleep 2; adb shell input keyevent 4; sleep 2
tap_tag tool_profit; sleep 2; shot 04c-costs 1; adb shell input keyevent 4; sleep 2
tap_tag tool_order; sleep 2; shot 04d-order 1; adb shell input keyevent 4; sleep 2
tap_tag_s share_image; sleep 2; shot 05a-profile-prompt 1; tap_tag share_anyway; sleep 4; shot 05-share-image 1
adb shell input keyevent 4; sleep 2
tap_tag_s share_pdf; sleep 2; tap_tag share_anyway; sleep 4; shot 06-share-pdf 1
adb shell input keyevent 4; sleep 2
tap_tag_s doc_boq; sleep 1; tap_tag share_image; sleep 2; tap_tag share_anyway; sleep 4; shot 06b-share-boq 1
adb shell input keyevent 4; sleep 2
adb shell am start -n $ACT > /dev/null; sleep 2

adb shell monkey -p $PKG --pct-syskeys 0 --throttle 120 -s 11 400 > "$OUT/monkey.txt" 2>&1 || true
sleep 2; adb shell am start -n $ACT > /dev/null; shot 07-after-monkey 3
adb logcat -d > "$OUT/logcat.txt"
if grep -E "FATAL EXCEPTION|ANR in $PKG" "$OUT/logcat.txt" | tee "$OUT/crashes.txt" | grep -q .; then
  grep -A40 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -120
  exit 1
fi
adb shell pidof $PKG > /dev/null || { echo "app not running"; exit 1; }
ls "$OUT"/*.png | wc -l
echo "OK"
