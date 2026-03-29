package com.example.kai;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Duolingo {

    private static final String PREFS_NAME = "Duolingo_Settings";
    private static Activity currentActivity; // 用于 dp 转换

    // 功能开关键名
    private static final String KEY_UNLOCK_VIP = "unlock_vip";
    private static final String KEY_FORCE_CORRECT = "force_correct";

    // 默认值：强制正确关闭，其他开启
    private static final boolean DEFAULT_UNLOCK_VIP = true;
    private static final boolean DEFAULT_FORCE_CORRECT = false;

    public static void hookVIP(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.duolingo")) return;

        // 先 hook Application 的 attach 以获得 Context 和 ClassLoader
        XposedHelpers.findAndHookMethod(android.app.Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Context context = (Context) param.args[0];
                final ClassLoader classLoader = context.getClassLoader();

                // 获取当前 Activity（用于设置界面弹窗）
                XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (currentActivity == null) {
                            currentActivity = (Activity) param.thisObject;
                        }
                    }
                });

                // 延迟加载所有功能，确保 SharedPreferences 可用
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    setupAllHooks(context, classLoader);
                }, 500);

                // 监听设置界面，弹出配置对话框
                try {
                    Class<?> settingsActivity = XposedHelpers.findClass("com.duolingo.settings.SettingsActivity", classLoader);
                    XposedHelpers.findAndHookMethod(settingsActivity, "onCreate", Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Activity act = (Activity) param.thisObject;
                            if (currentActivity == null) currentActivity = act;
                            showSettingsDialog(act);
                        }
                    });
                } catch (Throwable t) {
                    XposedBridge.log("设置页 Hook 失败: " + t.getMessage());
                }
            }
        });
    }

    private static void setupAllHooks(Context context, ClassLoader classLoader) {
        boolean unlockVip = getBooleanSetting(context, KEY_UNLOCK_VIP, DEFAULT_UNLOCK_VIP);
        boolean forceCorrect = getBooleanSetting(context, KEY_FORCE_CORRECT, DEFAULT_FORCE_CORRECT);

        if (unlockVip) {
            hookVip(classLoader);
        }
        if (forceCorrect) {
            hookForceCorrect(classLoader);
        }
        XposedBridge.log("Duolingo: 所有 Hook 已加载 (VIP:" + unlockVip + ", 强制正确:" + forceCorrect + ")");
    }

    // ==================== VIP 解锁 ====================
    private static void hookVip(ClassLoader cl) {
        try {
            // 1. SubscriberLevel 相关
            Class<?> subLevelClass = XposedHelpers.findClass("com.duolingo.data.user.SubscriberLevel", cl);
            XposedHelpers.findAndHookMethod(subLevelClass, "hasSubscription", XC_MethodReplacement.returnConstant(true));
            XposedHelpers.findAndHookMethod(subLevelClass, "getValue", XC_MethodReplacement.returnConstant("GOLD"));
            Object goldEnum = XposedHelpers.getStaticObjectField(subLevelClass, "GOLD");
            XposedBridge.log("Duolingo: SubscriberLevel hooked");

            // 2. User 构造函数替换等级并设置 L0
            Class<?> userClass = XposedHelpers.findClass("com.duolingo.data.user.User", cl);
            XposedBridge.hookAllConstructors(userClass, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Object[] args = param.args;
                    for (int i = 0; i < args.length; i++) {
                        if (subLevelClass.isInstance(args[i])) {
                            args[i] = goldEnum;
                            XposedBridge.log("Duolingo: Replaced SubscriberLevel arg[" + i + "]");
                        }
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    XposedHelpers.setBooleanField(param.thisObject, "L0", true);
                    XposedBridge.log("Duolingo: L0 set to true");
                }
            });
            XposedBridge.log("Duolingo: User constructor hooked");
        } catch (Throwable t) {
            XposedBridge.log("Duolingo VIP Hook 失败: " + t);
        }
    }

    // ==================== 强制题目正确 ====================
    private static void hookForceCorrect(ClassLoader cl) {
        try {
            Class<?> gradeClass = XposedHelpers.findClass("com.duolingo.grade.model.GradeResponse", cl);
            XposedHelpers.findAndHookMethod(gradeClass, "isWithinAcceptableThreshold", XC_MethodReplacement.returnConstant(true));
            XposedBridge.log("Duolingo: isWithinAcceptableThreshold -> true");
        } catch (Throwable t) {
            XposedBridge.log("Duolingo 强制正确 Hook 失败: " + t);
        }
    }

    // ==================== 设置界面 ====================
    private static void showSettingsDialog(final Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        try {
            final Context ctx = activity;
            final Dialog dialog = new Dialog(ctx);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(true);
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
                window.setLayout((int) (dm.widthPixels * 0.9), ViewGroup.LayoutParams.WRAP_CONTENT);
                window.setGravity(Gravity.CENTER);
            }

            LinearLayout main = new LinearLayout(ctx);
            main.setOrientation(LinearLayout.VERTICAL);
            main.setPadding(dp(20), 0, dp(20), dp(20));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(dp(12));
            main.setBackground(bg);

            // 标题栏
            LinearLayout titleContainer = new LinearLayout(ctx);
            titleContainer.setOrientation(LinearLayout.VERTICAL);
            titleContainer.setPadding(dp(24), dp(24), dp(24), dp(24));
            TextView title = new TextView(ctx);
            title.setText("多邻国助手设置");
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            title.setTextColor(Color.BLACK);
            title.setTypeface(null, Typeface.BOLD);
            title.setGravity(Gravity.CENTER);
            titleContainer.addView(title);
            View divider = new View(ctx);
            divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
            divParams.topMargin = dp(16);
            divParams.bottomMargin = dp(8);
            titleContainer.addView(divider, divParams);
            main.addView(titleContainer);

            // 滚动内容
            ScrollView scroll = new ScrollView(ctx) {
                @Override
                protected void onMeasure(int wSpec, int hSpec) {
                    int maxH = (int) (ctx.getResources().getDisplayMetrics().heightPixels * 0.5);
                    super.onMeasure(wSpec, View.MeasureSpec.makeMeasureSpec(maxH, View.MeasureSpec.AT_MOST));
                }
            };
            scroll.setFillViewport(true);
            scroll.setOverScrollMode(ScrollView.OVER_SCROLL_NEVER);
            setScrollbarColor(scroll, Color.BLACK);

            LinearLayout content = new LinearLayout(ctx);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(24), 0, dp(24), dp(24));

            // 添加开关
            addSwitch(content, ctx, "解锁会员", "模拟VIP状态，解锁本地会员功能", KEY_UNLOCK_VIP, DEFAULT_UNLOCK_VIP);
            addSwitch(content, ctx, "强制题目正确", "强制所有答案正确（默认关闭）", KEY_FORCE_CORRECT, DEFAULT_FORCE_CORRECT);

            scroll.addView(content);
            main.addView(scroll, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

            // 确定按钮
            Button ok = new Button(ctx);
            ok.setText("确定");
            ok.setTextColor(Color.WHITE);
            ok.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            ok.setTypeface(null, Typeface.BOLD);
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setColor(Color.BLACK);
            btnBg.setCornerRadius(dp(8));
            ok.setBackground(btnBg);
            ok.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));
            LinearLayout btnContainer = new LinearLayout(ctx);
            btnContainer.setPadding(dp(24), dp(16), dp(24), 0);
            btnContainer.addView(ok);
            main.addView(btnContainer);

            dialog.setContentView(main);
            ok.setOnClickListener(v -> {
                dialog.dismiss();
                Toast.makeText(ctx, "设置已保存，重启应用后生效", Toast.LENGTH_SHORT).show();
            });
            if (!activity.isFinishing()) dialog.show();
        } catch (Exception e) {
            XposedBridge.log("显示设置对话框异常: " + e.getMessage());
        }
    }

    private static void addSwitch(LinearLayout parent, final Context ctx, String label, String hint,
                                  final String key, boolean def) {
        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, dp(12), 0, dp(12));

        LinearLayout hor = new LinearLayout(ctx);
        hor.setOrientation(LinearLayout.HORIZONTAL);
        hor.setGravity(Gravity.CENTER_VERTICAL);
        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tv.setTextColor(Color.BLACK);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        hor.addView(tv);

        final TextView switchView = new TextView(ctx);
        boolean current = getBooleanSetting(ctx, key, def);
        switchView.setTag(current);
        updateSwitchAppearance(switchView, current);
        hor.addView(switchView, new LinearLayout.LayoutParams(dp(60), dp(30)) {{
            setMargins(dp(8), 0, 0, 0);
        }});
        container.addView(hor);

        TextView hintTv = new TextView(ctx);
        hintTv.setText(hint);
        hintTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        hintTv.setTextColor(Color.GRAY);
        hintTv.setPadding(0, dp(6), 0, 0);
        container.addView(hintTv);
        parent.addView(container);

        switchView.setOnClickListener(v -> {
            boolean cur = (boolean) v.getTag();
            boolean newState = !cur;
            v.setTag(newState);
            updateSwitchAppearance(switchView, newState);
            putBooleanSetting(ctx, key, newState);
        });
    }

    private static void updateSwitchAppearance(TextView switchView, boolean isOn) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(15));
        if (isOn) {
            bg.setColor(Color.BLACK);
            switchView.setText("开启");
            switchView.setTextColor(Color.WHITE);
        } else {
            bg.setColor(Color.parseColor("#CCCCCC"));
            switchView.setText("关闭");
            switchView.setTextColor(Color.BLACK);
        }
        switchView.setBackground(bg);
        switchView.setGravity(Gravity.CENTER);
        switchView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        switchView.setTypeface(null, Typeface.BOLD);
        int pad = dp(4);
        switchView.setPadding(pad, pad, pad, pad);
    }

    private static void setScrollbarColor(ScrollView scroll, int color) {
        try {
            java.lang.reflect.Field f = View.class.getDeclaredField("mScrollCache");
            f.setAccessible(true);
            Object cache = f.get(scroll);
            if (cache != null) {
                java.lang.reflect.Field barField = cache.getClass().getDeclaredField("scrollBar");
                barField.setAccessible(true);
                Object bar = barField.get(cache);
                if (bar != null) {
                    Method method = bar.getClass().getMethod("setVerticalThumbDrawable", Drawable.class);
                    GradientDrawable thumb = new GradientDrawable();
                    thumb.setColor(color);
                    thumb.setCornerRadius(dp(10));
                    thumb.setSize(dp(4), dp(40));
                    method.invoke(bar, thumb);
                }
            }
        } catch (Exception ignored) {
        }
    }

    // ==================== 工具方法 ====================
    private static boolean getBooleanSetting(Context ctx, String key, boolean def) {
        try {
            return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(key, def);
        } catch (Exception e) {
            return def;
        }
    }

    private static void putBooleanSetting(Context ctx, String key, boolean val) {
        try {
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(key, val).apply();
        } catch (Exception e) {
            XposedBridge.log("保存设置失败 " + key + ": " + e.getMessage());
        }
    }

    private static int dp(int px) {
        if (currentActivity == null) return px;
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px,
                currentActivity.getResources().getDisplayMetrics());
    }
}