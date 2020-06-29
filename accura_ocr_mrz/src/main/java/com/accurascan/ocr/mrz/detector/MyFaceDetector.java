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
import com.google.android.gms.vision.face.Face;

import java.io.ByteArrayOutputStream;

public class MyFaceDetector extends Detector<Face> {
    private Detector<Face> mDelegate;
    private Bitmap bitmap;

    public Bitmap getBitmap() {
        return bitmap;
    }

    public MyFaceDetector(Detector<Face> delegate) {
        mDelegate = delegate;
    }

    public SparseArray<Face> detect(Frame frame) {
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
        if (bitmap!=null && bitmap.isRecycled()){
            bitmap.recycle();bitmap = null;
        }
        bitmap =  BitmapUtil.centerCrop(finalb, finalb.getWidth(), finalb.getHeight() / 3);
//        finalb.recycle();
        storedBitmap.recycle();
        Frame.Builder  builder = new Frame.Builder().setBitmap(bitmap);

        return mDelegate.detect(builder.build());
    }

    public boolean isOperational() {
        return mDelegate.isOperational();
    }

    public boolean setFocus(int id) {
        return mDelegate.setFocus(id);
    }
}