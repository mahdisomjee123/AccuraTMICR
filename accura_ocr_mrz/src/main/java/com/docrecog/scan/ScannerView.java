package com.docrecog.scan;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.accurascan.ocr.mrz.interfaces.OcrCallback;
import com.accurascan.ocr.mrz.model.PDF417Data;

public abstract class ScannerView extends ScannerCameraPreview {
    public boolean isflashOn;
    private OcrCallback scanCallBack;
    private Context context;
    private boolean isPreviewAdded = false;

    public ScannerView(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    public abstract void onPlaySound();

    /**
     * add camera on this view
     *
     * @param view add camera preview to this view
     * @return
     */
    public ScannerView setView(ViewGroup view) {
        this.cameraContainer = view;
        return this;
    }

    /**
     * Set Camera type to scan PDF417 or BARCODE
     *
     * @param recogType
     * @return
     */
    public ScannerView setBarcodeType(RecogType recogType) {
        this.barcodeType = recogType;
        return this;
    }

    /**
     * add call back to get retrieve data
     * @param callBack
     * @return
     */
    public ScannerView setOcrCallBack(OcrCallback callBack) {
        this.scanCallBack = callBack;
        return this;
    }

    /**
     * Set Country code to load Scanner
     * @param countryId
     */
    public ScannerView setCardData(int countryId) {
        this.countryId = countryId;
        return this;
    }

    public ScannerView setBarcodeFormat(int barcodeFormat) {
        this.barcodeFormat = barcodeFormat;
        return this;
    }

    /**
     * call this method to initialized scanner
     */
    public void init() {
        if (this.cameraContainer == null) {
            throw new IllegalStateException("Must have to setView");
        }
        if (this.scanCallBack == null) {
            throw new NullPointerException(context.getClass().getName() + " must have to implement " + OcrCallback.class.getName());
        }
//        if (this.countryCode < 0) {
//            throw new IllegalStateException("Country Code must have to > 0");
//        }
        isPreviewAdded = false;
        initializeScanner(context, countryId);
    }

    private void addCameraPreview(Context context) {

//        this.cameraContainer.addView(this);
        DisplayMetrics dm = context.getResources().getDisplayMetrics();

        int W = dm.widthPixels - 20;
        int H = (dm.heightPixels) / 3;
        if (this.scanCallBack != null) {
            this.scanCallBack.onUpdateLayout(W, H);
        }
        isPreviewAdded = true;
    }

    public void startScan() {
        startCamera();
    }

//    public void flipImage(ImageView mFlipImage) {
//        try {
//            mFlipImage.setVisibility(View.VISIBLE);
//            ObjectAnimator anim = (ObjectAnimator) AnimatorInflater.loadAnimator(context, R.animator.flipping);
//            anim.setTarget(mFlipImage);
//            anim.setDuration(1000);
//
//
//            Animator.AnimatorListener animatorListener
//                    = new Animator.AnimatorListener() {
//
//                public void onAnimationStart(Animator animation) {
//                    playSound();
//                }
//
//                public void onAnimationRepeat(Animator animation) {
//
//                }
//
//                public void onAnimationEnd(Animator animation) {
//                    mFlipImage.setVisibility(View.INVISIBLE);
//                }
//
//                public void onAnimationCancel(Animator animation) {
//
//                }
//            };
//
//            anim.addListener(animatorListener);
//            anim.start();
//        } catch (Exception e) {
//
//        }
//
//    }

//    private void playSound() {
//        final MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.beep);
//        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
//        if (audioManager != null)
//            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
//        mediaPlayer.start();
//        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(MediaPlayer mediaPlayer1) {
//                mediaPlayer.stop();
//                mediaPlayer.release();
//            }
//        });
//    }

    public boolean isflashOn() {
        return isflashOn;
    }

    public void setFlash() {
        if (hasFlash()) {
            isflashOn = true;
            if (camera == null) {
                setCamera();
            }
//        cameraPreview.camera = null;
//        camera = getCamera(cameraSource);
            Camera.Parameters p;
            if (camera != null) {
                p = camera.getParameters();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(p);
            }
        }
    }

    public void stopFlash() {
        if (hasFlash()) {
            isflashOn = false;
            if (camera == null) {
                setCamera();
            }
            Camera.Parameters p;
            if (camera != null) {
                p = camera.getParameters();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(p);
            }
        }
    }

    public boolean hasFlash() {
        return context.getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    public void startCamera() {
        startCameraPreview();
        if (isflashOn)
            setFlash();
    }

    public void onDestroy(){
        destroy();
    }

    public void stopCamera() {
        stopCameraPreview();
    }

    @Override
    protected void onScannedSuccess(String rawResult) {
        onPlaySound();
        if (scanCallBack != null) {
            scanCallBack.onScannedComplete(rawResult);
        }
    }

    @Override
    protected void onScannedPDF417(PDF417Data result) {
        onPlaySound();
        if (scanCallBack != null) {
            scanCallBack.onScannedComplete(result);
        }
    }

    @Override
    protected void onError(String s) {
        if (scanCallBack != null) {
            scanCallBack.onError(s);
        }
    }

    @Override
    protected void onUpdate(int s, boolean isFlip) {
        if (scanCallBack != null) {
            if (!isPreviewAdded) {
                addCameraPreview(context);
            }
            this.scanCallBack.onProcessUpdate(s, null, isFlip);
        }
    }

}
