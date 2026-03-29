package com.example.kai;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import android.os.Bundle;

public class Cleaner {
    public static void hookVIP(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.magicalstory.cleaner")) {
            return;
        }

        // 1. 免登录：伪造 userId、userName + 登录状态
        try {
            XposedHelpers.findAndHookMethod(
                "com.magicalstory.cleaner.setting.settingActivity",
                lpparam.classLoader,
                "onCreate",
                Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object mmkv = XposedHelpers.callStaticMethod(
                            XposedHelpers.findClass("com.tencent.mmkv.MMKV", lpparam.classLoader),
                            "h");
                        XposedHelpers.callMethod(mmkv, "l", "userId", "fake_user");
                        XposedHelpers.callMethod(mmkv, "l", "userName", "免登录用户");
                        XposedHelpers.setBooleanField(param.thisObject, "x", true);
                    }
                }
            );
        } catch (Throwable ignored) {}

        // 2. MMKV 会员有效期强制返回永久时间
        try {
            XposedHelpers.findAndHookMethod(
                "com.tencent.mmkv.MMKV",
                lpparam.classLoader,
                "e",
                String.class,
                long.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        param.setResult(0x3BB2B0C6018L);
                    }
                }
            );
        } catch (Throwable ignored) {}
    }
}
