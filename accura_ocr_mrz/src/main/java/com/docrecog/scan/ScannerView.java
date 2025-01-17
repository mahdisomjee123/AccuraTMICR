package com.docrecog.scan;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.accurascan.ocr.mrz.interfaces.OcrCallback;
import com.accurascan.ocr.mrz.model.OcrData;
import com.accurascan.ocr.mrz.model.PDF417Data;

public abstract class ScannerView extends ScannerCameraPreview {
    public boolean isflashOn = false;
    private OcrCallback scanCallBack;
    private Context context;
    private boolean isPreviewAdded = false;
    private int cameraFacing;

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
     * @param frameBox crop camera preview according to frameBox
     * @return
     */
    public ScannerView setBoxView(View frameBox) {
        this.frameBox = frameBox;
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

    public ScannerView setCameraFacing(int cameraFacing){
        this.cameraFacing = cameraFacing;
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
        setFacing(cameraFacing);
        isPreviewAdded = false;
        initializeScanner(context, countryId);
    }

    private void addCameraPreview(Context context) {

//        this.cameraContainer.addView(this);
//        DisplayMetrics dm = context.getResources().getDisplayMetrics();
//
//        if (BitmapUtil.isPortraitMode(context)) {
//            cardWidth = dm.widthPixels - 20;
//            cardHeight = (dm.heightPixels) / 3;
//        } else {
//            cardHeight = (dm.heightPixels) - (int) (100 * dm.density);
//            cardWidth = (int) (cardHeight / 0.69);
//        }
        if (this.scanCallBack != null) {
            this.scanCallBack.onUpdateLayout(cardWidth, cardHeight);
        }
        isPreviewAdded = true;
    }

    public void startScan(boolean isReset) {
        if (!isReset) startCamera();
        else isReset(isReset);
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

    public void flipCamera(int i){
        this.cameraFacing = i;
        setFacing(this.cameraFacing);
        restartPreview();
    }

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
        scanCallBack = null;
        destroy();
    }

    public void stopCamera() {
        stopCameraPreview();
    }

    @Override
    protected void onScannedSuccess(OcrData result) {
        onPlaySound();
        if (scanCallBack != null) {
            scanCallBack.onScannedComplete(result);
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
    protected void onUpdate(int s,String feedBackMessage, boolean isFlip) {
        if (scanCallBack != null) {
            if (!isPreviewAdded) {
                addCameraPreview(context);
            }
            this.scanCallBack.onProcessUpdate(s, feedBackMessage, isFlip);
        }
    }

}
