package com.example.kai.taptap;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * TapTap SDK 核心 Hook 逻辑
 * 移植自 xmnh.soulfrog.app.TapTap
 */
public class TapTapHook {
    
    private static final String TAG = "KAI-TapTap";
    private static final AtomicBoolean isHooked = new AtomicBoolean(false);
    
    // 防沉迷 SP 名称（静态变量保持状态）
    private static String tapTapAntiAddictSp;

    public static void hook(Context context, ClassLoader classLoader) {
        if (!isHooked.compareAndSet(false, true)) {
            Log.d(TAG, "Already hooked, skip");
            return;
        }

        Log.d(TAG, "Starting TapTap SDK hook...");

        try {
            tapTapLicense(context, classLoader);
        } catch (Throwable e) {
            Log.d(TAG, "tapTapLicense => ", e);
        }

        // 防沉迷已注释，如需启用取消下面注释
        /*
        try {
            tapTapAntiAddict(context, classLoader);
        } catch (Throwable e) {
            Log.d(TAG, "tapTapAntiAddict => ", e);
        }
        */

        try {
            xdTapTap(context, classLoader);
        } catch (Throwable e) {
            Log.d(TAG, "xdTapTap => ", e);
        }

        // 注意：原代码有 AppUtil.finish(context)，这里移除避免杀死应用
        // 如需保留，请取消下面注释
        // AppUtil.finish(context);
        
        Log.d(TAG, "TapTap SDK hook completed");
    }

    /**
     * TapTap 授权验证 Hook（原 tapTapLicense 方法）
     */
    private static void tapTapLicense(Context context, ClassLoader classLoader) {
        Class<?> tapTapLicense = XposedHelpers.findClassIfExists("com.taptap.pay.sdk.library.TapTapLicense", classLoader);
        Class<?> tapLicense = XposedHelpers.findClassIfExists("com.taptap.sdk.license.TapTapLicense", classLoader);
        
        if (tapTapLicense == null && tapLicense == null) {
            Log.d(TAG, "No TapTap License class found");
            return;
        }

        // 伪造授权时间（当前时间 - 5天）
        long licenseDate = System.currentTimeMillis() - 430000000L;
        
        // 写入旧版 SP
        context.getSharedPreferences("tap_license", 0)
                .edit()
                .putLong("last_license_date", licenseDate)
                .putLong("last_license_date_second", licenseDate / 1000)
                .putLong("last_purchased_date", licenseDate)
                .apply();
        
        // 写入新版 SP
        context.getSharedPreferences("tap_sdk_sp", 0)
                .edit()
                .putLong("last_licensed_date", licenseDate)
                .putLong("last_licensed_date_second", licenseDate / 1000)
                .putLong("last_purchased_date", licenseDate)
                .putLong("last_licensed_date_five_days", licenseDate)
                .apply();

        Log.d(TAG, "Faked license data, date: " + licenseDate);

        // Hook TapPurchase 购买状态
        Class<?> tapPurchase = XposedHelpers.findClassIfExists("com.taptap.pay.sdk.library.TapPurchase", classLoader);
        if (tapPurchase != null) {
            XposedBridge.hookAllConstructors(tapPurchase, new XC_MethodHook() {
                public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    XposedHelpers.setBooleanField(param.thisObject, "isBought", true);
                }
            });
            Log.d(TAG, "Hooked TapPurchase");
        }

        // Hook PurchaseInfo 购买状态
        Class<?> purchaseInfo = XposedHelpers.findClassIfExists("com.taptap.sdk.license.internal.PurchaseInfo", classLoader);
        if (purchaseInfo != null) {
            XposedBridge.hookAllConstructors(purchaseInfo, new XC_MethodHook() {
                public void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    XposedHelpers.setBooleanField(param.thisObject, "isBought", true);
                }
            });
            Log.d(TAG, "Hooked PurchaseInfo");
        }

        // Hook 旧版 check 方法
        if (tapTapLicense != null) {
            try {
                XposedHelpers.findAndHookMethod(tapTapLicense, "check",
                        Activity.class, Fragment.class, boolean.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                super.beforeHookedMethod(param);
                                param.args[2] = false; // 强制不显示 UI
                                Log.d(TAG, "Intercepted old TapTapLicense.check");
                            }
                        });
            } catch (Throwable e) {
                Log.d(TAG, "tapTapLicense check => ", e);
            }
        }

        // Hook 新版 checkLicense 方法
        if (tapLicense != null) {
            XposedHelpers.findAndHookMethod(tapLicense, "checkLicense",
                    Activity.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            param.args[1] = false; // 强制不显示 UI
                            Log.d(TAG, "Intercepted new TapTapLicense.checkLicense");
                        }
                    });
        }

        // Hook 内部实现
        Class<?> tapLicenseInternal = XposedHelpers.findClassIfExists("com.taptap.sdk.license.internal.TapLicenseInternal", classLoader);
        if (tapLicenseInternal != null) {
            XposedHelpers.findAndHookMethod(tapLicenseInternal, "checkLicense",
                    Activity.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            param.args[1] = false;
                        }
                    });
            Log.d(TAG, "Hooked TapLicenseInternal");
        }

        // Hook DLC 相关（调试日志）
        Class<?> tapLicenseHelper = XposedHelpers.findClassIfExists("com.taptap.pay.sdk.library.TapLicenseHelper", classLoader);
        if (tapLicenseHelper != null) {
            try {
                XposedHelpers.findAndHookMethod(tapLicenseHelper, "setDLCCallback",
                        boolean.class, String.class,
                        classLoader.loadClass("com.taptap.pay.sdk.library.DLCManager$InventoryCallback"),
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                Log.d(TAG, "setDLCCallback called");
                            }
                        });
                
                XposedHelpers.findAndHookMethod(tapLicenseHelper, "purchaseDLC",
                        android.app.Activity.class, String.class,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                Log.d(TAG, "purchaseDLC called");
                            }
                        });
            } catch (Throwable e) {
                Log.d(TAG, "tapLicenseHelper setDLCCallback => ", e);
            }
        }
    }

    /**
     * 防沉迷 Hook（原 tapTapAntiAddict 方法，已注释）
     * 如需启用，取消调用处的注释
     */
    private static void tapTapAntiAddict(Context context, ClassLoader classLoader) throws ClassNotFoundException {
        Class<?> antiAddictionSettings = XposedHelpers.findClassIfExists("com.tapsdk.antiaddiction.settings.AntiAddictionSettings", classLoader);
        if (antiAddictionSettings != null) {
            try {
                XposedHelpers.findAndHookMethod(antiAddictionSettings, "getSPNameByUserId", String.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String result = (String) param.getResult();
                        Log.d(TAG, "getSPNameByUserId => " + result);
                        if (result != null) {
                            tapTapAntiAddictSp = result;
                        }
                    }
                });
            } catch (NoSuchMethodError e) {
                Log.d(TAG, "getSPNameByUserId => ", e);
            }

            if (tapTapAntiAddictSp != null) {
                Log.d(TAG, "tapTapAntiAddictSp => " + tapTapAntiAddictSp);
                SharedPreferences tapAntiSp = context.getSharedPreferences(tapTapAntiAddictSp, Context.MODE_PRIVATE);
                
                try {
                    JSONObject jsonObject = new JSONObject("{\"real_name\":{\"age_limit\":18,\"is_adult\":true},\"anti_addiction\":{\"policy_active\":\"time_range\",\"policy_model\":\"server\",\"policy_heartbeat_interval\":120},\"content_rating_check\":{\"allow\":true},\"local\":{\"time_range\":{\"time_start\":\"20:00\",\"time_end\":\"21:00\",\"text\":{\"allow\":{\"title\":\"健康游戏提示\",\"description\":\"你当前为<font color=\\\"#FF8156\\\"><b>未成年账号</b></font>，已被纳入防沉迷系统。根据国家新闻出版署《关于防止未成年人沉迷网络游戏的通知》《关于进一步严格管理 切实防止未成年人沉迷网络游戏的通知》，网络游戏仅可在周五、周六、周日和法定节假日每日 20 时至 21 时向未成年人提供 60 分钟网络游戏服务。今日游戏时间还剩余<font color=\\\"#FF8156\\\"> <b># ${remaining} #</b> </font>分钟。\"},\"reject\":{\"title\":\"健康游戏提示\",\"description\":\"<span color=\\\"#888888\\\">您当前为未成年账号，已被纳入防沉迷系统。根据国家新闻出版署《关于防止未成年人沉迷网络游戏的通知》《关于进一步严格管理 切实防止未成年人沉迷网络游戏的通知》，周五、周六、周日及法定节假日 20 点 -  21 点之外为<font color=\\\"#FF8156\\\"><b>健康保护时段</b></font>。当前时间段无法游玩，请合理安排时间。</span>\"}},\"holidays\":[\"2025-01-01\",\"2025-01-03\",\"2025-01-04\",\"2025-01-05\",\"2026-09-27\",\"2026-10-01\",\"2026-10-02\",\"2026-10-03\",\"2026-10-04\",\"2026-10-05\",\"2026-10-06\",\"2026-10-07\"]}}}");
                    SharedPreferences.Editor editor = tapAntiSp.edit()
                            .putString("userAntiConfigKey", jsonObject.toString());
                    editor.apply();
                    Log.d(TAG, "Injected fake anti-addiction config");
                } catch (JSONException e) {
                    Log.d(TAG, "userAntiConfigKey => ", e);
                }
            }
        }

        // Hook UserInfo 年龄信息
        Class<?> userInfo = XposedHelpers.findClassIfExists("com.tapsdk.antiaddiction.entities.UserInfo", classLoader);
        if (userInfo != null) {
            XposedBridge.hookAllConstructors(userInfo, new XC_MethodHook() {
                public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
                    XposedHelpers.setIntField(param.thisObject, "ageLimit", 18);
                    XposedHelpers.setIntField(param.thisObject, "remainTime", 999999999);
                }
            });
            Log.d(TAG, "Hooked UserInfo");
        }

        // Hook UI 回调
        Class<?> antiAddictionUIImpl = XposedHelpers.findClassIfExists("com.tapsdk.antiaddictionui.AntiAddictionUIImpl", classLoader);
        if (antiAddictionUIImpl != null) {
            XposedHelpers.findAndHookMethod(antiAddictionUIImpl,
                    "setAntiAddictionCallback",
                    classLoader.loadClass("com.tapsdk.antiaddictionui.AntiAddictionUICallback"),
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Object arg = param.args[0];
                            if (arg == null) return;
                            
                            Log.d(TAG, "setAntiAddictionCallback => " + arg.getClass().getName());
                            
                            XposedHelpers.findAndHookMethod(arg.getClass(), "onCallback",
                                    int.class, Map.class,
                                    new XC_MethodHook() {
                                        @Override
                                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                            param.args[0] = 500; // 强制成年状态码
                                            Log.d(TAG, "Forced anti-addiction callback to 500");
                                        }
                                    });
                        }
                    });
        }

        // Hook 服务实现
        Class<?> tapAntiAddictionServiceImpl = XposedHelpers.findClassIfExists("com.tapsdk.antiaddictionui.wrapper.TapAntiAddictionServiceImpl", classLoader);
        if (tapAntiAddictionServiceImpl != null) {
            XposedHelpers.findAndHookMethod(tapAntiAddictionServiceImpl, "sendCallbackDataToEngine",
                    int.class, java.util.Map.class, classLoader.loadClass("com.tds.common.bridge.BridgeCallback"),
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.args[0] = 500;
                            Object arg2 = param.args[2];
                            if (arg2 != null) {
                                Log.d(TAG, "BridgeCallback impl => " + arg2.getClass().getName());
                            }
                        }
                    });
        }

        Log.d(TAG, "tapTapAntiAddict => end");
    }

    /**
     * 心动 XDSDK Hook（原 xdTapTap 方法）
     * 注意：需要你的 HookUtil 支持 httpUrlConnection
     */
    private static void xdTapTap(Context context, ClassLoader classLoader) {
        Class<?> xDSDK = XposedHelpers.findClassIfExists("com.xd.xdsdk.XDSDK", classLoader);
        if (xDSDK == null) {
            Log.d(TAG, "XDSDK not found");
            return;
        }

        Log.d(TAG, "XDSDK found, hooking HTTP...");

        // 这里需要你的 HookUtil 支持
        // 如果 HookUtil 不存在，需要替换为其他 HTTP Hook 实现
        try {
            // 使用反射调用 HookUtil，避免编译依赖
            Class<?> hookUtilClass = Class.forName("xmnh.soulfrog.utils.HookUtil", true, classLoader);
            
            // 调用 httpUrlConnection 方法
            java.lang.reflect.Method httpMethod = hookUtilClass.getMethod("httpUrlConnection", 
                Class.forName("xmnh.soulfrog.utils.HookUtil$HttpURLConnectionCallback", true, classLoader));
            
            // 创建回调实例
            Object callback = java.lang.reflect.Proxy.newProxyInstance(
                classLoader,
                new Class[]{Class.forName("xmnh.soulfrog.utils.HookUtil$HttpURLConnectionCallback", true, classLoader)},
                (proxy, method, args) -> {
                    if ("onHook".equals(method.getName())) {
                        Class<?> cls = (Class<?>) args[0];
                        Object param = args[1];
                        
                        // Hook 授权接口
                        hookHttpUrlConnectionImpl(cls, "authorizations/taptap", hookParam -> {
                            String json = "{\"id\":88888888,\"user_id\":888888888,\"access_token\":\"root\",\"scopes\":\"user,sdk\"}";
                            InputStream newStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                            setResult(hookParam, newStream);
                        });
                        
                        // Hook 用户信息接口
                        hookHttpUrlConnectionImpl(cls, "user?access_token=", hookParam -> {
                            String json = "{\"id\":\"888888888\",\"name\":\"root\",\"nickname\":null,\"friendly_name\":\"root\",\"created\":1666666666,\"last_login\":0,\"site\":\"9\",\"client_id\":\"root\",\"authoriz_state\":1,\"is_upload_play_log\":1,\"id_card\":\"root\",\"adult_type\":4,\"tmp_to_xd\":true,\"safety\":true,\"privacy_agreement\":0,\"fcm\":0,\"anti_addiction_token\":\"root\",\"phone\":\"18888888888\",\"did\":\"a6b6c6d6e6f6-6666-abcd-6666-a6b6c6d6e6f6\",\"ip\":\"114.114.114.114\"}";
                            InputStream newStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                            setResult(hookParam, newStream);
                        });
                        
                        return null;
                    }
                    return null;
                }
            );
            
            httpMethod.invoke(null, callback);
            Log.d(TAG, "XDSDK HTTP hooked via HookUtil");
            
        } catch (Throwable e) {
            Log.e(TAG, "Failed to hook XDSDK via HookUtil: " + e.getMessage());
            // 降级：直接 Hook URLConnection（简化版）
            hookUrlConnectionDirectly(classLoader);
        }
    }

    /**
     * 简化版 HTTP Hook（降级方案）
     */
    private static void hookUrlConnectionDirectly(ClassLoader classLoader) {
        try {
            Class<?> httpUrlConnection = XposedHelpers.findClassIfExists("java.net.HttpURLConnection", classLoader);
            if (httpUrlConnection != null) {
                XposedHelpers.findAndHookMethod(httpUrlConnection, "getInputStream", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // 这里需要获取 URL 判断，简化处理
                        Log.d(TAG, "getInputStream called (direct hook)");
                    }
                });
            }
        } catch (Throwable e) {
            Log.e(TAG, "Direct URLConnection hook failed", e);
        }
    }

    /**
     * 辅助方法：模拟 HookUtil.hookHttpURLConnectionImpl
     */
    private static void hookHttpUrlConnectionImpl(Class<?> cls, String urlPattern, 
            java.util.function.Consumer<Object> action) {
        // 这里需要根据你的 HookUtil 实际实现调整
        // 简化占位
    }

    /**
     * 辅助方法：设置 Hook 结果
     */
    private static void setResult(Object hookParam, Object result) {
        try {
            java.lang.reflect.Method setResult = hookParam.getClass().getMethod("setResult", Object.class);
            setResult.invoke(hookParam, result);
        } catch (Throwable e) {
            Log.e(TAG, "setResult failed", e);
        }
    }

    /**
     * 防沉迷回调 Hook 工具（原 tapTapAntiAddictionCallback）
     */
    private static void tapTapAntiAddictionCallback(Class<?> cls, String methodName) {
        if (cls == null) return;
        XposedHelpers.findAndHookMethod(cls, methodName != null ? methodName : "onCallback",
                int.class, Map.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        param.args[0] = 500;
                        Log.d(TAG, param.thisObject.getClass().getName() + " tapTapAntiAddictionCallback param => "
                                + param.args[0] + " " + param.args[1]);
                    }
                });
    }
}
