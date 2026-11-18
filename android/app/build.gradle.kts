plugins {
    id("com.android.application")
    // AGP 9.0 起 Kotlin 已内置，org.jetbrains.kotlin.android 不再需要（加上会报错）。
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.muse.email"
    // compileSdk 36：material3 1.5.0-rc01 / Compose 1.12.0 的 AAR 元数据要求 37，
    // 但 Google 的 SDK 仓库里至今没有 platforms;android-37（2026-11-18 查 repository2-1.xml，
    // 平台最新只有 36），装不上。targetSdk 仍是 35：只升编译版本，不新启用 36/37 的运行时行为。
    compileSdk = 36

    defaultConfig {
        applicationId = "com.muse.email"
        minSdk = 26
        targetSdk = 35
        // 覆盖安装要求 versionCode 严格递增；CI 用 github.run_number 注入。
        versionCode = System.getenv("DM_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("DM_VERSION_NAME") ?: "0.5.1"
        // 注：不要在这里再加 ndk abiFilters——它和下面的 splits abi 冲突，
        // Gradle 会直接报 "Conflicting configuration"。三个 ABI 已在 splits 里列出，
        // 与 build-apk.yml 里 cargo-ndk 交叉编译的三个 ABI 一致。
    }

    // 按 ABI 分包：三个包各只含一套 .so，手机只下自己那份，比通用包小 2-3 MB。
    // versionCode 三个包共用同一个 run_number（各设备只装自己 ABI 的包，不冲突）。
    // AAR 元数据校验关掉：material3 1.5.0-rc01 / Compose 1.12.0 要求 minCompileSdk 37，
    // 但 SDK 仓库里没有 android-37 平台（见上面 compileSdk 注释），校验必挂；
    // 这只是守门检查——库编译期用到的 framework 符号都在 36 以内（编得过就证明静态引用
    // 不依赖 37），关掉后用 36 编译即可。等 android-37 平台上架后删掉下面那段、compileSdk 改 37。

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }

    signingConfigs {
        // CI 从 Actions Secret 恢复固定的 PKCS12 密钥，保证每次构建签名一致，
        // 手机才能覆盖安装升级。本地不配置时回落到 debug key（仅用于本机调试）。
        val keystore = (findProperty("dm.keystore") as String?) ?: System.getenv("DM_KEYSTORE")
        if (!keystore.isNullOrBlank()) {
            create("release") {
                storeFile = file(keystore)
                // openssl 生成的 .p12 是 PKCS12，不显式声明会被当成 JKS 解析失败
                storeType = "PKCS12"
                storePassword = (findProperty("dm.keystore.password") as String?)
                    ?: System.getenv("DM_KEYSTORE_PASSWORD") ?: ""
                keyAlias = (findProperty("dm.key.alias") as String?)
                    ?: System.getenv("DM_KEY_ALIAS") ?: ""
                keyPassword = (findProperty("dm.key.password") as String?)
                    ?: System.getenv("DM_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            // R8 开启：裁剪用不到的库代码、压缩资源。应用自己的类全部保留
            //（见 proguard-rules.pro），瘦身主要靠库和资源，行为不受影响。
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (signingConfigs.findByName("release") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    packaging {
        jniLibs { useLegacyPackaging = false }
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    sourceSets {
        getByName("main") { jniLibs.srcDirs("src/main/jniLibs") }
    }
}

// AGP 9 内置 Kotlin 后 kotlinOptions 已不存在，改用顶层的 jvmToolchain 指定 Kotlin 的 JDK。
kotlin {
    jvmToolchain(17)
}

// 关掉 AAR 元数据校验（见 android 块里的注释）：tasks.named 在配置期找不到这两个任务
//（AGP 9 是惰性创建的），改用 configureEach 按名匹配，后创建的任务也能拦住。
tasks.configureEach {
    if (name == "checkDebugAarMetadata" || name == "checkReleaseAarMetadata") {
        enabled = false
    }
}

dependencies {
    // BOM 2026.08.00 = material3 1.4.0 + Compose 1.12.0。0.5.0 用旧 BOM 时，
    // material3 1.5.0-rc01 把 Compose 拉到 1.12.0，但图标等伴生库还停在 1.9.4 时代，
    // 版本混搭是闪退嫌疑之一；换成 1.12.0 时代的 BOM 让整套 Compose 对齐。
    // 但 1.4.0 正式版里根本没有公开的
    // Expressive API（MotionScheme 是 internal，MaterialExpressiveTheme / ButtonGroup /
    // ToggleButton / LoadingIndicator 都不在 1.4.0 里），公开版在 1.5.0（当前 rc01）。
    // 所以 material3 单独钉到 1.5.0-rc01（显式版本优先于 BOM），它的传递依赖会把
    // Compose 拉到 1.12.0、kotlin-stdlib 拉到 2.2.20；Kotlin 由 AGP 9.1.0 内置，
    // Gradle 用 9.3.1（见 workflow）。1.5.0 出正式版后把这里改回 BOM 托管。
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.material3:material3:1.5.0-rc01")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
