package com.example.kai;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Abglvcha {
    public static void hookVIP(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.abglvcha.main")) return;

        hook(lpparam, "vs1", "h0", true);
        hook(lpparam, "vs1", "f0", true);
        hook(lpparam, "vs1", "X", 2);
        hook(lpparam, "vs1", "H", 4787107805000L);

        XposedBridge.log("KAI: 绿茶VPN 已生效");
    }

    private static void hook(XC_LoadPackage.LoadPackageParam lpparam, String cls, String method, Object result) {
        XposedHelpers.findAndHookMethod(cls, lpparam.classLoader, method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                param.setResult(result);
            }
        });
    }
}
