package com.docrecog.scan;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.accurascan.ocr.mrz.detector.BarcodeHelper;
import com.accurascan.ocr.mrz.detector.MyBardCodeDetector;
import com.accurascan.ocr.mrz.detector.MyFaceDetector;
import com.accurascan.ocr.mrz.model.InitModel;
import com.accurascan.ocr.mrz.model.PDF417Data;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;
import java.lang.reflect.Field;

abstract class ScannerCameraPreview /*extends SurfaceView implements SurfaceHolder.Callback */ {
    private static final String TAG = ScannerCameraPreview.class.getSimpleName();
    private final RecogEngine recogEngine;
    private CameraSource cameraSource;
    private DisplayMetrics displayMetrics;
    public Camera camera;
    public int barcodeFormat = Barcode.ALL_FORMATS;
    private Context mContext;
    private PDF417Data pdf417Data;
    private boolean isDone = false;
    public int countryId = 0;
    public RecogType barcodeType = null;
    protected Preview preview;
    protected ViewGroup cameraContainer;
    private SurfaceHolder mSurfaceHolder;
    private boolean isSelection = false;
    private BarcodeDetector barcodeDetector;
    private MyBardCodeDetector myBarcodedetecter;
    private FaceDetector detector;
    private MyFaceDetector myFaceDetector;
    private int scanSide = 0; // 0 for front side and 1 for back side
    private boolean isInitialized;
//    private ProgressBar progressBar;


    public boolean isInitialized() {
        return isInitialized;
    }

    public void setInitialized(boolean initialized) {
        isInitialized = initialized;
    }

    protected abstract void onScannedSuccess(String rawResult);

    protected abstract void onScannedPDF417(PDF417Data rawResult);

    protected abstract void onError(String s);

    protected abstract void onUpdate(int s, boolean isFlip);

    protected ScannerCameraPreview(Context context) {
        this.mContext = context;
        recogEngine = new RecogEngine();
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
            onUpdate(RecogEngine.SCAN_TITLE_MRZ_PDF417_FRONT, false);
        }
    }

    public void setBackSide() {
        scanSide = 1;
        if (isInitialized() && barcodeType == RecogType.PDF417) {
            onUpdate(RecogEngine.SCAN_TITLE_MRZ_PDF417_BACK, true);

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
        this.cameraContainer.addView(preview);
//        progressBar = new ProgressBar(mContext, null, android.R.attr.progressBarStyleLarge);
//        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(100, 100);
//        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
//        this.cameraContainer.addView(this.progressBar, lp);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                InitModel initModel = recogEngine.initScanner(context, countryId);
                if (initModel != null) {
                    if (initModel.getResponseCode() == 1) {
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
            onUpdate(RecogEngine.SCAN_TITLE_DEFAULT, false);
            isSelection = true;
        } else if (barcodeType == RecogType.PDF417) {
            barcodeFormat = Barcode.PDF417;
            if (scanSide > 0) {
                addScanner(this.mContext);
                onUpdate(RecogEngine.SCAN_TITLE_MRZ_PDF417_BACK, false);
            } else {
                initFrontCamera();
                onUpdate(RecogEngine.SCAN_TITLE_MRZ_PDF417_FRONT, false);
            }
        }
    }

    private void initFrontCamera() {

        detector = new FaceDetector.Builder(mContext)
                .setTrackingEnabled(false)
                .build();
        myFaceDetector = new MyFaceDetector(detector);

        cameraSource = new CameraSource.Builder(mContext, myFaceDetector)
                .setAutoFocusEnabled(true)
                .setRequestedPreviewSize(displayMetrics.heightPixels, displayMetrics.widthPixels)
                .build();

        camera = getCamera(cameraSource);

//        getHolder().addCallback(this);
        myFaceDetector.setProcessor(new Detector.Processor<Face>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Face> detections) {
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
                                        cameraSource.stop();
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
        barcodeDetector = new BarcodeDetector.Builder(context)
                .setBarcodeFormats(barcodeFormat).build();

        myBarcodedetecter = new MyBardCodeDetector(barcodeDetector);

        cameraSource = new CameraSource.Builder(context, myBarcodedetecter)
                .setAutoFocusEnabled(true)
                .setRequestedPreviewSize(displayMetrics.heightPixels, displayMetrics.widthPixels)
                .build();

        myBarcodedetecter.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {

                /*called when detect data*/

                final SparseArray<Barcode> qrcode = detections.getDetectedItems();

                if (qrcode.size() != 0) {

                    preview.post(new Runnable() {
                        @Override
                        public void run() {
                            cameraSource.stop();
                            if (myBarcodedetecter.getBitmap() != null && !myBarcodedetecter.getBitmap().isRecycled()) {
                                /*if (barcodeType == RecogType.BARCODE) {
                                    pdf417Data.docFrontBitmap = myBarcodedetecter.getBitmap().copy(Bitmap.Config.ARGB_8888, true);
                                } else*/
                                if (barcodeType == RecogType.PDF417) {
                                    pdf417Data.docBackBitmap = myBarcodedetecter.getBitmap().copy(Bitmap.Config.ARGB_8888, true);
                                }
                                myBarcodedetecter.getBitmap().recycle();
                            }
                            String output = qrcode.valueAt(0).rawValue;
                            if (BarcodeHelper.extractScanResult(output, pdf417Data)) {
                                if (!isDone) {
                                    isDone = true;
                                    onScannedPDF417(pdf417Data);
                                }
                            } else {
                                if (!isDone) {
                                    isDone = true;
                                    onScannedSuccess(output);
                                }
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

    class Preview extends SurfaceView implements SurfaceHolder.Callback {
        private final SurfaceHolder mHolder;

        public Preview(Context context) {
            super(context);
            mHolder = getHolder();
            mHolder.addCallback(this);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            mSurfaceHolder = holder;

            try {
                if (cameraSource != null) {
                    if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    cameraSource.start(holder);
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (cameraSource != null) {
                cameraSource.stop();
            }
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
            if (cameraSource != null && mSurfaceHolder != null) {
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                cameraSource.start(mSurfaceHolder);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            setCamera();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void destroy(){
        if (barcodeDetector != null) barcodeDetector.release();
        if (myBarcodedetecter != null) myBarcodedetecter.release();
        if (detector != null) detector.release();
        if (myFaceDetector != null) myFaceDetector.release();
        stopCameraPreview();
    }

    protected void stopCameraPreview() {
        if (cameraSource != null) {
            cameraSource.stop();
        }
//        getHolder().removeCallback(this);
    }
}
