package com.skippo.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Profile photos. They live as a base64 JPEG inside the save file and inside
 * the player's Firestore document, which keeps everything on the free plan -
 * no Cloud Storage bucket needed.
 */
object Avatars {

	/** Deliberately tiny: the leaderboard downloads one of these per player. */
	private const val SIDE = 96
	private const val MAX_BYTES = 9000

	private val cache = LinkedHashMap<String, ImageBitmap?>()

	fun image(b64: String): ImageBitmap? {
		if (b64.isEmpty()) return null
		if (cache.containsKey(b64)) return cache[b64]
		val img = runCatching {
			// the web build may hand us a full "data:image/...;base64,xxx" string
			val raw = if (b64.startsWith("data:")) b64.substringAfter(',', "") else b64
			val bytes = Base64.decode(raw, Base64.DEFAULT)
			BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
		}.getOrNull()
		if (cache.size > 60) cache.clear()
		cache[b64] = img
		return img
	}

	/** Centre-crops the picked photo to a square and shrinks it right down. */
	fun encode(ctx: Context, uri: Uri): String? = runCatching {
		val cr = ctx.contentResolver

		// measure first so a 12 megapixel photo never lands in memory whole
		val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
		cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
		var step = 1
		val biggest = max(bounds.outWidth, bounds.outHeight)
		while (biggest / step > SIDE * 2) step *= 2

		val opts = BitmapFactory.Options().apply { inSampleSize = step }
		val src = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
			?: error("cannot decode photo")

		val side = min(src.width, src.height)
		val square = Bitmap.createBitmap(
			src, (src.width - side) / 2, (src.height - side) / 2, side, side
		)
		val small = Bitmap.createScaledBitmap(square, SIDE, SIDE, true)

		var quality = 72
		var out = ByteArrayOutputStream()
		small.compress(Bitmap.CompressFormat.JPEG, quality, out)
		while (out.size() > MAX_BYTES && quality > 30) {
			quality -= 14
			out = ByteArrayOutputStream()
			small.compress(Bitmap.CompressFormat.JPEG, quality, out)
		}
		Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
	}.getOrNull()
}

/** Round profile picture, falling back to the first letter of the name. */
@Composable
fun AvatarDot(b64: String, size: Dp, ring: Color, initial: String = "") {
	val img = remember(b64) { Avatars.image(b64) }
	Box(
		Modifier
			.size(size)
			.clip(CircleShape)
			.background(if (img == null) C.accent else C.cardD, CircleShape)
			.border(3.dp, ring, CircleShape),
		contentAlignment = Alignment.Center
	) {
		if (img != null) {
			Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
		} else {
			Text(
				initial.trim().take(1).uppercase().ifBlank { "?" },
				color = C.white,
				fontWeight = FontWeight.Black,
				fontSize = (size.value * 0.42f).sp
			)
		}
	}
}

/** The name shown on the leaderboard. Placeholder follows the chosen language. */
@Composable
fun NameField(S: Store) {
	val L = S.lang
	val focus = LocalFocusManager.current
	var text by remember { mutableStateOf(S.name) }
	HardShadowBox(
		modifier = Modifier.fillMaxWidth(),
		shadow = 4.dp,
		shadowColor = Color.Black.copy(alpha = 0.13f),
		radius = 16.dp,
		bg = C.white,
		border = 4.dp,
		borderColor = C.cardD
	) {
		BasicTextField(
			value = text,
			onValueChange = { v ->
				val clean = v.replace("\n", "").take(14)
				text = clean
				S.name = clean.trim()
				S.save()
			},
			singleLine = true,
			textStyle = LocalTextStyle.current.copy(
				color = C.ink,
				fontWeight = FontWeight.Black,
				fontSize = 18.sp
			),
			cursorBrush = SolidColor(C.accent),
			keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
			keyboardActions = KeyboardActions(onDone = {
				focus.clearFocus()
				S.touch()
				Cloud.pushProfile(S)
			}),
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp, vertical = 13.dp),
			decorationBox = { inner ->
				if (text.isEmpty()) {
					Text(
						Lang.t("namePh", L),
						color = C.inkDim,
						fontWeight = FontWeight.Black,
						fontSize = 18.sp,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis
					)
				}
				inner()
			}
		)
	}
}

/** Pick a photo from the gallery, or throw the current one away. */
@Composable
fun AvatarPicker(S: Store) {
	val L = S.lang
	val ctx = LocalContext.current
	val scope = rememberCoroutineScope()
	val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
		if (uri != null) {
			scope.launch {
				// decoding happens off the game thread
				val b64 = withContext(Dispatchers.IO) { Avatars.encode(ctx, uri) }
				if (b64 == null) {
					Sfx.sErr()
				} else {
					S.avatar = b64
					S.save(); S.touch()
					Cloud.pushProfile(S)
					Sfx.sPower()
				}
			}
		}
	}
	Row(
		Modifier.fillMaxWidth(),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(14.dp)
	) {
		AvatarDot(S.avatar, 66.dp, C.cardD, S.name)
		Column(
			Modifier.weight(1f),
			verticalArrangement = Arrangement.spacedBy(8.dp)
		) {
			Btn(Lang.t("uploadPhoto", L), Modifier.fillMaxWidth()) { pick.launch("image/*") }
			if (S.avatar.isNotEmpty()) {
				Btn(Lang.t("removePhoto", L), Modifier.fillMaxWidth(), BtnKind.Ghost) {
					S.avatar = ""
					S.save(); S.touch()
					Cloud.pushProfile(S)
				}
			}
		}
	}
}
