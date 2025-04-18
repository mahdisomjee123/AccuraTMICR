package com.accurascan.ocr.mrz;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.accurascan.ocr.mrz.interfaces.OcrCallback;
import com.accurascan.ocr.mrz.model.BarcodeFormat;
import com.accurascan.ocr.mrz.util.AccuraLog;
import com.docrecog.scan.MRZDocumentType;
import com.docrecog.scan.OcrView;
import com.docrecog.scan.RecogType;
import com.docrecog.scan.ScannerView;
import com.google.android.gms.vision.barcode.Barcode;

@androidx.annotation.Keep
public class CameraView {

    private final Activity context;
    private RecogType type;
    private int countryId;
    private int cardId;
    private ViewGroup cameraContainer;
    private View frameBox;
    private int cameraFacing;
    private OcrCallback callback;
    private int statusBarHeight = 0;
    private boolean setPlayer = true;
    private int barcodeFormat = -1;
    private MediaPlayer mediaPlayer = null;
    private AudioManager audioManager = null;
    private OcrView ocrView = null;
    private ScannerView scannerView = null;
    private int documentSide = -1;
    private MRZDocumentType documentType = null;
    private int minFrame = 3;
    private String countryList = "all";
    private boolean enableCropping = false;
    private int buffer = 10;

    public CameraView(Activity context) {
        this.context = context;
    }

    /**
     * Set Camera type
     *
     * @param recogType {@link RecogType}
     * @return
     */
    public CameraView setRecogType(RecogType recogType) {
        this.type = recogType;
        return this;
    }

    /**
     * set data for scan specific document of country
     *
     * @param countryId
     * @return
     */
    public CameraView setCountryId(int countryId) {
        this.countryId = countryId;
        return this;
    }

    /**
     * set data for scan specific card
     *
     * @param cardId
     * @return
     */
    public CameraView setCardId(int cardId) {
        this.cardId = cardId;
        return this;
    }

    /**
     * set document type for mrz document(as like Passport, id, visa or other)
     *
     * @param documentType {@link MRZDocumentType}
     * @return
     */
    public CameraView setMRZDocumentType(MRZDocumentType documentType){
        this.documentType = documentType;
        return this;
    }


    /**
     * set MRZ Country codes with comma separated string as like "IND,ARK,RUS" or set "all"
     *
     * @param countryList default it is 'all'
     * @return
     */
    public CameraView setMRZCountryCodeList(String countryList){
        this.countryList = countryList;
        return this;
    }

    /**
     * add camera on this view
     *
     * @param cameraContainer add camera preview to this view
     * @return
     */
    public CameraView setView(ViewGroup cameraContainer) {
        this.cameraContainer = cameraContainer;
        return this;
    }

    /**
     * @param frameBox crop camera preview according to frameBox
     * @return
     */
    public CameraView setBoxView(View frameBox) {
        this.frameBox = frameBox;
        if(this.ocrView != null) this.ocrView.setBoxView(frameBox);
        else if(this.scannerView != null) this.scannerView.setBoxView(frameBox);
        return this;
    }

    public CameraView setCameraFacing(int cameraFacing) {
        this.cameraFacing = cameraFacing;
        return this;
    }

    public void flipCamera(){
        this.cameraFacing = (this.cameraFacing == 0/*FACING_BACK*/ ? 1/*FACING_FRONT*/ : 0/*FACING_BACK*/);
        if (this.ocrView != null) {
            this.ocrView.flipCamera(this.cameraFacing);
        }else if (this.scannerView != null) {
            this.scannerView.flipCamera(this.cameraFacing);
        }
    }

    /**
     * add call back
     *
     * @param callback
     * @return
     */
    public CameraView setOcrCallback(OcrCallback callback) {
        this.callback = callback;
        return this;
    }

    public CameraView setStatusBarHeight(int statusBarHeight) {
        this.statusBarHeight = statusBarHeight;
        return this;
    }


    /**
     * set false to disable sound
     * default true to enable sound
     *
     * @param isPlayMedia is default true
     * @return
     */
    public CameraView setEnableMediaPlayer(boolean isPlayMedia) {
        this.setPlayer = isPlayMedia;
        return this;
    }

    /**
     * set your custom play sound
     *
     * @param mediaPlayer
     * @return
     */
    public CameraView setCustomMediaPlayer(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
        return this;
    }

    /**
     * Set Barcode format to scan specific barcode.
     * Default Support All Barcode format
     *
     * @param barcodeFormat {@link BarcodeFormat}
     */
    public void setBarcodeFormat(int barcodeFormat) {
        this.barcodeFormat = barcodeFormat;
        if (this.barcodeFormat > -1 && scannerView != null)
            scannerView.updateFormat(this.barcodeFormat);
    }

    public CameraView setMinFrameForValidate(int minFrame) {
//        if (minFrame % 2 == 0) {
//            throw new IllegalArgumentException("Even number is not support");
//        }
//        if (minFrame < 3) {
//            throw new IllegalArgumentException("minFrame is grater or equal to 3");
//        }
        this.minFrame = minFrame;
        return this;
    }


    public CameraView enableCropping(boolean b, int buffer) {
        this.enableCropping = b;
        this.buffer = buffer;
        if (ocrView != null) ocrView.setEnableCropping(this.enableCropping, this.buffer);
        return this;
    }
    /**
     * call this method to initialized camera and ocr
     */
    public void init() {
        if (this.type == null) {
            throw new NullPointerException(CameraView.class.getName() + " must have to set recogType");
        }
        if (this.cameraContainer == null) {
            throw new NullPointerException(CameraView.class.getName() + " must have to setView");
        }
        if (this.callback == null) {
            throw new NullPointerException(context.getClass().getName() + " must have to implement " + OcrCallback.class.getName());
        }
        if (this.countryId < 0) {
            if (type == RecogType.OCR || type == RecogType.DL_PLATE || type == RecogType.PDF417) {
                throw new IllegalArgumentException("Country Code must have to > 0");
            } else if (type == RecogType.BARCODE) {
                countryId = 0;
            }
        }
        if (this.cardId < 0 || type == RecogType.DL_PLATE) {
            if (type == RecogType.OCR)
                throw new IllegalArgumentException("Card Code must have to > 0");
        }

        // initialize media to play sound after scanned.
        if (this.setPlayer) {
            if (this.mediaPlayer == null) {
                this.mediaPlayer = MediaPlayer.create(context, R.raw.beep);
            }
            this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }

        if (type == RecogType.OCR || type == RecogType.MRZ || type == RecogType.DL_PLATE || type == RecogType.MICR) {
            ocrView = new OcrView(context) {
                @Override
                public void onPlaySound() {
                    playEffect();
                }
            };
            ocrView.setRecogType(this.type)
                    .setEnableCropping(this.enableCropping, this.buffer)
                    .setView(this.cameraContainer)
                    .setBoxView(this.frameBox)
                    .setCameraFacing(this.cameraFacing)
                    .setCardData(countryId, cardId)
                    .setOcrCallBack(this.callback)
                    .setStatusBarHeight(this.statusBarHeight);
            if (this.type == RecogType.MRZ) {
                if (!TextUtils.isEmpty(this.countryList)) ocrView.setMrzCountries(this.countryList);
                ocrView.setMrzDocumentType(documentType != null ? documentType : MRZDocumentType.NONE);
            }


            if (this.type == RecogType.OCR) {
                if (minFrame % 2 == 0) {
                    throw new IllegalArgumentException("MinFrame is not support Even number");
                }
                if (minFrame < 3) {
                    throw new IllegalArgumentException("Must have to set minFrame is grater or equal to 3");
                }
                ocrView.setMrzCountries("all");
                ocrView.setMinFrameForValidate(this.minFrame);
                if (this.documentSide == 0) {
                    ocrView.setFrontSide();
                } else if (this.documentSide == 1) {
                    ocrView.setBackSide();
                }
            }
            ocrView.init();
        } else if (type == RecogType.PDF417 || type == RecogType.BARCODE) {
            scannerView = new ScannerView(context) {
                @Override
                public void onPlaySound() {
                    playEffect();
                }
            };
            scannerView.setBarcodeType(this.type)
                    .setOcrCallBack(this.callback)
                    .setBoxView(this.frameBox)
                    .setCameraFacing(this.cameraFacing)
                    .setView(this.cameraContainer)
                    .setCardData(countryId)
                    .setBarcodeFormat(barcodeFormat > -1 ? barcodeFormat : Barcode.ALL_FORMATS);
            if (this.type == RecogType.PDF417) {
                if (this.documentSide == 0) {
                    scannerView.setFrontSide();
                } else if (this.documentSide == 1) {
                    scannerView.setBackSide();
                }
            }
            scannerView.init();
        }
    }

    /**
     * call this method from
     *
     * @see com.accurascan.ocr.mrz.interfaces.OcrCallback#onUpdateLayout(int, int)
     * to start your camera preview and ocr
     */
    public void startOcrScan(boolean isReset) {
        AccuraLog.loge("CameraView" , "startOcrScan " + isReset);
        if (ocrView != null) ocrView.startOcrScan();
        if (scannerView != null)
            /*if (!isReset) scannerView.startScan();
             else */scannerView.startScan(isReset); // to restart scan

    }

    /**
     * To scan front side of document
     */
    public CameraView setFrontSide(){
        this.documentSide = 0;
        if (ocrView != null) ocrView.setFrontSide();
        if (scannerView != null) scannerView.setFrontSide();
        return this;
    }

    /**
     * Check back side is available or not
     * @return
     */
    public boolean isBackSideAvailable(){
        if (ocrView != null) return ocrView.isBackSide();
        if (scannerView != null) return scannerView.isBackSide();
        return false;
    }

    /**
     * To scan Back side of document
     * @return
     */
    public CameraView setBackSide(){
        AccuraLog.loge(CameraView.class.getSimpleName(), "Now scan backside of document");
        this.documentSide = 1;
        if (ocrView != null) ocrView.setBackSide();
        if (scannerView != null) scannerView.setBackSide();
        return this;
    }

    /**
     * To handle camera on change window focus
     *
     * @param hasFocus
     */
    public void onWindowFocusUpdate(boolean hasFocus) {
        if (ocrView != null) {
            ocrView.onFocusUpdate(hasFocus);
        }
    }

    public void startCamera() {
        if (ocrView != null) ocrView.restartPreview();
        else if (scannerView != null) scannerView.startCamera();
    }

    public void stopCamera() {
        if (ocrView != null) ocrView.stopPreview();
        if (scannerView != null) scannerView.stopCamera();
    }

    /**
     * Call on activity resume to restart preview
     */
    public void onResume() {
        AccuraLog.loge(CameraView.class.getSimpleName(), "onResume()");
        if (ocrView != null) {
            ocrView.resume();
        } /*else if (scannerView != null) scannerView.startScan();*/
    }

    /**
     * Call on activity pause to stop preview
     */
    public void onPause() {
        AccuraLog.loge(CameraView.class.getSimpleName(), "onPause()");
        if (ocrView != null) {
            ocrView.pause();
        }/*else if (scannerView != null) scannerView.stopCamera();*/
    }

    /**
     * Call destroy method to release camera
     */
    public void onDestroy() {
        AccuraLog.loge(CameraView.class.getSimpleName(), "onDestroy()");
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (ocrView != null) {
            ocrView.destroy();
        } else if (scannerView != null) {
            scannerView.onDestroy();
        }
    }

    /**
     * Call this method to flip card
     *
     * @param mFlipImage to animate imageView
     */
    public void flipImage(ImageView mFlipImage) {
        try {
            mFlipImage.setVisibility(View.VISIBLE);
            ObjectAnimator anim = (ObjectAnimator) AnimatorInflater.loadAnimator(context, R.animator.flipping);
            anim.setTarget(mFlipImage);
            anim.setDuration(1000);

            Animator.AnimatorListener animatorListener
                    = new Animator.AnimatorListener() {

                public void onAnimationStart(Animator animation) {
                    playEffect();
                }

                public void onAnimationRepeat(Animator animation) {

                }

                public void onAnimationEnd(Animator animation) {
                    mFlipImage.setVisibility(View.INVISIBLE);
                }

                public void onAnimationCancel(Animator animation) {

                }
            };

            anim.addListener(animatorListener);
            anim.start();
        } catch (Exception e) {

        }
    }

    private void playEffect() {
        if (setPlayer && mediaPlayer != null) {
            if (audioManager != null)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC), 0);
            mediaPlayer.start();
        }
    }

    public void release(boolean b) {
        if (ocrView != null) {
            ocrView.closeEngine(b);
        }
    }
}
