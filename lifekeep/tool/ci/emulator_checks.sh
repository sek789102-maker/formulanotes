#!/usr/bin/env bash
# Runs inside reactivecircus/android-emulator-runner (see workflow).
# Usage: emulator_checks.sh <smoke.apk> <production-release.apk> <out-dir>
set -uo pipefail
SMOKE_APK="$1"; PROD_APK="$2"; OUT="$3"
PKG=com.sek789102.lifekeep
ACT=com.sek789102.lifekeep.MainActivity
mkdir -p "$OUT"
REPORT="$OUT/device-report.md"
fails=0
ok()   { echo "- ✅ $1" | tee -a "$REPORT"; }
bad()  { echo "- ❌ $1" | tee -a "$REPORT"; fails=$((fails+1)); }

adb wait-for-device
{
  echo "# On-device verification"
  echo
  echo "- Device: $(adb shell getprop ro.product.model | tr -d '\r'), Android $(adb shell getprop ro.build.version.release | tr -d '\r') (API $(adb shell getprop ro.build.version.sdk | tr -d '\r'))"
  echo "- Date: $(date -u +%Y-%m-%dT%H:%MZ)"
  echo
  echo "## 1. Integration tests (debug build, real Android storage/plugins)"
} > "$REPORT"

( cd lifekeep && timeout 900 flutter test integration_test -d emulator-5554 --reporter expanded ) > "$OUT/integration_test.log" 2>&1 || echo "integration_test exited with $? (124 = timed out after 15 min)" >> "$OUT/integration_test.log"
if grep -q "All tests passed" "$OUT/integration_test.log"; then ok "integration_test: $(grep -oE '\+[0-9]+' "$OUT/integration_test.log" | tail -1 | tr -d '+') tests passed"; else bad "integration_test failed (see integration_test.log)"; fi
adb uninstall "$PKG" >/dev/null 2>&1 || true

echo -e "\n## 2. Release build (R8) functional check across a process restart" >> "$REPORT"
adb install -r "$SMOKE_APK" > "$OUT/install_smoke.log" 2>&1 && ok "release smoke APK installed" || bad "smoke APK install failed"
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS || true
adb shell appops set "$PKG" SCHEDULE_EXACT_ALARM allow || true
adb logcat -c
adb shell am start -W -n "$PKG/$ACT" > /dev/null
for i in $(seq 1 90); do adb logcat -d -s flutter:I | grep -qE "LKSMOKE (DONE PHASE 1|FAIL)" && break; sleep 1; done
adb logcat -d -s flutter:I | grep LKSMOKE > "$OUT/smoke_phase1.log"
grep -q "DONE PHASE 1" "$OUT/smoke_phase1.log" && ok "phase 1: $(grep -c PASS "$OUT/smoke_phase1.log") checks passed" || bad "phase 1 failed: $(grep -m1 FAIL "$OUT/smoke_phase1.log")"
grep "INFO" "$OUT/smoke_phase1.log" | sed 's/.*INFO/- ℹ️/' >> "$REPORT"

# Background the app and wait for the scheduled notification (~70 s).
adb shell input keyevent KEYCODE_HOME
for i in $(seq 1 120); do adb shell dumpsys notification --noredact | grep -q "LIFEKEEP smoke reminder" && break; sleep 2; done
adb shell dumpsys notification --noredact | grep -q "LIFEKEEP smoke reminder" && ok "scheduled notification delivered by Android" || bad "scheduled notification not delivered within 4 min"

# Kill the process (like the system would) WITHOUT clearing alarms, then relaunch.
adb shell am kill "$PKG"; sleep 2
adb logcat -c
adb shell am start -W -n "$PKG/$ACT" > /dev/null
for i in $(seq 1 60); do adb logcat -d -s flutter:I | grep -qE "LKSMOKE (DONE PHASE 2|FAIL)" && break; sleep 1; done
adb logcat -d -s flutter:I | grep LKSMOKE > "$OUT/smoke_phase2.log"
grep -q "DONE PHASE 2" "$OUT/smoke_phase2.log" && ok "phase 2 (after restart): $(grep -c PASS "$OUT/smoke_phase2.log") checks passed" || bad "phase 2 failed: $(grep -m1 FAIL "$OUT/smoke_phase2.log")"
adb logcat -d | grep -E "FATAL EXCEPTION|AndroidRuntime: " > "$OUT/smoke_crashes.log" || true
[ -s "$OUT/smoke_crashes.log" ] && bad "crash in smoke build (smoke_crashes.log)" || ok "no Java/Kotlin crash in release smoke build"
adb uninstall "$PKG" >/dev/null 2>&1 || true

echo -e "\n## 3. Production release APK launch" >> "$REPORT"
adb install "$PROD_APK" > "$OUT/install_prod.log" 2>&1 && ok "production release APK installed" || bad "production APK install failed"
adb logcat -c
adb shell am start -W -n "$PKG/$ACT" > "$OUT/launch.log"
sleep 10
pid=$(adb shell pidof "$PKG" | tr -d '\r')
[ -n "$pid" ] && ok "app running 10 s after launch (pid $pid)" || bad "app not running after launch"
adb exec-out screencap -p > "$OUT/release_launch.png"
adb logcat -d | grep -E "FATAL EXCEPTION|E/flutter" > "$OUT/release_errors.log" || true
[ -s "$OUT/release_errors.log" ] && bad "errors in logcat after launch (release_errors.log)" || ok "no crash / Flutter error in logcat"
grep -E "TotalTime" "$OUT/launch.log" | sed 's/^/- ℹ️ cold start /' >> "$REPORT"
# Relaunch after process death keeps data (DB opened from disk)
adb shell input keyevent KEYCODE_HOME; adb shell am kill "$PKG"; sleep 2
adb shell am start -W -n "$PKG/$ACT" > /dev/null; sleep 6
[ -n "$(adb shell pidof "$PKG" | tr -d '\r')" ] && ok "relaunch after process death OK" || bad "relaunch failed"
adb exec-out screencap -p > "$OUT/release_relaunch.png"

echo -e "\n**Result: $([ $fails -eq 0 ] && echo 'ALL CHECKS PASSED' || echo "$fails CHECK(S) FAILED")**" >> "$REPORT"
cat "$REPORT"
exit $fails
