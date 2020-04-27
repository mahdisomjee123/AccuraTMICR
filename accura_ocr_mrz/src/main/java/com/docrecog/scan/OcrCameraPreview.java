package com.docrecog.scan;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.CameraProfile;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.accurascan.ocr.mrz.R;
import com.accurascan.ocr.mrz.camerautil.CameraHolder;
import com.accurascan.ocr.mrz.camerautil.FocusManager;
import com.accurascan.ocr.mrz.model.InitModel;
import com.accurascan.ocr.mrz.model.OcrData;
import com.accurascan.ocr.mrz.model.RecogResult;
import com.accurascan.ocr.mrz.util.BitmapUtil;
import com.accurascan.ocr.mrz.util.Util;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.accurascan.ocr.mrz.camerautil.CameraSource.FOCUSING;
import static com.accurascan.ocr.mrz.camerautil.CameraSource.IDLE;
import static com.accurascan.ocr.mrz.camerautil.CameraSource.PREVIEW_STOPPED;
import static com.accurascan.ocr.mrz.camerautil.FocusManager.isSupported;

abstract class OcrCameraPreview implements Camera.PreviewCallback, FocusManager.Listener, RecogEngine.ScanListener {

    abstract void onProcessUpdate(String s, String s1, boolean b);

    abstract void onError(String s);

    abstract void onUpdateLayout(int width, int height);

    abstract void onScannedComplete(Object result);

    private static final String TAG = OcrCameraPreview.class.getSimpleName();

    protected Camera mCameraDevice;
    Camera.Parameters mParameters;
    int mPreviewWidth = 1280;
    int mPreviewHeight = 720;
    SurfaceHolder mSurfaceHolder;
    private int mCameraId;
    private int mCameraState = PREVIEW_STOPPED;

    // The subset of parameters we need to update in setCameraParameters().
    private static final int UPDATE_PARAM_INITIALIZE = 1;
    private static final int UPDATE_PARAM_PREFERENCE = 4;
    private static final int UPDATE_PARAM_ALL = -1;

    private static final int FIRST_TIME_INIT = 0;
    private static final int CLEAR_SCREEN_DELAY = 1;
    private static final int TRIGER_RESTART_RECOG = 5;
    private static final int TRIGER_RESTART_RECOG_DELAY = 40; //30 ms

    private static final int SNAPSHOT_IN_PROGRESS = 3;
    private static final int SELFTIMER_COUNTING = 4;

    final Handler mHandler = new Handler();

    private final CameraErrorCallback mErrorCallback = new CameraErrorCallback();
    private final AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback();

    // This handles everything about focus.
    FocusManager mFocusManager;
    private boolean mOpenCameraFail = false;
    private boolean mCameraDisabled = false, isTouchCalled;
    private boolean mOnResumePending;
    private boolean mPausing;
    private boolean mFirstTimeInitialized;

    // The display rotation in degrees. This is only valid when mCameraState is
    // not PREVIEW_STOPPED.
    private int mDisplayRotation;
    // The value for android.hardware.Camera.setDisplayOrientation.
    private int mDisplayOrientation;
    private final Lock _mutex = new ReentrantLock(true);
    private Thread mCameraOpenThread = new Thread(new Runnable() {
        public void run() {
            try {
                mCameraDevice = Util.openCamera(mActivity, mCameraId);
            } catch (Exception e) {
                mOpenCameraFail = true;
                mCameraDisabled = true;
            }
        }
    });
    private Thread mCameraPreviewThread = new Thread(new Runnable() {
        public void run() {
            initializeCapabilities();
            startPreview();
        }
    });

    private Activity mActivity;
    private int conutryCode = -1;
    private int cardCode = -1;
    //    private OcrCallback ocrCallBack;
    private ViewGroup cameraContainer;
//    private boolean isSetPlayer = true;

    private RecogEngine recogEngine;
    private final OcrData ocrData;
    private RecogResult g_recogResult;
    private int rectW, rectH;
    private DisplayMetrics dm;
    private boolean isbothavailable = false;
    private int checkmrz = 0;
    //    private MediaPlayer mediaPlayer = null;
//    private AudioManager audioManager = null;
    private boolean isValidate = false;
    private int titleBarHeight = 0;
    private RecogType recogType = null;
    private int mRecCnt = 0; //counter for mrz detecting
    private int bRet = 0; //counter for mrz detecting

    private ProgressBar progressBar;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.e(TAG, "handleMessage: " + msg.what);
            if (msg.what == 1) {
                init();
            }
        }
    };

    public OcrCameraPreview(Activity context) {
        this.mActivity = context;
        this.ocrData = new OcrData();
    }

    /**
     * set data for scan specific card of the country
     *
     * @param countryCode
     * @param cardCode
     * @return
     */
    OcrCameraPreview setData(int countryCode, int cardCode) {
        this.conutryCode = countryCode;
        this.cardCode = cardCode;
//        ocrData.setCardId(cardCode);
//        ocrData.setCountry_id(countryCode);
        return this;
    }

    /**
     * add camera on this view
     *
     * @param view add camera preview to this view
     * @return
     */
    OcrCameraPreview setLayout(ViewGroup view) {
        this.cameraContainer = view;
        return this;
    }

    /**
     * Set Camera type to scan Mrz or Ocr+Mrz
     *
     * @param recogType
     * @return
     */
    OcrCameraPreview setType(RecogType recogType) {
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
//    OcrCameraPreview isSetMediaPlayer(boolean isPlayMedia) {
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
//    OcrCameraPreview setCustomMPlayer(MediaPlayer mediaPlayer) {
//        this.mediaPlayer = mediaPlayer;
//        return this;
//    }

    OcrCameraPreview setHeight(int titleBarHeight) {
        this.titleBarHeight = titleBarHeight;
        return this;
    }

    /**
     * call this method to initialized camera and ocr
     */
    void start() {
        if (this.recogType == null) {
            throw new IllegalStateException("must have to set recogType");
        }
        if (this.cameraContainer == null) {
            throw new IllegalStateException("OcrCameraPreview must have to setView");
        }
        if (recogEngine == null) {
            recogEngine = new RecogEngine();
        }
        isValidate = true;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Util.isPermissionsGranted(mActivity)) {
//            throw new IllegalStateException("must have to granted Camera permission to access your hardware camera");
//        } else {
//            mCameraOpenThread.start();
//
////            // Make sure camera device is opened.
////            try {
////                mCameraOpenThread.join();
////                mCameraOpenThread = null;
////                if (mOpenCameraFail) {
////                    Util.showErrorAndFinish(mActivity, R.string.cannot_connect_camera);
////                    return;
////                } else if (mCameraDisabled) {
////                    Util.showErrorAndFinish(mActivity, R.string.camera_disabled);
////                    return;
////                }
////            } catch (InterruptedException ex) {
////                // ignore
////            }
//
//        }
        String[] defaultFocusModes = {"continuous-video", "auto", "continuous-picture"};
        mFocusManager = new FocusManager(defaultFocusModes);
        mCameraId = CameraHolder.instance().getBackCameraId();
        dm = mActivity.getResources().getDisplayMetrics();
        Preview preview = new Preview(mActivity);
        progressBar = new ProgressBar(mActivity, null, android.R.attr.progressBarStyleLarge);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(100, 100);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        if (recogType == RecogType.OCR) {
            if (this.conutryCode <= 0 || this.cardCode <= 0) {
                throw new IllegalStateException("OcrCameraPreview must have to setCardData");
            }
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            int widthMargin = -(dm.widthPixels / 5);
            int heightMargin = -((dm.heightPixels - this.titleBarHeight) / 5);
            params.setMargins(widthMargin, heightMargin, widthMargin, heightMargin);
            preview.setLayoutParams(params);
//            this.cameraContainer.addView(preview);
//            this.cameraContainer.addView(progressBar, lp);
//            doWork();
        } /*else {
            rectW = dm.widthPixels - 20;
            rectH = (dm.heightPixels - titleBarHeight) / 3;
            this.cameraContainer.addView(preview);
            this.cameraContainer.addView(progressBar, lp);
            new Thread() {
                public void run() {
                    onProcessUpdate(mActivity.getResources().getString(R.string.scan_front), null, false);
                    handler.sendEmptyMessage(1);
                }
            }.start();
        }*/
        this.cameraContainer.addView(preview);
//        this.cameraContainer.addView(progressBar, lp);

        doWork();
    }

    /**
     * call this method from
     *
     * @see com.accurascan.ocr.mrz.interfaces.OcrCallback#onUpdateLayout(int, int)
     * to start your camera preview and ocr
     */
    void startOcr() {

        if (!isValidate) return;

        mCameraPreviewThread.start();
        // Make sure preview is started.
        try {
            mCameraPreviewThread.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        mCameraPreviewThread = null;
        progressBar.setVisibility(View.GONE);
//        if (progressBar != null && progressBar.isShowing()) {
//            progressBar.dismiss();
//        }
    }

    private void doWork() {
//        progressBar = new ProgressDialog(mActivity);
//        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//        progressBar.setMessage("Please wait...");
//        progressBar.show();

        new Thread() {
            public void run() {
//        Runnable runnable = new Runnable() {
//            public void run() {
                Util.logd(TAG, "Worker started");
                if (recogType == RecogType.OCR) {
                    try {
                        final InitModel i1 = recogEngine.initOcr(OcrCameraPreview.this, mActivity, OcrCameraPreview.this.conutryCode, OcrCameraPreview.this.cardCode);
                        if (i1 != null && i1.getInitData() != null) {
                            rectH = i1.getInitData().getCameraHeight();
                            rectW = i1.getInitData().getCameraWidth();
                            ocrData.setCardname(i1.getInitData().getCardName());
                            isbothavailable = i1.getInitData().getIsbothavailable();
                            onProcessUpdate(String.format("Scan %s of %s", i1.getInitData().getCardside(), ocrData.getCardname()), null, false);
                            handler.sendEmptyMessage(1);
                        } else {
                            onError(i1.getResponseMessage());
                            handler.sendEmptyMessage(0);
                        }
                    } catch (Exception e) {
                        Log.e("threadmessage", e.getMessage());
                    }
                } else {

                    rectW = dm.widthPixels - 20;
                    rectH = (dm.heightPixels - titleBarHeight) / 3;
                    onProcessUpdate(mActivity.getResources().getString(R.string.scan_front), null, false);
                    handler.sendEmptyMessage(1);
                }
//            }
//        };
//        new Handler().postDelayed(runnable, 100);
            }
        }.start();
    }

    private void init() {
        Log.d(TAG, "init: ");
        //initialize the result value
        g_recogResult = new RecogResult();
        g_recogResult.recType = RecogEngine.RecType.INIT;
        g_recogResult.bRecDone = false;

//        // initialize media to play sound after scanned.
//        if (this.isSetPlayer) {
//            if (this.mediaPlayer == null) {
//                this.mediaPlayer = MediaPlayer.create(mActivity, R.raw.beep);
//            }
//            this.audioManager = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
//        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Util.isPermissionsGranted(mActivity)) {
            throw new IllegalStateException("must have to granted Camera permission to access your hardware camera");
        } else {
            mCameraOpenThread.start();

            // Make sure camera device is opened.
            try {
                mCameraOpenThread.join();
                mCameraOpenThread = null;
                if (mOpenCameraFail) {
                    Util.showErrorAndFinish(mActivity, R.string.cannot_connect_camera);
                    return;
                } else if (mCameraDisabled) {
                    Util.showErrorAndFinish(mActivity, R.string.camera_disabled);
                    return;
                }
            } catch (InterruptedException ex) {
                // ignore
            }

        }
        onUpdateLayout(rectW, rectH);

    }

    /**
     * to handle camera on window focus update
     *
     * @param hasFocus
     */
    void onWindowFocusChanged(boolean hasFocus) {
        Util.logd(TAG, "onWindowFocusChanged.hasFocus=" + hasFocus
                + ".mOnResumePending=" + mOnResumePending);
        if (hasFocus && mOnResumePending) {
            doOnResume();
            mOnResumePending = false;
        }
    }

    /**
     * call on activity resume to restart preview
     */
    void onResume() {
        Util.logd(TAG, "onResume. hasWindowFocus()=" + mActivity.hasWindowFocus());
        if (mCameraDevice == null) {
            Util.logd(TAG, "onResume. mOnResumePending=true");
//            mOnResumePending = true;
            if (mActivity.hasWindowFocus()) {
                doOnResume();
            } else
                mOnResumePending = true;
        } else {
            Util.logd(TAG, "onResume. mOnResumePending=false");
            doOnResume();

            mOnResumePending = false;
        }
    }

    private void doOnResume() {
        if (mOpenCameraFail || mCameraDisabled)
            return;

        // if (mRecogService != null && mRecogService.isProcessing())
        // showProgress(null);
        mPausing = false;

        // Start the preview if it is not started.
        if (mCameraState == PREVIEW_STOPPED) {
            try {
                mCameraDevice = Util.openCamera(mActivity, mCameraId);
                initializeCapabilities();
                startPreview();
            } catch (Exception e) {
                Util.showErrorAndFinish(mActivity, R.string.cannot_connect_camera);
                return;
            }
        }

        if (mSurfaceHolder != null) {
            // If first time initialization is not finished, put it in the
            // message queue.
            if (!mFirstTimeInitialized) {
                mHandler.sendEmptyMessage(FIRST_TIME_INIT);
            }
        }

        keepScreenOnAwhile();
    }

    /**
     * call on activity pause to stop preview
     */
    void onPause() {

        mOnResumePending = false;
        mPausing = true;

        stopPreview();

        // Close the camera now because other activities may need to use it.
        closeCamera();
        resetScreenOn();

        // Remove the messages in the event queue.
        mHandler.removeMessages(FIRST_TIME_INIT);
        mHandler.removeMessages(TRIGER_RESTART_RECOG);
    }

    /**
     * call destroy method to stop camera preview
     */
    void onDestroy() {
//        if (mediaPlayer != null)
//            mediaPlayer.release();
        stopPreview();
    }

    private void resetScreenOn() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void keepScreenOnAwhile() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mPausing) {
            return;
        }

        if (mCameraState != IDLE) {
            mCameraDevice.setOneShotPreviewCallback(OcrCameraPreview.this);
            return;
        }

        Thread recogThread = new Thread(new Runnable() {
            Bitmap bmCard;
            int ret;
            int faceret = 0;

            @Override
            public void run() {

                System.out.println("Start Run Thread++");

                bmCard = BitmapUtil.getBitmapFromData(data, camera, mDisplayOrientation, rectH, rectW, recogType);

                _mutex.lock();

                if (bmCard != null) {
                    if (recogType == RecogType.OCR) {
                        ImageOpencv imageOpencv = recogEngine.checkCard(bmCard);
                        if (imageOpencv != null) {
                            if (imageOpencv.isSucess && imageOpencv.mat != null) {
                                Bitmap card = imageOpencv.getBitmap(bmCard);
                                int ret = 0;
                                if (recogEngine.isMrzEnable) {
                                    ret = recogEngine.doRunData(bmCard, 0, g_recogResult);
                                }
                                if (checkmrz == 0) {
                                    recogEngine.doRecognition(OcrCameraPreview.this, card, imageOpencv.mat, ocrData);
                                } else {
                                    if (ret == 1 || ret == 2) {
                                        GotMRZData();
                                    } else {
                                        refreshPreview();
                                        bmCard.recycle();
                                    }
                                }
                            } else {
                                refreshPreview();
                                bmCard.recycle();
                            }
                        } else {
                            refreshPreview();
                            bmCard.recycle();
                        }
                    } else if (recogType == RecogType.MRZ) {
                        if (g_recogResult.recType == RecogEngine.RecType.INIT) {
                            ret = recogEngine.doRunData(bmCard, 1, g_recogResult);
                            if (ret <= 0 && mRecCnt > 2) {
                                // Bitmap docBmp = null;

                                if (mRecCnt % 4 == 1)
                                    faceret = recogEngine.doRunFaceDetect(bmCard, g_recogResult);
                            }
                            mRecCnt++; //counter increases
                        } else if (g_recogResult.recType == RecogEngine.RecType.FACE) { //have to do mrz
                            ret = recogEngine.doRunData(bmCard, 1, g_recogResult);
                            if (bRet > -1) {
                                bRet++;
                            }
                        } else if (g_recogResult.recType == RecogEngine.RecType.MRZ) { //have to do face
                            if (mRecCnt > 2) {
                                Bitmap docBmp = bmCard;

                                if (mRecCnt % 5 == 1)
                                    ret = recogEngine.doRunFaceDetect(docBmp, g_recogResult);
                            }
                            mRecCnt++;
                        }

                    }
                }

                _mutex.unlock();

                if (recogType == RecogType.MRZ) {
                    OcrCameraPreview.this.mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (ret > 0) {
                                mRecCnt = 0; //counter sets 0
                                Bitmap docBmp = bmCard;

                                if ((g_recogResult.recType == RecogEngine.RecType.MRZ && !g_recogResult.bRecDone) ||
                                        (g_recogResult.recType == RecogEngine.RecType.FACE && g_recogResult.bRecDone)) {
                                    if (!g_recogResult.bRecDone || bRet > 2 || bRet == -1) {
                                        g_recogResult.docBackBitmap = docBmp.copy(Bitmap.Config.ARGB_8888, false);
                                    } else {
                                        g_recogResult.docFrontBitmap = docBmp.copy(Bitmap.Config.ARGB_8888, false);
                                    }
                                }

                                if (g_recogResult.recType == RecogEngine.RecType.BOTH ||
                                        g_recogResult.recType == RecogEngine.RecType.MRZ && g_recogResult.bRecDone)
                                    g_recogResult.docFrontBitmap = docBmp.copy(Bitmap.Config.ARGB_8888, false);

                                docBmp.recycle();

                                if (g_recogResult.bRecDone) {
                                    sendInformation();
                                } else {
                                    onProcessUpdate(mActivity.getResources().getString(R.string.scan_front), null, true);
                                    refreshPreview();
                                    mHandler.sendMessageDelayed(
                                            mHandler.obtainMessage(TRIGER_RESTART_RECOG),
                                            TRIGER_RESTART_RECOG_DELAY);
                                }
                            } else {
                                Log.d(TAG, "failed");
                                if (mRecCnt > 3 && faceret > 0) //detected only face, so need to detect mrz
                                {
                                    mRecCnt = 0; //counter sets 0
                                    faceret = 0;
                                    g_recogResult.recType = RecogEngine.RecType.FACE;

                                    Bitmap docBmp = bmCard;
                                    g_recogResult.docFrontBitmap = docBmp.copy(Bitmap.Config.ARGB_8888, false);
                                    docBmp.recycle();
                                } else if (bRet == 2) {
                                    bRet = -1;
                                    onProcessUpdate(mActivity.getResources().getString(R.string.scan_back), null, true);
                                }

                                refreshPreview();

                                mHandler.sendMessageDelayed(
                                        mHandler.obtainMessage(TRIGER_RESTART_RECOG),
                                        TRIGER_RESTART_RECOG_DELAY);
                            }
                        }

                    });
                }
            }
        });
        recogThread.start();
    }

    private void refreshPreview() {
        if (!mPausing && mCameraDevice != null) {
            mCameraDevice.setOneShotPreviewCallback(OcrCameraPreview.this);
        }
    }

    public void autoFocus() {
        if (mCameraDevice != null) {
            Camera.Parameters p = mCameraDevice.getParameters();
            List<String> focusModes = p.getSupportedFocusModes();

            if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                if (FocusManager.isSupported(Camera.Parameters.FOCUS_MODE_AUTO, mParameters.getSupportedFocusModes())) {
                    mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    if (mCameraDevice != null) {
                        mCameraDevice.setParameters(mParameters);
                        mCameraDevice.autoFocus(mAutoFocusCallback);
                        setCameraState(FOCUSING);
                        isTouchCalled = false;
                    }
                }
            } else {
                isTouchCalled = false;
            }
        }
    }

    @Override
    public void cancelAutoFocus() {
        mCameraDevice.cancelAutoFocus();
        if (mCameraState != SELFTIMER_COUNTING
                && mCameraState != SNAPSHOT_IN_PROGRESS) {
            setCameraState(IDLE);
        }
        setCameraParameters(UPDATE_PARAM_PREFERENCE);
        isTouchCalled = false;
    }

    @Override
    public boolean capture() {
        // If we are already in the middle of taking a snapshot then ignore.
        if (mCameraState == SNAPSHOT_IN_PROGRESS || mCameraDevice == null) {
            return false;
        }
        setCameraState(SNAPSHOT_IN_PROGRESS);

        return true;
    }

    @Override
    public void setFocusParameters() {
        setCameraParameters(UPDATE_PARAM_PREFERENCE);
    }

    @Override
    public void playSound(int soundId) {

    }

    private final class AutoFocusCallback implements
            Camera.AutoFocusCallback {
        public void onAutoFocus(boolean focused, Camera camera) {
            if (mPausing)
                return;

            if (mCameraState == FOCUSING) {
                setCameraState(IDLE);
            }
            mFocusManager.onAutoFocus(focused);

            String focusMode = mFocusManager.getFocusMode();
            mParameters.setFocusMode(focusMode);
            mCameraDevice.setParameters(mParameters);
            isTouchCalled = false;
//            autoFocus();
        }
    }

    private class CameraErrorCallback implements Camera.ErrorCallback {
        private static final String TAG = "CameraErrorCallback";

        public void onError(int error, Camera camera) {
            Log.e(TAG, "Got camera error callback. error=" + error);
            if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
                // We are not sure about the current state of the app (in preview or
                // snapshot or recording). Closing the app is better than creating a
                // new Camera object.
                throw new RuntimeException("Media server died.");
            }
        }
    }

    class Preview extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener {
        private final SurfaceHolder mHolder;

        public Preview(Context context) {
            super(context);
            mHolder = getHolder();
            mHolder.addCallback(this);
            setOnTouchListener(this);
        }

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
            Util.logd(TAG, "(w,h)" + width + "," + height);
            // We need to save the holder for later use, even when the mCameraDevice
            // is null. This could happen if onResume() is invoked after this
            // function.
            mSurfaceHolder = holder;

            // The mCameraDevice will be null if it fails to connect to the camera
            // hardware. In this case we will show a dialog and then finish the
            // activity, so it's OK to ignore it.
            if (mCameraDevice == null)
                return;

            // Sometimes surfaceChanged is called after onPause or before onResume.
            // Ignore it.
            if (mPausing/* || cameraActivity.isFinishing()*/)
                return;

            // Set preview display if the surface is being created. Preview was
            // already started. Also restart the preview if display rotation has
            // changed. Sometimes this happens when the device is held in portrait
            // and camera app is opened. Rotation animation takes some time and
            // display rotation in onCreate may not be what we want.
            if (mCameraState == PREVIEW_STOPPED) {
                startPreview();
            } else {
                if (Util.getDisplayRotation(mActivity) != mDisplayRotation) {
                    setDisplayOrientation();
                }
                if (holder.isCreating()) {
                    // Set preview display if the surface is being created and
                    // preview
                    // was already started. That means preview display was set to
                    // null
                    // and we need to set it now.
                    setPreviewDisplay(holder);
                }
            }

            // If first time initialization is not finished, send a message to do
            // it later. We want to finish surfaceChanged as soon as possible to let
            // user see preview first.
            if (!mFirstTimeInitialized) {
                mHandler.sendEmptyMessage(FIRST_TIME_INIT);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Util.logd(TAG, "surfaceDestroyed: ");
            stopPreview();
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!isTouchCalled && mParameters != null) {
                String focusMode = mParameters.getFocusMode();
                if (focusMode == null || Camera.Parameters.FOCUS_MODE_INFINITY.equals(focusMode)) {
                    return false;
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    isTouchCalled = true;
                    autoFocus();
                }

            }

            return true;
        }
    }

    private void startPreview() {

        if (mCameraDevice != null) {
            if (mPausing || mActivity.isFinishing())
                return;

            mCameraDevice.setErrorCallback(mErrorCallback);

            // If we're previewing already, stop the preview first (this will blank
            // the screen).
            if (mCameraState != PREVIEW_STOPPED)
                stopPreview();

            setPreviewDisplay(mSurfaceHolder);
            setDisplayOrientation();

            mCameraDevice.setOneShotPreviewCallback(OcrCameraPreview.this);
            setCameraParameters(UPDATE_PARAM_ALL);

            // Inform the mainthread to go on the UI initialization.
            if (mCameraPreviewThread != null) {
                synchronized (mCameraPreviewThread) {
                    mCameraPreviewThread.notify();
                }
            }

            try {
                Log.v(TAG, "startPreview");
                mCameraDevice.startPreview();
//                autoFocus();
            } catch (Throwable ex) {
                closeCamera();
//                throw new RuntimeException("startPreview failed", ex);
            }

            setCameraState(IDLE);

            // notify again to make sure main thread is wake-up.
            if (mCameraPreviewThread != null) {
                synchronized (mCameraPreviewThread) {
                    mCameraPreviewThread.notify();
                }
            }
        }
    }

    private void stopPreview() {
        if (mCameraDevice == null)
            return;
        mCameraDevice.stopPreview();
        // mCameraDevice.setPreviewCallback(null);
        setCameraState(PREVIEW_STOPPED);
    }

    private void closeCamera() {
        if (mCameraDevice != null) {
            CameraHolder.instance().release();
            mCameraDevice.setErrorCallback(null);
            mCameraDevice = null;
            setCameraState(PREVIEW_STOPPED);
            mFocusManager.onCameraReleased();
        }
    }

    private void setPreviewDisplay(SurfaceHolder holder) {
        try {
            if (mCameraDevice != null) {
                mCameraDevice.setPreviewDisplay(holder);
            }
        } catch (Throwable ex) {
            closeCamera();
            throw new RuntimeException("setPreviewDisplay failed", ex);
        }
    }

    private void setDisplayOrientation() {
        mDisplayRotation = Util.getDisplayRotation(mActivity);
        mDisplayOrientation = Util.getDisplayOrientation(mDisplayRotation,
                mCameraId);
        mCameraDevice.setDisplayOrientation(mDisplayOrientation);
    }

    private void setCameraState(int state) {
        mCameraState = state;
    }

    private void initializeCapabilities() {

        if (mCameraDevice != null)
            mParameters = mCameraDevice.getParameters();
//        mCameraDevice.autoFocus(new AutoFocusCallback());
        if (mParameters != null) {
            mParameters.getFocusMode();
            mFocusManager.initializeParameters(mParameters);
        }

        if (mCameraDevice != null)
            mParameters = mCameraDevice.getParameters();
    }

    // We separate the parameters into several subsets, so we can update only
    // the subsets actually need updating. The PREFERENCE set needs extra
    // locking because the preference can be changed from GLThread as well.
    private void setCameraParameters(int updateSet) {
        if (mCameraDevice != null) {
            mParameters = mCameraDevice.getParameters();

            if ((updateSet & UPDATE_PARAM_INITIALIZE) != 0) {
                updateCameraParametersInitialize();
            }


            if ((updateSet & UPDATE_PARAM_PREFERENCE) != 0) {
                updateCameraParametersPreference();
            }

            if (mParameters != null)
                mCameraDevice.setParameters(mParameters);
        }
    }

    private void updateCameraParametersInitialize() {
        // Reset preview frame rate to the maximum because it may be lowered by
        // video camera Application.
        List<Integer> frameRates = mParameters.getSupportedPreviewFrameRates();
        if (frameRates != null) {
            Integer max = Collections.max(frameRates);
            mParameters.setPreviewFrameRate(max);
        }

        //mParameters.setRecordingHint(false);

        // Disable video stabilization. Convenience methods not available in API
        // level <= 14
        String vstabSupported = mParameters
                .get("video-stabilization-supported");
        if ("true".equals(vstabSupported)) {
            mParameters.set("video-stabilization", "false");
        }
    }

    private void updateCameraParametersPreference() {

        // Since change scene mode may change supported values,

        //mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//        mModeView.setText(R.string.preview_mode);

        int camOri = CameraHolder.instance().getCameraInfo()[mCameraId].orientation;
        // Set a preview size that is closest to the viewfinder height and has the right aspect ratio.
        List<Camera.Size> sizes = mParameters.getSupportedPreviewSizes();
        Camera.Size optimalSize;
        //if (mode == SettingsActivity.CAPTURE_MODE)
        //	optimalSize = Util.getOptimalPreviewSize(this, sizes, aspectWtoH);
        //else
        {
            int requiredArea = mPreviewWidth * mPreviewHeight;
            optimalSize = Util.getOptimalPreviewSizeByArea(mActivity, sizes, requiredArea);
        }

        Camera.Size original = mParameters.getPreviewSize();

        if (!original.equals(optimalSize)) {
            if (camOri == 0 || camOri == 180) {
                mParameters.setPreviewSize(optimalSize.height, optimalSize.width);
            } else {
                mParameters.setPreviewSize(optimalSize.width, optimalSize.height);
            }

            // Zoom related settings will be changed for different preview
            // sizes, so set and read the parameters to get lastest values

            if (mCameraDevice != null) {
                mCameraDevice.setParameters(mParameters);
                mParameters = mCameraDevice.getParameters();
            }
        }
        // Set JPEG quality.
        int jpegQuality = CameraProfile.getJpegEncodingQualityParameter(
                mCameraId, CameraProfile.QUALITY_HIGH);
        mParameters.setJpegQuality(jpegQuality);

        // For the following settings, we need to check if the settings are
        // still supported by latest driver, if not, ignore the settings.

        //if (Parameters.SCENE_MODE_AUTO.equals(mSceneMode))
        {
            if (mParameters != null) {
                // Set white balance parameter.
                String whiteBalance = "auto";
                if (isSupported(whiteBalance,
                        mParameters.getSupportedWhiteBalance())) {
                    mParameters.setWhiteBalance(whiteBalance);
                }

                String focusMode = mFocusManager.getFocusMode();
                mParameters.setFocusMode(focusMode);

                // Set exposure compensation
                int value = 0;
                int max = mParameters.getMaxExposureCompensation();
                int min = mParameters.getMinExposureCompensation();
                if (value >= min && value <= max) {
                    mParameters.setExposureCompensation(value);
                } else {
                    Log.w(TAG, "invalid exposure range: " + value);
                }
            }
        }

        if (mParameters != null) {
            // Set flash mode.
            String flashMode = "off";
            List<String> supportedFlash = mParameters.getSupportedFlashModes();
            if (isSupported(flashMode, supportedFlash)) {
                mParameters.setFlashMode(flashMode);
            }

            Log.e(TAG, "focusMode=" + mParameters.getFocusMode());
        }

    }

    @Override
    public void onUpdateProcess(String s) {
        onProcessUpdate(null, s, false);
    }

    @Override
    public void onScannedSuccess(boolean b, boolean isMRZRequired) {
        if (b) {
            if (isMRZRequired && g_recogResult.lines.equalsIgnoreCase("")) {
                // only get data of mrz
                checkmrz = 2;
                refreshPreview();
            }/* else {*/
            if (isbothavailable) {
                if (ocrData.getFrontData() != null && ocrData.getBackData() != null && checkmrz == 0) {
                    sendInformation();
                } else {
                    updateData();

                    refreshPreview();
                }
            } else {
                if (checkmrz == 0) {
                    sendInformation();
                }
            }
            /*}*/
        } else {
            refreshPreview();
        }
    }

    @Override
    public void onScannedFailed(String s) {
        Log.e(TAG, s);
    }

    private void GotMRZData() {
        checkmrz = 0;
        if (isbothavailable) {
            if (ocrData.getFrontData() != null && ocrData.getBackData() != null && checkmrz == 0) {
                sendInformation();
            } else {
                updateData();
            }
        } else {
            sendInformation();
        }
    }

    private void updateData() {
        String updateMessage = null;
        if (ocrData.getFrontData() != null && ocrData.getBackData() == null) {
            updateMessage = "Now Scan Back Side of " + ocrData.getCardname();
        } else if (ocrData.getBackData() != null && ocrData.getFrontData() == null) {
            updateMessage = "Now Scan Front Side of " + ocrData.getCardname();
        }
        if (ocrData.getBackData() == null || ocrData.getFrontData() == null && updateMessage != null) {
            onProcessUpdate(updateMessage, null, true);
        }
    }

    private void sendInformation() {
        checkmrz = 0;
//        try {
//            playEffect();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        recogEngine.closeEngine();
        ocrData.setMrzData(g_recogResult);
        if (recogType == RecogType.OCR) {
            onScannedComplete(ocrData);
        } else if (recogType == RecogType.MRZ) {
            onScannedComplete(g_recogResult);
        }
    }

//    ObjectAnimator anim = null;
//
//    void playEffect() {
//        if (isSetPlayer && mediaPlayer != null) {
//            if (audioManager != null)
//                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
//            mediaPlayer.start();
//        }
//    }

}
