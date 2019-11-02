object Versions {
    const val architectureComponents = "2.0.0"
    const val buildTools = "29.0.0"
    const val cling = "2.1.1"
    const val compileSdk = 29
    const val dagger = "2.24"
    const val detekt = "1.0.0"
    const val glide = "4.9.0"
    const val jetty = "8.2.0.v20160908"
    const val kotlin = "1.3.50"
    const val minSdk = 21
    const val navigation = "2.1.0"
    const val okHttp = "3.9.1"
    const val supportLibrary = "1.1.0"
    const val targetSdk = 29
    const val test = "1.2.0"
    const val versionCode = 48
    const val versionName = "1.5.3"
}

object ClasspathDependencies {
    val androidTools = "com.android.tools.build:gradle:3.5.1"
    val kotlinGradle = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    val navigationSafeArgs =
        "androidx.navigation:navigation-safe-args-gradle-plugin:${Versions.navigation}"
}

object Dependencies {
    const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
    const val timber = "com.jakewharton.timber:timber:4.7.1"
    const val okHttp = "com.squareup.okhttp3:okhttp:4.2.2"

    val androidx = mapOf(
        "appCompat" to "androidx.appcompat:appcompat:${Versions.supportLibrary}",
        "cardView" to "androidx.cardview:cardview:1.0.0",
        "constraintLayout" to "androidx.constraintlayout:constraintlayout:2.0.0-beta3",
        "material" to "com.google.android.material:material:1.2.0-alpha01",
        "lifecycle" to "androidx.lifecycle:lifecycle-extensions:${Versions.architectureComponents}",
        "navigation" to mapOf(
            "fragment" to "androidx.navigation:navigation-fragment-ktx:${Versions.navigation}",
            "ui" to "androidx.navigation:navigation-ui-ktx:${Versions.navigation}"
        ),
        "preference" to "androidx.preference:preference:${Versions.supportLibrary}",
        "recyclerView" to "androidx.recyclerview:recyclerview:1.0.0"
    )

    val coroutines = mapOf(
        "core" to "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0",
        "rxJava2" to "org.jetbrains.kotlinx:kotlinx-coroutines-rx2:1.3.0"
    )

    val dagger = mapOf(
        "android" to "com.google.dagger:dagger-android:${Versions.dagger}",
        "androidSupport" to "com.google.dagger:dagger-android-support:${Versions.dagger}",
        "annotation" to "javax.inject:javax.inject:1",
        "androidProcessor" to "com.google.dagger:dagger-android-processor:${Versions.dagger}",
        "compiler" to "com.google.dagger:dagger-compiler:${Versions.dagger}",
        "core" to "com.google.dagger:dagger:${Versions.dagger}"
    )

    val glide = mapOf(
        "compiler" to "com.github.bumptech.glide:compiler:${Versions.glide}",
        "core" to "com.github.bumptech.glide:glide:${Versions.glide}"
    )

    val rx = mapOf(
        "android" to "io.reactivex.rxjava2:rxandroid:2.0.1",
        "kotlin" to "com.github.ReactiveX:rxKotlin:2.3.0"
    )

    val test = mapOf(
        "junit" to "junit:junit:4.12",
        "mockito" to "org.mockito:mockito-core:1.10.19"
    )
}
