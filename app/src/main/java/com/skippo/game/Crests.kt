package com.skippo.game

import android.content.Context
import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * شعارات الملاعب: كل ملعب VIP (نادي أو منتخب) ليه شعاره.
 *
 * - لو حطيت صورة شعار حقيقية في `assets/crests/<id>.webp` (أو `.png` / `.jpg`)
 *   بتترسم هي علطول مكان الشعار المرسوم.
 * - غير كده بنرسم درع بألوان الفريق + اختصار اسمه (مفيش أي ملفات خارجية
 *   مطلوبة ومفيش مشاكل حقوق).
 */
object Crests {

	private val EXTS = listOf("webp", "png", "jpg")

	private var am: AssetManager? = null
	private val cache = HashMap<String, ImageBitmap?>()

	fun init(ctx: Context) {
		am = ctx.applicationContext.assets
	}

	/** صورة شعار حقيقية لو موجودة جوّا assets/crests. */
	fun img(id: String): ImageBitmap? {
		val a = am ?: return null
		if (cache.containsKey(id)) return cache[id]
		var out: ImageBitmap? = null
		for (ext in EXTS) {
			try {
				a.open("crests/$id.$ext").use { s ->
					val bm = BitmapFactory.decodeStream(s)
					if (bm != null) out = bm.asImageBitmap()
				}
			} catch (t: Throwable) {
				// مفيش ملف بالامتداد ده - عادي
			}
			if (out != null) break
		}
		cache[id] = out
		return out
	}

	internal val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
		textAlign = Paint.Align.CENTER
	}
}

/** شكل الدرع. */
private fun crestPath(cx: Float, cy: Float, r: Float): Path {
	val p = Path()
	val top = cy - r * 1.02f
	val bot = cy + r * 1.18f
	p.moveTo(cx - r, top + r * 0.2f)
	p.quadraticBezierTo(cx - r, top, cx - r * 0.76f, top)
	p.lineTo(cx + r * 0.76f, top)
	p.quadraticBezierTo(cx + r, top, cx + r, top + r * 0.2f)
	p.lineTo(cx + r, cy + r * 0.36f)
	p.quadraticBezierTo(cx + r * 0.9f, bot - r * 0.12f, cx, bot)
	p.quadraticBezierTo(cx - r * 0.9f, bot - r * 0.12f, cx - r, cy + r * 0.36f)
	p.close()
	return p
}

/**
 * بيرسم شعار الفريق: صورة حقيقية لو موجودة، وغير كده درع بلونين
 * واختصار اسم النادي / المنتخب.
 */
fun DrawScope.crestBadge(
	id: String,
	cx: Float,
	cy: Float,
	r: Float,
	c1: Color,
	c2: Color,
	code: String?,
	alpha: Float = 1f
) {
	val logo = Crests.img(id)
	if (logo != null) {
		val w = r * 2.1f
		val h = w * logo.height.toFloat() / logo.width.toFloat()
		drawImage(
			image = logo,
			srcOffset = IntOffset(0, 0),
			srcSize = IntSize(logo.width, logo.height),
			dstOffset = IntOffset((cx - w / 2f).roundToInt(), (cy - h / 2f).roundToInt()),
			dstSize = IntSize(max(1f, w).roundToInt(), max(1f, h).roundToInt()),
			alpha = alpha,
			filterQuality = FilterQuality.Medium
		)
		return
	}

	val p = crestPath(cx, cy, r)
	drawPath(p, c1.copy(alpha = alpha))
	clipPath(p) {
		// نص الدرع بلون الفانلة التاني + خط عرضي خفيف
		drawRect(c2.copy(alpha = alpha), Offset(cx, cy - r * 1.5f), Size(r * 1.4f, r * 3f))
		drawRect(
			Color.White.copy(alpha = 0.13f * alpha),
			Offset(cx - r * 1.4f, cy - r * 0.17f),
			Size(r * 2.8f, r * 0.34f)
		)
	}
	drawPath(p, Color.White.copy(alpha = 0.88f * alpha), style = Stroke(max(1.6f, r * 0.11f)))
	drawPath(p, Color.Black.copy(alpha = 0.2f * alpha), style = Stroke(max(1f, r * 0.04f)))

	if (code != null && code.isNotEmpty()) {
		val pt = Crests.paint
		pt.textSize = r * 0.58f
		pt.color = android.graphics.Color.WHITE
		pt.alpha = (255f * alpha).toInt().coerceIn(0, 255)
		pt.setShadowLayer(max(1.5f, r * 0.13f), 0f, r * 0.05f, android.graphics.Color.argb(150, 0, 0, 0))
		drawContext.canvas.nativeCanvas.drawText(code, cx, cy + r * 0.24f, pt)
	}
}
