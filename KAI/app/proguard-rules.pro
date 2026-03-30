# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# 核心调试信息保留（便于崩溃追溯，不影响混淆）
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 基础 Android 组件保护（避免系统组件被剥离）
-keep class android.app.Activity { *; }
-keep class android.content.ContentProvider { *; }
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepattributes Signature, InnerClasses, Annotation
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations

# R8 优化配置（适配 AGP 8.0+ 全模式，提升性能）
-allowaccessmodification
-dontskipnonpubliclibraryclasses
-processkotlinnullchecks # 优化 Kotlin 空检查（AGP 9.0+ 默认启用，显式声明兼容低版本）
-dontobfuscate # 禁用类名混淆（Xposed Hook 依赖固定类名，混淆会导致 Hook 失效）

# Compose 专项保护（避免 Composable 函数、状态管理被优化剥离）
-keep class androidx.compose.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class androidx.lifecycle.** { *; }
# 保留 Composable 注解方法（确保 UI 渲染正常）
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
# 保留 Compose 状态相关类（避免状态丢失）
-keep class * extends androidx.compose.runtime.AbstractApplier { *; }
-keep class * implements androidx.compose.runtime.CompositionLocal { *; }

# Xposed 模块核心保护（Hook 关键，不可修改）
# 保留入口类及 handleLoadPackage 方法（Xposed 框架识别必需）
-keep class com.example.kai.HookEntry {
    public void handleLoadPackage(de.robv.android.xposed.callbacks.XC_LoadPackage$LoadPackageParam);
}
# 保留所有 IXposedHook 接口实现类
-keep class * implements de.robv.android.xposed.IXposedHookLoadPackage {
    public void handleLoadPackage(de.robv.android.xposed.callbacks.XC_LoadPackage$LoadPackageParam);
}
# 保留 Xposed 相关依赖类（避免反射调用失败）
-keep class de.robv.android.xposed.** { *; }
-keep interface de.robv.android.xposed.** { *; }
# 保留项目核心代码（Hook 逻辑、配置管理等）
-keep class com.example.kai.** { *; }
-keep class com.example.kai.*$* { *; }
# 保留反射调用相关字段/方法（ConfigManager 等用到的反射逻辑）
-keepclassmembers class com.example.kai.** {
    public <fields>;
    public <methods>;
}

# 冗余日志剔除（减小 APK 体积，不影响功能）
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** i(...);
    public static *** v(...);
    public static *** w(...);
    public static *** e(...);
}
-assumenosideeffects class androidx.compose.ui.util.Log { *; }
-assumenosideeffects class kotlinx.coroutines.internal.Logging { *; }

# 忽略无害警告（避免构建日志冗余）
-dontwarn androidx.compose.ui.tooling.**
-dontwarn androidx.compose.ui.test.**
-dontwarn kotlinx.coroutines.debug.**
-dontwarn org.jetbrains.kotlin.**
-dontwarn de.robv.android.xposed.**
# 忽略 Compose 预览相关警告（debug 依赖，release 未引用）
-dontwarn androidx.compose.ui.tooling.preview.**

# 资源优化兼容（适配 R8 优化资源收缩，避免有用资源被删除）
-keepresources string/app_name
-keepresources mipmap/ic_launcher
-keepresources drawable/**.xml
-keepresources layout/**.xml
