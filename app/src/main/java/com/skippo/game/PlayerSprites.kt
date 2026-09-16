package com.skippo.game

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Decodes the runner sprite-sheets out of `assets/players`.
 *
 * A full sheet costs about a megabyte once decoded, so we deliberately keep
 * only a couple alive: whoever is running plus whoever was equipped before.
 * The shop scrolls past all 100 players, so it reads from a separate
 * quarter-scale cache that is cheap enough to hold many screenfuls.
 */
object PlayerSprites {

	private var am: AssetManager? = null

	/** Tiny LRU, so browsing the whole shop can never blow up the heap. */
	private class Cache(private val cap: Int) {
		private val map = HashMap<String, ImageBitmap?>()
		private val order = ArrayList<String>()

		fun peek(k: String): ImageBitmap? = map[k]
		fun has(k: String) = map.containsKey(k)

		fun get(k: String): ImageBitmap? {
			if (!map.containsKey(k)) return null
			order.remove(k)
			order.add(k)
			return map[k]
		}

		fun put(k: String, v: ImageBitmap?) {
			if (map.containsKey(k)) order.remove(k)
			map[k] = v
			order.add(k)
			while (order.size > cap) map.remove(order.removeAt(0))
		}
	}

	private val sheets = Cache(3)
	// 128 > كل اللاعيبة (100 عادي + الأساطير)، فالكرت اللي اتفتح مرة مايترميش من
	// الكاش وانت بترجع تزحلق - ده كان بيخلي الصور تتأخر وتتلخبط في المتجر.
	private val thumbs = Cache(128)
	/** Dedicated square face crops used by the store cards. */
	private val posters = Cache(128)
	private var ballImage: ImageBitmap? = null
	private var ballLoaded = false

	/** Call once from the activity, before anything tries to draw a player. */
	fun init(ctx: Context) {
		am = ctx.applicationContext.assets
	}

	private fun decode(id: String, sample: Int): ImageBitmap? {
		return decodeAsset("players/$id.webp", sample)
	}

	private fun decodeAsset(path: String, sample: Int): ImageBitmap? {
		val a = am ?: return null
		return try {
			val o = BitmapFactory.Options().apply {
				inSampleSize = sample
				inScaled = false
				inPreferredConfig = Bitmap.Config.ARGB_8888
			}
			a.open(path).use { BitmapFactory.decodeStream(it, null, o)?.asImageBitmap() }
		} catch (_: Throwable) {
			null
		}
	}

	/** Full-resolution sheet, for the runner on the field. */
	fun sheet(id: String): ImageBitmap? {
		if (sheets.has(id)) return sheets.get(id)
		val b = decode(id, 1)
		sheets.put(id, b)
		return b
	}

	/** Quarter-scale sheet, for the 60dp shop tiles. */
	fun thumb(id: String): ImageBitmap? {
		if (thumbs.has(id)) return thumbs.get(id)
		val b = decode(id, 4)
		thumbs.put(id, b)
		return b
	}

	/** Already-decoded thumbnail, or null - lets the shop paint without blocking. */
	fun cachedThumb(id: String): ImageBitmap? = thumbs.peek(id)

	/** Face-only square poster generated from the runner art. */
	fun poster(id: String): ImageBitmap? {
		if (posters.has(id)) return posters.get(id)
		val b = decodeAsset("posters/$id.webp", 2)
		posters.put(id, b)
		return b
	}

	fun cachedPoster(id: String): ImageBitmap? = posters.peek(id)

	/** Shared football artwork for the runner's dribble animation. */
	fun ball(): ImageBitmap? {
		if (ballLoaded) return ballImage
		ballLoaded = true
		ballImage = decodeAsset("ball.webp", 1)
		return ballImage
	}

	fun preload(id: String) {
		sheet(id)
	}
}
