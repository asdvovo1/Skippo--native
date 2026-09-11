package com.skippo.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The IC = { ... } inline SVG set from index.html.
 * Path data is copied verbatim; `currentColor` becomes the [tint] parameter.
 */

sealed class El {
	data class P(
		val d: String,
		val fill: Color? = null,
		val fillCurrent: Boolean = false,
		val stroke: Color? = null,
		val strokeCurrent: Boolean = false,
		val sw: Float = 0f,
		val cap: StrokeCap = StrokeCap.Butt,
		val join: StrokeJoin = StrokeJoin.Miter
	) : El()

	data class Cir(
		val cx: Float, val cy: Float, val r: Float,
		val fill: Color? = null,
		val stroke: Color? = null,
		val strokeCurrent: Boolean = false,
		val sw: Float = 0f
	) : El()

	data class Rct(
		val x: Float, val y: Float, val w: Float, val h: Float, val rx: Float,
		val fill: Color? = null,
		val stroke: Color? = null,
		val strokeCurrent: Boolean = false,
		val sw: Float = 0f
	) : El()

	data class Txt(
		val x: Float, val y: Float, val size: Float, val text: String, val fill: Color
	) : El()
}

data class IconDef(val els: List<El>)

object IC {

	private val W = Color(0xFFFFFFFF)

	val coin = IconDef(listOf(
		El.Cir(12f, 12f, 9f, fill = Color(0xFFFFC93C), stroke = Color(0xFFE0A020), sw = 2f),
		El.Cir(12f, 12f, 4f, fill = Color(0xFFE0A020))
	))

	val gem = IconDef(listOf(
		El.P(
			"M5 4h14l3 5-10 11L2 9z",
			fill = Color(0xFF4AD4E0), stroke = Color(0xFF2BA0AC), sw = 1.5f, join = StrokeJoin.Round
		)
	))

	val tv = IconDef(listOf(
		El.Rct(2f, 5f, 20f, 15f, 2f, fill = null, strokeCurrent = true, sw = 2.4f),
		El.P("M10 9l5 3-5 3z", fillCurrent = true)
	))

	val target = IconDef(listOf(
		El.Cir(12f, 12f, 9f, stroke = W, sw = 2.2f),
		El.Cir(12f, 12f, 5f, stroke = W, sw = 2.2f),
		El.Cir(12f, 12f, 1.5f, fill = W, stroke = W, sw = 2.2f)
	))

	val trophy = IconDef(listOf(
		El.P("M7 4h10v5a5 5 0 0 1-10 0z", stroke = W, sw = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
		El.P("M7 6H4v1a3 3 0 0 0 3 3M17 6h3v1a3 3 0 0 1-3 3", stroke = W, sw = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
		El.P("M9.5 18h5M10 18l.5-4h3l.5 4", stroke = W, sw = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
	))

	val check = IconDef(listOf(
		El.P("M5 13l4 4L19 7", stroke = W, sw = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
	))

	val medal1 = IconDef(listOf(
		El.Cir(12f, 14f, 7f, fill = Color(0xFFFFD700), stroke = Color(0xFFC9A400), sw = 1.5f),
		El.Txt(12f, 18f, 9f, "1", Color(0xFF8A6D00))
	))

	val medal2 = IconDef(listOf(
		El.Cir(12f, 14f, 7f, fill = Color(0xFFCFD6E0), stroke = Color(0xFF9AA4B2), sw = 1.5f),
		El.Txt(12f, 18f, 9f, "2", Color(0xFF5A6472))
	))

	val medal3 = IconDef(listOf(
		El.Cir(12f, 14f, 7f, fill = Color(0xFFE0A060), stroke = Color(0xFFB57838), sw = 1.5f),
		El.Txt(12f, 18f, 9f, "3", Color(0xFF6A3D10))
	))

	val puMagnet = IconDef(listOf(
		El.P("M6 4v7a6 6 0 0 0 12 0V4", stroke = W, sw = 2.4f, cap = StrokeCap.Round),
		El.P("M6 4h4M14 4h4", stroke = W, sw = 2.4f, cap = StrokeCap.Round)
	))

	val puShield = IconDef(listOf(
		El.P("M12 2l8 3v6c0 4.5-3.2 8.4-8 10-4.8-1.6-8-5.5-8-10V5z", fill = W)
	))

	val puClock = IconDef(listOf(
		El.Cir(12f, 12f, 9f, stroke = W, sw = 2.4f),
		El.P("M12 7v5l3 2", stroke = W, sw = 2.4f, cap = StrokeCap.Round)
	))

	val noads = IconDef(listOf(
		El.Rct(3f, 5f, 18f, 14f, 2f, stroke = W, sw = 2.2f),
		El.P("M4 4l16 16", stroke = W, sw = 2.2f, cap = StrokeCap.Round)
	))

	val gift = IconDef(listOf(
		El.Rct(3f, 9f, 18f, 12f, 1.5f, stroke = W, sw = 2.2f),
		El.P("M3 13h18M12 9v12", stroke = W, sw = 2.2f, join = StrokeJoin.Round),
		El.P("M12 9c-3 0-4-5 0-5s3 5 0 5z", stroke = W, sw = 2.2f, join = StrokeJoin.Round)
	))

	fun byName(n: String?): IconDef? = when (n) {
		"coin" -> coin
		"gem" -> gem
		"tv" -> tv
		"target" -> target
		"trophy" -> trophy
		"check" -> check
		"puMagnet" -> puMagnet
		"puShield" -> puShield
		"puClock" -> puClock
		"noads" -> noads
		"gift" -> gift
		else -> null
	}
}

/** Draws an icon inside a 24x24 viewBox scaled to [size]. */
fun DrawScope.drawIcon(def: IconDef, x: Float, y: Float, size: Float, tint: Color = Color.White) {
	val k = size / 24f
	translate(x, y) {
		scale(k, k, pivot = Offset.Zero) {
			for (e in def.els) {
				when (e) {
					is El.P -> {
						val p: Path = Svg.path(e.d)
						val f = if (e.fillCurrent) tint else e.fill
						if (f != null) drawPath(p, f)
						val s = if (e.strokeCurrent) tint else e.stroke
						if (s != null && e.sw > 0f) drawPath(p, s, style = Stroke(e.sw, cap = e.cap, join = e.join))
					}
					is El.Cir -> {
						e.fill?.let { drawCircle(it, e.r, Offset(e.cx, e.cy)) }
						val s = if (e.strokeCurrent) tint else e.stroke
						if (s != null && e.sw > 0f) drawCircle(s, e.r, Offset(e.cx, e.cy), style = Stroke(e.sw))
					}
					is El.Rct -> {
						e.fill?.let {
							drawRoundRect(it, Offset(e.x, e.y), Size(e.w, e.h), androidx.compose.ui.geometry.CornerRadius(e.rx, e.rx))
						}
						val s = if (e.strokeCurrent) tint else e.stroke
						if (s != null && e.sw > 0f) {
							drawRoundRect(
								s, Offset(e.x, e.y), Size(e.w, e.h),
								androidx.compose.ui.geometry.CornerRadius(e.rx, e.rx),
								style = Stroke(e.sw)
							)
						}
					}
					is El.Txt -> {
						val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
						paint.color = e.fill.toArgb32()
						paint.textSize = e.size
						paint.textAlign = android.graphics.Paint.Align.CENTER
						paint.typeface = Fonts.black
						drawContext.canvas.nativeCanvas.drawText(e.text, e.x, e.y, paint)
					}
				}
			}
		}
	}
}

fun Color.toArgb32(): Int = android.graphics.Color.argb(
	(alpha * 255f + 0.5f).toInt(),
	(red * 255f + 0.5f).toInt(),
	(green * 255f + 0.5f).toInt(),
	(blue * 255f + 0.5f).toInt()
)

@Composable
fun Icon24(def: IconDef, size: Dp = 18.dp, tint: Color = Color.White, modifier: Modifier = Modifier) {
	Canvas(modifier.size(size)) {
		drawIcon(def, 0f, 0f, this.size.width, tint)
	}
}
