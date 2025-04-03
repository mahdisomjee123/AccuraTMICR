package com.docrecog.scan;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import com.accurascan.ocr.mrz.interfaces.OcrCallback;

public abstract class OcrView extends OcrCameraPreview {

    private final Activity context;
    private OcrCallback ocrCallBack;
    private int countryId = -1;
    private int cardId = -1;
    private ViewGroup cameraContainer;
    private int cameraFacing;
//    private boolean isSetPlayer = true;
//    private MediaPlayer mediaPlayer = null;
    private int statusBarHeight = 0;
    private RecogType recogType = null;

    public OcrView(Activity context) {
        super(context);
        this.context = context;
    }

    public abstract void onPlaySound();

    /**
     * set data for scan specific card of the country
     *
     * @param countryId
     * @param cardId
     * @return
     */
    public OcrView setCardData(int countryId, int cardId) {
        this.countryId = countryId;
        this.cardId = cardId;
        return this;
    }

    /**
     * add call back to get camera update and ocr
     *
     * @param ocrCallBack
     * @return
     */
    public OcrView setOcrCallBack(OcrCallback ocrCallBack) {
        this.ocrCallBack = ocrCallBack;
        return this;
    }

    /**
     * add camera on this view
     *
     * @param view add camera preview to this view
     * @return
     */
    public OcrView setView(ViewGroup view) {
        this.cameraContainer = view;
        return this;
    }

    /**
     * @param frameBox crop camera preview according to frameBox
     * @return
     */
    public OcrView setBoxView(View frameBox) {
        this.frameBox = frameBox;
        return this;
    }

    public OcrView setCameraFacing(int cameraFacing){
        this.cameraFacing = cameraFacing;
        return this;
    }

    /**
     * Set Camera type to scan Mrz or Ocr+Mrz
     *
     * @param recogType
     * @return
     */
    public OcrView setRecogType(RecogType recogType) {
        this.recogType = recogType;
        return this;
    }

//    /**
//     * set false to disable sound after scanned success
//     * else true to enable sound
//     *
//     * @param isPlayMedia is default true
//     * @return
//     */
//    public OcrView setMediaPlayer(boolean isPlayMedia) {
//        this.isSetPlayer = isPlayMedia;
//        return this;
//    }
//
//    /**
//     * set your custom play sound
//     *
//     * @param mediaPlayer
//     * @return
//     */
//    public OcrView setCustomMediaPlayer(MediaPlayer mediaPlayer) {
//        this.mediaPlayer = mediaPlayer;
//        return this;
//    }

    public OcrView setStatusBarHeight(int statusBarHeight) {
        this.statusBarHeight = statusBarHeight;
        return this;
    }

    public OcrView setMinFrameForValidate(int minFrame) {
//        if (minFrame % 2 == 0) {
//            throw new IllegalArgumentException("Even number is not support");
//        }
//        if (minFrame < 3) {
//            throw new IllegalArgumentException("minFrame is grater or equal to 3");
//        }
        this.minFrame = minFrame;
        return this;
    }

    /**
     * call this method to initialized camera and ocr
     */
    public void init() {
        if (this.recogType == null) {
            throw new NullPointerException("Must have to set recogType");
        }
        if (this.cameraContainer == null) {
            throw new NullPointerException("Must have to setView");
        }
        if (this.ocrCallBack == null) {
            throw new NullPointerException(context.getClass().getName() + " must have to implement " + OcrCallback.class.getName());
        }
        if (recogType == RecogType.OCR || recogType == RecogType.DL_PLATE) {
            if (this.countryId < 0) {
                throw new IllegalArgumentException("Country Code must have to > 0");
            }
            if (this.cardId < 0) {
                throw new IllegalArgumentException("Card Code must have to > 0");
            }
        }
        setData(countryId, cardId)
        .setLayout(cameraContainer)
        .setType(recogType)
        .setHeight(statusBarHeight)
        .setFacing(cameraFacing)
        .start();
    }

    public void flipCamera(int i){
        this.cameraFacing = i;
        setFacing(this.cameraFacing);
        restartPreview();
    }

    /**
     * To handle camera on window focus update
     *
     * @param hasFocus
     */
    public void onFocusUpdate(boolean hasFocus) {
        onWindowFocusChanged(hasFocus);
    }

    /**
     * Call this method from
     *
     * @see com.accurascan.ocr.mrz.interfaces.OcrCallback#onUpdateLayout(int, int)
     * to start your camera preview and ocr
     */
    public void startOcrScan() {
        startOcr();
    }

//    /**
//     * call this method for show user to flip card
//     *
//     * @param mFlipImage to animate imageView
//     */
//    public void flipImage(ImageView mFlipImage) {
//        try {
//            mFlipImage.setVisibility(View.VISIBLE);
//            anim = (ObjectAnimator) AnimatorInflater.loadAnimator(context, R.animator.flipping);
//            anim.setTarget(mFlipImage);
//            anim.setDuration(1000);
//
//
//            Animator.AnimatorListener animatorListener
//                    = new Animator.AnimatorListener() {
//
//                public void onAnimationStart(Animator animation) {
//                    playEffect();
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

    /**
     * call on activity resume to restart preview
     */
    public void resume() {
        onResume();
    }

    /**
     * call on activity pause to stop preview
     */
    public void pause() {
        onPause();
    }

    /**
     * call destroy method to stop camera preview
     */
    public void destroy() {
        ocrCallBack = null;
        onDestroy();
    }


    @Override
    void onProcessUpdate(int s, String s1, boolean b) {
        if (ocrCallBack != null) {
//            context.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
                    OcrView.this.ocrCallBack.onProcessUpdate(s, s1, b);
//                }
//            });
        }
    }

    @Override
    void onError(String s) {
        if (ocrCallBack != null) {
//            context.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
                    OcrView.this.ocrCallBack.onError(s);
//                }
//            });
        }
    }

    @Override
    void onUpdateLayout(int width, int height) {
        if (ocrCallBack != null) {
//            context.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
                    OcrView.this.ocrCallBack.onUpdateLayout(width, height);
//                }
//            });
        }
    }

    @Override
    void onScannedComplete(Object result) {
        onPlaySound();
        if (ocrCallBack != null) {
            OcrView.this.ocrCallBack.onScannedComplete(result);
//            context.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    if (recogType == RecogType.OCR && result instanceof OcrData) {
////                        OcrView.this.ocrCallBack.onScannedComplete((OcrData) result, null, null);
//                        OcrView.this.ocrCallBack.onScannedComplete(result);
//                    } else if (recogType == RecogType.MRZ && result instanceof RecogResult) {
////                        OcrView.this.ocrCallBack.onScannedComplete(null, (RecogResult) result, null);
//                        OcrView.this.ocrCallBack.onScannedComplete(result);
//                    }
//                }
//            });
        }
    }

    public OcrView setEnableCropping(boolean enableCropping) {
        this.isCroppingEnable = enableCropping;
        return this;
    }
}
