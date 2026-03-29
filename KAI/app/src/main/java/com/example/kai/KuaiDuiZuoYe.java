package com.example.kai;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class KuaiDuiZuoYe {

    private static Activity Context;
    private static final String PREFS_NAME = "KuaiSnap_Settings";
    private static volatile boolean hasShownBlockedToast = false;
    private static final String IMAGE_DIR = "/storage/emulated/0/Android/data/com.kuaiduizuoye.scan/files/image/";
    private static final String TARGET_SUFFIX = "_TRANSITION2.jpg";
    public static final String[] adStarts = {
            "https://adx.zuoyebang.com",
            "https://c.kuaiduizuoye.com/adx",
            "https://ad.",
            "https://ads.",
            "http://ad.",
            "http://ads."
    };
    private static Dialog decryptProgressDialog;

    // ==================== 入口 ====================
    public static void hookVIP(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.kuaiduizuoye.scan")) return;

        XposedHelpers.findAndHookMethod(android.app.Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Context context = (Context) param.args[0];
                final ClassLoader classLoader = context.getClassLoader();
                if (shouldBlockHook(context, lpparam.packageName)) {
                    showBlockedToast(context);
                    return;
                }
                if (Context == null) {
                    XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam parameter) {
                            if (Context == null) Context = (Activity) parameter.thisObject;
                        }
                    });
                }

                // 设置页面弹窗
                try {
                    XposedHelpers.findAndHookMethod("com.kuaiduizuoye.scan.activity.common.CommonCacheHybridActivity",
                            classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) {
                                    final Activity activity = (Activity) param.thisObject;
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        if (!activity.isFinishing() && isSettingsPage(activity)) {
                                            showSettingsDialog(activity);
                                        }
                                    }, 1000);
                                }
                            });
                } catch (Throwable t) {
                    XposedBridge.log("设置页 Hook 失败: " + t.getMessage());
                }

                setupAllHooks(context, classLoader);
            }
        });
    }

    // ==================== 核心 Hook 加载 ====================
    private static void setupAllHooks(Context context, ClassLoader classLoader) {
        try {
            if (getBooleanSetting(context, "enable_vip", true)) setupVipHooks(classLoader);
            if (getBooleanSetting(context, "enable_rotate", true)) setupRotateHooks(classLoader);
            if (getBooleanSetting(context, "enable_hd", true)) setupHdHooks(classLoader);
            if (getBooleanSetting(context, "enable_vip_badge", true)) setupVipBadgeHooks(classLoader);
            if (getBooleanSetting(context, "enable_screen_capture", true)) setupScreenCaptureHooks(classLoader);
            if (getBooleanSetting(context, "enable_ad_block", true)) setupAdBlockHooks(classLoader);
            if (getBooleanSetting(context, "enable_sensor_block", false)) setupSensorBlockHooks(classLoader);
            if (getBooleanSetting(context, "block_collection_dialog", false)) setupCollectionDialogHooks(classLoader);
            if (getBooleanSetting(context, "enable_image_decrypt", true)) setupImageDecryptHooks(classLoader);
            if (getBooleanSetting(context, "block_new_user_banner", false)) setupNewUserBannerHooks(classLoader);
            if (getBooleanSetting(context, "remove_vip_banner", false)) setupVipBannerHooks(classLoader);
            if (getBooleanSetting(context, "enable_video_explanation", false)) setupVideoExplanationHooks(classLoader);
            setupViewBlockHooks(context, classLoader);
        } catch (Exception e) {
            XposedBridge.log("setupAllHooks error: " + e.getMessage());
        }
    }

    // ==================== 设置界面 ====================
    private static boolean isSettingsPage(Activity activity) {
        try {
            WebView webView = findWebView(activity);
            if (webView != null) {
                String title = webView.getTitle();
                return title != null && (title.equals("设置") || title.equals("Settings"));
            }
        } catch (Exception e) {
            XposedBridge.log("判断设置页面出错: " + e.getMessage());
        }
        return false;
    }

    private static WebView findWebView(Activity activity) {
        if (activity == null || activity.isFinishing()) return null;
        View decor = activity.getWindow().getDecorView();
        if (decor instanceof ViewGroup) return findWebViewInViewGroup((ViewGroup) decor);
        return null;
    }

    private static WebView findWebViewInViewGroup(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof WebView) return (WebView) child;
            if (child instanceof ViewGroup) {
                WebView wv = findWebViewInViewGroup((ViewGroup) child);
                if (wv != null) return wv;
            }
        }
        return null;
    }

    private static void showSettingsDialog(final Activity activity) {
        try {
            if (activity == null || activity.isFinishing()) return;
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

            // 标题
            LinearLayout titleContainer = new LinearLayout(ctx);
            titleContainer.setOrientation(LinearLayout.VERTICAL);
            titleContainer.setPadding(dp(24), dp(24), dp(24), dp(24));
            TextView title = new TextView(ctx);
            title.setText("快怼设置");
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
                    super.onMeasure(wSpec, MeasureSpec.makeMeasureSpec(maxH, MeasureSpec.AT_MOST));
                }
            };
            scroll.setFillViewport(true);
            scroll.setOverScrollMode(ScrollView.OVER_SCROLL_NEVER);
            setScrollbarColor(scroll, Color.BLACK);

            LinearLayout content = new LinearLayout(ctx);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(24), 0, dp(24), dp(24));

            addSwitch(content, ctx, "解锁会员", "模拟VIP状态，解锁本地会员及某些功能", "enable_vip", true);
            addSwitch(content, ctx, "图片保存", "通过解密函数绕过保存解析图片至相册限制", "enable_image_decrypt", true);
            addSwitch(content, ctx, "解锁高清内容", "解锁解析图片高清内容查看", "enable_hd", true);
            addSwitch(content, ctx, "解锁横屏旋转", "解锁解析页面横屏旋转功能", "enable_rotate", true);
            addSwitch(content, ctx, "会员金标", "显示会员金标", "enable_vip_badge", true);
            addSwitch(content, ctx, "去除截屏限制", "去除截屏和录屏限制", "enable_screen_capture", true);
            addSwitch(content, ctx, "纯净快对", "拦截所有广告，去他妈的广告", "enable_ad_block", true);
            addSwitch(content, ctx, "我不是新人", "屏蔽我的页面顶部新人优惠广告", "block_new_user_banner", false);
            addSwitch(content, ctx, "去除会员Banner", "屏蔽\"我不是新人\"开启后主页的会员Banner", "remove_vip_banner", false);
            addSwitch(content, ctx, "禁用传感器", "禁用陀螺仪和加速度传感器", "enable_sensor_block", false);
            addSwitch(content, ctx, "红包走开", "去除我的页面红包推广", "block_red_packet", false);
            addSwitch(content, ctx, "屏蔽提示", "去除解析页面上方的\"勤动脑，多思考\"横条", "block_notice_bar", false);
            addSwitch(content, ctx, "本大爷是VIP", "去除VIP专属功能右上角角标", "block_vip_badge", true);
            addSwitch(content, ctx, "不要收藏", "屏蔽每次退出解析页面时烦人的收藏弹窗", "block_collection_dialog", false);
            addSwitch(content, ctx, "解锁讲解视频(实验)", "延长拍照搜题后的讲解视频观看时间，不能真正解锁视频，正在开发，仅供娱乐",
                    "enable_video_explanation", false);

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
            ok.setOnClickListener(v -> dialog.dismiss());
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
            Field f = View.class.getDeclaredField("mScrollCache");
            f.setAccessible(true);
            Object cache = f.get(scroll);
            if (cache != null) {
                Field barField = cache.getClass().getDeclaredField("scrollBar");
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
        } catch (Exception ignored) {}
    }

    // ==================== 组件屏蔽 ====================
    private static void setupViewBlockHooks(Context context, ClassLoader classLoader) {
        try {
            if (getBooleanSetting(context, "block_red_packet", false)) {
                XposedHelpers.findAndHookMethod("com.kuaiduizuoye.scan.activity.main.activity.MainActivity",
                        classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                Activity act = (Activity) param.thisObject;
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    try {
                                        View v = act.findViewById(0x7f090c82);
                                        if (v != null && v.getParent() != null)
                                            ((ViewGroup) v.getParent()).removeView(v);
                                    } catch (Exception e) {
                                        XposedBridge.log("屏蔽红包失败: " + e.getMessage());
                                    }
                                }, 1000);
                            }
                        });
            }

            XposedHelpers.findAndHookMethod("com.kuaiduizuoye.scan.activity.scan.activity.BookCompleteDetailsPictureBrowseActivity",
                    classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            final Activity act = (Activity) param.thisObject;
                            final View decor = act.getWindow().getDecorView();
                            decor.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    decor.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    try {
                                        if (getBooleanSetting(act, "block_notice_bar", false)) {
                                            View textView = findViewByText(decor, "勤动脑多思考");
                                            if (textView != null) {
                                                ViewGroup bar = findNoticeBarRoot(textView);
                                                if (bar != null && bar.getParent() != null)
                                                    ((ViewGroup) bar.getParent()).removeView(bar);
                                                else {
                                                    ViewGroup parent = (ViewGroup) textView.getParent();
                                                    if (parent != null) parent.removeView(textView);
                                                }
                                            } else {
                                                View barById = act.findViewById(0x7f090144);
                                                if (barById != null && barById.getParent() != null)
                                                    ((ViewGroup) barById.getParent()).removeView(barById);
                                            }
                                        }
                                        if (getBooleanSetting(act, "block_vip_badge", true)) {
                                            View badge = act.findViewById(0x7f0910e7);
                                            if (badge != null && badge.getParent() != null)
                                                ((ViewGroup) badge.getParent()).removeView(badge);
                                            removeViewsByText(decor, "VIP");
                                        }
                                    } catch (Exception e) {
                                        XposedBridge.log("屏蔽组件失败: " + e.getMessage());
                                    }
                                }
                            });
                        }
                    });
        } catch (Exception e) {
            XposedBridge.log("setupViewBlockHooks error: " + e.getMessage());
        }
    }

    private static ViewGroup findNoticeBarRoot(View textView) {
        View current = textView;
        while (current != null && current.getParent() instanceof View) {
            View parent = (View) current.getParent();
            if (parent instanceof ViewGroup) {
                ViewGroup pg = (ViewGroup) parent;
                ViewGroup.LayoutParams lp = pg.getLayoutParams();
                if (lp != null && (lp.width == ViewGroup.LayoutParams.MATCH_PARENT ||
                        (lp.width > 0 && lp.width < 1000)) && pg.getHeight() > 0 && pg.getHeight() < 100) {
                    return pg;
                }
                if (pg.getChildCount() > 1) {
                    for (int i = 0; i < pg.getChildCount(); i++) {
                        View child = pg.getChildAt(i);
                        if (child != textView && (child instanceof ImageView ||
                                (child instanceof TextView && ((TextView) child).getText().toString().contains("关闭")))) {
                            return pg;
                        }
                    }
                }
            }
            current = parent;
        }
        return null;
    }

    private static View findViewByText(View root, String target) {
        if (root instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) {
                View found = findViewByText(((ViewGroup) root).getChildAt(i), target);
                if (found != null) return found;
            }
        } else if (root instanceof TextView) {
            String text = ((TextView) root).getText().toString().replace(" ", "").replace("，", "");
            if (text.contains(target)) return root;
        }
        return null;
    }

    private static void removeViewsByText(View root, String target) {
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) removeViewsByText(group.getChildAt(i), target);
        } else if (root instanceof TextView && ((TextView) root).getText().toString().contains(target)) {
            ViewGroup parent = (ViewGroup) root.getParent();
            if (parent != null) parent.removeView(root);
        }
    }

    // ==================== VIP 相关 ====================
    private static void setupVipHooks(ClassLoader cl) {
        try {
            Class<?> config = XposedHelpers.findClass("com.kuaiduizuoye.scan.common.net.model.v1.ShareresourceCollectConfig", cl);
            XposedHelpers.findAndHookMethod("com.kuaiduizuoye.scan.activity.database.a.i", cl, "a", config,
                    new XC_MethodHook() { @Override protected void afterHookedMethod(MethodHookParam p) { p.setResult(false); } });
            XposedHelpers.findAndHookMethod("com.kuaiduizuoye.scan.activity.database.a.i", cl, "c",
                    new XC_MethodHook() { @Override protected void afterHookedMethod(MethodHookParam p) { p.setResult("1"); } });
            XposedHelpers.findAndHookMethod("com.kuaiduizuoye.scan.activity.mine.fragment.MineFragment", cl, "h",
                    new XC_MethodHook() { @Override protected void afterHookedMethod(MethodHookParam p) { p.setResult("1"); } });
            XposedHelpers.findAndHookMethod("com.kuaiduizuoye.scan.activity.mine.util.c", cl, "k",
                    new XC_MethodHook() { @Override protected void afterHookedMethod(MethodHookParam p) { p.setResult("1"); } });
            XposedHelpers.findAndHookMethod("com.kuaiduizuoye.scan.activity.newadvertisement.f.a", cl, "c",
                    new XC_MethodHook() { @Override protected void afterHookedMethod(MethodHookParam p) { p.setResult(true); } });
        } catch (Exception e) {
            XposedBridge.log("setupVipHooks error: " + e.getMessage());
        }
    }

    private static void setupRotateHooks(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod("com.kuaiduizuoye.scan.activity.vip.a.a", cl, "c",
                    new XC_MethodHook() { @Override protected void afterHookedMethod(MethodHookParam p) { p.setResult(1); } });
        } catch (Exception e) {
            XposedBridge.log("setupRotateHooks error: " + e.getMessage());
        }
    }

    private static void setupHdHooks(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod("com.kuaiduizuoye.scan.activity.scan.util.ai", cl, "b",
                    new XC_MethodHook() { @Override protected void afterHookedMethod(MethodHookParam p) { p.setResult(true); } });
        } catch (Exception e) {
            XposedBridge.log("setupHdHooks error: " + e.getMessage());
        }
    }

    private static void setupVipBadgeHooks(ClassLoader cl) {
        try {
            hookVipIcon(cl, "com.kuaiduizuoye.scan.activity.mine.widget.MineAiUserLoginView");
            hookVipIcon(cl, "com.kuaiduizuoye.scan.activity.mine.widget.MineUserLoginView");
        } catch (Exception e) {
            XposedBridge.log("setupVipBadgeHooks error: " + e.getMessage());
        }
    }

    private static void hookVipIcon(ClassLoader cl, String cls) {
        XposedHelpers.findAndHookMethod(cls, cl, "setVipIcon", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam p) {
                Object inst = p.thisObject;
                Object user = XposedHelpers.getObjectField(inst, "mUserInfo");
                if (user != null) {
                    Object vip = XposedHelpers.getObjectField(user, "vip");
                    if (vip != null) XposedHelpers.setIntField(vip, "status", 1);
                    else {
                        try {
                            Class<?> vipCls = XposedHelpers.findClass("com.kuaiduizuoye.scan.entity.VipInfo", cl);
                            Object newVip = vipCls.newInstance();
                            XposedHelpers.setIntField(newVip, "status", 1);
                            XposedHelpers.setObjectField(user, "vip", newVip);
                        } catch (Exception e) {
                            XposedBridge.log("创建 VIP 对象失败: " + e.getMessage());
                        }
                    }
                }
            }
        });
    }

    private static void setupScreenCaptureHooks(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod("com.kuaiduizuoye.scan.activity.scan.activity.BookCompleteDetailsPictureBrowseActivity",
                    cl, "u", new XC_MethodHook() { @Override protected void beforeHookedMethod(MethodHookParam p) { p.setResult(null); } });
        } catch (Exception e) {
            XposedBridge.log("setupScreenCaptureHooks error: " + e.getMessage());
        }
    }

    private static void setupAdBlockHooks(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookConstructor("java.net.URL", cl, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    String url = (String) p.args[0];
                    if (url != null) {
                        for (String ad : adStarts) {
                            if (url.startsWith(ad)) {
                                p.args[0] = "about:blank";
                                break;
                            }
                        }
                    }
                }
            });
            XposedHelpers.findAndHookMethod(WebView.class, "loadUrl", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    String url = (String) p.args[0];
                    if (url != null) {
                        for (String ad : adStarts) {
                            if (url.startsWith(ad)) {
                                p.setResult(null);
                                return;
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log("setupAdBlockHooks error: " + e.getMessage());
        }
    }

    private static void setupSensorBlockHooks(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod(SensorManager.class, "getDefaultSensor", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    int type = (Integer) p.args[0];
                    if (type == Sensor.TYPE_GYROSCOPE || type == Sensor.TYPE_ACCELEROMETER) p.setResult(null);
                }
            });
            XposedHelpers.findAndHookMethod(SensorManager.class, "getSensorList", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    int type = (Integer) p.args[0];
                    if (type == Sensor.TYPE_ALL || type == Sensor.TYPE_GYROSCOPE || type == Sensor.TYPE_ACCELEROMETER) {
                        List<Sensor> sensors = (List<Sensor>) p.getResult();
                        if (sensors != null) {
                            List<Sensor> filtered = new ArrayList<>();
                            for (Sensor s : sensors) {
                                int t = s.getType();
                                if (t != Sensor.TYPE_GYROSCOPE && t != Sensor.TYPE_ACCELEROMETER) filtered.add(s);
                            }
                            p.setResult(filtered);
                        }
                    }
                }
            });
            XposedHelpers.findAndHookMethod(SensorManager.class, "registerListener",
                    SensorEventListener.class, Sensor.class, int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) {
                            Sensor s = (Sensor) p.args[1];
                            if (s != null && (s.getType() == Sensor.TYPE_GYROSCOPE || s.getType() == Sensor.TYPE_ACCELEROMETER))
                                p.setResult(false);
                        }
                    });
        } catch (Exception e) {
            XposedBridge.log("setupSensorBlockHooks error: " + e.getMessage());
        }
    }

    private static void setupCollectionDialogHooks(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod("com.kuaiduizuoye.scan.activity.scan.activity.BookCompleteDetailsPictureBrowseActivity",
                    cl, "onBackPressed", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) {
                            ((Activity) p.thisObject).finish();
                            p.setResult(null);
                        }
                    });
            XposedHelpers.findAndHookMethod("com.kuaiduizuoye.scan.activity.scan.activity.SearchScanCodeResultActivity",
                    cl, "M", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) {
                            ((Activity) p.thisObject).finish();
                            p.setResult(null);
                        }
                    });
        } catch (Exception e) {
            XposedBridge.log("setupCollectionDialogHooks error: " + e.getMessage());
        }
    }

    private static void setupNewUserBannerHooks(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookConstructor("java.net.URL", cl, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    String url = (String) p.args[0];
                    if (url != null && url.equals("https://www.kuaiduizuoye.com/kdapi/conf/mycard"))
                        p.args[0] = "about:blank";
                }
            });
        } catch (Exception e) {
            XposedBridge.log("setupNewUserBannerHooks error: " + e.getMessage());
        }
    }

    private static void setupVipBannerHooks(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod("com.kuaiduizuoye.scan.activity.main.activity.MainActivity",
                    cl, "onCreate", Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam p) {
                            Activity act = (Activity) p.thisObject;
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                try {
                                    View banner = act.findViewById(0x7f091224);
                                    if (banner != null && banner.getParent() != null)
                                        ((ViewGroup) banner.getParent()).removeView(banner);
                                    else tryFindVipBanner(act);
                                } catch (Exception e) {
                                    XposedBridge.log("屏蔽会员Banner失败: " + e.getMessage());
                                }
                            }, 1000);
                        }
                    });
        } catch (Exception e) {
            XposedBridge.log("setupVipBannerHooks error: " + e.getMessage());
        }
    }

    private static void tryFindVipBanner(Activity act) {
        try {
            View v = findViewByText(act.getWindow().getDecorView(), "会员");
            if (v != null && v.getParent() != null) ((ViewGroup) v.getParent()).removeView(v);
            else {
                v = findVipBannerByStructure(act.getWindow().getDecorView());
                if (v != null && v.getParent() != null) ((ViewGroup) v.getParent()).removeView(v);
            }
        } catch (Exception e) {
            XposedBridge.log("备用查找会员Banner失败: " + e.getMessage());
        }
    }

    private static View findVipBannerByStructure(View root) {
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) {
                View child = g.getChildAt(i);
                if (child.getHeight() > 0 && child.getHeight() < 200 && child.getWidth() > 0
                        && child.getWidth() > g.getWidth() / 2) return child;
                View found = findVipBannerByStructure(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void setupVideoExplanationHooks(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod("com.zybang.sdk.player.ui.model.MultipleVideoBean", cl, "getHasBuy",
                    new XC_MethodHook() { @Override protected void afterHookedMethod(MethodHookParam p) { p.setResult(1); } });
            XposedHelpers.findAndHookMethod("com.kuaiduizuoye.scan.activity.video.sdk.VideoPlayerActivity",
                    cl, "onCreate", Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam p) {
                            Activity act = (Activity) p.thisObject;
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                View v = act.findViewById(0x7f0912e3);
                                if (v != null && v.getParent() != null) ((ViewGroup) v.getParent()).removeView(v);
                            }, 1000);
                        }
                    });
        } catch (Exception e) {
            XposedBridge.log("setupVideoExplanationHooks error: " + e.getMessage());
        }
    }

    // ==================== 图片解密 ====================
    private static void setupImageDecryptHooks(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod(
                    "com.kuaiduizuoye.scan.activity.scan.util.-$$Lambda$az$yoGpX1V-cpnva81wmw-_K8CTTPI",
                    cl, "onClick", View.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            View view = (View) param.args[0];
                            if (view.getId() == 0x7f090e80) {
                                param.setResult(null);
                                final Context ctx = view.getContext();
                                if (ctx != null) {
                                    try {
                                        Object lambda = param.thisObject;
                                        Object dialogInst = XposedHelpers.getObjectField(lambda, "f$1");
                                        if (dialogInst != null) XposedHelpers.callMethod(dialogInst, "dismissViewDialog");
                                    } catch (Throwable t) { XposedBridge.log("关闭原对话框失败: " + t.getMessage()); }
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        showDecryptProgress(ctx);
                                        new Thread(() -> {
                                            long start = System.currentTimeMillis();
                                            List<FileInfo> decrypted = decryptAll();
                                            long wait = Math.max(3000 - (System.currentTimeMillis() - start), 0);
                                            if (wait > 0) try { Thread.sleep(wait); } catch (InterruptedException ignored) {}
                                            new Handler(Looper.getMainLooper()).post(() -> {
                                                hideDecryptProgress();
                                                if (decrypted.isEmpty()) Toast.makeText(ctx, "未找到可解密的图片", Toast.LENGTH_SHORT).show();
                                                else showImageSelection(ctx, decrypted);
                                            });
                                        }).start();
                                    });
                                }
                            }
                        }
                    });
        } catch (Exception e) {
            XposedBridge.log("setupImageDecryptHooks error: " + e.getMessage());
        }
    }

    private static List<FileInfo> decryptAll() {
        List<FileInfo> list = new ArrayList<>();
        File dir = new File(IMAGE_DIR);
        if (!dir.exists() || !dir.isDirectory()) return list;
        File[] files = dir.listFiles((d, n) -> n.endsWith(TARGET_SUFFIX));
        if (files == null || files.length == 0) return list;
        for (File f : files) {
            try {
                File dec = decryptFile(f);
                FileInfo info = new FileInfo();
                info.file = dec;
                info.createTime = f.lastModified();
                info.selected = false;
                list.add(info);
            } catch (Exception e) {
                XposedBridge.log("解密失败: " + f.getName() + " - " + e.getMessage());
            }
        }
        Collections.sort(list, (a, b) -> Long.compare(b.createTime, a.createTime));
        return list;
    }

    private static File decryptFile(File encrypted) throws IOException {
        byte[] data = readAll(encrypted);
        byte[] head = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xDB};
        int first = indexOf(data, head, 0);
        int second = indexOf(data, head, first + 1);
        if (first == -1 || second == -1) throw new IOException("JPEG头未找到");
        int secondEnd = second + (second - first);
        byte[] cleaned = new byte[data.length - (secondEnd - second)];
        System.arraycopy(data, 0, cleaned, 0, second);
        System.arraycopy(data, secondEnd, cleaned, second, data.length - secondEnd);
        String name = encrypted.getName().replace(TARGET_SUFFIX, "_DECRYPTED.jpg");
        File out = new File(encrypted.getParentFile(), name);
        writeAll(out, cleaned);
        return out;
    }

    private static int indexOf(byte[] arr, byte[] pat, int from) {
        outer:
        for (int i = from; i <= arr.length - pat.length; i++) {
            for (int j = 0; j < pat.length; j++) if (arr[i + j] != pat[j]) continue outer;
            return i;
        }
        return -1;
    }

    private static byte[] readAll(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = fis.read(buf)) != -1) bos.write(buf, 0, len);
            return bos.toByteArray();
        }
    }

    private static void writeAll(File f, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(data);
        }
    }

    private static void showDecryptProgress(Context ctx) {
        try {
            if (decryptProgressDialog != null && decryptProgressDialog.isShowing()) decryptProgressDialog.dismiss();
            decryptProgressDialog = new Dialog(ctx);
            decryptProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            decryptProgressDialog.setCancelable(false);
            Window win = decryptProgressDialog.getWindow();
            if (win != null) {
                win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                win.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            LinearLayout layout = new LinearLayout(ctx);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);
            layout.setPadding(dp(24), dp(24), dp(24), dp(24));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(dp(12));
            layout.setBackground(bg);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) layout.setElevation(dp(8));
            ProgressBar pb = new ProgressBar(ctx);
            pb.setIndeterminate(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) pb.setIndeterminateTintList(ColorStateList.valueOf(Color.BLACK));
            LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(dp(48), dp(48));
            pbParams.gravity = Gravity.CENTER;
            pbParams.bottomMargin = dp(16);
            layout.addView(pb, pbParams);
            TextView tv = new TextView(ctx);
            tv.setText("正在解密图片...");
            tv.setTextColor(Color.BLACK);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tv.setGravity(Gravity.CENTER);
            layout.addView(tv);
            decryptProgressDialog.setContentView(layout);
            decryptProgressDialog.show();
        } catch (Exception e) {
            XposedBridge.log("显示进度对话框失败: " + e.getMessage());
        }
    }

    private static void hideDecryptProgress() {
        try {
            if (decryptProgressDialog != null && decryptProgressDialog.isShowing()) decryptProgressDialog.dismiss();
            decryptProgressDialog = null;
        } catch (Exception e) {
            XposedBridge.log("隐藏进度对话框失败: " + e.getMessage());
        }
    }

    private static void showImageSelection(final Context ctx, final List<FileInfo> files) {
        try {
            final Dialog dialog = new Dialog(ctx);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(false);
            Window win = dialog.getWindow();
            if (win != null) {
                win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
                win.setLayout((int) (dm.widthPixels * 0.85), (int) (dm.heightPixels * 0.75));
                win.setGravity(Gravity.CENTER);
            }

            LinearLayout main = new LinearLayout(ctx);
            main.setOrientation(LinearLayout.VERTICAL);
            main.setPadding(dp(20), dp(20), dp(20), dp(20));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(dp(16));
            main.setBackground(bg);

            TextView title = new TextView(ctx);
            title.setText("选择要导出到相册的图片");
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            title.setTextColor(Color.BLACK);
            title.setTypeface(null, Typeface.BOLD);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, 0, 0, dp(16));
            main.addView(title);

            LinearLayout allLayout = new LinearLayout(ctx);
            allLayout.setOrientation(LinearLayout.HORIZONTAL);
            allLayout.setGravity(Gravity.CENTER_VERTICAL);
            allLayout.setPadding(0, 0, 0, dp(16));
            final CheckBox selectAll = new CheckBox(ctx);
            selectAll.setText("全选");
            selectAll.setTextColor(Color.BLACK);
            selectAll.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) selectAll.setButtonTintList(ColorStateList.valueOf(Color.BLACK));
            GradientDrawable cbBg = new GradientDrawable();
            cbBg.setColor(Color.parseColor("#F5F5F5"));
            cbBg.setCornerRadius(dp(4));
            selectAll.setBackground(cbBg);
            selectAll.setPadding(dp(8), dp(4), dp(8), dp(4));
            allLayout.addView(selectAll);
            main.addView(allLayout);

            final TextView countText = new TextView(ctx);
            countText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            countText.setTextColor(Color.GRAY);
            countText.setGravity(Gravity.CENTER);
            countText.setPadding(0, 0, 0, dp(16));
            main.addView(countText);

            ScrollView scroll = new ScrollView(ctx);
            scroll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
            LinearLayout grid = new LinearLayout(ctx);
            grid.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(grid);

            final List<CheckBox> checkBoxes = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            final Runnable updateCount = () -> {
                int sel = 0;
                for (CheckBox cb : checkBoxes) if (cb.isChecked()) sel++;
                countText.setText("已选择: " + sel + " / " + files.size());
                selectAll.setChecked(sel == files.size());
            };
            updateCount.run();

            LinearLayout row = null;
            for (int i = 0; i < files.size(); i++) {
                final int index = i;  // 修复：将 i 赋值给 final 变量
                final FileInfo info = files.get(i);
                if (i % 2 == 0) {
                    row = new LinearLayout(ctx);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setWeightSum(2);
                    grid.addView(row);
                }
                LinearLayout item = new LinearLayout(ctx);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setGravity(Gravity.CENTER_HORIZONTAL);
                LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                itemLp.setMargins(i % 2 == 0 ? 0 : dp(8), 0, 0, dp(16));
                item.setLayoutParams(itemLp);

                FrameLayout imgContainer = new FrameLayout(ctx);
                imgContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(120)));
                GradientDrawable imgBg = new GradientDrawable();
                imgBg.setColor(Color.LTGRAY);
                imgBg.setCornerRadius(dp(8));
                imgContainer.setBackground(imgBg);

                final ImageView imageView = new ImageView(ctx);
                FrameLayout.LayoutParams imgParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                imgParams.setMargins(dp(2), dp(2), dp(2), dp(2));
                imageView.setLayoutParams(imgParams);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setClipToOutline(true);
                imgContainer.addView(imageView);

                final CheckBox cb = new CheckBox(ctx);
                cb.setChecked(info.selected);
                FrameLayout.LayoutParams cbParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                cbParams.gravity = Gravity.TOP | Gravity.START;
                cbParams.setMargins(dp(8), dp(8), 0, 0);
                cb.setLayoutParams(cbParams);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) cb.setButtonTintList(ColorStateList.valueOf(Color.BLACK));
                cb.setOnCheckedChangeListener((button, isChecked) -> {
                    info.selected = isChecked;
                    updateCount.run();
                });
                imgContainer.addView(cb);
                checkBoxes.add(cb);

                TextView name = new TextView(ctx);
                name.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                name.setPadding(dp(4), dp(8), dp(4), 0);
                name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                name.setTextColor(Color.DKGRAY);
                name.setSingleLine(true);
                name.setEllipsize(TextUtils.TruncateAt.END);
                String fname = info.file.getName();
                if (fname.length() > 20) fname = fname.substring(0, 17) + "...";
                name.setText(fname + "\n" + sdf.format(new Date(info.createTime)));
                item.addView(imgContainer);
                item.addView(name);

                item.setOnClickListener(v -> cb.setChecked(!cb.isChecked()));
                item.setOnLongClickListener(v -> {
                    showEnlargedImage(ctx, info.file, index, files, checkBoxes);
                    return true;
                });

                new Thread(() -> {
                    Bitmap thumb = createThumbnail(info.file.getAbsolutePath(), dp(200), dp(200));
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (thumb != null) imageView.setImageBitmap(thumb);
                        else imageView.setBackgroundColor(Color.DKGRAY);
                    });
                }).start();

                if (row != null) row.addView(item);
            }

            main.addView(scroll);

            LinearLayout btnLayout = new LinearLayout(ctx);
            btnLayout.setOrientation(LinearLayout.HORIZONTAL);
            btnLayout.setGravity(Gravity.CENTER);
            btnLayout.setPadding(0, dp(16), 0, 0);
            Button cancel = new Button(ctx);
            cancel.setText("取消");
            cancel.setTextColor(Color.WHITE);
            cancel.setBackground(createButtonBg(ctx, Color.parseColor("#9E9E9E")));
            cancel.setPadding(dp(24), dp(8), dp(24), dp(8));
            Button confirm = new Button(ctx);
            confirm.setText("导出选中");
            confirm.setTextColor(Color.WHITE);
            confirm.setBackground(createButtonBg(ctx, Color.BLACK));
            confirm.setPadding(dp(24), dp(8), dp(24), dp(8));

            cancel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            LinearLayout.LayoutParams confParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            confParams.setMargins(dp(16), 0, 0, 0);
            confirm.setLayoutParams(confParams);
            btnLayout.addView(cancel);
            btnLayout.addView(confirm);
            main.addView(btnLayout);

            selectAll.setOnCheckedChangeListener((button, isChecked) -> {
                for (CheckBox cbx : checkBoxes) cbx.setChecked(isChecked);
                updateCount.run();
            });

            cancel.setOnClickListener(v -> {
                for (FileInfo info : files) if (info.file.exists()) info.file.delete();
                dialog.dismiss();
                Toast.makeText(ctx, "已取消导出，解密文件已删除", Toast.LENGTH_SHORT).show();
            });

            confirm.setOnClickListener(v -> new Thread(() -> {
                // 优先公共目录，失败则私有目录
                File publicDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "KuaiDuizuoye");
                boolean usePublic = publicDir.exists() || publicDir.mkdirs();
                File targetDir = usePublic ? publicDir : new File(ctx.getExternalFilesDir(null), "Exported");
                if (!targetDir.exists() && !targetDir.mkdirs()) {
                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(ctx, "无法创建保存目录", Toast.LENGTH_SHORT).show());
                    return;
                }
                int exported = 0, deleted = 0;
                for (int i = 0; i < files.size(); i++) {
                    FileInfo info = files.get(i);
                    File src = info.file;
                    if (checkBoxes.get(i).isChecked()) {
                        File dest = new File(targetDir, src.getName());
                        try {
                            copyFile(src, dest);
                            if (src.delete()) {
                                exported++;
                                if (usePublic) {
                                    MediaScannerConnection.scanFile(ctx, new String[]{dest.getAbsolutePath()},
                                            new String[]{"image/jpeg"}, null);
                                }
                            }
                        } catch (IOException e) {
                            XposedBridge.log("导出失败: " + e.getMessage());
                        }
                    } else {
                        if (src.exists() && src.delete()) deleted++;
                    }
                }
                final int finalExp = exported, finalDel = deleted;
                final boolean finalPublic = usePublic;
                final String finalPath = targetDir.getAbsolutePath();
                new Handler(Looper.getMainLooper()).post(() -> {
                    dialog.dismiss();
                    String msg;
                    if (finalPublic && finalExp > 0) msg = "导出成功: " + finalExp + "张\n已保存到相册";
                    else if (!finalPublic && finalExp > 0) msg = "导出成功: " + finalExp + "张\n因权限不足，保存到:\n" + finalPath + "\n请使用文件管理器查看";
                    else msg = "导出成功: " + finalExp + "张, 删除: " + finalDel + "张";
                    Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
                });
            }).start());

            dialog.setContentView(main);
            dialog.show();
        } catch (Exception e) {
            XposedBridge.log("显示图片选择对话框失败: " + e.getMessage());
        }
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
        }
    }

    private static void showEnlargedImage(Context ctx, File img, int idx, List<FileInfo> files, List<CheckBox> boxes) {
        try {
            final Dialog dialog = new Dialog(ctx, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setCancelable(true);
            final FrameLayout root = new FrameLayout(ctx);
            root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            root.setBackgroundColor(Color.BLACK);

            final TouchImageView imageView = new TouchImageView(ctx);
            imageView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            LinearLayout toolbar = new LinearLayout(ctx);
            toolbar.setOrientation(LinearLayout.HORIZONTAL);
            toolbar.setGravity(Gravity.CENTER_VERTICAL);
            toolbar.setBackgroundColor(Color.parseColor("#80000000"));
            FrameLayout.LayoutParams toolParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(56));
            toolParams.gravity = Gravity.TOP;
            toolbar.setLayoutParams(toolParams);
            toolbar.setPadding(dp(16), 0, dp(16), 0);

            ImageButton back = new ImageButton(ctx);
            back.setImageResource(android.R.drawable.ic_menu_revert);
            back.setBackground(null);
            back.setColorFilter(Color.WHITE);
            back.setOnClickListener(v -> dialog.dismiss());
            toolbar.addView(back);

            final TextView fileName = new TextView(ctx);
            fileName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            fileName.setTextColor(Color.WHITE);
            fileName.setPadding(dp(16), 0, 0, 0);
            fileName.setSingleLine(true);
            fileName.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            toolbar.addView(fileName, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            final TextView selection = new TextView(ctx);
            selection.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            selection.setTextColor(Color.WHITE);
            selection.setPadding(dp(16), 0, 0, 0);
            toolbar.addView(selection);

            final CheckBox checkBox = new CheckBox(ctx);
            checkBox.setButtonTintList(ColorStateList.valueOf(Color.WHITE));
            toolbar.addView(checkBox);

            root.addView(imageView);
            root.addView(toolbar);

            final ProgressBar progress = new ProgressBar(ctx);
            progress.setIndeterminate(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) progress.setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));
            FrameLayout.LayoutParams progParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            progParams.gravity = Gravity.CENTER;
            root.addView(progress, progParams);

            final int[] curIdx = {idx};
            final Runnable updateUI = () -> {
                checkBox.setOnCheckedChangeListener(null);
                String name = files.get(curIdx[0]).file.getName();
                fileName.setText(name.length() > 25 ? name.substring(0, 22) + "..." : name);
                checkBox.setChecked(boxes.get(curIdx[0]).isChecked());
                int sel = 0;
                for (CheckBox cb : boxes) if (cb.isChecked()) sel++;
                selection.setText((curIdx[0] + 1) + "/" + files.size() + " (已选: " + sel + ")");
                checkBox.setOnCheckedChangeListener((btn, isChecked) -> {
                    boxes.get(curIdx[0]).setChecked(isChecked);
                    int newSel = 0;
                    for (CheckBox cb : boxes) if (cb.isChecked()) newSel++;
                    selection.setText((curIdx[0] + 1) + "/" + files.size() + " (已选: " + newSel + ")");
                });
            };
            updateUI.run();

            final GestureDetector gesture = new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
                    float dx = e2.getX() - e1.getX();
                    if (Math.abs(dx) > 100 && Math.abs(vX) > 100) {
                        if (dx > 0) {
                            if (curIdx[0] > 0) {
                                curIdx[0]--;
                                loadFullImage(dialog, root, imageView, progress, files.get(curIdx[0]).file);
                                updateUI.run();
                            }
                        } else {
                            if (curIdx[0] < files.size() - 1) {
                                curIdx[0]++;
                                loadFullImage(dialog, root, imageView, progress, files.get(curIdx[0]).file);
                                updateUI.run();
                            }
                        }
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if (imageView.getCurrentZoom() == imageView.getMinZoom())
                        imageView.setZoom(imageView.getMaxZoom() / 2, e.getX(), e.getY());
                    else
                        imageView.setZoom(imageView.getMinZoom(), e.getX(), e.getY());
                    return true;
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    dialog.dismiss();
                    return true;
                }
            });

            imageView.setOnTouchListener((v, event) -> gesture.onTouchEvent(event));
            loadFullImage(dialog, root, imageView, progress, img);
            dialog.setContentView(root);
            dialog.show();
        } catch (Exception e) {
            XposedBridge.log("显示放大图片失败: " + e.getMessage());
        }
    }

    private static void loadFullImage(Dialog dialog, FrameLayout root, TouchImageView iv, ProgressBar pb, File img) {
        pb.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 1;
                final Bitmap bmp = BitmapFactory.decodeFile(img.getAbsolutePath(), opts);
                new Handler(Looper.getMainLooper()).post(() -> {
                    pb.setVisibility(View.GONE);
                    if (bmp != null) {
                        iv.setImageBitmap(bmp);
                        iv.setZoom(iv.getMinZoom(), iv.getWidth() / 2, iv.getHeight() / 2);
                    } else {
                        TextView err = new TextView(dialog.getContext());
                        err.setText("无法加载图片");
                        err.setTextColor(Color.WHITE);
                        err.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        err.setGravity(Gravity.CENTER);
                        root.removeAllViews();
                        root.addView(err);
                    }
                });
            } catch (OutOfMemoryError e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    pb.setVisibility(View.GONE);
                    Bitmap thumb = createThumbnail(img.getAbsolutePath(), root.getWidth(), root.getHeight());
                    if (thumb != null) iv.setImageBitmap(thumb);
                    else {
                        TextView err = new TextView(dialog.getContext());
                        err.setText("图片太大，无法加载");
                        err.setTextColor(Color.WHITE);
                        err.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        err.setGravity(Gravity.CENTER);
                        root.removeAllViews();
                        root.addView(err);
                    }
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> pb.setVisibility(View.GONE));
            }
        }).start();
    }

    private static Bitmap createThumbnail(String path, int reqW, int reqH) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, opts);
            opts.inSampleSize = calcSampleSize(opts, reqW, reqH);
            opts.inJustDecodeBounds = false;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(path, opts);
        } catch (OutOfMemoryError e) {
            try {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, opts);
                opts.inSampleSize = calcSampleSize(opts, reqW / 2, reqH / 2);
                opts.inJustDecodeBounds = false;
                opts.inPreferredConfig = Bitmap.Config.RGB_565;
                return BitmapFactory.decodeFile(path, opts);
            } catch (Exception ex) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static int calcSampleSize(BitmapFactory.Options opts, int reqW, int reqH) {
        int h = opts.outHeight, w = opts.outWidth;
        int sample = 1;
        if (h > reqH || w > reqW) {
            int halfH = h / 2, halfW = w / 2;
            while ((halfH / sample) >= reqH && (halfW / sample) >= reqW) sample *= 2;
        }
        return sample;
    }

    private static Drawable createButtonBg(Context ctx, int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(8));
        return d;
    }

    // ==================== 工具 ====================
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

    private static boolean shouldBlockHook(Context ctx, String pkg) {
        File priv = new File(ctx.getExternalFilesDir(null), ".Kuaisnap/config.json");
        if (priv.exists() && hasBlockFlag(priv)) return true;
        File pub = new File("/storage/emulated/0/.Kuaisnap/config.json");
        return pub.exists() && hasBlockFlag(pub);
    }

    private static boolean hasBlockFlag(File f) {
        try {
            String json = new String(readAll(f));
            return new JSONObject(json).optBoolean("block_hook", false);
        } catch (Exception e) {
            return false;
        }
    }

    private static void showBlockedToast(final Context ctx) {
        if (hasShownBlockedToast) return;
        hasShownBlockedToast = true;
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(ctx, "检测到模块非法，Hook功能被禁用", Toast.LENGTH_LONG).show());
    }

    private static int dp(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, Context.getResources().getDisplayMetrics());
    }

    // ==================== 内部类 ====================
    static class FileInfo {
        File file;
        long createTime;
        boolean selected;
    }

    public static class TouchImageView extends ImageView {
        private Matrix matrix = new Matrix();
        private Matrix saved = new Matrix();
        private static final int NONE = 0, DRAG = 1, ZOOM = 2;
        private int mode = NONE;
        private PointF start = new PointF();
        private PointF mid = new PointF();
        private float oldDist = 1f;
        private float minScale = 1f, maxScale = 4f;
        private ScaleGestureDetector scaleDetector;
        private GestureDetector gestureDetector;

        public TouchImageView(Context ctx) { super(ctx); init(ctx); }
        public TouchImageView(Context ctx, android.util.AttributeSet attrs) { super(ctx, attrs); init(ctx); }

        private void init(Context ctx) {
            setClickable(true);
            scaleDetector = new ScaleGestureDetector(ctx, new ScaleListener());
            gestureDetector = new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if (getCurrentZoom() == minScale) setZoom(maxScale / 2, e.getX(), e.getY());
                    else setZoom(minScale, e.getX(), e.getY());
                    return true;
                }
            });
            matrix.setTranslate(1f, 1f);
            setImageMatrix(matrix);
            setScaleType(ScaleType.MATRIX);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            gestureDetector.onTouchEvent(event);
            scaleDetector.onTouchEvent(event);
            PointF curr = new PointF(event.getX(), event.getY());
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    saved.set(matrix);
                    start.set(curr);
                    mode = DRAG;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = spacing(event);
                    if (oldDist > 10f) {
                        saved.set(matrix);
                        midPoint(mid, event);
                        mode = ZOOM;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        matrix.set(saved);
                        matrix.postTranslate(curr.x - start.x, curr.y - start.y);
                    } else if (mode == ZOOM) {
                        float newDist = spacing(event);
                        if (newDist > 10f) {
                            matrix.set(saved);
                            float scale = newDist / oldDist;
                            matrix.postScale(scale, scale, mid.x, mid.y);
                        }
                    }
                    break;
            }
            fixScaleAndTranslate();
            setImageMatrix(matrix);
            return true;
        }

        private void fixScaleAndTranslate() {
            float[] m = new float[9];
            matrix.getValues(m);
            float sx = m[Matrix.MSCALE_X], sy = m[Matrix.MSCALE_Y];
            float tx = m[Matrix.MTRANS_X], ty = m[Matrix.MTRANS_Y];
            if (sx < minScale) matrix.postScale(minScale / sx, minScale / sy, getWidth() / 2, getHeight() / 2);
            else if (sx > maxScale) matrix.postScale(maxScale / sx, maxScale / sy, getWidth() / 2, getHeight() / 2);
            matrix.getValues(m);
            tx = m[Matrix.MTRANS_X];
            ty = m[Matrix.MTRANS_Y];
            if (getDrawable() != null) {
                float w = getDrawable().getIntrinsicWidth() * m[Matrix.MSCALE_X];
                float h = getDrawable().getIntrinsicHeight() * m[Matrix.MSCALE_Y];
                if (tx > 0) tx = 0;
                else if (tx + w < getWidth()) tx = getWidth() - w;
                if (ty > 0) ty = 0;
                else if (ty + h < getHeight()) ty = getHeight() - h;
                matrix.postTranslate(tx - m[Matrix.MTRANS_X], ty - m[Matrix.MTRANS_Y]);
            }
        }

        private float spacing(MotionEvent e) {
            float x = e.getX(0) - e.getX(1);
            float y = e.getY(0) - e.getY(1);
            return (float) Math.sqrt(x * x + y * y);
        }

        private void midPoint(PointF p, MotionEvent e) {
            p.set((e.getX(0) + e.getX(1)) / 2, (e.getY(0) + e.getY(1)) / 2);
        }

        private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
            @Override
            public boolean onScale(ScaleGestureDetector d) {
                float sf = d.getScaleFactor();
                float[] m = new float[9];
                matrix.getValues(m);
                float sx = m[Matrix.MSCALE_X];
                if ((sx < maxScale || sf < 1f) && (sx > minScale || sf > 1f))
                    matrix.postScale(sf, sf, d.getFocusX(), d.getFocusY());
                return true;
            }
        }

        public float getCurrentZoom() {
            float[] m = new float[9];
            matrix.getValues(m);
            return m[Matrix.MSCALE_X];
        }
        public float getMinZoom() { return minScale; }
        public float getMaxZoom() { return maxScale; }
        public void setZoom(float scale, float fx, float fy) {
            float[] m = new float[9];
            matrix.getValues(m);
            float cur = m[Matrix.MSCALE_X];
            matrix.postScale(scale / cur, scale / cur, fx, fy);
            fixScaleAndTranslate();
            setImageMatrix(matrix);
        }
    }
}