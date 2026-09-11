package com.skippo.game

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build

/**
 * The web build uses `system-ui, sans-serif` with weight 900.
 * On Android Chrome `system-ui` resolves to the very same system font that
 * Typeface.DEFAULT points at, so we deliberately do NOT ship a font file -
 * bundling one would make the app diverge from the browser on OEM skins.
 */
object Fonts {

	val black: Typeface by lazy {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			Typeface.create(Typeface.DEFAULT, 900, false)
		} else {
			Typeface.create("sans-serif-black", Typeface.NORMAL)
		}
	}

	val bold: Typeface by lazy {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			Typeface.create(Typeface.DEFAULT, 800, false)
		} else {
			Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
		}
	}

	/** ctx.font = '900 <px>px system-ui, sans-serif' */
	fun canvasPaint(px: Float, align: Paint.Align = Paint.Align.CENTER): Paint {
		val p = Paint(Paint.ANTI_ALIAS_FLAG)
		p.typeface = black
		p.textSize = px
		p.textAlign = align
		return p
	}
}
