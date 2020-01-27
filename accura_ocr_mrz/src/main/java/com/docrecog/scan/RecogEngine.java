package com.docrecog.scan;


import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RecogEngine {

    static {
        try { // for Ocr
            System.loadLibrary("accurasdk");
            Log.e(RecogEngine.class.getSimpleName(), "static initializer: accurasdk");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface ScanCallBack {

        /**
         * This is called after scanned success.
         *
         * @param data
         * @param isMRZRequired
         */
        void onScannedSuccess(boolean data, boolean isMRZRequired);

        /**
         * This is called on scanned failed.
         */
        void onScannedFailed(String s);

    }

    public static RecogResult g_recogResult = new RecogResult();

    private static final String TAG = "PassportRecog";
    public byte[] pDic = null;
    public int pDicLen = 0;
    public byte[] pDic1 = null;
    public int pDicLen1 = 0;
    public static String[] assetNames = {"mMQDF_f_Passport_bottom_Gray.dic", "mMQDF_f_Passport_bottom.dic"};
    private FirebaseVisionTextRecognizer detector;
    private ScanCallBack callBack;

    public static float[] fConf = new float[1]; //face detection confidence
    public static int[] faced = new int[1]; //value for detected face or not

    public static int[] intData = new int[3000];

    public static int NOR_W = 400;//1200;//1006;
    public static int NOR_H = 400;//750;//1451;

    public Context con;

    public RecogEngine() {

    }

    //This is SDK app calling JNI method
    public native int loadDictionary(Context activity, byte[] img_Dic, int len_Dic, byte[] img_Dic1, int len_Dic1,/*, byte[] licenseKey*/AssetManager assets);

    //return value: 0:fail,1:success,correct document, 2:success,incorrect document
    public native int doRecogYuv420p(byte[] yuvdata, int width, int height, int facepick, int rot, int[] intData, Bitmap faceBitmap, boolean unknownVal);

    public native int doRecogBitmap(Bitmap bitmap, int facepick, int[] intData, Bitmap faceBitmap, int[] faced, boolean unknownVal);

    public native int doFaceDetect(Bitmap bitmap, Bitmap faceBitmap, float[] fConf);

    public native String getCameraHeight(int cardid, int widthPixels);

    public native ImageOpencv checkCardInFrames(long matInput, long matOut, Context context, AssetManager assetManager);

    public native String MapData(long src, int[][] boxBoundsLTRB, String[] textElements);

    // for failed -> responseCode = 0,
    // for success -> responseCode = 1 && data
    public InitModel initOcr(int cardid, int widthPixels) {
        String s = getCameraHeight(cardid, widthPixels);
        try {
            if (s != null && !s.equals("")) {
                JSONObject jsonObject = new JSONObject(s);
                return new Gson().fromJson(jsonObject.toString(), InitModel.class);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    // for failed -> responseCode = 0,
    // for success -> responseCode = 1 && data has cardSide is front or back and ocrdata.
    private OcrData.MapData MapDataFunction(long src, FirebaseVisionText firebaseVisionText) {
        int[][] boxBoundsLTRB;
        String[] textElements;

        List<FirebaseVisionText.Element> elementArrayList = new ArrayList<>();
        List<FirebaseVisionText.TextBlock> textBlocks = firebaseVisionText.getTextBlocks();
        for (FirebaseVisionText.TextBlock textBlock : textBlocks) {
            for (FirebaseVisionText.Line line : textBlock.getLines()) {
                for (FirebaseVisionText.Element element : line.getElements()) {
                    if (element == null)
                        continue;
                    elementArrayList.add(element);
                }
            }
        }
        boxBoundsLTRB = new int[elementArrayList.size()][];
        textElements = new String[elementArrayList.size()];
        int counter = 0;
        for (FirebaseVisionText.Element element : elementArrayList) {
            Rect rect = element.getBoundingBox();
            if (rect != null) {
                boxBoundsLTRB[counter] = new int[]{rect.left, rect.top, rect.right, rect.bottom};
                textElements[counter] = element.getText();
                counter++;
            }
        }

        String mapResult = MapData(src, boxBoundsLTRB, textElements);
        try {
            if (mapResult != null && !mapResult.equals("")) {
                JSONObject jsonObject = new JSONObject(mapResult);
                if (jsonObject.getInt("responseCode") == 1) {
                    return new Gson().fromJson(jsonObject.get("data").toString(), OcrData.MapData.class);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;

    }


    // if success then outMat has image data
    public ImageOpencv nativeCheckCardIsInFrame(Context context, Bitmap bmp) {
        Mat clone = new Mat();
        Utils.bitmapToMat(bmp, clone);

        Mat outMat = new Mat();
        ImageOpencv frames = checkCardInFrames(clone.getNativeObjAddr(), outMat.getNativeObjAddr(), context, context.getAssets());
        if (frames != null) {
            if (frames.isSucess) {
                frames.mat = new Mat();
                outMat.copyTo(frames.mat);
            }
            return frames;
        } else {
            return null;
        }
    }

    public static Bitmap bitmapFromMat(Mat mat) {
        if (mat != null) {
            Bitmap bmp = Bitmap.createBitmap(mat.width(), mat.height(), Config.ARGB_8888);
            Utils.matToBitmap(mat, bmp);
            return bmp;
        }
        return null;
    }

    public void initEngine(Context context) {

        //call Sdk  method InitEngine
        // parameter to pass : FaceCallback callback, int fmin, int fmax, float resizeRate, String modelpath, String weightpath, AssetManager assets
        // this method will return the integer value
        //  the return value by initEngine used the identify the particular error
        // -1 - No key found
        // -2 - Invalid Key
        // -3 - Invalid Platform
        // -4 - Invalid License

        con = context;
        getAssetFile(assetNames[0], assetNames[1]);
        int ret = loadDictionary(context, pDic, pDicLen, pDic1, pDicLen1, context.getAssets());
        Log.i("recogPassport", "loadDictionary: " + ret);
        if (ret < 0) {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
            if (ret == -1) {
                builder1.setMessage("No Key Found");
            } else if (ret == -2) {
                builder1.setMessage("Invalid Key");
            } else if (ret == -3) {
                builder1.setMessage("Invalid Platform");
            } else if (ret == -4) {
                builder1.setMessage("Invalid License");
            }

            builder1.setCancelable(true);

            builder1.setPositiveButton(
                    "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();

                        }
                    });

            AlertDialog alert11 = builder1.create();
            alert11.show();
        }

        if (detector == null) {
            detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        }

    }

    public int getAssetFile(String fileName, String fileName1) {

        int size = 0;
        try {
            InputStream is = this.con.getResources().getAssets().open(fileName);
            size = is.available();
            pDic = new byte[size];
            pDicLen = size;
            is.read(pDic);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            InputStream is = this.con.getResources().getAssets().open(fileName1);
            size = is.available();
            pDic1 = new byte[size];
            pDicLen1 = size;
            is.read(pDic1);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return size;
    }

    //If fail, empty string.
    // both => 0
    // only face => 1
    // only mrz => 2
    public int doRunData(byte[] data, int width, int height, int facepick, int rot, RecogResult result) {
        result.faceBitmap = null;
        if (facepick == 1) {
            result.faceBitmap = Bitmap.createBitmap(NOR_W, NOR_H, Config.ARGB_8888);
        }
        long startTime = System.currentTimeMillis();
        int ret = doRecogYuv420p(data, width, height, facepick, rot, intData, result.faceBitmap, true);
        long endTime = System.currentTimeMillis() - startTime;
        if (ret > 0) //>0
        {
            result.ret = ret;
            result.SetResult(intData);
        }
        //Log.i(Defines.APP_LOG_TITLE, "Recog failed - " + String.valueOf(ret) + "- "  + String.valueOf(drawResult[0]));
        return ret;
    }

    //If fail, empty string.
    public int doRunData(Bitmap bmCard, int facepick, int rot, RecogResult result) {
        Bitmap faceBmp = null;
        if (facepick == 1) {
            faceBmp = Bitmap.createBitmap(NOR_W, NOR_H, Config.ARGB_8888);
        }
        long startTime = System.currentTimeMillis();
        //int ret = doRecogYuv420p(data, width, height, facepick,rot,intData,faceBitmap, true);
        int ret = doRecogBitmap(bmCard, facepick, intData, faceBmp, faced, true);
        long endTime = System.currentTimeMillis() - startTime;

        if (ret > 0) {
            if (result.recType == RecType.INIT) {
                if (faced[0] == 0) {
                    result.faceBitmap = null; //face not detected
                    result.recType = RecType.MRZ;
                } else {
                    result.faceBitmap = faceBmp.copy(Config.ARGB_8888, false);
                    result.recType = RecType.BOTH;
                    result.bRecDone = true;
                }
            } else if (result.recType == RecType.FACE) {
				/*if (faced[0] > 0)
				{
					result.faceBitmap = faceBmp.copy(Bitmap.Config.ARGB_8888, false);
					result.bFaceReplaced = true;
				}*/
                result.bRecDone = true;
            }

            result.ret = ret;
            result.SetResult(intData);
        }
        //Log.i(Defines.APP_LOG_TITLE, "Recog failed - " + String.valueOf(ret) + "- "  + String.valueOf(drawResult[0]));
        return ret;
    }

    public int doRunFaceDetect(Bitmap bmImg, RecogResult result) {
        if (result.faceBitmap != null) {
            return 1;

        }
        result.faceBitmap = Bitmap.createBitmap(NOR_W, NOR_H, Config.ARGB_8888);
        long startTime = System.currentTimeMillis();
        int ret = doFaceDetect(bmImg, result.faceBitmap, fConf);
        long endTime = System.currentTimeMillis() - startTime;

        //ret > 0 => detect face ok
        if (ret <= 0) result.faceBitmap = null;

//		if (ret > 0 && result.recType == RecType.MRZ)
//            result.bRecDone = true;

        return ret;
    }


    public void closeEngine() {
        try {
            if (detector != null) {
                detector.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param scanCallBack to get status of data
     * @param mat pass met to retrieve ocr data.
     * @param ocrData to fill data to this object
     */
    public void Loaddata(ScanCallBack scanCallBack, Mat mat, OcrData ocrData) {
        RecogEngine.this.callBack = scanCallBack;

        Bitmap image = RecogEngine.bitmapFromMat(mat);
        System.out.println("Loaddata++");

        if (detector != null) {
            detector.processImage(FirebaseVisionImage.fromBitmap(image))
                    .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                        @Override
                        public void onSuccess(FirebaseVisionText firebaseVisionText) {

                            OcrData.MapData mapData = MapDataFunction(mat.getNativeObjAddr(), firebaseVisionText);
                            List<OcrData.MapData.ScannedData> result = null;
                            if (mapData != null) {
                                result = mapData.getOcr_data();
                            }
                            //check data is not null and empty
                            boolean isdone = result != null && result.size() != 0;
                            System.out.println("done++" + isdone);
                            if (isdone) {
                                if (mapData.getCardSide().toLowerCase().contains("front")) {
                                    if (ocrData.getFrontData() != null) {
                                        isdone = false;
                                    }
                                    ocrData.setFrontData(mapData);
                                    ocrData.setFrontimage(image);
                                } else {
                                    if (ocrData.getBackData() != null)
                                        isdone = false;
                                    ocrData.setBackData(mapData);
                                    ocrData.setBackimage(image);
                                }
                                System.out.println("MappingDone");
                            }
                            if (callBack != null) {
                                callBack.onScannedSuccess(isdone, CheckMRZisRequired(mapData));
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if (callBack != null) {
                                callBack.onScannedFailed(e.getMessage());
                            }
                        }
                    });
        }
    }

    //    return true if mrz is Required
    private boolean CheckMRZisRequired(OcrData.MapData mapData) {
        if (mapData != null) {
            for (OcrData.MapData.ScannedData data : mapData.getOcr_data()) {
                if (data != null) {
                    String key = data.getKey();
                    if (key.equalsIgnoreCase("mrz")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
