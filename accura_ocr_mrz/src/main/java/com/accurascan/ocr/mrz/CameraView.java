package com.accurascan.ocr.mrz;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.accurascan.ocr.mrz.interfaces.OcrCallback;
import com.docrecog.scan.OcrView;
import com.docrecog.scan.RecogType;
import com.docrecog.scan.ScannerView;

public class CameraView {

    private final Activity context;
    private RecogType type;
    private int countryCode;
    private int cardCode;
    private ViewGroup cameraContainer;
    private OcrCallback callback;
    private int titleBarHeight = 0;
    private boolean isSetPlayer = true;
    private MediaPlayer mediaPlayer = null;
    private AudioManager audioManager = null;
    ObjectAnimator anim = null;
    private OcrView ocrView = null;
    private ScannerView scannerView = null;

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
     * @param countryCode
     * @return
     */
    public CameraView setCountryCode(int countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    /**
     * set data for scan specific card
     *
     * @param cardCode
     * @return
     */
    public CameraView setCardCode(int cardCode) {
        this.cardCode = cardCode;
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
     * add call back
     *
     * @param callback
     * @return
     */
    public CameraView setOcrCallback(OcrCallback callback) {
        this.callback = callback;
        return this;
    }

    public CameraView setTitleBarHeight(int titleBarHeight) {
        this.titleBarHeight = titleBarHeight;
        return this;
    }


    /**
     * set false to disable sound after scanned success
     * else true to enable sound
     *
     * @param isPlayMedia is default true
     * @return
     */
    public CameraView setMediaPlayer(boolean isPlayMedia) {
        this.isSetPlayer = isPlayMedia;
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
     * call this method to initialized camera and ocr
     */
    public void init() {
        if (this.type == null) {
            throw new IllegalStateException(CameraView.class.getName() + " must have to set recogType");
        }
        if (this.cameraContainer == null) {
            throw new IllegalStateException(CameraView.class.getName() + " must have to setView");
        }
        if (this.callback == null) {
            throw new NullPointerException(context.getClass().getName() + " must have to implement " + OcrCallback.class.getName());
        }
        if (this.countryCode < 0) {
            if (type != RecogType.MRZ) {
                throw new IllegalStateException(CameraView.class.getName() + " Country Code must have to > 0");
            }
        }
        if (this.cardCode < 0) {
            if (type == RecogType.OCR)
                throw new IllegalStateException(CameraView.class.getName() + " Card Code must have to > 0");
        }

        // initialize media to play sound after scanned.
        if (this.isSetPlayer) {
            if (this.mediaPlayer == null) {
                this.mediaPlayer = MediaPlayer.create(context, R.raw.beep);
            }
            this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }

        if (type == RecogType.OCR || type == RecogType.MRZ) {
            ocrView = new OcrView(context) {
                @Override
                public void onPlaySound() {
                    playEffect();
                }
            };
            ocrView.setRecogType(this.type)
                    .setView(this.cameraContainer)
                    .setCardData(countryCode, cardCode)
                    .setOcrCallBack(this.callback)
                    .setTitleBarHeight(this.titleBarHeight)
//                    .setCustomMediaPlayer(this.mediaPlayer)
//                    .setMediaPlayer(isSetPlayer)
                    .init();
        } else if (type == RecogType.PDF417) {
            scannerView = new ScannerView(context) {
                @Override
                public void onPlaySound() {
                    playEffect();
                }
            };
            scannerView.setOcrCallBack(this.callback)
                    .setView(this.cameraContainer)
                    .setCountryCode(countryCode)
                    .init();
        }
    }

    /**
     * call this method from
     *
     * @see com.accurascan.ocr.mrz.interfaces.OcrCallback#onUpdateLayout(int, int)
     * to start your camera preview and ocr
     */
    public void startOcrScan() {
        if (ocrView != null) ocrView.startOcrScan();
        if (scannerView != null) scannerView.startScan();
    }

    /**
     * to handle camera on change window focus
     *
     * @param hasFocus
     */
    public void onWindowFocusUpdate(boolean hasFocus) {
        if (ocrView != null) {
            ocrView.onFocusUpdate(hasFocus);
        }
    }

    /**
     * call on activity resume to restart preview
     */
    public void onResume() {
        if (ocrView != null) {
            ocrView.resume();
        }
    }

    /**
     * call on activity pause to stop preview
     */
    public void onPause() {
        if (ocrView != null) {
            ocrView.pause();
        }
    }

    /**
     * call destroy method to stop camera preview
     */
    public void onDestroy() {
        if (mediaPlayer != null)
            mediaPlayer.release();
        if (ocrView != null) {
            ocrView.destroy();
        } else if (scannerView != null) {
            scannerView.stopCamera();
        }
    }

    /**
     * call this method for show user to flip card
     *
     * @param mFlipImage to animate imageView
     */
    public void flipImage(ImageView mFlipImage) {
        try {
            mFlipImage.setVisibility(View.VISIBLE);
            anim = (ObjectAnimator) AnimatorInflater.loadAnimator(context, R.animator.flipping);
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
        if (isSetPlayer && mediaPlayer != null) {
            if (audioManager != null)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
            mediaPlayer.start();
        }
    }
}
