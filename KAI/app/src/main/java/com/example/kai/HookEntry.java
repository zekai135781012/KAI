package com.example.kai;

import android.app.Application;
import android.content.Context;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class HookEntry implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) throws Throwable {
        if (lp == null) return;
        String pkg = lp.packageName;

        // 跳过系统应用和自身
        if (pkg.startsWith("android.") || pkg.startsWith("com.android.") || pkg.equals("com.example.kai")) {
            return;
        }

        XposedBridge.log("HookEntry: handling " + pkg);

        // 拦截 Application.attach() 获取 Context
        XposedHelpers.findAndHookMethod(
            Application.class,
            "attach",
            Context.class,
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.args[0];
                    
                    // 初始化 ConfigManager（会预加载规则）
                    ConfigManager.INSTANCE.initForXposed(context);
                    
                    // 直接调用 ComponentBlocker（内部已预加载规则）
                    try {
                        ComponentBlocker.INSTANCE.handleLoadPackage(lp);
                    } catch (Throwable t) {
                        XposedBridge.log("ComponentBlocker error: " + t.getMessage());
                    }
                }
            }
        );

        // 原有适配逻辑
        try {
            if ("com.clover.daysmatter".equals(pkg)) {
                DaysMatter.hookVIP(lp);
            } else if ("com.magicalstory.cleaner".equals(pkg)) {
                Cleaner.hookVIP(lp);
            } else if ("com.wangc.bill".equals(pkg)) {
                YimuJizhang.hookVIP(lp);
            } else if ("com.lerist.fakelocation".equals(pkg)) {
                FakeLocation.hookFakeLocation(lp);
            } else if ("com.bly.dkplat".equals(pkg)) {
                XiaoXClone.hookVIP(lp);
            } else if ("com.xiaobai.screen.record".equals(pkg)) {
                xiaobai.hookVIP(lp);
            } else if ("com.huluxia.gametools".equals(pkg)) {
                HuluxiaHook.hookVIP(lp);
            } else if ("com.duolingo".equals(pkg)) {
                Duolingo.hookVIP(lp);
            } else if ("cn.soulapp.android".equals(pkg)) {
                SOUL.hookVIP(lp);
            } else if ("com.kuaiduizuoye.scan".equals(pkg)) {
                KuaiDuiZuoYe.hookVIP(lp);
            } else if ("com.abglvcha.main".equals(pkg)) {
                Abglvcha.hookVIP(lp);
            }
        } catch (Throwable t) {
            XposedBridge.log("HookEntry specific hook error: " + t.getMessage());
        }
    }
}
