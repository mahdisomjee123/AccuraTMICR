////
//// Source code recreated from a .class file by IntelliJ IDEA
//// (powered by FernFlower decompiler)
////
//
//package com.checkem;
//
//import android.app.Activity;
//import android.graphics.Bitmap;
//import android.graphics.Point;
//import android.graphics.SurfaceTexture;
//import android.hardware.Camera;
//import android.os.AsyncTask;
//import android.os.Bundle;
//import android.view.Display;
//import android.view.TextureView;
//import android.view.View;
//import android.widget.FrameLayout;
//import android.widget.TextView;
//
//import com.accurascan.ocr.mrz.R.layout;
//import com.accurascan.ocr.mrz.R.id;
//import com.accurascan.ocr.mrz.R.string;
//
//import java.io.IOException;
//import java.util.Iterator;
//import java.util.List;
//
//public class MicrScanActivity extends Activity implements TextureView.SurfaceTextureListener {
//    private CheckEm.LayoutStrategy layoutStrategy;
//    private FrameLayout previewFrameLayout;
//    private TextView routingNumberTextView;
//    private TextView accountNumberTextView;
//    private View okButton;
//    private Camera camera;
//    private TextureView textureView;
//    double scanWidthRatio;
//    double scanHeightRatio;
//    MicrOcrUtil micrOcrUtil;
//    MicrOcrUtil.MicrOcrResult result;
//    ScanAsyncTask scanAsyncTask;
//
//    public MicrScanActivity() {
//    }
//
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        this.layoutStrategy = CheckEm.getSharedInstance().layoutStrategy;
//        if (this.layoutStrategy == null) {
//            this.layoutStrategy = this.produceDefaultLayoutStrategy();
//        }
//
//        this.setContentView(this.layoutStrategy.getLayoutResId());
//        this.previewFrameLayout = (FrameLayout)this.findViewById(this.layoutStrategy.getPreviewFrameLayoutId());
//        this.routingNumberTextView = (TextView)this.findViewById(this.layoutStrategy.getRoutingNumberTextViewId());
//        this.accountNumberTextView = (TextView)this.findViewById(this.layoutStrategy.getAccountNumberTextViewId());
//        this.okButton = this.findViewById(this.layoutStrategy.getOkButtonId());
//        this.textureView = new TextureView(this);
//        this.textureView.setSurfaceTextureListener(this);
//        this.previewFrameLayout.addView(this.textureView);
//        this.okButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                if (MicrScanActivity.this.result != null) {
//                    if (MicrScanActivity.this.result.routingNumber != null) {
//                        CheckEm.getSharedInstance().routingNumberResult = MicrScanActivity.this.result.routingNumber;
//                    }
//
//                    if (MicrScanActivity.this.result.accountNumber != null) {
//                        CheckEm.getSharedInstance().accountNumberResult = MicrScanActivity.this.result.accountNumber;
//                    }
//                }
//
//                MicrScanActivity.this.finish();
//            }
//        });
//        this.scanWidthRatio = this.layoutStrategy.getScanWidthRatio();
//        if (this.scanWidthRatio > 1.0 || this.scanWidthRatio <= 0.0) {
//            this.scanWidthRatio = 1.0;
//        }
//
//        this.scanHeightRatio = this.layoutStrategy.getScanHeightRatio();
//        if (this.scanHeightRatio > 1.0 || this.scanHeightRatio <= 0.0) {
//            this.scanHeightRatio = 1.0;
//        }
//
//        Display display = this.getWindowManager().getDefaultDisplay();
//        Point size = new Point();
//        display.getSize(size);
//        int screenWidth = size.x;
//        int screenHeight = size.y;
//        int verticalBorderHeight = (int)((double)screenHeight * 0.5 * (1.0 - this.scanHeightRatio));
//        int scanHeight = screenHeight - 2 * verticalBorderHeight;
//        int horizontalBorderWidth = (int)((double)screenWidth * 0.5 * (1.0 - this.scanWidthRatio));
//        int maskColor = this.layoutStrategy.getMaskColor();
//        View topBorder = new View(this);
//        topBorder.setBackgroundColor(maskColor);
//        FrameLayout.LayoutParams topBorderLayoutParams = new FrameLayout.LayoutParams(-1, verticalBorderHeight, 49);
//        this.previewFrameLayout.addView(topBorder, topBorderLayoutParams);
//        View leftBorder = new View(this);
//        leftBorder.setBackgroundColor(maskColor);
//        FrameLayout.LayoutParams leftBorderLayoutParams = new FrameLayout.LayoutParams(horizontalBorderWidth, scanHeight, 19);
//        this.previewFrameLayout.addView(leftBorder, leftBorderLayoutParams);
//        View rightBorder = new View(this);
//        rightBorder.setBackgroundColor(maskColor);
//        FrameLayout.LayoutParams rightBorderLayoutParams = new FrameLayout.LayoutParams(horizontalBorderWidth, scanHeight, 21);
//        this.previewFrameLayout.addView(rightBorder, rightBorderLayoutParams);
//        View bottomBorder = new View(this);
//        bottomBorder.setBackgroundColor(maskColor);
//        FrameLayout.LayoutParams bottomBorderLayoutParams = new FrameLayout.LayoutParams(-1, verticalBorderHeight, 81);
//        this.previewFrameLayout.addView(bottomBorder, bottomBorderLayoutParams);
//        this.clear();
//        this.micrOcrUtil = new MicrOcrUtil(this);
//    }
//
//    protected void onResume() {
//        super.onResume();
//        this.getWindow().setFlags(1024, 1024);
//    }
//
//    protected void onPause() {
//        super.onPause();
//    }
//
//    protected void onStop() {
//        if (this.scanAsyncTask != null) {
//            this.scanAsyncTask.shouldKill = true;
//        }
//
//        super.onStop();
//    }
//
//    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//        this.camera = Camera.open();
//        Camera.Parameters parameters = this.camera.getParameters();
//        Camera.Size previewSize = this.getOptimalPreviewSize(parameters.getSupportedPreviewSizes(), width, height);
//        parameters.setPreviewSize(previewSize.width, previewSize.height);
//        if (this.getPackageManager().hasSystemFeature("android.hardware.camera.flash")) {
//            parameters.setFlashMode("torch");
//        }
//
//        parameters.setFocusMode("continuous-picture");
//        this.camera.setParameters(parameters);
//
//        try {
//            this.camera.setPreviewTexture(surface);
//            this.camera.startPreview();
//        } catch (IOException var7) {
//        }
//
//    }
//
//    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//    }
//
//    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//        this.camera.stopPreview();
//        this.camera.release();
//        return true;
//    }
//
//    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//        if (this.micrOcrUtil != null && !this.micrOcrUtil.isRunning()) {
//            Bitmap bitmap = this.textureView.getBitmap();
//            if (bitmap == null) {
//                return;
//            }
//
//            int bitmapWidth = bitmap.getWidth();
//            int bitmapHeight = bitmap.getHeight();
//            int halfBitmapWidth = bitmapWidth / 2;
//            int halfScanWidth = (int)((double)halfBitmapWidth * this.scanWidthRatio);
//            int halfBitmapHeight = bitmapHeight / 2;
//            int halfScanHeight = (int)((double)halfBitmapHeight * this.scanHeightRatio);
//            int startX = halfBitmapWidth - halfScanWidth;
//            int endX = halfBitmapWidth + halfScanWidth;
//            int startY = halfBitmapHeight - halfScanHeight;
//            int endY = halfBitmapHeight + halfScanHeight;
//            if (startX < 0) {
//                startX = 0;
//            }
//
//            if (endX > bitmapWidth) {
//                endX = bitmapWidth;
//            }
//
//            if (startY < 0) {
//                startY = 0;
//            }
//
//            if (endY > bitmapHeight) {
//                endY = bitmapHeight;
//            }
//
//            int scanWidth = endX - startX;
//            int scanHeight = endY - startY;
//            bitmap = Bitmap.createBitmap(bitmap, startX, startY, scanWidth, scanHeight);
//            this.scanAsyncTask = new ScanAsyncTask();
//            this.scanAsyncTask.execute(new Bitmap[]{bitmap});
//        }
//
//    }
//
//    private CheckEm.LayoutStrategy produceDefaultLayoutStrategy() {
//        return new CheckEm.LayoutStrategy() {
//            public int getLayoutResId() {
//                return layout.activity__micr_scan_activity;
//            }
//
//            public int getPreviewFrameLayoutId() {
//                return id.micr_scan_activity__preview_frame;
//            }
//
//            public int getRoutingNumberTextViewId() {
//                return id.micr_scan_activity__routing_text;
//            }
//
//            public int getAccountNumberTextViewId() {
//                return id.micr_scan_activity__account_text;
//            }
//
//            public int getOkButtonId() {
//                return id.micr_scan_activity__ok_button;
//            }
//
//            public double getScanWidthRatio() {
//                return 0.875;
//            }
//
//            public double getScanHeightRatio() {
//                return 0.25;
//            }
//
//            public int getMaskColor() {
//                return 1073741824;
//            }
//        };
//    }
//
//    private void clear() {
//        this.routingNumberTextView.setText(string.micr_scan__empty_routing);
//        this.accountNumberTextView.setText(string.micr_scan__empty_account);
//        this.okButton.setVisibility(8);
//    }
//
//    private void setRoutingNumberResult(String routingNumberResult) {
//        this.routingNumberTextView.setText(this.getString(string.micr_scan__routing_format, new Object[]{routingNumberResult}));
//    }
//
//    private void setAccountNumberResult(String accountNumberResult) {
//        this.accountNumberTextView.setText(this.getString(string.micr_scan__account_format, new Object[]{accountNumberResult}));
//    }
//
//    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
//        double ASPECT_TOLERANCE = 0.05;
//        double targetRatio = (double)w / (double)h;
//        if (sizes == null) {
//            return null;
//        } else {
//            Camera.Size optimalSize = null;
//            double minDiff = Double.MAX_VALUE;
//            int targetHeight = h;
//            Iterator var12 = sizes.iterator();
//
//            Camera.Size size;
//            while(var12.hasNext()) {
//                size = (Camera.Size)var12.next();
//                double ratio = (double)size.width / (double)size.height;
//                if (!(Math.abs(ratio - targetRatio) > 0.05) && (double)Math.abs(size.height - targetHeight) < minDiff) {
//                    optimalSize = size;
//                    minDiff = (double)Math.abs(size.height - targetHeight);
//                }
//            }
//
//            if (optimalSize == null) {
//                minDiff = Double.MAX_VALUE;
//                var12 = sizes.iterator();
//
//                while(var12.hasNext()) {
//                    size = (Camera.Size)var12.next();
//                    if ((double)Math.abs(size.height - targetHeight) < minDiff) {
//                        optimalSize = size;
//                        minDiff = (double)Math.abs(size.height - targetHeight);
//                    }
//                }
//            }
//
//            return optimalSize;
//        }
//    }
//
//    class ScanAsyncTask extends AsyncTask<Bitmap, Long, MicrOcrUtil.MicrOcrResult> {
//        boolean shouldKill;
//
//        ScanAsyncTask() {
//        }
//
//        protected MicrOcrUtil.MicrOcrResult doInBackground(Bitmap... params) {
//            return MicrScanActivity.this.micrOcrUtil.processBitmap(params[0]);
//        }
//
//        protected void onPostExecute(MicrOcrUtil.MicrOcrResult micrOcrResult) {
//            super.onPostExecute(micrOcrResult);
//            if (this.shouldKill) {
//                MicrScanActivity.this.micrOcrUtil.destroy();
//            } else {
//                MicrScanActivity.this.result = micrOcrResult;
//                MicrScanActivity.this.runOnUiThread(new Runnable() {
//                    public void run() {
//                        if (MicrScanActivity.this.result.routingNumber != null) {
//                            MicrScanActivity.this.setRoutingNumberResult(MicrScanActivity.this.result.routingNumber);
//                        }
//
//                        if (MicrScanActivity.this.result.accountNumber != null) {
//                            MicrScanActivity.this.setAccountNumberResult(MicrScanActivity.this.result.accountNumber);
//                        }
//
//                        if (MicrScanActivity.this.result.routingNumber != null && MicrScanActivity.this.result.accountNumber != null) {
//                            MicrScanActivity.this.okButton.setVisibility(0);
//                        }
//
//                    }
//                });
//            }
//        }
//    }
//}
