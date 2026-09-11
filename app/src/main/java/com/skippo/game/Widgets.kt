package com.skippo.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlin.math.max
import kotlin.math.min

/* =========================================================================
   CSS-faithful widgets.
   Every number here is copied from the stylesheet in index.html.
   ========================================================================= */

enum class BtnKind { Normal, Primary, Gold, Ghost }

/**
 * A CSS `box-shadow: 0 Npx 0 color` hard shadow.
 * On press the box translates down and the shadow shrinks, exactly like `.btn:active`.
 */
@Composable
fun HardShadowBox(
	modifier: Modifier = Modifier,
	shadow: Dp,
	pressedShadow: Dp = shadow,
	pressOffset: Dp = 0.dp,
	shadowColor: Color,
	radius: Dp,
	bg: Color,
	border: Dp = 0.dp,
	borderColor: Color = Color.Transparent,
	onClick: (() -> Unit)? = null,
	enabled: Boolean = true,
	content: @Composable BoxScope.() -> Unit
) {
	val interaction = remember { MutableInteractionSource() }
	val pressed by interaction.collectIsPressedAsState()
	val shTarget = if (pressed && enabled) pressedShadow else shadow
	val offTarget = if (pressed && enabled) pressOffset else 0.dp
	val sh by animateDpAsState(shTarget, tween(100, easing = EaseDefault), label = "sh")
	val off by animateDpAsState(offTarget, tween(100, easing = EaseDefault), label = "off")
	val shape = RoundedCornerShape(radius)

	// propagateMinConstraints: when the caller stretches the button (fillMaxWidth,
	// width(...)) the coloured surface must stretch too, otherwise only the shadow
	// layer fills the row and you see a dark bar sticking out behind a small button.
	Box(modifier.padding(bottom = shadow), propagateMinConstraints = true) {
		Box(
			Modifier
				.matchParentSize()
				.offset(y = sh + off)
				.background(shadowColor, shape)
		)
		Box(
			Modifier
				.offset(y = off)
				.clip(shape)
				.background(bg, shape)
				.then(if (border > 0.dp) Modifier.border(border, borderColor, shape) else Modifier)
				.then(
					if (onClick != null) Modifier.clickable(
						interactionSource = interaction,
						indication = null,
						enabled = enabled,
						onClick = onClick
					) else Modifier
				),
			contentAlignment = Alignment.Center,
			content = content
		)
	}
}

/**
 * `.btn` — font-size 22, padding 16/30, radius 22, border 4 solid --card-d,
 * box-shadow 0 9px 0 rgba(0,0,0,.2); :active translateY(7px) + shadow 0 2px.
 */
@Composable
fun Btn(
	label: String,
	modifier: Modifier = Modifier,
	kind: BtnKind = BtnKind.Normal,
	fontSize: TextUnit = 22.sp,
	padH: Dp = 30.dp,
	padV: Dp = 16.dp,
	leading: (@Composable () -> Unit)? = null,
	enabled: Boolean = true,
	onClick: () -> Unit
) {
	val bg = when (kind) {
		BtnKind.Normal -> C.white
		BtnKind.Primary -> C.accent
		BtnKind.Gold -> C.gold
		BtnKind.Ghost -> Color.White.copy(alpha = 0.14f)
	}
	val fg = when (kind) {
		BtnKind.Normal -> C.ink
		BtnKind.Primary -> C.white
		BtnKind.Gold -> C.ink
		BtnKind.Ghost -> C.white
	}
	val bc = when (kind) {
		BtnKind.Normal -> C.cardD
		BtnKind.Primary -> C.accentD
		BtnKind.Gold -> C.goldD
		BtnKind.Ghost -> Color.White.copy(alpha = 0.7f)
	}
	val shadow = if (kind == BtnKind.Ghost) 6.dp else 9.dp
	val shadowCol = if (kind == BtnKind.Ghost) Color.Black.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.2f)

	HardShadowBox(
		modifier = modifier,
		shadow = shadow,
		pressedShadow = 2.dp,
		pressOffset = 7.dp,
		shadowColor = shadowCol,
		radius = 22.dp,
		bg = bg,
		border = 4.dp,
		borderColor = bc,
		enabled = enabled,
		onClick = onClick
	) {
		Row(
			Modifier.padding(horizontal = padH, vertical = padV),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(10.dp)
		) {
			leading?.invoke()
			Text(
				label,
				color = fg,
				fontSize = fontSize,
				fontWeight = FontWeight.Black,
				letterSpacing = (fontSize.value * 0.02f).sp,
				textAlign = TextAlign.Center
			)
		}
	}
}

/** `.curr` — bg card, border 4 card-d, padding 8/18, radius 100, shadow 0 5px 0, 20px. */
@Composable
fun Curr(icon: IconDef, value: String, small: Boolean = false, modifier: Modifier = Modifier) {
	HardShadowBox(
		modifier = modifier,
		shadow = if (small) 3.dp else 5.dp,
		shadowColor = Color.Black.copy(alpha = 0.15f),
		radius = 100.dp,
		bg = C.card,
		border = if (small) 3.dp else 4.dp,
		borderColor = C.cardD
	) {
		Row(
			Modifier.padding(
				horizontal = if (small) 12.dp else 18.dp,
				vertical = if (small) 5.dp else 8.dp
			),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp)
		) {
			Icon24(icon, size = if (small) 16.dp else 22.dp, tint = C.ink)
			Text(
				value,
				color = C.ink,
				fontWeight = FontWeight.Black,
				fontSize = if (small) 15.sp else 20.sp
			)
		}
	}
}

/** `.pill` — bg card, padding 8/16, radius 100, border 4, shadow 0 6px 0, 22px. */
@Composable
fun Pill(icon: IconDef?, value: String, modifier: Modifier = Modifier) {
	HardShadowBox(
		modifier = modifier,
		shadow = 6.dp,
		shadowColor = Color.Black.copy(alpha = 0.15f),
		radius = 100.dp,
		bg = C.card,
		border = 4.dp,
		borderColor = C.cardD
	) {
		Row(
			Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp)
		) {
			if (icon != null) Icon24(icon, size = 22.dp, tint = C.ink)
			Text(value, color = C.ink, fontWeight = FontWeight.Black, fontSize = 22.sp)
		}
	}
}

/**
 * `.toggle` — 76x42 track, radius 100, 28px knob.
 *
 * The knob lives 4px (border) + 3px inside the track, so it travels 7 -> 41 and
 * is vertically centred. The old code offset it by `3.dp - 4.dp`, i.e. one dp
 * ABOVE the track, so the rounded clip sliced the top off the white circle.
 *
 * `Modifier.offset` is layout-direction aware, and the app is laid out RTL when
 * the phone is Arabic, which mirrored the knob: "on" pushed it to the left. The
 * widget is therefore pinned to LTR so the knob always slides to the right when
 * the switch is on, exactly like the web build.
 */
@Composable
fun Toggle(on: Boolean, onChange: (Boolean) -> Unit) {
	val trackW = 76.dp
	val trackH = 42.dp
	val knob = 28.dp
	val inset = 7.dp                       // 4px border + 3px gap
	val knobY = (trackH - knob) / 2f       // 7dp — centred, never clipped
	val knobX by animateDpAsState(
		if (on) trackW - knob - inset else inset,
		tween(220, easing = Ease),
		label = "knob"
	)
	CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
		Box(
			Modifier
				.size(trackW, trackH)
				.clip(RoundedCornerShape(100.dp))
				.background(if (on) C.green else C.trackC, RoundedCornerShape(100.dp))
				.border(4.dp, if (on) C.greenD else C.trackB, RoundedCornerShape(100.dp))
				.clickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null
				) { onChange(!on) }
		) {
			Box(
				Modifier
					.offset(x = knobX, y = knobY)
					.size(knob)
					.background(C.white, RoundedCornerShape(50))
			)
		}
	}
}

/** `.seg` — gap 10, bg track, padding 8, radius 20, border 3 track-b. */
@Composable
fun Seg(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
	Row(
		Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(20.dp))
			.background(C.trackC, RoundedCornerShape(20.dp))
			.border(3.dp, C.trackB, RoundedCornerShape(20.dp))
			.padding(8.dp),
		horizontalArrangement = Arrangement.spacedBy(10.dp)
	) {
		for ((id, label) in options) {
			val active = id == selected
			Box(
				Modifier
					.weight(1f)
					.clip(RoundedCornerShape(13.dp))
					.background(if (active) C.accent else Color.Transparent, RoundedCornerShape(13.dp))
					.clickable(
						interactionSource = remember { MutableInteractionSource() },
						indication = null
					) { onSelect(id) }
					.padding(vertical = 14.dp),
				contentAlignment = Alignment.Center
			) {
				Text(
					label,
					color = if (active) C.white else C.inkDim,
					fontWeight = FontWeight.Black,
					fontSize = 18.sp
				)
			}
		}
	}
}

/** `.tabs` — bg rgba(0,0,0,.2), padding 7, gap 6, radius 20; `.tab.active` white pill. */
@Composable
fun Tabs(tabs: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
	Row(
		Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(20.dp))
			.background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
			.padding(7.dp),
		horizontalArrangement = Arrangement.spacedBy(6.dp)
	) {
		for ((id, label) in tabs) {
			val active = id == selected
			Box(
				Modifier
					.weight(1f)
					.clip(RoundedCornerShape(13.dp))
					.background(if (active) C.white else Color.Transparent, RoundedCornerShape(13.dp))
					.clickable(
						interactionSource = remember { MutableInteractionSource() },
						indication = null
					) { onSelect(id) }
					.padding(horizontal = 6.dp, vertical = 12.dp),
				contentAlignment = Alignment.Center
			) {
				Text(
					label,
					color = if (active) C.ink else Color.White.copy(alpha = 0.65f),
					fontWeight = FontWeight.Black,
					fontSize = 16.sp,
					maxLines = 1
				)
			}
		}
	}
}

/**
 * زي [Tabs] بس بتزحلق أفقي: خانات المتجر بقت كتيرة (لاعيبة / أساطير /
 * توهج / آثار / ثيمات / ثيمات VIP / ملاعب مشهورة / ملاعب VIP / مميزات /
 * كوينز) وماينفعش تتزنق كلها جانب بعض بعروض متساوية.
 */
@Composable
fun ScrollTabs(tabs: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
	BoxWithConstraints(
		Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(20.dp))
			.background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
	) {
		// الشريط بيتفرد على عرض اللوحة كله: لو الخانات أصغر من الشاشة
		// بتتوزع من الأول للآخر بدل ما تتكوّم في النص، ولو زادت بترجع
		// تتزحلق زي الأول. (widthIn مش weight - weight بيضرب جوّا Row متزحلقة.)
		val inner = (maxWidth - 14.dp).coerceAtLeast(0.dp)
		Row(
			Modifier
				.horizontalScroll(rememberScrollState())
				.padding(7.dp)
				.widthIn(min = inner),
			horizontalArrangement = Arrangement.SpaceEvenly,
			verticalAlignment = Alignment.CenterVertically
		) {
			for ((id, label) in tabs) {
				val active = id == selected
				Box(
					Modifier
						.padding(horizontal = 3.dp)
						.clip(RoundedCornerShape(13.dp))
						.background(if (active) C.white else Color.Transparent, RoundedCornerShape(13.dp))
						.clickable(
							interactionSource = remember { MutableInteractionSource() },
							indication = null
						) { onSelect(id) }
						.padding(horizontal = 14.dp, vertical = 12.dp),
					contentAlignment = Alignment.Center
				) {
					Text(
						label,
						color = if (active) C.ink else Color.White.copy(alpha = 0.65f),
						fontWeight = FontWeight.Black,
						fontSize = 16.sp,
						maxLines = 1
					)
				}
			}
		}
	}
}

/**
 * `.screen-inner` + fitScreen(): scale down (never up) so the stack always fits,
 * s = min(1, (clientWidth-24)/contentW, (clientHeight-24)/contentH).
 */
@Composable
fun FitScale(
	modifier: Modifier = Modifier,
	gap: Dp = 22.dp,
	content: @Composable ColumnScope.() -> Unit
) {
	SubcomposeLayout(modifier) { constraints ->
		val pad = 24.dp.roundToPx()
		val vw = (constraints.maxWidth - pad).coerceAtLeast(1)
		val vh = (constraints.maxHeight - pad).coerceAtLeast(1)

		val probe = subcompose("probe") {
			Column(
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(gap),
				content = content
			)
		}.first().measure(Constraints(maxWidth = constraints.maxWidth))

		val s = minOf(1f, vw.toFloat() / probe.width, vh.toFloat() / probe.height)

		val real = subcompose("real") {
			Box(
				Modifier.graphicsLayer {
					scaleX = s; scaleY = s
					transformOrigin = TransformOrigin.Center
				}
			) {
				Column(
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.spacedBy(gap),
					content = content
				)
			}
		}.first().measure(Constraints(maxWidth = constraints.maxWidth))

		layout(constraints.maxWidth, constraints.maxHeight) {
			real.place(
				(constraints.maxWidth - real.width) / 2,
				(constraints.maxHeight - real.height) / 2
			)
		}
	}
}

/* -------------------------------------------------------------------------
   The SKIPPO logo.
   .hero-title { animation: floatY 3s ease-in-out infinite }
   .logo  { font-size:clamp(54px,13.5vw,118px); weight 900; letter-spacing:-0.03em;
            line-height:0.86; color:var(--white);
            text-shadow:0 10px 0 #2f4d7a, 0 16px 0 rgba(0,0,0,.18); }
   .logo span { color:var(--accent); text-shadow:0 10px 0 var(--accent-d),0 16px 0 rgba(0,0,0,.18);
                display:inline-block; transform:rotate(-6deg); }
   ------------------------------------------------------------------------- */
@Composable
fun LogoTitle(modifier: Modifier = Modifier) {
	val conf = LocalConfiguration.current
	val density = LocalDensity.current
	val vw = conf.screenWidthDp.toFloat()
	val fsDp = clampCss(54f, 13.5f * vw / 100f, 118f)

	// floatY: 0% 0 -> 50% -8px -> 100% 0, ease-in-out, 3s
	val inf = rememberInfiniteTransition(label = "floatY")
	val ty by inf.animateFloat(
		initialValue = 0f,
		targetValue = -8f,
		animationSpec = infiniteRepeatable(
			animation = tween(1500, easing = EaseInOut),
			repeatMode = RepeatMode.Reverse
		),
		label = "ty"
	)

	// الكورة اللي جوّا الـ O بتلف لفة كاملة كل 2.6 ثانية، على طول من غير وقفة.
	val spin = inf.animateFloat(
		initialValue = 0f,
		targetValue = 360f,
		animationSpec = infiniteRepeatable(
			animation = tween(2600, easing = androidx.compose.animation.core.LinearEasing),
			repeatMode = RepeatMode.Restart
		),
		label = "spin"
	)

	val fsPx = with(density) { fsDp.dp.toPx() }
	val paint = remember(fsPx) { Fonts.canvasPaint(fsPx) }
	paint.textSize = fsPx
	paint.letterSpacing = -0.03f
	paint.textAlign = android.graphics.Paint.Align.LEFT

	val w1 = paint.measureText("SKIPP")
	val w2 = paint.measureText("O")
	val totalW = w1 + w2
	// حدود جليف الـ O بالظبط، عشان نعرف مركز الفتحة اللي جوّاه ونحط الكورة فيها
	val oBox = remember { android.graphics.Rect() }
	paint.getTextBounds("O", 0, 1, oBox)
	val ballPaint = remember {
		android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
	}
	val ballDst = remember { android.graphics.RectF() }
	val ctx = LocalContext.current
	// صورة الكورة الحقيقية من assets/ball.webp
	val ballBmp = remember(ctx) { BallSprite.img(ctx) }
	val fm = paint.fontMetrics
	val lineBox = 0.86f * fsPx                 // line-height:0.86
	val halfLeading = (lineBox - (-fm.ascent + fm.descent)) / 2f
	val baseline = -fm.ascent + halfLeading
	val sh1 = with(density) { 10.dp.toPx() }   // 0 10px 0
	val sh2 = with(density) { 16.dp.toPx() }   // 0 16px 0
	val extra = sh2

	Canvas(
		modifier
			.width(with(density) { totalW.toDp() })
			.height(with(density) { (lineBox + extra).toDp() })
			.offset(y = ty.dp)
	) {
		val canvas = drawContext.canvas.nativeCanvas

		fun pass(dy: Float, cMain: Int, cO: Int) {
			paint.color = cMain
			canvas.drawText("SKIPP", 0f, baseline + dy, paint)
			// the O is an inline-block rotated -6deg about its own centre
			val cx = w1 + w2 / 2f
			val cy = lineBox / 2f + dy
			canvas.save()
			canvas.rotate(-6f, cx, cy)
			paint.color = cO
			canvas.drawText("O", w1, baseline + dy, paint)
			canvas.restore()
		}

		val blk = android.graphics.Color.argb((0.18f * 255).toInt(), 0, 0, 0)
		pass(sh2, blk, blk)                                  // 0 16px 0 rgba(0,0,0,.18)
		pass(sh1, C.logoShadow.toArgb32(), C.accentD.toArgb32()) // 0 10px 0
		pass(0f, C.white.toArgb32(), C.accent.toArgb32())     // the glyphs themselves

		// ---- الكورة جوّا فتحة الـ O ----
		// مركز الجليف ناقص سمك الحلقة (تقريباً 22% من طول الحرف في الخط
		// التقيل) = الفتحة اللي الكورة بتقعد جوّاها.
		val bx = w1 + (oBox.left + oBox.right) / 2f
		val by = baseline + (oBox.top + oBox.bottom) / 2f
		val ring = oBox.height() * 0.22f
		val innerW = (oBox.width() - 2f * ring).coerceAtLeast(2f)
		val innerH = (oBox.height() - 2f * ring).coerceAtLeast(2f)
		val br = 0.48f * min(innerW, innerH)

		canvas.save()
		canvas.rotate(-6f, w1 + w2 / 2f, lineBox / 2f)   // نفس ميلة الـ O

		// ضل جامد تحت الكورة، زي ضل اللوجو بالظبط
		ballPaint.style = android.graphics.Paint.Style.FILL
		ballPaint.color = android.graphics.Color.argb((0.22f * 255).toInt(), 0, 0, 0)
		canvas.drawCircle(bx, by + sh1, br, ballPaint)

		val bmp = ballBmp
		if (bmp != null) {
			// صورة الكورة نفسها، بتلف حوالين مركزها
			canvas.save()
			canvas.rotate(spin.value, bx, by)
			// alpha لازم يرجع 255 بعد الضل، غير كده الصورة تطلع شفافة
			ballPaint.color = android.graphics.Color.WHITE
			ballDst.set(bx - br, by - br, bx + br, by + br)
			canvas.drawBitmap(bmp, null as android.graphics.Rect?, ballDst, ballPaint)
			canvas.restore()
		} else {
			// احتياطي لو الصورة مش موجودة لأي سبب
			drawFootball(canvas, bx, by, br, spin.value, ballPaint)
		}
		canvas.restore()
	}
}

/**
 * صورة الكورة اللي جوّا حرف الـ O.
 *
 * بتتقرا مرة واحدة بس من `assets/ball.webp` وتفضل في الذاكرة، مفيش قراية
 * من الديسك كل فريم.
 */
private object BallSprite {
	private var bmp: android.graphics.Bitmap? = null
	private var loaded = false

	fun img(ctx: android.content.Context): android.graphics.Bitmap? {
		if (loaded) return bmp
		loaded = true
		for (ext in listOf("webp", "png")) {
			try {
				ctx.assets.open("ball.$ext").use { s ->
					bmp = android.graphics.BitmapFactory.decodeStream(s)
				}
			} catch (t: Throwable) {
				// مفيش ملف بالامتداد ده - عادي
			}
			if (bmp != null) break
		}
		return bmp
	}
}

/** احتياطي: كورة مرسومة بالكود لو الصورة مش موجودة، بزاوية [spin] درجة. */
private fun drawFootball(
	canvas: android.graphics.Canvas,
	cx: Float,
	cy: Float,
	r: Float,
	spin: Float,
	p: android.graphics.Paint
) {
	val dark = android.graphics.Color.rgb(26, 22, 40)

	canvas.save()
	canvas.rotate(spin, cx, cy)

	p.style = android.graphics.Paint.Style.FILL
	p.color = android.graphics.Color.WHITE
	canvas.drawCircle(cx, cy, r, p)

	// الضلوع متقصوصة على حدود الكورة عشان ماتخرجش برّا الدايرة
	canvas.save()
	val clip = android.graphics.Path()
	clip.addCircle(cx, cy, r, android.graphics.Path.Direction.CW)
	canvas.clipPath(clip)
	p.color = dark
	pentagonAt(canvas, p, cx, cy, r * 0.38f, 0f)
	for (i in 0 until 5) {
		val a = Math.toRadians((i * 72f - 90f).toDouble())
		pentagonAt(
			canvas, p,
			cx + r * 0.80f * kotlin.math.cos(a).toFloat(),
			cy + r * 0.80f * kotlin.math.sin(a).toFloat(),
			r * 0.30f,
			i * 72f + 36f
		)
	}
	canvas.restore()

	// برواز غامق يفصلها عن لون الحرف
	p.style = android.graphics.Paint.Style.STROKE
	p.strokeWidth = r * 0.14f
	p.color = dark
	canvas.drawCircle(cx, cy, r - r * 0.07f, p)
	p.style = android.graphics.Paint.Style.FILL

	canvas.restore()
}

/** مخمّس مصمت مركزه (cx, cy) ونصف قطره [r]، ملفوف [rot] درجة. */
private fun pentagonAt(
	canvas: android.graphics.Canvas,
	p: android.graphics.Paint,
	cx: Float,
	cy: Float,
	r: Float,
	rot: Float
) {
	val path = android.graphics.Path()
	for (i in 0 until 5) {
		val a = Math.toRadians((rot + i * 72f - 90f).toDouble())
		val x = cx + r * kotlin.math.cos(a).toFloat()
		val y = cy + r * kotlin.math.sin(a).toFloat()
		if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
	}
	path.close()
	canvas.drawPath(path, p)
}

/** `.stats-card` — bg card, border 5 card-d, radius 28, padding 22/40, shadow 0 12px 0, gap 40. */
@Composable
fun StatsCard(items: List<Pair<String, String>>) {
	HardShadowBox(
		shadow = 12.dp,
		shadowColor = Color.Black.copy(alpha = 0.25f),
		radius = 28.dp,
		bg = C.card,
		border = 5.dp,
		borderColor = C.cardD
	) {
		Row(
			Modifier.padding(horizontal = 40.dp, vertical = 22.dp),
			horizontalArrangement = Arrangement.spacedBy(40.dp)
		) {
			for ((label, value) in items) {
				Column(horizontalAlignment = Alignment.CenterHorizontally) {
					Text(label, color = C.inkDim, fontWeight = FontWeight.Black, fontSize = 16.sp)
					Text(
						value,
						color = C.ink,
						fontWeight = FontWeight.Black,
						fontSize = 52.sp,
						modifier = Modifier.padding(top = 4.dp)
					)
				}
			}
		}
	}
}

/** `#score` — 900, huge, -webkit-text-stroke 5px rgba(36,30,58,.42), paint-order stroke fill. */
@Composable
fun ScoreText(text: String, fontSizeSp: Float, modifier: Modifier = Modifier) {
	val density = LocalDensity.current
	val px = with(density) { fontSizeSp.sp.toPx() }
	val paint = remember(px) { Fonts.canvasPaint(px) }
	paint.textSize = px
	paint.textAlign = android.graphics.Paint.Align.LEFT
	val fm = paint.fontMetrics
	val w = paint.measureText(text) + with(density) { 10.dp.toPx() }
	val h = (-fm.ascent + fm.descent) + with(density) { 10.dp.toPx() }
	Canvas(
		modifier
			.width(with(density) { w.toDp() })
			.height(with(density) { h.toDp() })
	) {
		val canvas = drawContext.canvas.nativeCanvas
		val x = with(density) { 5.dp.toPx() }
		val y = -fm.ascent
		// text-shadow: 0 5px 0 rgba(0,0,0,.22)
		paint.style = android.graphics.Paint.Style.FILL
		paint.color = android.graphics.Color.argb((0.22f * 255).toInt(), 0, 0, 0)
		canvas.drawText(text, x, y + with(density) { 5.dp.toPx() }, paint)
		// paint-order: stroke fill
		paint.style = android.graphics.Paint.Style.STROKE
		paint.strokeWidth = with(density) { 5.dp.toPx() }
		paint.color = android.graphics.Color.argb((0.42f * 255).toInt(), 36, 30, 58)
		canvas.drawText(text, x, y, paint)
		paint.style = android.graphics.Paint.Style.FILL
		paint.color = C.white.toArgb32()
		canvas.drawText(text, x, y, paint)
	}
}
