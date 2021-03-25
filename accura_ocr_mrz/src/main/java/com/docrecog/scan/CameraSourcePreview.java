package com.docrecog.scan;

import android.content.Context;
import android.content.res.Configuration;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import com.accurascan.ocr.mrz.util.Util;
import com.google.android.gms.common.images.Size;

public class CameraSourcePreview extends FrameLayout {
    private static final String TAG = CameraSourcePreview.class.getSimpleName();
    private final Context context;
    private final OcrCameraPreview ocrCameraPreview;
    private final ScannerCameraPreview scannerCameraPreview;
    private final SurfaceView surfaceView;
    private int childXOffset = 0;
    private int childYOffset = 0;
    private int childWidth = 0;
    private int childHeight = 0;

    public CameraSourcePreview(OcrCameraPreview ocrCameraPreview, Context context) {
        super(context);
        this.scannerCameraPreview = null;
        this.context = context;

        surfaceView = new SurfaceView(context);
        surfaceView.getHolder().addCallback(new SurfaceCallback());
        setOnTouchListener(new OnSurfaceTouchListener());
        addView(surfaceView);
        this.ocrCameraPreview = ocrCameraPreview;
    }

    public CameraSourcePreview(ScannerCameraPreview scannerCameraPreview, SurfaceView surfaceView, Context context) {
        super(context);
        this.ocrCameraPreview = null;
        this.context = context;

        this.surfaceView = surfaceView;
        addView(surfaceView);
        this.scannerCameraPreview = scannerCameraPreview;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // To Set preview for camera ratio
        // The isPreviewSet will be false then update surface view to maintain camera ratio for all device
        int width = 0, height = 0;
        if (ocrCameraPreview != null) {
            width = ocrCameraPreview.getPreviewWidth();
            height = ocrCameraPreview.getPreviewHeight();
            if (ocrCameraPreview.mCameraDevice != null) {
                Size size = ocrCameraPreview.getPreviewSize();
                if (size != null) {
                    width = (int) size.getWidth();
                    height = (int) size.getHeight();
                }
            }
        } else if (scannerCameraPreview != null) {
            width = 1280;
            height = 720;
            if (scannerCameraPreview.cameraSource != null) {
                Size size = scannerCameraPreview.cameraSource.getPreviewSize();
                if (size != null) {
                    width = (int) size.getWidth();
                    height = (int) size.getHeight();
                }
            }
        }

        // Swap width and height sizes when in portrait, since it will be rotated 90 degrees
        if (isPortraitMode()) {
            int tmp = width;
            width = height;
            height = tmp;
        }

        final int layoutWidth = right - left;
        final int layoutHeight = bottom - top;

        // Computes height and width for potentially doing fit width.
        int childWidth;
        int childHeight;
        int childXOffset = 0;
        int childYOffset = 0;
        float widthRatio = (float) layoutWidth / (float) width;
        float heightRatio = (float) layoutHeight / (float) height;

        // To fill the view with the camera preview, while also preserving the correct aspect ratio,
        // it is usually necessary to slightly oversize the child and to crop off portions along one
        // of the dimensions.  We scale up based on the dimension requiring the most correction, and
        // compute a crop offset for the other dimension.
        if (widthRatio > heightRatio) {
            childWidth = layoutWidth;
            childHeight = (int) ((float) height * widthRatio);
            childYOffset = (childHeight - layoutHeight) / 2;
        } else {
            childWidth = (int) ((float) width * heightRatio);
            childHeight = layoutHeight;
            childXOffset = (childWidth - layoutWidth) / 2;
        }
        this.childXOffset = childXOffset;
        this.childYOffset = childYOffset;
        this.childWidth = childWidth;
        this.childHeight = childHeight;
        if (scannerCameraPreview != null) {
            scannerCameraPreview.setFrameParam(this.childXOffset, this.childYOffset,this.childWidth, this.childHeight);
        }
        Util.logd(TAG, "onLayout: (" + childXOffset + "," + childYOffset + ") - (" + childWidth + "x" + childHeight + ")");
        for (int i = 0; i < getChildCount(); ++i) {
            getChildAt(i).layout(
                    -1 * childXOffset, -1 * childYOffset,
                    childWidth - childXOffset, childHeight - childYOffset);
        }
    }

    public int getChildXOffset() {
        return childXOffset;
    }

    public int getChildYOffset() {
        return childYOffset;
    }

    public int getChildWidth() {
        return childWidth;
    }

    public int getChildHeight() {
        return childHeight;
    }

    public SurfaceHolder getHolder() {
        return surfaceView.getHolder();
    }

    private class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Util.logd(TAG, "surfaceCreated: ");
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Util.logd(TAG, "surfaceChanged: ");
// Make sure we have a surface in the holder before proceeding.
            if (holder.getSurface() == null) {
                Util.logd(TAG, "holder.getSurface() == null");
                return;
            }
            ocrCameraPreview.startIfReady(holder);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Util.logd(TAG, "surfaceDestroyed: ");
            ocrCameraPreview.stopPreview();
        }
    }

    private class OnSurfaceTouchListener implements OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return ocrCameraPreview.onTouchView(v, event);
        }
    }

    private boolean isPortraitMode() {
        int orientation = context.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return false;
        }
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true;
        }
        return false;
    }
}