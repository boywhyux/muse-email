plugins {
    id("com.android.application") version "9.1.0" apply false
    // AGP 9.0 起 Kotlin 已内置，不再需要 org.jetbrains.kotlin.android
    //（加上会直接报错 "no longer required"）；Compose 编译器插件仍需单独声明。
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
}
