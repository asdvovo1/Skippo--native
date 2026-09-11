package com.skippo.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/**
 * Every `.screen` in index.html, one branch per id.
 */
@Composable
fun GameScreens(
	nav: Nav,
	S: Store,
	e: Engine,
	setNav: (Nav) -> Unit,
	back: () -> Unit,
	toast: (String) -> Unit
) {
	@Suppress("UNUSED_EXPRESSION") S.rev   // recompose on every save
	val L = S.lang
	val conf = LocalConfiguration.current
	val adHost = rememberAdHost()

	fun startRun() {
		// showMidgameAd(start) — the interstitial plays before the round begins
		Ads.midgame(adHost, S) {
			if (S.perk("headstart")) e.st.shieldT = 240f
			e.startRun()
			if (S.perk("headstart")) e.st.shieldT = 240f
			Sfx.bio = e.bioKey()
			Sfx.refresh()
			setNav(Nav.Playing)
		}
	}

	fun goHome() {
		// toMenu() also clears the crates/coins of the finished run, so the parked
		// cube can never end up standing inside an obstacle on the start screen
		e.toMenu()
		setNav(Nav.Start)
	}

	when (nav) {
		Nav.Playing -> Unit

		/* ------------------------------------------------------ #startScreen */
		Nav.Start -> ScreenShell(menuBg = false) {
			// .curr-bar.keep-corner { position:absolute; top:14; right:16 }
			Row(
				Modifier.align(Alignment.TopEnd),
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				Curr(IC.coin, S.coins.toString(), small = true)
				Curr(IC.gem, S.gems.toString(), small = true)
			}
			FitScale {
				LogoTitle()
				// .best-pill
				HardShadowBox(
					shadow = 6.dp, shadowColor = Color.Black.copy(alpha = 0.15f),
					radius = 100.dp, bg = C.gold, border = 4.dp, borderColor = C.goldD
				) {
					Row(
						Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
						horizontalArrangement = Arrangement.spacedBy(10.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						Text(Lang.t("best", L), color = C.ink, fontWeight = FontWeight.Black, fontSize = 17.sp)
						Text(S.best.toString(), color = C.ink, fontWeight = FontWeight.Black, fontSize = 22.sp)
					}
				}
				XpBar(S)
				MenuCol {
					Btn(Lang.t("play", L), Modifier.fillMaxWidth(), BtnKind.Primary) { startRun() }
					Btn(
						Lang.t("daily", L), Modifier.fillMaxWidth(), BtnKind.Gold,
						leading = { Icon24(IC.gift, 26.dp, C.ink) }
					) { setNav(Nav.Daily) }
				}
				BtnRow {
					Btn(
						Lang.t("shop", L), Modifier.weight(1f), fontSize = 17.sp,
						padH = 0.dp, padV = 14.dp,
						leading = { Icon24(UiIcons.cart, 26.dp, C.ink) }
					) { setNav(Nav.Shop) }
					Btn(
						Lang.t("leaderboard", L), Modifier.weight(1f), fontSize = 17.sp,
						padH = 0.dp, padV = 14.dp,
						leading = { Icon24(UiIcons.bars, 26.dp, C.ink) }
					) { setNav(Nav.Lb) }
				}
				BtnRow {
					Btn(
						Lang.t("missions", L), Modifier.weight(1f), fontSize = 17.sp,
						padH = 0.dp, padV = 14.dp,
						leading = { Icon24(UiIcons.targetB, 26.dp, C.ink) }
					) { setNav(Nav.Missions) }
					Btn(
						Lang.t("ach", L), Modifier.weight(1f), fontSize = 17.sp,
						padH = 0.dp, padV = 14.dp,
						leading = { Icon24(UiIcons.trophyB, 26.dp, C.ink) }
					) { setNav(Nav.Ach) }
				}
				MenuCol {
					Btn(
						Lang.t("settings", L), Modifier.fillMaxWidth(),
						fontSize = 19.sp, padH = 0.dp, padV = 14.dp,
						leading = { Icon24(UiIcons.gear, 26.dp, C.ink) }
					) { setNav(Nav.Settings) }
				}
			}
		}

		/* ------------------------------------------------------ #pauseScreen */
		Nav.Pause -> ScreenShell(menuBg = true) {
			FitScale {
				// one line only: the title must never break, not even before the "!"
				Text(
					Lang.t("paused", L),
					color = C.white,
					fontWeight = FontWeight.Black,
					fontSize = clampCss(48f, 13f * conf.screenWidthDp / 100f, 84f).sp,
					textAlign = TextAlign.Center,
					maxLines = 1,
					softWrap = false,
					overflow = TextOverflow.Visible
				)
				MenuCol {
					Btn(Lang.t("resume", L), Modifier.fillMaxWidth(), BtnKind.Primary) {
						e.st.mode = "play"
						setNav(Nav.Playing)
					}
					Btn(Lang.t("settings", L), Modifier.fillMaxWidth()) { setNav(Nav.Settings) }
					Btn(Lang.t("quit", L), Modifier.fillMaxWidth(), BtnKind.Ghost) { goHome() }
				}
			}
		}

		/* ------------------------------------------------------ #endScreen */
		Nav.End -> ScreenShell(menuBg = true) {
			FitScale {
				// "خسرت!" is one word plus its exclamation mark: keep it on a single
				// line so the "!" can never be pushed onto a line of its own.
				Text(
					Lang.t("gameover", L),
					color = C.white,
					fontWeight = FontWeight.Black,
					fontSize = clampCss(52f, 13f * conf.screenWidthDp / 100f, 84f).sp,
					textAlign = TextAlign.Center,
					maxLines = 1,
					softWrap = false,
					overflow = TextOverflow.Visible
				)
				if (e.st.score > 0 && e.st.score >= S.best) {
					// .newbest-badge
					HardShadowBox(
						shadow = 5.dp, shadowColor = Color.Black.copy(alpha = 0.15f),
						radius = 100.dp, bg = C.gold, border = 4.dp, borderColor = C.goldD
					) {
						Row(
							Modifier.padding(horizontal = 22.dp, vertical = 9.dp),
							horizontalArrangement = Arrangement.spacedBy(6.dp),
							verticalAlignment = Alignment.CenterVertically
						) {
							Icon24(UiIcons.star, 16.dp, C.ink)
							Text(Lang.t("newbest", L), color = C.ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
						}
					}
				}
				// .stats-card
				HardShadowBox(
					shadow = 12.dp, shadowColor = Color.Black.copy(alpha = 0.25f),
					radius = 28.dp, bg = C.card, border = 5.dp, borderColor = C.cardD
				) {
					Row(
						Modifier.padding(horizontal = 40.dp, vertical = 22.dp),
						horizontalArrangement = Arrangement.spacedBy(40.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						Column(horizontalAlignment = Alignment.CenterHorizontally) {
							Text(Lang.t("scoreLbl", L), color = C.inkDim, fontWeight = FontWeight.Black, fontSize = 16.sp)
							Text(e.st.score.toString(), color = C.accent, fontWeight = FontWeight.Black, fontSize = 52.sp)
						}
						Box(Modifier.width(4.dp).height(72.dp).background(C.cardD, RoundedCornerShape(4.dp)))
						Column(horizontalAlignment = Alignment.CenterHorizontally) {
							Text(Lang.t("coinsLbl", L), color = C.inkDim, fontWeight = FontWeight.Black, fontSize = 16.sp)
							Text(e.st.run.coins.toString(), color = C.goldD, fontWeight = FontWeight.Black, fontSize = 52.sp)
						}
					}
				}
				MenuCol {
					if (!e.st.revived) {
						val cost = S.reviveCost()
						Btn(
							Lang.t("revive", L) + "   " + cost,
							Modifier.fillMaxWidth(), BtnKind.Gold
						) {
							fun doRevive() {
								e.st.revived = true
								e.st.mode = "play"
								e.st.shieldT = 240f
								e.obs.clear()
								e.pows.clear()
								e.resetPlayer()
								e.spwnT = 90f
								Sfx.sPower()
								S.save(); S.touch()
								setNav(Nav.Playing)
							}
							if (S.coins >= cost) {
								S.coins -= cost
								doRevive()
							} else if (Ads.rewardReady && Ads.enabled(S)) {
								// showRewardedAd() — a free revive instead of the coins
								Ads.rewarded(adHost, S) { doRevive() }
							} else {
								Sfx.sErr(); toast(Lang.t("noCoins", L))
							}
						}
					}
					Btn(Lang.t("retry", L), Modifier.fillMaxWidth(), BtnKind.Primary) { startRun() }
					Btn(Lang.t("home", L), Modifier.fillMaxWidth(), BtnKind.Ghost) { goHome() }
				}
			}
		}

		/* ------------------------------------------------------ #settingsScreen */
		Nav.Settings -> ScreenShell(menuBg = true) {
			Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
				// leaving settings syncs whatever was typed, even without pressing Done
				// wide = true: الإعدادات مفرودة بعرض الشاشة زي المتجر والمتصدرين
				Sheet(
					Lang.t("settingsTitle", L),
					onClose = { Cloud.pushProfile(S); back() },
					wide = true
				) {
					// ---- who you are on the leaderboard ----
					Text(Lang.t("playerName", L), color = C.ink, fontWeight = FontWeight.Black, fontSize = 21.sp)
					Spacer(Modifier.height(12.dp))
					NameField(S)
					Spacer(Modifier.height(18.dp))
					Text(Lang.t("playerPhoto", L), color = C.ink, fontWeight = FontWeight.Black, fontSize = 21.sp)
					Spacer(Modifier.height(12.dp))
					AvatarPicker(S)
					Spacer(Modifier.height(18.dp))
					Text(Lang.t("language", L), color = C.ink, fontWeight = FontWeight.Black, fontSize = 21.sp)
					Spacer(Modifier.height(12.dp))
					Seg(listOf("ar" to "\u0627\u0644\u0639\u0631\u0628\u064a\u0629", "en" to "English"), S.lang) {
						S.lang = it; S.langSet = true; S.save(); S.touch()
					}
					Spacer(Modifier.height(18.dp))
					Text(Lang.t("difficulty", L), color = C.ink, fontWeight = FontWeight.Black, fontSize = 21.sp)
					Spacer(Modifier.height(12.dp))
					Seg(
						listOf(
							"easy" to Lang.t("easy", L),
							"normal" to Lang.t("normal", L),
							"hard" to Lang.t("hard", L)
						),
						S.diff
					) { S.diff = it; S.save(); S.touch() }
					Spacer(Modifier.height(6.dp))
					SettingRow(Lang.t("sfx", L)) {
						Toggle(S.sfx) { S.sfx = it; S.save(); S.touch() }
					}
					SettingRow(Lang.t("ambient", L)) {
						Toggle(S.ambient) { S.ambient = it; Sfx.setWind(it); S.save(); S.touch() }
					}
					SettingRow(Lang.t("haptics", L)) {
						Toggle(S.haptic) { S.haptic = it; S.save(); S.touch() }
					}
					Spacer(Modifier.height(16.dp))
					Row(
						Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically
					) {
						Text(Lang.t("volume", L), color = C.ink, fontWeight = FontWeight.Black, fontSize = 21.sp)
						// .val-badge
						Box(
							Modifier
								.background(C.accent, RoundedCornerShape(100.dp))
								.padding(horizontal = 16.dp, vertical = 5.dp)
						) {
							Text(S.vol.toString(), color = C.white, fontWeight = FontWeight.Black, fontSize = 18.sp)
						}
					}
					VolSlider(S)
					Spacer(Modifier.height(16.dp))
					// .danger-link
					Text(
						Lang.t("resetAll", L),
						color = C.accent,
						fontWeight = FontWeight.Black,
						fontSize = 18.sp,
						textAlign = TextAlign.Center,
						modifier = Modifier
							.fillMaxWidth()
							.clickable(
								interactionSource = remember { MutableInteractionSource() },
								indication = null
							) { S.resetAll(); Sfx.vol = S.vol; S.touch() }
					)
				}
			}
		}

		/* ------------------------------------------------------ #shopScreen */
		Nav.Shop -> ShopScreen(S, back, toast)

		/* ------------------------------------------------------ #lbScreen */
		Nav.Lb -> ScreenShell(menuBg = true) {
			Column(
				Modifier.fillMaxSize(),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(22.dp)
			) {
				PanelHead(Lang.t("leaderboardTitle", L), back)
				LaunchedEffect(Cloud.on) { Cloud.loadTop() }
				// the online board once Firebase answers, this device's records otherwise
				val cloud = Cloud.on && Cloud.top.isNotEmpty()
				val rows =
					if (cloud) Cloud.top.map {
						ScoreRow(
							if (it.uid == Cloud.uid) "" else it.name.ifBlank { "Player" },
							it.best,
							0L
						)
					} else S.scores.sortedByDescending { it.score }
				// photos line up one-to-one with the rows above
				val faces = if (cloud) Cloud.top.map { it.avatar } else rows.map { S.avatar }
				if (Cloud.loading && rows.isEmpty()) {
					Text(
						Lang.t("lbLoading", L),
						color = Color.White.copy(alpha = 0.75f),
						fontWeight = FontWeight.ExtraBold,
						fontSize = 16.sp,
						modifier = Modifier.padding(vertical = 30.dp, horizontal = 10.dp)
					)
				} else if (rows.isEmpty()) {
					Text(
						Lang.t("lbEmpty", L),
						color = Color.White.copy(alpha = 0.75f),
						fontWeight = FontWeight.ExtraBold,
						fontSize = 16.sp,
						modifier = Modifier.padding(vertical = 30.dp, horizontal = 10.dp)
					)
				} else {
					LazyColumn(
						Modifier.width(panelWidth().dp).fillMaxHeight(),
						verticalArrangement = Arrangement.spacedBy(12.dp),
						contentPadding = PaddingValues(bottom = 14.dp)
					) {
						itemsIndexed(rows) { i, row ->
							LbRow(i + 1, row, S, faces.getOrElse(i) { "" })
						}
					}
				}
				Text(
					Lang.t(if (cloud) "cloudOn" else "lbNoteLocal", L),
					color = Color.White.copy(alpha = 0.7f),
					fontWeight = FontWeight.ExtraBold, fontSize = 14.sp
				)
			}
		}

		/* ------------------------------------------------------ #missionsScreen */
		Nav.Missions -> ScreenShell(menuBg = true) {
			remember { S.rollMissions(); 0 }
			Column(
				Modifier.fillMaxSize(),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(22.dp)
			) {
				PanelHead(Lang.t("missionsTitle", L), back)
				Column(
					Modifier.width(panelWidth().dp).verticalScroll(rememberScrollState()),
					verticalArrangement = Arrangement.spacedBy(12.dp)
				) {
					for (m in S.missions) {
						val def = S.missionDef(m.id)
						ListRow(
							icon = UiIcons.targetB,
							title = def.name(L),
							sub = min(m.prog, def.goal).toString() + " / " + def.goal,
							progress = m.prog.toFloat() / def.goal,
							reward = def.rw,
							claimed = m.claimed,
							done = m.done
						) {
							m.claimed = true
							S.coins += def.rw
							Sfx.sBuy(); S.save(); S.touch()
						}
					}
					Spacer(Modifier.height(14.dp))
				}
			}
		}

		/* ------------------------------------------------------ #achScreen */
		Nav.Ach -> ScreenShell(menuBg = true) {
			Column(
				Modifier.fillMaxSize(),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(22.dp)
			) {
				PanelHead(Lang.t("achTitle", L), back)
				Column(
					Modifier.width(panelWidth().dp).verticalScroll(rememberScrollState()),
					verticalArrangement = Arrangement.spacedBy(12.dp)
				) {
					for (t in Catalog.ACH_TRACKS) {
						val lvl = S.achLvl[t.id] ?: 0
						val goal = Catalog.achGoalAt(t, lvl)
						val have = S.statOf(t.stat)
						val rw = Catalog.achRewardAt(t, lvl)
						ListRow(
							icon = UiIcons.trophyB,
							title = t.name(L) + "  \u2022  " + Lang.t("lvl", L) + " " + (lvl + 1),
							sub = min(have, goal).toString() + " / " + goal,
							progress = have.toFloat() / goal,
							reward = rw,
							claimed = false,
							done = have >= goal
						) {
							S.achLvl[t.id] = lvl + 1
							S.coins += rw
							Sfx.sBuy(); S.save(); S.touch()
						}
					}
					Spacer(Modifier.height(14.dp))
				}
			}
		}

		/* ------------------------------------------------------ #dailyScreen */
		Nav.Daily -> ScreenShell(menuBg = true) {
			// uiW() مش conf.screenWidthDp — نفس سبب panelWidth(): الحساب الخام
			// بيطلع الجريد قاعد في نص الشاشة والجنبين فاضيين.
			val gw = min(500f, uiW() * 0.92f)
			val rewards = S.dailyRewards()
			val today = S.dailyStreak % 7
			val claimedToday = S.dailyLast == S.todayStr()
			FitScale {
				Text(Lang.t("dailyTitle", L), color = C.white, fontWeight = FontWeight.Black, fontSize = 32.sp)
				// .week-label
				Box(
					Modifier
						.background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(100.dp))
						.padding(horizontal = 20.dp, vertical = 6.dp)
				) {
					Text(
						Lang.t("week", L) + " " + (S.dailyStreak / 7 + 1),
						color = Color.White.copy(alpha = 0.85f),
						fontWeight = FontWeight.Black, fontSize = 18.sp
					)
				}
				// .daily-grid — 4 columns, gap 12
				LazyVerticalGrid(
					columns = GridCells.Fixed(4),
					modifier = Modifier
						.width(gw.dp)
						.height((((gw - 36f) / 4f) * 2f + 12f).dp),
					horizontalArrangement = Arrangement.spacedBy(12.dp),
					verticalArrangement = Arrangement.spacedBy(12.dp),
					userScrollEnabled = false
				) {
					items(7) { i ->
						DayCell(
							i,
							rewards[i],
							claimed = i < today || (i == today && claimedToday),
							isToday = i == today
						)
					}
				}
				MenuCol {
					Btn(
						if (claimedToday) Lang.t("tomorrow", L) else Lang.t("claim", L),
						Modifier.fillMaxWidth(),
						if (claimedToday) BtnKind.Ghost else BtnKind.Gold,
						enabled = !claimedToday
					) {
						val rw = rewards[today]
						if (rw.first == "gem") S.gems += rw.second else S.coins += rw.second
						S.dailyLast = S.todayStr()
						S.dailyStreak += 1
						Sfx.sBuy(); S.save(); S.touch()
					}
					Btn(Lang.t("back", L), Modifier.fillMaxWidth(), BtnKind.Ghost) { back() }
				}
			}
		}
	}
}

/** input[type=range] with the accent thumb. */
@Composable
private fun VolSlider(S: Store) {
	var v by remember { mutableFloatStateOf(S.vol.toFloat()) }
	Slider(
		value = v,
		onValueChange = {
			v = it
			S.vol = it.toInt()
			Sfx.vol = S.vol
			Sfx.refresh()
		},
		onValueChangeFinished = { S.save(); S.touch() },
		valueRange = 0f..100f,
		colors = SliderDefaults.colors(
			thumbColor = C.accent,
			activeTrackColor = C.accent,
			inactiveTrackColor = C.trackC
		)
	)
}
