package com.skippo.game

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/** wrapv(v, m) */
fun wrapv(v: Float, m: Float): Float {
	var x = v % m
	if (x < 0) x += m
	return x
}

/** hsh(n) - deterministic pseudo random used by the scenery generators */
fun hsh(n: Float): Float {
	val x = sin(n.toDouble() * 127.1 + 311.7) * 43758.5453
	return (x - floor(x)).toFloat()
}

private fun rrPath(x: Float, y: Float, w: Float, h: Float, r: Float): Path {
	val p = Path()
	val rad = min(r, min(abs(w) / 2f, abs(h) / 2f))
	p.addRoundRect(
		androidx.compose.ui.geometry.RoundRect(
			Rect(x, y, x + w, y + h), CornerRadius(rad, rad)
		)
	)
	return p
}

/**
 * Complete port of the canvas renderer.
 * Every draw call happens in css-pixel space; the caller applies the SC / density scale.
 */
class Renderer(val e: Engine, val S: Store) {

	private val W get() = e.W
	private val H get() = e.H
	private val st get() = e.st
	private val p get() = e.p

	fun DrawScope.rrFill(x: Float, y: Float, w: Float, h: Float, r: Float, c: Color) {
		drawPath(rrPath(x, y, w, h, r), c)
	}

	fun DrawScope.rrStroke(x: Float, y: Float, w: Float, h: Float, r: Float, c: Color, sw: Float) {
		drawPath(rrPath(x, y, w, h, r), c, style = Stroke(sw))
	}

	// ------------------------------------------------------------------

	fun DrawScope.drawAll() {
		var dx = 0f
		var dy = 0f
		if (e.shake > 0.25f) {
			dx = (kotlin.random.Random.nextFloat() - 0.5f) * e.shake
			dy = (kotlin.random.Random.nextFloat() - 0.5f) * e.shake
		}
		translate(dx, dy) {
			drawSky()
			drawFar()
			drawMid()
			drawAmbient()
			drawG()
			drawShadow()
			drawGlowTrail()
			drawTrail()
			drawO()
			drawC()
			drawPows()
			drawPts()
			drawPlayer()
			drawFlo()
			drawNear()
		}
	}

	// ---------------- sky ----------------

	private fun DrawScope.drawSky() {
		val th = e.activeWorld()
		val b = e.bioKey()
		drawRect(
			Brush.verticalGradient(listOf(th.top!!, th.bot!!), 0f, H),
			Offset.Zero, Size(W, H)
		)
		if (b == "space") {
			val sx = W * 0.76f; val sy = H * 0.2f; val R = min(W, H) * 0.1f
			drawCircle(Color(0xFF6A4FD0), R, Offset(sx, sy))
			drawCircle(Color(0x24FFFFFF), R * 0.55f, Offset(sx - R * 0.32f, sy - R * 0.3f))
			rotate(-0.38f * 180f / PI.toFloat(), Offset(sx, sy)) {
				drawOval(
					Color(0x52FFFFFF),
					Offset(sx - R * 1.75f, sy - R * 0.4f),
					Size(R * 3.5f, R * 0.8f),
					style = Stroke(max(2f, R * 0.09f))
				)
			}
		} else if (b == "ocean") {
			for (i in 0 until 5) {
				val x = wrapv(st.dist * 0.02f + i * W * 0.28f, W * 1.5f) - W * 0.25f
				val pth = Path()
				pth.moveTo(x, 0f)
				pth.lineTo(x + W * 0.08f, 0f)
				pth.lineTo(x + W * 0.2f, H)
				pth.lineTo(x + W * 0.04f, H)
				pth.close()
				drawPath(pth, Color.White, alpha = 0.1f)
			}
		} else {
			val sx = W * 0.8f; val sy = H * 0.16f; val R = min(W, H) * 0.07f
			val tint = if (b == "city") Color(0xFFFFE2AA) else Color(0xFFFFFACD)
			drawRect(
				Brush.radialGradient(
					0f to tint.copy(alpha = 0.9f),
					0.3f to tint.copy(alpha = 0.4f),
					1f to tint.copy(alpha = 0f),
					center = Offset(sx, sy),
					radius = R * 3.6f
				),
				Offset(sx - R * 3.8f, sy - R * 3.8f), Size(R * 7.6f, R * 7.6f)
			)
			drawCircle(shade(th.top, 0.8f), R, Offset(sx, sy))
		}
		if (b != "space" && b != "ocean") drawClouds(b)
	}

	private fun DrawScope.drawClouds(b: String) {
		val span = W + 420f
		val off = wrapv(st.dist * 0.05f, span)
		val col = if (b == "city") Color(0x6BFFFFFF) else Color(0xDBFFFFFF)
		val cn = if (H > W * 1.25f) 10 else 7
		val sky = H - e.gh()
		for (i in 0 until cn) {
			val x = wrapv(i * (span / cn) - off, span) - 140f
			val y = H * 0.05f + sky * 0.74f * hsh(i * 3.7f)
			val s = (0.6f + hsh(i + 9f) * 0.7f) * min(W, H) * 0.05f
			val pth = Path()
			pth.addOval(Rect(x - s, y - s, x + s, y + s))
			pth.addOval(Rect(x + s * 0.95f - s * 0.7f, y + s * 0.2f - s * 0.7f, x + s * 0.95f + s * 0.7f, y + s * 0.2f + s * 0.7f))
			pth.addOval(Rect(x - s * 0.9f - s * 0.58f, y + s * 0.24f - s * 0.58f, x - s * 0.9f + s * 0.58f, y + s * 0.24f + s * 0.58f))
			pth.addOval(Rect(x + s * 0.08f - s * 0.6f, y - s * 0.52f - s * 0.6f, x + s * 0.08f + s * 0.6f, y - s * 0.52f + s * 0.6f))
			drawPath(pth, col)
		}
	}

	// ---------------- scenery helpers ----------------

	private fun DrawScope.hillBand(off: Float, baseY: Float, amp: Float, freq: Float, color: Color, ph: Float) {
		val pth = Path()
		pth.moveTo(0f, baseY)
		var x = 0f
		while (x <= W + 14f) {
			val t = (x + off) * freq + ph
			val y = baseY - amp * (0.5f + 0.4f * sin(t.toDouble()).toFloat() + 0.16f * sin(t * 2.4).toFloat())
			pth.lineTo(x, y)
			x += 14f
		}
		pth.lineTo(W, baseY); pth.lineTo(W, H); pth.lineTo(0f, H); pth.close()
		drawPath(pth, color)
	}

	private fun DrawScope.skyline(off: Float, baseY: Float, maxH: Float, color: Color, win: Color?) {
		val tw = max(46f, W * 0.055f)
		val n = ceil(W / tw).toInt() + 2
		val start = floor(off / tw).toInt()
		val hs = FloatArray(n)
		for (i in 0 until n) {
			val idx = start + i
			val h = maxH * (0.32f + hsh(idx * 1.7f) * 0.68f)
			hs[i] = h
			drawRect(color, Offset(idx * tw - off, baseY - h), Size(tw - 7f, h + 6f))
		}
		if (win != null) {
			for (i in 0 until n) {
				val idx = start + i
				val x = idx * tw - off
				val h = hs[i]
				var wy = baseY - h + 16f
				while (wy < baseY - 14f) {
					var wx = x + 9f
					while (wx < x + tw - 20f) {
						if (hsh(idx * 31.3f + wy * 0.7f + wx * 0.3f) > 0.5f) {
							drawRect(win, Offset(wx, wy), Size(7f, 9f))
						}
						wx += 17f
					}
					wy += 22f
				}
			}
		}
	}

	private fun DrawScope.sceneryRow(off: Float, step: Float, baseY: Float, fn: DrawScope.(Float, Float, Int) -> Unit) {
		val n = ceil(W / step).toInt() + 2
		val start = floor(off / step).toInt()
		for (i in 0 until n) {
			val idx = start + i
			fn(idx * step - off + step * 0.45f * hsh(idx * 5.3f), baseY, idx)
		}
	}

	private fun DrawScope.tree(x: Float, gy: Float, idx: Int, col: Color, col2: Color, sc: Float) {
		val h = (46f + hsh(idx.toFloat()) * 34f) * sc
		drawRect(col2, Offset(x - 3.5f * sc, gy - h * 0.52f), Size(7f * sc, h * 0.55f))
		val pth = Path()
		pth.addOval(Rect(x - h * 0.3f, gy - h * 0.72f - h * 0.3f, x + h * 0.3f, gy - h * 0.72f + h * 0.3f))
		pth.addOval(Rect(x - h * 0.21f - h * 0.24f, gy - h * 0.52f - h * 0.24f, x - h * 0.21f + h * 0.24f, gy - h * 0.52f + h * 0.24f))
		pth.addOval(Rect(x + h * 0.21f - h * 0.24f, gy - h * 0.52f - h * 0.24f, x + h * 0.21f + h * 0.24f, gy - h * 0.52f + h * 0.24f))
		drawPath(pth, col)
	}

	private fun DrawScope.cactus(x: Float, gy: Float, idx: Int, col: Color) {
		val h = 40f + hsh(idx.toFloat()) * 30f
		rrFill(x - 6f, gy - h, 12f, h + 4f, 6f, col)
		if (hsh(idx + 3.3f) > 0.4f) {
			rrFill(x - 19f, gy - h * 0.66f, 9f, h * 0.36f, 4f, col)
			rrFill(x - 19f, gy - h * 0.66f, 20f, 8f, 4f, col)
		}
	}

	private fun DrawScope.bush(x: Float, gy: Float, idx: Int, col: Color) {
		val r = 11f + hsh(idx.toFloat()) * 11f
		val pth = Path()
		pth.addOval(Rect(x - r, gy - r * 0.45f - r, x + r, gy - r * 0.45f + r))
		pth.addOval(Rect(x - r * 0.85f - r * 0.68f, gy - r * 0.05f - r * 0.68f, x - r * 0.85f + r * 0.68f, gy - r * 0.05f + r * 0.68f))
		pth.addOval(Rect(x + r * 0.85f - r * 0.68f, gy - r * 0.05f - r * 0.68f, x + r * 0.85f + r * 0.68f, gy - r * 0.05f + r * 0.68f))
		drawPath(pth, col)
	}

	private fun DrawScope.rock(x: Float, gy: Float, idx: Int, col: Color) {
		val r = 8f + hsh(idx.toFloat()) * 10f
		val pth = Path()
		pth.moveTo(x - r, gy)
		pth.lineTo(x - r * 0.45f, gy - r * 0.95f)
		pth.lineTo(x + r * 0.35f, gy - r)
		pth.lineTo(x + r, gy)
		pth.close()
		drawPath(pth, col)
	}

	private fun DrawScope.turf(x: Float, gy: Float, idx: Int, col: Color) {
		val h = 12f + hsh(idx.toFloat()) * 13f
		for (k in 0 until 3) {
			val bx = x + (k - 1) * 7f
			drawLine(col, Offset(bx, gy), Offset(bx + (k - 1) * 5f, gy - h), 3.5f, StrokeCap.Round)
		}
	}

	private fun DrawScope.drawFar() {
		val th = e.activeWorld(); val b = e.bioKey(); val gy = H - e.gh() + 10f
		when (b) {
			"stadium" -> {
				stands(st.dist * 0.16f, gy, th)
				// الشماريخ وأنوار الليزر بتضرب في ملاعب VIP بس
				if (e.vipArena()) {
					lasers(gy, th)
					flares(gy, th)
					// طبقة تهدية: بتخفف الوهج والزحمة ورا اللعب
					calmVip(gy)
				}
				// شعار النادي أو المنتخب على المدرج
				crestOnStands(st.dist * 0.16f, gy, th, S.theme)
			}
			"city" -> skyline(st.dist * 0.14f, gy, min(230f, H * 0.32f), shade(th.bot!!, -0.44f), Color(0x73FFE8A4))
			"dunes" -> hillBand(st.dist * 0.12f, gy, min(130f, H * 0.18f), 0.005f, shade(th.bot!!, -0.12f), 0f)
			"space" -> sceneryRow(st.dist * 0.12f, 240f, gy) { x, y, i ->
				val r = 30f + hsh(i.toFloat()) * 24f
				val pth = Path()
				pth.arcTo(Rect(x - r, y + 8f - r, x + r, y + 8f + r), 180f, 180f, true)
				pth.close()
				drawPath(pth, shade(th.bot!!, -0.34f))
			}
			"ocean" -> hillBand(st.dist * 0.1f, gy, min(120f, H * 0.17f), 0.0038f, shade(th.bot!!, -0.3f), 1.2f)
			else -> {
				hillBand(st.dist * 0.11f, gy, min(160f, H * 0.22f), 0.0042f, shade(th.bot!!, -0.15f), 0f)
				hillBand(st.dist * 0.2f, gy, min(112f, H * 0.16f), 0.0072f, shade(th.bot!!, -0.27f), 2.1f)
			}
		}
	}

	private fun DrawScope.drawMid() {
		val th = e.activeWorld(); val b = e.bioKey(); val gy = H - e.gh() + 6f
		when (b) {
			"stadium" -> hoardings(st.dist * 0.6f, gy, th)
			"forest" -> sceneryRow(st.dist * 0.36f, 124f, gy) { x, y, i -> tree(x, y, i, shade(th.ground!!, 0.2f), shade(th.ground, -0.08f), 1f) }
			"hills" -> sceneryRow(st.dist * 0.36f, 178f, gy) { x, y, i -> tree(x, y, i, shade(th.bot!!, -0.44f), shade(th.ground!!, -0.12f), 0.92f) }
			"city" -> skyline(st.dist * 0.36f, gy, min(150f, H * 0.21f), shade(th.bot!!, -0.62f), Color(0xB8FFDC8C))
			"dunes" -> sceneryRow(st.dist * 0.36f, 196f, gy) { x, y, i -> cactus(x, y, i, shade(th.ground!!, -0.06f)) }
			"space" -> sceneryRow(st.dist * 0.36f, 210f, gy) { x, y, i ->
				val h = 34f + hsh(i.toFloat()) * 46f
				val pth = Path()
				pth.moveTo(x - 11f, y); pth.lineTo(x, y - h); pth.lineTo(x + 11f, y); pth.close()
				drawPath(pth, Color(0x809E74FF))
			}
			else -> sceneryRow(st.dist * 0.36f, 158f, gy) { x, y, i ->
				val h = 46f + hsh(i.toFloat()) * 54f
				val pth = Path()
				pth.moveTo(x, y)
				pth.quadraticBezierTo(x + sin((st.t * 0.03f + i).toDouble()).toFloat() * 16f, y - h * 0.6f, x, y - h)
				drawPath(pth, Color(0x802EA692), style = Stroke(7f, cap = StrokeCap.Round))
			}
		}
	}

	private fun DrawScope.drawNear() {
		val th = e.activeWorld(); val b = e.bioKey()
		val gy = H - e.gh() + max(26f, e.gh() * 0.3f)
		val off = st.dist * 0.85f
		val a = 0.42f
		when (b) {
			// Pitch-side grass tufts blurring past. This is the closest and fastest
			// layer, so the stadium reads as moving even while the stands are far.
			"stadium" -> sceneryRow(off, 210f, gy) { x, y, i ->
				turf(x, y, i, shade(th.gTop ?: Color(0xFF3D9B4C), -0.34f).copy(alpha = a))
			}
			"dunes", "space" -> sceneryRow(off, 270f, gy) { x, y, i -> rock(x, y, i, shade(th.ground!!, -0.26f).copy(alpha = a)) }
			"city" -> sceneryRow(off, 320f, gy) { x, y, i -> bush(x, y, i, shade(th.ground!!, -0.32f).copy(alpha = a)) }
			"ocean" -> sceneryRow(off, 300f, gy) { x, y, i -> rock(x, y, i, shade(th.ground!!, -0.2f).copy(alpha = a)) }
			else -> sceneryRow(off, 250f, gy) { x, y, i -> bush(x, y, i, shade(th.ground!!, -0.24f).copy(alpha = a)) }
		}
	}

	// ---------------- stadium (club themes) ----------------

	/** Far layer for club themes: a packed stand + floodlights in the club colours. */
	private fun DrawScope.stands(off: Float, baseY: Float, th: Item) {
		val c1 = th.c ?: Color(0xFFEFEFEF)
		val c2 = th.c2 ?: Color(0xFF232323)
		val sh = min(max(130f, H * 0.3f), 260f)
		val topY = baseY - sh
		val roofH = max(12f, sh * 0.15f)

		// floodlight pylons behind the roof
		val pStep = max(240f, W * 0.46f)
		val pOff = off * 0.6f
		val pn = ceil(W / pStep).toInt() + 2
		val pStart = floor(pOff / pStep).toInt()
		for (i in 0 until pn) {
			val x = (pStart + i) * pStep - pOff
			val mast = sh * 0.62f
			val bw = 56f
			val bhh = 22f
			val by = topY - roofH - mast - bhh
			drawRect(shade(c2, -0.6f), Offset(x - 4f, topY - roofH - mast), Size(8f, mast))
			drawRect(shade(c2, -0.45f), Offset(x - bw / 2f, by), Size(bw, bhh))
			for (k in 0 until 4) {
				drawCircle(Color(0xFFFFF7CC), 5f, Offset(x - bw / 2f + 10f + k * 12f, by + bhh / 2f), alpha = 0.92f)
			}
		}

		// roof + stand shell
		drawRect(shade(c2, -0.5f), Offset(0f, topY - roofH), Size(W, roofH))
		drawRect(
			Brush.verticalGradient(listOf(shade(c2, -0.28f), shade(c2, -0.05f)), topY, baseY),
			Offset(0f, topY), Size(W, sh)
		)

		// crowd rows
		for (r in 0 until 4) {
			val par = 0.55f + r * 0.14f
			val ro = off * par
			val step = 17f
			val n = ceil(W / step).toInt() + 2
			val start = floor(ro / step).toInt()
			val y = topY + sh * (0.2f + r * 0.19f)
			for (i in 0 until n) {
				val idx = start + i
				val x = idx * step - ro
				val hv = hsh(idx * 2.7f + r * 11.3f)
				val col = when {
					hv > 0.72f -> c1
					hv > 0.4f -> c2
					hv > 0.2f -> shade(c1, 0.3f)
					else -> shade(c2, 0.25f)
				}
				drawCircle(col, step * 0.3f, Offset(x + step / 2f, y), alpha = 0.9f)
			}
		}

		// club banner along the front rail
		val bandH = max(10f, sh * 0.12f)
		drawRect(c1, Offset(0f, baseY - bandH), Size(W, bandH))
		val sStep = 46f
		val sOff = wrapv(off, sStep * 2f)
		var bx = -sOff
		while (bx < W + sStep * 2f) {
			drawRect(c2, Offset(bx, baseY - bandH), Size(sStep, bandH))
			bx += sStep * 2f
		}
		drawRect(shade(c2, -0.55f), Offset(0f, baseY - bandH - 4f), Size(W, 5f))
	}

	/**
	 * شماريخ ملاعب VIP: شماريخ في إيدين الجمهور بتنور وتطلع دخان، وشماريخ
	 * بتطلع من المدرج وتفرقع في السما. كل حاجة بتتلون بألوان الملعب نفسه
	 * (th.c / th.c2)، فكل ملعب VIP بيبان بشخصيته.
	 */
	private fun DrawScope.flares(baseY: Float, th: Item) {
		val c1 = th.c ?: Color(0xFFFF5A3C)
		val c2 = th.c2 ?: Color(0xFFFFC93C)
		val t = st.t.toFloat()
		val sh = min(max(130f, H * 0.3f), 260f)      // نفس ارتفاع المدرج في stands()
		val topY = baseY - sh
		val slots = if (W > 900f) 3 else 2

		// (1) شماريخ في إيدين الجمهور: نور نابض + دخان ملون بيطلع من المدرجات
		val hand = slots * 2
		for (i in 0 until hand) {
			val fx = W * (i + 0.25f + 0.5f * hsh(i * 2.7f)) / hand
			val fy = topY + sh * (0.28f + 0.44f * hsh(i * 4.1f))
			val pul = 0.5f + 0.5f * sin((t * 0.13f + i * 1.7f).toDouble()).toFloat()
			val fc = if (i % 2 == 0) c1 else c2
			for (k in 0 until 3) {
				val sy = fy - 14f - k * 16f - (t * 0.4f + i * 9f) % 16f
				val sway = sin((t * 0.02f + k * 0.9f + i).toDouble()).toFloat() * 6f
				drawCircle(fc.copy(alpha = (0.08f - k * 0.022f) * (0.5f + pul * 0.5f)), 9f + k * 6f, Offset(fx + sway, sy))
			}
			drawCircle(fc.copy(alpha = 0.16f + 0.12f * pul), 10f + 3f * pul, Offset(fx, fy))
			drawCircle(Color.White.copy(alpha = 0.4f + 0.2f * pul), 3.2f, Offset(fx, fy))
		}

		// (2) شماريخ تضرب في السما: تطلع من المدرج بعدين تفرقع سباركس
		for (i in 0 until slots) {
			val period = 200f + hsh(i * 5.3f) * 180f
			val clock = t + hsh(i * 9.1f) * period
			val u = (clock % period) / period
			if (u > 0.72f) continue                  // فترة راحة قبل الشمروخ اللي بعده
			val cyc = floor(clock / period)
			val jit = hsh(cyc * 0.61f + i * 3.7f)
			val x = W * (i + 0.15f + 0.7f * jit) / slots
			val col = if ((cyc.toInt() + i) % 3 == 0) shade(c2, 0.2f) else c1
			val startY = topY + sh * 0.42f
			val apexY = max(H * 0.05f, startY - min(H * 0.42f, sh * 1.7f))

			if (u < 0.24f) {
				// الطلوع: شعلة ماشية لفوق وسايبة أثر دخان
				val k = u / 0.24f
				val ease = 1f - (1f - k) * (1f - k)
				val y = startY + (apexY - startY) * ease
				drawLine(col.copy(alpha = 0.55f), Offset(x, y + 26f), Offset(x, y), 4f, StrokeCap.Round)
				drawCircle(col.copy(alpha = 0.2f), 10f, Offset(x, y))
				drawCircle(Color.White.copy(alpha = 0.6f), 3f, Offset(x, y))
				for (k2 in 0 until 3) {
					val ty = y + 34f + k2 * 20f
					val sway = sin((ty * 0.05f + i).toDouble()).toFloat() * 4f
					drawCircle(col.copy(alpha = 0.13f - k2 * 0.035f), 7f - k2 * 1.6f, Offset(x + sway, ty))
				}
			} else {
				// الفرقعة: سباركس بتتفتح وتقع بالجازبية
				val b = (u - 0.24f) / 0.48f
				val fade = 1f - b
				val R = min(W, H) * 0.19f * (0.18f + 0.82f * (1f - (1f - b) * (1f - b)))
				drawCircle(col.copy(alpha = 0.13f * fade * fade), R * 0.9f, Offset(x, apexY))
				val n = 12
				for (k in 0 until n) {
					val ang = (k.toFloat() / n) * PI.toFloat() * 2f + jit * 5f
					val rr = R * (0.72f + 0.42f * hsh(k * 1.7f + i))
					val sx = x + cos(ang.toDouble()).toFloat() * rr
					val sy = apexY + sin(ang.toDouble()).toFloat() * rr * 0.86f + b * b * R * 0.75f
					val sc = when (k % 3) {
						0 -> c1
						1 -> c2
						else -> Color.White
					}
					drawCircle(sc.copy(alpha = 0.6f * fade), max(1.2f, 4.2f * fade), Offset(sx, sy))
				}
			}
		}
	}

	/**
	 * أنوار ليزر ملاعب VIP: كشافات فوق سقف المدرج بتلف رايح جاي على طول،
	 * والشعاع بلون النادي أو المنتخب نفسه (th.c / th.c2)، فكل ملعب VIP ليه
	 * لون ليزر مختلف. الشعاع بيتفرد وهو بيبعد وبيبهت لما يوصل للأرض.
	 */
	private fun DrawScope.lasers(baseY: Float, th: Item) {
		val c1 = th.c ?: Color(0xFF3CE0FF)
		val c2 = th.c2 ?: Color(0xFFFF4AD8)
		val t = st.t.toFloat()
		val sh = min(max(130f, H * 0.3f), 260f)      // نفس ارتفاع المدرج في stands()
		val topY = baseY - sh
		val roofH = max(12f, sh * 0.15f)
		val srcY = topY - roofH * 0.45f
		val n = if (W > 900f) 3 else 2
		val len = min(H * 0.5f, sh * 2.1f)

		for (i in 0 until n) {
			val sx = W * (i + 0.5f) / n
			val sp = 0.006f + hsh(i * 3.1f) * 0.004f
			// رايح جاي: الكشاف بيلف يمين وشمال على طول
			val sweep = sin((t * sp + i * 1.7f).toDouble()).toFloat()
			val ang = PI.toFloat() / 2f + sweep * 0.5f
			val dx = cos(ang.toDouble()).toFloat()
			val dy = sin(ang.toDouble()).toFloat()
			val ex = sx + dx * len
			val ey = srcY + dy * len
			val col = if (i % 2 == 0) c1 else c2
			val pulse = 0.7f + 0.3f * sin((t * 0.08f + i * 2.3f).toDouble()).toFloat()
			val spread = 12f + 16f * abs(sweep)
			val px = -dy * spread
			val py = dx * spread

			val beam = Path()
			beam.moveTo(sx - dy * 4f, srcY + dx * 4f)
			beam.lineTo(ex + px, ey + py)
			beam.lineTo(ex - px, ey - py)
			beam.lineTo(sx + dy * 4f, srcY - dx * 4f)
			beam.close()
			drawPath(
				beam,
				Brush.linearGradient(
					0f to col.copy(alpha = 0.2f * pulse),
					0.5f to col.copy(alpha = 0.07f * pulse),
					1f to Color(0x00000000),
					start = Offset(sx, srcY),
					end = Offset(ex, ey)
				),
				blendMode = BlendMode.Plus
			)
			// قلب الشعاع الأبيض
			drawLine(
				Color.White.copy(alpha = 0.1f * pulse),
				Offset(sx, srcY),
				Offset(sx + dx * len * 0.55f, srcY + dy * len * 0.55f),
				3.4f, StrokeCap.Round, blendMode = BlendMode.Plus
			)
			// جسم الكشاف بيلف مع الشعاع
			rotate((ang * 180f / PI.toFloat()) - 90f, Offset(sx, srcY)) {
				rrFill(sx - 4.5f, srcY - 17f, 9f, 17f, 3f, shade(c2, -0.62f))
				rrFill(sx - 9.5f, srcY - 7f, 19f, 16f, 5f, shade(c2, -0.45f))
				rrFill(sx - 6.5f, srcY + 5f, 13f, 6f, 3f, col)
			}
			drawCircle(col.copy(alpha = 0.26f * pulse), 11f + 3f * pulse, Offset(sx, srcY), blendMode = BlendMode.Plus)
			drawCircle(Color.White.copy(alpha = 0.5f), 3f, Offset(sx, srcY))
		}
	}

	/**
	 * طبقة تهدية لملاعب الشماريخ والليزر: بتخفف الوهج والزحمة اللي ورا
	 * اللعب، فاللاعب والعقبات يبانوا واضحين والعين ماتتعبش.
	 */
	private fun DrawScope.calmVip(baseY: Float) {
		val sh = min(max(130f, H * 0.3f), 260f)
		val topY = max(0f, baseY - sh * 1.7f)
		drawRect(
			Brush.verticalGradient(
				0f to Color(0x00080D18),
				0.5f to Color(0x1A080D18),
				1f to Color(0x4D080D18),
				startY = topY,
				endY = baseY
			),
			Offset(0f, topY),
			Size(W, baseY - topY)
		)
	}

	/** شعار النادي / المنتخب: معلق على المدرج ومتكرر على طوله. */
	private fun DrawScope.crestOnStands(off: Float, baseY: Float, th: Item, id: String?) {
		val tid = id ?: return
		val code = Catalog.crestCode(tid) ?: return
		val c1 = th.c ?: Color(0xFFEFEFEF)
		val c2 = th.c2 ?: Color(0xFF232323)
		val sh = min(max(130f, H * 0.3f), 260f)
		val topY = baseY - sh
		val r = max(20f, sh * 0.19f)
		val step = max(360f, W * 0.75f)
		val cOff = off * 0.62f
		val n = ceil(W / step).toInt() + 2
		val start = floor(cOff / step).toInt()
		val cy = topY + sh * 0.44f
		for (i in 0 until n) {
			val cx = (start + i) * step - cOff + step * 0.5f
			// دايرة غامقة ورا الشعار عشان يبان على الجمهور
			drawCircle(Color(0xFF0A1120).copy(alpha = 0.3f), r * 1.55f, Offset(cx, cy))
			crestBadge(tid, cx, cy, r, c1, c2, code, alpha = 0.96f)
		}
	}

	/** Mid layer for club themes: pitch-side advertising boards. */
	private fun DrawScope.hoardings(off: Float, baseY: Float, th: Item) {
		val c1 = th.c ?: Color(0xFFEFEFEF)
		val c2 = th.c2 ?: Color(0xFF232323)
		val bh = max(20f, min(38f, H * 0.052f))
		val step = 104f
		val n = ceil(W / step).toInt() + 2
		val start = floor(off / step).toInt()
		for (i in 0 until n) {
			val idx = start + i
			val x = idx * step - off
			val col = if (idx % 2 == 0) c1 else c2
			drawRect(col, Offset(x, baseY - bh), Size(step - 6f, bh))
			drawRect(Color.White.copy(alpha = 0.18f), Offset(x, baseY - bh), Size(step - 6f, bh * 0.28f))
			drawRect(shade(col, -0.4f), Offset(x, baseY - 3f), Size(step - 6f, 4f))
		}
	}

	/** Ground for club themes: a mown pitch with markings instead of grass + dirt. */
	private fun DrawScope.drawPitch(th: Item) {
		val g = e.gh()
		val y = H - g
		val grass = th.gTop ?: Color(0xFF3D9B4C)
		val deep = th.ground ?: Color(0xFF24632F)

		drawRect(
			Brush.verticalGradient(listOf(shade(grass, 0.06f), shade(deep, -0.12f)), y, H),
			Offset(0f, y), Size(W, g)
		)

		// Mown stripes scrolling with the run. These carry most of the sense of
		// speed, so they are strong enough to actually see going past.
		val step = 82f
		val off = wrapv(st.dist, step * 2f)
		var x = -off
		while (x < W + step * 2f) {
			drawRect(Color.White.copy(alpha = 0.15f), Offset(x, y), Size(step, g))
			drawRect(Color.Black.copy(alpha = 0.06f), Offset(x + step, y), Size(step, g))
			x += step * 2f
		}

		// Grass flecks: fine detail travelling at full pitch speed, which is what
		// makes the ground read as moving under the runner's feet.
		val fStep = 46f
		val fOff = wrapv(st.dist, fStep)
		val fleck = shade(deep, -0.2f).copy(alpha = 0.5f)
		var fx = -fOff
		while (fx < W + fStep) {
			drawRect(fleck, Offset(fx, y + g * 0.3f), Size(15f, 4f))
			drawRect(fleck, Offset(fx + 21f, y + g * 0.52f), Size(11f, 4f))
			drawRect(fleck, Offset(fx + 8f, y + g * 0.74f), Size(13f, 4f))
			fx += fStep
		}

		// touchline + top edge highlight
		val lineY = y + max(9f, g * 0.13f)
		drawRect(Color.White.copy(alpha = 0.85f), Offset(0f, lineY), Size(W, 4f))
		drawRect(Color(0x3DFFFFFF), Offset(0f, y), Size(W, 3f))

		// penalty boxes drifting past
		val bStep = 720f
		val bo = wrapv(st.dist, bStep)
		val bw = 240f
		val bhh = max(26f, g * 0.5f)
		val col = Color.White.copy(alpha = 0.6f)
		var px = -bo
		while (px < W + bStep) {
			drawRect(col, Offset(px, lineY), Size(4f, bhh))
			drawRect(col, Offset(px + bw, lineY), Size(4f, bhh))
			drawRect(col, Offset(px, lineY + bhh), Size(bw, 4f))
			drawCircle(col, 5f, Offset(px + bw / 2f, lineY + bhh * 0.62f))
			px += bStep
		}
	}

	// ---------------- ground ----------------

	private fun groundTop(th: Item, b: String): Color = when (b) {
		"hills" -> Color(0xFF5CB85C)
		"forest" -> Color(0xFF3F9D5D)
		"dunes" -> shade(th.gTop!!, 0.14f)
		else -> th.gTop!!
	}

	private fun groundBase(th: Item, b: String): Color = when (b) {
		"hills" -> Color(0xFF93704A)
		"forest" -> Color(0xFF6B4F34)
		else -> th.ground!!
	}

	private fun DrawScope.drawG() {
		val th = e.activeWorld(); val b = e.bioKey()
		if (b == "stadium") { drawPitch(th); return }
		val g = e.gh(); val y = H - g
		val base = groundBase(th, b); val top = groundTop(th, b)

		drawRect(
			Brush.verticalGradient(listOf(base, shade(base, -0.3f)), y, H),
			Offset(0f, y), Size(W, g)
		)
		val bh = max(13f, g * 0.15f)
		drawRect(top, Offset(0f, y), Size(W, bh))

		val step = 30f
		val off = wrapv(st.dist, step)
		var x = -off
		while (x < W + step) {
			val cxp = x + step / 2f
			val r = step * 0.42f
			val pth = Path()
			pth.arcTo(Rect(cxp - r, y + bh - r, cxp + r, y + bh + r), 0f, 180f, true)
			pth.close()
			drawPath(pth, top)
			x += step
		}

		drawRect(Color(0x3DFFFFFF), Offset(0f, y), Size(W, 3f))

		val bladeCol = shade(top, -0.22f)
		val bo = wrapv(st.dist, 27f)
		var bx = -bo
		while (bx < W + 27f) {
			drawLine(bladeCol, Offset(bx, y + bh + 2f), Offset(bx + 4f, y + bh - 9f), 3f, StrokeCap.Round)
			bx += 27f
		}

		val dot = shade(base, -0.2f)
		val so = wrapv(st.dist, 74f)
		var sx = -so
		while (sx < W + 74f) {
			drawRect(dot, Offset(sx, y + g * 0.34f), Size(20f, 7f))
			drawRect(dot, Offset(sx + 34f, y + g * 0.52f), Size(13f, 6f))
			drawRect(dot, Offset(sx + 12f, y + g * 0.72f), Size(17f, 6f))
			drawRect(dot, Offset(sx + 50f, y + g * 0.88f), Size(11f, 5f))
			sx += 74f
		}
		val dot2 = shade(base, 0.14f)
		val so2 = wrapv(st.dist, 126f)
		var s2 = -so2
		while (s2 < W + 126f) {
			drawRect(dot2, Offset(s2 + 28f, y + g * 0.58f), Size(28f, 5f))
			s2 += 126f
		}
	}

	// ---------------- entities ----------------

	private fun DrawScope.drawAmbient() {
		if (e.amb.isEmpty()) return
		for (a in e.amb) {
			if (a.type == "star") {
				val al = 0.45f + sin(((st.t + a.tw * 10f) * 0.1f).toDouble()).toFloat() * 0.4f
				drawCircle(Color.White, a.r, Offset(a.x, a.y), alpha = al.coerceIn(0f, 1f))
			} else {
				drawCircle(Color(0xD9FFFFFF), a.r, Offset(a.x, a.y), alpha = 0.32f, style = Stroke(1.2f))
			}
		}
	}

	private fun DrawScope.drawShadow() {
		val gy = H - e.gh()
		val alt = max(0f, (gy - p.r) - p.y)
		val k = max(0.3f, 1f - alt / (H * 0.55f))
		val rx = p.r * 1.02f * k
		val ry = p.r * 0.28f * k
		drawOval(Color.Black, Offset(p.x - rx, gy + 5f - ry), Size(rx * 2f, ry * 2f), alpha = 0.2f * k)
	}

	private fun DrawScope.drawGlowTrail() {
		if (e.glowTrail.isEmpty()) return
		for (gt in e.glowTrail) {
			val col = e.glowColor(gt.seed) ?: continue
			val R = p.r * 1.1f * gt.life + p.r * 0.35f
			drawCircle(
				brush = Brush.radialGradient(
					0f to col, 0.55f to col, 1f to Color(0x00000000),
					center = Offset(gt.x, gt.y), radius = R
				),
				radius = R,
				center = Offset(gt.x, gt.y),
				alpha = 0.28f * gt.life,
				blendMode = BlendMode.Plus
			)
		}
	}

	private fun DrawScope.drawTrail() {
		if (S.trail == "none" || p.trailPts.size < 2) return
		val tr = Catalog.TRAILS[S.trail] ?: return
		for (i in p.trailPts.indices) {
			val pt = p.trailPts[i]
			val a = 1f - i.toFloat() / p.trailPts.size
			val col = when (tr.kind) {
				"rainbow" -> hsl2c(((st.t * 6 + i * 25) % 360).toDouble(), 90.0, 60.0)
				"star" -> if (i % 2 == 1) Color(0xFFFFD700) else Color(0xFFFFFFFF)
				else -> tr.c ?: return
			}
			val rr2 = p.r * (0.8f - i * 0.04f)
			drawCircle(col, max(2f, rr2), Offset(pt.x, pt.y), alpha = a * 0.6f)
		}
	}

	// ---------------- الأقماع (بدل المطبات القديمة) ----------------

	private val coneLite = Color(0xFFFFA23A)
	private val coneMid = Color(0xFFFF7A18)
	private val coneDeep = Color(0xFFE85B00)
	private val coneDark = Color(0xFF9C3B00)

	/** قمع ملاعب طويل: جسم مخروطي برتقالي + قاعدة مربعة. */
	private fun DrawScope.drawCone(o: Obstacle) {
		val cx = o.x + o.w / 2f
		val bot = o.y + o.h
		val baseH = max(4.5f, o.h * 0.1f)
		val baseW = o.w
		val botY = bot - baseH * 0.55f
		val topY = o.y + 1f
		val halfBot = o.w * 0.37f
		val halfTop = max(3f, o.w * 0.1f)

		// ظل على الأرض
		drawOval(
			Color.Black.copy(alpha = 0.18f),
			Offset(cx - baseW * 0.54f, bot - baseH * 0.5f),
			Size(baseW * 1.08f, baseH * 1.1f)
		)

		// القاعدة
		rrFill(cx - baseW / 2f, bot - baseH, baseW, baseH, baseH * 0.45f, coneDeep)
		rrFill(cx - baseW / 2f, bot - baseH, baseW, baseH * 0.5f, baseH * 0.35f, coneMid)
		drawCircle(coneDark.copy(alpha = 0.5f), max(1.2f, baseH * 0.16f), Offset(cx - baseW * 0.36f, bot - baseH * 0.45f))
		drawCircle(coneDark.copy(alpha = 0.5f), max(1.2f, baseH * 0.16f), Offset(cx + baseW * 0.36f, bot - baseH * 0.45f))

		// جسم المخروط
		val body = Path()
		body.moveTo(cx - halfBot, botY)
		body.lineTo(cx - halfTop, topY)
		body.lineTo(cx + halfTop, topY)
		body.lineTo(cx + halfBot, botY)
		body.close()
		drawPath(body, Brush.verticalGradient(listOf(coneLite, coneDeep), topY, botY))
		drawPath(body, coneDark.copy(alpha = 0.55f), style = Stroke(1.8f))

		// لمعة على الشمال
		val hi = Path()
		hi.moveTo(cx - halfTop * 0.72f, topY)
		hi.lineTo(cx - halfTop * 0.1f, topY)
		hi.lineTo(cx - halfBot * 0.26f, botY)
		hi.lineTo(cx - halfBot * 0.68f, botY)
		hi.close()
		drawPath(hi, Color.White.copy(alpha = 0.17f))

		// الفتحة اللي فوق
		drawOval(
			coneDark.copy(alpha = 0.72f),
			Offset(cx - halfTop * 0.85f, topY - halfTop * 0.35f),
			Size(halfTop * 1.7f, halfTop * 0.7f)
		)
	}

	/** أقماع الصحون الواطية المتكدسة للمطبات القصيرة. */
	private fun DrawScope.drawDiscCone(o: Obstacle) {
		val cx = o.x + o.w / 2f
		val bot = o.y + o.h
		val n = max(2, min(5, Math.round(o.h / 12f)))
		val layerH = o.h / n
		val wBot = o.w * 1.1f

		drawOval(
			Color.Black.copy(alpha = 0.18f),
			Offset(cx - wBot * 0.52f, bot - layerH * 0.5f),
			Size(wBot * 1.04f, layerH * 1.1f)
		)

		for (i in 0 until n) {
			val hw = wBot * 0.5f * (1f - 0.16f * i)
			val yTop = bot - (i + 1) * layerH
			val eh = layerH * 1.85f
			val col = if (i % 2 == 0) coneMid else coneLite
			drawOval(shade(col, -0.3f), Offset(cx - hw, yTop), Size(hw * 2f, eh))
			drawOval(col, Offset(cx - hw, yTop - eh * 0.16f), Size(hw * 2f, eh))
		}

		// الخرم اللي في نص القمع
		val topHw = wBot * 0.5f * (1f - 0.16f * (n - 1)) * 0.4f
		drawOval(
			coneDark.copy(alpha = 0.7f),
			Offset(cx - topHw, o.y - layerH * 0.1f),
			Size(topHw * 2f, layerH * 0.8f)
		)
		drawOval(
			Color.White.copy(alpha = 0.16f),
			Offset(cx - wBot * 0.4f, o.y + o.h * 0.24f),
			Size(wBot * 0.3f, o.h * 0.22f)
		)
	}

	/** صف أقماع صغيرة بدل الشوك القديم. */
	private fun DrawScope.drawConeRow(o: Obstacle) {
		val n = max(2, Math.round(o.w / 15f))
		val sw = o.w / n
		val bot = o.y + o.h
		val baseH = max(3.5f, o.h * 0.14f)
		rrFill(o.x - 1.5f, bot - baseH, o.w + 3f, baseH, baseH * 0.45f, coneDeep)
		for (i in 0 until n) {
			val cx = o.x + sw * (i + 0.5f)
			val halfBot = sw * 0.44f
			val halfTop = max(1.6f, sw * 0.1f)
			val topY = o.y + 2f
			val botY = bot - baseH * 0.45f
			val pth = Path()
			pth.moveTo(cx - halfBot, botY)
			pth.lineTo(cx - halfTop, topY)
			pth.lineTo(cx + halfTop, topY)
			pth.lineTo(cx + halfBot, botY)
			pth.close()
			drawPath(pth, Brush.verticalGradient(listOf(coneLite, coneDeep), topY, botY))
			drawPath(pth, coneDark.copy(alpha = 0.5f), style = Stroke(1.4f))
		}
	}

	private fun DrawScope.drawFlyer(o: Obstacle) {
		val yy = o.y + sin(((st.t + o.x * 0.05f) * 0.12f).toDouble()).toFloat() * 3f
		rrFill(o.x, yy, o.w, o.h, 9f, Color(0xFF7038C0))
		rrFill(o.x, yy, o.w, 6f, 6f, Color(0xFF4A1A7A))
		val eye = Path()
		eye.addOval(Rect(o.x + o.w * 0.36f - 3.2f, yy + o.h * 0.55f - 3.2f, o.x + o.w * 0.36f + 3.2f, yy + o.h * 0.55f + 3.2f))
		eye.addOval(Rect(o.x + o.w * 0.64f - 3.2f, yy + o.h * 0.55f - 3.2f, o.x + o.w * 0.64f + 3.2f, yy + o.h * 0.55f + 3.2f))
		drawPath(eye, Color(0xFFFBFAF6))
		drawLine(Color(0xFF4A1A7A), Offset(o.x - 6f, yy + o.h * 0.4f), Offset(o.x, yy + o.h * 0.5f), 3f)
		drawLine(Color(0xFF4A1A7A), Offset(o.x + o.w + 6f, yy + o.h * 0.4f), Offset(o.x + o.w, yy + o.h * 0.5f), 3f)
	}

	private fun DrawScope.drawMover(o: Obstacle) {
		val cx = o.x + o.w / 2f; val cy = o.y + o.h / 2f
		val R = max(o.w, o.h) / 2f + 2f
		rotate(st.t * 0.12f * 180f / PI.toFloat(), Offset(cx, cy)) {
			val n = 8
			val pth = Path()
			for (i in 0 until n * 2) {
				val ang = PI * i / n
				val rr2 = if (i % 2 == 1) R else R * 0.62f
				val px = cx + (cos(ang) * rr2).toFloat()
				val py = cy + (sin(ang) * rr2).toFloat()
				if (i == 0) pth.moveTo(px, py) else pth.lineTo(px, py)
			}
			pth.close()
			drawPath(pth, Color(0xFFFF3B30))
		}
		drawCircle(Color(0xFF8A1A12), R * 0.4f, Offset(cx, cy))
	}

	private fun DrawScope.drawO() {
		for (o in e.obs) {
			when (o.type) {
				// صف أقماع صغيرة
				"spikes" -> drawConeRow(o)
				"flyer" -> drawFlyer(o)
				"mover" -> drawMover(o)
				// المطبات بقت أقماع: الواطي صحون متكدسة والعالي قمع كامل
				else -> if (o.h < 52f) drawDiscCone(o) else drawCone(o)
			}
		}
	}

	/** خماسية سودا جوه الكورة. */
	private fun pentaInto(pth: Path, cx: Float, cy: Float, r: Float, rot: Double) {
		for (k in 0 until 5) {
			val a = rot + k * (2.0 * PI / 5.0)
			val x = cx + (cos(a) * r).toFloat()
			val y = cy + (sin(a) * r).toFloat()
			if (k == 0) pth.moveTo(x, y) else pth.lineTo(x, y)
		}
		pth.close()
	}

	/** كورة الملعب: بتلف في الهوا وبتلمع، ولما تاخدها بتتحسب 5 كوينز. */
	private fun DrawScope.drawBall(c: Coin, yy: Float) {
		val R = c.r
		val spin = ((st.t + c.b * 20f) * 0.035f).toDouble()
		val pul = 0.62f + 0.38f * sin(((st.t + c.b * 10f) * 0.12f).toDouble()).toFloat()

		// هالة دهبي عشان تبان إنها مش كوينة عادية
		drawCircle(
			brush = Brush.radialGradient(
				0f to Color(0x00FFD54A), 0.58f to Color(0x00FFD54A),
				0.78f to Color(0x8CFFD54A), 1f to Color(0x00FFD54A),
				center = Offset(c.x, yy), radius = R * 2f
			),
			radius = R * 2f, center = Offset(c.x, yy),
			alpha = pul.coerceIn(0f, 1f), blendMode = BlendMode.Plus
		)

		drawCircle(Color(0xFFFBFAF6), R, Offset(c.x, yy))

		// الرقع السودا مقصوصة على حدود الكورة
		val patch = Path()
		pentaInto(patch, c.x, yy, R * 0.42f, spin - PI / 2.0)
		for (k in 0 until 5) {
			val a = spin + k * (2.0 * PI / 5.0) - PI / 2.0
			val px = c.x + (cos(a) * R * 0.86f).toFloat()
			val py = yy + (sin(a) * R * 0.86f).toFloat()
			pentaInto(patch, px, py, R * 0.34f, a + PI)
		}
		val ring = Path()
		ring.addOval(Rect(c.x - R * 0.97f, yy - R * 0.97f, c.x + R * 0.97f, yy + R * 0.97f))
		val patches = Path()
		patches.op(patch, ring, PathOperation.Intersect)
		drawPath(patches, Color(0xFF1C1C26))

		drawCircle(Color(0xFF2A2440).copy(alpha = 0.45f), R, Offset(c.x, yy), style = Stroke(1.8f))
		drawCircle(Color.White.copy(alpha = 0.5f), R * 0.2f, Offset(c.x - R * 0.38f, yy - R * 0.42f))

		// شارة: الكورة = 5 كوينز
		val bx = c.x + R * 0.82f
		val by = yy - R * 0.82f
		drawCircle(Color(0xFFFFC93C), R * 0.46f, Offset(bx, by))
		drawCircle(Color(0xFF8A5B00).copy(alpha = 0.55f), R * 0.46f, Offset(bx, by), style = Stroke(1.6f))
		val paint = Fonts.canvasPaint(R * 0.62f)
		paint.color = android.graphics.Color.argb(255, 58, 38, 0)
		drawContext.canvas.nativeCanvas.drawText("5", bx, by + R * 0.22f, paint)
	}

	private fun DrawScope.drawC() {
		for (c in e.cns) {
			val yy = c.y + sin(((st.t + c.b) * 0.15f).toDouble()).toFloat() * 5f
			if (c.ball) {
				drawBall(c, yy)
				continue
			}
			drawCircle(Color(0xFFFFC93C), c.r, Offset(c.x, yy))
			drawCircle(Color(0xFFFFF0B8), c.r * 0.4f, Offset(c.x - c.r * 0.2f, yy - c.r * 0.2f))
		}
	}

	private fun DrawScope.icoMagnet(x: Float, y: Float, s: Float) {
		val arc = Path()
		arc.arcTo(Rect(x - s * 0.5f, y - s * 0.05f - s * 0.5f, x + s * 0.5f, y - s * 0.05f + s * 0.5f), 180f, 180f, true)
		drawPath(arc, Color.White, style = Stroke(s * 0.3f))
		drawLine(Color.White, Offset(x - s * 0.5f, y - s * 0.05f), Offset(x - s * 0.5f, y + s * 0.5f), s * 0.3f)
		drawLine(Color.White, Offset(x + s * 0.5f, y - s * 0.05f), Offset(x + s * 0.5f, y + s * 0.5f), s * 0.3f)
		drawLine(Color(0xFFFF3B30), Offset(x - s * 0.5f, y + s * 0.32f), Offset(x - s * 0.5f, y + s * 0.5f), s * 0.3f)
		drawLine(Color(0xFFFF3B30), Offset(x + s * 0.5f, y + s * 0.32f), Offset(x + s * 0.5f, y + s * 0.5f), s * 0.3f)
	}

	private fun DrawScope.icoShield(x: Float, y: Float, s: Float) {
		val pth = Path()
		pth.moveTo(x, y - s * 0.55f)
		pth.lineTo(x + s * 0.45f, y - s * 0.28f)
		pth.lineTo(x + s * 0.45f, y + s * 0.12f)
		pth.quadraticBezierTo(x + s * 0.45f, y + s * 0.5f, x, y + s * 0.62f)
		pth.quadraticBezierTo(x - s * 0.45f, y + s * 0.5f, x - s * 0.45f, y + s * 0.12f)
		pth.lineTo(x - s * 0.45f, y - s * 0.28f)
		pth.close()
		drawPath(pth, Color.White)
	}

	private fun DrawScope.icoClock(x: Float, y: Float, s: Float) {
		drawCircle(Color.White, s * 0.5f, Offset(x, y), style = Stroke(s * 0.13f, cap = StrokeCap.Round))
		drawLine(Color.White, Offset(x, y), Offset(x, y - s * 0.32f), s * 0.13f, StrokeCap.Round)
		drawLine(Color.White, Offset(x, y), Offset(x + s * 0.22f, y), s * 0.13f, StrokeCap.Round)
	}

	private fun DrawScope.drawPows() {
		for (pw in e.pows) {
			val yy = pw.y + sin(((st.t + pw.b) * 0.12f).toDouble()).toFloat() * 6f
			drawCircle(e.powColor(pw.type), pw.r, Offset(pw.x, yy))
			when (pw.type) {
				"magnet" -> icoMagnet(pw.x, yy, pw.r)
				"shield" -> icoShield(pw.x, yy, pw.r)
				else -> icoClock(pw.x, yy, pw.r)
			}
		}
	}

	private fun DrawScope.drawPts() {
		for (pt in e.pts) {
			val s = pt.s
			val a = max(0f, pt.l / 28f).coerceAtMost(1f)
			drawRect(pt.c, Offset(pt.x - s / 2f, pt.y - s / 2f), Size(s, s), alpha = a)
		}
	}

	private fun DrawScope.drawPlayer() {
		val def = Players[S.player]
		val col = e.glowColor()
		if (col != null) {
			val R = p.r * 1.35f * (1f + sin((st.t * 0.15f).toDouble()).toFloat() * 0.08f)
			drawCircle(
				brush = Brush.radialGradient(
					0f to col, 0.6f to col, 1f to Color(0x00000000),
					center = Offset(p.x, p.y), radius = R
				),
				radius = R, center = Offset(p.x, p.y),
				alpha = 0.4f, blendMode = BlendMode.Plus
			)
		}
		if (st.shieldT > 0) {
			val a = 0.35f + sin((st.t * 0.3f).toDouble()).toFloat() * 0.15f
			drawCircle(Color(0xFF4A9FE0), p.r * 1.6f, Offset(p.x, p.y), alpha = a.coerceIn(0f, 1f))
		}
		val sheet = PlayerSprites.sheet(def.id)
		if (sheet == null) {
			// art still decoding, or missing - a plain marker keeps the run readable
			drawCircle(def.c, p.r * 0.6f, Offset(p.x, p.y))
			return
		}

		// The step cycle comes from the engine: paced by distance travelled, rate
		// limited so the legs never blur, and frozen while airborne so the jump
		// holds a single clean pose instead of flicking through the strip.
		val cyc = e.runCyc
		val fi = ((cyc.toInt() % def.frames) + def.frames) % def.frames

		val ph = p.r * 2.55f
		val pwd = ph * def.aspect
		// Half the cube's squash: a person should flex, not turn into a pancake.
		val sq = 1f + (p.sq - 1f) * 0.5f
		// Anchored on the feet - the player's centre sits one radius above the ground.
		translate(p.x, p.y + p.r) {
			// No tilt at all: the runner stays upright, so a jump is a straight
			// vertical hop and a straight landing. The only deformation left is the
			// take-off stretch and the landing knee bend.
			scale(2f - sq, sq, Offset.Zero) {
				drawImage(
					image = sheet,
					srcOffset = IntOffset(fi * def.fw, 0),
					srcSize = IntSize(def.fw, def.fh),
					dstOffset = IntOffset(Math.round(-pwd / 2f), Math.round(-ph)),
					dstSize = IntSize(Math.round(pwd), Math.round(ph)),
					filterQuality = FilterQuality.Medium
				)
			}
		}
	}

	private fun DrawScope.drawFlo() {
		if (e.flo.isEmpty()) return
		val size = Math.round(p.r * 0.72f).toFloat()
		val fill = Fonts.canvasPaint(size)
		val strokeP = Fonts.canvasPaint(size)
		strokeP.style = android.graphics.Paint.Style.STROKE
		strokeP.strokeWidth = 4f
		val canvas = drawContext.canvas.nativeCanvas
		for (fo in e.flo) {
			val a = min(1f, fo.l / 26f)
			val al = (a * 255f).toInt().coerceIn(0, 255)
			strokeP.color = android.graphics.Color.argb((0.55f * al).toInt(), 42, 36, 64)
			fill.color = android.graphics.Color.argb(al, 0xFF, 0xD5, 0x4A)
			canvas.drawText(fo.txt, fo.x, fo.y, strokeP)
			canvas.drawText(fo.txt, fo.x, fo.y, fill)
		}
	}
}
