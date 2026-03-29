package com.example.kai;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import android.app.Application;
import android.content.Context;

public class DaysMatter {
    public static void hookVIP(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.clover.daysmatter")) {
            return;
        }

        // 1) Lcom/clover/daysmatter/o0OO0o00;->OooOO0o(Landroid/app/Application;)Z
        try {
            XposedHelpers.findAndHookMethod(
                "com.clover.daysmatter.o0OO0o00",
                lpparam.classLoader,
                "OooOO0o",
                Application.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        param.setResult(true);
                    }
                }
            );
        } catch (Throwable ignored) {}

        // 2) Lcom/clover/daysmatter/ooOoo00;->OooO0Oo(Landroid/content/Context;)Z
        try {
            XposedHelpers.findAndHookMethod(
                "com.clover.daysmatter.ooOoo00",
                lpparam.classLoader,
                "OooO0Oo",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        param.setResult(true);
                    }
                }
            );
        } catch (Throwable ignored) {}
    }
}
