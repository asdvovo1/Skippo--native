package com.skippo.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * `#shopScreen` — six tabs, `.grid` = repeat(auto-fill, minmax(130px, 1fr)) gap 14.
 */
@Composable
fun ShopScreen(S: Store, back: () -> Unit, toast: (String) -> Unit) {
	val L = S.lang
	var tab by remember { mutableStateOf("players") }

	ScreenShell(menuBg = true) {
		// لازم جوّا ScreenShell: هي اللي بتظبّط مقياس الـ dp، ولو حسبنا العرض
		// برّا بيطلع بمقياس تاني والمتجر يقعد في نص الشاشة والجنبين فاضيين.
		val pw = panelWidth()
		Column(
			Modifier.fillMaxSize(),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			// .shop-head — balances pinned top-left, title centred, close top-right,
			// all on one row so the tabs and the grid move up and stay visible.
			Row(
				Modifier.width(pw.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Row(
					horizontalArrangement = Arrangement.spacedBy(8.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					Curr(IC.coin, S.coins.toString(), small = true)
					Curr(IC.gem, S.gems.toString(), small = true)
				}
				Text(
					Lang.t("shopTitle", L),
					color = C.white,
					fontWeight = FontWeight.Black,
					fontSize = 28.sp
				)
				IconBtn(UiIcons.close, onClick = back)
			}

			Box(Modifier.width(pw.dp)) {
				// كل نوع في خانة لوحده: لاعيبة عادية / أساطير VIP / ثيمات / ثيمات VIP /
				// ملاعب مشهورة / ملاعب VIP (دول اللي فيهم شماريخ)
				ScrollTabs(
					listOf(
						"players" to Lang.t("tabPlayers", L),
						"legends" to Lang.t("tabLegends", L),
						"glows" to Lang.t("tabGlows", L),
						"trails" to Lang.t("tabTrails", L),
						"themes" to Lang.t("tabThemes", L),
						"themesVip" to Lang.t("tabThemesVip", L),
						"arenas" to Lang.t("tabArenas", L),
						"arenasVip" to Lang.t("tabArenasVip", L),
						"arenasClubVip" to Lang.t("tabArenasClubVip", L),
						"power" to Lang.t("tabPower", L),
						"coins" to Lang.t("tabCoins", L)
					),
					tab
				) { tab = it }
			}

			// key(tab): كل خانة تبقى شجرة لوحدها. من غير كده الجريد بتاع "لاعيبة"
			// بيتعاد استخدامه لـ "أساطير" فالكروت بتفضل شايلة صور اللاعيبة القدام
			// وتحصل اللخبطة اللي كانت بتبان لما تنقل ما بين الخانتين.
			key(tab) {
			when (tab) {
				"players", "legends" -> LazyVerticalGrid(
					columns = GridCells.Adaptive(130.dp),
					modifier = Modifier.width(pw.dp).fillMaxHeight(),
					horizontalArrangement = Arrangement.spacedBy(14.dp),
					verticalArrangement = Arrangement.spacedBy(14.dp),
					contentPadding = PaddingValues(bottom = 14.dp)
				) {
					// key = اسم اللاعب: الكرت يفضل مربوط بلاعبه مهما اتزحلق الجريد
					items(
						if (tab == "legends") Players.LEGEND_IDS else Players.COIN_IDS,
						key = { it }
					) { id ->
						PlayerItem(S, Players[id], L, toast)
					}
				}

				"power" -> Column(
					Modifier.width(pw.dp).verticalScroll(rememberScrollState()),
					verticalArrangement = Arrangement.spacedBy(12.dp)
				) {
					for (p in Catalog.PERKS) PerkRow(S, p, L, toast)
					Spacer(Modifier.height(14.dp))
				}

				"coins" -> Column(
					Modifier.width(pw.dp).verticalScroll(rememberScrollState()),
					verticalArrangement = Arrangement.spacedBy(12.dp)
				) {
					for (cp in Catalog.COINPACKS) CoinPackRow(S, cp, L, toast)
					Spacer(Modifier.height(14.dp))
				}

				else -> {
					val map = Catalog.tabMap(tab)
					// الملكية لسه متخزنة تحت "themes"، فالمشتريات القديمة ماتضيعش
					val own = Catalog.tabOwnCat(tab)
					val keys = map.keys.toList()
					LazyVerticalGrid(
						columns = GridCells.Adaptive(130.dp),
						modifier = Modifier.width(pw.dp).fillMaxHeight(),
						horizontalArrangement = Arrangement.spacedBy(14.dp),
						verticalArrangement = Arrangement.spacedBy(14.dp),
						contentPadding = PaddingValues(bottom = 14.dp)
					) {
						items(keys, key = { it }) { id ->
							ShopItem(S, own, id, map.getValue(id), L, toast)
						}
					}
				}
			}
			}
		}
	}
}

/** `.item` — bg card, border 4, radius 22, padding 14/12/12, gap 9, shadow 0 6px 0. */
@Composable
private fun ShopItem(S: Store, cat: String, id: String, it: Item, L: String, toast: (String) -> Unit) {
	val owned = S.has(cat, id)
	val equipped = when (cat) {
		"glows" -> S.glow == id
		"trails" -> S.trail == id
		else -> S.theme == id
	}

	HardShadowBox(
		modifier = Modifier.fillMaxWidth(),
		shadow = 6.dp,
		shadowColor = if (equipped) C.greenD else Color.Black.copy(alpha = 0.15f),
		radius = 22.dp,
		bg = C.card,
		border = 4.dp,
		// إطار دهبي لأي حاجة VIP (بالجواهر)
		borderColor = if (equipped) C.green else if (it.gem != null) C.gold else C.cardD,
		onClick = {
			if (owned) {
				when (cat) {
					"glows" -> S.glow = id
					"trails" -> S.trail = id
					else -> S.theme = id
				}
				Sfx.sBuy(); S.save(); S.touch()
			} else {
				val gem = it.gem
				if (gem != null) {
					if (S.gems >= gem) {
						S.gems -= gem
						S.owned[cat]?.add(id)
						Sfx.sBuy(); S.save(); S.touch()
					} else { Sfx.sErr(); toast(Lang.t("noGems", L)) }
				} else {
					val price = it.price ?: 0
					if (S.coins >= price) {
						S.coins -= price
						S.owned[cat]?.add(id)
						Sfx.sBuy(); S.save(); S.touch()
					} else { Sfx.sErr(); toast(Lang.t("noCoins", L)) }
				}
			}
		}
	) {
		Column(
			Modifier
				.fillMaxWidth()
				.padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(9.dp)
		) {
			ItemPreview(cat, id, it)
			// Always reserve two lines. "نار" wraps to one line and "ملعب الأحمر
			// الإنجليزي" wraps to two, so without minLines the short cards come out
			// ~20dp shorter and the grid row looks stepped.
			Text(
				it.name(L),
				color = C.ink, fontWeight = FontWeight.Black, fontSize = 15.sp,
				textAlign = TextAlign.Center, minLines = 2, maxLines = 2,
				overflow = TextOverflow.Ellipsis
			)
			when {
				equipped -> TagPill(Lang.t("equipped", L), C.green, C.white, IC.check)
				owned -> TagPill(Lang.t("equip", L), C.white, C.inkDim, null)
				it.gem != null -> TagPill(it.gem.toString(), C.gem, C.white, IC.gem)
				(it.price ?: 0) == 0 -> TagPill(Lang.t("free", L), C.gold, C.ink, null)
				else -> TagPill(it.price.toString(), C.gold, C.ink, IC.coin)
			}
		}
	}
}

/** `.item-preview` — 60x60, radius 16, inset ring rgba(0,0,0,.08). */
@Composable
private fun ItemPreview(cat: String, id: String, it: Item) {
	Canvas(Modifier.size(60.dp)) {
		val r = 16.dp.toPx()
		val s = size.width
		val cr = CornerRadius(r, r)
		when (cat) {
			"themes" -> {
				drawRoundRect(
					Brush.verticalGradient(listOf(it.top ?: C.white, it.bot ?: C.white)),
					cornerRadius = cr
				)
				if (it.kind == "club") {
					val k1 = it.c ?: C.white
					val k2 = it.c2 ?: C.ink
					drawRect(shade(k2, -0.45f), Offset(0f, s * 0.26f), Size(s, s * 0.06f))
					for (i in 0 until 6) {
						drawRect(if (i % 2 == 0) k1 else k2, Offset(s * i / 6f, s * 0.32f), Size(s / 6f, s * 0.26f))
					}
					drawRect(it.ground ?: C.ink, Offset(0f, s * 0.58f), Size(s, s * 0.42f))
					drawRect(it.gTop ?: C.ink, Offset(0f, s * 0.58f), Size(s, s * 0.22f))
					drawRect(Color.White.copy(alpha = 0.8f), Offset(0f, s * 0.72f), Size(s, s * 0.03f))
					if (it.gem != null) {
						// ملعب VIP: شماريخ صغيرة بألوان الملعب في البريفيو
						drawLine(k1, Offset(s * 0.2f, s * 0.27f), Offset(s * 0.2f, s * 0.09f), 3f, StrokeCap.Round)
						drawCircle(k1.copy(alpha = 0.45f), s * 0.1f, Offset(s * 0.2f, s * 0.08f))
						drawCircle(Color.White, s * 0.035f, Offset(s * 0.2f, s * 0.08f))
						drawLine(k2, Offset(s * 0.79f, s * 0.3f), Offset(s * 0.79f, s * 0.14f), 3f, StrokeCap.Round)
						drawCircle(k2.copy(alpha = 0.45f), s * 0.09f, Offset(s * 0.79f, s * 0.13f))
						drawCircle(Color.White, s * 0.03f, Offset(s * 0.79f, s * 0.13f))
						// شعار النادي / المنتخب جوّا الكرت
						val code = Catalog.crestCode(id)
						if (code != null) crestBadge(id, s * 0.5f, s * 0.4f, s * 0.2f, k1, k2, code)
					}
				} else {
					drawRect(it.ground ?: C.ink, Offset(0f, s * 0.72f), Size(s, s * 0.28f))
					drawRect(it.gTop ?: C.ink, Offset(0f, s * 0.72f), Size(s, s * 0.07f))
				}
			}
			"glows" -> {
				drawRoundRect(C.ink, cornerRadius = cr)
				val col = if (it.kind == "rainbow") hsl2c(300.0, 95.0, 62.0) else it.c
				if (col != null) {
					drawCircle(
						Brush.radialGradient(
							0f to col, 0.55f to col, 1f to Color(0x00000000),
							center = Offset(s / 2f, s / 2f), radius = s * 0.45f
						),
						radius = s * 0.45f, center = Offset(s / 2f, s / 2f)
					)
				} else {
					drawCircle(C.trackB, s * 0.2f, Offset(s / 2f, s / 2f))
				}
			}
			"trails" -> {
				drawRoundRect(C.white, cornerRadius = cr)
				for (i in 0 until 5) {
					val col = when (it.kind) {
						"rainbow" -> hsl2c((i * 55).toDouble(), 90.0, 60.0)
						"star" -> if (i % 2 == 1) Color(0xFFFFD700) else Color.White
						else -> it.c ?: C.trackB
					}
					drawCircle(col, s * (0.12f - i * 0.015f), Offset(s * (0.2f + i * 0.16f), s / 2f))
				}
			}
			else -> {
				drawRoundRect(C.white, cornerRadius = cr)
				val pr = s * 0.34f
				val cx = s / 2f
				val cy = s / 2f
				drawRoundRect(
					it.c ?: C.accent,
					topLeft = Offset(cx - pr, cy - pr),
					size = Size(pr * 2f, pr * 2f),
					cornerRadius = CornerRadius(pr * 0.3f, pr * 0.3f)
				)
				drawRoundRect(
					it.c2 ?: C.accentD,
					topLeft = Offset(cx - pr, cy + pr * 0.5f),
					size = Size(pr * 2f, pr * 0.5f),
					cornerRadius = CornerRadius(pr * 0.3f, pr * 0.3f)
				)
				drawCircle(C.ink, pr * 0.22f, Offset(cx - pr * 0.3f, cy - pr * 0.1f))
				drawCircle(C.ink, pr * 0.22f, Offset(cx + pr * 0.3f, cy - pr * 0.1f))
				drawCircle(C.white, pr * 0.12f, Offset(cx - pr * 0.3f, cy - pr * 0.1f))
				drawCircle(C.white, pr * 0.12f, Offset(cx + pr * 0.3f, cy - pr * 0.1f))
			}
		}
		drawRoundRect(
			Color.Black.copy(alpha = 0.08f),
			cornerRadius = cr,
			style = Stroke(2.dp.toPx())
		)
	}
}

@Composable
private fun PerkRow(S: Store, p: Perk, L: String, toast: (String) -> Unit) {
	val owned = S.perk(p.id)
	val icon = when (p.icon) {
		"coin" -> IC.coin
		"target" -> UiIcons.targetB
		"puMagnet" -> IC.puMagnet
		"puShield" -> IC.puShield
		"puClock" -> IC.puClock
		else -> null
	}
	HardShadowBox(
		modifier = Modifier.fillMaxWidth(),
		shadow = 5.dp,
		shadowColor = if (owned) C.greenD else Color.Black.copy(alpha = 0.15f),
		radius = 20.dp,
		bg = C.card,
		border = 4.dp,
		borderColor = if (owned) C.green else C.cardD,
		onClick = {
			if (!owned) {
				if (S.coins >= p.price) {
					S.coins -= p.price
					S.owned["perks"]?.add(p.id)
					if (p.id == "doubler") S.doubler = true
					Sfx.sBuy(); S.save(); S.touch()
				} else { Sfx.sErr(); toast(Lang.t("noCoins", L)) }
			}
		}
	) {
		Row(
			Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(14.dp)
		) {
			Box(
				Modifier.size(52.dp).background(C.menu1, RoundedCornerShape(14.dp)),
				contentAlignment = Alignment.Center
			) {
				when {
					p.icon == "x2" -> Text("x2", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
					icon == null -> Text("\u2605", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
					else -> Icon24(icon, 28.dp, Color.White)
				}
			}
			Column(Modifier.weight(1f)) {
				Text(p.name(L), color = C.ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
				Text(p.desc(L), color = C.inkDim, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
			}
			Box(Modifier.width(110.dp)) {
				if (owned) TagPill(Lang.t("owned", L), C.green, C.white, IC.check)
				else TagPill(p.price.toString(), C.gold, C.ink, IC.coin)
			}
		}
	}
}

@Composable
private fun CoinPackRow(S: Store, cp: CoinPack, L: String, toast: (String) -> Unit) {
	HardShadowBox(
		modifier = Modifier.fillMaxWidth(),
		shadow = 5.dp,
		shadowColor = Color.Black.copy(alpha = 0.15f),
		radius = 20.dp,
		bg = C.card,
		border = 4.dp,
		borderColor = C.cardD,
		onClick = {
			if (S.gems >= cp.gem) {
				S.gems -= cp.gem
				S.coins += cp.amt
				Sfx.sBuy(); S.save(); S.touch()
			} else { Sfx.sErr(); toast(Lang.t("noGems", L)) }
		}
	) {
		Row(
			Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(14.dp)
		) {
			Box(
				Modifier.size(52.dp).background(C.menu1, RoundedCornerShape(14.dp)),
				contentAlignment = Alignment.Center
			) { Icon24(IC.coin, 28.dp, Color.White) }
			Text(
				cp.amt.toString() + " " + Lang.t("coinsU", L),
				color = C.ink, fontWeight = FontWeight.Black, fontSize = 18.sp,
				modifier = Modifier.weight(1f)
			)
			Box(Modifier.width(110.dp)) {
				TagPill(cp.gem.toString(), C.gem, C.white, IC.gem)
			}
		}
	}
}

/** Same card as [ShopItem], but the preview is a real frame of the runner. */
@Composable
private fun PlayerItem(S: Store, def: PlayerDef, L: String, toast: (String) -> Unit) {
	val owned = S.has("players", def.id)
	val equipped = S.player == def.id

	fun equip() {
		S.player = def.id
		PlayerSprites.preload(def.id)
		Sfx.sBuy(); S.save(); S.touch()
	}

	HardShadowBox(
		modifier = Modifier.fillMaxWidth(),
		shadow = 6.dp,
		shadowColor = if (equipped) C.greenD else Color.Black.copy(alpha = 0.15f),
		radius = 22.dp,
		bg = C.card,
		border = 4.dp,
		// الأساطير (VIP) بإطار دهبي
		borderColor = if (equipped) C.green else if (def.gem != null) C.gold else C.cardD,
		onClick = {
			if (owned) {
				equip()
			} else {
				val gem = def.gem
				if (gem != null) {
					if (S.gems >= gem) {
						S.gems -= gem
						S.owned["players"]?.add(def.id)
						equip()          // buying puts him straight on the pitch
					} else { Sfx.sErr(); toast(Lang.t("noGems", L)) }
				} else {
					val price = def.price ?: 0
					if (S.coins >= price) {
						S.coins -= price
						S.owned["players"]?.add(def.id)
						equip()
					} else { Sfx.sErr(); toast(Lang.t("noCoins", L)) }
				}
			}
		}
	) {
		Column(
			Modifier
				.fillMaxWidth()
				.padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(9.dp)
		) {
			PlayerPreview(def)
			// Two lines always: "ميدو" is one line, "زلاتان إبراهيموفيتش" is two, and
			// a lazy grid tops-aligns its row, so mixed heights read as crooked cards.
			Text(
				def.name(L),
				color = C.ink, fontWeight = FontWeight.Black, fontSize = 15.sp,
				textAlign = TextAlign.Center, minLines = 2, maxLines = 2,
				overflow = TextOverflow.Ellipsis
			)
			when {
				equipped -> TagPill(Lang.t("equipped", L), C.green, C.white, IC.check)
				owned -> TagPill(Lang.t("equip", L), C.white, C.inkDim, null)
				def.gem != null -> TagPill(def.gem.toString(), C.gem, C.white, IC.gem)
				def.isFree -> TagPill(Lang.t("free", L), C.gold, C.ink, null)
				else -> TagPill(def.price.toString(), C.gold, C.ink, IC.coin)
			}
		}
	}
}

/** 60x60 tile: one mid-stride frame standing on a wash of the player's kit colour. */
@Composable
private fun PlayerPreview(def: PlayerDef) {
	val id = def.id
	// Decoded off the main thread so flicking through 100 players stays smooth.
	//
	// الباج القديم: produceState لما الـ key بيتغير بيعيد تشغيل الـ producer بس
	// بيسيب الـ value القديمة زي ما هي، والشرط `if (value == null)` كان بيمنع أي
	// تحميل جديد - يعني الكرت يفضل لابس صورة اللاعب اللي قبله للأبد.
	// الحل: نخزّن الـ id مع الصورة، ومانرسمش غير لما الاتنين يبقوا نفس اللاعب.
	val slot by produceState<Pair<String, ImageBitmap?>?>(null, id) {
		val cached = PlayerSprites.cachedThumb(id)
		value = id to cached
		if (cached == null) {
			value = id to withContext(Dispatchers.IO) { PlayerSprites.thumb(id) }
		}
	}
	val thumb = slot?.takeIf { it.first == id }?.second
	Canvas(Modifier.size(60.dp)) {
		val r = 16.dp.toPx()
		val cr = CornerRadius(r, r)
		val s = size.width
		drawRoundRect(
			Brush.verticalGradient(listOf(C.white, def.c.copy(alpha = 0.30f))),
			cornerRadius = cr
		)
		val img = thumb
		if (img != null) {
			val fw = img.width / def.frames
			val k = def.frames / 3
			val h = s * 0.84f
			val w = h * def.aspect
			drawImage(
				image = img,
				srcOffset = IntOffset(k * fw, 0),
				srcSize = IntSize(fw, img.height),
				dstOffset = IntOffset(Math.round((s - w) / 2f), Math.round(s - h - s * 0.04f)),
				dstSize = IntSize(Math.round(w), Math.round(h)),
				filterQuality = FilterQuality.Medium
			)
		}
		drawRoundRect(Color.Black.copy(alpha = 0.08f), cornerRadius = cr, style = Stroke(2.dp.toPx()))
	}
}
