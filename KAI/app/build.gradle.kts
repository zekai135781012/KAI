plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    namespace = "com.example.kai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.kai"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.10"

        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("boolean", "IS_XPOSED_MODULE", "true")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            proguardFile("proguard-xposed.pro")
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    buildFeatures {
        compose = true
        viewBinding = false
        dataBinding = false
        buildConfig = true
        aidl = false
        renderScript = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.8"
    }

    packaging {
        resources {
            excludes.add("/META-INF/AL2.0")
            excludes.add("/META-INF/LGPL2.1")
            excludes.add("/META-INF/*.kotlin_module")
            excludes.add("/META-INF/*.txt")
            excludes.add("/fonts/**")
            excludes.add("/images/**")
            pickFirsts.add("META-INF/services/de.robv.android.xposed.IXposedHookLoadPackage")
        }
        jniLibs {
            excludes.add("armeabi-v7a")
            excludes.add("x86")
            excludes.add("x86_64")
            excludes.add("armeabi")
        }
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-Xskip-metadata-version-check",
            "-Xjvm-default=all-compatibility"
        )
    }
}

dependencies {
    // Compose 核心依赖（保留必需，移除图标库）
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // 仅保留核心图标库（系统内置基础图标，无下载问题）
    implementation("androidx.compose.material:material-icons-core:1.5.4")
    // 彻底移除：无法下载的 filled/outlined 图标库
    // implementation("androidx.compose.material:material-icons-filled:1.5.4")
    // implementation("androidx.compose.material:material-icons-outlined:1.5.4")

    // 基础依赖（不变）
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // 调试依赖（不变）
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling-preview")

    // Xposed 编译依赖（仅本地 Jar 包）
    compileOnly(files("libs/XposedBridgeAPI-82_compileonly.jar"))
}

// Xposed 服务文件生成任务
tasks.register("createXposedServiceFile") {
    doLast {
        val serviceDir = file("src/main/resources/META-INF/services")
        serviceDir.mkdirs()
        val serviceFile = file("$serviceDir/de.robv.android.xposed.IXposedHookLoadPackage")
        serviceFile.writeText("com.example.kai.HookEntry")
    }
}

tasks.named("preBuild") {
    dependsOn("createXposedServiceFile")
}

// Kotlin 编译配置
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs = kotlinOptions.freeCompilerArgs + listOf(
        "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
        "-Xskip-metadata-version-check",
        "-Xjvm-default=all-compatibility"
    )
}
