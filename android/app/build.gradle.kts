import org.gradle.api.tasks.Sync

plugins {
    id("com.android.application")
}

/*
 * The web app lives at the repository root and stays the single source of truth.
 * Only the files the shell needs are copied into the APK; sw.js is left out on
 * purpose, so the service worker never tries to cache the bundled page.
 */
val webAssetsDir = layout.buildDirectory.dir("web-assets")

val syncWebAssets by tasks.registering(Sync::class) {
    from(rootProject.projectDir.parentFile) {
        include("index.html")
        include("icon.svg")
        include("icon-512.png")
    }
    into(webAssetsDir)
}

android {
    namespace = "com.fluffytighnari.weiguang"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fluffytighnari.weiguang"
        minSdk = 23
        targetSdk = 34
        versionCode = 5
        versionName = "0.1.4"
    }

    /*
     * The keystore is committed on purpose: Android only installs an update over
     * an existing app when the signing key matches. Losing the key means users
     * have to uninstall first, which also wipes their saved moments.
     */
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/weiguang.jks")
            storePassword = "weiguang"
            keyAlias = "weiguang"
            keyPassword = "weiguang"
        }
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // The shell is tiny and hand-checked; skipping lint keeps release builds fast
    // and avoids pulling a large lint toolchain.
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets", webAssetsDir)
        }
    }
}

tasks.named("preBuild") { dependsOn(syncWebAssets) }
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }
    .configureEach { dependsOn(syncWebAssets) }
