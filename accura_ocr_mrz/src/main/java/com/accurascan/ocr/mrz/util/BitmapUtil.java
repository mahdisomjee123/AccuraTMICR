/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.accurascan.ocr.mrz.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import com.accurascan.ocr.mrz.BuildConfig;
import com.docrecog.scan.RecogType;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import java.io.ByteArrayOutputStream;

/**
 * Provides static functions to decode bitmaps at the optimal size
 */
public class BitmapUtil {
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private BitmapUtil() {
    }

    /**
     * Decode an image into a Bitmap, using sub-sampling if the hinted dimensions call for it.
     * Does not crop to fit the hinted dimensions.
     *
     * @param src an encoded image
     * @param w   hint width in px
     * @param h   hint height in px
     * @return a decoded Bitmap that is not exactly sized to the hinted dimensions.
     */
    public static Bitmap decodeByteArray(byte[] src, int w, int h) {
        try {
            // calculate sample size based on w/h
            final BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(src, 0, src.length, opts);
            if (opts.mCancel || opts.outWidth == -1 || opts.outHeight == -1) {
                return null;
            }
            opts.inSampleSize = Math.min(opts.outWidth / w, opts.outHeight / h);
            opts.inJustDecodeBounds = false;
            return BitmapFactory.decodeByteArray(src, 0, src.length, opts);
        } catch (Throwable t) {
            Log.w("TAG", "unable to decode image");
            return null;
        }
    }

    /**
     * Decode an image into a Bitmap, using sub-sampling if the desired dimensions call for it.
     * Also applies a center-crop a la {@link android.widget.ImageView.ScaleType#CENTER_CROP}.
     *
     * @param src an encoded image
     * @param w   desired width in px
     * @param h   desired height in px
     * @return an exactly-sized decoded Bitmap that is center-cropped.
     */
    public static Bitmap decodeByteArrayWithCenterCrop(byte[] src, int w, int h) {
        try {
            final Bitmap decoded = decodeByteArray(src, w, h);
            return centerCrop(decoded, w, h);
        } catch (Throwable t) {
            Log.w("TAG", "unable to crop image");
            return null;
        }
    }

    /**
     * Returns a new Bitmap copy with a center-crop effect a la
     * {@link android.widget.ImageView.ScaleType#CENTER_CROP}. May return the input bitmap if no
     * scaling is necessary.
     *
     * @param src original bitmap of any size
     * @param w   desired width in px
     * @param h   desired height in px
     * @return a copy of src conforming to the given width and height, or src itself if it already
     * matches the given width and height
     */
    public static Bitmap centerCrop(final Bitmap src, final int w, final int h) {
        return crop(src, w, h, 0.5f, 0.5f);
    }

    /**
     * Returns a new Bitmap copy with a crop effect depending on the crop anchor given. 0.5f is like
     * {@link android.widget.ImageView.ScaleType#CENTER_CROP}. The crop anchor will be be nudged
     * so the entire cropped bitmap will fit inside the src. May return the input bitmap if no
     * scaling is necessary.
     * <p>
     * <p>
     * Countrywisedata of changing verticalCenterPercent:
     * _________            _________
     * |         |          |         |
     * |         |          |_________|
     * |         |          |         |/___0.3f
     * |---------|          |_________|\
     * |         |<---0.5f  |         |
     * |---------|          |         |
     * |         |          |         |
     * |         |          |         |
     * |_________|          |_________|
     *
     * @param src                     original bitmap of any size
     * @param w                       desired width in px
     * @param h                       desired height in px
     * @param horizontalCenterPercent determines which part of the src to crop from. Range from 0
     *                                .0f to 1.0f. The value determines which part of the src
     *                                maps to the horizontal center of the resulting bitmap.
     * @param verticalCenterPercent   determines which part of the src to crop from. Range from 0
     *                                .0f to 1.0f. The value determines which part of the src maps
     *                                to the vertical center of the resulting bitmap.
     * @return a copy of src conforming to the given width and height, or src itself if it already
     * matches the given width and height
     */
    public static Bitmap crop(final Bitmap src, final int w, final int h,
                              final float horizontalCenterPercent, final float verticalCenterPercent) {
        if (horizontalCenterPercent < 0 || horizontalCenterPercent > 1 || verticalCenterPercent < 0
                || verticalCenterPercent > 1) {
            throw new IllegalArgumentException(
                    "horizontalCenterPercent and verticalCenterPercent must be between 0.0f and "
                            + "1.0f, inclusive.");
        }
        final int srcWidth = src.getWidth();
        final int srcHeight = src.getHeight();
        // exit early if no resize/crop needed
        if (w == srcWidth && h == srcHeight) {
            return src;
        }
        final Matrix m = new Matrix();
        final float scale = Math.max(
                (float) w / srcWidth,
                (float) h / srcHeight);
        m.setScale(scale, scale);
        final int srcCroppedW, srcCroppedH;
        int srcX, srcY;
        srcCroppedW = Math.round(w / scale);
        srcCroppedH = Math.round(h / scale);
        srcX = (int) (srcWidth * horizontalCenterPercent - srcCroppedW / 2);
        srcY = (int) (srcHeight * verticalCenterPercent - srcCroppedH / 2);
        // Nudge srcX and srcY to be within the bounds of src
        srcX = Math.max(Math.min(srcX, srcWidth - srcCroppedW), 0);
        srcY = Math.max(Math.min(srcY, srcHeight - srcCroppedH), 0);
        final Bitmap cropped = Bitmap.createBitmap(src, srcX, srcY, srcCroppedW, srcCroppedH, m,
                true /* filter */);
        if (DEBUG)/* Log.i("TAG",
                "IN centerCrop, srcW/H=%s/%s desiredW/H=%s/%s srcX/Y=%s/%s" +
                        " innerW/H=%s/%s scale=%s resultW/H=%s/%s",
                srcWidth, srcHeight, w, h, srcX, srcY, srcCroppedW, srcCroppedH, scale,
                cropped.getWidth(), cropped.getHeight());*/
            if (DEBUG && (w != cropped.getWidth() || h != cropped.getHeight())) {
                Log.e("TAG", "last center crop violated assumptions.");
            }
        return cropped;
    }

    /**
     * Decode an image into a Bitmap, and also center cropped according to croppedHeight, croppedWidth
     * {@link Camera.PreviewCallback#onPreviewFrame(byte[] data, Camera camera)}
     *
     * @param data                an encoded image
     * @param camera
     * @param mDisplayOrientation camera orientation
     * @param croppedHeight       center cropped image according to height which is getting from {@link com.accurascan.ocr.mrz.model.InitModel.InitData} cameraHeight
     * @param croppedWidth        center cropped image according to width which is getting from {@link com.accurascan.ocr.mrz.model.InitModel.InitData} cameraWidth
     * @param recogType
     * @return a decoded bitmap with cropped image
     */
    public static Bitmap getBitmapFromData(byte[] data, Camera camera, int mDisplayOrientation, int croppedHeight, int croppedWidth, RecogType recogType) {

        final int width;
        final int height;
        final int format;
        if (camera != null) {
            width = camera.getParameters().getPreviewSize().width;
            height = camera.getParameters().getPreviewSize().height;
            format = camera.getParameters().getPreviewFormat();
        } else {
            return null;
        }

        YuvImage temp = new YuvImage(data, format, width, height, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        temp.compressToJpeg(new Rect(0, 0, temp.getWidth(), temp.getHeight()), 100, os);

        //getting original bitmap of scan result
        try {
            Bitmap bmCard = null;
            Bitmap bmp_org = BitmapFactory.decodeByteArray(os.toByteArray(), 0, os.toByteArray().length);
            os.close();
            Matrix matrix = new Matrix();
            matrix.postRotate(mDisplayOrientation);
            Bitmap bmp1 = Bitmap.createBitmap(bmp_org, 0, 0, bmp_org.getWidth(), bmp_org.getHeight(), matrix, true);

            if (RecogType.OCR == recogType) {
                DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
                Point centerOfCanvas = new Point(dm.widthPixels / 2, dm.heightPixels / 2);
                int left = centerOfCanvas.x - (croppedWidth / 2);
                int top = centerOfCanvas.y - (croppedHeight / 2);
                int right = centerOfCanvas.x + (croppedWidth / 2);
                int bottom = centerOfCanvas.y + (croppedHeight / 2);
                Rect frameRect = new Rect(left, top, right, bottom);

                float widthScaleFactor = (float) dm.widthPixels / (float) height;
                float heightScaleFactor = (float) (dm.heightPixels) / (float) width;
                frameRect.left = (int) (frameRect.left / widthScaleFactor);
                frameRect.top = (int) (frameRect.top / heightScaleFactor);
                frameRect.right = (int) (frameRect.right / widthScaleFactor);
                frameRect.bottom = (int) (frameRect.bottom / heightScaleFactor);
                Rect finalrect = new Rect((int) (frameRect.left), (int) (frameRect.top), (int) (frameRect.right), (int) (frameRect.bottom));

                bmCard = Bitmap.createBitmap(bmp1, finalrect.left, finalrect.top, finalrect.width(), finalrect.height());
            } else if (RecogType.MRZ == recogType || RecogType.PDF417 == recogType) {
                bmCard = BitmapUtil.centerCrop(bmp1, bmp1.getWidth(), bmp1.getHeight() / 3);
            }

            bmp_org.recycle();
            bmp1.recycle();
            return bmCard;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }


    }

//    @Nullable
//    public static Bitmap getBitmap(byte[] imageInBuffer, FrameMetadata metadata) {
////        data.rewind();
////        byte[] imageInBuffer = new byte[data.limit()];
////        data.get(imageInBuffer, 0, imageInBuffer.length);
//        try {
//            YuvImage image =
//                    new YuvImage(
//                            imageInBuffer, ImageFormat.NV21, metadata.getWidth(), metadata.getHeight(), null);
//            if (image != null) {
//                ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                image.compressToJpeg(new Rect(0, 0, metadata.getWidth(), metadata.getHeight()), 80, stream);
//
//                Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
//
//                stream.close();
//                return rotateBitmap(bmp, metadata.getRotation(), metadata.getCameraFacing());
//            }
//        } catch (Exception e) {
//            Log.e("VisionProcessorBase", "Error: " + e.getMessage());
//        }
//        return null;
//    }

    // Rotates a bitmap if it is converted from a bytebuffer.
    private static Bitmap rotateBitmap(Bitmap bitmap, int rotation, int facing) {
        Matrix matrix = new Matrix();
        int rotationDegree = 0;
        switch (rotation) {
            case FirebaseVisionImageMetadata.ROTATION_90:
                rotationDegree = 90;
                break;
            case FirebaseVisionImageMetadata.ROTATION_180:
                rotationDegree = 180;
                break;
            case FirebaseVisionImageMetadata.ROTATION_270:
                rotationDegree = 270;
                break;
            default:
                break;
        }

        // Rotate the image back to straight.}
        matrix.postRotate(rotationDegree);
        if (facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } else {
            // Mirror the image along X axis for front-facing camera image.
            matrix.postScale(-1.0f, 1.0f);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
    }

    public static int getRotation(Activity activity, int mDisplayOrientation) {
        WindowManager windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        int degrees = 0;
        int rotation = windowManager.getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                Log.e("TAG", "Bad rotation value: " + rotation);
        }


        int angle;
        int displayAngle;
        int facing = Camera.CameraInfo.CAMERA_FACING_BACK;
        if (facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            angle = (mDisplayOrientation + degrees) % 360;
            displayAngle = (360 - angle) % 360; // compensate for it being mirrored
        } else { // back-facing
            angle = (mDisplayOrientation - degrees + 360) % 360;
            displayAngle = angle;
        }


        return (angle / 90);
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap rotatedCropBitmap(Bitmap bitmap, Rect rect, float degreesRotated) {

        // crop and rotate the cropped image in one  operation
        Matrix matrix1 = new Matrix();
        matrix1.postRotate(degreesRotated);
//        if (degreesRotated < -45 && degreesRotated >= -135) {
//            degreesRotated = -90;
//            matrix.postRotate(degreesRotated);
//        } else if (degreesRotated > 45 && degreesRotated < 135) {
//            degreesRotated = 90;
//            matrix.postRotate(degreesRotated);
//        } else if (degreesRotated < 45 && degreesRotated > -45) {
//            degreesRotated = 0.5f;
//            matrix.postRotate(degreesRotated);
//        }

//        Log.e("TAG", "rotatedCropBitmap: " + rect.toString());
//        final RectF rectF = new RectF(rect);
//        final Matrix matrix = new Matrix();
//        matrix.setRotate(degreesRotated, rect.centerX(), rect.centerY());
//        matrix.mapRect(rectF);
//        rect.set((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
//        Log.e("TAG", "rotatedCropBitmap 1: " + rectF.toString() + rect.toString());

        return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height(), matrix1, true);
    }

    public static Bitmap rotateRectForOrientation(final int orientation, final Rect fullRect,
                                                final Rect partialRect, Bitmap bitmap) {
        final Matrix matrix = new Matrix();
        // Exif orientation specifies how the camera is rotated relative to the actual subject.
        // First rotate in the opposite direction.
        matrix.setRotate(-orientation);
        final RectF fullRectF = new RectF(fullRect);
        final RectF partialRectF = new RectF(partialRect);
        matrix.mapRect(fullRectF);
        matrix.mapRect(partialRectF);
        // Then translate so that the upper left corner of the rotated full rect is at (0,0).
        matrix.reset();
        matrix.setTranslate(-fullRectF.left, -fullRectF.top);
        matrix.mapRect(fullRectF);
        matrix.mapRect(partialRectF);
        // Orientation transformation is complete.
        fullRect.set((int) fullRectF.left, (int) fullRectF.top, (int) fullRectF.right,
                (int) fullRectF.bottom);
        partialRect.set((int) partialRectF.left, (int) partialRectF.top, (int) partialRectF.right,
                (int) partialRectF.bottom);
        Bitmap result = Bitmap.createBitmap(bitmap, fullRect.left, fullRect.top, fullRect.width(), fullRect.height()/*, matrix, true*/);
         return Bitmap.createBitmap(result, partialRect.left, partialRect.top, partialRect.width(), partialRect.height()/*, matrix, true*/);

    }

    public static float getRotation(float v) {
        Util.logd("TAG", "old 0: " + v);
        if (v < -75 && v >= -105) {
            v = -90;
        } else if (v > 75 && v < 105) {
            v = 90;
        } else if (v > -15 && v < 15) {
            v = 0;
        } /*else if (v > 110 && v < -110) {
            v = 0;
        }*/
        Util.logd("TAG", "new 1: " + v);
        return v;
    }
}