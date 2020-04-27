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

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;

import com.docrecog.scan.RecogEngine;
import com.docrecog.scan.RecogType;

import java.io.ByteArrayOutputStream;

/**
 * Provides static functions to decode bitmaps at the optimal size
 */
public class BitmapUtil {
    private static final boolean DEBUG = false;

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
     * @param data an encoded image
     * @param camera
     * @param mDisplayOrientation camera orientation
     * @param croppedHeight center cropped image according to height which is getting from {@link com.accurascan.ocr.mrz.model.InitModel.InitData} cameraHeight
     * @param croppedWidth center cropped image according to width which is getting from {@link com.accurascan.ocr.mrz.model.InitModel.InitData} cameraWidth
     * @param recogType
     * @return a decoded bitmap with cropped image
     */
    public static Bitmap getBitmapFromData(byte[] data, Camera camera, int mDisplayOrientation, int croppedHeight, int croppedWidth, RecogType recogType) {

        final int width = camera.getParameters().getPreviewSize().width;
        final int height = camera.getParameters().getPreviewSize().height;
        final int format = camera.getParameters().getPreviewFormat();

        YuvImage temp = new YuvImage(data, format, width, height, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        temp.compressToJpeg(new Rect(0, 0, temp.getWidth(), temp.getHeight()), 100, os);

        //getting original bitmap of scan result
        try {
            Bitmap bmCard = null;
            Bitmap bmp_org = BitmapFactory.decodeByteArray(os.toByteArray(), 0, os.toByteArray().length);
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
            } else if (RecogType.MRZ == recogType || RecogType.PDF417 == recogType){
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
}