# Keep LSPosed entry point and method hooks so R8 never strips them
-keep class * implements de.robv.android.xposed.IXposedHookLoadPackage {
    public <init>();
    public void handleLoadPackage(de.robv.android.xposed.callbacks.XC_LoadPackage$LoadPackageParam);
}
-keep class * extends de.robv.android.xposed.XC_MethodHook { *; }
-dontwarn de.robv.android.xposed.**
