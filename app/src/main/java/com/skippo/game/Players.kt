package com.skippo.game

import androidx.compose.ui.graphics.Color

/**
 * A playable footballer.
 *
 * The artwork lives in `assets/players/<id>.webp` as one horizontal strip of
 * [frames] cells, each [fw] x [fh] pixels. [c] / [c2] are the kit colours,
 * sampled off the shirt, and stand in for what the old cube skin used to give
 * us (death particles, leaderboard dot).
 */
data class PlayerDef(
	val id: String,
	val ar: String,
	val en: String,
	val price: Int? = null,
	val gem: Int? = null,
	val frames: Int,
	val fw: Int,
	val fh: Int,
	val c: Color,
	val c2: Color
) {
	fun name(lang: String) = if (lang == "ar") ar else en

	val asset: String get() = "players/$id.webp"

	/** Aspect of a single cell - used to size the runner on screen. */
	val aspect: Float get() = fw.toFloat() / fh.toFloat()

	val isFree: Boolean get() = gem == null && (price ?: 0) <= 0
}

/**
 * Every runner in the game, in shop order: the free starter, then the coin
 * players cheapest first, then the gem-only legends.
 */
object Players {

	/** Handed out for free, and what a fresh save runs with. */
	const val DEFAULT = "garcia"

	val ALL: LinkedHashMap<String, PlayerDef> = linkedMapOf(
		"garcia" to PlayerDef("garcia", "جارسيا", "Garcia", price = 0, frames = 12, fw = 81, fh = 176, c = Color(0xFF9D1129), c2 = Color(0xFF5C0A18)),
		"hamza" to PlayerDef("hamza", "حمزة", "Hamza", price = 1200, frames = 14, fw = 97, fh = 176, c = Color(0xFFCA1E28), c2 = Color(0xFF771217)),
		"ashour" to PlayerDef("ashour", "عاشور", "Ashour", price = 1300, frames = 12, fw = 88, fh = 176, c = Color(0xFFCC1018), c2 = Color(0xFF78090E)),
		"mido" to PlayerDef("mido", "ميدو", "Mido", price = 1600, frames = 12, fw = 94, fh = 176, c = Color(0xFFCA1E24), c2 = Color(0xFF771115)),
		"zidan" to PlayerDef("zidan", "محمد زيدان", "Mohamed Zidan", price = 1600, frames = 16, fw = 90, fh = 176, c = Color(0xFFC8151D), c2 = Color(0xFF750C11)),
		"trezeguet" to PlayerDef("trezeguet", "محمود تريزيجيه", "Trezeguet", price = 1700, frames = 12, fw = 96, fh = 176, c = Color(0xFFDF5047), c2 = Color(0xFF832F2A)),
		"ahmedhassan" to PlayerDef("ahmedhassan", "أحمد حسن", "Ahmed Hassan", price = 1800, frames = 12, fw = 90, fh = 176, c = Color(0xFFC2131B), c2 = Color(0xFF720B10)),
		"aldawsari" to PlayerDef("aldawsari", "سالم الدوسري", "Al-Dawsari", price = 1800, frames = 12, fw = 89, fh = 176, c = Color(0xFF317E5A), c2 = Color(0xFF1D4A35)),
		"bounou" to PlayerDef("bounou", "ياسين بونو", "Bounou", price = 1900, frames = 16, fw = 100, fh = 169, c = Color(0xFF12974C), c2 = Color(0xFF0A582C)),
		"hossamhassan" to PlayerDef("hossamhassan", "حسام حسن", "Hossam Hassan", price = 1900, frames = 12, fw = 94, fh = 176, c = Color(0xFFC41C21), c2 = Color(0xFF731013)),
		"kudus" to PlayerDef("kudus", "محمد كودوس", "Kudus", price = 1900, frames = 10, fw = 87, fh = 176, c = Color(0xFF542C1B), c2 = Color(0xFF311910)),
		"rice" to PlayerDef("rice", "ديكلان رايس", "Declan Rice", price = 2000, frames = 12, fw = 100, fh = 176, c = Color(0xFFF5B99F), c2 = Color(0xFF906C5D)),
		"elhadary" to PlayerDef("elhadary", "عصام الحضري", "El Hadary", price = 2000, frames = 12, fw = 113, fh = 176, c = Color(0xFFF7DB15), c2 = Color(0xFF91810C)),
		"marmoush" to PlayerDef("marmoush", "عمر مرموش", "Marmoush", price = 2000, frames = 12, fw = 100, fh = 176, c = Color(0xFFD21F29), c2 = Color(0xFF7B1218)),
		"ziyech" to PlayerDef("ziyech", "حكيم زياش", "Ziyech", price = 2000, frames = 12, fw = 121, fh = 176, c = Color(0xFFE72932), c2 = Color(0xFF87181D)),
		"enzo" to PlayerDef("enzo", "إنزو فيرنانديز", "Enzo Fernandez", price = 2100, frames = 12, fw = 99, fh = 176, c = Color(0xFFF6BB96), c2 = Color(0xFF916E58)),
		"nwilliams" to PlayerDef("nwilliams", "نيكو ويليامز", "N. Williams", price = 2100, frames = 12, fw = 93, fh = 176, c = Color(0xFF951228), c2 = Color(0xFF570B17)),
		"raphinha" to PlayerDef("raphinha", "رافينيا", "Raphinha", price = 2100, frames = 12, fw = 93, fh = 176, c = Color(0xFFF3DA22), c2 = Color(0xFF8F8014)),
		"tchouameni" to PlayerDef("tchouameni", "تشواميني", "Tchouameni", price = 2100, frames = 16, fw = 76, fh = 176, c = Color(0xFF2B3A67), c2 = Color(0xFF19223C)),
		"bernardo" to PlayerDef("bernardo", "برناردو سيلفا", "Bernardo Silva", price = 2200, frames = 12, fw = 93, fh = 176, c = Color(0xFF7E1428), c2 = Color(0xFF4A0B17)),
		"dembele" to PlayerDef("dembele", "عثمان ديمبيلي", "Dembele", price = 2200, frames = 12, fw = 92, fh = 176, c = Color(0xFF2A3A6E), c2 = Color(0xFF182241)),
		"gavi" to PlayerDef("gavi", "جافي", "Gavi", price = 2200, frames = 12, fw = 127, fh = 176, c = Color(0xFFCA162B), c2 = Color(0xFF770D19)),
		"hakimi" to PlayerDef("hakimi", "أشرف حكيمي", "Hakimi", price = 2200, frames = 12, fw = 93, fh = 176, c = Color(0xFFBE2229), c2 = Color(0xFF6F1418)),
		"palmer" to PlayerDef("palmer", "كول بالمر", "Palmer", price = 2200, frames = 16, fw = 88, fh = 176, c = Color(0xFFFAC2AC), c2 = Color(0xFF937265)),
		"trent" to PlayerDef("trent", "ترينت أرنولد", "Trent", price = 2200, frames = 12, fw = 98, fh = 176, c = Color(0xFFC3CADD), c2 = Color(0xFF737782)),
		"wirtz" to PlayerDef("wirtz", "فلوريان فيرتز", "Wirtz", price = 2200, frames = 14, fw = 102, fh = 176, c = Color(0xFFF8C09C), c2 = Color(0xFF91715B)),
		"casemiro" to PlayerDef("casemiro", "كاسيميرو", "Casemiro", price = 2300, frames = 12, fw = 82, fh = 176, c = Color(0xFFF2D726), c2 = Color(0xFF8E7F16)),
		"dimaria" to PlayerDef("dimaria", "أنخيل دي ماريا", "Di Maria", price = 2300, frames = 12, fw = 118, fh = 176, c = Color(0xFFB6E0EF), c2 = Color(0xFF6B848C)),
		"donnarumma" to PlayerDef("donnarumma", "دوناروما", "Donnarumma", price = 2300, frames = 12, fw = 84, fh = 176, c = Color(0xFFA1CA76), c2 = Color(0xFF5F7745)),
		"emartinez" to PlayerDef("emartinez", "إيميليانو مارتينيز", "E. Martinez", price = 2300, frames = 16, fw = 100, fh = 168, c = Color(0xFF0A5C66), c2 = Color(0xFF06363C)),
		"alvarez" to PlayerDef("alvarez", "خوليان ألفاريز", "J. Alvarez", price = 2300, frames = 12, fw = 116, fh = 176, c = Color(0xFF80B5D6), c2 = Color(0xFF4B6A7E)),
		"mahrez" to PlayerDef("mahrez", "رياض محرز", "Mahrez", price = 2300, frames = 12, fw = 100, fh = 176, c = Color(0xFF0F934E), c2 = Color(0xFF08562E)),
		"rodrygo" to PlayerDef("rodrygo", "رودريجو", "Rodrygo", price = 2300, frames = 12, fw = 94, fh = 176, c = Color(0xFFE7CE09), c2 = Color(0xFF887905)),
		"bruno" to PlayerDef("bruno", "برونو فيرنانديز", "Bruno F.", price = 2400, frames = 12, fw = 91, fh = 176, c = Color(0xFF871A2E), c2 = Color(0xFF4F0F1B)),
		"kimmich" to PlayerDef("kimmich", "جوشوا كيميش", "Kimmich", price = 2400, frames = 10, fw = 92, fh = 176, c = Color(0xFFE7ECF0), c2 = Color(0xFF888A8D)),
		"lautaro" to PlayerDef("lautaro", "لاوتارو مارتينيز", "Lautaro", price = 2400, frames = 16, fw = 96, fh = 176, c = Color(0xFFCDEDF9), c2 = Color(0xFF788B92)),
		"oblak" to PlayerDef("oblak", "يان أوبلاك", "Oblak", price = 2400, frames = 10, fw = 106, fh = 176, c = Color(0xFF22AD4A), c2 = Color(0xFF14652C)),
		"osimhen" to PlayerDef("osimhen", "فيكتور أوسيمين", "Osimhen", price = 2400, frames = 12, fw = 78, fh = 176, c = Color(0xFF158B53), c2 = Color(0xFF0C5131)),
		"milla" to PlayerDef("milla", "روجيه ميلا", "Roger Milla", price = 2400, frames = 12, fw = 89, fh = 176, c = Color(0xFF1A6E43), c2 = Color(0xFF0F4027)),
		"saka" to PlayerDef("saka", "بوكايو ساكا", "Saka", price = 2400, frames = 12, fw = 121, fh = 176, c = Color(0xFFCDD9E6), c2 = Color(0xFF787F87)),
		"griezmann" to PlayerDef("griezmann", "أنطوان جريزمان", "Griezmann", price = 2500, frames = 12, fw = 118, fh = 176, c = Color(0xFF1A489A), c2 = Color(0xFF0F2A5A)),
		"son" to PlayerDef("son", "سون هيونج مين", "Son", price = 2500, frames = 16, fw = 91, fh = 176, c = Color(0xFFCA202C), c2 = Color(0xFF77131A)),
		"alisson" to PlayerDef("alisson", "أليسون", "Alisson", price = 2600, frames = 16, fw = 97, fh = 176, c = Color(0xFF1CA24D), c2 = Color(0xFF105F2D)),
		"courtois" to PlayerDef("courtois", "تيبو كورتوا", "Courtois", price = 2600, frames = 12, fw = 98, fh = 176, c = Color(0xFF11A19D), c2 = Color(0xFF0A5F5C)),
		"foden" to PlayerDef("foden", "فيل فودين", "Foden", price = 2600, frames = 12, fw = 120, fh = 176, c = Color(0xFFF8B79D), c2 = Color(0xFF926B5C)),
		"kvara" to PlayerDef("kvara", "كفاراتسخيليا", "Kvaratskhelia", price = 2600, frames = 12, fw = 123, fh = 176, c = Color(0xFFF9B9A0), c2 = Color(0xFF926C5E)),
		"musiala" to PlayerDef("musiala", "جمال موسيالا", "Musiala", price = 2600, frames = 12, fw = 110, fh = 176, c = Color(0xFFCAD4DB), c2 = Color(0xFF777C81)),
		"ramos" to PlayerDef("ramos", "سيرجيو راموس", "Ramos", price = 2600, frames = 12, fw = 93, fh = 176, c = Color(0xFFBE161D), c2 = Color(0xFF700D11)),
		"gullit" to PlayerDef("gullit", "رود خوليت", "Gullit", price = 2700, frames = 12, fw = 108, fh = 176, c = Color(0xFFD26617), c2 = Color(0xFF7C3C0D)),
		"kahn" to PlayerDef("kahn", "أوليفر كان", "Kahn", price = 2700, frames = 12, fw = 86, fh = 176, c = Color(0xFFCDF529), c2 = Color(0xFF789018)),
		"matthaus" to PlayerDef("matthaus", "لوثار ماتيوس", "Matthaus", price = 2700, frames = 16, fw = 85, fh = 176, c = Color(0xFFF7BF99), c2 = Color(0xFF91705A)),
		"rodri" to PlayerDef("rodri", "رودري", "Rodri", price = 2700, frames = 16, fw = 94, fh = 176, c = Color(0xFFCE0F2B), c2 = Color(0xFF790919)),
		"vandijk" to PlayerDef("vandijk", "فيرجيل فان دايك", "Van Dijk", price = 2700, frames = 12, fw = 112, fh = 176, c = Color(0xFFE97A32), c2 = Color(0xFF89481D)),
		"weah" to PlayerDef("weah", "جورج وياه", "Weah", price = 2700, frames = 12, fw = 91, fh = 176, c = Color(0xFF0F5BA0), c2 = Color(0xFF09355E)),
		"casillas" to PlayerDef("casillas", "إيكر كاسياس", "Casillas", price = 2800, frames = 12, fw = 83, fh = 176, c = Color(0xFF7C681C), c2 = Color(0xFF493D10)),
		"delpiero" to PlayerDef("delpiero", "أليساندرو دل بييرو", "Del Piero", price = 2800, frames = 12, fw = 104, fh = 176, c = Color(0xFF1353A0), c2 = Color(0xFF0B315E)),
		"etoo" to PlayerDef("etoo", "صامويل إيتو", "Eto'o", price = 2800, frames = 16, fw = 97, fh = 176, c = Color(0xFF157344), c2 = Color(0xFF0C4428)),
		"neuer" to PlayerDef("neuer", "مانويل نوير", "Neuer", price = 2800, frames = 16, fw = 100, fh = 171, c = Color(0xFF315C67), c2 = Color(0xFF1D363C)),
		"pirlo" to PlayerDef("pirlo", "أندريا بيرلو", "Pirlo", price = 2800, frames = 12, fw = 83, fh = 176, c = Color(0xFF1D58A8), c2 = Color(0xFF113362)),
		"raul" to PlayerDef("raul", "راؤول", "Raul", price = 2800, frames = 12, fw = 83, fh = 176, c = Color(0xFFA11A2D), c2 = Color(0xFF5F0F1A)),
		"rivaldo" to PlayerDef("rivaldo", "ريفالدو", "Rivaldo", price = 2800, frames = 10, fw = 86, fh = 176, c = Color(0xFFF0D625), c2 = Color(0xFF8D7E15)),
		"suarez" to PlayerDef("suarez", "لويس سواريز", "Suarez", price = 2800, frames = 14, fw = 100, fh = 176, c = Color(0xFF53A8DB), c2 = Color(0xFF316381)),
		"totti" to PlayerDef("totti", "فرانشيسكو توتي", "Totti", price = 2800, frames = 12, fw = 91, fh = 176, c = Color(0xFF216BA3), c2 = Color(0xFF133F60)),
		"buffon" to PlayerDef("buffon", "جيانلويجي بوفون", "Buffon", price = 2900, frames = 10, fw = 93, fh = 176, c = Color(0xFF4D6C81), c2 = Color(0xFF2D3F4C)),
		"drogba" to PlayerDef("drogba", "ديدييه دروجبا", "Drogba", price = 2900, frames = 12, fw = 86, fh = 176, c = Color(0xFFF1952A), c2 = Color(0xFF8E5818)),
		"gerrard" to PlayerDef("gerrard", "ستيفن جيرارد", "Gerrard", price = 2900, frames = 12, fw = 95, fh = 176, c = Color(0xFFF7BA96), c2 = Color(0xFF916D58)),
		"kane" to PlayerDef("kane", "هاري كين", "Kane", price = 2900, frames = 16, fw = 99, fh = 176, c = Color(0xFFF4BF9B), c2 = Color(0xFF90705B)),
		"xavi" to PlayerDef("xavi", "تشافي", "Xavi", price = 2900, frames = 12, fw = 92, fh = 176, c = Color(0xFFC2212B), c2 = Color(0xFF721319)),
		"beckham" to PlayerDef("beckham", "ديفيد بيكهام", "Beckham", price = 3000, frames = 12, fw = 84, fh = 176, c = Color(0xFFF3C4A3), c2 = Color(0xFF8F7360)),
		"ibrahimovic" to PlayerDef("ibrahimovic", "زلاتان إبراهيموفيتش", "Ibrahimovic", price = 3000, frames = 14, fw = 107, fh = 176, c = Color(0xFFF4D21D), c2 = Color(0xFF8F7B11)),
		"iniesta" to PlayerDef("iniesta", "أندريس إنييستا", "Iniesta", price = 3000, frames = 12, fw = 91, fh = 176, c = Color(0xFFBA1C23), c2 = Color(0xFF6D1014)),
		"kaka" to PlayerDef("kaka", "كاكا", "Kaka", price = 3000, frames = 12, fw = 93, fh = 176, c = Color(0xFFF1CF2E), c2 = Color(0xFF8E7A1B)),
		"modric" to PlayerDef("modric", "لوكا مودريتش", "Modric", price = 3000, frames = 16, fw = 87, fh = 176, c = Color(0xFFE33038), c2 = Color(0xFF851C21)),
		"debruyne" to PlayerDef("debruyne", "كيفين دي بروين", "De Bruyne", price = 3100, frames = 16, fw = 85, fh = 176, c = Color(0xFFBC1921), c2 = Color(0xFF6F0E13)),
		"henry" to PlayerDef("henry", "تيري هنري", "Henry", price = 3100, frames = 12, fw = 88, fh = 176, c = Color(0xFF194592), c2 = Color(0xFF0F2956)),
		"lewa" to PlayerDef("lewa", "ليفاندوفسكي", "Lewandowski", price = 3200, frames = 16, fw = 90, fh = 176, c = Color(0xFFAE222B), c2 = Color(0xFF661419)),
		"figo" to PlayerDef("figo", "لويس فيجو", "Figo", price = 3300, frames = 12, fw = 81, fh = 176, c = Color(0xFF801827), c2 = Color(0xFF4B0E17)),
		"robertocarlos" to PlayerDef("robertocarlos", "روبرتو كارلوس", "Roberto Carlos", price = 3350, frames = 12, fw = 104, fh = 176, c = Color(0xFFF5DA18), c2 = Color(0xFF90800E)),
		"baggio" to PlayerDef("baggio", "روبرتو باجيو", "Baggio", price = 3400, frames = 12, fw = 80, fh = 176, c = Color(0xFF0E52A4), c2 = Color(0xFF083061)),
		"platini" to PlayerDef("platini", "ميشيل بلاتيني", "Platini", price = 3450, frames = 12, fw = 85, fh = 176, c = Color(0xFF2F3C6A), c2 = Color(0xFF1B233E)),
		"romario" to PlayerDef("romario", "روماريو", "Romario", price = 3500, frames = 12, fw = 84, fh = 176, c = Color(0xFFF4D923), c2 = Color(0xFF8F7F14)),
		"gerdmuller" to PlayerDef("gerdmuller", "جيرد مولر", "Gerd Muller", price = 3600, frames = 12, fw = 89, fh = 176, c = Color(0xFFF3BB99), c2 = Color(0xFF8F6E5A)),
		"maldini" to PlayerDef("maldini", "باولو مالديني", "Maldini", price = 3650, frames = 12, fw = 77, fh = 176, c = Color(0xFF1E54A8), c2 = Color(0xFF113163)),
		"vanbasten" to PlayerDef("vanbasten", "ماركو فان باستن", "Van Basten", price = 3700, frames = 12, fw = 92, fh = 176, c = Color(0xFFE6843C), c2 = Color(0xFF874E23)),
		"bellingham" to PlayerDef("bellingham", "جود بيلينجهام", "Bellingham", gem = 45, frames = 16, fw = 100, fh = 176, c = Color(0xFFCDD9E7), c2 = Color(0xFF787F88)),
		"neymar" to PlayerDef("neymar", "نيمار", "Neymar", gem = 45, frames = 12, fw = 85, fh = 176, c = Color(0xFFF3DA2A), c2 = Color(0xFF8F8019)),
		"vini" to PlayerDef("vini", "فينيسيوس جونيور", "Vinicius Jr", price = 3800, frames = 16, fw = 96, fh = 176, c = Color(0xFFF2E245), c2 = Color(0xFF8E8528)),
		"aboutrika" to PlayerDef("aboutrika", "محمد أبو تريكة", "Aboutrika", gem = 50, frames = 12, fw = 92, fh = 176, c = Color(0xFFC80D18), c2 = Color(0xFF75070E)),
		// شيت يامال مقاسه 1600x171 = 16 فريم × 100 بكسل بالظبط. كان مكتوب
		// 103x176 فالرسمة كانت بتقرا من برّه حدود الصورة، فكانت بتظهر حتة من
		// الفريم اللي بعده ويتقص جزء من اللاعب وهو بيجري.
		"yamal" to PlayerDef("yamal", "لامين يامال", "Yamal", gem = 50, frames = 16, fw = 100, fh = 171, c = Color(0xFFBE1D28), c2 = Color(0xFF6F1117)),
		"beckenbauer" to PlayerDef("beckenbauer", "فرانتز بيكنباور", "Beckenbauer", price = 4000, frames = 12, fw = 87, fh = 176, c = Color(0xFFFABE98), c2 = Color(0xFF937059)),
		"haaland" to PlayerDef("haaland", "إيرلينج هالاند", "Haaland", gem = 55, frames = 12, fw = 93, fh = 176, c = Color(0xFFE1292E), c2 = Color(0xFF84181B)),
		"cruyff" to PlayerDef("cruyff", "يوهان كرويف", "Cruyff", price = 4200, frames = 12, fw = 106, fh = 176, c = Color(0xFFEC7B39), c2 = Color(0xFF8B4822)),
		"mbappe" to PlayerDef("mbappe", "كيليان مبابي", "Mbappe", gem = 60, frames = 12, fw = 99, fh = 176, c = Color(0xFF1D439D), c2 = Color(0xFF11275C)),
		"salah" to PlayerDef("salah", "محمد صلاح", "Salah", gem = 60, frames = 12, fw = 88, fh = 176, c = Color(0xFFCC2027), c2 = Color(0xFF781317)),
		"ronaldinho" to PlayerDef("ronaldinho", "رونالدينيو", "Ronaldinho", gem = 65, frames = 12, fw = 83, fh = 176, c = Color(0xFFF0D32A), c2 = Color(0xFF8D7C19)),
		"r9" to PlayerDef("r9", "رونالدو الظاهرة", "R9 Ronaldo", price = 4500, frames = 12, fw = 87, fh = 176, c = Color(0xFFF1D12F), c2 = Color(0xFF8E7B1B)),
		"zidane" to PlayerDef("zidane", "زين الدين زيدان", "Zidane", price = 4600, frames = 12, fw = 93, fh = 176, c = Color(0xFF0D42A0), c2 = Color(0xFF07275E)),
		"maradona" to PlayerDef("maradona", "دييجو مارادونا", "Maradona", price = 5000, frames = 12, fw = 94, fh = 176, c = Color(0xFF82AECB), c2 = Color(0xFF4C6677)),
		"pele" to PlayerDef("pele", "بيليه", "Pele", price = 5200, frames = 12, fw = 92, fh = 176, c = Color(0xFFF3D228), c2 = Color(0xFF8F7B17)),
		"messi" to PlayerDef("messi", "ليونيل ميسي", "Lionel Messi", gem = 85, frames = 12, fw = 88, fh = 159, c = Color(0xFF74ACDF), c2 = Color(0xFF2F75A3)),
		"ronaldo" to PlayerDef("ronaldo", "كريستيانو رونالدو", "Cristiano", gem = 90, frames = 12, fw = 91, fh = 176, c = Color(0xFFB61521), c2 = Color(0xFF6B0C13))
	)

	val IDS: List<String> = ALL.keys.toList()

	/** لاعيبة الكوينز العاديين - خانة "لاعيبة". */
	val COIN_IDS: List<String> = ALL.filter { it.value.gem == null }.keys.toList()

	/** الأساطير: اللاعيبة الـ VIP بالجواهر - خانة لوحدها. */
	val LEGEND_IDS: List<String> = ALL.filter { it.value.gem != null }.keys.toList()

	/** Never null - falls back to the starter if a save names a missing player. */
	operator fun get(id: String?): PlayerDef = ALL[id] ?: ALL[DEFAULT]!!
}
