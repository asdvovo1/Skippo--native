package com.skippo.game

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

data class ScoreRow(val name: String, val score: Int, val ts: Long)

data class MissionState(var id: String, var prog: Int, var done: Boolean, var claimed: Boolean)

/**
 * Port of the `S` save object + localStorage persistence.
 * Storage key is identical to the web build: skip_save_v21
 */
class Store(ctx: Context) {

	companion object {
		/**
		 * ===== مفتاح نسخة التجربة =====
		 *
		 *   true  = كل حاجة مفتوحة ومجانية (للتجربة بس)
		 *   false = اللعبة طبيعية زي ما هي (النسخة اللي بتتنشر)
		 *
		 * وهي true:
		 *  • كل اللاعيبة والأساطير والثيمات والملاعب والتوهج والآثار مفتوحة
		 *  • كل المميزات (perks) شغالة + مضاعف الكوينز
		 *  • كوينز وجواهر مليانة
		 *  • مفيش إعلانات
		 *
		 * مهم: نسخة التجربة بتحفظ في خانة منفصلة خالص، يعني تقدمك الحقيقي
		 * مابيتلمسش. رجّع السطر ده false وكل حاجة ترجع زي ما كانت بالظبط.
		 */
		const val UNLOCK_ALL = true

		/** خانة الحفظ. نسخة التجربة ليها خانتها لوحدها. */
		val LS = if (UNLOCK_ALL) "skip_save_v21_test" else "skip_save_v21"
	}

	private val prefs = ctx.getSharedPreferences("skippo", Context.MODE_PRIVATE)

	// bump this to force any observing composable to re-read the store
	var rev by mutableIntStateOf(0)
		private set

	var lang = "en"
	var langSet = false
	var name = ""
	var avatar = ""          // base64 jpeg profile photo, "" = none
	var tutDone = false
	var ambient = true
	var sfx = true
	var haptic = true
	var vol = 70
	var diff = "normal"
	var best = 0
	var coins = 0
	var gems = 0
	var notif = false
	var notifLast = ""
	var skin = "classic"
	var trail = "none"
	var theme = "day"
	var glow = "none"
	var player = Players.DEFAULT   // the footballer that runs in-game
	var doubler = false
	var removeAds = false

	val owned = hashMapOf(
		"players" to mutableListOf(Players.DEFAULT),
		"skins" to mutableListOf("classic"),
		"trails" to mutableListOf("none"),
		"themes" to mutableListOf("day"),
		"glows" to mutableListOf("none"),
		"perks" to mutableListOf<String>()
	)

	var runs = 0
	var totalScore = 0
	var totalCoins = 0
	var maxCombo = 0
	var jumps = 0

	var dailyLast = ""
	var dailyStreak = 0

	var missionsDate = ""
	val missions = mutableListOf<MissionState>()

	val achLvl = hashMapOf<String, Int>()
	var xp = 0
	var playerLvl = 1
	val scores = mutableListOf<ScoreRow>()

	init {
		load()
		if (UNLOCK_ALL) unlockAll()
	}

	/** فتح كل حاجة لنسخة التجربة. بيتكتب في خانة التجربة بس. */
	private fun unlockAll() {
		if (coins < 1_000_000) coins = 9_999_999
		if (gems < 10_000) gems = 99_999
		doubler = true      // ×2 كوينز
		removeAds = true    // مفيش إعلانات تقطع التجربة
	}

	fun touch() {
		rev++
	}

	// في نسخة التجربة كل حاجة محسوبة مشتراة، فالمتجر بيوري "تفعيل" بدل السعر
	// ومابيتخصمش أي حاجة. مافيش حاجة بتتكتب في قائمة الممتلكات، فلما
	// ترجّع UNLOCK_ALL لـ false كل حاجة ترجع زي ما كانت.
	fun has(cat: String, id: String) = UNLOCK_ALL || owned[cat]?.contains(id) == true

	fun perk(id: String) = UNLOCK_ALL || owned["perks"]?.contains(id) == true

	fun statOf(key: String): Int = when (key) {
		"best" -> best
		"totalCoins" -> totalCoins
		"runs" -> runs
		"jumps" -> jumps
		"maxCombo" -> maxCombo
		else -> 0
	}

	/** reviveCost() from index.html */
	fun reviveCost(): Int = (250.0 * (if (perk("saver")) 0.6 else 1.0)).roundToInt()

	/** xpNeed(lvl) from index.html */
	fun xpNeed(lvl: Int): Int =
		(150.0 + (lvl - 1) * 120.0 + (lvl - 1).toDouble() * (lvl - 1).toDouble() * 12.0).roundToInt()

	/** addXp(n) - returns the number of levels gained. */
	fun addXp(n: Int): Int {
		xp += n
		var gained = 0
		while (xp >= xpNeed(playerLvl)) {
			xp -= xpNeed(playerLvl)
			playerLvl++
			gained++
			coins += 60 * playerLvl
			if (playerLvl % 5 == 0) gems += 5 + (playerLvl / 5) * 2
		}
		return gained
	}

	fun todayStr(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

	/** dailyRewards() from index.html */
	fun dailyRewards(): List<Pair<String, Int>> {
		val wk = floor(dailyStreak / 7.0).toInt()
		val m = 1.0 + wk * 0.6
		return listOf(
			"coin" to (100 * m).roundToInt(),
			"coin" to (150 * m).roundToInt(),
			"coin" to (220 * m).roundToInt(),
			"gem" to (3 + wk * 2),
			"coin" to (320 * m).roundToInt(),
			"coin" to (450 * m).roundToInt(),
			"gem" to (10 + wk * 4)
		)
	}

	/** seededPick(seed, pool, n) - identical hash to the web version. */
	fun rollMissions() {
		val today = todayStr()
		if (missionsDate == today && missions.isNotEmpty()) return
		var h = 0L
		for (ch in today) h = (h * 31L + ch.code.toLong()) and 0xFFFFFFFFL
		val pool = Catalog.MISSION_POOL.toMutableList()
		val out = mutableListOf<MissionDef>()
		var n = 4
		while (n > 0 && pool.isNotEmpty()) {
			h = (h * 1103515245L + 12345L) and 0xFFFFFFFFL
			val i = (h % pool.size.toLong()).toInt()
			out.add(pool.removeAt(i))
			n--
		}
		missionsDate = today
		missions.clear()
		out.forEach { missions.add(MissionState(it.id, 0, false, false)) }
		save()
	}

	fun missionDef(id: String) = Catalog.MISSION_POOL.first { it.id == id }

	fun addScoreRow(score: Int) {
		scores.add(ScoreRow(if (name.isBlank()) "" else name, score, System.currentTimeMillis()))
		scores.sortByDescending { it.score }
		while (scores.size > 20) scores.removeAt(scores.size - 1)
	}

	// ---------- persistence ----------

	fun save() {
		val o = JSONObject()
		o.put("lang", lang); o.put("langSet", langSet); o.put("name", name)
		o.put("avatar", avatar)
		o.put("tutDone", tutDone); o.put("ambient", ambient); o.put("sfx", sfx)
		o.put("haptic", haptic); o.put("vol", vol); o.put("diff", diff)
		o.put("best", best); o.put("coins", coins); o.put("gems", gems)
		o.put("notif", notif); o.put("notifLast", notifLast)
		o.put("skin", skin); o.put("trail", trail); o.put("theme", theme); o.put("glow", glow)
		o.put("player", player)
		o.put("doubler", doubler); o.put("removeAds", removeAds)
		o.put("xp", xp); o.put("playerLvl", playerLvl)

		val ow = JSONObject()
		owned.forEach { (k, v) -> ow.put(k, JSONArray(v)) }
		o.put("owned", ow)

		val st = JSONObject()
		st.put("runs", runs); st.put("totalScore", totalScore); st.put("totalCoins", totalCoins)
		st.put("maxCombo", maxCombo); st.put("jumps", jumps)
		o.put("stats", st)

		val dl = JSONObject()
		dl.put("last", dailyLast); dl.put("streak", dailyStreak)
		o.put("daily", dl)

		val ms = JSONObject()
		ms.put("date", missionsDate)
		val arr = JSONArray()
		missions.forEach {
			arr.put(JSONObject().put("id", it.id).put("prog", it.prog).put("done", it.done).put("claimed", it.claimed))
		}
		ms.put("data", arr)
		o.put("missions", ms)

		val al = JSONObject()
		achLvl.forEach { (k, v) -> al.put(k, v) }
		o.put("achLvl", al)

		val sc = JSONArray()
		scores.forEach { sc.put(JSONObject().put("name", it.name).put("score", it.score).put("ts", it.ts)) }
		o.put("scores", sc)

		prefs.edit().putString(LS, o.toString()).apply()
		touch()
	}

	private fun load() {
		val raw = prefs.getString(LS, null) ?: return
		try {
			val o = JSONObject(raw)
			lang = o.optString("lang", lang); langSet = o.optBoolean("langSet", langSet)
			name = o.optString("name", name); avatar = o.optString("avatar", avatar)
			tutDone = o.optBoolean("tutDone", tutDone)
			ambient = o.optBoolean("ambient", ambient); sfx = o.optBoolean("sfx", sfx)
			haptic = o.optBoolean("haptic", haptic); vol = o.optInt("vol", vol)
			diff = o.optString("diff", diff); best = o.optInt("best", best)
			coins = o.optInt("coins", coins); gems = o.optInt("gems", gems)
			notif = o.optBoolean("notif", notif); notifLast = o.optString("notifLast", notifLast)
			skin = o.optString("skin", skin); trail = o.optString("trail", trail)
			theme = o.optString("theme", theme); glow = o.optString("glow", glow)
			player = o.optString("player", player)
			if (!Players.ALL.containsKey(player)) player = Players.DEFAULT
			doubler = o.optBoolean("doubler", doubler); removeAds = o.optBoolean("removeAds", removeAds)
			xp = o.optInt("xp", xp); playerLvl = o.optInt("playerLvl", playerLvl)

			o.optJSONObject("owned")?.let { ow ->
				for (k in owned.keys) {
					val a = ow.optJSONArray(k) ?: continue
					val l = owned[k]!!
					for (i in 0 until a.length()) {
						val v = a.optString(i)
						if (v.isNotEmpty() && !l.contains(v)) l.add(v)
					}
				}
			}
			o.optJSONObject("stats")?.let {
				runs = it.optInt("runs", 0); totalScore = it.optInt("totalScore", 0)
				totalCoins = it.optInt("totalCoins", 0); maxCombo = it.optInt("maxCombo", 0)
				jumps = it.optInt("jumps", 0)
			}
			o.optJSONObject("daily")?.let {
				dailyLast = it.optString("last", ""); dailyStreak = it.optInt("streak", 0)
			}
			o.optJSONObject("missions")?.let { m ->
				missionsDate = m.optString("date", "")
				missions.clear()
				val a = m.optJSONArray("data")
				if (a != null) for (i in 0 until a.length()) {
					val e = a.optJSONObject(i) ?: continue
					val id = e.optString("id")
					if (Catalog.MISSION_POOL.none { it.id == id }) continue
					missions.add(MissionState(id, e.optInt("prog"), e.optBoolean("done"), e.optBoolean("claimed")))
				}
			}
			o.optJSONObject("achLvl")?.let { al ->
				al.keys().forEach { k -> achLvl[k] = al.optInt(k, 0) }
			}
			o.optJSONArray("scores")?.let { a ->
				scores.clear()
				for (i in 0 until a.length()) {
					val e = a.optJSONObject(i) ?: continue
					scores.add(ScoreRow(e.optString("name"), e.optInt("score"), e.optLong("ts")))
				}
			}
		} catch (_: Throwable) {
			// corrupt save -> keep defaults, exactly like the try/catch in the web build
		}
	}

	fun resetAll() {
		prefs.edit().remove(LS).apply()
		lang = "en"; langSet = false; name = ""; avatar = ""; tutDone = false
		ambient = true; sfx = true; haptic = true; vol = 70; diff = "normal"
		best = 0; coins = 0; gems = 0; notif = false; notifLast = ""
		skin = "classic"; trail = "none"; theme = "day"; glow = "none"
		player = Players.DEFAULT
		doubler = false; removeAds = false
		owned["players"] = mutableListOf(Players.DEFAULT)
		owned["skins"] = mutableListOf("classic")
		owned["trails"] = mutableListOf("none")
		owned["themes"] = mutableListOf("day")
		owned["glows"] = mutableListOf("none")
		owned["perks"] = mutableListOf()
		runs = 0; totalScore = 0; totalCoins = 0; maxCombo = 0; jumps = 0
		dailyLast = ""; dailyStreak = 0
		missionsDate = ""; missions.clear()
		achLvl.clear(); xp = 0; playerLvl = 1; scores.clear()
		if (UNLOCK_ALL) unlockAll()
		save()
	}
}
