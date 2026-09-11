package com.skippo.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * AdMob layer — the same three placements the web build had:
 * showMenuBanner(), showMidgameAd() and showRewardedAd().
 *
 * Every entry point is guarded by [Ads.enabled], so flipping `ads_enabled`
 * to false in Firebase Remote Config silences the whole app at once, and a
 * failed load never blocks the game.
 */
object Ads {

	// app: ca-app-pub-3889348095037305~9531490174
	const val bannerUnit = "ca-app-pub-3889348095037305/2953759524"
	private const val INTER_UNIT = "ca-app-pub-3889348095037305/9368104570"
	private const val REWARD_UNIT = "ca-app-pub-3889348095037305/7607320401"

	/** never two full screen ads closer together than this */
	private const val GAP_MS = 60_000L

	var ready by mutableStateOf(false)
		private set

	/** true when a rewarded ad is loaded and can be offered to the player */
	var rewardReady by mutableStateOf(false)
		private set

	private var inter: InterstitialAd? = null
	private var reward: RewardedAd? = null
	private var runs = 0
	private var lastFull = 0L
	private var started = false

	/** off when the remote switch is flipped, or the player bought Remove Ads. */
	fun enabled(S: Store): Boolean = Cloud.adsEnabled && !S.removeAds

	fun init(ctx: Context) {
		if (started) return
		started = true
		val app = ctx.applicationContext
		runCatching {
			MobileAds.initialize(app) {
				ready = true
				loadInter(app)
				loadReward(app)
			}
		}
	}

	// ------------------------------------------------------------ preloading

	private fun loadInter(ctx: Context) {
		InterstitialAd.load(
			ctx, INTER_UNIT, AdRequest.Builder().build(),
			object : InterstitialAdLoadCallback() {
				override fun onAdLoaded(ad: InterstitialAd) {
					inter = ad
				}

				override fun onAdFailedToLoad(err: LoadAdError) {
					inter = null
				}
			}
		)
	}

	private fun loadReward(ctx: Context) {
		RewardedAd.load(
			ctx, REWARD_UNIT, AdRequest.Builder().build(),
			object : RewardedAdLoadCallback() {
				override fun onAdLoaded(ad: RewardedAd) {
					reward = ad
					rewardReady = true
				}

				override fun onAdFailedToLoad(err: LoadAdError) {
					reward = null
					rewardReady = false
				}
			}
		)
	}

	// --------------------------------------------------------- showMidgameAd

	/**
	 * Runs [then] straight away unless an interstitial is due: never on the
	 * first round of a session and never twice within a minute.
	 */
	fun midgame(act: Activity?, S: Store, then: () -> Unit) {
		runs += 1
		val ad = inter
		val now = System.currentTimeMillis()
		if (!enabled(S) || act == null || ad == null || runs <= 1 || now - lastFull < GAP_MS) {
			then()
			return
		}
		inter = null
		lastFull = now
		ad.fullScreenContentCallback = object : FullScreenContentCallback() {
			override fun onAdDismissedFullScreenContent() {
				loadInter(act)
				then()
			}

			override fun onAdFailedToShowFullScreenContent(err: AdError) {
				loadInter(act)
				then()
			}
		}
		ad.show(act)
	}

	// -------------------------------------------------------- showRewardedAd

	/** [onReward] only fires if the player actually watched to the end. */
	fun rewarded(act: Activity?, S: Store, onReward: () -> Unit) {
		val ad = reward
		if (!enabled(S) || act == null || ad == null) return
		reward = null
		rewardReady = false
		ad.fullScreenContentCallback = object : FullScreenContentCallback() {
			override fun onAdDismissedFullScreenContent() {
				loadReward(act)
			}

			override fun onAdFailedToShowFullScreenContent(err: AdError) {
				loadReward(act)
			}
		}
		ad.show(act) { onReward() }
	}

	/** Compose hands out a themed context; dig out the Activity underneath. */
	fun hostOf(ctx: Context): Activity? {
		var c: Context? = ctx
		while (c is ContextWrapper) {
			if (c is Activity) return c
			c = c.baseContext
		}
		return null
	}
}

@Composable
fun rememberAdHost(): Activity? = Ads.hostOf(LocalContext.current)

/** `.ad-banner` — the menu banner, hidden while a round is running. */
@Composable
fun BannerAd(S: Store, modifier: Modifier = Modifier) {
	if (!Ads.ready || !Ads.enabled(S)) return
	AndroidView(
		modifier = modifier.fillMaxWidth(),
		factory = { c ->
			AdView(c).apply {
				setAdSize(AdSize.BANNER)
				adUnitId = Ads.bannerUnit
				loadAd(AdRequest.Builder().build())
			}
		}
	)
}
