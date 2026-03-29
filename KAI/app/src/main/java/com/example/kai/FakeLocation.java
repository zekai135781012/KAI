package com.example.kai;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

// 类名和文件名保持一致：FakeLocation
public class FakeLocation {

    public static void hookFakeLocation(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.lerist.fakelocation")) {
            return;
        }

        try {
            // 核心 HOOK，直接生效
            XposedHelpers.findAndHookMethod(
                "ށ.ރ.ؠ.ؠ.֏",
                lpparam.classLoader,
                "ޅ",
                XC_MethodReplacement.returnConstant(true)
            );

            XposedBridge.log("KAI: FakeLocation 专业版已解锁");
        } catch (Throwable e) {
            XposedBridge.log("KAI: FakeLocation  Hook失败: " + e);
        }
    }
}
