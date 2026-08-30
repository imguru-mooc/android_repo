// Top-level build file.
plugins {
    // 안드로이드 애플리케이션 플러그인
    id("com.android.application") version "8.4.1" apply false

    // 코틀린 안드로이드 플러그인
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false

    // ⚠️ 이 줄을 추가하세요.
    // KSP 플러그인 선언
    id("com.google.devtools.ksp") version "1.9.23-1.0.20" apply false
}