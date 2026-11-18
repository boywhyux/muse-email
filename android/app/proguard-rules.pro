# Rust cdylib 通过 JNI 调用，保留 native 方法不被混淆/裁剪
-keep class com.muse.email.RustBridge { *; }
-keepclasseswithmembernames class * { native <methods>; }

# R8 开启后：应用自己的类全部保留（org.json 解析不走反射，但 Compose
# 与序列化代码混淆后难排错）；瘦身主要靠裁剪用不到的库代码和资源。
-keep class com.muse.email.** { *; }
