import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.kailand"
    compileSdk {
        version = release(37) { minorApiLevel = 0 }
    }
    defaultConfig {
        applicationId = "io.kailand"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
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
    testImplementation(libs.junit)
}
