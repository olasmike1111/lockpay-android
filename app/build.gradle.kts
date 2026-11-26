plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.lockpay"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.lockpay"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
        val pub = providers.gradleProperty("LOCKPAY_PUBKEY").getOrElse("")
        val srv = providers.gradleProperty("LOCKPAY_SERVER").getOrElse("")
        buildConfigField("String", "PUBLIC_KEY", "\"$pub\"")
        buildConfigField("String", "SERVER_URL", "\"$srv\"")
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1") // Ed25519 on all Android versions
}
