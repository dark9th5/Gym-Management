plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.compose)
}

android {
	namespace = "com.lc9th5.gym"
	compileSdk = 35

	defaultConfig {
		applicationId = "com.lc9th5.gym"
		minSdk = 26
		targetSdk = 35
		versionCode = 1
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

		buildConfigField("String", "SUPABASE_URL", "\"https://ppjmdjenkupghkogktso.supabase.co\"")
		buildConfigField("String", "SUPABASE_ANON_KEY", "\"sb_publishable__1BAdhepnUyPDuT8mJfKlw_wH3c-CIl\"")
	}

	// Signing configs cho release build
	signingConfigs {
		create("release") {
			// Keystore file nằm trong thư mục gốc project
			val keystoreFile = rootProject.file("gym-release.jks")
			
			if (keystoreFile.exists()) {
				storeFile = keystoreFile
				storePassword = findProperty("KEYSTORE_PASSWORD")?.toString() ?: System.getenv("KEYSTORE_PASSWORD") ?: "Lucvip2003"
				keyAlias = findProperty("KEY_ALIAS")?.toString() ?: System.getenv("KEY_ALIAS") ?: "gym"
				keyPassword = findProperty("KEY_PASSWORD")?.toString() ?: System.getenv("KEY_PASSWORD") ?: "Lucvip2003"
			} else {
				println("WARNING: Keystore not found at: ${keystoreFile.absolutePath}")
			}
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			isShrinkResources = true  // Xóa resources không sử dụng
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
			// Sử dụng signing config nếu có
			signingConfigs.findByName("release")?.let {
				signingConfig = it
			}
		}
		debug {
			isMinifyEnabled = false
			isShrinkResources = false
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_21
		targetCompatibility = JavaVersion.VERSION_21
	}
	kotlin {
		compilerOptions {
			jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
		}
	}
	buildFeatures {
		compose = true
		buildConfig = true
	}
}

dependencies {

	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.activity.compose)
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.compose.ui)
	implementation(libs.androidx.compose.ui.graphics)
	implementation(libs.androidx.compose.ui.tooling.preview)
	implementation(libs.androidx.compose.material3)
	implementation(libs.androidx.compose.material.icons.core)
	implementation(libs.androidx.compose.material.icons.extended)
	implementation(libs.androidx.navigation.compose)
	implementation(libs.androidx.lifecycle.viewmodel.compose)
	implementation(libs.retrofit)
	implementation(libs.retrofit.converter.gson)
	implementation(libs.okhttp)
	implementation(libs.okhttpLoggingInterceptor)
	implementation(libs.gson)
	implementation(libs.androidx.navigation.compose)
	implementation(libs.coil.compose)
	implementation(libs.media3.exoplayer)
	implementation(libs.media3.ui)
	implementation("androidx.security:security-crypto:1.1.0-alpha06")
	// Supabase Storage
	implementation("io.github.jan-tennert.supabase:storage-kt:2.1.0")
	implementation(libs.ktor.client.core)
	implementation(libs.ktor.client.okhttp)
	implementation(libs.ktor.client.content.negotiation)
	implementation(libs.ktor.serialization.kotlinx.json)
	implementation(libs.ktor.client.plugins)
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.compose.ui.test.junit4)
	debugImplementation(libs.androidx.compose.ui.tooling)
	debugImplementation(libs.androidx.compose.ui.test.manifest)
}