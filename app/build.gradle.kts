import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Team secrets stay in the signing operator's process, never in tracked properties.
val releaseSigningNames = listOf(
    "MEMORIDE_KEYSTORE_FILE", "MEMORIDE_KEYSTORE_PASSWORD", "MEMORIDE_KEY_ALIAS", "MEMORIDE_KEY_PASSWORD",
)
val releaseSigningValues = releaseSigningNames.associateWith { providers.environmentVariable(it).orNull }
val hasReleaseSigning = releaseSigningValues.values.any { !it.isNullOrBlank() }
if (hasReleaseSigning) {
    val missing = releaseSigningNames.filter { releaseSigningValues[it].isNullOrBlank() }
    require(missing.isEmpty()) { "Incomplete release signing environment: ${missing.joinToString()}" }
    require(!releaseSigningValues.getValue("MEMORIDE_KEY_ALIAS").equals("androiddebugkey", ignoreCase = true)) {
        "A team release key is required; the Android debug alias is not allowed."
    }
}
val candidateVersionCode = providers.gradleProperty("memorideVersionCode").orNull?.let {
    requireNotNull(it.toIntOrNull()?.takeIf { code -> code in 1..2_100_000_000 }) {
        "memorideVersionCode must be an integer from 1 to 2100000000."
    }
} ?: 1

android {
    useLibrary("android.car", false)
    namespace = "io.kailand"
    compileSdk {
        version = release(37) { minorApiLevel = 0 }
    }
    defaultConfig {
        applicationId = "io.kailand"
        minSdk = 28
        targetSdk = 37
        versionCode = candidateVersionCode
        versionName = "0.1.0"
    }
    signingConfigs {
        if (hasReleaseSigning) {
            create("teamRelease") {
                val keyFile = rootProject.file(releaseSigningValues.getValue("MEMORIDE_KEYSTORE_FILE")!!)
                require(keyFile.isFile) { "The configured team keystore file does not exist." }
                storeFile = keyFile
                storePassword = releaseSigningValues.getValue("MEMORIDE_KEYSTORE_PASSWORD")
                keyAlias = releaseSigningValues.getValue("MEMORIDE_KEY_ALIAS")
                keyPassword = releaseSigningValues.getValue("MEMORIDE_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = false
            // With no credentials this remains unsigned, never silently debug-signed.
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("teamRelease")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets["main"].assets.directories.add(
        layout.buildDirectory.dir("generated/memorideAssets").get().asFile.absolutePath,
    )
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

// The root Catalog/fixtures/PNG files remain the only editable asset sources.
val syncMemorideAssets by tasks.registering(Sync::class) {
    from(rootProject.file("content")) { into("content") }
    from(rootProject.file("fixtures")) { into("fixtures") }
    from(rootProject.file("assets/starter")) {
        include("*.png")
        into("assets/starter")
    }
    into(layout.buildDirectory.dir("generated/memorideAssets"))
}
tasks.named("preBuild").configure { dependsOn(syncMemorideAssets) }

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.preview)
    implementation(libs.activity.compose)
    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.serialization.json)
    debugImplementation(libs.compose.tooling)
    // A03 runtime probe; production integration follows verified platform behavior.
    debugImplementation("ai.pleos.playground:NaviHelper:3.0.5")
    testImplementation(libs.junit)
}
