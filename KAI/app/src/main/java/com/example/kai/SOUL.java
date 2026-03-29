package com.example.kai;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SOUL {
    public static void hookVIP(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("cn.soulapp.android")) return;

        // 解除聊天限制
        XposedHelpers.findAndHookMethod(
                "cn.soulapp.android.lib.common.bean.ChatLimitModel",
                lpparam.classLoader,
                "isLimit",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(false);
                    }
                }
        );

        // 消息随时撤回
        XposedHelpers.findAndHookMethod(
                "cn.soulapp.imlib.msg.ImMessage",
                lpparam.classLoader,
                "getLocalTime",
                XC_MethodReplacement.returnConstant(System.currentTimeMillis())
        );

        // 解除社交能力限制
        XposedHelpers.findAndHookMethod(
                "cn.soulapp.android.component.planet.soulmatch.utils.SocialScoreUtils",
                lpparam.classLoader,
                "b",
                int.class, int.class,
                XC_MethodReplacement.DO_NOTHING
        );

        XposedBridge.log("KAI: SOUL 已生效");
    }
}
