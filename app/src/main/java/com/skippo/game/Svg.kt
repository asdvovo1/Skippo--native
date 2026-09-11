package com.skippo.game

import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Minimal but complete SVG path (`d` attribute) parser.
 *
 * The icons are copied verbatim out of index.html, so parsing the original
 * path data at runtime removes any chance of transcription drift.
 * Supports M m L l H h V v C c S s Q q T t A a Z z.
 */
object Svg {

	private class Reader(val s: String) {
		var i = 0
		fun skip() {
			while (i < s.length && (s[i] == ' ' || s[i] == ',' || s[i] == '\n' || s[i] == '\t' || s[i] == '\r')) i++
		}

		fun eof(): Boolean {
			skip(); return i >= s.length
		}

		fun peek(): Char {
			skip(); return s[i]
		}

		fun num(): Float {
			skip()
			val st = i
			if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
			while (i < s.length && (s[i].isDigit() || s[i] == '.')) {
				// a second '.' starts a new number (e.g. "1.5.5")
				if (s[i] == '.' && s.substring(st, i).contains('.')) break
				i++
			}
			if (i < s.length && (s[i] == 'e' || s[i] == 'E')) {
				i++
				if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
				while (i < s.length && s[i].isDigit()) i++
			}
			return s.substring(st, i).toFloatOrNull() ?: 0f
		}

		fun flag(): Boolean {
			skip()
			val c = s[i]; i++
			return c == '1'
		}
	}

	fun path(d: String): Path {
		val p = Path()
		val r = Reader(d)
		var cx = 0f
		var cy = 0f
		var sx = 0f
		var sy = 0f
		var lastCx = 0f
		var lastCy = 0f
		var lastQx = 0f
		var lastQy = 0f
		var cmd = ' '
		var prev = ' '

		while (!r.eof()) {
			val ch = r.peek()
			if (ch.isLetter()) {
				cmd = ch
				r.i++
			} else if (cmd == 'M') cmd = 'L' else if (cmd == 'm') cmd = 'l'

			when (cmd) {
				'M', 'm' -> {
					var x = r.num(); var y = r.num()
					if (cmd == 'm') { x += cx; y += cy }
					p.moveTo(x, y); cx = x; cy = y; sx = x; sy = y
				}
				'L', 'l' -> {
					var x = r.num(); var y = r.num()
					if (cmd == 'l') { x += cx; y += cy }
					p.lineTo(x, y); cx = x; cy = y
				}
				'H', 'h' -> {
					var x = r.num()
					if (cmd == 'h') x += cx
					p.lineTo(x, cy); cx = x
				}
				'V', 'v' -> {
					var y = r.num()
					if (cmd == 'v') y += cy
					p.lineTo(cx, y); cy = y
				}
				'C', 'c' -> {
					var x1 = r.num(); var y1 = r.num()
					var x2 = r.num(); var y2 = r.num()
					var x = r.num(); var y = r.num()
					if (cmd == 'c') { x1 += cx; y1 += cy; x2 += cx; y2 += cy; x += cx; y += cy }
					p.cubicTo(x1, y1, x2, y2, x, y)
					lastCx = x2; lastCy = y2; cx = x; cy = y
				}
				'S', 's' -> {
					var x2 = r.num(); var y2 = r.num()
					var x = r.num(); var y = r.num()
					if (cmd == 's') { x2 += cx; y2 += cy; x += cx; y += cy }
					val refl = prev == 'C' || prev == 'c' || prev == 'S' || prev == 's'
					val x1 = if (refl) 2 * cx - lastCx else cx
					val y1 = if (refl) 2 * cy - lastCy else cy
					p.cubicTo(x1, y1, x2, y2, x, y)
					lastCx = x2; lastCy = y2; cx = x; cy = y
				}
				'Q', 'q' -> {
					var x1 = r.num(); var y1 = r.num()
					var x = r.num(); var y = r.num()
					if (cmd == 'q') { x1 += cx; y1 += cy; x += cx; y += cy }
					p.quadraticBezierTo(x1, y1, x, y)
					lastQx = x1; lastQy = y1; cx = x; cy = y
				}
				'T', 't' -> {
					var x = r.num(); var y = r.num()
					if (cmd == 't') { x += cx; y += cy }
					val refl = prev == 'Q' || prev == 'q' || prev == 'T' || prev == 't'
					val x1 = if (refl) 2 * cx - lastQx else cx
					val y1 = if (refl) 2 * cy - lastQy else cy
					p.quadraticBezierTo(x1, y1, x, y)
					lastQx = x1; lastQy = y1; cx = x; cy = y
				}
				'A', 'a' -> {
					val rx = r.num(); val ry = r.num(); val rot = r.num()
					val large = r.flag(); val sweep = r.flag()
					var x = r.num(); var y = r.num()
					if (cmd == 'a') { x += cx; y += cy }
					arcTo(p, cx, cy, rx, ry, rot, large, sweep, x, y)
					cx = x; cy = y
				}
				'Z', 'z' -> {
					p.close(); cx = sx; cy = sy
				}
				else -> return p
			}
			prev = cmd
		}
		return p
	}

	/** endpoint -> centre parameterisation, per the SVG spec appendix. */
	private fun arcTo(
		p: Path, x0: Float, y0: Float,
		rx0: Float, ry0: Float, rotDeg: Float,
		large: Boolean, sweep: Boolean,
		x1: Float, y1: Float
	) {
		var rx = abs(rx0)
		var ry = abs(ry0)
		if (rx == 0f || ry == 0f) { p.lineTo(x1, y1); return }
		val phi = rotDeg * PI.toFloat() / 180f
		val cosP = cos(phi.toDouble()); val sinP = sin(phi.toDouble())
		val dx2 = (x0 - x1) / 2.0; val dy2 = (y0 - y1) / 2.0
		val x1p = cosP * dx2 + sinP * dy2
		val y1p = -sinP * dx2 + cosP * dy2
		var rxd = rx.toDouble(); var ryd = ry.toDouble()
		val lam = (x1p * x1p) / (rxd * rxd) + (y1p * y1p) / (ryd * ryd)
		if (lam > 1.0) { val s = sqrt(lam); rxd *= s; ryd *= s }
		var num = rxd * rxd * ryd * ryd - rxd * rxd * y1p * y1p - ryd * ryd * x1p * x1p
		if (num < 0.0) num = 0.0
		val den = rxd * rxd * y1p * y1p + ryd * ryd * x1p * x1p
		val co = (if (large != sweep) 1.0 else -1.0) * sqrt(if (den == 0.0) 0.0 else num / den)
		val cxp = co * rxd * y1p / ryd
		val cyp = -co * ryd * x1p / rxd
		val cxc = cosP * cxp - sinP * cyp + (x0 + x1) / 2.0
		val cyc = sinP * cxp + cosP * cyp + (y0 + y1) / 2.0

		fun ang(ux: Double, uy: Double, vx: Double, vy: Double): Double {
			val dot = ux * vx + uy * vy
			val len = sqrt(ux * ux + uy * uy) * sqrt(vx * vx + vy * vy)
			var a = acos((dot / len).coerceIn(-1.0, 1.0))
			if (ux * vy - uy * vx < 0) a = -a
			return a
		}

		val ux = (x1p - cxp) / rxd; val uy = (y1p - cyp) / ryd
		val vx = (-x1p - cxp) / rxd; val vy = (-y1p - cyp) / ryd
		val theta1 = ang(1.0, 0.0, ux, uy)
		var dTheta = ang(ux, uy, vx, vy)
		if (!sweep && dTheta > 0) dTheta -= 2 * PI
		if (sweep && dTheta < 0) dTheta += 2 * PI

		val segs = ceil(abs(dTheta) / (PI / 2)).toInt().coerceAtLeast(1)
		val delta = dTheta / segs
		val t = 4.0 / 3.0 * kotlin.math.tan(delta / 4.0)
		var th = theta1
		for (s in 0 until segs) {
			val cosT1 = cos(th); val sinT1 = sin(th)
			val th2 = th + delta
			val cosT2 = cos(th2); val sinT2 = sin(th2)
			fun map(a: Double, b: Double): Pair<Float, Float> =
				Pair(
					(cosP * rxd * a - sinP * ryd * b + cxc).toFloat(),
					(sinP * rxd * a + cosP * ryd * b + cyc).toFloat()
				)
			val c1 = map(cosT1 - t * sinT1, sinT1 + t * cosT1)
			val c2 = map(cosT2 + t * sinT2, sinT2 - t * cosT2)
			val e = map(cosT2, sinT2)
			p.cubicTo(c1.first, c1.second, c2.first, c2.second, e.first, e.second)
			th = th2
		}
	}
}
