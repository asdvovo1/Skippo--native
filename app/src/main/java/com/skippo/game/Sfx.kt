package com.skippo.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Re-implementation of the Web Audio graph in index.html.
 *
 * Everything here mirrors the original numbers exactly:
 *  - tone(f, d, type, vm)  ->  gain 0.2*vm*(vol/100), exponential ramp to 0.001
 *  - wind: 2 s white noise loop -> bandpass(480 Hz, Q 0.7) modulated by a
 *    0.09 Hz LFO with +/-260 Hz depth -> gain 0.06*(vol/100)
 *  - music: 16 step sequencer, spb = 60/((play)?130:100)/2
 *
 * Oscillators use band limited wavetables, which is what the Web Audio
 * built-in waveforms do internally, so the timbre matches instead of aliasing.
 */
object Sfx {

	const val SR = 44100
	private const val NY = SR / 2.0

	const val SINE = 0
	const val SQUARE = 1
	const val SAW = 2
	const val TRI = 3

	// ---------------- band limited wavetables ----------------

	private const val TSIZE = 2048
	private val tables = HashMap<Long, FloatArray>()

	private fun table(type: Int, maxHarm: Int): FloatArray {
		val key = type.toLong() * 100000L + maxHarm
		tables[key]?.let { return it }
		val t = FloatArray(TSIZE)
		for (i in 0 until TSIZE) {
			val th = 2.0 * PI * i / TSIZE
			var v = 0.0
			when (type) {
				SINE -> v = sin(th)
				SQUARE -> { var n = 1; while (n <= maxHarm) { v += sin(n * th) / n; n += 2 }; v *= 4.0 / PI }
				SAW -> { var n = 1; while (n <= maxHarm) { v += (if (n % 2 == 1) 1.0 else -1.0) * sin(n * th) / n; n++ }; v *= 2.0 / PI }
				TRI -> { var n = 1; var s = 1.0; while (n <= maxHarm) { v += s * sin(n * th) / (n.toDouble() * n); s = -s; n += 2 }; v *= 8.0 / (PI * PI) }
			}
			t[i] = v.toFloat()
		}
		tables[key] = t
		return t
	}

	private fun harmFor(freq: Double): Int {
		if (freq <= 0.0) return 1
		return floor(NY / freq).toInt().coerceIn(1, 512)
	}

	// ---------------- voices ----------------

	private class Voice(
		val freq: Double,
		type: Int,
		val start: Long,          // absolute sample index when the note starts
		val end: Long,            // absolute sample index when the note stops
		val music: Boolean,
		val peak: Double,         // tone(): starting gain. music(): ramp target
		val durSamples: Long
	) {
		val tbl: FloatArray = table(type, harmFor(freq))
		var phase = 0.0
		val inc = freq / SR * TSIZE

		fun sample(n: Long): Float {
			val i = phase.toInt()
			val fr = (phase - i).toFloat()
			val a = tbl[i and (TSIZE - 1)]
			val b = tbl[(i + 1) and (TSIZE - 1)]
			phase += inc
			if (phase >= TSIZE) phase -= TSIZE
			val s = a + (b - a) * fr
			return (s * gain(n)).toFloat()
		}

		/** exact reproduction of the scheduled AudioParam curves */
		private fun gain(n: Long): Double {
			val el = (n - start).toDouble() / SR
			if (el < 0) return 0.0
			val dur = durSamples.toDouble() / SR
			return if (!music) {
				// setValueAtTime(v) then exponentialRampToValueAtTime(0.001, t+d)
				if (el >= dur) 0.0 else peak * (0.001 / peak).pow(el / dur)
			} else {
				// 0.0001 -> linear 0.025 s -> peak -> exponential to 0.0008 at dur
				when {
					el < 0.025 -> 0.0001 + (peak - 0.0001) * (el / 0.025)
					el < dur -> {
						val p = (el - 0.025) / (dur - 0.025).coerceAtLeast(1e-6)
						peak * (0.0008 / peak).pow(p)
					}
					else -> 0.0008
				}
			}
		}
	}

	// ---------------- state ----------------

	private var track: AudioTrack? = null
	private var running = false
	private var clock = 0L                      // absolute sample counter
	private val voices = ArrayList<Voice>()
	private val pending = ArrayList<Voice>()
	private val lock = Any()

	// settings mirrored from S
	@Volatile var sfxOn = true
	@Volatile var ambientOn = true
	@Volatile var vol = 70
	@Volatile var playing = false               // st.mode === 'play'
	@Volatile var bio = "hills"

	// wind
	private var windOn = false
	private var windG = 0.0
	private var windTarget = 0.0
	private var windTc = 0.4
	private lateinit var noise: FloatArray
	private var noiseIdx = 0
	private var z1 = 0.0; private var z2 = 0.0; private var y1 = 0.0; private var y2 = 0.0
	private var b0 = 0.0; private var b1 = 0.0; private var b2 = 0.0; private var a1 = 0.0; private var a2 = 0.0
	private var lfoPhase = 0.0

	// music
	private var mOn = false
	private var mGain = 0.0
	private var mTarget = 0.0
	private var mTc = 0.5
	private var mNext = 0L
	private var mStep = 0

	// لما هتاف ملعب VIP يشتغل، الموسيقى المولّدة بتتقفل مؤقتًا من غير ما
	// نلمس إعداد Ambient بتاع اللاعب - أول ما الهتاف يقف ترجع زي ما كانت.
	@Volatile private var musicDuck = false

	private val M_SCALES = mapOf(
		"hills" to intArrayOf(0, 4, 7, 11, 12, 7, 4, 2),
		"forest" to intArrayOf(0, 3, 7, 10, 12, 10, 7, 3),
		"city" to intArrayOf(0, 5, 7, 12, 10, 7, 5, 3),
		"dunes" to intArrayOf(0, 2, 5, 7, 9, 7, 5, 2),
		"space" to intArrayOf(0, 7, 12, 16, 19, 16, 12, 7),
		"ocean" to intArrayOf(0, 3, 5, 10, 12, 10, 5, 3)
	)

	private fun mRoot(): Double = when (bio) {
		"city" -> 196.0
		"space" -> 146.83
		"ocean" -> 164.81
		"forest" -> 155.56
		else -> 174.61
	}

	// ---------------- public api ----------------

	fun start() {
		if (running) return
		running = true
		noise = FloatArray(2 * SR) { (Random.nextDouble() * 2.0 - 1.0).toFloat() }
		setBandpass(480.0)

		val minBuf = AudioTrack.getMinBufferSize(
			SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT
		).coerceAtLeast(4096)

		val t = AudioTrack.Builder()
			.setAudioAttributes(
				AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_GAME)
					.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
					.build()
			)
			.setAudioFormat(
				AudioFormat.Builder()
					.setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
					.setSampleRate(SR)
					.setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
					.build()
			)
			.setBufferSizeInBytes(minBuf * 2)
			.setTransferMode(AudioTrack.MODE_STREAM)
			.build()
		track = t
		t.play()

		thread(name = "skippo-audio", isDaemon = true) {
			val block = 512
			val buf = FloatArray(block)
			while (running) {
				render(buf)
				try {
					t.write(buf, 0, block, AudioTrack.WRITE_BLOCKING)
				} catch (_: Throwable) {
					break
				}
			}
		}
	}

	fun release() {
		running = false
		try { track?.stop(); track?.release() } catch (_: Throwable) {}
		track = null
	}

	/** tone(f, d, type, vm) with an optional setTimeout style delay in ms */
	fun tone(f: Double, d: Double, type: Int, vm: Double = 1.0, delayMs: Int = 0) {
		if (!sfxOn) return
		val v = 0.2 * vm * (vol / 100.0)
		if (v <= 0.0) return
		val st = clock + (delayMs / 1000.0 * SR).toLong()
		val dur = (d * SR).toLong()
		synchronized(lock) {
			pending.add(Voice(f, type, st, st + dur, false, v, dur))
		}
	}

	fun sJump() = tone(440.0, 0.1, SQUARE, 0.6)

	fun sCoin() {
		tone(880.0, 0.05, SINE, 0.8)
		tone(1200.0, 0.1, SINE, 0.8, 50)
	}

	fun sHit() {
		tone(150.0, 0.3, SAW, 1.0)
		tone(100.0, 0.4, SAW, 1.0, 100)
	}

	fun sBuy() {
		tone(660.0, 0.08, SINE, 0.7)
		tone(990.0, 0.12, SINE, 0.7, 80)
	}

	fun sErr() = tone(180.0, 0.2, SQUARE, 0.5)

	fun sPower() {
		tone(520.0, 0.06, SINE, 0.7)
		tone(780.0, 0.06, SINE, 0.7, 60)
		tone(1040.0, 0.1, SINE, 0.7, 120)
	}

	/** setWind(on) - also drives musicSet(on), exactly like the original. */
	fun setWind(on: Boolean) {
		synchronized(lock) {
			windOn = on
			windTarget = if (on) 0.06 * (vol / 100.0) else 0.0
			windTc = if (on) 0.4 else 0.08
			musicSet(on)
		}
	}

	private fun musicSet(on: Boolean) {
		if (on && ambientOn && !musicDuck) {
			mOn = true
			mTarget = 0.05 * (vol / 100.0)
			mTc = 0.5
			// لو الموسيقى كانت مقفولة شوية، الخطوة الجاية بتبقى قديمة والسيكوينسر
			// كان هيرمي 24 نوتة مرة واحدة أول ما ترجع. نبدأ من دلوقتي.
			if (mNext == 0L || mNext < clock) mNext = clock + (0.12 * SR).toLong()
		} else {
			mOn = false
			mTarget = 0.0
			mTc = 0.12
		}
	}

	/**
	 * يقفل / يفتح موسيقى الخلفية المولّدة من غير ما يلمس الريح.
	 *
	 * بيتندَه من [Voices] لما هتاف ملعب VIP يشتغل: الهتاف بيبقى هو الصوت
	 * الرئيسي والموسيقى بتفصل، وأول ما الهتاف يقف الموسيقى ترجع.
	 */
	fun setAmbientMusic(on: Boolean) {
		synchronized(lock) {
			val duck = !on
			if (musicDuck == duck) return
			musicDuck = duck
			musicSet(windOn)
		}
	}

	/** call after S.vol / S.ambient changed */
	fun refresh() {
		synchronized(lock) {
			if (windOn) {
				windTarget = 0.06 * (vol / 100.0)
				musicSet(true)
			}
		}
	}

	// ---------------- dsp ----------------

	/** RBJ constant 0 dB peak gain bandpass, same as Web Audio BiquadFilterNode. */
	private fun setBandpass(f0: Double) {
		val q = 0.7
		val w0 = 2.0 * PI * f0.coerceIn(10.0, NY - 100.0) / SR
		val alpha = sin(w0) / (2.0 * q)
		val a0 = 1.0 + alpha
		b0 = alpha / a0
		b1 = 0.0
		b2 = -alpha / a0
		a1 = -2.0 * cos(w0) / a0
		a2 = (1.0 - alpha) / a0
	}

	private fun schedMusic() {
		if (!mOn) return
		val spb = 60.0 / (if (playing) 130.0 else 100.0) / 2.0
		val sc = M_SCALES[bio] ?: M_SCALES["hills"]!!
		val root = mRoot()
		val spbS = (spb * SR).toLong()
		var guard = 0
		while (mNext < clock + (0.35 * SR).toLong() && guard++ < 24) {
			val s16 = mStep % 16
			if (s16 % 4 == 0) {
				addMusic(root / 2.0 * 2.0.pow(sc[(mStep / 4) % 8] / 12.0), mNext, spb * 3.2, TRI, 0.5)
			}
			addMusic(root * 2.0.pow(sc[mStep % 8] / 12.0), mNext, spb * 0.8, SINE, 0.3)
			if (s16 == 13) {
				addMusic(root * 2.0 * 2.0.pow(sc[(mStep + 3) % 8] / 12.0), mNext, spb * 1.4, TRI, 0.16)
			}
			mNext += spbS
			mStep++
		}
	}

	private val musicVoices = ArrayList<Voice>()

	private fun addMusic(f: Double, t0: Long, dur: Double, type: Int, v: Double) {
		val ds = (dur * SR).toLong()
		musicVoices.add(Voice(f, type, t0, t0 + ds + (0.06 * SR).toLong(), true, v, ds))
	}

	private fun render(out: FloatArray) {
		synchronized(lock) {
			if (pending.isNotEmpty()) { voices.addAll(pending); pending.clear() }
			schedMusic()
		}

		val n = out.size
		val kWind = 1.0 - exp(-1.0 / (windTc * SR))
		val kMus = 1.0 - exp(-1.0 / (mTc * SR))

		for (i in 0 until n) out[i] = 0f

		// oscillator voices (connected straight to destination)
		var vi = 0
		while (vi < voices.size) {
			val v = voices[vi]
			if (clock >= v.end) { voices.removeAt(vi); continue }
			var s = clock
			for (i in 0 until n) {
				if (s in v.start until v.end) out[i] += v.sample(s)
				s++
			}
			vi++
		}

		// music bus
		if (musicVoices.isNotEmpty() || mGain > 1e-6) {
			val mix = FloatArray(n)
			var mi = 0
			while (mi < musicVoices.size) {
				val v = musicVoices[mi]
				if (clock >= v.end) { musicVoices.removeAt(mi); continue }
				var s = clock
				for (i in 0 until n) {
					if (s in v.start until v.end) mix[i] += v.sample(s)
					s++
				}
				mi++
			}
			for (i in 0 until n) {
				mGain += (mTarget - mGain) * kMus
				out[i] += (mix[i] * mGain).toFloat()
			}
		}

		// wind bus: noise -> bandpass(480 +/- 260 @ 0.09 Hz) -> gain
		if (windOn || windG > 1e-6) {
			for (i in 0 until n) {
				if ((clock + i) % 32L == 0L) {
					setBandpass(480.0 + 260.0 * sin(lfoPhase))
				}
				lfoPhase += 2.0 * PI * 0.09 / SR
				if (lfoPhase > 2 * PI) lfoPhase -= 2 * PI

				val x = noise[noiseIdx].toDouble()
				noiseIdx++
				if (noiseIdx >= noise.size) noiseIdx = 0

				val y = b0 * x + b1 * z1 + b2 * z2 - a1 * y1 - a2 * y2
				z2 = z1; z1 = x; y2 = y1; y1 = y

				windG += (windTarget - windG) * kWind
				out[i] += (y * windG).toFloat()
			}
		}

		for (i in 0 until n) {
			if (out[i] > 1f) out[i] = 1f
			if (out[i] < -1f) out[i] = -1f
		}
		clock += n
	}

	@Suppress("unused")
	private fun unusedLn() = ln(1.0)

	@Suppress("unused")
	private val legacyStream = AudioManager.STREAM_MUSIC
}
