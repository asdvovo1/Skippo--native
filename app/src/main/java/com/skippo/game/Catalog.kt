package com.skippo.game

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import kotlin.math.pow

/**
 * Shop item. Mirrors the plain JS objects in SKINS / GLOWS / TRAILS / THEMES.
 * `kind` replaces the magic string colours ("rainbow" / "star") used in JS.
 */
data class Item(
	var ar: String,
	var en: String,
	val c: Color? = null,
	val c2: Color? = null,
	val kind: String? = null,
	val top: Color? = null,
	val bot: Color? = null,
	val ground: Color? = null,
	val gTop: Color? = null,
	var price: Int? = null,
	var gem: Int? = null,
	/** مجموعة الخانة: "nations" لملاعب المنتخبات و "clubsVip" لملاعب الأندية VIP. */
	var grp: String? = null
) {
	fun name(lang: String) = if (lang == "ar") ar else en
}

data class Perk(
	val id: String,
	val ar: String,
	val en: String,
	val dAr: String,
	val dEn: String,
	val price: Int,
	val icon: String?
) {
	fun name(lang: String) = if (lang == "ar") ar else en
	fun desc(lang: String) = if (lang == "ar") dAr else dEn
}

data class CoinPack(val id: String, val amt: Int, val gem: Int)

data class MissionDef(val id: String, val ar: String, val en: String, val goal: Int, val rw: Int, val type: String) {
	fun name(lang: String) = if (lang == "ar") ar else en
}

data class AchTrack(val id: String, val ar: String, val en: String, val base: Int, val mult: Double, val rw: Int, val rwMult: Double, val stat: String) {
	fun name(lang: String) = if (lang == "ar") ar else en
}

data class World(
	val id: String,
	val ar: String,
	val en: String,
	val top: Color,
	val bot: Color,
	val ground: Color,
	val gTop: Color,
	val grav: Float,
	val jump: Float,
	val wind: Float
) {
	fun name(lang: String) = if (lang == "ar") ar else en
}

object Catalog {

	// ---------- base catalogues (exact order preserved) ----------

	val SKINS: LinkedHashMap<String, Item> = linkedMapOf(
		"classic" to Item("الأصلي", "Classic", Color(0xFFFF5A3C), Color(0xFFCC3D24), price = 0),
		"mint" to Item("نعناع", "Mint", Color(0xFF39C07A), Color(0xFF2A9860), price = 150),
		"sky" to Item("سماوي", "Sky", Color(0xFF4A9FE0), Color(0xFF2F6BAD), price = 150),
		"grape" to Item("عنبي", "Grape", Color(0xFF9B5DE5), Color(0xFF7038C0), price = 250),
		"sun" to Item("شمسي", "Sun", Color(0xFFFFC93C), Color(0xFFE0A020), price = 250),
		"rose" to Item("وردي", "Rose", Color(0xFFFF7EB6), Color(0xFFE04F8F), price = 250),
		"cherry" to Item("كرز", "Cherry", Color(0xFFE63946), Color(0xFFA01A26), price = 300),
		"aqua" to Item("مائي", "Aqua", Color(0xFF00B4D8), Color(0xFF0077A8), price = 300),
		"panda" to Item("باندا", "Panda", Color(0xFFFFFFFF), Color(0xFF2A2440), price = 400),
		"ninja" to Item("نينجا", "Ninja", Color(0xFF3A3550), Color(0xFF1A1626), price = 400),
		"robot" to Item("روبوت", "Robot", Color(0xFF8B9DC3), Color(0xFF5A6A8A), price = 450),
		"ghost" to Item("شبح", "Ghost", Color(0xFFE8E8F5), Color(0xFFB0B0D0), price = 450),
		"lava" to Item("حمم", "Lava", Color(0xFFFF3B30), Color(0xFF8A1A12), price = 500),
		"cosmic" to Item("كوني", "Cosmic", Color(0xFF7B2FBE), Color(0xFF4A1A7A), price = 700),
		"gold" to Item("ذهبي", "Golden", Color(0xFFFFD700), Color(0xFFB8860B), gem = 40),
		"diamond" to Item("ماسي", "Diamond", Color(0xFFB9F2FF), Color(0xFF5BC8E0), gem = 60)
	)

	val GLOWS: LinkedHashMap<String, Item> = linkedMapOf(
		"none" to Item("بدون", "None", null, price = 0),
		"redglow" to Item("توهج أحمر", "Red Glow", Color(0xFFFF3B30), price = 300),
		"blueglow" to Item("توهج أزرق", "Blue Glow", Color(0xFF2F8FFF), price = 300),
		"greenglow" to Item("توهج أخضر", "Green Glow", Color(0xFF2FE07A), price = 350),
		"goldglow" to Item("توهج ذهبي", "Gold Glow", Color(0xFFFFCF3C), price = 400),
		"cyanglow" to Item("توهج سماوي", "Cyan Glow", Color(0xFF00E5FF), price = 450),
		"rainbowglow" to Item("توهج قوس قزح", "Rainbow Glow", null, kind = "rainbow", price = 900),
		"whiteglow" to Item("توهج أبيض", "White Glow", Color(0xFFFFFFFF), gem = 35)
	)

	val TRAILS: LinkedHashMap<String, Item> = linkedMapOf(
		"none" to Item("بدون", "None", null, price = 0),
		"fire" to Item("نار", "Fire", Color(0xFFFF5A3C), price = 200),
		"ice" to Item("جليد", "Ice", Color(0xFF7FD8FF), price = 200),
		"leaf" to Item("أوراق", "Leaves", Color(0xFF39C07A), price = 250),
		"gold" to Item("ذهب", "Gold", Color(0xFFFFC93C), price = 350),
		"purple" to Item("بنفسجي", "Purple", Color(0xFF9B5DE5), price = 350),
		"electric" to Item("كهرباء", "Electric", Color(0xFF00E5FF), price = 400),
		"rainbow" to Item("قوس قزح", "Rainbow", null, kind = "rainbow", price = 800),
		"star" to Item("نجوم", "Stars", null, kind = "star", gem = 30)
	)

	val THEMES: LinkedHashMap<String, Item> = linkedMapOf(
		"day" to Item("نهار", "Day", top = Color(0xFF4A9FE0), bot = Color(0xFF8FD0F0), ground = Color(0xFF2F6BAD), gTop = Color(0xFF3F83C9), price = 0),
		"sunset" to Item("غروب", "Sunset", top = Color(0xFFFF7E5F), bot = Color(0xFFFEB47B), ground = Color(0xFFA84860), gTop = Color(0xFFC86078), price = 400),
		"night" to Item("ليل", "Night", top = Color(0xFF1A1A3E), bot = Color(0xFF3A3560), ground = Color(0xFF12122A), gTop = Color(0xFF22224A), price = 500),
		"forest" to Item("غابة", "Forest", top = Color(0xFF2D6A4F), bot = Color(0xFF74C69D), ground = Color(0xFF1B4332), gTop = Color(0xFF2D6A4F), price = 600),
		"desert" to Item("صحراء", "Desert", top = Color(0xFFE9C46A), bot = Color(0xFFF4E4BC), ground = Color(0xFFC8974A), gTop = Color(0xFFE0B060), price = 600),
		"space" to Item("فضاء", "Space", top = Color(0xFF0F0524), bot = Color(0xFF2D1B52), ground = Color(0xFF1A0F30), gTop = Color(0xFF2A1A4A), price = 900),
		"neon" to Item("نيون", "Neon", top = Color(0xFF1A0033), bot = Color(0xFF330066), ground = Color(0xFF0D001A), gTop = Color(0xFFFF00FF), gem = 50),

		// ---------- أندية / ملاعب: الخلفية بتبقى ملعب كامل بألوان النادي (kind = "club") ----------
		"clubCatalan" to Item("ملعب الكتالوني", "Catalan Arena", c = Color(0xFF0B3B8C), c2 = Color(0xFF8E1B3D), kind = "club", top = Color(0xFF102A63), bot = Color(0xFF5E85C4), ground = Color(0xFF24632F), gTop = Color(0xFF3D9B4C), price = 800),
		"clubRoyal" to Item("ملعب الأبيض الملكي", "Royal White Arena", c = Color(0xFFF5F5F5), c2 = Color(0xFFD9A833), kind = "club", top = Color(0xFF3D5B8A), bot = Color(0xFFC3D9F2), ground = Color(0xFF256A31), gTop = Color(0xFF41A350), price = 800),
		"clubCairoRed" to Item("ملعب القلعة الحمرا", "Cairo Red Arena", c = Color(0xFFD8232A), c2 = Color(0xFFFFFFFF), kind = "club", top = Color(0xFF8A2A2E), bot = Color(0xFFF2B9A2), ground = Color(0xFF215C2C), gTop = Color(0xFF3A9448), price = 900),
		"clubCairoWhite" to Item("ملعب الأبيض", "Cairo White Arena", c = Color(0xFFFFFFFF), c2 = Color(0xFFC8102E), kind = "club", top = Color(0xFF4E6E8E), bot = Color(0xFFD6E7F5), ground = Color(0xFF256A31), gTop = Color(0xFF44A455), price = 900),
		"clubMersey" to Item("ملعب الأحمر الإنجليزي", "Mersey Red Arena", c = Color(0xFFC8102E), c2 = Color(0xFF00B2A9), kind = "club", top = Color(0xFF5C2632), bot = Color(0xFFE3ADA4), ground = Color(0xFF1F5A2A), gTop = Color(0xFF388F45), price = 1000),
		"clubSkyBlue" to Item("ملعب السماوي", "Sky Blue Arena", c = Color(0xFF6CABDD), c2 = Color(0xFF1C2C5B), kind = "club", top = Color(0xFF2C5C8E), bot = Color(0xFFAAD7F2), ground = Color(0xFF24632F), gTop = Color(0xFF3D9B4C), price = 1000),
		"clubBavarian" to Item("ملعب الأحمر البافاري", "Bavarian Red Arena", c = Color(0xFFDC052D), c2 = Color(0xFF0066B2), kind = "club", top = Color(0xFF7A1B2C), bot = Color(0xFFEBB3A8), ground = Color(0xFF215C2C), gTop = Color(0xFF3A9448), price = 1100),
		"clubParisian" to Item("ملعب الأزرق الباريسي", "Parisian Blue Arena", c = Color(0xFF0B2A5E), c2 = Color(0xFFE02020), kind = "club", top = Color(0xFF14224F), bot = Color(0xFF7E93C6), ground = Color(0xFF1F5A2A), gTop = Color(0xFF35893F), price = 1100),
		"clubMono" to Item("ملعب الأبيض والأسود", "Black & White Arena", c = Color(0xFF1B1B1B), c2 = Color(0xFFF2F2F2), kind = "club", top = Color(0xFF3A3A46), bot = Color(0xFFBFC2CC), ground = Color(0xFF1D5527), gTop = Color(0xFF338240), price = 1200),
		"clubRedBlack" to Item("ملعب الأحمر والأسود", "Red & Black Arena", c = Color(0xFFCC1F2B), c2 = Color(0xFF1A1A1A), kind = "club", top = Color(0xFF4A1C24), bot = Color(0xFFD79A96), ground = Color(0xFF1D5527), gTop = Color(0xFF338240), price = 1200),
		"clubBlueBlack" to Item("ملعب الأزرق والأسود", "Blue & Black Arena", c = Color(0xFF0C2073), c2 = Color(0xFF121212), kind = "club", top = Color(0xFF141A4A), bot = Color(0xFF7C86BE), ground = Color(0xFF1B4E27), gTop = Color(0xFF2F8340), price = 1300),
		"clubGreen" to Item("ملعب الأخضر", "Green Kings Arena", c = Color(0xFF0B7A42), c2 = Color(0xFFFFFFFF), kind = "club", top = Color(0xFF14523A), bot = Color(0xFFA8D9BE), ground = Color(0xFF215C2C), gTop = Color(0xFF3A9448), price = 1300),
		"clubYellowWall" to Item("ملعب الجدار الأصفر", "Yellow Wall Arena", c = Color(0xFFF7DF16), c2 = Color(0xFF141414), kind = "club", top = Color(0xFF3A3A16), bot = Color(0xFFE8DE9C), ground = Color(0xFF1D5527), gTop = Color(0xFF368B42), price = 1400),
		"clubGolden" to Item("الملعب الذهبي", "Golden Arena", c = Color(0xFFFFD24A), c2 = Color(0xFF2A1A00), kind = "club", top = Color(0xFF2A1F05), bot = Color(0xFFE8C978), ground = Color(0xFF1B4E27), gTop = Color(0xFF2F8340), price = 1500)
	)

	// ---------- procedural expansion (100 extra items per category) ----------

	private val CBASE = listOf(
		"ياقوت" to "Ruby", "زمرد" to "Emerald", "سافير" to "Sapphire", "كهرمان" to "Amber",
		"مرجان" to "Coral", "يشم" to "Jade", "أونكس" to "Onyx", "لؤلء" to "Pearl",
		"توباز" to "Topaz", "أوبال" to "Opal", "كوارتز" to "Quartz", "كوبالت" to "Cobalt",
		"نيلي" to "Indigo", "قرمزي" to "Crimson", "ليموني" to "Lime", "خوخي" to "Peach",
		"برقوقي" to "Plum", "تركواز" to "Teal", "لافندر" to "Lavender", "نحاسي" to "Copper",
		"برونزي" to "Bronze", "فضي" to "Silver", "فحمي" to "Charcoal", "عاجي" to "Ivory",
		"ماجنتا" to "Magenta"
	)

	private val CFAM = mapOf(
		"skins" to listOf("نيون" to "Neon", "مثلج" to "Frost", "جمري" to "Ember", "ملكي" to "Royal"),
		"glows" to listOf("هادي" to "Soft", "ساطع" to "Bright", "نابض" to "Pulse", "غامق" to "Deep"),
		"trails" to listOf("نيون" to "Neon", "مثلج" to "Frost", "جمري" to "Ember", "ملكي" to "Royal"),
		"themes" to listOf("الفجر" to "Dawn", "الظهيرة" to "Noon", "الغروب" to "Dusk", "الليل" to "Night")
	)

	private fun expandCat(
		obj: LinkedHashMap<String, Item>,
		cat: String,
		prefix: String,
		basePrice: Int,
		step: Int,
		make: (Double) -> Item
	) {
		for (i in 0 until 100) {
			val b = CBASE[i % 25]
			val fam = CFAM[cat]!![(i / 25) % 4]
			val idx = i + 1
			val h = (i * 137.508) % 360.0
			val it = make(h)
			when (cat) {
				"skins" -> { it.ar = b.first + " " + fam.first; it.en = fam.second + " " + b.second }
				"glows" -> { it.ar = "نور " + b.first + " " + fam.first; it.en = fam.second + " " + b.second + " Glow" }
				"trails" -> { it.ar = "أثر " + b.first + " " + fam.first; it.en = fam.second + " " + b.second + " Trail" }
				else -> { it.ar = b.first + " " + fam.first; it.en = b.second + " " + fam.second }
			}
			if (idx % 10 == 0) {
				it.gem = 12 + (idx / 10) * 4
				it.ar += " VIP"
				it.en += " VIP"
			} else {
				it.price = (((basePrice + step * idx).toDouble() / 25.0).roundToInt()) * 25
			}
			obj[prefix + idx] = it
		}
	}

	// ---------- أندية / ملاعب: 100 ملعب بألوان الأندية ----------

	private data class ClubDef(val id: String, val ar: String, val en: String, val c: Long, val c2: Long)

	private val CLUBDEFS = listOf(
		ClubDef("clubManRed", "ملعب مانشستر الأحمر", "Manchester Red Arena", 0xFFDA291C, 0xFFFBE122),
		ClubDef("clubLondonWhite", "ملعب لندن الأبيض", "London White Arena", 0xFFF5F5F5, 0xFF132257),
		ClubDef("clubLondonBlue", "ملعب لندن الأزرق", "London Blue Arena", 0xFF034694, 0xFFF0F0F0),
		ClubDef("clubLondonRed", "ملعب لندن الأحمر", "London Red Arena", 0xFFEF0107, 0xFF9C824A),
		ClubDef("clubLondonClaret", "ملعب لندن العنابي", "London Claret Arena", 0xFF7A263A, 0xFF1BB1E7),
		ClubDef("clubNewcastle", "ملعب نيوكاسل المخطط", "Newcastle Stripes Arena", 0xFF2B2B2B, 0xFFE8E8E8),
		ClubDef("clubLeeds", "ملعب ليدز الأبيض", "Leeds White Arena", 0xFFFFFFFF, 0xFF1D428A),
		ClubDef("clubMerseyBlue", "ملعب ميرسي الأزرق", "Mersey Blue Arena", 0xFF003399, 0xFFFFFFFF),
		ClubDef("clubBirmingham", "ملعب برمنجهام السماوي", "Birmingham Sky Arena", 0xFF6C1D45, 0xFF95BFE5),
		ClubDef("clubNottingham", "ملعب نوتنغهام الأحمر", "Nottingham Red Arena", 0xFFE53233, 0xFFFFFFFF),
		ClubDef("clubMadridRed", "ملعب مدريد الأحمر", "Madrid Red Arena", 0xFFCB3524, 0xFF262E62),
		ClubDef("clubSevilleWhite", "ملعب إشبيلية الأبيض", "Seville White Arena", 0xFFF7F7F7, 0xFFD81920),
		ClubDef("clubSevilleGreen", "ملعب إشبيلية الأخضر", "Seville Green Arena", 0xFF00954C, 0xFFFFFFFF),
		ClubDef("clubValencia", "ملعب فالنسيا البرتقالي", "Valencia Orange Arena", 0xFFF7931E, 0xFF1B1B1B),
		ClubDef("clubBilbao", "ملعب بلباو الأحمر", "Bilbao Red Arena", 0xFFEE2523, 0xFF1B1B1B),
		ClubDef("clubBasque", "ملعب الباسك الأزرق", "Basque Blue Arena", 0xFF0067B1, 0xFFFFFFFF),
		ClubDef("clubVigo", "ملعب فيغو السماوي", "Vigo Sky Arena", 0xFF8AC3EE, 0xFFFFFFFF),
		ClubDef("clubCordoba", "ملعب قرطبة الأصفر", "Cordoba Yellow Arena", 0xFFFFE500, 0xFF0B3F91),
		ClubDef("clubTurinMaroon", "ملعب تورينو العنابي", "Turin Maroon Arena", 0xFF7B1E24, 0xFFF0F0F0),
		ClubDef("clubRomeGold", "ملعب روما الذهبي", "Rome Gold Arena", 0xFF8E1F2F, 0xFFF0BC42),
		ClubDef("clubRomeSky", "ملعب روما السماوي", "Rome Sky Arena", 0xFF87D8F7, 0xFFFFFFFF),
		ClubDef("clubNaples", "ملعب نابولي الأزرق", "Naples Blue Arena", 0xFF12A0D7, 0xFFFFFFFF),
		ClubDef("clubFlorence", "ملعب فلورنسا البنفسجي", "Florence Purple Arena", 0xFF562883, 0xFFFFFFFF),
		ClubDef("clubGenoa", "ملعب جنوة الأحمر", "Genoa Red Arena", 0xFF9F1B27, 0xFF10375C),
		ClubDef("clubBergamo", "ملعب برجامو الأزرق", "Bergamo Blue Arena", 0xFF1B5CA8, 0xFF141414),
		ClubDef("clubSicily", "ملعب صقلية الوردي", "Sicily Pink Arena", 0xFFEE7CA8, 0xFF141414),
		ClubDef("clubLeverkusen", "ملعب ليفركوزن الأحمر", "Leverkusen Red Arena", 0xFFE32221, 0xFF141414),
		ClubDef("clubHamburg", "ملعب هامبورغ الأزرق", "Hamburg Blue Arena", 0xFF0F52A0, 0xFFFFFFFF),
		ClubDef("clubLeipzig", "ملعب لايبزيغ الأحمر", "Leipzig Red Arena", 0xFFDD0741, 0xFFF0F0F0),
		ClubDef("clubBremen", "ملعب بريمن الأخضر", "Bremen Green Arena", 0xFF1D9053, 0xFFFFFFFF),
		ClubDef("clubRhine", "ملعب الراين الأبيض", "Rhine White Arena", 0xFFFAFAFA, 0xFF00A94F),
		ClubDef("clubFrankfurt", "ملعب فرانكفورت الأسود", "Frankfurt Black Arena", 0xFF1A1A1A, 0xFFE1000F),
		ClubDef("clubStuttgart", "ملعب شتوتغارت الأبيض", "Stuttgart White Arena", 0xFFF7F7F7, 0xFFE32219),
		ClubDef("clubBerlin", "ملعب برلين الأزرق", "Berlin Blue Arena", 0xFF004E9E, 0xFFFFFFFF),
		ClubDef("clubMarseille", "ملعب مارسيليا السماوي", "Marseille Sky Arena", 0xFF2FAEE0, 0xFFFFFFFF),
		ClubDef("clubLyon", "ملعب ليون الأزرق", "Lyon Blue Arena", 0xFF1B3282, 0xFFDA291C),
		ClubDef("clubMonaco", "ملعب موناكو الأحمر", "Monaco Red Arena", 0xFFE63946, 0xFFFFFFFF),
		ClubDef("clubLille", "ملعب ليل الأحمر", "Lille Red Arena", 0xFFE01E13, 0xFF041E42),
		ClubDef("clubStEtienne", "ملعب سانت إتيان الأخضر", "Saint Etienne Green Arena", 0xFF008B4A, 0xFFFFFFFF),
		ClubDef("clubNantes", "ملعب نانت الأصفر", "Nantes Yellow Arena", 0xFFFDD100, 0xFF00954C),
		ClubDef("clubLisbonRed", "ملعب لشبونة الأحمر", "Lisbon Red Arena", 0xFFCE1126, 0xFFFFD200),
		ClubDef("clubLisbonGreen", "ملعب لشبونة الأخضر", "Lisbon Green Arena", 0xFF008C51, 0xFFFFFFFF),
		ClubDef("clubPorto", "ملعب بورتو الأزرق", "Porto Blue Arena", 0xFF00428C, 0xFFFFFFFF),
		ClubDef("clubAmsterdam", "ملعب أمستردام الأحمر", "Amsterdam Red Arena", 0xFFD2122E, 0xFFFFFFFF),
		ClubDef("clubEindhoven", "ملعب أيندهوفن الأحمر", "Eindhoven Red Arena", 0xFFED1C24, 0xFF141414),
		ClubDef("clubRotterdam", "ملعب روتردام الأبيض", "Rotterdam White Arena", 0xFFFFFFFF, 0xFF00A650),
		ClubDef("clubBruges", "ملعب بروج الأزرق", "Bruges Blue Arena", 0xFF0A4D9E, 0xFF141414),
		ClubDef("clubGlasgowGreen", "ملعب غلاسكو الأخضر", "Glasgow Green Arena", 0xFF018749, 0xFFFFFFFF),
		ClubDef("clubGlasgowBlue", "ملعب غلاسكو الأزرق", "Glasgow Blue Arena", 0xFF1B458F, 0xFFED1C24),
		ClubDef("clubIstanbulYellow", "ملعب إستانبول الأصفر", "Istanbul Yellow Arena", 0xFFFFED00, 0xFF00306B),
		ClubDef("clubIstanbulRed", "ملعب إستانبول الأحمر", "Istanbul Red Arena", 0xFFE30A17, 0xFFFDB912),
		ClubDef("clubIstanbulBlack", "ملعب إستانبول الأسود", "Istanbul Black Arena", 0xFF1B1B1B, 0xFFFFFFFF),
		ClubDef("clubAthensGreen", "ملعب أثينا الأخضر", "Athens Green Arena", 0xFF006B3F, 0xFFFFFFFF),
		ClubDef("clubAthensRed", "ملعب أثينا الأحمر", "Athens Red Arena", 0xFFD4001F, 0xFFF0F0F0),
		ClubDef("clubBelgrade", "ملعب بلغراد الأحمر", "Belgrade Red Arena", 0xFFB3202E, 0xFF141414),
		ClubDef("clubKyiv", "ملعب كييف الأزرق", "Kyiv Blue Arena", 0xFF0057B8, 0xFFFFD700),
		ClubDef("clubMoscow", "ملعب موسكو الأحمر", "Moscow Red Arena", 0xFFC8102E, 0xFF1B4E9B),
		ClubDef("clubAlexRed", "ملعب الإسكندرية الأحمر", "Alexandria Red Arena", 0xFFB3161C, 0xFFFFFFFF),
		ClubDef("clubAlexWhite", "ملعب الإسكندرية الأبيض", "Alexandria White Arena", 0xFFF7F7F7, 0xFF1B7A3D),
		ClubDef("clubIsmailia", "ملعب الإسماعيلية الأصفر", "Ismailia Yellow Arena", 0xFFF2C300, 0xFF141414),
		ClubDef("clubPortSaid", "ملعب بورسعيد الأخضر", "Port Said Green Arena", 0xFF0E7A4A, 0xFFFFFFFF),
		ClubDef("clubSuez", "ملعب السويس الأزرق", "Suez Blue Arena", 0xFF1B62A5, 0xFFFFFFFF),
		ClubDef("clubTanta", "ملعب طنطا الأحمر", "Tanta Red Arena", 0xFFC0392B, 0xFFF5C518),
		ClubDef("clubAsyut", "ملعب أسيوط البرتقالي", "Asyut Orange Arena", 0xFFE07B2A, 0xFF1B1B1B),
		ClubDef("clubRiyadhBlue", "ملعب الرياض الأزرق", "Riyadh Blue Arena", 0xFF0B5EA8, 0xFFFFFFFF),
		ClubDef("clubRiyadhYellow", "ملعب الرياض الأصفر", "Riyadh Yellow Arena", 0xFFFFD200, 0xFF0B3F91),
		ClubDef("clubJeddahGreen", "ملعب جدة الأخضر", "Jeddah Green Arena", 0xFF0E7A3C, 0xFFFFFFFF),
		ClubDef("clubJeddahRed", "ملعب جدة الأحمر", "Jeddah Red Arena", 0xFFD32F2F, 0xFF141414),
		ClubDef("clubDoha", "ملعب الدوحة العنابي", "Doha Maroon Arena", 0xFF8A1538, 0xFFFFFFFF),
		ClubDef("clubDubai", "ملعب دبي الأحمر", "Dubai Red Arena", 0xFFB71C1C, 0xFFFFD54F),
		ClubDef("clubAbuDhabi", "ملعب أبوظبي الأخضر", "Abu Dhabi Green Arena", 0xFF1B7A5A, 0xFFFFFFFF),
		ClubDef("clubKuwait", "ملعب الكويت الأزرق", "Kuwait Blue Arena", 0xFF1565C0, 0xFFFFEB3B),
		ClubDef("clubBaghdad", "ملعب بغداد الأخضر", "Baghdad Green Arena", 0xFF1B5E20, 0xFFFFC107),
		ClubDef("clubAmman", "ملعب عمان الأحمر", "Amman Red Arena", 0xFFA31621, 0xFFF0F0F0),
		ClubDef("clubCasablanca", "ملعب كازابلانكا الأحمر", "Casablanca Red Arena", 0xFFD32F2F, 0xFF1B1B1B),
		ClubDef("clubRabat", "ملعب الرباط الأخضر", "Rabat Green Arena", 0xFF12805A, 0xFFFFD200),
		ClubDef("clubTunisRed", "ملعب تونس الأحمر", "Tunis Red Arena", 0xFFE53935, 0xFFFFFFFF),
		ClubDef("clubTunisWhite", "ملعب تونس الأبيض", "Tunis White Arena", 0xFFFAFAFA, 0xFF1976D2),
		ClubDef("clubAlgiers", "ملعب الجزائر الأخضر", "Algiers Green Arena", 0xFF2E7D5B, 0xFFFFFFFF),
		ClubDef("clubTripoli", "ملعب طرابلس الأخضر", "Tripoli Green Arena", 0xFF388E3C, 0xFF141414),
		ClubDef("clubLagos", "ملعب لاغوس الأخضر", "Lagos Green Arena", 0xFF00A651, 0xFF141414),
		ClubDef("clubAccra", "ملعب أكرا الذهبي", "Accra Gold Arena", 0xFFFBC02D, 0xFFD32F2F),
		ClubDef("clubJoburg", "ملعب جوهانسبرغ الذهبي", "Johannesburg Gold Arena", 0xFFFFC107, 0xFF212121),
		ClubDef("clubBuenos", "ملعب بوينس آيرس الأزرق", "Buenos Aires Blue Arena", 0xFF124A9C, 0xFFFFD200),
		ClubDef("clubRio", "ملعب ريو الأخضر", "Rio Green Arena", 0xFF7A1725, 0xFF0B6E3F),
		ClubDef("clubTokyo", "ملعب طوكيو الأزرق", "Tokyo Blue Arena", 0xFF1C3F94, 0xFFE60012)
	)

	/** pitch tones: grass top + soil base */
	private val PITCH = listOf(
		Color(0xFF3D9B4C) to Color(0xFF24632F),
		Color(0xFF41A350) to Color(0xFF256A31),
		Color(0xFF368B42) to Color(0xFF1D5527),
		Color(0xFF44A455) to Color(0xFF215C2C)
	)

	private fun mixC(a: Color, b: Color, t: Float): Color = Color(
		red = a.red + (b.red - a.red) * t,
		green = a.green + (b.green - a.green) * t,
		blue = a.blue + (b.blue - a.blue) * t,
		alpha = 1f
	)

	/** Club stadiums: club colours + 3 kick-off moods (day / dusk / floodlit night). */
	private fun expandClubs() {
		for (i in CLUBDEFS.indices) {
			val d = CLUBDEFS[i]
			val c = Color(d.c)
			val c2 = Color(d.c2)
			val p = PITCH[i % 4]
			val mood = i % 3
			val idx = i + 1
			val top = when (mood) {
				0 -> mixC(c, Color(0xFF2C6FB5), 0.58f)
				1 -> mixC(c, Color(0xFF7A3B2E), 0.52f)
				else -> mixC(c, Color(0xFF0B1226), 0.7f)
			}
			val bot = when (mood) {
				0 -> mixC(c, Color(0xFFDCEEFF), 0.62f)
				1 -> mixC(c, Color(0xFFF3C09A), 0.55f)
				else -> mixC(c, Color(0xFF3A4470), 0.5f)
			}
			val item = Item(
				d.ar, d.en,
				c = c, c2 = c2, kind = "club",
				top = top, bot = bot,
				ground = if (mood == 2) shade(p.second, -0.2f) else p.second,
				gTop = if (mood == 2) shade(p.first, -0.16f) else p.first
			)
			// كل ملاعب الأندية بقت بالكوينز العادية - خانة الـ VIP بقت للمنتخبات بس
			item.price = (((900 + 22 * idx).toDouble() / 25.0).roundToInt()) * 25
			THEMES[d.id] = item
		}
	}

	/**
	 * ملاعب المنتخبات: دي بقت ملاعب الـ VIP (شماريخ + ليزر) وكل واحد ليه
	 * هتافه في `assets/sfx/nations/<id>.mp3` (شوف [Voices]).
	 */
	private data class NationDef(
		val id: String,
		val ar: String,
		val en: String,
		/** اختصار الاسم اللي بيتكتب جوّا الشعار. */
		val code: String,
		val c: Long,
		val c2: Long
	)

	private val NATIONDEFS = listOf(
		NationDef("natSpain", "منتخب إسبانيا", "Spain", "ESP", 0xFFC60B1E, 0xFF1D3A8F),
		NationDef("natArgentina", "منتخب الأرجنتين", "Argentina", "ARG", 0xFF75AADB, 0xFFFFFFFF),
		NationDef("natBrazil", "منتخب البرازيل", "Brazil", "BRA", 0xFFFFDF00, 0xFF009C3B),
		NationDef("natSenegal", "منتخب السنغال", "Senegal", "SEN", 0xFF00853F, 0xFFFDEF42),
		NationDef("natSweden", "منتخب السويد", "Sweden", "SWE", 0xFFFECC02, 0xFF005293),
		NationDef("natGermany", "منتخب ألمانيا", "Germany", "GER", 0xFFF2F2F2, 0xFF1A1A1A),
		NationDef("natMorocco", "منتخب المغرب", "Morocco", "MAR", 0xFFC1272D, 0xFF006233),
		NationDef("natMexico", "منتخب المكسيك", "Mexico", "MEX", 0xFF006847, 0xFFCE1126),
		NationDef("natNorway", "منتخب النرويج", "Norway", "NOR", 0xFFEF2B2D, 0xFF002868),
		NationDef("natJapan", "منتخب اليابان", "Japan", "JPN", 0xFF1B2E83, 0xFFFFFFFF),
		NationDef("natUsa", "منتخب أمريكا", "USA", "USA", 0xFFF2F2F2, 0xFF0A3161),
		NationDef("natEngland", "منتخب إنجلترا", "England", "ENG", 0xFFF7F7F7, 0xFFCE1124),
		NationDef("natUruguay", "منتخب أوروجواي", "Uruguay", "URU", 0xFF56A0D3, 0xFF1A1A1A),
		NationDef("natItaly", "منتخب إيطاليا", "Italy", "ITA", 0xFF0066A6, 0xFFFFFFFF),
		NationDef("natPortugal", "منتخب البرتغال", "Portugal", "POR", 0xFFDA291C, 0xFF006233),
		NationDef("natFrance", "منتخب فرنسا", "France", "FRA", 0xFF002395, 0xFFFFFFFF),
		NationDef("natIvory", "منتخب كوت ديفوار", "Ivory Coast", "CIV", 0xFFF77F00, 0xFF009E60),
		NationDef("natKorea", "منتخب كوريا الجنوبية", "South Korea", "KOR", 0xFFCD2E3A, 0xFF0047A0),
		NationDef("natEgypt", "منتخب مصر", "Egypt", "EGY", 0xFFCE1126, 0xFFFFFFFF),
		NationDef("natNetherlands", "منتخب هولندا", "Netherlands", "NED", 0xFFEE7C00, 0xFF21468B)
	)

	/** ملعب منتخب: ألوان الفانلة + جو ماتش كبير تحت الكشافات. */
	private fun expandNations() {
		for (i in NATIONDEFS.indices) {
			val d = NATIONDEFS[i]
			val c = Color(d.c)
			val c2 = Color(d.c2)
			val p = PITCH[i % 4]
			THEMES[d.id] = Item(
				d.ar, d.en,
				c = c, c2 = c2, kind = "club",
				top = mixC(c, Color(0xFF0B1226), 0.66f),
				bot = mixC(c, Color(0xFFDCEEFF), 0.58f),
				ground = shade(p.second, -0.14f),
				gTop = shade(p.first, -0.1f),
				gem = 40 + i * 2,
				grp = "nations"
			)
			CRESTS[d.id] = d.code
		}
	}

	// ---------- ملاعب الأندية VIP ----------

	/**
	 * ملاعب الأندية VIP: خانة لوحدها بالجواهر (شماريخ + ليزر + شعار)
	 * وكل نادي ليه هتافه في `assets/sfx/arenas/<id>.mp3` (شوف [Voices]).
	 */
	private data class VipClubDef(
		val id: String,
		val ar: String,
		val en: String,
		val code: String,
		val c: Long,
		val c2: Long
	)

	private val VIPCLUBDEFS = listOf(
		VipClubDef("vipAtletico", "ملعب أتلتيكو مدريد", "Atletico Madrid", "ATM", 0xFFCB3524, 0xFF1B2C57),
		VipClubDef("vipArsenal", "ملعب أرسنال", "Arsenal", "ARS", 0xFFEF0107, 0xFF9C824A),
		VipClubDef("vipIttihad", "ملعب الاتحاد", "Al Ittihad", "ITT", 0xFFF7D200, 0xFF141414),
		VipClubDef("vipAhly", "ملعب الأهلي", "Al Ahly", "AHL", 0xFFD8232A, 0xFFFFFFFF),
		VipClubDef("vipZamalek", "ملعب الزمالك", "Zamalek", "ZAM", 0xFFF7F7F7, 0xFFD8232A),
		VipClubDef("vipNassr", "ملعب النصر", "Al Nassr", "NSR", 0xFFF7D200, 0xFF0B4EA2),
		VipClubDef("vipHilal", "ملعب الهلال", "Al Hilal", "HIL", 0xFF0B4EA2, 0xFFFFFFFF),
		VipClubDef("vipInter", "ملعب إنتر ميلان", "Inter Milan", "INT", 0xFF0B2073, 0xFF121212),
		VipClubDef("vipMilan", "ملعب ميلان", "AC Milan", "ACM", 0xFFCC1F2B, 0xFF1A1A1A),
		VipClubDef("vipParis", "ملعب باريس", "Paris", "PSG", 0xFF0B2A5E, 0xFFE02020),
		VipClubDef("vipBayern", "ملعب بايرن ميونخ", "Bayern Munich", "BAY", 0xFFDC052D, 0xFF0066B2),
		VipClubDef("vipBarca", "ملعب برشلونة", "Barcelona", "BAR", 0xFF0B3B8C, 0xFF8E1B3D),
		VipClubDef("vipDortmund", "ملعب دورتموند", "Dortmund", "BVB", 0xFFF7DF16, 0xFF141414),
		VipClubDef("vipChelsea", "ملعب تشيلسي", "Chelsea", "CHE", 0xFF034694, 0xFFF0F0F0),
		VipClubDef("vipTottenham", "ملعب توتنهام", "Tottenham", "TOT", 0xFFF5F5F5, 0xFF132257),
		VipClubDef("vipReal", "ملعب ريال مدريد", "Real Madrid", "RMA", 0xFFF5F5F5, 0xFFD9A833),
		VipClubDef("vipLiverpool", "ملعب ليفربول", "Liverpool", "LFC", 0xFFC8102E, 0xFF00B2A9),
		VipClubDef("vipManCity", "ملعب مانشستر سيتي", "Man City", "MCI", 0xFF6CABDD, 0xFF1C2C5B),
		VipClubDef("vipManUnited", "ملعب مانشستر يونايتد", "Man United", "MUN", 0xFFDA291C, 0xFFFBE122),
		VipClubDef("vipJuventus", "ملعب يوفنتوس", "Juventus", "JUV", 0xFF1B1B1B, 0xFFF2F2F2)
	)

	/** ملعب نادي VIP: ألوان الفانلة + جو ماتش ليلي تحت الكشافات. */
	private fun expandVipClubs() {
		for (i in VIPCLUBDEFS.indices) {
			val d = VIPCLUBDEFS[i]
			val c = Color(d.c)
			val c2 = Color(d.c2)
			val p = PITCH[i % 4]
			THEMES[d.id] = Item(
				d.ar, d.en,
				c = c, c2 = c2, kind = "club",
				top = mixC(c, Color(0xFF0B1226), 0.7f),
				bot = mixC(c, Color(0xFFDCEEFF), 0.6f),
				ground = shade(p.second, -0.14f),
				gTop = shade(p.first, -0.1f),
				gem = 45 + i * 2,
				grp = "clubsVip"
			)
			CRESTS[d.id] = d.code
		}
	}

	// ---------- شعارات الملاعب ----------

	private val CRESTS = HashMap<String, String>()

	/** اختصار اسم النادي / المنتخب اللي بيترسم جوّا الشعار، null لو الملعب مفيش له شعار. */
	fun crestCode(themeId: String?): String? {
		if (themeId == null) return null
		return CRESTS[themeId]
	}

	init {
		expandCat(SKINS, "skins", "sk", 200, 38) { h -> Item("", "", c = hsl2c(h, 78.0, 58.0), c2 = hsl2c(h, 72.0, 38.0)) }
		expandCat(GLOWS, "glows", "gl", 260, 42) { h -> Item("", "", c = hsl2c(h, 92.0, 62.0)) }
		expandCat(TRAILS, "trails", "tr", 240, 40) { h -> Item("", "", c = hsl2c(h, 88.0, 60.0)) }
		expandClubs()
		expandNations()
		expandVipClubs()
		expandCat(THEMES, "themes", "th", 300, 46) { h ->
			Item(
				"", "",
				top = hsl2c(h, 58.0, 52.0),
				bot = hsl2c(h, 62.0, 78.0),
				ground = hsl2c(h, 45.0, 30.0),
				gTop = hsl2c(h, 50.0, 42.0)
			)
		}
	}

	fun cat(name: String): LinkedHashMap<String, Item> = when (name) {
		"skins" -> SKINS
		"glows" -> GLOWS
		"trails" -> TRAILS
		else -> THEMES
	}

	// ---------- خانات المتجر: عادي / مشهور / VIP ----------

	/** أي حاجة سعرها جواهر = VIP. */
	fun isVip(it: Item): Boolean = it.gem != null

	private fun pickThemes(pred: (Item) -> Boolean): LinkedHashMap<String, Item> {
		val out = LinkedHashMap<String, Item>()
		for ((k, v) in THEMES) if (pred(v)) out[k] = v
		return out
	}

	/** ثيمات عادية بالكوينز (مش ملاعب ومش VIP). */
	val THEMES_PLAIN: LinkedHashMap<String, Item> by lazy { pickThemes { it.kind != "club" && !isVip(it) } }

	/** ثيمات VIP لوحدها. */
	val THEMES_VIP: LinkedHashMap<String, Item> by lazy { pickThemes { it.kind != "club" && isVip(it) } }

	/** الملاعب المشهورة (ألوان الأندية) بالكوينز - لوحدها. */
	val ARENAS: LinkedHashMap<String, Item> by lazy { pickThemes { it.kind == "club" && !isVip(it) } }

	/** ملاعب VIP - دول بس اللي فيهم شماريخ. */
	val ARENAS_VIP: LinkedHashMap<String, Item> by lazy {
		pickThemes { it.kind == "club" && isVip(it) && it.grp != "clubsVip" }
	}

	/** ملاعب الأندية VIP - خانة لوحدها. */
	val ARENAS_CLUB_VIP: LinkedHashMap<String, Item> by lazy {
		pickThemes { it.kind == "club" && isVip(it) && it.grp == "clubsVip" }
	}

	/** خانة المتجر -> الكتالوج بتاعها. */
	fun tabMap(tab: String): LinkedHashMap<String, Item> = when (tab) {
		"skins" -> SKINS
		"glows" -> GLOWS
		"trails" -> TRAILS
		"themes" -> THEMES_PLAIN
		"themesVip" -> THEMES_VIP
		"arenas" -> ARENAS
		"arenasVip" -> ARENAS_VIP
		"arenasClubVip" -> ARENAS_CLUB_VIP
		else -> THEMES
	}

	/**
	 * خانة المتجر -> نوع الملكية في الحفظ. الملاعب والثيمات كلها بتتحفظ تحت
	 * "themes" زي الأول، فأي حاجة اتشترت قبل كده تفضل مشتراة بعد التقسيم.
	 */
	fun tabOwnCat(tab: String): String = when (tab) {
		"skins" -> "skins"
		"glows" -> "glows"
		"trails" -> "trails"
		else -> "themes"
	}

	/** الشماريخ بتضرب في ملاعب VIP بس. */
	fun hasFlares(themeId: String?): Boolean {
		val it = THEMES[themeId ?: return false] ?: return false
		return it.kind == "club" && isVip(it)
	}

	// ---------- perks ----------

	val PERKS = listOf(
		Perk("doubler", "مضاعف الكوينز", "Coin Doubler", "×2 كوينز للأبد", "×2 coins forever", 2500, "x2"),
		Perk("lucky", "الحظ السعيد", "Lucky Charm", "+30% كوينز إضافي", "+30% extra coins", 3500, "coin"),
		Perk("headstart", "بداية قوية", "Head Start", "درع مجاني كل جولة", "Free shield each run", 3000, "target"),
		Perk("magnetpro", "مغناطيس برو", "Magnet Pro", "مدة أطول 60%", "60% longer magnet", 2000, "puMagnet"),
		Perk("shieldpro", "درع برو", "Shield Pro", "مدة أطول 50%", "50% longer shield", 2200, "puShield"),
		Perk("slowpro", "بطء برو", "Slow Pro", "مدة أطول 60%", "60% longer slow-mo", 1800, "puClock"),
		// NOTE: the original references IC.medal which does not exist, so the game
		// falls back to the star glyph. Kept identical on purpose.
		Perk("saver", "خصم الإحياء", "Revive Saver", "إحياء أرخص 40%", "40% cheaper revive", 4000, null)
	)

	val COINPACKS = listOf(
		CoinPack("cp1", 2000, 10),
		CoinPack("cp2", 6000, 25),
		CoinPack("cp3", 20000, 60)
	)

	// ---------- missions ----------

	val MISSION_POOL = listOf(
		MissionDef("score50", "اوصل 50 نقطة", "Reach 50 score", 50, 100, "score"),
		MissionDef("score100", "اوصل 100 نقطة", "Reach 100 score", 100, 200, "score"),
		MissionDef("score150", "اوصل 150 نقطة", "Reach 150 score", 150, 300, "score"),
		MissionDef("coins30", "اجمع 30 كوين", "Collect 30 coins", 30, 120, "coins"),
		MissionDef("coins60", "اجمع 60 كوين", "Collect 60 coins", 60, 200, "coins"),
		MissionDef("coins100", "اجمع 100 كوين", "Collect 100 coins", 100, 320, "coins"),
		MissionDef("runs3", "العب 3 جولات", "Play 3 runs", 3, 80, "runs"),
		MissionDef("runs5", "العب 5 جولات", "Play 5 runs", 5, 150, "runs"),
		MissionDef("runs8", "العب 8 جولات", "Play 8 runs", 8, 250, "runs"),
		MissionDef("jumps50", "انط 50 مرة", "Jump 50 times", 50, 100, "jumps"),
		MissionDef("jumps100", "انط 100 مرة", "Jump 100 times", 100, 200, "jumps"),
		MissionDef("combo3", "كومبو 3", "Combo of 3", 3, 150, "combo"),
		MissionDef("combo5", "كومبو 5", "Combo of 5", 5, 280, "combo")
	)

	val ACH_TRACKS = listOf(
		AchTrack("score", "محترف النقاط", "Score Master", 50, 2.0, 200, 1.6, "best"),
		AchTrack("coins", "جامع الكوينز", "Coin Collector", 500, 2.0, 150, 1.6, "totalCoins"),
		AchTrack("runs", "مدمن لعب", "Dedicated", 5, 2.0, 100, 1.5, "runs"),
		AchTrack("jumps", "ملك القفز", "Jump King", 100, 2.0, 120, 1.5, "jumps"),
		AchTrack("combo", "ملك الكومبو", "Combo King", 3, 1.6, 150, 1.7, "maxCombo")
	)

	fun achGoalAt(t: AchTrack, lvl: Int): Int = (t.base * t.mult.pow(lvl)).roundToInt()
	fun achRewardAt(t: AchTrack, lvl: Int): Int = (t.rw * t.rwMult.pow(lvl)).roundToInt()

	// ---------- worlds ----------

	val WORLDS = listOf(
		null,
		World("sunset", "الغروب", "Sunset", Color(0xFFFF7E5F), Color(0xFFFEB47B), Color(0xFFA84860), Color(0xFFC86078), 1.0f, -18f, 1.03f),
		World("forest", "الغابة", "Forest", Color(0xFF2D6A4F), Color(0xFF74C69D), Color(0xFF1B4332), Color(0xFF2D6A4F), 1.06f, -18f, 1.0f),
		World("night", "الليل", "Night", Color(0xFF1A1A3E), Color(0xFF3A3560), Color(0xFF12122A), Color(0xFF22224A), 1.0f, -18f, 1.07f),
		World("desert", "الصحراء", "Desert", Color(0xFFE9C46A), Color(0xFFF4E4BC), Color(0xFFC8974A), Color(0xFFE0B060), 1.0f, -18f, 1.15f),
		World("space", "الفضاء", "Space", Color(0xFF0F0524), Color(0xFF2D1B52), Color(0xFF1A0F30), Color(0xFF2A1A4A), 0.64f, -15f, 0.94f),
		World("ocean", "المحيط", "Ocean", Color(0xFF0A3D62), Color(0xFF3C9DD0), Color(0xFF062C47), Color(0xFF0A4A6E), 0.72f, -14f, 0.85f)
	)
}
