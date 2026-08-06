plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * The TMDB key comes from the CI secret when one is configured, otherwise from a
 * local `local.properties`. When neither is present the build still succeeds and
 * the app asks the user for a key on first launch — see [OnboardingScreen].
 *
 * Read through `providers` so the configuration cache stays valid.
 */
val tmdbApiKey: String =
    providers.environmentVariable("TMDB_API_KEY").orNull?.takeIf { it.isNotBlank() }
        ?: providers.fileContents(rootProject.layout.projectDirectory.file("local.properties"))
            .asText.orNull
            ?.lineSequence()
            ?.firstOrNull { it.trimStart().startsWith("tmdb.apiKey=") }
            ?.substringAfter('=')
            ?.trim()
            ?: ""

android {
    namespace = "com.orphybel.bacine"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.orphybel.bacine"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }

    lint {
        // The CI build must not be blocked by lint warnings, but real errors
        // (a broken TV manifest, for instance) still fail the build.
        warningsAsErrors = false
        abortOnError = true
        // Print findings into the build log. Without this the only record is a
        // file inside build/, which is useless when reading CI output — the
        // failure message just points at a path that no longer exists.
        textReport = true
        disable += "MissingTranslation"
        // LintError is what lint reports when a detector itself throws, as
        // opposed to a finding about this code. The Compose runtime 1.9 lint
        // jar — pulled in transitively by the YouTube player's lifecycle 2.9
        // dependency — is compiled against a newer Kotlin analysis API than
        // AGP 8.7's lint bundles, so its detectors die with an
        // IncompatibleClassChangeError. Disabling the individual detectors is
        // whack-a-mole: every one of them that touches the analysis API breaks
        // the same way. Real findings are still reported and still fail the
        // build. Drop this once AGP is new enough to load that jar.
        disable += "LintError"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.youtube.player)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
