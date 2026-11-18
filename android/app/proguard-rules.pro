# Rust cdylib 通过 JNI 调用，保留 native 方法不被混淆/裁剪
-keep class com.muse.email.RustBridge { *; }
-keepclasseswithmembernames class * { native <methods>; }

# R8 开启后：应用自己的类全部保留（org.json 解析不走反射，但 Compose
# 与序列化代码混淆后难排错）；瘦身主要靠裁剪用不到的库代码和资源。
-keep class com.muse.email.** { *; }

# Tink（androidx.security:security-crypto 拉进来的加密库）在编译期引用了
# 这些注解类，打包时并不需要，R8 缺省会当成缺失类报错。只放行注解类，
# 不放行 Tink 自身，Tink 缺类仍会正常报错。
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# 0.5.0 在红米 Note 10 5G（Android 13）上点开就闪退，0.4.0 正常；最大嫌疑是
# AGP 9.1.0 的 R8 激进优化（full mode 在 AGP 9 里关不掉了，旧属性
# android.enableR8.fullMode=false 已废弃，官方认可的等价办法就是 -dontoptimize）。
# 先关优化、只保留裁剪和混淆，等抓到真凶再决定要不要重新打开。
-dontoptimize
