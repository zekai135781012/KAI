package com.example.kai.taptap;

import android.content.Context;

/**
 * XDSDK Hook 包装类
 * 实际逻辑在 TapTapHook 中
 */
public class XDSdkHook {
    
    public static void hook(Context context, ClassLoader classLoader) {
        // 直接调用 TapTapHook 中的实现
        // 避免重复代码
        try {
            java.lang.reflect.Method method = TapTapHook.class.getDeclaredMethod(
                "xdTapTap", Context.class, ClassLoader.class);
            method.setAccessible(true);
            method.invoke(null, context, classLoader);
        } catch (Throwable e) {
            // 如果反射失败，说明 xdTapTap 被改名或移除
            // 这里可以放置备用逻辑
            android.util.Log.e("KAI-XDSDK", "Failed to invoke xdTapTap", e);
        }
    }
}
