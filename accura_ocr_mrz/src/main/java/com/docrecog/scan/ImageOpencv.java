package com.docrecog.scan;

import android.graphics.Bitmap;
import android.graphics.Rect;

import org.opencv.core.Mat;

public class ImageOpencv {
    public String message = "";
    public boolean isSucess = false;
    public Mat mat;
    public String rect;

    public Bitmap getBitmap(Bitmap docBmp) {
        try {
            String[] reststring = rect.split(",");
            if (reststring.length == 4) {
                Rect rect = new Rect(Integer.valueOf(reststring[0]), Integer.valueOf(reststring[1]), Integer.valueOf(reststring[2]), Integer.valueOf(reststring[3]));
                docBmp = Bitmap.createBitmap(docBmp, rect.left, rect.top, rect.width(), rect.height());//memory leak
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return docBmp;
    }
}
