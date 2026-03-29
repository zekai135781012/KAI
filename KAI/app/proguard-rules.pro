# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# 保留行号信息，便于崩溃日志追溯
-keepattributes SourceFile,LineNumberTable
# 隐藏原始源文件名
-renamesourcefileattribute SourceFile

# 基础Android核心规则
-keep class android.app.Activity { *; }
-keep class android.content.ContentProvider { *; }
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepattributes Signature, InnerClasses, Annotation
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations

# R8核心优化配置
-allowaccessmodification
-dontskipnonpubliclibraryclasses
-processkotlinnullchecks

# Compose核心保护规则
-keep class androidx.compose.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# 项目核心代码保护（Xposed Hook关键）
-keep class com.example.kai.** { *; }
-keep class com.example.kai.*$* { *; }

# 剔除冗余日志
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** i(...);
    public static *** v(...);
    public static *** w(...);
    public static *** e(...);
}
-assumenosideeffects class androidx.compose.ui.util.Log { *; }
-assumenosideeffects class kotlinx.coroutines.internal.Logging { *; }

# 忽略无用代码警告
-dontwarn androidx.compose.ui.tooling.**
-dontwarn androidx.compose.ui.test.**
-dontwarn kotlinx.coroutines.debug.**
-dontwarn de.robv.android.xposed.**
-dontwarn org.jetbrains.kotlin.**
