plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
}

android {
	namespace = "com.skippo.game"
	compileSdk = 34

	defaultConfig {
		applicationId = "com.skippo.game"
		minSdk = 24
		targetSdk = 34
		versionCode = 1
		versionName = "1.0"
	}

	buildTypes {
		getByName("debug") {
			isMinifyEnabled = false
			// Dev.kt بيقرا BuildConfig.DEV_UNLOCK. لازم يتعرّف هنا عشان يتولّد.
			// في نسخة الديبوج = true يعني مفتوح للتجربة.
			buildConfigField("boolean", "DEV_UNLOCK", "true")
		}
		getByName("release") {
			isMinifyEnabled = false
			// في نسخة النشر = false عشان ماتطلعش للناس مفتوحة بالغلط.
			buildConfigField("boolean", "DEV_UNLOCK", "false")
			// signed with the debug key so `assembleRelease` also gives an installable apk
			signingConfig = signingConfigs.getByName("debug")
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	kotlinOptions {
		jvmTarget = "17"
	}
	buildFeatures {
		compose = true
		// AGP 8 وقّف توليد كلاس BuildConfig افتراضياً، و Dev.kt بيستعمله
		// (زي BuildConfig.DEBUG). من غير السطر ده الكلاس مابيتعملش أصلاً.
		buildConfig = true
	}
	composeOptions {
		kotlinCompilerExtensionVersion = "1.5.14"
	}
	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}

	androidResources {
		// هتافات الملاعب بتتقرا من جوّا الـ apk بـ AssetManager.openFd()، وده
		// مابيشتغلش غير لو الملف متخزّن من غير ضغط - من غير السطر ده الصوت
		// ممكن يطلع خالص في بعض الأجهزة.
		noCompress += "mp3"
	}
}

dependencies {
	implementation("androidx.core:core-ktx:1.13.1")
	implementation("androidx.activity:activity-compose:1.9.0")
	implementation(platform("androidx.compose:compose-bom:2024.06.00"))
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.ui:ui-graphics")
	implementation("androidx.compose.foundation:foundation")
	implementation("androidx.compose.material3:material3")

	// ---- Firebase: online leaderboard + remote ads switch ----
	implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
	implementation("com.google.firebase:firebase-auth")
	implementation("com.google.firebase:firebase-firestore")
	implementation("com.google.firebase:firebase-config")

	// ---- AdMob ----
	implementation("com.google.android.gms:play-services-ads:23.2.0")
}

// Firebase only wires itself up once app/google-services.json exists, so the
// project still builds and runs (leaderboard falls back to local) without it.
if (project.file("google-services.json").exists()) {
	apply(plugin = "com.google.gms.google-services")
} else {
	logger.warn("\u26a0  app/google-services.json missing \u2014 Firebase disabled, leaderboard stays local")
}
