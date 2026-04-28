import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.realm.kotlin)
}

android {
    namespace = "com.example.theglobalcarbonfootprintproject"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.theglobalcarbonfootprintproject"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        val props = Properties()
        val propsFile = rootProject.file("local.properties")
        if (propsFile.exists()) {
            propsFile.inputStream().use { props.load(it) }
        }
        buildConfigField("String", "GEMINI_API_KEY", "\"${props.getProperty("GEMINI_API_KEY") ?: ""}\"")
        buildConfigField("String", "MONGODB_CLIENT_ID", "\"${props.getProperty("MONGODB_CLIENT_ID") ?: ""}\"")
        buildConfigField("String", "MONGODB_CLIENT_SECRET", "\"${props.getProperty("MONGODB_CLIENT_SECRET") ?: ""}\"")
        buildConfigField("String", "MONGODB_URI", "\"${props.getProperty("MONGODB_URI") ?: ""}\"")
        buildConfigField("String", "ATLAS_DATA_API_KEY", "\"${props.getProperty("ATLAS_DATA_API_KEY") ?: ""}\"")
        buildConfigField("String", "ATLAS_APP_ID", "\"${props.getProperty("ATLAS_APP_ID") ?: ""}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/native-image/**"
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.material)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Charts
    implementation(libs.vico.compose.m3)

    // Lottie
    implementation(libs.lottie.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Play Services Location
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)

    // AI & Cloud Sync
    implementation(libs.generativeai)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.realm.base)
    implementation(libs.realm.sync)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}