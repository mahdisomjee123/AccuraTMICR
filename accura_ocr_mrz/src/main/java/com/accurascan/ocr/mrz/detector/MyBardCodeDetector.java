package com.accurascan.ocr.mrz.detector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.SparseArray;

import com.accurascan.ocr.mrz.util.BitmapUtil;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;

import java.io.ByteArrayOutputStream;

public class MyBardCodeDetector extends Detector<Barcode> {
    private Detector<Barcode> mDelegate;
    private Bitmap bitmap;

    public Bitmap getBitmap() {
        return bitmap;
    }

    public MyBardCodeDetector(Detector<Barcode> delegate) {
        mDelegate = delegate;
    }

    public SparseArray<Barcode> detect(Frame frame) {
        int width = frame.getMetadata().getWidth();
        int height = frame.getMetadata().getHeight();

        YuvImage yuvImage = new YuvImage(frame.getGrayscaleImageData().array(), ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, byteArrayOutputStream);
        byte[] jpegArray = byteArrayOutputStream.toByteArray();
        Bitmap storedBitmap = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.length);
        Matrix mat = new Matrix();
        mat.postRotate(90);
        Bitmap finalb = Bitmap.createBitmap(storedBitmap, 0, 0, storedBitmap.getWidth(), storedBitmap.getHeight(), mat, true);
        Bitmap centercrop =  BitmapUtil.centerCrop(finalb, finalb.getWidth(), finalb.getHeight() / 3);
        if (bitmap!=null){
            if (!bitmap.isRecycled()) bitmap.recycle();
            bitmap = null;
        }
        bitmap =  centercrop.copy(Bitmap.Config.ARGB_8888, true);
        Frame.Builder builder = new Frame.Builder().setBitmap(centercrop);
        finalb.recycle();
        storedBitmap.recycle();

        return mDelegate.detect(builder.build());
    }

    public boolean isOperational() {
        return mDelegate.isOperational();
    }

    public boolean setFocus(int id) {
        return mDelegate.setFocus(id);
    }
}
