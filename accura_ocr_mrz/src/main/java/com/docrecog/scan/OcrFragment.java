package com.docrecog.scan;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CameraProfile;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.accurascan.ocr.mrz.R;
import com.accurascan.ocr.mrz.ui.OcrResultActivity;
import com.docrecog.scan.textrecognition.GraphicOverlay;
import com.docrecog.scan.textrecognition.Utils;

import org.opencv.core.Mat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * create an instance of this fragment.
 */
public class OcrFragment extends Fragment implements SurfaceHolder.Callback, Camera.PreviewCallback,
        FocusManager.Listener, View.OnTouchListener, RecogEngine.ScanCallBack {

    private static final String TAG = "OcrFragment";

    private Activity cameraActivity;

    protected static final int IDLE = 0; // preview is active
    protected static final int SAVING_PICTURES = 5;
    private static final int PREVIEW_STOPPED = 0;
    // Focus is in progress. The exact focus state is in Focus.java.
    private static final int FOCUSING = 2;
    private static final int SNAPSHOT_IN_PROGRESS = 3;
    private static final int SELFTIMER_COUNTING = 4;
    private static final int FIRST_TIME_INIT = 0;
    private static final int CLEAR_SCREEN_DELAY = 1;
    private static final int SET_CAMERA_PARAMETERS_WHEN_IDLE = 4;
    // number clear
    private static final int TRIGER_RESTART_RECOG = 5;
    private static final int TRIGER_RESTART_RECOG_DELAY = 40; //30 ms
    // The subset of parameters we need to update in setCameraParameters().
    private static final int UPDATE_PARAM_INITIALIZE = 1;
    private static final int UPDATE_PARAM_PREFERENCE = 4;
    private static final int UPDATE_PARAM_ALL = -1;
    private static final long VIBRATE_DURATION = 200L;
    private static boolean LOGV = true;
    private static RecogEngine mCardScanner;
    private static int mRecCnt = 0; //counter for mrz detecting
    private ImageView mFlipImage = null;
    private final Lock _mutex = new ReentrantLock(true);
    OcrData ocrData;

    /////////////////////
    //audio
    MediaPlayer mediaPlayer = null;
    AudioManager audioManager = null;

    final Handler mHandler = new Handler();
    private final CameraErrorCallback mErrorCallback = new CameraErrorCallback();
    private final AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback();
    protected Camera mCameraDevice;
    // The first rear facing camera
    Camera.Parameters mParameters;
    SurfaceHolder mSurfaceHolder;
    // This handles everything about focus.
    FocusManager mFocusManager;
    int mPreviewWidth = 1280;//640;
    int mPreviewHeight = 720;//480;
    /*private CheckBox chkRecogType;*/
    private byte[] cameraData;
    private Camera.Parameters mInitialParams;
    private int mCameraState = PREVIEW_STOPPED;
    private int mCameraId;
    private boolean mOpenCameraFail = false;
    private boolean mCameraDisabled = false, isTouchCalled;
    Thread mCameraOpenThread = new Thread(new Runnable() {
        public void run() {
            try {
                mCameraDevice = Util.openCamera(cameraActivity, mCameraId);
            } catch (Exception e) {
                mOpenCameraFail = true;
                mCameraDisabled = true;
            }
        }
    });
    private boolean mOnResumePending;
    private boolean mPausing;
    private boolean mFirstTimeInitialized;

    private View mPreviewFrame;
    RelativeLayout rel_main;// Preview frame area for SurfaceView.
    private boolean mbVibrate;
    // The display rotation in degrees. This is only valid when mCameraState is
    // not PREVIEW_STOPPED.
    private int mDisplayRotation;
    // The value for android.hardware.Camera.setDisplayOrientation.
    private int mDisplayOrientation;
    Thread mCameraPreviewThread = new Thread(new Runnable() {
        public void run() {
            initializeCapabilities();
            startPreview();
        }
    });

    //new
    private String cardType = "";
    private int cardid;
    private String countryname;
    private TextView tv_side_msg, tv_scan_msg;
    private boolean isbothavailable = false;
    private int checkmrz = 0; //0 ideal 1 not require 2 require
    private int rectW, rectH;
    DisplayMetrics dm;
    RecogResult face_recogResult;

    GraphicOverlay graphicOverlay;
    private View ocrView;
    private OcrCallback mCallback;

    public int getCheckmrz() {
        return checkmrz;
    }

    private static boolean isSupported(String value, List<String> supported) {
        return supported != null && supported.indexOf(value) >= 0;
    }

    public OcrFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ocrView = inflater.inflate(R.layout.fragment_ocr, container, false);
        return ocrView;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            cameraActivity = (Activity) context;
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() instanceof OcrCallback)
            mCallback = (OcrCallback) getActivity();
        if (cameraActivity == null) {
            cameraActivity = (Activity) getActivity();
        }
        ocrData = new OcrData().getInstance(cameraActivity);
        mCameraId = CameraHolder.instance().getBackCameraId();
        //String str = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
        String[] defaultFocusModes = {"continuous-video", "auto", "continuous-picture"};
        mFocusManager = new FocusManager(defaultFocusModes);

        if (getArguments().containsKey("card_code")) {
            cardid = getArguments().getInt("card_code", 0);
            countryname = getArguments().getString("countryname");
        } else {
            cardid = ocrData.getCardId();
            countryname = ocrData.getCountryname();
        }
        ocrData.setCardId(cardid);
        ocrData.setCountryname(countryname);

        dm = getResources().getDisplayMetrics();

        mPreviewFrame = ocrView.findViewById(R.id.camera_preview);
        graphicOverlay = ocrView.findViewById(R.id.camera_preview_graphic_overlay);
        rel_main = ocrView.findViewById(R.id.rel_main);

        mPreviewFrame.setOnTouchListener(this);
        SurfaceView preview = ocrView.findViewById(R.id.camera_preview);
        SurfaceHolder holder = preview.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        tv_side_msg = (TextView) ocrView.findViewById(R.id.tv_side_msg);
        tv_scan_msg = (TextView) ocrView.findViewById(R.id.tv_scan_msg);
        mFlipImage = (ImageView) ocrView.findViewById(R.id.ivFlipImage);
        mFlipImage.setVisibility(View.INVISIBLE);
        // create scan engine.
        if (mCardScanner == null) {
            mCardScanner = new RecogEngine();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Utils.isPermissionsGranted(cameraActivity)) {
            requestCameraPermission();
        } else {
            init();
        }
    }

    private void init() {
        /*
         * To reduce startup time, we start the camera open and preview threads.
         * We make sure the preview is started at the end of onCreate.
         */
//        mCameraOpenThread.start();

        if (SetTempleteImage()) {

            // initialize the scan engine.
            mCardScanner.initEngine(cameraActivity);

            //initialize the result value
            mRecCnt = 0;
            RecogEngine.g_recogResult = new RecogResult();
            RecogEngine.g_recogResult.recType = RecType.INIT;
            RecogEngine.g_recogResult.bRecDone = false;
            RecogEngine.g_recogResult.bFaceReplaced = false;
            RecogEngine.g_recogResult.faceBitmap = null;
            RecogEngine.g_recogResult.docBackBitmap = null;
            RecogEngine.g_recogResult.docFrontBitmap = null;
            ocrData.setFaceBitmap(null);

            face_recogResult = new RecogResult();
            face_recogResult.recType = RecType.FACE;


            ///audio init
            mediaPlayer = MediaPlayer.create(cameraActivity, R.raw.beep);
            audioManager = (AudioManager) cameraActivity.getSystemService(Context.AUDIO_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Utils.isPermissionsGranted(cameraActivity)) {
                requestCameraPermission();
            } else {
                startCamera();
            }


            // do init
            // initializeZoomMax(mInitialParams);

            // Make sure preview is started.
            try {
                mCameraPreviewThread.join();
            } catch (InterruptedException ex) {
                // ignore
            }
            mCameraPreviewThread = null;
//        requestCameraPermission();
            drawOverlay();
        }
    }

    private void startCamera() {
        mCameraOpenThread.start();

        // Make sure camera device is opened.
        try {
            mCameraOpenThread.join();
            mCameraOpenThread = null;
            if (mOpenCameraFail) {
                Util.showErrorAndFinish(cameraActivity, R.string.cannot_connect_camera);
                return;
            } else if (mCameraDisabled) {
                Util.showErrorAndFinish(cameraActivity, R.string.camera_disabled);
                return;
            }
        } catch (InterruptedException ex) {
            // ignore
        }

        mCameraPreviewThread.start();
    }


    // Snapshots can only be taken after this is called. It should be called
    // once only. We could have done these things in onCreate() but we want to
    // make preview screen appear as soon as possible.
    private void initializeFirstTime() {
        if (mFirstTimeInitialized)
            return;

        mCameraId = CameraHolder.instance().getBackCameraId();

        Util.initializeScreenBrightness(cameraActivity.getWindow(), cameraActivity.getContentResolver());
        mFirstTimeInitialized = true;
    }

    // If the activity is paused and resumed, this method will be called in
    // onResume.
    private void initializeSecondTime() {
        //mOrientationListener.enable();
    }

    private void resetScreenOn() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        cameraActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void keepScreenOnAwhile() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        cameraActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    private void drawOverlay() {
//        //draw white rectangle frame according to templete image aspect ratio

        cameraActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewGroup.LayoutParams layoutParams = rel_main.getLayoutParams();
                layoutParams.width = (int) rectW;
                layoutParams.height = rectH;
                rel_main.setLayoutParams(layoutParams);
            }
        });


    }

    @Override
    public void onResume() {
        super.onResume();
        mbVibrate = true;
        if (LOGV) Log.v(TAG, "onResume. hasWindowFocus()=" + cameraActivity.hasWindowFocus());
        if (mCameraDevice == null) {// && isKeyguardLocked()) {
            if (LOGV) Log.v(TAG, "onResume. mOnResumePending=true");
            mOnResumePending = true;
        } else {
            if (LOGV) Log.v(TAG, "onResume. mOnResumePending=false");
            doOnResume();


            mOnResumePending = false;
        }
    }

    protected void doOnResume() {
        if (mOpenCameraFail || mCameraDisabled)
            return;

        // if (mRecogService != null && mRecogService.isProcessing())
        // showProgress(null);
        mRecCnt = 0;
        mPausing = false;

        // Start the preview if it is not started.
        if (mCameraState == PREVIEW_STOPPED) {
            try {
                mCameraDevice = Util.openCamera(cameraActivity, mCameraId);
                initializeCapabilities();
                startPreview();
            } catch (Exception e) {
                Util.showErrorAndFinish(cameraActivity, R.string.cannot_connect_camera);
                return;
            }
        }

        if (mSurfaceHolder != null) {
            // If first time initialization is not finished, put it in the
            // message queue.
            if (!mFirstTimeInitialized) {
                mHandler.sendEmptyMessage(FIRST_TIME_INIT);
            } else {
                initializeSecondTime();
            }
        }

        keepScreenOnAwhile();
        Log.i(TAG, "doOnresume end");
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");

        mOnResumePending = false;
        mPausing = true;

        stopPreview();

        // Close the camera now because other activities may need to use it.
        closeCamera();
        resetScreenOn();

        // Remove the messages in the event queue.
        mHandler.removeMessages(FIRST_TIME_INIT);
        mHandler.removeMessages(TRIGER_RESTART_RECOG);
        super.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        // finalize the scan engine.
        if (mediaPlayer != null)
            mediaPlayer.release();

        super.onDestroy();
    }


    private void initializeCapabilities() {

        if (mCameraDevice != null)
            mInitialParams = mCameraDevice.getParameters();
//        mCameraDevice.autoFocus(new AutoFocusCallback());
        if (mInitialParams != null) {
            mInitialParams.getFocusMode();
            mFocusManager.initializeParameters(mInitialParams);
        }

        if (mCameraDevice != null)
            mParameters = mCameraDevice.getParameters();
    }

    private void startPreview() {

        if (mCameraDevice != null) {
            if (mPausing || cameraActivity.isFinishing())
                return;

            mCameraDevice.setErrorCallback(mErrorCallback);

            // If we're previewing already, stop the preview first (this will blank
            // the screen).
            if (mCameraState != PREVIEW_STOPPED)
                stopPreview();

            setPreviewDisplay(mSurfaceHolder);
            setDisplayOrientation();

            mCameraDevice.setOneShotPreviewCallback(OcrFragment.this);
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
        mDisplayRotation = Util.getDisplayRotation(cameraActivity);
        mDisplayOrientation = Util.getDisplayOrientation(mDisplayRotation,
                mCameraId);
        mCameraDevice.setDisplayOrientation(mDisplayOrientation);
    }

    private void stopPreview() {
        if (mCameraDevice == null)
            return;
        mCameraDevice.stopPreview();
        // mCameraDevice.setPreviewCallback(null);
        setCameraState(PREVIEW_STOPPED);
    }

    private void setCameraState(int state) {
        mCameraState = state;
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

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mPausing) {
            return;
        }

        if (mCameraState != IDLE) {
            mCameraDevice.setOneShotPreviewCallback(OcrFragment.this);
            return;
        }

        Thread recogThread = new Thread(new Runnable() {
            Bitmap bmCard;

            @Override
            public void run() {

                System.out.println("Start Run Thread++");

                bmCard = BitmapUtil.getBitmapFromData(data, camera, mDisplayOrientation, rectH, rectW);

                _mutex.lock();

                checkCard(bmCard);


                _mutex.unlock();


            }
        });
        recogThread.start();
    }

    private void checkCard(Bitmap bmp) {

        int ret = mCardScanner.doRunData(bmp, 1, 0, RecogEngine.g_recogResult);

        if (mRecCnt > 2) {
            if (mRecCnt % 4 == 1) {

                int faceret = mCardScanner.doRunFaceDetect(bmp, face_recogResult);
                System.out.println("faceret+++" + faceret);
                if (face_recogResult.faceBitmap != null) {
                    ocrData.setFaceBitmap(face_recogResult.faceBitmap);
                }
            }
        }
        mRecCnt++;

        ImageOpencv imageOpencv = mCardScanner.nativeCheckCardIsInFrame(cameraActivity, bmp);
        if (imageOpencv != null) {

            showToast(imageOpencv.message);

            if (imageOpencv.isSucess && imageOpencv.mat != null) {
                Mat card = imageOpencv.mat;
                /*if mat isn't null then passing it  for text detection*/
                System.out.println("Check card In frames success");

                // call when successs
                if (getCheckmrz() == 0) {
//                        activityStarted = true;
                    mCardScanner.Loaddata(OcrFragment.this, card, ocrData);
                } else {
                    if (ret == 1 || ret == 2) {
                        GotMRZData();
                    } else {
                        refreshPreview();
                        bmp.recycle();
                    }
                }
            } else {
                refreshPreview();
                bmp.recycle();
            }

        } else {
            refreshPreview();
            bmp.recycle();
        }

    }

    private void refreshPreview() {
        if (!mPausing && mCameraDevice != null) {
            mCameraDevice.setOneShotPreviewCallback(OcrFragment.this);
        }
    }

    public void showToast(final String msg) {
        if (!cameraActivity.isFinishing()) {
//            View view = toast.getView();
//            if (view.getWindowVisibility() != View.VISIBLE) {
//                System.out.println("mesage++"+toast.getView());
//            if (!msg.equalsIgnoreCase(ToastString)) {
//                    toast.setText(msg);
//                    toast.show();
//                ToastString = msg;
            Runnable runnable = new Runnable() {
                public void run() {
                    tv_scan_msg.setText(msg);
                }
            };
            cameraActivity.runOnUiThread(runnable);
//                drawOverlay();
//            }
//            }
        }
    }

    ObjectAnimator anim = null;

    //flip the image
    private void flipImage() {
        try {
            mFlipImage.setVisibility(View.VISIBLE);
            anim = (ObjectAnimator) AnimatorInflater.loadAnimator(cameraActivity, R.animator.flipping);
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
        // Set the preview frame aspect ratio according to the picture size.
        Camera.Size size = mParameters.getPictureSize();
        double aspectWtoH = 0.0;
        if ((camOri == 0 || camOri == 180) && size.height > size.width) {
            aspectWtoH = (double) size.height / size.width;
        } else {
            aspectWtoH = (double) size.width / size.height;
        }

//        if (LOGV)
//            Log.e(TAG, "picture width=" + size.width + ", height=" + size.height);

        // Set a preview size that is closest to the viewfinder height and has the right aspect ratio.
        List<Camera.Size> sizes = mParameters.getSupportedPreviewSizes();
        Camera.Size optimalSize;
        //if (mode == SettingsActivity.CAPTURE_MODE)
        //	optimalSize = Util.getOptimalPreviewSize(this, sizes, aspectWtoH);
        //else
        {
            int requiredArea = mPreviewWidth * mPreviewHeight;

            //optimalSize = Util.getOptimalPreviewSize(this, sizes, aspectWtoH);
            optimalSize = Util.getOptimalPreviewSizeByArea(cameraActivity, sizes, requiredArea);
        }

        // Camera.Size optimalSize = Util.getMaxPreviewSize(sizes, camOri);
        Camera.Size original = mParameters.getPreviewSize();

        Log.i(TAG, " Sensor[" + mCameraId + "]'s orientation is " + camOri);
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
//        if (LOGV)
//            Log.e(TAG, "Preview size is " + optimalSize.width + "x"
//                    + optimalSize.height);

        String previewSize = "";
        previewSize = "[" + optimalSize.width + "x" + optimalSize.height + "]";
//        mPreviewSizeView.setText(previewSize);

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

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
// Make sure we have a surface in the holder before proceeding.
        if (holder.getSurface() == null) {
            Log.d(TAG, "holder.getSurface() == null");
            return;
        }

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
        if (mPausing || cameraActivity.isFinishing())
            return;

        // Set preview display if the surface is being created. Preview was
        // already started. Also restart the preview if display rotation has
        // changed. Sometimes this happens when the device is held in portrait
        // and camera app is opened. Rotation animation takes some time and
        // display rotation in onCreate may not be what we want.
        if (mCameraState == PREVIEW_STOPPED) {
            startPreview();
        } else {
            if (Util.getDisplayRotation(cameraActivity) != mDisplayRotation) {
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
        } else {
            initializeSecondTime();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
    }

    @Override
    public boolean onTouch(View v, MotionEvent e) {
        if (!isTouchCalled && mParameters != null) {
            String focusMode = mParameters.getFocusMode();
            if (focusMode == null || Camera.Parameters.FOCUS_MODE_INFINITY.equals(focusMode)) {
                return false;
            }

            if (e.getAction() == MotionEvent.ACTION_UP) {
                isTouchCalled = true;
                autoFocus();
            }

        }

        return true;
    }

    //    @Override
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

    void playEffect() {
        if (audioManager != null)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer1) {
            }
        });
    }

    private boolean SetTempleteImage() {

        InitModel i1 = mCardScanner.initOcr(cardid, dm.widthPixels);
        if (i1 == null) {
            return false;
        }
        if (i1.getInitData() != null) {
            rectH = i1.getInitData().getCameraHeight();
            rectW = i1.getInitData().getCameraWidth();
            cardType = i1.getInitData().getCardName();
            ocrData.setCardname(cardType);
            tv_side_msg.setText("Scan " + ocrData.getCardname());
            isbothavailable = i1.getInitData().getIsbothavailable();
            return true;
        } else {
            showToast(i1.getResponseMessage());
            return false;
        }

    }


    private void AfterMapping(boolean result, boolean isMRZRequired) {

        if (result) {
            if (isMRZRequired && RecogEngine.g_recogResult.lines.equalsIgnoreCase("")) {
                // only get data of mrz
                checkmrz = 2;
                refreshPreview();
            } else {
                if (isbothavailable) {
                    if (ocrData.getFrontData() != null && ocrData.getBackData() != null && checkmrz == 0) {
                        sendInformation();
                    } else {
                        if (ocrData.getFrontData() != null && ocrData.getBackData() == null) {
                            tv_side_msg.setText("Now Scan Back Side of " + ocrData.getCardname());
                        } else if (ocrData.getBackData() != null && ocrData.getFrontData() == null) {
                            tv_side_msg.setText("Now Scan Front Side of " + ocrData.getCardname());
                        }
                        if (ocrData.getBackData() == null || ocrData.getFrontData() == null) {
                            flipImage();
                        }
                        refreshPreview();
                    }
                } else {
                    sendInformation();
                }
            }
        } else {
            refreshPreview();
        }
    }


    private void sendInformation() {
        checkmrz = 0;

        try {
            playEffect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mCardScanner.closeEngine();
        if (mCallback != null) {
            mCallback.onScannedSuccess(ocrData, RecogEngine.g_recogResult);
        } else {
            OcrData.setOcrResult(ocrData);
            Intent intent = new Intent(cameraActivity, OcrResultActivity.class);
            startActivity(intent);
            cameraActivity.finish();
        }
    }

    //requesting the camera permission
    public void requestCameraPermission() {
        int currentapiVersion = Build.VERSION.SDK_INT;
        if (currentapiVersion >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(cameraActivity, Manifest.permission.CAMERA) &&
                        ActivityCompat.shouldShowRequestPermissionRationale(cameraActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            1);

                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            1);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
        }
        switch (requestCode) {
            case 1:
                // If request is cancelled, the result arrays are empty.
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Start your camera handling here
                    try {
                        init();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    Toast.makeText(cameraActivity, "You declined to allow the app to access your camera", Toast.LENGTH_LONG).show();
                }
        }
    }

    public void GotMRZData() {
        checkmrz = 0;
        if (isbothavailable) {
            if (ocrData.getFrontData() != null && ocrData.getBackData() != null && checkmrz == 0) {
                sendInformation();
            } else {
                if (ocrData.getFrontData() != null && ocrData.getBackData() == null) {
                    tv_side_msg.setText("Now Scan Back Side of " + ocrData.getCardname());
                } else if (ocrData.getBackData() != null && ocrData.getFrontData() == null) {
                    tv_side_msg.setText("Now Scan Front Side of " + ocrData.getCardname());
                }
                if (ocrData.getBackData() == null || ocrData.getFrontData() == null) {
                    flipImage();
                }
            }
        } else {
            sendInformation();
        }
    }

    @Override
    public void onScannedSuccess(boolean data, boolean isMRZRequired) {
        AfterMapping(data, isMRZRequired);
    }

    @Override
    public void onScannedFailed(String s) {

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

    public class CameraErrorCallback implements Camera.ErrorCallback {
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
}
