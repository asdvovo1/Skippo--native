package com.skippo.game

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class Obstacle(
	var x: Float, var y: Float, var w: Float, var h: Float,
	var s: Boolean, var type: String,
	var baseY: Float = 0f, var amp: Float = 0f, var phase: Float = 0f
)

/**
 * كوينة عادية، أو كورة الملعب (ball = true) اللي بتتحسب worth كوينز مرة واحدة.
 */
class Coin(
	var x: Float, var y: Float, var r: Float, var g: Boolean, var b: Float,
	var ball: Boolean = false, var worth: Int = 1
)
class Pow(var x: Float, var y: Float, var r: Float, var type: String, var got: Boolean, var b: Float)
class Pt(var x: Float, var y: Float, var vx: Float, var vy: Float, var l: Float, var c: Color, var s: Float)
class Flo(var x: Float, var y: Float, var l: Float, var txt: String)
class GlowPt(var x: Float, var y: Float, var life: Float, var seed: Int)
class Amb(var type: String, var x: Float, var y: Float, var r: Float, var v: Float, var tw: Float = 0f)
class TrailPt(var x: Float, var y: Float)

/**
 * الكورة اللي اللاعب بيسوقها برجله: بيزقها كل خطوة وهو بيجري، ولما ينط
 * بينططها برجله في الهوا. كل الأرقام بالبكسل ماعدا spin اللي بالدرجات.
 */
class Dribble {
	/** المسافة الأفقية بين نص اللاعب ومركز الكورة. */
	var lead = 0f
	var vLead = 0f

	/** ارتفاع مركز الكورة فوق الأرض. */
	var y = 0f
	var vy = 0f

	/** زاوية اللف ومعدل اللف (درجة / فريم). */
	var spin = 0f
	var spinVel = 0f

	/** آخر لحظة في دورة الخطوات لمست فيها الرجل الكورة. */
	var lastTouch = -99f

	/** مانع رجفة: مفيش لمستين ورا بعض في نفس اللحطة. */
	var touchCd = 0f

	/** عدد تنطيطات الرجل في النطة الحالية. */
	var juggles = 0

	/** فلاش صغير مكان اللمسة عشان الزقة تبان. */
	var kickFx = 0f
}
/** A decorative sky cloud. Named SkyCloud so it does not clash with `object Cloud` (the Firebase layer in Cloud.kt). */
class SkyCloud(var x: Float, var y: Float, var w: Float, var dur: Float, var t: Float)

class Player {
	var x = 0f
	var y = 0f
	var r = 26f
	var vy = 0f
	var g = false
	/** نطات الهوا اللي اتستعملت من غير ما اللاعب ينزل الأرض. */
	var airJumps = 0
	var rot = 0f
	var sq = 1f
	val trailPts = ArrayList<TrailPt>()
}

class RunStats {
	var coins = 0
	var maxCombo = 0
}

class St {
	var mode = "start"      // start | play | pause | over
	var t = 0
	var score = 0
	var coins = 0
	var spd = 10f
	var dist = 0f
	var level = 1
	var combo = 0
	var comboT = 0f
	var magnetT = 0f
	var shieldT = 0f
	var slowT = 0f
	var revived = false
	var countedRun = false
	var bankedCoins = 0
	var bankedRunCoins = 0
	var bankedScore = 0
	val run = RunStats()
}

/**
 * Direct port of the game logic in index.html.
 * All values are in CSS pixels, which map 1:1 to Android dp.
 */
class Engine(val S: Store) {

	companion object {
		const val GRAV = 1.0f
		const val JUMP_V = -18f
		const val WORLD_SPAN = 15

		/** نطة إضافية في الهوا: دوس تاني وانت طاير = نطة مزدوجة. */
		const val MAX_AIR_JUMPS = 1
		/** نطة الهوا أخف شوية من نطة الأرض عشان ما تعديش الشاشة كلها. */
		const val AIR_JUMP_SCALE = 0.92f

		/** Travel, in css px, that advances the runner sprite by one frame. */
		const val STRIDE = 9f
		/** Ceiling on sprite frames per game frame, so the legs never blur out. */
		const val CADENCE = 0.4f
		/** How fast the menu scene walks the runner past the camera. */
		const val MENU_DRIFT = 2.8f
		/** الكورة اللي في اللعب: لما تاخدها بتتحسب 5 كوينز. */
		const val BALL_COINS = 5

		/** نصف قطر كورة الدربلة كنسبة من نصف قطر اللاعب. */
		const val BALL_RAD = 0.42f
		/** جاذبية الكورة أخف من جاذبية اللاعب عشان التنطيط يبان. */
		const val BALL_GRAV = 0.62f
		/** قوة الزقة اللي الرجل بتدفع بيها الكورة لقدام. */
		const val KICK_PUSH = 1.45f
		/** الرفعة الصغيرة اللي بتاخدها الكورة من طرف الجزمة. */
		const val KICK_HOP = 1.7f
		/** قوة تنطيط الكورة بالرجل وهو في الهوا. */
		const val JUGGLE_UP = 3.4f
		/** أقصى معدل لف للكورة (درجة/فريم) - أسرع من كده بيبان غير واقعي. */
		const val SPIN_MAX = 13f
		val POWTYPES = arrayOf("magnet", "shield", "slow")
		val SPARK = arrayOf(Color(0xFFFFD54A), Color(0xFFFF9D3C), Color(0xFFFFF2C0))
		val dv = mapOf(
			"easy" to Pair(7.5f, 0.0014f),
			"normal" to Pair(10f, 0.0028f),
			"hard" to Pair(13f, 0.0048f)
		)
	}

	// viewport in css px (== dp)
	var W = 620f
	var H = 560f

	val st = St()
	val p = Player()
	var obs = ArrayList<Obstacle>()
	var cns = ArrayList<Coin>()
	var pows = ArrayList<Pow>()
	var pts = ArrayList<Pt>()
	var flo = ArrayList<Flo>()
	var glowTrail = ArrayList<GlowPt>()
	var amb = ArrayList<Amb>()
	var clouds = ArrayList<SkyCloud>()

	var spwnT = 30f
	var powT = 400f + Random.nextFloat() * 300f
	var ballT = 300f + Random.nextFloat() * 260f
	var shake = 0f
	var best = 0

	/**
	 * Phase of the runner's step cycle. Driven by distance travelled so the feet
	 * stay planted on the pitch, rate limited so the legs read as real steps, and
	 * frozen while airborne so a jump holds one clean pose.
	 */
	var runCyc = 0f

	/** حالة الكورة اللي اللاعب بيزقها برجله وبينططها وهو طاير. */
	val drb = Dribble()

	var curGrav = GRAV
	var curJump = JUMP_V
	var curWind = 1f

	// hooks so the UI/audio layers can react without the engine knowing about them
	var onDie: (() -> Unit)? = null
	var onLevelUp: ((Int) -> Unit)? = null
	var onCoin: (() -> Unit)? = null
	var onPower: (() -> Unit)? = null
	var onJump: (() -> Unit)? = null
	var onBuzz: ((Int) -> Unit)? = null

	init {
		best = S.best
		for (i in 0 until 5) {
			clouds.add(
				SkyCloud(
					x = Random.nextFloat(),
					y = 5f + Random.nextFloat() * 45f,
					w = 80f + Random.nextFloat() * 80f,
					dur = 25f + Random.nextFloat() * 25f,
					t = Random.nextFloat()
				)
			)
		}
	}

	// ---------------- worlds ----------------

	// 1:1 with index.html -> worldIndex(){ Math.min(WORLDS.length-1, Math.floor(st.score/WORLD_SPAN)) }
	fun worldIndex(): Int = min(Catalog.WORLDS.size - 1, st.score / WORLD_SPAN)

	/** activeWorld(): the world overrides the theme once you climb levels. */
	fun activeWorld(): Item {
		// club/stadium themes stay put: the stadium look survives every level
		val club = Catalog.THEMES[S.theme]
		if (club?.kind == "club") return club
		val w = Catalog.WORLDS[worldIndex()]
		if (w != null) {
			return Item(w.ar, w.en, top = w.top, bot = w.bot, ground = w.ground, gTop = w.gTop)
		}
		return Catalog.THEMES[S.theme] ?: Catalog.THEMES["day"]!!
	}

	fun worldKey(): String? = Catalog.WORLDS[worldIndex()]?.id

	fun bioKey(): String {
		if (Catalog.THEMES[S.theme]?.kind == "club") return "stadium"
		val k = worldKey() ?: S.theme
		return when (k) {
			"forest" -> "forest"
			"desert" -> "dunes"
			"space" -> "space"
			"ocean" -> "ocean"
			"night", "neon", "sunset" -> "city"
			else -> "hills"
		}
	}

	/** ملاعب VIP بس هي اللي فيها شماريخ. */
	fun vipArena(): Boolean = Catalog.hasFlares(S.theme)

	fun applyWorldPhysics() {
		val w = Catalog.WORLDS[worldIndex()]
		curGrav = w?.grav ?: GRAV
		curJump = w?.jump ?: JUMP_V
		curWind = w?.wind ?: 1f
	}

	// ---------------- geometry ----------------

	/** gh(): ground height */
	fun gh(): Float {
		val tall = H > W * 1.25f
		return max(104f, if (tall) min(H * 0.33f, 420f) else min(196f, H * 0.25f))
	}

	fun maxRise(): Float = (curJump * curJump) / (2f * curGrav)
	fun maxObsHeight(): Float = max(60f, maxRise() - p.r * 2f - 24f)

	fun speedNow(): Float =
		(if (st.slowT > 0) st.spd * 0.55f else st.spd) * curWind * max(0.6f, min(1f, W / 620f))

	// ---------------- كورة اللاعب ----------------

	/** نصف قطر الكورة بالبكسل. */
	fun ballR(): Float = p.r * BALL_RAD

	/** عرض اللاعب على الشاشة - منه نعرف رجله بتوصل لفين. */
	private fun runnerW(): Float = p.r * 2.55f * Players[S.player].aspect

	/** المسافة الطبيعية بين نص اللاعب والكورة: قدام رجله بشعرة. */
	private fun restLead(): Float =
		if (p.g) max(p.r * 0.86f, runnerW() * 0.30f) + ballR()
		else max(p.r * 0.60f, runnerW() * 0.20f) + ballR() * 0.7f

	/** ترجيع الكورة قدام رجل اللاعب على الأرض. */
	fun resetDribble() {
		drb.lead = restLead()
		drb.vLead = 0f
		drb.y = ballR()
		drb.vy = 0f
		drb.spin = 0f
		drb.spinVel = 0f
		drb.lastTouch = -99f
		drb.touchCd = 0f
		drb.juggles = 0
		drb.kickFx = 0f
	}

	/**
	 * فيزياء الكورة. على الأرض: كل ما الرجل تكمل نص دورة خطوة بتزق الكورة لقدام
	 * وترفعها شعرة عن الأرض. في الهوا: الكورة بتفضل تحت رجله وهو بينططها بيها.
	 * اللف بقى بمعدل واقعي بسقف، بدل اللف المجنون اللي كان مربوط بالمسافة كلها.
	 */
	private fun stepDribble(f: Float, spd: Float) {
		val br = ballR()
		val rest = restLead()
		// رجل اللاعب فوق الأرض بقد إيه (صفر وهو ماشي)
		val footY = max(0f, (H - gh()) - (p.y + p.r))
		// زقة كل نص دورة خطوات، فالكورة ماشية مع الرجل مش مع الزمن
		val everyTouch = max(3f, Players[S.player].frames * 0.5f)
		if (drb.touchCd > 0f) drb.touchCd -= f

		if (p.g) {
			drb.juggles = 0
			if (runCyc - drb.lastTouch >= everyTouch && drb.touchCd <= 0f) {
				drb.lastTouch = runCyc
				drb.touchCd = 4f
				drb.vLead += KICK_PUSH + spd * 0.045f
				if (drb.y <= br * 1.4f) drb.vy = KICK_HOP
				drb.spinVel += 2.4f
				drb.kickFx = 7f
				kickDust()
			}
		} else {
			// اللمسة بتحصل لما الكورة تنزل لمستوى الجزمة وهي نازلة
			val reach = footY + br * 1.2f
			if (drb.vy <= 0f && drb.touchCd <= 0f && drb.y <= reach && drb.y >= reach - br * 3f) {
				drb.vy = JUGGLE_UP + footY * 0.035f
				drb.vLead += (rest - drb.lead) * 0.22f
				drb.spinVel = -drb.spinVel * 0.5f + 3f
				drb.touchCd = 6f
				drb.juggles++
				drb.kickFx = 7f
			}
		}

		// طيران الكورة ونطتها على الأرض
		drb.vy -= BALL_GRAV * f
		drb.y += drb.vy * f
		if (drb.y <= br) {
			drb.y = br
			drb.vy = if (drb.vy < -0.35f) -drb.vy * 0.42f else 0f
		}

		// الكورة مربوطة قدام رجله بسستة ناعمة، مش ملزوقة في مكان ثابت
		drb.vLead += (rest - drb.lead) * 0.030f * f
		drb.vLead *= max(0f, 1f - 0.075f * f)
		drb.lead += drb.vLead * f
		drb.lead = drb.lead.coerceIn(rest * 0.55f, rest * 1.95f)

		// لف واقعي: معدل التدحرج مهدّى ومحدود بسقف
		val roll = (spd / max(1f, br)) * 57.2958f * 0.28f
		drb.spinVel += (roll - drb.spinVel) * min(1f, 0.10f * f)
		drb.spinVel = drb.spinVel.coerceIn(-SPIN_MAX, SPIN_MAX)
		drb.spin += drb.spinVel * f
		if (drb.spin > 360f || drb.spin < -360f) drb.spin %= 360f
		if (drb.kickFx > 0f) drb.kickFx -= f
	}

	/** تراب صغير تحت الكورة وقت الزقة. */
	private fun kickDust() {
		if (st.mode != "play") return
		val bx = p.x + drb.lead
		val by = (H - gh()) - drb.y
		for (i in 0 until 3) {
			pts.add(
				Pt(
					bx - ballR() * 0.6f, by + ballR() * 0.8f,
					-1.6f - Random.nextFloat() * 2.2f, -Random.nextFloat() * 1.4f,
					10f + Random.nextFloat() * 6f, Color(0xFFFFFFFF), 2.6f
				)
			)
		}
	}

	// ---------------- lifecycle ----------------

	fun resetPlayer() {
		p.x = if (H > W * 1.25f) W * 0.2f else W * 0.25f
		p.y = H - gh() - p.r
		p.vy = 0f
		p.g = true
		p.airJumps = 0
		p.rot = 0f
		p.sq = 1f
		p.trailPts.clear()
		resetDribble()
	}

	/**
	 * Drops every leftover of a finished run: obstacles, coins, power-ups and
	 * effects. Without it the crate you crashed into stays frozen in the world
	 * while the menu scenery keeps scrolling, so the parked cube ends up sitting
	 * inside an obstacle on the start screen.
	 */
	fun clearWorld() {
		obs.clear(); cns.clear(); pows.clear(); pts.clear(); flo.clear()
		glowTrail.clear()
		spwnT = 30f
		powT = 400f + Random.nextFloat() * 300f
		ballT = 300f + Random.nextFloat() * 260f
	}

	/** Leaves the run and parks the cube on a clean menu scene. */
	fun toMenu() {
		st.mode = "start"
		clearWorld()
		resetPlayer()
	}

	fun startRun() {
		st.mode = "play"
		st.t = 0
		st.score = 0
		st.coins = 0
		st.spd = (dv[S.diff] ?: dv["normal"]!!).first
		st.dist = 0f
		st.level = 1
		st.combo = 0
		st.comboT = 0f
		st.magnetT = 0f
		st.shieldT = 0f
		st.slowT = 0f
		st.revived = false
		st.countedRun = false
		st.bankedCoins = 0
		st.bankedRunCoins = 0
		st.bankedScore = 0
		st.run.coins = 0
		st.run.maxCombo = 0
		obs.clear(); cns.clear(); pows.clear(); pts.clear(); flo.clear()
		glowTrail.clear(); amb.clear()
		spwnT = 30f
		powT = 400f + Random.nextFloat() * 300f
		ballT = 300f + Random.nextFloat() * 260f
		runCyc = 0f
		applyWorldPhysics()
		resetPlayer()
		if (S.perk("headstart")) st.shieldT = 480f * (if (S.perk("shieldpro")) 1.5f else 1f)
	}

	fun jump() {
		if (st.mode != "play") return
		// دوسة تانية واللاعب طاير = النطة المزدوجة. بعد ما تتستعمل لازم ينزل
		// الأرض الأول عشان ترجع تاني.
		val air = !p.g
		if (air && p.airJumps >= MAX_AIR_JUMPS) return
		if (air) p.airJumps++
		p.vy = if (air) curJump * AIR_JUMP_SCALE else curJump
		p.g = false
		// A person stretches up off the take-off foot. The old cube squashed flat
		// here, which made the hop look like a stumble.
		p.sq = if (air) 1.22f else 1.16f
		S.jumps++
		onJump?.invoke()
		onBuzz?.invoke(if (air) 14 else 10)
		if (air) {
			airJumpFx()
		} else {
			for (i in 0 until 6) {
				pts.add(
					Pt(
						p.x, p.y + p.r,
						(Random.nextFloat() - .5f) * 6f, Random.nextFloat() * 2f,
						14f, Color(0xFFFFFFFF), 4f
					)
				)
			}
		}
	}

	/** حلقة شرر تحت رجل اللاعب عشان النطة التانية تبان إنها حاجة مختلفة. */
	private fun airJumpFx() {
		for (i in 0 until 14) {
			val a = (i / 14f) * 6.2832f
			pts.add(
				Pt(
					p.x + cos(a) * p.r * 0.7f, p.y + p.r * 0.6f + sin(a) * p.r * 0.3f,
					cos(a) * 4.6f, sin(a) * 2.2f + 0.6f,
					18f, Color(0xFFBFEFFF), 3.4f
				)
			)
		}
	}

	fun landFx(vy: Float) {
		onBuzz?.invoke(6)
		val n = min(12, (abs(vy) * 0.6f).toInt() + 4)
		for (i in 0 until n) {
			pts.add(
				Pt(
					p.x + (Random.nextFloat() - .5f) * p.r, p.y + p.r,
					(Random.nextFloat() - .5f) * 7f, -Random.nextFloat() * 3f,
					16f, Color(0xFFFFFFFF), 4f
				)
			)
		}
	}

	// ---------------- spawning ----------------

	private fun rnd() = Random.nextFloat()

	fun addObs() {
		val g = gh()
		val cap = maxObsHeight()
		val roll = rnd()
		val type = when {
			roll < 0.34f -> "block"
			roll < 0.52f -> "tall"
			roll < 0.66f -> "spikes"
			roll < 0.8f -> "flyer"
			roll < 0.91f -> "mover"
			else -> "double"
		}
		val w = 34f + rnd() * 26f
		when (type) {
			"tall" -> {
				var h = 70f + rnd() * 50f
				h = min(h, cap)
				val o = Obstacle(W + 50f, H - g - h, w, h, false, "tall")
				obs.add(o)
				if (rnd() < 0.65f) cns.add(Coin(W + 50f + w / 2f, o.y - 70f - rnd() * 40f, 14f, false, rnd() * 6f))
			}
			"spikes" -> {
				var h = 40f + rnd() * 22f
				h = min(h, cap)
				val o = Obstacle(W + 50f, H - g - h, w, h, false, "spikes")
				obs.add(o)
				if (rnd() < 0.65f) cns.add(Coin(W + 50f + w / 2f, o.y - 70f - rnd() * 40f, 14f, false, rnd() * 6f))
			}
			"flyer" -> {
				val fy = H - g - p.r * 2f - 96f - rnd() * 40f
				val o = Obstacle(W + 50f, fy, w + 16f, 26f, false, "flyer")
				obs.add(o)
				if (rnd() < 0.5f) cns.add(Coin(W + 50f + w / 2f, H - g - p.r - 8f, 14f, false, rnd() * 6f))
			}
			"mover" -> {
				val mh = 34f + rnd() * 18f
				val midY = H - g - p.r * 2f - 90f - rnd() * 30f
				val o = Obstacle(W + 50f, midY, w + 10f, mh, false, "mover", midY, 34f + rnd() * 26f, rnd() * 6f)
				obs.add(o)
				if (rnd() < 0.5f) cns.add(Coin(W + 50f + w / 2f, H - g - p.r - 8f, 14f, false, rnd() * 6f))
			}
			"double" -> {
				var h1 = 34f + rnd() * 16f
				h1 = min(h1, cap)
				obs.add(Obstacle(W + 50f, H - g - h1, w, h1, false, "block"))
				val w2 = 34f + rnd() * 16f
				var h2 = 34f + rnd() * 16f
				h2 = min(h2, cap)
				val gap = p.r * 2f + 40f + rnd() * 20f
				obs.add(Obstacle(W + 50f + w + gap, H - g - h2, w2, h2, false, "block"))
				cns.add(Coin(W + 50f + w + gap * 0.5f, H - g - 152f - rnd() * 12f, 14f, false, rnd() * 6f))
			}
			else -> {
				var h = 40f + rnd() * 30f
				h = min(h, cap)
				val o = Obstacle(W + 50f, H - g - h, w, h, false, type)
				obs.add(o)
				if (rnd() < 0.65f) cns.add(Coin(W + 50f + w / 2f, o.y - 70f - rnd() * 40f, 14f, false, rnd() * 6f))
			}
		}
	}

	fun addPow() {
		val g = gh()
		val type = POWTYPES[floor(rnd() * POWTYPES.size).toInt().coerceIn(0, 2)]
		pows.add(Pow(W + 60f, H - g - 120f - rnd() * 70f, 20f, type, false, rnd() * 6f))
	}

	/**
	 * كورة الملعب: بتيجي في الهوا على ارتفاع تقدر توصله بنطة واحدة، ولما
	 * تاخدها بتتحسب BALL_COINS كوينز بدل كوين واحدة.
	 */
	fun addBall() {
		val g = gh()
		val reach = max(40f, min(maxRise() - p.r * 2f - 30f, 150f))
		val y = H - g - p.r - 14f - rnd() * reach
		cns.add(Coin(W + 60f, y, 21f, false, rnd() * 6f, true, BALL_COINS))
	}

	fun powColor(t: String): Color = when (t) {
		"magnet" -> Color(0xFFFF5A3C)
		"shield" -> Color(0xFF4A9FE0)
		else -> Color(0xFF9B5DE5)
	}

	fun glowColor(seed: Int = 0): Color? {
		val gl = Catalog.GLOWS[S.glow] ?: return null
		if (gl.kind == "rainbow") return hsl2c(((st.t * 4 + seed * 8) % 360).toDouble(), 95.0, 62.0)
		return gl.c
	}

	// ---------------- collision ----------------

	fun hitsObstacle(o: Obstacle): Boolean {
		val hw = p.r * 0.86f
		val hh = p.r * 0.86f
		val bl = p.x - hw; val br = p.x + hw
		val bt = p.y - hh; val bb = p.y + hh
		if (o.type == "mover") {
			val mcx = o.x + o.w / 2f
			val mcy = o.y + o.h / 2f
			val mr = (max(o.w, o.h) / 2f + 2f) * 0.76f
			val nx = max(bl, min(mcx, br))
			val ny = max(bt, min(mcy, bb))
			val dx = mcx - nx; val dy = mcy - ny
			return dx * dx + dy * dy < mr * mr
		}
		// الأقماع: الشكل مخروط، فالمساحة اللي بتتحسب بتضيق كل ما نطلع لفوق
		if (o.type == "block" || o.type == "tall") {
			val ct = o.y + 2f
			val cb = o.y + o.h
			if (bb <= ct || bt >= cb) return false
			val k = ((min(bb, cb) - ct) / max(1f, cb - ct)).coerceIn(0f, 1f)
			val ccx = o.x + o.w / 2f
			val half = (o.w / 2f - 1.5f) * (0.34f + 0.66f * k)
			return br > ccx - half && bl < ccx + half
		}
		var ox = o.x + 2f; var ow = o.w - 4f
		var oy = o.y + 2f; var oh = o.h - 4f
		if (o.type == "spikes") {
			ox = o.x + 3f; ow = o.w - 6f; oy = o.y + o.h * 0.2f; oh = o.h * 0.8f - 2f
		} else if (o.type == "flyer") {
			oy = o.y + 2f + sin(((st.t + o.x * 0.05f) * 0.12f).toDouble()).toFloat() * 3f
		}
		return br > ox && bl < ox + ow && bb > oy && bt < oy + oh
	}

	// ---------------- ambient ----------------

	fun updAmbient(f: Float, spd: Float) {
		val key = worldKey()
		if (key == "space") {
			if (st.t % 5 == 0) amb.add(
				Amb("star", W + 6f, rnd() * (H - gh() - 40f), 0.6f + rnd() * 1.8f, 0.4f + rnd() * 1.2f, rnd() * 6f)
			)
		} else if (key == "ocean") {
			if (st.t % 7 == 0) amb.add(
				Amb("bubble", rnd() * W, H - gh() + 8f, 2f + rnd() * 4f, 0.5f + rnd() * 0.9f)
			)
		}
		for (a in amb) {
			if (a.type == "star") a.x -= a.v * f * (spd / 10f + 0.4f) else a.y -= a.v * f
		}
		amb = ArrayList(amb.filter { it.x > -12f && it.y > -12f })
		if (amb.size > 140) amb = ArrayList(amb.subList(amb.size - 140, amb.size))
	}

	// ---------------- main step ----------------

	fun step(nowMs: Double, lastMs: Double) {
		var f = ((nowMs - lastMs) / 16.6667).toFloat()
		if (!f.isFinite() || f <= 0f) f = 1f
		if (f > 3f) f = 3f

		if (shake > 0.25f) shake *= 0.9f else shake = 0f

		if (st.mode == "play") {
			st.t++
			st.spd += (dv[S.diff] ?: dv["normal"]!!).second * f
			val spd = speedNow()
			st.dist += spd * f
			val lv = st.score / WORLD_SPAN + 1
			if (lv > st.level) {
				st.level = lv
				applyWorldPhysics()
				st.spd += 0.5f
				onLevelUp?.invoke(lv)
			}
			if (st.magnetT > 0) st.magnetT -= f
			if (st.shieldT > 0) st.shieldT -= f
			if (st.slowT > 0) st.slowT -= f
			if (st.comboT > 0) {
				st.comboT -= f
				if (st.comboT <= 0) { st.comboT = 0f; st.combo = 0 }
			}

			p.vy += curGrav * f
			p.y += p.vy * f
			val flr = H - gh() - p.r
			if (p.y >= flr) {
				// impact bends the knees instead of stretching the runner tall
				if (!p.g) { p.sq = 0.78f; landFx(p.vy) }
				p.y = flr; p.vy = 0f; p.g = true
				p.airJumps = 0
			}
			p.sq += (1f - p.sq) * min(1f, 0.15f * f)
			// A footballer is not the old cube: the jump goes straight up and comes
			// straight back down. Any lean is always pulled back to zero, so nothing
			// tips forward onto its face in mid-air.
			p.rot *= max(0f, 1f - 0.4f * f)
			if (abs(p.rot) < 0.012f) p.rot = 0f

			// Step cycle: paced by real travel, capped so the legs stay readable at
			// speed, and held still while airborne.
			if (p.g) runCyc += min(spd * f / STRIDE, CADENCE * f)
			stepDribble(f, spd)

			if (glowColor() != null) glowTrail.add(GlowPt(p.x, p.y, 1f, st.t))
			for (gt in glowTrail) { gt.x -= spd * f; gt.life -= 0.045f * f }
			glowTrail = ArrayList(glowTrail.filter { it.life > 0f && it.x > -60f })
			updAmbient(f, spd)

			if (p.g && st.t % 2 == 0) {
				val c = SPARK[floor(rnd() * 3f).toInt().coerceIn(0, 2)]
				pts.add(
					Pt(
						p.x - p.r * 0.7f, p.y + p.r * 0.6f,
						-3f - rnd() * 3f - spd * 0.15f, -rnd() * 2.5f,
						12f + rnd() * 8f, c, 3f
					)
				)
			}
			if (S.trail != "none") {
				p.trailPts.add(0, TrailPt(p.x, p.y))
				if (p.trailPts.size > 14) p.trailPts.removeAt(p.trailPts.size - 1)
			}

			spwnT -= f
			if (spwnT <= 0f) { addObs(); spwnT = 55f - min(25f, st.spd * 1.5f) + rnd() * 35f }
			powT -= f
			if (powT <= 0f) { addPow(); powT = 500f + rnd() * 400f }
			ballT -= f
			if (ballT <= 0f) { addBall(); ballT = 420f + rnd() * 380f }

			var died = false
			for (o in obs) {
				o.x -= spd * f
				if (o.type == "mover") {
					o.y = o.baseY + sin(((st.t + o.phase * 30f) * 0.06f).toDouble()).toFloat() * o.amp
				}
				if (!o.s && o.x + o.w < p.x - p.r) { o.s = true; st.score++ }
				if (hitsObstacle(o)) {
					if (st.shieldT > 0) {
						st.shieldT = 0f; o.s = true; o.x = -999f
						onPower?.invoke(); onBuzz?.invoke(20); shake = 16f
					} else died = true
				}
			}
			obs = ArrayList(obs.filter { it.x + it.w > -20f })

			for (c in cns) {
				if (st.magnetT > 0) {
					val dx = p.x - c.x; val dy = p.y - c.y
					val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
					if (dist < 220f && dist > 0f) { c.x += dx / dist * 7f * f; c.y += dy / dist * 7f * f }
				}
				c.x -= spd * f
				val ddx = p.x - c.x; val ddy = p.y - c.y
				if (!c.g && ddx * ddx + ddy * ddy < (p.r + c.r) * (p.r + c.r)) {
					c.g = true
					st.combo++
					st.comboT = 90f
					if (st.combo > st.run.maxCombo) st.run.maxCombo = st.combo
					var gain = c.worth * (if (S.doubler) 2 else 1) * (1 + st.combo / 3)
					if (S.perk("lucky")) gain = ceil(gain * 1.3).toInt()
					st.coins += gain
					st.run.coins += gain
					onCoin?.invoke()
					if (c.ball) onBuzz?.invoke(14)
					flo.add(Flo(c.x, c.y - 6f, if (c.ball) 52f else 40f, "+$gain"))
					val burst = if (c.ball) 20 else 10
					for (i in 0 until burst) pts.add(
						Pt(
							c.x, c.y,
							(rnd() - .5f) * (if (c.ball) 12f else 8f),
							(rnd() - .5f) * (if (c.ball) 12f else 8f),
							if (c.ball) 32f else 25f,
							if (c.ball && i % 3 == 0) Color(0xFFFFFFFF) else Color(0xFFFFC93C),
							5f
						)
					)
				}
			}
			cns = ArrayList(cns.filter { it.x > -20f && !it.g })

			for (pw in pows) {
				pw.x -= spd * f
				val dx = p.x - pw.x; val dy = p.y - pw.y
				if (!pw.got && dx * dx + dy * dy < (p.r + pw.r) * (p.r + pw.r)) {
					pw.got = true
					onPower?.invoke(); onBuzz?.invoke(15)
					when (pw.type) {
						"magnet" -> st.magnetT = 360f * (if (S.perk("magnetpro")) 1.6f else 1f)
						"shield" -> st.shieldT = 480f * (if (S.perk("shieldpro")) 1.5f else 1f)
						"slow" -> st.slowT = 300f * (if (S.perk("slowpro")) 1.6f else 1f)
					}
					for (i in 0 until 14) pts.add(
						Pt(pw.x, pw.y, (rnd() - .5f) * 9f, (rnd() - .5f) * 9f, 28f, powColor(pw.type), 5f)
					)
				}
			}
			pows = ArrayList(pows.filter { it.x > -30f && !it.got })

			if (died) die()
		}

		for (pt in pts) { pt.x += pt.vx * f; pt.y += pt.vy * f; pt.vy += 0.4f * f; pt.l -= f }
		pts = ArrayList(pts.filter { it.l > 0f })

		if (st.mode != "play") {
			// The menu scene actually travels: the runner walks the pitch past the
			// camera instead of marching on the spot behind the panels.
			st.dist += MENU_DRIFT * f
			runCyc += min(MENU_DRIFT * f / STRIDE, CADENCE * f)
			stepDribble(f, MENU_DRIFT)
			updAmbient(f, 4f)
			// The scenery still scrolls outside a run, so anything left in the world
			// has to travel with it at the same rate instead of hanging in place on
			// top of the cube. Only the menu drifts; a paused run stays put.
			if (st.mode == "start") {
				val drift = MENU_DRIFT * f
				for (o in obs) o.x -= drift
				for (c in cns) c.x -= drift
				for (pw in pows) pw.x -= drift
				obs = ArrayList(obs.filter { it.x + it.w > -20f })
				cns = ArrayList(cns.filter { it.x > -20f })
				pows = ArrayList(pows.filter { it.x > -30f })
			}
		}
		for (fo in flo) { fo.y -= 1.15f * f; fo.l -= f }
		flo = ArrayList(flo.filter { it.l > 0f })

		for (c in clouds) {
			c.t += f * 16.6667f / 1000f / c.dur
			if (c.t > 1f) c.t -= 1f
		}
	}

	fun die() {
		if (st.mode != "play") return
		shake = 30f
		st.mode = "over"
		// confetti in the runner kit colours
		val kit = Players[S.player]
		for (i in 0 until 20) pts.add(
			Pt(p.x, p.y, (rnd() - .5f) * 15f, (rnd() - .5f) * 15f, 40f,
				if (i % 3 == 0) kit.c2 else kit.c, 6f)
		)
		onDie?.invoke()
	}
}
