package com.example.kai.taptap;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * TapTap SDK Hook 统一入口
 */
public class TapTapEntry {
    
    private static final String TAG = "KAI-TapTap";

    /**
     * 主入口：自动检测并 Hook
     */
    public static void handle(XC_LoadPackage.LoadPackageParam lp) {
        if (lp == null || lp.packageName == null) return;

        // 延迟检测，等待 Application 初始化
        XposedHelpers.findAndHookMethod(
            "android.app.Application",
            lp.classLoader,
            "attach",
            Context.class,
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.args[0];
                    ClassLoader cl = context.getClassLoader();
                    String pkg = context.getPackageName();

                    // 检测并执行 Hook
                    if (detectAndHook(context, cl, pkg)) {
                        android.util.Log.d(TAG, "Successfully hooked: " + pkg);
                    }
                }
            }
        );
    }

    /**
     * 强制 Hook（已知包名时使用）
     */
    public static void forceHook(XC_LoadPackage.LoadPackageParam lp) {
        XposedHelpers.findAndHookMethod(
            "android.app.Application",
            lp.classLoader,
            "attach",
            Context.class,
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.args[0];
                    ClassLoader cl = context.getClassLoader();
                    
                    // 直接调用，不检测
                    TapTapHook.hook(context, cl);
                }
            }
        );
    }

    /**
     * 检测 SDK 并执行对应 Hook
     */
    private static boolean detectAndHook(Context context, ClassLoader cl, String pkg) {
        boolean hooked = false;

        // 检测 TapTap SDK
        if (TapTapUtils.hasTapTapSdk(cl)) {
            android.util.Log.d(TAG, "TapTap SDK detected in: " + pkg);
            TapTapHook.hook(context, cl);
            hooked = true;
        }

        // 检测心动 XDSDK（TapTapHook 内部也会检测，这里可选）
        if (TapTapUtils.hasXDSdk(cl)) {
            android.util.Log.d(TAG, "XDSDK detected in: " + pkg);
            // XDSDK 的 Hook 已经在 TapTapHook.hook() 中执行
            // 如果需要单独 Hook，取消下面注释
            // XDSdkHook.hook(context, cl);
            hooked = true;
        }

        return hooked;
    }
}
