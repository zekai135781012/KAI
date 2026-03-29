package com.example.kai;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class YimuJizhang {
    public static void hookVIP(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.wangc.bill")) return;
        final String TARGET_CLASS = "com.wangc.bill.http.entity.User";

        // 永久会员
        XposedHelpers.findAndHookMethod(
                TARGET_CLASS, lpparam.classLoader,
                "getVipType",
                XC_MethodReplacement.returnConstant(2)
        );

        // 是VIP
        XposedHelpers.findAndHookMethod(
                TARGET_CLASS, lpparam.classLoader,
                "isVip",
                XC_MethodReplacement.returnConstant(true)
        );

        // 到期时间 2100-01-01
        XposedHelpers.findAndHookMethod(
                TARGET_CLASS, lpparam.classLoader,
                "getVipTime",
                XC_MethodReplacement.returnConstant(4102415999000L)
        );

        XposedBridge.log("KAI: 一木记账VIP已生效");
    }
}
