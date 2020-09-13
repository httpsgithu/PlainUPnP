object Versions {
    const val architectureComponents = "2.2.0"
    const val buildTools = "29.0.3"
    const val cardView = "1.0.0"
    const val cling = "2.2.0"
    const val compileSdk = 29
    const val constraintLayout = "2.0.0-rc1"
    const val coroutines = "1.3.9"
    const val coreTesting = "2.1.0"
    const val dagger = "2.27"
    const val detekt = "1.0.0"
    const val hilt = "2.28-alpha"
    const val jetty = "8.1.8.v20121106"
    const val kotlin = "1.4.10"
    const val okHttp = "4.9.0"
    const val material = "1.2.0"
    const val minSdk = 21
    const val nanohttpd = "2.3.1"
    const val recyclerView = "1.1.0"
    const val supportLibrary = "1.1.0"
    const val sqlDelight = "1.4.0"
    const val targetSdk = 29
    const val test = "1.2.0"
    const val versionCode = 66
    const val versionName = "2.8.1"
}

object ClasspathDependencies {
    const val androidTools = "com.android.tools.build:gradle:4.0.1"
    const val kotlinGradle = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val sqlDelight = "com.squareup.sqldelight:gradle-plugin:${Versions.sqlDelight}"
}

object Dependencies {
    const val javaxCdi = "javax.enterprise:cdi-api:2.0.SP1"
    const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
    const val glide = "com.github.bumptech.glide:glide:4.11.0"
    const val fastScroll = "me.zhanghai.android.fastscroll:library:1.1.3"

    val okHttp = mapOf(
        "core" to "com.squareup.okhttp3:okhttp:${Versions.okHttp}"
    )

    const val sqlDelightAndroidDriver =
        "com.squareup.sqldelight:android-driver:${Versions.sqlDelight}"
    const val timber = "com.jakewharton.timber:timber:4.7.1"
    const val coreLibraryDesugaring = "com.android.tools:desugar_jdk_libs:1.0.10"

    val androidx = mapOf(
        "appCompat" to "androidx.appcompat:appcompat:${Versions.supportLibrary}",
        "cardView" to "androidx.cardview:cardview:${Versions.cardView}",
        "constraintLayout" to "androidx.constraintlayout:constraintlayout:${Versions.constraintLayout}",
        "coreKtx" to "androidx.core:core-ktx:1.3.1",
        "coreTesting" to "androidx.arch.core:core-testing:${Versions.coreTesting}",
        "material" to "com.google.android.material:material:${Versions.material}",
        "lifecycle" to mapOf(
            "extensions" to "androidx.lifecycle:lifecycle-extensions:${Versions.architectureComponents}",
            "liveDataKtx" to "androidx.lifecycle:lifecycle-livedata-ktx:${Versions.architectureComponents}",
            "viewModelKtx" to "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.architectureComponents}",
            "lifecycleKtx" to "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.architectureComponents}"
        ),
        "preference" to "androidx.preference:preference:${Versions.supportLibrary}",
        "recyclerView" to "androidx.recyclerview:recyclerview:${Versions.recyclerView}"
    )

    val coroutines = mapOf(
        "core" to "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}",
        "test" to "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}"
    )

    val dagger = mapOf(
        "annotation" to "javax.inject:javax.inject:1",
        "compiler" to "com.google.dagger:dagger-compiler:${Versions.dagger}",
        "core" to "com.google.dagger:dagger:${Versions.dagger}"
    )

    val nanohttpd = mapOf(
        "core" to "org.nanohttpd:nanohttpd:${Versions.nanohttpd}",
        "webserver" to "org.nanohttpd:nanohttpd-webserver:${Versions.nanohttpd}"
    )

    val test = mapOf(
        "junit" to "org.junit.jupiter:junit-jupiter:5.6.2",
        "mockito" to "com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0",
        "mockitoInline" to "org.mockito:mockito-inline:2.13.0"
    )
}
