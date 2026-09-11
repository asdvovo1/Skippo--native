# Skippo — Android (Kotlin + Jetpack Compose)

A 1:1 native port of `index.html`. No WebView, no HTML, no JavaScript anywhere.
The canvas renderer, the physics, the audio graph and every UI panel were
re-written in Kotlin, keeping the original numbers byte for byte.

---

## Build the APK

### GitHub Codespaces (recommended)

Open the repo in a Codespace, then in the terminal:

```bash
bash setup.sh
./gradlew assembleDebug
```

The APK lands at:

```
app/build/outputs/apk/debug/app-debug.apk
```

Right click it in the Codespaces file explorer and choose **Download**.

`setup.sh` installs the Android command line tools, platform 34, build tools
34.0.0 and Gradle 8.7, then generates the Gradle wrapper. It only needs to run
once per Codespace.

### Android Studio

Open the folder, let it sync, then **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.

### Release build

```bash
./gradlew assembleRelease
```

The release variant is currently signed with the debug key so it installs
immediately. Swap in your own keystore in `app/build.gradle.kts` before
shipping to Play.

---

## Requirements

| | |
|---|---|
| JDK | 17 |
| compileSdk / targetSdk | 34 |
| minSdk | 24 (Android 7.0) |
| Kotlin | 1.9.24 |
| AGP | 8.5.2 |
| Compose BOM | 2024.06.00 |

---

## Project layout

```
app/src/main/java/com/skippo/game/
  Colors.kt        CSS custom properties, shade(), hsl2c(), clamp()
  Lang.kt          the full AR / EN string table
  Catalog.kt       skins, glows, trails, themes, perks, packs,
                   missions, achievements, worlds + the 100 item expansion
  Store.kt         localStorage -> SharedPreferences, same save shape
  Sfx.kt           the Web Audio graph as an AudioTrack synth
  Svg.kt           SVG path parser -> Compose Path
  Icons.kt         the 14 inline <svg> icons
  Fonts.kt         system-ui @ weight 900
  Engine.kt        the game loop, physics, spawning, collisions
  Draw.kt          the canvas renderer, draw call for draw call
  Widgets.kt       .btn, .curr, .pill, .toggle, .seg, .tabs, the logo, the score
  Ui.kt            .screen, .sheet, .list-item, .lb-item, .day-cell, .xp-wrap
  Screens.kt       the nine screens
  Shop.kt          the six shop tabs
  MainActivity.kt  fullscreen host + the frame loop
```

---

## How the 1:1 mapping works

**Geometry.** On Android Chrome one CSS pixel equals one dp, so every CSS number
became a `.dp` untouched. The canvas keeps the original virtual viewport:

```
SC = max(1, min(1.7, min(width/620, height/560)))
W  = width  / SC
H  = height / SC
```

The renderer then scales by `density * SC`, so the engine still thinks in the
same coordinate space the JavaScript used.

**Sharper than the web build.** The browser caps `devicePixelRatio` at 2. The
native canvas renders at full device density, so on a 3x or 4x screen the
geometry is identical but the edges are crisper.

**Colors.** Every hex value, every `shade()` amount, every `rgba()` alpha and
both gradients were copied literally. `globalCompositeOperation = 'lighter'`
became `BlendMode.Plus`.

**The logo.** `clamp(54px, 13.5vw, 118px)`, weight 900, `letter-spacing:-0.03em`,
`line-height:0.86`, the two stacked hard shadows at +10px and +16px, the `O`
rotated -6deg about its own inline-block box, and `floatY` as a 3s ease-in-out
reverse loop between 0 and -8px.

**Buttons.** `box-shadow: 0 9px 0` is a real offset box behind the content, and
`:active` animates `translateY(7px)` plus the shadow shrinking to 2px over 100ms.

**Audio.** Band-limited wavetables reproduce the Web Audio oscillators without
aliasing. The wind is a 2s white-noise loop through a 480Hz Q=0.7 bandpass
modulated by a 0.09Hz LFO, and the sequencer keeps `spb = 60/(play?130:100)/2`
with the same six scales and root notes.

**Saves.** `localStorage['skip_save_v21']` became a SharedPreferences entry with
the identical JSON shape, so an exported web save can be dropped straight in.

---

## What changed on purpose

| Web | Android |
|---|---|
| CrazyGames SDK ads | removed; revive is coin-only (`adAvailable()` was already `false`) |
| CrazyGames cloud save | local SharedPreferences |
| CrazyGames username / avatar | local profile |
| `navigator.vibrate` | `Vibrator` + `VibrationEffect` |
| `localStorage` | SharedPreferences |

Hooks for AdMob rewarded video and Play Games Services leaderboards drop into
`MainActivity.kt` where the revive and leaderboard callbacks already are.

---

## Verifying pixel parity

Codespaces has no KVM, so no emulator. To diff against the browser:

1. Screenshot the web build with Playwright at a fixed viewport.
2. Screenshot the Compose build with Roborazzi under Robolectric
   `@GraphicsMode(NATIVE)`.
3. `compare -metric SSIM web.png native.png diff.png`.

Final sign-off should always be on a real phone.
