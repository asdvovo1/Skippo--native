#!/usr/bin/env bash
# Turn ONE png into every launcher icon Android needs.
#
#   bash tools/make-icon.sh my-icon.png
#
# Best input: a square png, 1024x1024, no rounded corners (Android crops it).
set -euo pipefail

SRC="${1:-}"
if [ -z "$SRC" ] || [ ! -f "$SRC" ]; then
	echo "usage: bash tools/make-icon.sh <icon.png>"
	exit 1
fi
SRC="$(readlink -f "$SRC")"
cd "$(dirname "$0")/.."
RES="app/src/main/res"

if ! command -v magick >/dev/null 2>&1 && ! command -v convert >/dev/null 2>&1; then
	echo "==> installing imagemagick"
	sudo apt-get update -qq && sudo apt-get install -y -qq imagemagick
fi
IM="$(command -v magick || command -v convert)"

# 1) legacy square + round icons
for pair in "48 mdpi" "72 hdpi" "96 xhdpi" "144 xxhdpi" "192 xxxhdpi"; do
	set -- $pair
	size="$1"; dir="$RES/mipmap-$2"
	mkdir -p "$dir"
	"$IM" "$SRC" -resize "${size}x${size}^" -gravity center -extent "${size}x${size}" \
		-strip "PNG32:$dir/ic_launcher.png"
	cp "$dir/ic_launcher.png" "$dir/ic_launcher_round.png"
done

# 2) adaptive foreground (Android 8+): 108dp canvas, art kept inside the 72dp safe zone
for pair in "108 mdpi" "162 hdpi" "216 xhdpi" "324 xxhdpi" "432 xxxhdpi"; do
	set -- $pair
	size="$1"; dir="$RES/mipmap-$2"
	inner=$(( size * 2 / 3 ))
	"$IM" "$SRC" -resize "${inner}x${inner}" -background none -gravity center \
		-extent "${size}x${size}" -strip "PNG32:$dir/ic_launcher_foreground.png"
done

# 3) make the adaptive icon use the new foreground
mkdir -p "$RES/mipmap-anydpi-v26"
cat > "$RES/mipmap-anydpi-v26/ic_launcher.xml" <<'XML'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
	<background android:drawable="@color/ic_launcher_background" />
	<foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
XML

# 4) 512x512 for the Play Store listing (not packed into the apk)
"$IM" "$SRC" -resize "512x512^" -gravity center -extent "512x512" -strip \
	"PNG32:ic_launcher-playstore.png"

echo
echo "done."
echo "  background colour -> app/src/main/res/values/colors.xml  (ic_launcher_background)"
echo "  rebuild           -> ./gradlew assembleDebug"
