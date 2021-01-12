package com.docrecog.scan;

import android.graphics.Bitmap;
import android.graphics.Rect;

import androidx.annotation.Keep;

import org.opencv.core.Mat;

@Keep
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

    public Bitmap getBitmap(Bitmap docBmp, int width, int height, boolean b) {
        try {
            String[] reststring = rect.split(",");
            if (reststring.length == 4) {
                Rect rect = new Rect(Integer.parseInt(reststring[0]), Integer.parseInt(reststring[1]), Integer.parseInt(reststring[2]), Integer.parseInt(reststring[3]));

                // {{Getting ImageOpencv.rect are with black border surround to image from SDK
                // and docBmp is without black border
                // should update rect according to docBMP.
                float x = width*0.10f;
                float y = height*0.10f;
                int left = b ? (rect.left - (int) x) : rect.left;
                int top = b ? (rect.top - (int) y) : rect.top;
                int right = b ? (left + rect.width()) : rect.right;
                int bottom = b ? (top + rect.height()) : rect.bottom;
                // }}End Crop image by remove black border

                // {{Start resize rect according to original preview image
                float scaleX = docBmp.getWidth() / (float) width;
                float scaleY = (float) docBmp.getHeight() / (float) height;
                rect.left = (int) (left * scaleX);
                rect.top = (int) (top * scaleY);
                rect.right = (int) (right * scaleX);
                rect.bottom = (int) (bottom * scaleY);
                // }}End resize rect according to original preview image

                //{{Start Crop image by adding black border without resize rect
//                Bitmap newBitmap = Bitmap.createBitmap((int) (docBmp.getWidth() + (docBmp.getWidth()*0.50)), (int)(docBmp.getHeight() + (docBmp.getHeight()*0.50)), Bitmap.Config.ARGB_8888);
//                Canvas canvas = new Canvas(newBitmap);
//                canvas.drawColor(Color.parseColor("#00000000"));
//                float marginLeft = (float) (newBitmap.getWidth() * 0.5 - docBmp.getWidth() * 0.5);
//                float marginTop = (float) (newBitmap.getHeight() * 0.5 - docBmp.getHeight() * 0.5);
//                canvas.drawBitmap(docBmp, marginLeft, marginTop, null);
                // end Crop image by adding black border without resize rect

                rect = new Rect(Math.max(rect.left,(int) 0),
                        Math.max(rect.top,0),
                        Math.min(rect.right,rect.width()+rect.left),
                        Math.min(rect.bottom,(int)rect.top+ rect.height()));
                docBmp = Bitmap.createBitmap(docBmp, rect.left, rect.top, rect.width(), rect.height());
//                newBitmap.recycle();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return docBmp;
    }
}
