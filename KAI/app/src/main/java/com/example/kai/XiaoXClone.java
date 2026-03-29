package com.example.kai;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage; // 注意这里是 callbacks 包

public class XiaoXClone{
    public static void hookVIP(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.bly.dkplat")) return;
        final String TARGET_CLASS = "com.bly.dkplat.cache.UserCache";

        XposedHelpers.findAndHookMethod(
                TARGET_CLASS, lpparam.classLoader,
                "getVipType",
                XC_MethodReplacement.returnConstant(1)
        );

        XposedHelpers.findAndHookMethod(
                TARGET_CLASS, lpparam.classLoader,
                "isVipUser",
                XC_MethodReplacement.returnConstant(true)
        );

        XposedHelpers.findAndHookMethod(
                TARGET_CLASS, lpparam.classLoader,
                "getExpiredTime",
                XC_MethodReplacement.returnConstant(9994086995115L)
        );

        XposedBridge.log("KAI: 小x分身VIP已生效");
    }
}
