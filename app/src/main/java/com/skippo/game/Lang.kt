package com.skippo.game

/** Straight port of the T = { ar: {...}, en: {...} } dictionary. */
object Lang {

	private val ar = mapOf(
		"best" to "أفضل نتيجة", "play" to "إلعب دلوقتي", "daily" to "هدية اليوم", "shop" to "المتجر",
		"missions" to "مهام", "ach" to "إنجازات", "settings" to "إعدادات", "leaderboard" to "المتصدرين",
		"paused" to "توقّف", "resume" to "كمّل لعب", "quit" to "خروج للقائمة", "gameover" to "خسرت!",
		"newbest" to "رقم قياسي جديد!", "scoreLbl" to "النتيجة", "coinsLbl" to "الكوينز",
		"revive" to "إحياء", "free" to "مجاناً", "retry" to "إلعب تاني", "home" to "القائمة الرئيسية",
		"settingsTitle" to "الإعدادات", "ambient" to "الموسيقى", "sfx" to "المؤثرات", "haptics" to "الاهتزاز",
		"volume" to "مستوى الصوت", "difficulty" to "الصعوبة", "easy" to "سهل", "normal" to "عادي",
		"hard" to "صعب", "language" to "اللغة", "resetAll" to "إعادة ضبط كل شيء",
		"playerName" to "اسمك في المتصدرين", "namePh" to "اكتب اسمك",
		"shopTitle" to "المتجر", "tabStore" to "شراء", "tabPlayers" to "لاعيبة", "tabSkins" to "شخصيات", "tabGlows" to "توهج",
		"tabTrails" to "آثار", "tabThemes" to "ثيمات", "tabPower" to "مميزات", "tabCoins" to "كوينز",
		"missionsTitle" to "المهام اليومية", "achTitle" to "الإنجازات", "dailyTitle" to "هدية اليوم",
		"claim" to "استلام", "back" to "رجوع",
		"leaderboardTitle" to "المتصدرين", "lbNote" to "أفضل جولاتك على الجهاز ده",
		"lbNoteLocal" to "محلي فقط (السحابة مقفولة)", "lbLoading" to "بيحمّل...",
		"lbEmpty" to "مفيش نتايج لسه، إلعب عشان تظهر!", "you" to "إنت",
		"cloudOn" to "متصل بالسحابة", "cloudOff" to "أوفلاين",
		"equipped" to "مفعّل", "equip" to "تفعيل", "doubler" to "مضاعف الكوينز", "doublerDesc" to "×2 كوينز للأبد",
		"combo" to "كومبو!", "day" to "يوم", "owned" to "تم التفعيل", "bought" to "تم الشراء!",
		"noCoins" to "كوينز مش كفاية", "noGems" to "جواهر مش كفاية",
		"revived" to "اتحييت!", "resetConfirm" to "متأكد؟ هيتمسح كل حاجة",
		"adNone" to "مفيش إعلان متاح دلوقتي — جرّب كمان شوية",
		"adBlocked" to "مانع الإعلانات شغّال — قفله عشان تاخد المكافأة",
		"adLoading" to "جاري تحميل الإعلان...", "yes" to "أيوه", "no" to "لأ",
		"needStore" to "محتاج ربط بوابة دفع حقيقية",
		"adWait" to "استنى", "newMissions" to "مهام جديدة بعد", "hrs" to "س", "min" to "د", "sec" to "ث",
		"coinsU" to "كوين", "gemsU" to "جوهرة", "tomorrow" to "رجعلك بكرة", "lvl" to "مستوى", "week" to "الأسبوع",
		"noAds" to "إزالة الإعلانات", "noAdsDesc" to "ألعب من غير أي إعلانات", "adsGone" to "الإعلانات اتشالت!",
		"best_deal" to "الأفضل", "popular" to "الأكثر طلباً", "notifs" to "الإشعارات",
		"notifDailyT" to "🎁 هديتك اليومية جاهزة!",
		"dealTitle" to "عرض اليوم", "dealBuy" to "اشترِ الآن", "dealDone" to "اشتريته ✓", "dealEnds" to "ينتهي بعد",
		"playerPhoto" to "صورتك الشخصية", "uploadPhoto" to "📷 رفع صورة", "removePhoto" to "حذف الصورة",
		"tapJump" to "دوس أو Space = نطّة", "skip" to "تخطّي", "copy" to "نسخ",
		"level" to "المستوى "
	)

	private val en = mapOf(
		"best" to "BEST", "play" to "PLAY NOW", "daily" to "Daily Gift", "shop" to "Shop",
		"missions" to "Missions", "ach" to "Trophies", "settings" to "Settings", "leaderboard" to "My Records",
		"paused" to "PAUSED", "resume" to "Resume", "quit" to "Quit to Menu", "gameover" to "GAME OVER",
		"newbest" to "NEW BEST!", "scoreLbl" to "SCORE", "coinsLbl" to "COINS",
		"revive" to "Revive", "free" to "FREE", "retry" to "Play Again", "home" to "Main Menu",
		"settingsTitle" to "Settings", "ambient" to "Music", "sfx" to "Sound FX", "haptics" to "Vibration",
		"volume" to "Volume", "difficulty" to "Difficulty", "easy" to "Easy", "normal" to "Normal",
		"hard" to "Hard", "language" to "Language", "resetAll" to "Reset Everything",
		"playerName" to "Leaderboard name", "namePh" to "Enter your name",
		"shopTitle" to "Shop", "tabStore" to "Store", "tabPlayers" to "Players", "tabSkins" to "Skins", "tabGlows" to "Glow",
		"tabTrails" to "Trails", "tabThemes" to "Themes", "tabPower" to "Perks", "tabCoins" to "Coins",
		"missionsTitle" to "Daily Missions", "achTitle" to "Achievements", "dailyTitle" to "Daily Gift",
		"claim" to "Claim", "back" to "Back",
		"leaderboardTitle" to "My Records", "lbNote" to "Your best runs on this device",
		"lbNoteLocal" to "Local only (cloud off)", "lbLoading" to "Loading...",
		"lbEmpty" to "No runs yet — play to fill your board!", "you" to "You",
		"cloudOn" to "Cloud synced", "cloudOff" to "Offline",
		"equipped" to "Equipped", "equip" to "Equip", "doubler" to "Coin Doubler", "doublerDesc" to "×2 coins forever",
		"combo" to "COMBO!", "day" to "Day", "owned" to "Owned", "bought" to "Purchased!",
		"noCoins" to "Not enough coins", "noGems" to "Not enough gems",
		"revived" to "Revived!", "resetConfirm" to "Sure? This wipes everything",
		"adNone" to "No ad available right now - try again later",
		"adBlocked" to "Ad blocker detected - disable it to get the reward",
		"adLoading" to "Loading ad...", "yes" to "Yes", "no" to "No",
		"needStore" to "Needs a real payment gateway",
		"adWait" to "Wait", "newMissions" to "New missions in", "hrs" to "h", "min" to "m", "sec" to "s",
		"coinsU" to "coins", "gemsU" to "gems", "tomorrow" to "Come back tomorrow", "lvl" to "Lv", "week" to "Week",
		"noAds" to "Remove Ads", "noAdsDesc" to "Play with zero ads", "adsGone" to "Ads removed!",
		"best_deal" to "BEST", "popular" to "POPULAR", "notifs" to "Notifications",
		"notifDailyT" to "🎁 Your daily gift is ready!",
		"dealTitle" to "Daily Deal", "dealBuy" to "Buy Now", "dealDone" to "Purchased ✓", "dealEnds" to "Ends in",
		"playerPhoto" to "Your photo", "uploadPhoto" to "📷 Upload photo", "removePhoto" to "Remove photo",
		"tapJump" to "TAP / SPACE = JUMP", "skip" to "Skip", "copy" to "Copy",
		"level" to "Level "
	)

	// خانات المتجر الجديدة: أساطير / ملاعب مشهورة / ملاعب VIP / ثيمات VIP
	private val extraAr = mapOf(
		"tabLegends" to "أساطير",
		"tabArenas" to "ملاعب مشهورة",
		"tabArenasVip" to "ملاعب منتخبات VIP",
		"tabArenasClubVip" to "ملاعب أندية VIP",
		"tabThemesVip" to "ثيمات VIP",
		"vip" to "VIP",
		"flares" to "شماريخ"
	)

	private val extraEn = mapOf(
		"tabLegends" to "Legends",
		"tabArenas" to "Famous Arenas",
		"tabArenasVip" to "VIP Nations",
		"tabArenasClubVip" to "VIP Clubs",
		"tabThemesVip" to "VIP Themes",
		"vip" to "VIP",
		"flares" to "Flares"
	)

	fun t(key: String, lang: String): String =
		(if (lang == "ar") ar else en)[key]
			?: (if (lang == "ar") extraAr else extraEn)[key]
			?: ar[key] ?: extraAr[key] ?: key
}
