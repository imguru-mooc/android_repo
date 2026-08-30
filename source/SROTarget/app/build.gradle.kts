plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.srotarget"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.srotarget"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 1. 버전의 종류를 정의합니다.
    flavorDimensions += "version"

    // 2. 각 버전(flavor)을 만듭니다.
    productFlavors {
        create("original") {
            dimension = "version"
        }
        create("themed") {
            dimension = "version"
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
    }

//    // SRO 설정을 위해 sourceSets 추가
//    sourceSets {
//        getByName("main") {
//            res.srcDirs("src/main/res", "../sro_overlay/res")
//        }
//    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}