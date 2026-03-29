package com.example.kai;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class xiaobai {

    public static void hookVIP(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.xiaobai.screen.record")) return;

        // 解锁VIP
        XposedHelpers.findAndHookMethod(
            "com.dream.era.global.cn.network.SettingsManager",
            lpparam.classLoader,
            "c",
            XC_MethodReplacement.returnConstant(true));

        XposedHelpers.findAndHookMethod(
            "com.dream.era.global.cn.network.SettingsManager",
            lpparam.classLoader,
            "d",
            XC_MethodReplacement.returnConstant(true));
    }
}
