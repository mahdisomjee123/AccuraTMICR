/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.accurascan.ocr.mrz.camerautil;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera.Area;
import android.hardware.Camera.Parameters;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.accurascan.ocr.mrz.util.Util;

import java.util.ArrayList;
import java.util.List;

// A class that handles everything about focus in still picture mode.
// This also handles the metering area because it is the same as focus area.
public class FocusManager {
    private static final String TAG = "FocusManager";

    private static final int RESET_TOUCH_FOCUS = 0;
    private static final int RESET_FOCUS_FRAME = 1;
    private static final int FOCUS_BEEP_VOLUME = 100;
    private static final int RESET_TOUCH_FOCUS_DELAY = 3000;

    private int mState = STATE_IDLE;
    private static final int STATE_IDLE = 0; // Focus is not active.
    private static final int STATE_FOCUSING = 1; // Focus is in progress.
    // Focus is in progress and the camera should take a picture after focus finishes.
    private static final int STATE_FOCUSING_SNAP_ON_FINISH = 2;
    private static final int STATE_SUCCESS = 3; // Focus finishes and succeeds.
    private static final int STATE_FAIL = 4; // Focus finishes and fails.

    private boolean mInitialized;
    private boolean mFocusAreaSupported;
    private boolean mLockAeAwbNeeded;
    private boolean mAeAwbLock;
    private Matrix mMatrix;
    private View mFocusIndicatorRotateLayout;
    private View mPreviewFrame;
    private List<Area> mFocusArea; // focus area in driver format
    private List<Area> mMeteringArea; // metering area in driver format
    private String mFocusMode;
    private String[] mDefaultFocusModes;
    private String mOverrideFocusMode;
    private Parameters mParameters;
    private Handler mHandler;
    Listener mListener;
    
    private boolean mEnableFaceBeauty;
    private static final int CONTINUOUS_FOCUSING = 0;
    private static final int CONTINUOUS_FOCUS_SUCCESS = 1;
    private static final int CONTINUOUS_FOCUS_FAIL = 2;

    private static final int FOCUS_FRAME_DELAY = 1000;
    private  boolean mIsVideoFocusMode = false;

    public interface Listener {
        public void autoFocus();
        public void cancelAutoFocus();
        public boolean capture();
        public void setFocusParameters();
        public void playSound(int soundId);
    }

    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RESET_TOUCH_FOCUS: {
                    cancelAutoFocus();
                    break;
                }
                case RESET_FOCUS_FRAME:
                    break;	
            }
        }
    }

    public FocusManager(String[] defaultFocusModes) {
        mDefaultFocusModes = defaultFocusModes;
        mHandler = new MainHandler();
        mMatrix = new Matrix();
    }

    public FocusManager(String[] defaultFocusModes,boolean focusMode) {
        this(defaultFocusModes);
        mIsVideoFocusMode = focusMode;
        
    }

    // This has to be initialized before initialize().
    public void initializeParameters(Parameters parameters) {
        mParameters = parameters;
        mFocusAreaSupported = (mParameters.getMaxNumFocusAreas() > 0
                && isSupported(Parameters.FOCUS_MODE_AUTO,
                        mParameters.getSupportedFocusModes()));
        mLockAeAwbNeeded = (mParameters.isAutoExposureLockSupported() ||
                mParameters.isAutoWhiteBalanceLockSupported());
    }

    public void initialize(View previewFrame, Listener listener, int displayOrientation) {
        
        mPreviewFrame = previewFrame;
        mListener = listener;

        Matrix matrix = new Matrix();
        Util.prepareMatrix(matrix, false, displayOrientation,
                previewFrame.getWidth(), previewFrame.getHeight());
        // In face detection, the matrix converts the driver coordinates to UI
        // coordinates. In tap focus, the inverted matrix converts the UI
        // coordinates to driver coordinates.
        matrix.invert(mMatrix);

        if (mParameters != null) {
            mInitialized = true;
        } else {
            Util.logd(TAG, "mParameters is not initialized.");
        }
    }

    public void onShutterDown() {
        if (!mInitialized) return;

        // Lock AE and AWB so users can half-press shutter and recompose.
        if (mLockAeAwbNeeded && !mAeAwbLock) {
            mAeAwbLock = true;
            mListener.setFocusParameters();
        }

        if (needAutoFocusCall()) {
            // Do not focus if touch focus has been triggered.
            if (mState != STATE_SUCCESS && mState != STATE_FAIL) {
                autoFocus();
            }
        }
    }

    public void onShutterUp() {
        if (!mInitialized) return;

        if (needAutoFocusCall()) {
            // User releases half-pressed focus key.
            if (mState == STATE_FOCUSING || mState == STATE_SUCCESS
                    || mState == STATE_FAIL) {
                cancelAutoFocus();
            }
        }

        // Unlock AE and AWB after cancelAutoFocus. Camera API does not
        // guarantee setParameters can be called during autofocus.
        if (mLockAeAwbNeeded && mAeAwbLock && (mState != STATE_FOCUSING_SNAP_ON_FINISH)) {
            mAeAwbLock = false;
            mListener.setFocusParameters();
        }
    }

    public void doSnap() {
        if (!mInitialized) return;

        // If the user has half-pressed the shutter and focus is completed, we
        // can take the photo right away. If the focus mode is infinity, we can
        // also take the photo.
        if (!needAutoFocusCall() || (mState == STATE_SUCCESS || mState == STATE_FAIL)) {
            capture();
        } else if (mState == STATE_FOCUSING) {
            // Half pressing the shutter (i.e. the focus button event) will
            // already have requested AF for us, so just request capture on
            // focus here.
            mState = STATE_FOCUSING_SNAP_ON_FINISH;
        } else if (mState == STATE_IDLE) {
            // We didn't do focus. This can happen if the user press focus key
            // while the snapshot is still in progress. The user probably wants
            // the next snapshot as soon as possible, so we just do a snapshot
            // without focusing again.
            	capture();
        }
    }

    public void onShutter() {
        resetTouchFocus();
        updateFocusUI();
    }

    public void onAutoFocus(boolean focused) {
    	Util.logd(TAG, "onAutoFocus, mState = " + String.valueOf(mState));
        if (mState == STATE_FOCUSING_SNAP_ON_FINISH) {
            // Take the picture no matter focus succeeds or fails. No need
            // to play the AF sound if we're about to play the shutter
            // sound.
            if (focused) {
                mState = STATE_SUCCESS;
            } else {
                mState = STATE_FAIL;
            }
            updateFocusUI();
            capture();
        } else if (mState == STATE_FOCUSING) {
            // This happens when (1) user is half-pressing the focus key or
            // (2) touch focus is triggered. Play the focus tone. Do not
            // take the picture now.
            if (focused) {
                mState = STATE_SUCCESS;
                // Do not play the sound in continuous autofocus mode. It does
                // not do a full scan. The focus callback arrives before doSnap
                // so the state is always STATE_FOCUSING.
                if (!Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.
                        equals(mFocusMode)) {
                	// play sound for focus completed
                }
            } else {
                mState = STATE_FAIL;
            }
            updateFocusUI();
            // If this is triggered by touch focus, cancel focus after a
            // while.
            if (mFocusArea != null) {
                mHandler.sendEmptyMessageDelayed(RESET_TOUCH_FOCUS, FOCUS_FRAME_DELAY);
            }
        } else if (mState == STATE_IDLE) {
            // User has released the focus key before focus completes.
            // Do nothing.
        }
    }

    public boolean onTouch(MotionEvent e) 
    {
        if (!mInitialized || mState == STATE_FOCUSING_SNAP_ON_FINISH) return false;

        mHandler.removeMessages(RESET_FOCUS_FRAME);
        // Let users be able to cancel previous touch focus.
        if ((mFocusArea != null) && (mState == STATE_FOCUSING ||
                    mState == STATE_SUCCESS || mState == STATE_FAIL)) {
            cancelAutoFocus();
        }

        // Initialize variables.
        int x = Math.round(e.getX());
        int y = Math.round(e.getY());
        int focusWidth = mFocusIndicatorRotateLayout.getWidth();
        int focusHeight = mFocusIndicatorRotateLayout.getHeight();
        if (focusWidth == 0 || focusHeight == 0) {
        	Util.logd(TAG, "UI Component not initialized, cancel this touch");
        	return false;
        }
	
	
        Util.logd(TAG, "TouchFocus: touch.x = " + x + " touch.y = " + y);
        int previewWidth = mPreviewFrame.getWidth();
        int previewHeight = mPreviewFrame.getHeight();
        if (mFocusArea == null) {
            mFocusArea = new ArrayList<Area>();
            mFocusArea.add(new Area(new Rect(), 1));
            mMeteringArea = new ArrayList<Area>();
            mMeteringArea.add(new Area(new Rect(), 1));
        }

        // Convert the coordinates to driver format.
        // AE area is bigger because exposure is sensitive and
        // easy to over- or underexposure if area is too small.
        calculateTapArea(focusWidth, focusHeight, 1f, x, y, previewWidth, previewHeight,  mFocusArea.get(0).rect);
        calculateTapArea(focusWidth, focusHeight, 1.5f, x, y, previewWidth, previewHeight,mMeteringArea.get(0).rect);

        // Use margin to set the focus indicator to the touched area.
        RelativeLayout.LayoutParams p =
                (RelativeLayout.LayoutParams) mFocusIndicatorRotateLayout.getLayoutParams();
        int left = Util.clamp(x - focusWidth / 2, 0, previewWidth - focusWidth);
        int top = Util.clamp(y - focusHeight / 2, 0, previewHeight - focusHeight);
        p.setMargins(left, top, 0, 0);
        // Disable "center" rule because we no longer want to put it in the center.
        int[] rules = p.getRules();
        rules[RelativeLayout.CENTER_IN_PARENT] = 0;
        mFocusIndicatorRotateLayout.requestLayout();

        // Set the focus area and metering area.
        mListener.setFocusParameters();
        if (mFocusAreaSupported && (e.getAction() == MotionEvent.ACTION_UP))
        {
            autoFocus();
        }
        else
        {  // Just show the indicator in all other cases.
            updateFocusUI();
            // Reset the metering area in 3 seconds.
            mHandler.removeMessages(RESET_TOUCH_FOCUS);
            mHandler.sendEmptyMessageDelayed(RESET_TOUCH_FOCUS, RESET_TOUCH_FOCUS_DELAY);
        }

        return true;
    }

    public void onPreviewStarted() {
        mState = STATE_IDLE;
    }

    public void onPreviewStopped() {
        mState = STATE_IDLE;
        resetTouchFocus();
        // If auto focus was in progress, it would have been canceled.
        updateFocusUI();
    }

    public void onCameraReleased() {
        onPreviewStopped();
    }

    private void autoFocus() {
        Util.logd(TAG, "Start autofocus.");
        mListener.autoFocus();
        mState = STATE_FOCUSING;
        updateFocusUI();
        mHandler.removeMessages(RESET_TOUCH_FOCUS);
    }

    private void cancelAutoFocus() {
        Util.logd(TAG, "Cancel autofocus.");

        // Reset the tap area before calling mListener.cancelAutofocus.
        // Otherwise, focus mode stays at auto and the tap area passed to the
        // driver is not reset.
        resetTouchFocus();
        mListener.cancelAutoFocus();
        mState = STATE_IDLE;
        updateFocusUI();
        mHandler.removeMessages(RESET_TOUCH_FOCUS);
    }

    private void capture() {
        if (mListener.capture()) {
            mState = STATE_IDLE;
            mHandler.removeMessages(RESET_TOUCH_FOCUS);
        }
    }

    // This can only be called after mParameters is initialized.
    public String getFocusMode() {
        if (mOverrideFocusMode != null) return mOverrideFocusMode;
        List<String> supportedFocusModes = mParameters.getSupportedFocusModes();

        if (mFocusAreaSupported && mFocusArea != null) {
            // Always use autofocus in tap-to-focus.
            mFocusMode = Parameters.FOCUS_MODE_AUTO;
        } else {

            // Try to find a supported focus mode from the default list.
            if (mFocusMode == null) {
                for (int i = 0; i < mDefaultFocusModes.length; i++) {
                    String mode = mDefaultFocusModes[i];
                    if (isSupported(mode, supportedFocusModes)) {
                        mFocusMode = mode;
                        break;
                    }
                }
            }
        }
        if (!isSupported(mFocusMode, supportedFocusModes)) {
            // For some reasons, the driver does not support the current
            // focus mode. Fall back to auto.
            if (isSupported(Parameters.FOCUS_MODE_AUTO,
                    mParameters.getSupportedFocusModes())) {
                mFocusMode = Parameters.FOCUS_MODE_AUTO;
            } else {
                mFocusMode = mParameters.getFocusMode();
            }
        }
        Util.logd(TAG, "get Focus mode :" + mFocusMode);
        return mFocusMode;
    }

    public List<Area> getFocusAreas() {
        return mFocusArea;
    }

    public List<Area> getMeteringAreas() {
        return mMeteringArea;
    }

    public void updateFocusUI() {
    }

    public void resetTouchFocus() {
        if (!mInitialized) return;

        // Put focus indicator to the center.
        RelativeLayout.LayoutParams p =
                (RelativeLayout.LayoutParams) mFocusIndicatorRotateLayout.getLayoutParams();
        int[] rules = p.getRules();
        rules[RelativeLayout.CENTER_IN_PARENT] = RelativeLayout.TRUE;
        p.setMargins(0, 0, 0, 0);

        mFocusArea = null;
        mMeteringArea = null;
    }

    public void calculateTapArea(int focusWidth, int focusHeight, float areaMultiple,
            int x, int y, int previewWidth, int previewHeight, Rect rect) {
        int areaWidth = (int)(focusWidth * areaMultiple);
        int areaHeight = (int)(focusHeight * areaMultiple);
        int left = Util.clamp(x - areaWidth / 2, 0, previewWidth - areaWidth);
        int top = Util.clamp(y - areaHeight / 2, 0, previewHeight - areaHeight);

        RectF rectF = new RectF(left, top, left + areaWidth, top + areaHeight);
        mMatrix.mapRect(rectF);
        Util.rectFToRect(rectF, rect);
    }

    public boolean isFocusCompleted() {
        return mState == STATE_SUCCESS || mState == STATE_FAIL;
    }

    public boolean isFocusingSnapOnFinish() {
        return mState == STATE_FOCUSING_SNAP_ON_FINISH;
    }

    public void removeMessages() {
        mHandler.removeMessages(RESET_TOUCH_FOCUS);
    }

    public void overrideFocusMode(String focusMode) {
        mOverrideFocusMode = focusMode;
    }

    public void setAeAwbLock(boolean lock) {
        mAeAwbLock = lock;
    }

    public boolean getAeAwbLock() {
        return mAeAwbLock;
    }

    public static boolean isSupported(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }

    private boolean needAutoFocusCall() {
        String focusMode = getFocusMode();
        return !(focusMode.equals(Parameters.FOCUS_MODE_INFINITY)
                || focusMode.equals(Parameters.FOCUS_MODE_FIXED)
                || focusMode.equals(Parameters.FOCUS_MODE_EDOF));
    }
    
    public boolean isCameraIdle() {
    	return mState == STATE_IDLE;
    }
}
