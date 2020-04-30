package com.docrecog.scan;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.accurascan.ocr.mrz.R;
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

abstract class ScannerCameraPreview /*extends SurfaceView implements SurfaceHolder.Callback */{
    private static final String TAG = ScannerCameraPreview.class.getSimpleName();
    private final RecogEngine recogEngine;
    private CameraSource cameraSource;
    private DisplayMetrics displayMetrics;
    public Camera camera;
    private int barcodeFormate = Barcode.PDF417;
    private Context mContext;
    private PDF417Data pdf417Data;
    private boolean isDone = false;
    public int countryCode = 0;
    protected Preview preview;
    protected ViewGroup cameraContainer;
    private SurfaceHolder mSurfaceHolder;
    private ProgressBar progressBar;

    protected abstract void onScannedSuccess(String rawResult);

    protected abstract void onScannedPDF417(PDF417Data rawResult);

    protected abstract void onError(String s);

    protected abstract void onUpdate(String s);

    protected ScannerCameraPreview(Context context) {
        this.mContext = context;
        recogEngine = new RecogEngine();
    }

    protected void initializeScanner(Context context, int countryCode) {
        preview = new Preview(mContext);
        this.cameraContainer.addView(preview);
//        progressBar = new ProgressBar(mContext, null, android.R.attr.progressBarStyleLarge);
//        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(100, 100);
//        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
//        this.cameraContainer.addView(this.progressBar, lp);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                InitModel initModel = recogEngine.initScanner(context, countryCode);
                if (initModel != null ) {
                    if (initModel.getResponseCode() == 1) {
                        initFrontCamera();
                        onUpdate(mContext.getResources().getString(R.string.scan_front));
                    } else {
                        onError(initModel.getResponseMessage());
                    }
//                    handler.sendEmptyMessage(1);
                } else {
                    onError("Failed");
//                    handler.sendEmptyMessage(0);
                }
            }
        },100);
    }

    private void initFrontCamera() {
        pdf417Data = new PDF417Data();
        displayMetrics = this.mContext.getResources().getDisplayMetrics();
        FaceDetector detector = new FaceDetector.Builder(mContext)
                .setTrackingEnabled(false)
                .build();
        MyFaceDetector myFaceDetector = new MyFaceDetector(detector);

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
                                    detector.release();
                                    myFaceDetector.release();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                preview.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        cameraSource.stop();
                                        onScannedSuccess(mContext.getResources().getString(R.string.scan_back));

                                        Runnable runnable = new Runnable() {
                                            public void run() {
                                                addScanner(mContext);
                                            }
                                        };
                                        new Handler().postDelayed(runnable, 950);
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
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context)
                .setBarcodeFormats(barcodeFormate).build();

        MyBardCodeDetector myBarcodedetecter = new MyBardCodeDetector(barcodeDetector);

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
                            pdf417Data.docBackBitmap = myBarcodedetecter.getBitmap().copy(Bitmap.Config.ARGB_8888, true);

                            String output = qrcode.valueAt(0).rawValue;
                            if (BarcodeHelper.extractScanResult(output, pdf417Data)) {
                                if (!isDone) {
                                    isDone = true;
                                    onScannedPDF417(pdf417Data);
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

    class Preview extends SurfaceView implements SurfaceHolder.Callback{
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
                cameraSource.start(holder);
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
        try {
            cameraSource.start(mSurfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        setCamera();
    }

    protected void stopCameraPreview() {
        if (cameraSource != null) {
            cameraSource.stop();
        }
//        getHolder().removeCallback(this);
    }
}
