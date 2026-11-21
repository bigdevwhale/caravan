#!/usr/bin/env bash
# Screenshots from the demo build (app.caravan.demo, central Moscow) into docs/screenshots/<lang>/.
#   ANDROID_SERIAL=<phone> tools/capture.sh ru|en
set -e
LANG_=${1:-ru}
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Android/Android Studio/jbr}"
./gradlew -q :app:assembleDebug -Pdemo -Pshots -Pdemolang="$LANG_"
PKG=app.caravan.demo
adb install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null
adb shell svc power stayon usb
OUT=docs/screenshots/$LANG_; mkdir -p "$OUT"
shot() { # name tab [open]
  adb shell pm clear $PKG >/dev/null
  if [ -n "$3" ]; then adb shell am start -n $PKG/app.caravan.MainActivity --ei tab "$2" --es open "$3" >/dev/null
  else adb shell am start -n $PKG/app.caravan.MainActivity --ei tab "$2" >/dev/null; fi
  sleep 7
  # the demo switches its language once on first start: start again so the tab and sheet apply
  if [ -n "$3" ]; then adb shell am start -S -n $PKG/app.caravan.MainActivity --ei tab "$2" --es open "$3" >/dev/null
  else adb shell am start -S -n $PKG/app.caravan.MainActivity --ei tab "$2" >/dev/null; fi
  sleep 6
  adb exec-out screencap -p > "$OUT/$1.png"
  echo "$OUT/$1.png"
}
shot 1-gate 0
shot 2-market 0 market
shot 3-house 1
shot 4-map 2
shot 5-merchant 0 merchant:old
shot 6-curios 3 curios
shot 7-trip 3 trip
shot 8-guild 4
adb shell svc power stayon false
