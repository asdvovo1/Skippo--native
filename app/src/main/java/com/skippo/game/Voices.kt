package com.skippo.game

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/**
 * أصوات الـ VIP.
 *
 * - كل لاعب VIP له صوته في `assets/sfx/players/<id>.mp3`
 * - كل منتخب VIP له هتافه في `assets/sfx/arenas/<id>.mp3`
 *
 * الصوت بيتعاد لوحده لما يخلص (looping)، وبيقف لما تسيب اللاعب / الملعب أو لما
 * تقفل الـ SFX من الإعدادات. [Sfx] فاضل زي ما هو للأصوات المولّدة (نغمات
 * الجمب والكوينز والريح) - الملف ده بس اللي بيشغّل ملفات mp3 حقيقية.
 */
object Voices {

	private var app: Context? = null

	/** مربوطين بإعدادات [Store]: سويتش الـ SFX + الفوليوم (0..100). */
	@Volatile var on: Boolean = true
	@Volatile var vol: Int = 70

	// slot اللاعب
	private var pMp: MediaPlayer? = null
	private var pId: String? = null

	// slot الملعب / المنتخب
	private var aMp: MediaPlayer? = null
	private var aId: String? = null

	fun init(ctx: Context) {
		app = ctx.applicationContext
	}

	private fun gain(): Float =
		if (!on) 0f else (vol.coerceIn(0, 100) / 100f) * 0.9f

	/** صوت اللاعب الـ VIP الملبوس - `null` يعني اسكت. */
	fun setPlayer(id: String?) {
		if (id != null && id == pId && pMp != null) {
			kick(pMp)
			return
		}
		pMp = swap(pMp, if (id == null) null else "sfx/players/$id.mp3")
		pId = if (pMp == null) null else id
	}

	/** هتاف المنتخب الـ VIP المختار - `null` يعني اسكت. */
	fun setArena(id: String?) {
		if (id != null && id == aId && aMp != null) {
			kick(aMp)
			return
		}
		aMp = swap(aMp, if (id == null) null else "sfx/arenas/$id.mp3")
		aId = if (aMp == null) null else id
	}

	/** يظبّط الفوليوم ويشغّل / يوقف حسب سويتش الـ SFX. */
	fun refresh() {
		val g = gain()
		for (mp in listOf(pMp, aMp)) {
			if (mp == null) continue
			try {
				mp.setVolume(g, g)
				if (!on) {
					if (mp.isPlaying) mp.pause()
				} else if (!mp.isPlaying) {
					mp.start()
				}
			} catch (t: Throwable) {
				// المشغّل لسه بيحمّل - اللي جاي هيظبّطه
			}
		}
	}

	/** الأبليكيشن راح للخلفية / الجيم اتوقف. */
	fun pauseAll() {
		for (mp in listOf(pMp, aMp)) {
			if (mp == null) continue
			try {
				if (mp.isPlaying) mp.pause()
			} catch (t: Throwable) {
			}
		}
	}

	fun resumeAll() {
		if (!on) return
		val g = gain()
		for (mp in listOf(pMp, aMp)) {
			if (mp == null) continue
			try {
				mp.setVolume(g, g)
				if (!mp.isPlaying) mp.start()
			} catch (t: Throwable) {
			}
		}
	}

	fun stopAll() {
		pMp = swap(pMp, null)
		pId = null
		aMp = swap(aMp, null)
		aId = null
	}

	fun release() {
		stopAll()
	}

	private fun kick(mp: MediaPlayer?) {
		if (mp == null) return
		try {
			val g = gain()
			mp.setVolume(g, g)
			if (on && !mp.isPlaying) mp.start()
		} catch (t: Throwable) {
		}
	}

	/** يقفل القديم ويفتح الجديد على وضع التكرار. */
	private fun swap(old: MediaPlayer?, path: String?): MediaPlayer? {
		try {
			old?.reset()
		} catch (t: Throwable) {
		}
		try {
			old?.release()
		} catch (t: Throwable) {
		}

		val ctx = app
		if (path == null || ctx == null) return null

		val mp = try {
			MediaPlayer()
		} catch (t: Throwable) {
			return null
		}
		mp.setAudioAttributes(
			AudioAttributes.Builder()
				.setUsage(AudioAttributes.USAGE_GAME)
				.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
				.build()
		)

		// مهم: openFd() بيرمي استثناء لو الـ mp3 متخزّن مضغوط جوّا الـ apk.
		// قبل كده كان الاستثناء بيتمسك بصمت والنتيجة مفيش صوت خالص ومفيش
		// أي رسالة خطأ. دلوقتي لو فشل، بنفكّ الملف مرة واحدة في الكاش ونشغّله
		// من هناك، فالصوت بيطلع مهما كان إعداد الضغط في Gradle.
		var ok = false
		val fd = try {
			ctx.assets.openFd(path)
		} catch (t: Throwable) {
			null
		}
		if (fd != null) {
			try {
				mp.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
				ok = true
			} catch (t: Throwable) {
			} finally {
				try {
					fd.close()
				} catch (t: Throwable) {
				}
			}
		}
		if (!ok) {
			val f = extract(ctx, path)
			if (f != null) {
				try {
					mp.setDataSource(f.absolutePath)
					ok = true
				} catch (t: Throwable) {
				}
			}
		}
		if (!ok) {
			try {
				mp.release()
			} catch (t: Throwable) {
			}
			return null
		}

		return try {
			mp.isLooping = true                 // يعيد نفسه لما يخلص
			val g = gain()
			mp.setVolume(g, g)
			mp.setOnPreparedListener { p ->
				try {
					p.isLooping = true
					p.setVolume(gain(), gain())
					if (on) p.start()
				} catch (t: Throwable) {
				}
			}
			mp.setOnErrorListener { p, _, _ ->
				if (p === pMp) {
					pMp = null
					pId = null
				}
				if (p === aMp) {
					aMp = null
					aId = null
				}
				try {
					p.reset()
					p.release()
				} catch (t: Throwable) {
				}
				true
			}
			mp.prepareAsync()                   // بره الـ main thread
			mp
		} catch (t: Throwable) {
			try {
				mp.release()
			} catch (t2: Throwable) {
			}
			null
		}
	}

	/**
	 * يفكّ ملف صوت من الـ assets لملف حقيقي في الكاش (مرة واحدة بس،
	 * وبعدين بيتعاد استخدامه). الحل الاحتياطي لما openFd مايقدرش يفتح
	 * الأصل المضغوط.
	 */
	private fun extract(ctx: Context, path: String): java.io.File? {
		return try {
			val out = java.io.File(ctx.cacheDir, "voices/" + path.replace('/', '_'))
			if (out.exists() && out.length() > 0L) return out
			out.parentFile?.mkdirs()
			ctx.assets.open(path).use { ins ->
				java.io.FileOutputStream(out).use { os ->
					ins.copyTo(os)
				}
			}
			if (out.length() > 0L) out else null
		} catch (t: Throwable) {
			null
		}
	}
}
