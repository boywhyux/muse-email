plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.muse.email"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.muse.email"
        minSdk = 26
        targetSdk = 35
        // 覆盖安装要求 versionCode 严格递增；CI 用 github.run_number 注入。
        versionCode = System.getenv("DM_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("DM_VERSION_NAME") ?: "0.5.0"
        // 注：不要在这里再加 ndk abiFilters——它和下面的 splits abi 冲突，
        // Gradle 会直接报 "Conflicting configuration"。三个 ABI 已在 splits 里列出，
        // 与 build-apk.yml 里 cargo-ndk 交叉编译的三个 ABI 一致。
    }

    // 按 ABI 分包：三个包各只含一套 .so，手机只下自己那份，比通用包小 2-3 MB。
    // versionCode 三个包共用同一个 run_number（各设备只装自己 ABI 的包，不冲突）。
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
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    packaging {
        jniLibs { useLegacyPackaging = false }
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    sourceSets {
        getByName("main") { jniLibs.srcDirs("src/main/jniLibs") }
    }
}

dependencies {
    // BOM 2025.10.01 = material3 1.4.0（含 Material 3 Expressive）+ Compose 1.9.4。
    // Expressive API 在 1.4.0 仍是实验性的，用时要加 @OptIn(ExperimentalMaterial3ExpressiveApi::class)。
    // 注意：Compose 1.9.x 要求 Kotlin 2.2.x（见根 build.gradle.kts），Kotlin 2.2 又要求 Gradle 8.11+（见 workflow）。
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
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
