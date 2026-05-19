plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0" // ✅ ضروري لـ serialization
}

android {
    namespace = "com.example.servicesapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.servicesapp"
        minSdk = 24 // ⚠️ Supabase يتطلب حد أدنى 26، أو فعل Desugaring لـ 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // ✅ ضروري إذا كان minSdk < 26
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions { jvmTarget = "11" }
}

dependencies {
    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    
    // ✅ Core Library Desugaring (للدعم Android 24-25)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // 🔥 Supabase - باستخدام BOM لإدارة الإصدارات
    implementation(platform("io.github.jan-tennert.supabase:bom:2.5.4"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:gotrue-kt") // ✅ هذا هو auth في الإصدار 2.x
    implementation("io.github.jan-tennert.supabase:storage-kt") // ✅ ✅ ✅ جديد: لتخزين الصور

    // 🔥 Ktor - متوافق مع Supabase 2.5.4
    implementation("io.ktor:ktor-client-okhttp:2.3.7")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7") // ✅ ضروري
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7") // ✅ ضروري

    // 🔥 Coroutines & Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // ✅ ✅ ✅ جديد: Glide لتحميل وعرض الصور
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}