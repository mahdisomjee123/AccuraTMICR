//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.checkem;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

public class CheckEm {
    private static final String TAG = CheckEm.class.getCanonicalName();
    private static CheckEm sharedInstance;
    String routingNumberResult;
    String accountNumberResult;
    LayoutStrategy layoutStrategy;

    public static String getCheckEmString() {
        return "Check Em";
    }

    public static CheckEm getSharedInstance() {
        return sharedInstance = sharedInstance == null ? new CheckEm() : sharedInstance;
    }

    private CheckEm() {
    }

    public void checkEm(Activity activity) {
        if (activity == null) {
            this.log("Null activity.");
        } else if (!activity.getPackageManager().hasSystemFeature("android.hardware.camera")) {
            this.log("No camera.");
        } else if (!TesseractDataManager.ensureTesseractData(activity)) {
            this.log("Failed to write Tesseract data.");
        } else {
//            Intent intent = new Intent(activity, MicrScanActivity.class);
//            activity.startActivity(intent);
        }
    }

    public String getRoutingNumberResult() {
        return this.routingNumberResult;
    }

    public String getAccountNumberResult() {
        return this.accountNumberResult;
    }

    public void setLayoutStrategy(LayoutStrategy layoutStrategy) {
        this.layoutStrategy = layoutStrategy;
    }

    public void clearResults() {
        this.routingNumberResult = null;
        this.accountNumberResult = null;
    }

    private boolean checkCameraHardware(Activity activity) {
        return activity.getPackageManager().hasSystemFeature("android.hardware.camera");
    }

    private void log(String string) {
        Log.d(TAG, string);
    }

    interface LayoutStrategy {
        int getLayoutResId();

        int getPreviewFrameLayoutId();

        int getRoutingNumberTextViewId();

        int getAccountNumberTextViewId();

        int getOkButtonId();

        double getScanWidthRatio();

        double getScanHeightRatio();

        int getMaskColor();
    }
}
