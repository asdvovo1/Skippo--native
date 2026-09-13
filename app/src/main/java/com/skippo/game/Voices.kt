package com.skippo.game

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/**
 * الأصوات الحقيقية (ملفات mp3) - غير [Sfx] اللي بيولّد النغمات والريح.
 *
 * خانتين بس بيشتغلوا:
 *  - صوت اللاعب: لاعيبة الـ VIP بس ليهم صوت في assets/sfx/players/<id>.mp3
 *  - هتاف الملعب: ملاعب الـ VIP ليها هتاف في assets/sfx/arenas/<id>.mp3
 *
 * قاعدة التشغيل:
 *  - لاعب VIP ملبوس  -> صوت اللاعب بس، وهتاف الملعب بيفصل.
 *  - لاعب عادي ملبوس -> هتاف ملعب الـ VIP هو اللي بيشتغل (عشان اللاعب العادي
 *    مالوش صوت)، والموسيقى المولّدة بتفصل طول ما الهتاف شغال.
 *
 * كل ده متطبّق في [apply] فمافيش مكان تاني يقدر يتعارض معاه.
 *
 * الإصلاح المهم: قبل كده كان بيتنده start() والمشغّل لسه في حالة Preparing،
 * وده بيرمي استثناء وساعات بيودّي الـ MediaPlayer لحالة Error فالصوت مايطلعش
 * خالص أو يطلع مرة ومرة لأ. دلوقتي كل خانة بتفتكر إذا كانت جاهزة ولا لأ،
 * والتشغيل بيستنى onPrepared.
 */
object Voices {

	private var app: Context? = null

	/** مربوطين بإعدادات [Store]: سويتش الـ SFX + الفوليوم (0..100). */
	@Volatile var on: Boolean = true
	@Volatile var vol: Int = 70

	/** خانة صوت واحدة: المشغّل + هويّة اللي شغال + حالة التحضير. */
	private class Slot {
		var mp: MediaPlayer? = null
		var id: String? = null
		var ready = false
		var wantPlay = true
	}

	private val pSlot = Slot()   // اللاعب
	private val aSlot = Slot()   // الملعب

	private val lock = Any()

	fun init(ctx: Context) {
		app = ctx.applicationContext
	}

	private fun gain(): Float =
		if (!on) 0f else (vol.coerceIn(0, 100) / 100f) * 0.9f

	/** فيه هتاف ملعب متحمّل دلوقتي؟ (الموسيقى بتفصل وهو شغال) */
	val arenaActive: Boolean get() = aSlot.mp != null

	/**
	 * المدخل الوحيد لتشغيل الأصوات.
	 *
	 * @param playerId اللاعب الملبوس (الفلترة بتحصل جوّه).
	 * @param themeId  الملعب الملبوس.
	 * @param active   شغّال دلوقتي؟ (لعب / متجر) - غير كده كله بيسكت.
	 */
	fun apply(playerId: String?, themeId: String?, active: Boolean) {
		if (!on || !active) {
			stopAll()
			return
		}

		// لاعب VIP = اللي بالجواهر، وهو الوحيد اللي ليه ملف صوت.
		val vipPlayer = playerId != null && Players[playerId].gem != null
		val voiceId = if (vipPlayer) playerId else null

		// الهتاف بيشتغل مع اللاعيبة العادية بس - لاعب الـ VIP صوته بيقفل الملعب.
		val arenaId = if (!vipPlayer && Catalog.hasFlares(themeId)) themeId else null

		setSlot(pSlot, if (voiceId == null) null else "sfx/players/" + voiceId + ".mp3", voiceId)
		setSlot(aSlot, if (arenaId == null) null else "sfx/arenas/" + arenaId + ".mp3", arenaId)
		syncMusic()
		refresh()
	}

	/** يظبّط الفوليوم ويشغّل / يوقف حسب سويتش الـ SFX. */
	fun refresh() {
		val g = gain()
		for (s in listOf(pSlot, aSlot)) {
			val mp = s.mp ?: continue
			s.wantPlay = on
			try {
				mp.setVolume(g, g)
			} catch (t: Throwable) {
			}
			if (!s.ready) continue          // لسه بيحمّل - onPrepared هيكمّل
			try {
				if (!on) {
					if (mp.isPlaying) mp.pause()
				} else if (!mp.isPlaying) {
					mp.start()
				}
			} catch (t: Throwable) {
			}
		}
	}

	/** الأبليكيشن راح للخلفية / الجيم اتوقف. */
	fun pauseAll() {
		for (s in listOf(pSlot, aSlot)) {
			s.wantPlay = false
			val mp = s.mp ?: continue
			if (!s.ready) continue
			try {
				if (mp.isPlaying) mp.pause()
			} catch (t: Throwable) {
			}
		}
	}

	fun resumeAll() {
		if (!on) return
		val g = gain()
		for (s in listOf(pSlot, aSlot)) {
			s.wantPlay = true
			val mp = s.mp ?: continue
			if (!s.ready) continue
			try {
				mp.setVolume(g, g)
				if (!mp.isPlaying) mp.start()
			} catch (t: Throwable) {
			}
		}
	}

	fun stopAll() {
		synchronized(lock) {
			close(pSlot)
			close(aSlot)
		}
		syncMusic()
	}

	fun release() {
		stopAll()
	}

	/** الموسيقى المولّدة بتفصل طول ما فيه هتاف ملعب شغال. */
	private fun syncMusic() {
		try {
			Sfx.setAmbientMusic(!arenaActive)
		} catch (t: Throwable) {
		}
	}

	/** يحمّل ملف الخانة، ولو هو نفسه اللي شغال بيسيبه زي ما هو. */
	private fun setSlot(s: Slot, path: String?, id: String?) {
		synchronized(lock) {
			if (id != null && id == s.id && s.mp != null) {
				s.wantPlay = on
				kick(s)
				return
			}

			close(s)
			if (path == null) return

			val mp = open(path, s) ?: return
			s.mp = mp
			s.id = id
			s.ready = false
			s.wantPlay = on
		}
	}

	private fun close(s: Slot) {
		val mp = s.mp
		s.mp = null
		s.id = null
		s.ready = false
		if (mp == null) return
		try {
			mp.setOnPreparedListener(null)
			mp.setOnErrorListener(null)
		} catch (t: Throwable) {
		}
		try {
			mp.reset()
		} catch (t: Throwable) {
		}
		try {
			mp.release()
		} catch (t: Throwable) {
		}
	}

	private fun kick(s: Slot) {
		val mp = s.mp ?: return
		if (!s.ready) return
		try {
			val g = gain()
			mp.setVolume(g, g)
			if (on && s.wantPlay && !mp.isPlaying) mp.start()
		} catch (t: Throwable) {
		}
	}

	/** يفتح ملف من الـ assets على وضع التكرار، والتشغيل بيستنى onPrepared. */
	private fun open(path: String, s: Slot): MediaPlayer? {
		val ctx = app ?: return null

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

		// openFd() بيفشل لو الـ mp3 متخزّن مضغوط جوّا الـ apk، فبنفكّه في الكاش
		// مرة واحدة ونشغّله من هناك - كده الصوت بيطلع مهما كان إعداد الضغط.
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
				if (s.mp === p) {
					s.ready = true
					try {
						p.isLooping = true
						val gg = gain()
						p.setVolume(gg, gg)
						if (on && s.wantPlay) p.start()
					} catch (t: Throwable) {
					}
				}
			}
			mp.setOnErrorListener { p, _, _ ->
				if (s.mp === p) {
					s.mp = null
					s.id = null
					s.ready = false
				}
				try {
					p.reset()
					p.release()
				} catch (t: Throwable) {
				}
				// الهتاف وقع -> الموسيقى ترجع بدل ما الجو يفضل ساكت.
				syncMusic()
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
	 * وبعدين بيتعاد استخدامه).
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
