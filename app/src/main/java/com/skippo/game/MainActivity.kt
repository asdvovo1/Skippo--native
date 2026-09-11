package com.skippo.game

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		PlayerSprites.init(this)
		Voices.init(this)
		Crests.init(this)
		// the web build runs edge to edge inside the CrazyGames frame
		WindowCompat.setDecorFitsSystemWindows(window, false)
		val c = WindowInsetsControllerCompat(window, window.decorView)
		c.hide(WindowInsetsCompat.Type.systemBars())
		c.systemBarsBehavior =
			WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
		setContent { GameRoot() }
	}

	override fun onPause() {
		super.onPause()
		Sfx.setWind(false)
		Voices.pauseAll()
	}

	override fun onResume() {
		super.onResume()
		Voices.resumeAll()
	}
}

@Composable
fun GameRoot() {
	val ctx = LocalContext.current
	val density = LocalDensity.current
	val S = remember { Store(ctx) }
	// anonymous sign-in + remote flags; a no-op when offline or unconfigured
	LaunchedEffect(Unit) {
		Cloud.init(S)
		Ads.init(ctx)
	}
	// decode the equipped runner off the main thread so frame one is clean
	LaunchedEffect(S.player) {
		withContext(Dispatchers.IO) { PlayerSprites.preload(S.player) }
	}
	val e = remember { Engine(S) }
	val renderer = remember { Renderer(e, S) }

	val vib = remember {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			(ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
		} else {
			@Suppress("DEPRECATION")
			ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
		}
	}

	var nav by remember { mutableStateOf(Nav.Start) }
	var prevNav by remember { mutableStateOf(Nav.Start) }
	var tick by remember { mutableIntStateOf(0) }
	var toast by remember { mutableStateOf("") }

	// navigator.vibrate(ms)
	fun buzz(ms: Int) {
		if (!S.haptic) return
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			vib.vibrate(VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
		} else {
			@Suppress("DEPRECATION")
			vib.vibrate(ms.toLong())
		}
	}

	DisposableEffect(Unit) {
		Sfx.vol = S.vol
		Sfx.bio = e.bioKey()
		Sfx.start()
		e.best = S.best
		e.onJump = { if (S.sfx) Sfx.sJump() }
		e.onCoin = { if (S.sfx) Sfx.sCoin() }
		e.onPower = { if (S.sfx) Sfx.sPower() }
		e.onBuzz = { ms -> buzz(ms) }
		e.onLevelUp = { lv ->
			if (S.sfx) Sfx.sPower()
			Sfx.bio = e.bioKey()
			Sfx.refresh()
			val w = Catalog.WORLDS[e.worldIndex()]
			toast = if (w != null) {
				Lang.t("level", S.lang) + " " + lv + "  \u2022  " + w.name(S.lang)
			} else {
				Lang.t("level", S.lang) + " " + lv
			}
		}
		e.onDie = {
			if (S.sfx) Sfx.sHit()
			buzz(40)
			dieCore(S, e)
			e.best = S.best
			nav = Nav.End
		}
		onDispose { Sfx.release(); Voices.release() }
	}

	// applyWind(): the wind bed + the soundtrack follow the Ambient switch
	LaunchedEffect(S.ambient, nav) { Sfx.setWind(S.ambient) }

	// أصوات الـ VIP: صوت اللاعب الملبوس + هتاف منتخب الملعب، والاتنين
	// بيتعادوا لوحدهم لما يخلصوا (looping) وبيقفوا في المينيو والبوز
	// S.rev لازم يبقى في المفاتيح: بقية حقول Store (theme/player/sfx/vol) مجرد
	// var عادي ومش Compose state، فتغييرها لوحده مابيعملش recomposition.
	// ومن غير recomposition الـ LaunchedEffect مابيقراش المفاتيح تاني، يعني
	// لما تلبس ملعب VIP من المتجر الهتاف ماكانش يشتغل. rev هو الوحيد
	// اللي mutableIntStateOf و touch() بيزوّده مع كل حفظ.
	LaunchedEffect(nav, S.rev, S.player, S.theme, S.sfx, S.vol) {
		Voices.on = S.sfx
		Voices.vol = S.vol
		when {
			!S.sfx -> Voices.stopAll()
			nav == Nav.Pause -> Voices.pauseAll()
			nav == Nav.Playing || nav == Nav.Shop -> {
				Voices.setPlayer(if (Players[S.player].gem != null) S.player else null)
				Voices.setArena(if (Catalog.hasFlares(S.theme)) S.theme else null)
				Voices.refresh()
			}
			else -> Voices.stopAll()
		}
	}

	BackHandler(enabled = nav != Nav.Start) {
		when (nav) {
			Nav.Playing -> { e.st.mode = "pause"; prevNav = Nav.Playing; nav = Nav.Pause }
			Nav.Pause, Nav.End -> { e.toMenu(); nav = Nav.Start }
			else -> nav = prevNav
		}
	}

	BoxWithConstraints(
		Modifier
			.fillMaxSize()
			.background(Color(0xFF8FD0F0))
	) {
		val cssW = maxWidth.value
		val cssH = maxHeight.value

		// SC = max(1, min(1.7, min(innerWidth/620, innerHeight/560)))
		val sc = max(1f, min(1.7f, min(cssW / 620f, cssH / 560f)))
		e.W = cssW / sc
		e.H = cssH / sc

		// The cube is parked using W/H, which are only known once the viewport has
		// been measured. Without this it stays at its construction-time position
		// (up in the top-left corner) on every screen except an active run.
		LaunchedEffect(cssW, cssH) {
			if (e.st.mode != "play") e.resetPlayer()
		}

		// requestAnimationFrame(loop)
		LaunchedEffect(Unit) {
			var last = withFrameNanos { it } / 1_000_000.0
			while (true) {
				withFrameNanos { now ->
					val ms = now / 1_000_000.0
					e.step(ms, last)
					last = ms
					tick += 1
				}
			}
		}

		val pxPerCss = with(density) { 1.dp.toPx() } * sc

		Canvas(
			Modifier
				.fillMaxSize()
				.pointerInput(Unit) {
					detectTapGestures(onPress = { if (e.st.mode == "play") e.jump() })
				}
		) {
			tick.let { }
			scale(pxPerCss, pxPerCss, pivot = Offset.Zero) {
				with(renderer) { drawAll() }
			}
		}

		// .ad-banner — menu only, same slot as showMenuBanner() in the web build
		if (nav == Nav.Start) {
			BannerAd(S, Modifier.align(Alignment.BottomCenter))
		}

		/* ------------------------------------------------------------ #hud */
		if (nav == Nav.Playing) {
			tick.let { }
			Row(
				Modifier
					.fillMaxWidth()
					.padding(22.dp),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.Top
			) {
				// .score-wrap
				Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
					ScoreText(e.st.score.toString(), clampCss(44f, 11f * cssW / 100f, 76f))
					if (e.st.combo >= 2) {
						Text(
							"x" + e.st.combo + " " + Lang.t("combo", S.lang),
							color = C.gold,
							fontSize = 20.sp,
							fontWeight = FontWeight.Black
						)
					}
				}
				// .hud-right
				Column(
					horizontalAlignment = Alignment.End,
					verticalArrangement = Arrangement.spacedBy(12.dp)
				) {
					// .icon-btn 64x64
					IconBtn(UiIcons.pause, size = 56) {
						e.st.mode = "pause"
						prevNav = Nav.Playing
						nav = Nav.Pause
					}
					Pill(IC.coin, e.st.coins.toString())
				}
			}

			// .powerup-timers { bottom:24; left:50% }
			Row(
				Modifier
					.align(Alignment.BottomCenter)
					.padding(bottom = 24.dp),
				horizontalArrangement = Arrangement.spacedBy(10.dp)
			) {
				if (e.st.magnetT > 0f) PuTimer(IC.puMagnet, ceil(e.st.magnetT / 60f).toInt())
				if (e.st.shieldT > 0f) PuTimer(IC.puShield, ceil(e.st.shieldT / 60f).toInt())
				if (e.st.slowT > 0f) PuTimer(IC.puClock, ceil(e.st.slowT / 60f).toInt())
			}
		}

		/* -------------------------------------------------------- .screen */
		GameScreens(
			nav = nav,
			S = S,
			e = e,
			setNav = { n -> prevNav = nav; nav = n },
			back = { nav = prevNav },
			toast = { m -> toast = m }
		)

		if (toast.isNotEmpty()) {
			LaunchedEffect(toast) {
				delay(1600)
				toast = ""
			}
			Box(
				Modifier
					.align(Alignment.TopCenter)
					.padding(top = 96.dp)
					.background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(100.dp))
					.padding(horizontal = 22.dp, vertical = 10.dp)
			) {
				Text(toast, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
			}
		}
	}
}

/** `.pu-timer` — rgba(0,0,0,.4), padding 6/15, radius 100, 18px. */
@Composable
fun PuTimer(icon: IconDef, secs: Int) {
	Row(
		Modifier
			.background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
			.padding(horizontal = 15.dp, vertical = 6.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(7.dp)
	) {
		Icon24(icon, 19.dp, Color.White)
		Text(secs.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
	}
}

/** dieCore() — bank the run, count it once, log the score, add XP, save. */
fun dieCore(S: Store, e: Engine) {
	val st = e.st
	S.coins += st.coins - st.bankedCoins
	st.bankedCoins = st.coins
	S.totalCoins += st.run.coins - st.bankedRunCoins
	st.bankedRunCoins = st.run.coins
	S.totalScore += st.score - st.bankedScore
	st.bankedScore = st.score
	if (!st.countedRun) {
		S.runs += 1
		st.countedRun = true
	}
	if (st.run.maxCombo > S.maxCombo) S.maxCombo = st.run.maxCombo
	S.addScoreRow(st.score)
	if (st.score > S.best) S.best = st.score
	Cloud.submit(st.score)
	updateMissions(S, e)
	S.addXp(st.score + st.run.coins / 2)
	S.save()
	S.touch()
}

/** updateMissions() — progress every active mission with the run that just ended. */
fun updateMissions(S: Store, e: Engine) {
	for (m in S.missions) {
		val def = S.missionDef(m.id)
		val v = when (def.type) {
			"score" -> e.st.score
			"coins" -> e.st.run.coins
			"runs" -> 1
			"combo" -> e.st.run.maxCombo
			else -> 0
		}
		m.prog = when (def.type) {
			"score", "combo" -> max(m.prog, v)
			"jumps" -> S.jumps
			else -> m.prog + v
		}
		if (m.prog >= def.goal) m.done = true
	}
}
