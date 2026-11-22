# Shared adb helpers: tap an element by its Compose test tag, take a screenshot.
shot() { sleep "${2:-2}"; adb exec-out screencap -p > "$OUT/$1.png"; }
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
# Like tap_tag but scrolls the list down until the element appears (lazy lists only compose what
# is visible). One UI dump per step keeps it fast. Starts from the top of the list.
find_tag() {
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/ui.xml > /dev/null 2>&1
  python3 - "$1" <<'PY2'
import re, sys
xml = open('/tmp/ui.xml', encoding='utf-8').read()
m = re.search(r'resource-id="%s"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"' % re.escape(sys.argv[1]), xml)
if m:
    a, b, c, d = map(int, m.groups())
    if d - b > 20: print((a + c) // 2, (b + d) // 2)
PY2
}
scroll_top() { for _ in 1 2 3 4 5 6 7 8; do adb shell input swipe 540 500 540 1600 120; done; sleep 0.5; }
tap_tag_s() {
  [ "${2:-}" = "top" ] && scroll_top
  for _ in $(seq 1 16); do
    xy=$(find_tag "$1")
    if [ -n "$xy" ]; then adb shell input tap $xy; return 0; fi
    adb shell input swipe 540 1450 540 750 250; sleep 0.6
  done
  echo "tag not found after scrolling: $1"; return 1
}
