package com.skippo.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

enum class Nav { Start, Playing, Pause, End, Settings, Shop, Lb, Missions, Ach, Daily }

/** The remaining inline SVG glyphs from the markup (class="bi"). */
object UiIcons {
	val pause = IconDef(listOf(
		El.Rct(6f, 4f, 4f, 16f, 1f, fill = C.ink),
		El.Rct(14f, 4f, 4f, 16f, 1f, fill = C.ink)
	))
	val close = IconDef(listOf(
		El.P("M6 6l12 12M18 6L6 18", strokeCurrent = true, sw = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
	))
	val cart = IconDef(listOf(
		El.Cir(9f, 20f, 1.6f, strokeCurrent = true, sw = 2.2f),
		El.Cir(18f, 20f, 1.6f, strokeCurrent = true, sw = 2.2f),
		El.P("M2 3h3l2.5 12h11l2-8H6.5", strokeCurrent = true, sw = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
	))
	val bars = IconDef(listOf(
		El.Rct(9f, 8f, 6f, 13f, 1f, strokeCurrent = true, sw = 2.2f),
		El.Rct(3f, 12f, 6f, 9f, 1f, strokeCurrent = true, sw = 2.2f),
		El.Rct(15f, 10f, 6f, 11f, 1f, strokeCurrent = true, sw = 2.2f)
	))
	val targetB = IconDef(listOf(
		El.Cir(12f, 12f, 9f, strokeCurrent = true, sw = 2.2f),
		El.Cir(12f, 12f, 5f, strokeCurrent = true, sw = 2.2f),
		El.Cir(12f, 12f, 1.5f, fill = C.ink)
	))
	val trophyB = IconDef(listOf(
		El.P("M7 4h10v5a5 5 0 0 1-10 0z", strokeCurrent = true, sw = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
		El.P("M7 6H4v1a3 3 0 0 0 3 3M17 6h3v1a3 3 0 0 1-3 3", strokeCurrent = true, sw = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
		El.P("M9.5 18h5M10 18l.5-4h3l.5 4", strokeCurrent = true, sw = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
	))
	val gear = IconDef(listOf(
		El.Cir(12f, 12f, 3f, strokeCurrent = true, sw = 2.2f),
		El.P(
			"M12 2v3M12 19v3M4.2 4.2l2.1 2.1M17.7 17.7l2.1 2.1M2 12h3M19 12h3M4.2 19.8l2.1-2.1M17.7 6.3l2.1-2.1",
			strokeCurrent = true, sw = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round
		)
	))
	val star = IconDef(listOf(
		El.P("M12 2l2.4 6.9H22l-6 4.4 2.3 7-6.3-4.6L5.7 20.7 8 13.3 2 8.9h7.6z", fill = C.ink)
	))
}

/** `.screen` — padding 28 (14 under 620px tall) + the menu / start backgrounds. */
/**
 * The overlay UI is authored against a ~900x500 css viewport in index.html.
 * On a phone that layout is far too big, so every screen renders inside a
 * uniformly scaled dp space: one factor shrinks every dp and sp at once, which
 * keeps all the CSS proportions 1:1 while making the panels fit.
 */
val LocalUiScale = staticCompositionLocalOf { 1f }

/** Screen width expressed in the scaled dp space (the `vw` of the port). */
@Composable
fun uiW(): Float = LocalConfiguration.current.screenWidthDp / LocalUiScale.current

@Composable
fun ScreenShell(menuBg: Boolean, content: @Composable BoxScope.() -> Unit) {
	val conf = LocalConfiguration.current
	val dens = LocalDensity.current
	val uiScale = min(conf.screenWidthDp / 900f, conf.screenHeightDp / 500f)
		.coerceIn(0.5f, 1f)
	val short = conf.screenHeightDp <= 620
	val bg = if (menuBg) {
		Brush.linearGradient(
			listOf(C.menu1, C.menu2),
			start = Offset(0f, 0f),
			end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
		)
	} else {
		Brush.verticalGradient(listOf(Color(0x100A0A1E), Color(0x570A0A1E)))
	}
	CompositionLocalProvider(
		LocalUiScale provides uiScale,
		// fontScale 1f: a game must not be resized by the system font setting
		LocalDensity provides Density(dens.density * uiScale, 1f)
	) {
		Box(
			Modifier
				.fillMaxSize()
				.background(bg)
				.padding(if (short) 14.dp else 28.dp),
			content = content
		)
	}
}

/** `.menu-col` — gap 12, width min(340px, 86vw). */
@Composable
fun MenuCol(content: @Composable ColumnScope.() -> Unit) {
	val w = min(340f, uiW() * 0.86f)
	Column(
		Modifier.width(w.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		content = content
	)
}

/** `.btn-row` — gap 12, width min(340px, 86vw). */
@Composable
fun BtnRow(content: @Composable RowScope.() -> Unit) {
	val w = min(340f, uiW() * 0.86f)
	Row(
		Modifier.width(w.dp),
		horizontalArrangement = Arrangement.spacedBy(12.dp),
		content = content
	)
}

/**
 * عرض اللوحات: المتجر / اللوحة / المهام / الإنجازات.
 *
 * قبل كده كان متقفّل على 640dp، فعلى الموبايل المتجر كان بيقعد في نص الشاشة
 * والجنبين فاضيين. دلوقتي اللوحة مفرودة من أول الشاشة لآخرها - العرض كله ناقص
 * نفس الهامش اللي [ScreenShell] بيحطه.
 */
@Composable
fun panelWidth(): Float {
	val conf = LocalConfiguration.current
	val inset = if (conf.screenHeightDp <= 620) 14f else 28f
	return (uiW() - inset * 2f).coerceAtLeast(260f)
}

@Composable
fun IconBtn(icon: IconDef, size: Int = 46, onClick: () -> Unit) {
	HardShadowBox(
		shadow = 7.dp, pressedShadow = 2.dp, pressOffset = 5.dp,
		shadowColor = Color.Black.copy(alpha = 0.15f),
		radius = 22.dp, bg = C.card, border = 4.dp, borderColor = C.cardD,
		onClick = onClick
	) {
		Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
			Icon24(icon, 28.dp, C.ink)
		}
	}
}

/** `.panel-head` — h2 32px white, 52px spacer. */
@Composable
fun PanelHead(title: String, onBack: () -> Unit) {
	Row(
		Modifier.width(panelWidth().dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		IconBtn(UiIcons.close, onClick = onBack)
		Text(title, color = C.white, fontWeight = FontWeight.Black, fontSize = 32.sp)
		Spacer(Modifier.width(52.dp))
	}
}

/** `.sheet` — bg card, border 6, radius 34, padding 22/28/26, width min(440px, 92vw). */
@Composable
fun Sheet(
	title: String,
	onClose: () -> Unit,
	wide: Boolean = false,
	content: @Composable ColumnScope.() -> Unit
) {
	// wide = نفس عرض لوحات المتجر / المتصدرين / المهام: الشاشة كلها ناقص
	// هامش [ScreenShell]. الافتراضي لسه الكارت الضيق بتاع الـ CSS.
	val w = if (wide) panelWidth() else min(440f, uiW() * 0.92f)
	HardShadowBox(
		modifier = Modifier.width(w.dp),
		shadow = 16.dp,
		shadowColor = Color.Black.copy(alpha = 0.28f),
		radius = 34.dp,
		bg = C.card,
		border = 6.dp,
		borderColor = C.cardD
	) {
		Column(
			Modifier
				.padding(start = 28.dp, end = 28.dp, top = 22.dp, bottom = 26.dp)
				.verticalScroll(rememberScrollState())
		) {
			Row(
				Modifier.fillMaxWidth().padding(bottom = 14.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text(title, color = C.ink, fontWeight = FontWeight.Black, fontSize = 30.sp)
				IconBtn(UiIcons.close, onClick = onClose)
			}
			Box(Modifier.fillMaxWidth().height(4.dp).background(C.cardD))
			Spacer(Modifier.height(16.dp))
			content()
		}
	}
}

/** `.setting-row` — padding 16/4, 3px bottom rule, label 21px. */
@Composable
fun SettingRow(label: String, right: @Composable () -> Unit) {
	Column {
		Row(
			Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 16.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Text(label, color = C.ink, fontWeight = FontWeight.Black, fontSize = 21.sp)
			right()
		}
		Box(Modifier.fillMaxWidth().height(3.dp).background(C.cardD))
	}
}

/** `.xp-wrap` — rgba(255,255,255,.16) card with a gold gradient fill. */
@Composable
fun XpBar(S: Store) {
	// لازم uiW() مش screenWidthDp: جوّا ScreenShell الـ dp متقاسة بمقياس
	// تاني، فالحساب الخام كان بيطلع البار أنحف من أزرار MenuCol تحته.
	val w = min(340f, uiW() * 0.86f)
	val need = S.xpNeed(S.playerLvl)
	Column(
		Modifier
			.width(w.dp)
			.clip(RoundedCornerShape(16.dp))
			.background(Color.White.copy(alpha = 0.16f))
			.border(3.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
			.padding(horizontal = 14.dp, vertical = 9.dp),
		verticalArrangement = Arrangement.spacedBy(6.dp)
	) {
		Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
			Text("Lv " + S.playerLvl, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
			Text(
				S.xp.toString() + " / " + need,
				color = Color.White.copy(alpha = 0.8f),
				fontWeight = FontWeight.Black, fontSize = 13.sp
			)
		}
		Box(
			Modifier
				.fillMaxWidth()
				.height(12.dp)
				.clip(RoundedCornerShape(100.dp))
				.background(Color.Black.copy(alpha = 0.28f))
		) {
			Box(
				Modifier
					.fillMaxWidth(min(1f, S.xp.toFloat() / need))
					.fillMaxHeight()
					.background(
						Brush.horizontalGradient(listOf(Color(0xFFFFC93C), Color(0xFFFF9D3C))),
						RoundedCornerShape(100.dp)
					)
			)
		}
	}
}

/** `.item-tag` — 14px 900, padding 6/4, radius 12, gap 5. */
@Composable
fun TagPill(text: String, bg: Color, fg: Color, icon: IconDef?) {
	Row(
		Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(12.dp))
			.background(bg, RoundedCornerShape(12.dp))
			.padding(horizontal = 4.dp, vertical = 6.dp),
		horizontalArrangement = Arrangement.Center,
		verticalAlignment = Alignment.CenterVertically
	) {
		if (icon != null) {
			Icon24(icon, 15.dp, fg)
			Spacer(Modifier.width(5.dp))
		}
		Text(text, color = fg, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
	}
}

/** `.list-item` + `.li-bar` + `.li-claim`. */
@Composable
fun ListRow(
	icon: IconDef,
	title: String,
	sub: String,
	progress: Float,
	reward: Int,
	claimed: Boolean,
	done: Boolean,
	onClaim: () -> Unit
) {
	HardShadowBox(
		modifier = Modifier.fillMaxWidth(),
		shadow = 5.dp,
		shadowColor = Color.Black.copy(alpha = 0.15f),
		radius = 20.dp,
		bg = C.card,
		border = 4.dp,
		borderColor = C.cardD
	) {
		Row(
			Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(14.dp)
		) {
			Box(
				Modifier.size(52.dp).background(C.menu1, RoundedCornerShape(14.dp)),
				contentAlignment = Alignment.Center
			) { Icon24(icon, 28.dp, Color.White) }
			Column(Modifier.weight(1f)) {
				Text(title, color = C.ink, fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
				Text(sub, color = C.inkDim, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
				Box(
					Modifier
						.padding(top = 8.dp)
						.fillMaxWidth()
						.height(10.dp)
						.clip(RoundedCornerShape(100.dp))
						.background(C.trackC)
				) {
					Box(
						Modifier
							.fillMaxWidth(progress.coerceIn(0f, 1f))
							.fillMaxHeight()
							.background(C.green, RoundedCornerShape(100.dp))
					)
				}
			}
			val bg = if (claimed) C.green else if (done) C.gold else C.trackC
			val bc = if (claimed) C.greenD else if (done) C.goldD else C.trackB
			val fg = if (claimed) Color.White else if (done) C.ink else C.inkDim
			Box(
				Modifier
					.clip(RoundedCornerShape(12.dp))
					.background(bg, RoundedCornerShape(12.dp))
					.border(3.dp, bc, RoundedCornerShape(12.dp))
					.clickable(
						interactionSource = remember { MutableInteractionSource() },
						indication = null,
						enabled = done && !claimed
					) { onClaim() }
					.padding(horizontal = 14.dp, vertical = 8.dp)
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(5.dp)
				) {
					if (claimed) {
						Icon24(IC.check, 16.dp, Color.White)
					} else {
						Icon24(IC.coin, 16.dp, fg)
						Text(reward.toString(), color = fg, fontWeight = FontWeight.Black, fontSize = 14.sp)
					}
				}
			}
		}
	}
}

/** `.lb-item` — radius 18, padding 12/16, shadow 0 4px 0; `.me` uses the accent border. */
@Composable
fun LbRow(rank: Int, row: ScoreRow, S: Store, avatar: String = "") {
	val me = row.name.isEmpty() || row.name == S.name
	HardShadowBox(
		modifier = Modifier.fillMaxWidth(),
		shadow = 4.dp,
		shadowColor = if (me) C.accentD else Color.Black.copy(alpha = 0.13f),
		radius = 18.dp,
		bg = C.card,
		border = 4.dp,
		borderColor = if (me) C.accent else C.cardD
	) {
		Row(
			Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(14.dp)
		) {
			when (rank) {
				1 -> Icon24(IC.medal1, 38.dp, C.ink)
				2 -> Icon24(IC.medal2, 38.dp, C.ink)
				3 -> Icon24(IC.medal3, 38.dp, C.ink)
				else -> Box(Modifier.widthIn(min = 38.dp), contentAlignment = Alignment.Center) {
					Text(rank.toString(), color = C.inkDim, fontWeight = FontWeight.Black, fontSize = 22.sp)
				}
			}
			if (avatar.isNotEmpty()) {
				AvatarDot(avatar, 38.dp, if (me) C.accent else C.cardD, row.name)
			}
			Text(
				if (row.name.isEmpty()) Lang.t("you", S.lang) else row.name,
				color = C.ink, fontWeight = FontWeight.Black, fontSize = 18.sp,
				maxLines = 1, overflow = TextOverflow.Ellipsis,
				modifier = Modifier.weight(1f)
			)
			Text(row.score.toString(), color = C.goldD, fontWeight = FontWeight.Black, fontSize = 20.sp)
		}
	}
}

/** `.day-cell` — radius 18, border 4, padding 14/6; claimed = green, today = gold ring. */
@Composable
fun DayCell(i: Int, reward: Pair<String, Int>, claimed: Boolean, isToday: Boolean) {
	val bg = if (claimed) C.green else C.card
	val bc = if (claimed) C.greenD else if (isToday) C.gold else C.cardD
	Box(
		Modifier
			.clip(RoundedCornerShape(18.dp))
			.background(bg, RoundedCornerShape(18.dp))
			.border(4.dp, bc, RoundedCornerShape(18.dp))
			.padding(horizontal = 6.dp, vertical = 14.dp)
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(6.dp),
			modifier = Modifier.fillMaxWidth()
		) {
			Text(
				(i + 1).toString(),
				color = if (claimed) Color.White else C.inkDim,
				fontWeight = FontWeight.Black, fontSize = 13.sp
			)
			Icon24(if (reward.first == "gem") IC.gem else IC.coin, 30.dp, C.ink)
			Text(
				reward.second.toString(),
				color = if (claimed) Color.White else C.ink,
				fontWeight = FontWeight.Black, fontSize = 15.sp
			)
		}
	}
}
