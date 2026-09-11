package com.skippo.game

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 1:1 port of the CSS custom properties in :root of index.html.
 * Do not "improve" these values - they are the reference palette.
 */
object C {
	val menu1 = Color(0xFF4936A6)
	val menu2 = Color(0xFF2C1F66)
	val ink = Color(0xFF2A2440)
	val inkDim = Color(0xFF6B6480)
	val white = Color(0xFFFBFAF6)
	val card = Color(0xFFF7F4EE)
	val cardD = Color(0xFFD6CFBF)
	val accent = Color(0xFFFF5A3C)
	val accentD = Color(0xFFCC3D24)
	val green = Color(0xFF39C07A)
	val greenD = Color(0xFF2A9860)
	val gold = Color(0xFFFFC93C)
	val goldD = Color(0xFFE0A020)
	val gem = Color(0xFF4AD4E0)
	val gemD = Color(0xFF2BA0AC)
	val trackC = Color(0xFFC9C2D6)
	val trackB = Color(0xFF8B83A0)

	// logo shadow colour, hard coded in .logo
	val logoShadow = Color(0xFF2F4D7A)
}

/** --ease: cubic-bezier(0.22, 1, 0.36, 1) */
val Ease = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/** CSS `ease-in-out` keyword == cubic-bezier(0.42, 0, 0.58, 1) */
val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

/** CSS `ease` keyword == cubic-bezier(0.25, 0.1, 0.25, 1) */
val EaseDefault = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

/** CSS `linear` */
fun hex(v: Long): Color = Color(v or 0xFF000000L)

/**
 * Port of shade(hex, amt) from index.html.
 * amt > 0 lightens toward white, amt < 0 darkens toward black.
 */
fun shade(c: Color, amt: Float): Color {
	var r = c.red * 255f
	var g = c.green * 255f
	var b = c.blue * 255f
	if (amt > 0f) {
		r += (255f - r) * amt
		g += (255f - g) * amt
		b += (255f - b) * amt
	} else {
		r *= (1f + amt)
		g *= (1f + amt)
		b *= (1f + amt)
	}
	return Color(
		r.roundToInt().coerceIn(0, 255),
		g.roundToInt().coerceIn(0, 255),
		b.roundToInt().coerceIn(0, 255)
	)
}

/** Port of hsl2hex(h, s, l) from index.html (the catalogue colour generator). */
fun hsl2c(h: Double, s: Double, l: Double): Color {
	val sa = s / 100.0
	val li = l / 100.0
	val a = sa * min(li, 1.0 - li)
	fun k(n: Double) = (n + h / 30.0) % 12.0
	fun f(n: Double) = li - a * max(-1.0, min(min(k(n) - 3.0, 9.0 - k(n)), 1.0))
	fun two(v: Double) = (255.0 * v).roundToInt().coerceIn(0, 255)
	return Color(two(f(0.0)), two(f(8.0)), two(f(4.0)))
}

/**
 * Reproduces `linear-gradient(<angle>deg, ...)` exactly as CSS defines it:
 * 0deg points up, angles run clockwise, and the gradient line is sized so the
 * corners of the box land on the 0% / 100% stops.
 */
fun cssLinearGradient(angleDeg: Float, colors: List<Color>, size: Size): Brush {
	val a = Math.toRadians(angleDeg.toDouble())
	val w = size.width
	val h = size.height
	val len = abs(w * sin(a)) + abs(h * cos(a))
	val cx = w / 2f
	val cy = h / 2f
	val dx = (sin(a) * len / 2.0).toFloat()
	val dy = (-cos(a) * len / 2.0).toFloat()
	return Brush.linearGradient(
		colors = colors,
		start = Offset(cx - dx, cy - dy),
		end = Offset(cx + dx, cy + dy)
	)
}

/** CSS clamp(min, preferred, max) */
fun clampCss(minV: Float, pref: Float, maxV: Float): Float = min(max(pref, minV), maxV)
