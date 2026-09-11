package com.skippo.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlin.math.max

/** One row of the online board — mirrors a document in the `players` collection. */
data class LbEntry(
	val uid: String,
	val name: String,
	val best: Int,
	/** base64 jpeg profile photo, "" = none. Written by pushProfile() as the `avatar` field. */
	val avatar: String = ""
)

/**
 * Firebase layer: anonymous sign-in, the online leaderboard, and the remote
 * ads kill switch.
 *
 * Everything degrades quietly. If Firebase is missing, offline, or the rules
 * reject us, `on` stays false and the leaderboard keeps showing the local
 * records exactly like it does today — the game never blocks on the network.
 */
object Cloud {

	private const val COLLECTION = "players"
	private const val TOP_LIMIT = 50L

	/** true once anonymous sign-in succeeded. */
	var on by mutableStateOf(false)
		private set

	/** true while the board is being fetched. */
	var loading by mutableStateOf(false)
		private set

	/** our own Firestore document id, used to highlight our row. */
	var uid: String? = null
		private set

	/** top players, highest score first. */
	val top = mutableStateListOf<LbEntry>()

	/** Remote Config -> ads_enabled. Flip it in the console to kill every ad. */
	var adsEnabled by mutableStateOf(true)
		private set

	private var db: FirebaseFirestore? = null
	private var store: Store? = null
	private var pendingBest = -1
	private var lastPushed = -1

	// ------------------------------------------------------------------ init

	fun init(S: Store) {
		store = S
		runCatching {
			val auth = FirebaseAuth.getInstance()
			val cur = auth.currentUser
			if (cur != null) {
				signedIn(cur.uid)
			} else {
				auth.signInAnonymously().addOnSuccessListener { res ->
					val id = res.user?.uid
					if (id != null) signedIn(id)
				}
			}
			remoteFlags()
		}
	}

	private fun signedIn(id: String) {
		uid = id
		db = FirebaseFirestore.getInstance()
		on = true
		if (pendingBest >= 0) {
			val b = pendingBest
			pendingBest = -1
			push(b)
		}
		loadTop()
	}

	// ----------------------------------------------------------- leaderboard

	/** Called at the end of every run. Only writes when we beat our own record. */
	fun submit(score: Int) {
		val S = store ?: return
		val best = max(S.best, score)
		if (best <= 0 || best <= lastPushed) return
		if (!on) {
			pendingBest = best
			return
		}
		push(best)
	}

	private fun push(best: Int) {
		val id = uid ?: return
		val d = db ?: return
		val S = store ?: return
		val kit = Players[S.player]
		val row = hashMapOf<String, Any>(
			"name" to if (S.name.isBlank()) "Player" else S.name,
			"avatar" to S.avatar,
			"best" to best,
			"col" to hex(kit.c),
			"updatedAt" to System.currentTimeMillis()
		)
		// merge so the fields written by the web build are left untouched
		d.collection(COLLECTION).document(id).set(row, SetOptions.merge())
			.addOnSuccessListener {
				lastPushed = best
				loadTop()
			}
	}

	/**
	 * Name or photo changed - send it up on its own so the board shows it
	 * without waiting for the player to beat their record.
	 */
	fun pushProfile(S: Store) {
		val id = uid ?: return
		val d = db ?: return
		val kit = Players[S.player]
		val row = hashMapOf<String, Any>(
			"name" to if (S.name.isBlank()) "Player" else S.name,
			"avatar" to S.avatar,
			"col" to hex(kit.c),
			"updatedAt" to System.currentTimeMillis()
		)
		d.collection(COLLECTION).document(id).set(row, SetOptions.merge())
			.addOnSuccessListener { loadTop() }
	}

	fun loadTop() {
		val d = db ?: return
		if (loading) return
		loading = true
		d.collection(COLLECTION)
			.orderBy("best", Query.Direction.DESCENDING)
			.limit(TOP_LIMIT)
			.get()
			.addOnSuccessListener { snap ->
				top.clear()
				for (doc in snap.documents) {
					val best = (doc.getLong("best") ?: 0L).toInt()
					if (best <= 0) continue
					top.add(
						LbEntry(
							doc.id,
							doc.getString("name").orEmpty(),
							best,
							doc.getString("avatar").orEmpty()
						)
					)
				}
				loading = false
			}
			.addOnFailureListener { loading = false }
	}

	// --------------------------------------------------------- remote config

	private fun remoteFlags() {
		val rc = FirebaseRemoteConfig.getInstance()
		rc.setDefaultsAsync(mapOf<String, Any>("ads_enabled" to true))
		rc.setConfigSettingsAsync(
			FirebaseRemoteConfigSettings.Builder()
				.setMinimumFetchIntervalInSeconds(3600)
				.build()
		)
		rc.fetchAndActivate().addOnSuccessListener {
			adsEnabled = rc.getBoolean("ads_enabled")
		}
		// pushes the switch to phones that are already open, within seconds
		rc.addOnConfigUpdateListener(object : ConfigUpdateListener {
			override fun onUpdate(configUpdate: ConfigUpdate) {
				rc.activate().addOnSuccessListener {
					adsEnabled = rc.getBoolean("ads_enabled")
				}
			}

			override fun onError(error: FirebaseRemoteConfigException) {}
		})
	}

	private fun hex(c: Color?): String {
		if (c == null) return "#ff5a3c"
		val r = (c.red * 255f).toInt().coerceIn(0, 255)
		val g = (c.green * 255f).toInt().coerceIn(0, 255)
		val b = (c.blue * 255f).toInt().coerceIn(0, 255)
		return String.format("#%02x%02x%02x", r, g, b)
	}
}
