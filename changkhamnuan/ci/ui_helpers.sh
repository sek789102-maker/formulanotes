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
