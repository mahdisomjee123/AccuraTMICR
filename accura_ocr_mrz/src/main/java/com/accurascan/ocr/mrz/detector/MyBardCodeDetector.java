package com.accurascan.ocr.mrz.detector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
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
    private int cardWidth;
    private int cardHeight;
    private int childXOffset;
    private int childYOffset;
    private int childWidth;
    private int childHeight;
    private Point point;

    public Bitmap getBitmap() {
        return bitmap;
    }

    public MyBardCodeDetector(Detector<Barcode> delegate) {
        mDelegate = delegate;
    }

    public SparseArray<Barcode> detect(Frame frame) {
        int width = frame.getMetadata().getWidth();
        int height = frame.getMetadata().getHeight();
        Bitmap storedBitmap = null;
        Bitmap finalb = null;
        Bitmap centercrop = null;
        centercrop = BitmapUtil.getBitmapFromData(frame.getGrayscaleImageData().array(),width,height, ImageFormat.NV21,frame.getMetadata().getRotation(),cardHeight,cardWidth,point,
                childXOffset,childYOffset,childWidth,childHeight);

        if (centercrop == null) {
            YuvImage yuvImage = new YuvImage(frame.getGrayscaleImageData().array(), ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, byteArrayOutputStream);
            byte[] jpegArray = byteArrayOutputStream.toByteArray();
            storedBitmap = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.length);
            Matrix mat = new Matrix();

            int rotationDegree = 0;
            switch (frame.getMetadata().getRotation()) {
                case 1: // ROTATION_90:
                    rotationDegree = 90;
                    break;
                case 2: // ROTATION_180:
                    rotationDegree = 180;
                    break;
                case 3: //ROTATION_270:
                    rotationDegree = 270;
                    break;
                default:
                    break;
            }
            mat.postRotate(rotationDegree);
            finalb = Bitmap.createBitmap(storedBitmap, 0, 0, storedBitmap.getWidth(), storedBitmap.getHeight(), mat, true);
            centercrop = finalb.copy(Bitmap.Config.ARGB_8888, true);
        }
        if (bitmap!=null){
            if (!bitmap.isRecycled()) bitmap.recycle();
            bitmap = null;
        }
        bitmap =  centercrop.copy(Bitmap.Config.ARGB_8888, true);
        Frame.Builder builder = new Frame.Builder().setBitmap(centercrop);
        if (finalb != null) {
            finalb.recycle();
        }
        if (storedBitmap != null) {
            storedBitmap.recycle();
        }

        return mDelegate.detect(builder.build());
    }

    public boolean isOperational() {
        return mDelegate.isOperational();
    }

    public boolean setFocus(int id) {
        return mDelegate.setFocus(id);
    }

    public void setFrameParam(int cardWidth, int cardHeight, int childXOffset, int childYOffset, int childWidth, int childHeight, Point centerPoint) {
        if (cardWidth > 0) this.cardWidth = cardWidth;
        if (cardHeight > 0) this.cardHeight = cardHeight;
        this.childXOffset = childXOffset;
        this.childYOffset = childYOffset;
        this.childWidth = childWidth;
        this.childHeight = childHeight;
        this.point = centerPoint;
    }

    public void setData(Point centerPoint){
        this.point = centerPoint;
    }
}