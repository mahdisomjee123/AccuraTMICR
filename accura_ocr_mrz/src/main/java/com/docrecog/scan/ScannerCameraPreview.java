package com.docrecog.scan;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.accurascan.ocr.mrz.R;
import com.accurascan.ocr.mrz.detector.BarcodeHelper;
import com.accurascan.ocr.mrz.detector.MyBardCodeDetector;
import com.accurascan.ocr.mrz.detector.MyFaceDetector;
import com.accurascan.ocr.mrz.model.InitModel;
import com.accurascan.ocr.mrz.model.OcrData;
import com.accurascan.ocr.mrz.model.PDF417Data;
import com.accurascan.ocr.mrz.motiondetection.data.GlobalData;
import com.accurascan.ocr.mrz.util.AccuraLog;
import com.accurascan.ocr.mrz.util.BitmapUtil;
import com.accurascan.ocr.mrz.util.Util;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;

abstract class ScannerCameraPreview /*extends SurfaceView implements SurfaceHolder.Callback */ {
    private static final String TAG = ScannerCameraPreview.class.getSimpleName();
    private final RecogEngine recogEngine;
    protected CameraSource cameraSource;
    private DisplayMetrics displayMetrics;
    public Camera camera;
    private int facing = 0;
    public int barcodeFormat = Barcode.ALL_FORMATS;
    private Context mContext;
    private PDF417Data pdf417Data;
    private boolean isDone = false;
    public int countryId = 0;
    public RecogType barcodeType = null;
    protected Preview preview;
    protected CameraSourcePreview cameraSourcePreview;
    protected ViewGroup cameraContainer;
    private SurfaceHolder mSurfaceHolder;
    private boolean isSelection = false;
    private BarcodeDetector barcodeDetector;
    private MyBardCodeDetector myBarcodedetecter;
    private FaceDetector detector;
    private MyFaceDetector myFaceDetector;
    private int scanSide = 0; // 0 for front side and 1 for back side
    private boolean isInitialized;
    private static final long CLICK_TIME_INTERVAL = 2500;
    private long mLastClickTime;
    private boolean previewIsRunning;
    protected int cardWidth;
    protected int cardHeight;
//    private ProgressBar progressBar;


    public boolean isInitialized() {
        return isInitialized;
    }

    public void setInitialized(boolean initialized) {
        isInitialized = initialized;
    }

    protected abstract void onScannedSuccess(OcrData rawResult);

    protected abstract void onScannedPDF417(PDF417Data rawResult);

    protected abstract void onError(String s);

    protected abstract void onUpdate(int s, String feedBackMessage, boolean isFlip);

    protected ScannerCameraPreview(Context context) {
        this.mContext = context;
        recogEngine = new RecogEngine();
        this.mLastClickTime = System.currentTimeMillis();
    }

    ScannerCameraPreview setFacing(int facing){
        AccuraLog.loge(TAG, "Camera " + facing);
        this.facing = facing;
        return this;
    }

    public void updateFormat(int barcodeFormat) {
        if (barcodeType == RecogType.BARCODE && isSelection) {
            this.barcodeFormat = barcodeFormat;
            addScanner(this.mContext);
        }
    }

    public void setFrontSide() {
        scanSide = 0;
        if (isInitialized() && barcodeType == RecogType.PDF417) {
            barcodeFormat = Barcode.PDF417;
            initFrontCamera();
            onUpdate(RecogEngine.SCAN_TITLE_MRZ_PDF417_FRONT, null,false);
        }
    }

    public void setBackSide() {
        scanSide = 1;
        if (isInitialized() && barcodeType == RecogType.PDF417) {
            onUpdate(RecogEngine.SCAN_TITLE_MRZ_PDF417_BACK, null,true);

            Runnable runnable = new Runnable() {
                public void run() {
                    addScanner(mContext);
                }
            };
            new Handler().postDelayed(runnable, 950);
        }
    }

    public boolean isBackSide() {
        if (barcodeType == RecogType.PDF417) {
            return true;
        }
        return false;
    }

    protected void initializeScanner(Context context, int countryId) {
        isDone = false;
        preview = new Preview(mContext);
        cameraSourcePreview = new CameraSourcePreview(this, preview, mContext);
        this.cameraContainer.addView(cameraSourcePreview);
//        progressBar = new ProgressBar(mContext, null, android.R.attr.progressBarStyleLarge);
//        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(100, 100);
//        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
//        this.cameraContainer.addView(this.progressBar, lp);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                InitModel initModel = recogEngine.initScanner(context, countryId);
                AccuraLog.loge(TAG, "InitializeS");
                if (initModel != null) {
                    if (initModel.getResponseCode() == 1) {
                        DisplayMetrics dm = context.getResources().getDisplayMetrics();

                        if (BitmapUtil.isPortraitMode(context)) {
                            cardWidth = dm.widthPixels - 20;
                            cardHeight = (dm.heightPixels) / 3;
                        } else {
                            cardHeight = (dm.heightPixels) - (int) (100 * dm.density);
                            cardWidth = (int) (cardHeight / 0.69);
                        }
                        setInitialized(true);
                        startScan();
                    } else {
                        setInitialized(false);
                        onError(initModel.getResponseMessage());
                    }
//                    handler.sendEmptyMessage(1);
                } else {
                    setInitialized(false);
                    onError("Failed");
//                    handler.sendEmptyMessage(0);
                }
            }
        }, 100);
    }

    private void startScan() {
        pdf417Data = new PDF417Data();
        displayMetrics = this.mContext.getResources().getDisplayMetrics();
        if (barcodeType == RecogType.BARCODE) {
            addScanner(this.mContext);
            onUpdate(RecogEngine.SCAN_TITLE_DEFAULT, null,false);
            isSelection = true;
        } else if (barcodeType == RecogType.PDF417) {
            barcodeFormat = Barcode.PDF417;
            if (scanSide > 0) {
                addScanner(this.mContext);
                onUpdate(RecogEngine.SCAN_TITLE_MRZ_PDF417_BACK, null,false);
            } else {
                initFrontCamera();
                onUpdate(RecogEngine.SCAN_TITLE_MRZ_PDF417_FRONT, null,false);
            }
        }
    }

    private void initFrontCamera() {
        AccuraLog.loge(TAG, "Front data");
        isDone = false;
        detector = new FaceDetector.Builder(mContext)
                .setTrackingEnabled(false)
                .build();
        myFaceDetector = new MyFaceDetector(detector);
        myFaceDetector.setFrameParam(cardWidth, cardHeight,
                cameraSourcePreview.getChildXOffset(), cameraSourcePreview.getChildYOffset(),
                cameraSourcePreview.getChildWidth(), cameraSourcePreview.getChildHeight());

        cameraSource = new CameraSource.Builder(mContext, myFaceDetector)
                .setFacing(facing)
                .setAutoFocusEnabled(true)
                .setRequestedPreviewSize(displayMetrics.heightPixels, displayMetrics.widthPixels)
                .build();

        camera = getCamera(cameraSource);
        cameraSourcePreview.requestLayout();

//        getHolder().addCallback(this);
        myFaceDetector.setProcessor(new Detector.Processor<Face>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Face> detections) {
                if (GlobalData.isPhoneInMotion()) {
                    onUpdate(RecogEngine.SCAN_TITLE_DEFAULT, RecogEngine.ACCURA_ERROR_CODE_MOTION, false);
                    return;
                }
                SparseArray<Face> faces = detections.getDetectedItems();
                if (faces.size() > 0) {
                    Face face = faces.valueAt(0);
                    if (face != null) {
                        Bitmap source = myFaceDetector.getBitmap();


                        if (recogEngine.checkValid(source)) {
                            Bitmap faceBitmap1 = Bitmap.createBitmap(source,
                                    (int) face.getPosition().x,
                                    (int) face.getPosition().y,
                                    (int) face.getWidth(),
                                    (int) face.getHeight());
                            if (faceBitmap1 != null) {
                                pdf417Data.faceBitmap = faceBitmap1;
                                pdf417Data.docFrontBitmap = source.copy(Bitmap.Config.ARGB_8888, true);
                                try {
                                    if (!source.isRecycled()) source.recycle();
                                    if (myFaceDetector.getBitmap() != null && !myFaceDetector.getBitmap().isRecycled())
                                        myFaceDetector.getBitmap().recycle();
                                    detector.release();
                                    myFaceDetector.release();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                preview.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        stopCameraPreview();
                                        if (!isDone) {
                                            isDone = true;
                                            onScannedPDF417(pdf417Data);
                                        }
//                                        onUpdate(RecogEngine.SCAN_TITLE_MRZ_PDF417_BACK, true);
//
//                                        Runnable runnable = new Runnable() {
//                                            public void run() {
//                                                addScanner(mContext);
//                                            }
//                                        };
//                                        new Handler().postDelayed(runnable, 950);
                                    }
                                });

                            }
                        }
                    }
                }
            }
        });


    }

    private void addScanner(Context context) {
        AccuraLog.loge(TAG, "Scanner");
        if (barcodeDetector != null) {
            barcodeDetector.release();
        }
        barcodeDetector = new BarcodeDetector.Builder(context)
                .setBarcodeFormats(barcodeFormat).build();

        myBarcodedetecter = new MyBardCodeDetector(barcodeDetector);
        myBarcodedetecter.setFrameParam(cardWidth, cardHeight,
                cameraSourcePreview.getChildXOffset(), cameraSourcePreview.getChildYOffset(),
                cameraSourcePreview.getChildWidth(), cameraSourcePreview.getChildHeight());

        CameraSource.Builder builder = new CameraSource.Builder(context, myBarcodedetecter)
                .setAutoFocusEnabled(true)
                .setFacing(facing == 1? CameraSource.CAMERA_FACING_FRONT : CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(displayMetrics.heightPixels, displayMetrics.widthPixels);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder = builder.setFocusMode(
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        cameraSource = builder.build();
        cameraSourcePreview.requestLayout();
        myBarcodedetecter.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {

                /*called when detect data*/
                if (GlobalData.isPhoneInMotion()) {
                    onUpdate(RecogEngine.SCAN_TITLE_DEFAULT, RecogEngine.ACCURA_ERROR_CODE_MOTION, false);
                    return;
                }

                final SparseArray<Barcode> qrcode = detections.getDetectedItems();

                if (qrcode.size() != 0) {

                    preview.post(new Runnable() {
                        @Override
                        public void run() {
                            Bitmap bitmap = null;

                            long now = System.currentTimeMillis();
                            if (now - mLastClickTime < CLICK_TIME_INTERVAL) return;
                            if (myBarcodedetecter.getBitmap() != null && !myBarcodedetecter.getBitmap().isRecycled()) {
                                stopCameraPreview();
                                bitmap = myBarcodedetecter.getBitmap().copy(Bitmap.Config.ARGB_8888, true);
                                myBarcodedetecter.getBitmap().recycle();
                            }
                            Barcode barcode = qrcode.valueAt(0);
                            String output = barcode.rawValue;
                            Log.e(TAG, "run: " + barcode.valueFormat+" , " + barcode.isRecognized);
//                            Barcode.DriverLicense driverLicense = qrcode.valueAt(0).driverLicense;
                            if (bitmap != null && !bitmap.isRecycled() && !isDone) {
                                isDone = true;
                                if (BarcodeHelper.extractScanResult(output, pdf417Data)) {
                                    if (barcodeType == RecogType.BARCODE) {
                                        pdf417Data.docFrontBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                                    } else if (barcodeType == RecogType.PDF417) {
                                        pdf417Data.docBackBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                                    }
                                    bitmap.recycle();
                                    onScannedPDF417(pdf417Data);
                                } else {
                                    try {
                                        OcrData ocrData = new OcrData();
                                        ocrData.setFrontimage(bitmap.copy(Bitmap.Config.ARGB_8888, true));
                                        JSONObject mapObject = new JSONObject();
                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("type",1);
                                        jsonObject.put("key", "Barcode");
                                        jsonObject.put("key_data", output);
                                        mapObject.put("ocr_data",new JSONArray().put(jsonObject));
                                        mapObject.put("is_face",0);
                                        mapObject.put("card_side","Front Side");
                                        OcrData.MapData mapData = new Gson().fromJson(mapObject.toString(), OcrData.MapData.class);
                                        ocrData.setFrontData(mapData);
                                        bitmap.recycle();
                                        onScannedSuccess(ocrData);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                mLastClickTime = System.currentTimeMillis();
                            }
                        }
                    });
                }
            }
        });
        startCameraPreview();
    }

    private Camera getCamera(@NonNull CameraSource cameraSource) {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();

        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    Camera camera = (Camera) field.get(cameraSource);
                    if (camera != null) {
                        return camera;
                    }

                    return null;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                break;
            }
        }

        return null;
    }

    public void setFrameParam(int childXOffset, int childYOffset, int childWidth, int childHeight){
        if (myBarcodedetecter!=null) {
            myBarcodedetecter.setFrameParam(0,0, childXOffset, childYOffset,childWidth, childHeight);
        }
        if (myFaceDetector!=null) {
            myFaceDetector.setFrameParam(0,0, childXOffset, childYOffset,childWidth, childHeight);
        }
    }

    class Preview extends SurfaceView implements SurfaceHolder.Callback {

        public Preview(Context context) {
            super(context);
            SurfaceHolder mHolder = getHolder();
            mHolder.addCallback(this);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            previewIsRunning = false;
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                AccuraLog.loge(TAG, "Camera Permission not granted");
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            mSurfaceHolder = holder;

            try {
                startCameraPreview();
            } catch (RuntimeException e) {
                onError(mContext.getString(R.string.cannot_connect_camera));
                if (mContext instanceof Activity) {
                    Util.showErrorAndFinish((Activity) mContext, R.string.cannot_connect_camera);
                }
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            stopCameraPreview();
        }
    }

    protected void setCamera() {
        if (camera == null) {
            camera = getCamera(cameraSource);
        }
    }

    protected void startCameraPreview() {
//        progressBar.setVisibility(View.GONE);
//        getHolder().addCallback(this);
        isDone = false;
        try {
            if (!previewIsRunning && cameraSource != null && mSurfaceHolder != null) {
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                AccuraLog.loge(TAG, "start Preview");
                cameraSource.start(mSurfaceHolder);
                previewIsRunning = true;
            }
        } catch (Exception e) {
            onError(mContext.getString(R.string.cannot_connect_camera));
            if (mContext instanceof Activity) {
                Util.showErrorAndFinish((Activity) mContext, R.string.cannot_connect_camera);
            }
            e.printStackTrace();
        }
//        try {
//            setCamera();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    protected void destroy(){
        if (barcodeDetector != null) barcodeDetector.release();
        if (myBarcodedetecter != null) myBarcodedetecter.release();
        if (detector != null) detector.release();
        if (myFaceDetector != null) myFaceDetector.release();
        stopCameraPreview();
    }

    protected void stopCameraPreview() {
        if (previewIsRunning && cameraSource != null) {
            AccuraLog.loge(TAG, "stop Preview");
            cameraSource.stop();
            previewIsRunning = false;
        }
//        getHolder().removeCallback(this);
    }

    public void isReset(boolean b) {
        Util.logd(TAG, "isReset" + b + this.isDone);
        if (this.isDone && b) {
            this.isDone = false;
        }
    }

    public void restartPreview(){
        stopCameraPreview();
        if (barcodeType == RecogType.BARCODE) {
            addScanner(this.mContext);
            isSelection = true;
        } else if (barcodeType == RecogType.PDF417) {
            barcodeFormat = Barcode.PDF417;
            if (scanSide > 0) {
                addScanner(this.mContext);
            } else {
                initFrontCamera();
                startCameraPreview();
            }
        }
    }
}
