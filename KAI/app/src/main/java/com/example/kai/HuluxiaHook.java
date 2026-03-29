package com.example.kai;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import android.app.Activity;
import android.content.Intent;
import java.util.List;
import android.app.AndroidAppHelper;
import android.os.Bundle;
import android.os.Handler;

public final class HuluxiaHook {

    public static void hookVIP(LoadPackageParam lpparam) {
        if (!"com.huluxia.gametools".equals(lpparam.packageName))
            return;

        // 1. Hook 请求参数
        try {
            Class<?> reqClass = XposedHelpers.findClass("com.huluxia.http.request.a", lpparam.classLoader);
            XposedBridge.hookAllConstructors(reqClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args.length > 3 && param.args[3] instanceof List) {
                        List<?> list = (List<?>) param.args[3];
                        for (Object obj : list) {
                            if (obj == null) continue;
                            try {
                                String key = (String) XposedHelpers.getObjectField(obj, "mKey");
                                if ("glorify_id".equals(key)) {
                                    XposedHelpers.setObjectField(obj, "mValue", "163");
                                    break;
                                }
                            } catch (Throwable t) {}
                        }
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log("葫芦侠 Hook1 err: " + e.getMessage());
        }

        // 2. Hook 主题
        try {
            Class<?> themeClass = XposedHelpers.findClass("com.huluxia.data.theme.ThemeStyle", lpparam.classLoader);
            XposedBridge.hookAllConstructors(themeClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedHelpers.setIntField(param.thisObject, "isuse", 1);
                }
            });
            XposedHelpers.findAndHookMethod(themeClass, "getIsuse", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(1);
                }
            });
        } catch (Exception e) {
            XposedBridge.log("葫芦侠 Hook2 err: " + e.getMessage());
        }

        // 3. Hook 评论状态
        try {
            XposedHelpers.findAndHookMethod(
                "com.huluxia.data.topic.RefCommentItem",
                lpparam.classLoader,
                "getState",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(1);
                    }
                }
            );
        } catch (Exception e) {
            XposedBridge.log("葫芦侠 Hook3 err: " + e.getMessage());
        }

        // 4. Hook 评论文本
        try {
            XposedHelpers.findAndHookMethod(
                "com.huluxia.data.topic.RefCommentItem",
                lpparam.classLoader,
                "getText",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String text = (String) param.getResult();
                        int state = XposedHelpers.getIntField(param.thisObject, "state");
                        param.setResult(state != 1 ? text + "【已删除】" : text);
                    }
                }
            );
        } catch (Exception e) {
            XposedBridge.log("葫芦侠 Hook4 err: " + e.getMessage());
        }

        // 5. Hook 跳过启动页
        try {
            XposedHelpers.findAndHookMethod(
                "com.huluxia.gametools.ui.ToolSplashActivity",
                lpparam.classLoader,
                "onCreate",
                Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        final Activity act = (Activity) param.thisObject;
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Intent intent = new Intent();
                                    intent.setClassName("com.huluxia.gametools", "com.huluxia.ui.home.ToolHomeActivity");
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    act.startActivity(intent);
                                    act.finish();
                                } catch (Exception e) {
                                    XposedBridge.log("葫芦侠 Splash err: " + e.getMessage());
                                }
                            }
                        }, 500);
                    }
                }
            );
        } catch (Exception e) {
            XposedBridge.log("葫芦侠 Hook5 err: " + e.getMessage());
        }
    }
}
