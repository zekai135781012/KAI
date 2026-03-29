package com.example.kai.taptap;

import de.robv.android.xposed.XposedHelpers;

/**
 * TapTap SDK 检测工具
 */
public class TapTapUtils {
    
    // TapTap SDK 特征类
    private static final String[] TAPTAP_CLASSES = {
        "com.taptap.sdk.license.TapTapLicense",
        "com.taptap.pay.sdk.library.TapTapLicense",
        "com.taptap.pay.sdk.library.TapPurchase",
        "com.taptap.sdk.license.internal.TapLicenseInternal",
        "com.taptap.pay.sdk.library.TapLicenseHelper",
        "com.tapsdk.antiaddiction.settings.AntiAddictionSettings"
    };
    
    // 心动 XDSDK 特征类
    private static final String[] XDSDK_CLASSES = {
        "com.xd.xdsdk.XDSDK",
        "com.xd.sdk.core.XDCore"
    };
    
    public static boolean hasTapTapSdk(ClassLoader cl) {
        for (String className : TAPTAP_CLASSES) {
            if (XposedHelpers.findClassIfExists(className, cl) != null) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean hasXDSdk(ClassLoader cl) {
        for (String className : XDSDK_CLASSES) {
            if (XposedHelpers.findClassIfExists(className, cl) != null) {
                return true;
            }
        }
        return false;
    }
}
