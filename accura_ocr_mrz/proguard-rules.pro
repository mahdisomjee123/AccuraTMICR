# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Android\SDK\SDK/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
# http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
# public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.**
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class com.accurascan.ocr.mrz.motiondetection.data.GlobalData
-keep public class com.accurascan.ocr.mrz.util.AccuraLog{
    isLogEnable();
    enableLogs(...);
    refreshLogfile(...);
    loge(...);
}
-keep public class com.docrecog.scan.RecogEngine{
    public static final <fields>;
    native <methods>;
    public int setBlurPercentage(...);
    public int setFaceBlurPercentage(...);
    public int setGlarePercentage(...);
    public int isCheckPhotoCopy(...);
    public int SetHologramDetection(...);
    public int setLowLightTolerance(...);
    public int setMotionThreshold(...);
    public void setDialog(...);
    initEngine(...);
    getCardList(...);
}