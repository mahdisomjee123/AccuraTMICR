package com.docrecog.scan;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.accurascan.ocr.mrz.R;
import com.accurascan.ocr.mrz.model.ContryModel;
import com.accurascan.ocr.mrz.model.InitModel;
import com.accurascan.ocr.mrz.model.OcrData;
import com.accurascan.ocr.mrz.model.RecogResult;
import com.accurascan.ocr.mrz.util.BitmapUtil;
import com.accurascan.ocr.mrz.util.Util;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RecogEngine {

    static {
        try { // for Ocr
            System.loadLibrary("accurasdk");
            Log.e(RecogEngine.class.getSimpleName(), "Load success");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    abstract static class ScanListener {
        /**
         * This is called to get scanned processed message.
         */
        void onUpdateProcess(String s) {
        }


        /**
         * This is called after scanned success.
         *
         * @param isDone
         * @param isMRZRequired
         */
        abstract void onScannedSuccess(boolean isDone, boolean isMRZRequired);

        /**
         * This is called on scanned failed.
         */
        void onScannedFailed(String s) {
        }

    }

    public class SDKModel {
        public int i;
        public boolean isMRZEnable = false;
        public boolean isOCREnable = false;
        public boolean isAllBarcodeEnable = false;
    }

    public static final int SCAN_TITLE_OCR_FRONT = 1;
    public static final int SCAN_TITLE_OCR_BACK = 2;
    public static final int SCAN_TITLE_OCR = 3;
    public static final int SCAN_TITLE_MRZ_PDF417_FRONT = 4;
    public static final int SCAN_TITLE_MRZ_PDF417_BACK = 5;
    public static final int SCAN_TITLE_DLPLATE = 6;
    public static final int SCAN_TITLE_DEFAULT = 0;
    private static final String TAG = "PassportRecog";
    private byte[] pDic = null;
    private int pDicLen = 0;
    private byte[] pDic1 = null;
    private int pDicLen1 = 0;
    private static String[] assetNames = {"mMQDF_f_Passport_bottom_Gray.dic", "mMQDF_f_Passport_bottom.dic"};
    private static FirebaseVisionTextRecognizer detector;
    private static FirebaseVisionFaceDetector faceDetector;
    private boolean findFace = false;
    private boolean isComplete = false;
    private ScanListener callBack;
    static String nM;
    static float mT = 15;
    Boolean isMrzEnable = true;
    static float v = 5f;

    private static float[] fConf = new float[3]; //face detection confidence
    private static int[] faced = new int[3]; //value for detected face or not

    private static int[] intData = new int[3000];

    private static int NOR_W = 400;//1200;//1006;
    private static int NOR_H = 400;//750;//1451;

    private Context con;
    private Activity activity;

    public RecogEngine() {

    }

    public RecogEngine(Activity activity) {
        this.activity = activity;
    }

    void setCallBack(ScanListener scanListener, RecogType recogType) {
        this.callBack = scanListener;
        isComplete = false;
        if (recogType == RecogType.OCR) {
            updateData("Back");
        }
    }

    void removeCallBack(ScanListener scanListener) {
        this.callBack = scanListener;
    }

    //This is SDK app calling JNI method
    private native int loadDictionary(Context activity, String s, byte[] img_Dic, int len_Dic, byte[] img_Dic1, int len_Dic1,/*, byte[] licenseKey*/AssetManager assets);
//    public native int loadDictionary(Context activity, byte[] img_Dic, int len_Dic, byte[] img_Dic1, int len_Dic1,/*, byte[] licenseKey*/AssetManager assets);

    //return value: 0:fail,1:success,correct document, 2:success,incorrect document
    private native int doRecogYuv420p(byte[] yuvdata, int width, int height, int facepick, int rot, int[] intData, Bitmap faceBitmap);

    public native String doCheckData(byte[] yuvdata, int width, int height);

    private native int doRecogBitmap(Bitmap bitmap, int facepick, int[] intData, Bitmap faceBitmap, int[] faced, boolean unknownVal);

    private native int doFaceDetect(Bitmap bitmap, Bitmap faceBitmap, float[] fConf);

    private native String doFaceCheck(long l, float v);

    private native String loadData(Context context, int[] i);

    /**
     * Set Blur Percentage to allow blur on document
     *
     * @param context        Activity context
     * @param blurPercentage is 0 to 100, 0 - clean document and 100 - Blurry document
     * @return 1 if success else 0
     */
    public native int setBlurPercentage(Context context, int blurPercentage, String errorMessage);

    /**
     * Set Blur Percentage to allow blur on detected Face
     *
     * @param context            Activity context
     * @param faceBlurPercentage is 0 to 100, 0 - clean face and 100 - Blurry face
     * @return 1 if success else 0
     */
    public native int setFaceBlurPercentage(Context context, int faceBlurPercentage, String errorMessage);

    /**
     * @param context
     * @param minPercentage
     * @param maxPercentage
     * @return 1 if success else 0
     */
    public native int setGlarePercentage(Context context, int minPercentage, int maxPercentage, String errorMessage);

    /**
     * Set CheckPhotoCopy to allow photocopy document or not
     *
     * @param context
     * @param isCheckPhotoCopy if true then reject photo copy document else vice versa
     * @return 1 if success else 0
     */
    public native int isCheckPhotoCopy(Context context, boolean isCheckPhotoCopy, String errorMessage);

    /**
     * set Hologram detection to allow hologram on face or not
     *
     * @param context
     * @param isDetectHologram if true then reject hologram is on face else it is allow .
     * @return 1 if success else 0
     */
    public native int SetHologramDetection(Context context, boolean isDetectHologram, String errorMessage);

    /**
     * set light tolerance to detect light on document if low light
     *
     * @param context
     * @param tolerance is 0 to 100, 0 - allow full dark document and 100 - allow full bright document
     * @return 1 if success else 0
     */
    public native int setLowLightTolerance(Context context, int tolerance, String errorMessage);

    /**
     * set motion threshold to detect motion on camera document
     *
     * @param context
     * @param motionThreshold
     * @return
     */
    private native int setMotionThreshold(Context context, int motionThreshold, @NonNull String message);

    private native String loadOCR(Context context, AssetManager assetManager, int countryid, int cardid, int widthPixels);

    private native ImageOpencv checkDocument(long matInput, long matOut, float v);

    private native String recognizeData(long src, int[][] boxBoundsLTRB, String[] textElements);

    private native int updateData(String s);

    private native String loadScanner(Context context, AssetManager assetManager, int countryid);

    private native String loadLicense(Context context, int countryid, int cardid);

    private native int doBlurCheck(long srcMat);

    private native String doLightCheck(long srcMat);

    private native int closeOCR(int i);

    private native int doDetectNumberPlate(String s, int[] intData, int id, int card_id);

    public int setMotionData(Activity activity, int motionThreshold, @NonNull String message) {
        mT = motionThreshold;
        nM = message;
        return setMotionThreshold(activity, motionThreshold, message);
    }

    /**
     * Must have to call initEngine on app open
     *
     * @param context
     * @return
     */
    public SDKModel initEngine(Context context) {
        /*
           initialized sdk by InitEngine from thread
          The return value by initEngine used the identify
          Return ret < 0 if license not valid
          -1 - No key found
          -2 - Invalid Key
          -3 - Invalid Platform
          -4 - Invalid License

          Return ret > 0 if license is valid
          1 - for MRZ only
          2 - for Ocr only
          3 - for PDF417 only
          4 - for Ocr + MRZ both
          5 - for Ocr + PDF417 both
          6 - for PDF417 + MRZ both
          7 - for Ocr + MRZ + PDF417 both
         */
        this.con = context;

        getAssetFile(assetNames[0], assetNames[1]);
//        File file = loadClassifierData(context);
        int ret = loadDictionary(context, /*file != null ? file.getAbsolutePath() : */"", pDic, pDicLen, pDic1, pDicLen1, context.getAssets());
        Log.i("recogPassport", "loadDictionary: " + ret);
        nM = "Keep Document Steady";
        if (ret < 0) {
            String message = "";
            if (ret == -1) {
                message = "No Key Found";
            } else if (ret == -2) {
                message = "Invalid Key";
            } else if (ret == -3) {
                message = "Invalid Platform";
            } else if (ret == -4) {
                message = "Invalid License";
            }
            if (!(context instanceof Activity)) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            } else {
                String finalMessage = message;
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                        builder1.setMessage(finalMessage);

                        builder1.setCancelable(true);

                        builder1.setPositiveButton(
                                "OK",
                                (dialog, id) -> dialog.cancel());

                        AlertDialog alert11 = builder1.create();
                        alert11.show();
                    }
                });
            }
        }
        SDKModel sdkModel = new SDKModel();
        sdkModel.isMRZEnable = ret == 1 || ret == 4 || ret == 6 || ret == 7;
        sdkModel.isOCREnable = ret == 2 || ret == 4 || ret == 5 || ret == 7;
        sdkModel.isAllBarcodeEnable = ret == 3 || ret == 5 || ret == 6 || ret == 7;
        sdkModel.i = ret;
        return sdkModel;
    }

    public List<ContryModel> getCardList(Context context) {
        int[] i = new int[1];
        String s = loadData(context, i);
        try {
            if (i[0] > 0) {
                if (s != null && !s.equals("")) {
                    JSONArray jsonObject = new JSONArray(s);
                    Type listType = new TypeToken<List<ContryModel>>() {
                    }.getType();
                    return new Gson().fromJson(jsonObject.toString(), listType);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Initialized ocr
     *
     * @param scanListener to get ocr update
     * @param context      is activity context
     * @param countryId    is country Id
     * @param cardId       is Card Id
     * @return {@link InitModel}
     */
    // for failed -> responseCode = 0,
    // for success -> responseCode = 1
    protected InitModel initOcr(ScanListener scanListener, Context context, int countryId, int cardId) {
        findFace = false;
        if (scanListener != null) {
            this.callBack = scanListener;
        } else {
            throw new RuntimeException(" must implement " + ScanListener.class.getName());
        }
        if (context instanceof Activity) {
            this.activity = (Activity) context;
        }
        if (detector == null) {
            detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        }
        isComplete = false;
//        init();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        String s = loadOCR(context, context.getAssets(), countryId, cardId, dm.widthPixels);
        try {
            if (s != null && !s.equals("")) {
                JSONObject jsonObject = new JSONObject(s);
                InitModel initModel = new Gson().fromJson(jsonObject.toString(), InitModel.class);
                isMrzEnable = initModel.getInitData() != null ? initModel.getInitData().getMRZEnable() : false;
                return initModel;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void init() {
        // To initialise the detector

        if (faceDetector == null) {
//            FirebaseVisionFaceDetectorOptions options =
//                    new FirebaseVisionFaceDetectorOptions.Builder()
////                            .setClassificationMode(FirebaseVisionFaceDetectorOptions.FAST)
////                            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
////                            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
////                            .enableTracking()
//                            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
//                            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
//                            .setClassificationMode(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
//                            .enableTracking()
//                            .build();
//
//            faceDetector = FirebaseVision.getInstance().getVisionFaceDetector(options);

            FirebaseVisionFaceDetectorOptions options =
                    new FirebaseVisionFaceDetectorOptions.Builder()
                            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                            .setClassificationMode(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
                            .enableTracking()
                            .build();

            faceDetector = FirebaseVision.getInstance().getVisionFaceDetector(options);
        }
    }

    /**
     * Initialized ocr
     *
     * @param context   is activity context
     * @param countryId is country code
     * @return {@link InitModel}
     */
    // for failed -> responseCode = 0,
    // for success -> responseCode = 1
    InitModel initScanner(Context context, int countryId) {

        String s = loadScanner(context, context.getAssets(), countryId);
        try {
            if (s != null && !s.equals("")) {
                JSONObject jsonObject = new JSONObject(s);
                InitModel initModel = new Gson().fromJson(jsonObject.toString(), InitModel.class);
                return initModel;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Initialized ocr
     *
     * @param context   is activity context
     * @param countryId is country code
     * @return {@link InitModel}
     */
    // for failed -> responseCode = 0,
    // for success -> responseCode = 1
    InitModel initLicense(Context context, int countryId, int cardId) {

        String s = loadLicense(context, countryId, cardId);
        try {
            if (s != null && !s.equals("")) {
                JSONObject jsonObject = new JSONObject(s);
                InitModel initModel = new Gson().fromJson(jsonObject.toString(), InitModel.class);
                return initModel;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    boolean checkValid(Bitmap bitmap) {
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);
        int i = doBlurCheck(src.getNativeObjAddr());
        src.release();
        return i == 0;
    }

    boolean checkLight(Bitmap bitmap) {
        int ret = 0;
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);
        String s = doLightCheck(src.getNativeObjAddr());
        try {
            if (s != null && !TextUtils.isEmpty(s)) {
                JSONObject jsonObject = new JSONObject(s);
                ret = jsonObject.getInt("responseCode");
                if (ret > 0) {
                    callBack.onUpdateProcess(jsonObject.getString("responseMessage"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        src.release();
        return ret == 1;
    }

    int doCheckFrame(byte[] bytes, int w, int h) {
        int ret = 0;
        if (bytes != null && w > 0 && h > 0) {
            try {
                String s = doCheckData(bytes, w, h);
                if (s != null && !TextUtils.isEmpty(s)) {
                    JSONObject jsonObject = new JSONObject(s);
                    ret = jsonObject.getInt("responseCode");
                    if (ret >= 0) {
                        nM = jsonObject.getString("responseMessage");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                ret = 0;
            }
        }
        return ret;
    }

    /**
     * To get scanned data from document
     *
     * @param src  is an Mat::getNativeObjAddr()
     * @param text is data
     * @return for failed -> responseCode = 0,
     * for success -> responseCode = 1 && data has cardSide is front or back and ocrdata.
     */
    private OcrData.MapData MapDataFunction(long src, FirebaseVisionText text) {
        int[][] boxBoundsLTRB;
        String[] textElements;

        List<FirebaseVisionText.Element> elementArrayList = new ArrayList<>();
        List<FirebaseVisionText.TextBlock> textBlocks = text.getTextBlocks();
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

        String mapResult = recognizeData(src, boxBoundsLTRB, textElements);
        try {
            if (mapResult != null && !mapResult.equals("")) {
                JSONObject jsonObject = new JSONObject(mapResult);
                int ic = jsonObject.getInt("responseCode");
                if (ic == 1) {
                    return new Gson().fromJson(jsonObject.get("data").toString(), OcrData.MapData.class);
                } else if (ic == 10) {
                    String message = jsonObject.getString("responseMessage");
                    if (!message.isEmpty() && this.callBack != null) {
                        this.callBack.onUpdateProcess(message);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * call this method to check document
     *
     * @param bmp input image to scan document
     * @return if success then outMat has image data else outMAt is null
     */
    ImageOpencv checkCard(Bitmap bmp) {
        ImageOpencv frames;
        Mat clone = new Mat();
        Utils.bitmapToMat(bmp, clone);

        Mat outMat = new Mat();
        frames = checkDocument(clone.getNativeObjAddr(), outMat.getNativeObjAddr(), v);
        if (frames != null) {
            if (!frames.message.isEmpty() && this.callBack != null) {
                this.callBack.onUpdateProcess(frames.message);
            }
            if (frames.isSucess) {
                frames.mat = new Mat();
                outMat.copyTo(frames.mat);
                this.callBack.onUpdateProcess("3"/*"Processing..."*/);
            } else {
                bmp.recycle();
                frames.mat = null;
            }
            return frames;
        } else {
            return null;
        }
    }

    /**
     * To get MRZ data from documnet
     *
     * @param bmCard document bitmap
     * @param result {@link RecogResult} to get data
     * @return 0 if failed and >0 if success
     */
    int doRunData(Bitmap bmCard, int facepick, RecogResult result) {
        int ret = 1;
        //If fail, empty string.
        // both => 0
        // only face => 1
        // only mrz => 2
       /* if (facepick == 1 && result.faceBitmap == null) {
            detectFace(bmCard, result, new ScanListener() {
                @Override
                public void onUpdateProcess(String s) {
//                    doRunData(bmCard, 0, result);
                }

                @Override
                public void onScannedSuccess(boolean isDone, boolean isMRZRequired) {
                    callBack.onScannedSuccess(true,false);
//                    doRunData(bmCard, 0, result);
                }

                @Override
                public void onScannedFailed(String s) {
                    callBack.onScannedSuccess(false, false);
                }
            });
        }*/ /*else {*/
        Bitmap faceBmp = null;
//            if (facepick == 1) {
//                faceBmp = Bitmap.createBitmap(NOR_W, NOR_H, Config.ARGB_8888);
//            }
        ret = doRecogBitmap(bmCard, 0, intData, faceBmp, faced, true);

        if (ret > 0) {
            if (result.recType == RecType.INIT) {
                if (faced[0] == 0) {
                    result.faceBitmap = null; //face not detected
                    result.recType = RecType.MRZ;
                } else {
                    if (faceBmp != null) {
                        result.faceBitmap = faceBmp.copy(Config.ARGB_8888, false);
                        if (faced[1] < 400 || faced[2] < 400)
                            result.faceBitmap = Bitmap.createBitmap(result.faceBitmap, 0, 0, faced[1], faced[2]);
                        result.recType = RecType.BOTH;
                        result.bRecDone = true;
                    }
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
//        }
        return ret;
    }

    int doRunFaceDetect(Bitmap bmImg, RecogResult result) {
        if (result.faceBitmap != null) {
            return 1;

        }
        if (/*checkValid(bmImg) &&*/ result.faceBitmap == null) {
            detectFace(bmImg, null, result, new ScanListener() {

                @Override
                public void onScannedSuccess(boolean isDone, boolean isMRZRequired) {
                    callBack.onScannedSuccess(true, true);
                }

            });
        }

//        result.faceBitmap = Bitmap.createBitmap(NOR_W, NOR_H, Config.ARGB_8888);
//        int ret = doFaceDetect(bmImg, result.faceBitmap, fConf);
//
//        //ret > 0 => detect face ok
//        if (ret <= 0) result.faceBitmap = null;
//        else if (fConf[1] < 400 || fConf[2] < 400)
//            result.faceBitmap = Bitmap.createBitmap(result.faceBitmap, 0, 0, (int) fConf[1], (int) fConf[2]);
//
//        if (ret > 0 && result.recType == RecType.MRZ)
//            result.bRecDone = true;

        return 0;
    }

    /**
     * To detect face from your camera frame
     * @param i
     * @param bitmap         document bitmap
     * @param ocrData        to save face image
     * @param result         to save face image
     * @param scanListener   call back required to getting success or failed response
     */
    void doFaceDetect(int i, Bitmap bitmap, OcrData ocrData, RecogResult result, ScanListener scanListener) {

        detectFace(bitmap, ocrData, result, new ScanListener() {
            @Override
            public void onUpdateProcess(String s) {

            }

            @Override
            public void onScannedSuccess(boolean isDone, boolean isMRZRequired) {
                scanListener.onScannedSuccess(true, true);
            }

            @Override
            public void onScannedFailed(String s) {
                if (s.equals("1") && result != null && i % 2 == 0) {
                    doFaceDetect(1, BitmapUtil.rotateBitmap(bitmap, 180), ocrData, result, scanListener);
                } else {
                    callBack.onScannedSuccess(false, false);
                }
            }
        });
    }

    /**
     * To detect and recognize Driving License Plate
     * @param bmCard       camera frame
     * @param countryId    is country Id
     * @param cardId       is Card Id
     * @param result       to save data in object
     */
    public void doRecognition(Bitmap bmCard, int countryId, int cardId, RecogResult result) {
        if (detector == null) {
            detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        }
        final Bitmap docBmp = bmCard.copy(Bitmap.Config.ARGB_8888, false);
        detector.processImage(FirebaseVisionImage.fromBitmap(docBmp))
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText text) {
                        result.lines = "";
                        StringBuilder MlKitOcr = new StringBuilder();
                        List<FirebaseVisionText.TextBlock> textBlocks = text.getTextBlocks();
                        for (FirebaseVisionText.TextBlock textBlock : textBlocks)
                            for (FirebaseVisionText.Line line : textBlock.getLines())
                                for (FirebaseVisionText.Element element : line.getElements()) {
                                    if (element == null)
                                        continue;
                                    if (element.getBoundingBox() != null) {
                                        int heightDifference = element.getBoundingBox().bottom - element.getBoundingBox().top;
//                                        Util.logd("ocr_log", "h, text -> " + heightDifference + "," + element.getText());
                                        if (heightDifference > 20) {
                                            MlKitOcr.append(element.getText());
                                        }
                                    }
                                }

                        docBmp.recycle();
                        if (!TextUtils.isEmpty(MlKitOcr)) {
                            int ret = doDetectNumberPlate(MlKitOcr.toString(), intData, countryId, cardId);
                            if (ret > 0) {
                                int i, k = 0, len;
                                byte[] tmp = new byte[100];
                                len = intData[k++];
                                for (i = 0; i < len; ++i) tmp[i] = (byte) intData[k++];
                                tmp[i] = 0;
                                result.lines = RecogResult.convchar2string(tmp);
                                result.docFrontBitmap = bmCard.copy(Bitmap.Config.ARGB_8888, false);
                                bmCard.recycle();
                                if (RecogEngine.this.callBack != null) {
                                    RecogEngine.this.callBack.onScannedSuccess(true,false);
                                }
                            } else if (RecogEngine.this.callBack != null) {
                                if (ret < 0) RecogEngine.this.callBack.onUpdateProcess("DL Plate not Added");
                                bmCard.recycle();
                                RecogEngine.this.callBack.onScannedSuccess(false,false);
                            }
                        } else if (RecogEngine.this.callBack != null) {
                            bmCard.recycle();
                            RecogEngine.this.callBack.onScannedSuccess(false,false);
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });

    }

    /**
     * Call this method if document is valid after {@see checkCard(Bitmap bmp)}
     *
//     * @param scanListener to get scanned data
     * @param src
     * @param mat          pass met to retrieve ocr data.
     * @param ocrData      to fill data to this object
     */
    void doRecognition(/*ScanListener scanListener, */Bitmap src, Mat mat, OcrData ocrData) {
//        RecogEngine.this.callBack = scanListener;

//        if (findFace == true && ocrData.getFaceImage() == null) {
//            try {
//                Bitmap image = bitmapFromMat(mat);
//                detectFace(image, null, null, 0, ocrData, null, new ScanListener() {
//
//                    @Override
//                    public void onScannedSuccess(boolean isDone, boolean isMRZRequired) {
//                        detectText(src, mat, ocrData);
//                    }
//
//                    @Override
//                    void onScannedFailed(String s) {
//                        callBack.onScannedSuccess(false, false);
//                    }
//                });
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else {
        detectText(src, mat, ocrData);
//        }

    }

    private void detectText(Bitmap src, Mat mat, OcrData ocrData) {
        Bitmap image = bitmapFromMat(mat);
        if (detector == null) {
            detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        }

        detector.processImage(FirebaseVisionImage.fromBitmap(image))
                .addOnSuccessListener(visionText -> {

                    OcrData.MapData mapData = MapDataFunction(mat.getNativeObjAddr(), visionText);
//                    if (detector != null) {
//                        try {
//                            detector.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
                    List<OcrData.MapData.ScannedData> result = null;
                    if (mapData != null) {
                        result = mapData.getOcr_data();
                        findFace = mapData.getFace();
                    }
                    //check data is not null and empty
                    boolean isdone = result != null && result.size() != 0;
                    boolean isFinalDone = isdone;
                    boolean isContinue = true;
                    System.out.println("done++" + isdone);
                    if (isdone) {
                        if (mapData.getCardSide().toLowerCase().contains("front")) {
                            if (ocrData.getFrontData() != null) {
                                isdone = false;
//                                ocrData.getFrontimage().recycle();
                            } else ocrData.setFrontimage(src.copy(Config.ARGB_8888, false));
                            ocrData.setFrontData(mapData);
                        } else {
                            if (ocrData.getBackData() != null) {
                                isdone = false;
                                ocrData.getBackimage().recycle();
                            } else ocrData.setBackimage(src.copy(Config.ARGB_8888, false));
                            ocrData.setBackData(mapData);
                        }
                    }
                    try {
                        boolean finalIsdone = isFinalDone;
                        if (findFace) {
                            isContinue = false;
                            boolean finalIsdone1 = isdone;
                            detectFace(src.copy(Config.ARGB_8888, false), ocrData, null, new ScanListener() {
                                @Override
                                void onScannedSuccess(boolean isDone, boolean isMRZRequired) {
                                    boolean isFinal;
                                    if (isComplete) isFinal = finalIsdone1;
                                    else isFinal = finalIsdone;
                                    if (isFinal && ocrData.getFrontData() != null && ocrData.getFaceImage() != null && ocrData.getBackData() == null) {
                                        updateData(mapData.card_side);
                                        isComplete = true;
                                    }
                                    if (callBack != null) {
                                        findFace = false;
                                        callBack.onScannedSuccess(isFinal, isMrzEnable && CheckMRZisRequired(mapData));
                                    }
                                    src.recycle();
                                    mat.release();
                                }

                                @Override
                                void onScannedFailed(String s) {
//                                    if (finalIsdone && ocrData.getFrontData() != null && ocrData.getFaceImage() != null && ocrData.getBackData() == null) {
//                                        updateData(mapData.card_side);
//                                    }
                                    callBack.onScannedSuccess(false, isMrzEnable && CheckMRZisRequired(mapData));
                                    src.recycle();
                                    mat.release();
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (isContinue && callBack != null) {
                        if (isdone && ocrData.getFrontData() != null && ocrData.getBackData() == null) {
                            updateData(mapData.card_side);
                        }
                        callBack.onScannedSuccess(isdone, isMrzEnable && CheckMRZisRequired(mapData));
                        src.recycle();
                        image.recycle();
                        mat.release();
                    }
                })
                .addOnFailureListener(e -> {
//                    if (detector != null) {
//                        try {
//                            detector.close();
//                        } catch (IOException e1) {
//                            e1.printStackTrace();
//                        }
//                    }
                    src.recycle();
                    image.recycle();
                    mat.release();
                    if (callBack != null) {
                        callBack.onScannedFailed(e.getMessage());
                    }
                });
    }

    public void detectFace(Bitmap image, OcrData ocrData, RecogResult result, ScanListener scanListener) {
        if (/*(ocrData != null && ocrData.getFaceImage() != null) || */(result != null && result.faceBitmap != null)) {
            if (scanListener != null) {
                scanListener.onScannedSuccess(true, true);
            }
            image.recycle();
            return;
        }
        if (image != null && !image.isRecycled()) {
            init();

//            FrameMetadata frameMetadata = new FrameMetadata.Builder()
//                    .setWidth(camera.getParameters().getPreviewSize().width)
//                    .setHeight(camera.getParameters().getPreviewSize().height)
//                    .setFormate(camera.getParameters().getPreviewFormat())
//                    .setRotation(BitmapUtil.getRotation(activity, mDisplayOrientation))
//                    .build();
//
//            Bitmap image1 = BitmapUtil.getBitmap(data, frameMetadata);
            Bitmap image1 = image.copy(Config.ARGB_8888, false);

            if (image1 != null && !image1.isRecycled()/* && checkValid(image)*/) {
//                FirebaseVisionImageMetadata metadata =
//                        new FirebaseVisionImageMetadata.Builder()
////                                .setFormat(camera.getParameters().getPreviewFormat())
//                                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
//                                .setWidth(frameMetadata.getWidth())
//                                .setHeight(frameMetadata.getHeight())
//                                .setRotation(frameMetadata.getRotation())
//                                .build();
//////            image = null;
////
//                FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromByteArray(data, metadata);
                FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(image1);
                faceDetector.detectInImage(firebaseVisionImage)
                        .addOnSuccessListener(faces -> {
                            if (faces.size() > 0) {
                                int index = 0;
                                int indexW = 0;
                                int indexH = 0;
                                int currentPosition = 0;
                                for (FirebaseVisionFace visionFace : faces) {
                                    Rect bounds = visionFace.getBoundingBox();
                                    if (bounds.width() > indexW || bounds.height() > indexH) {
                                        indexW = bounds.width();
                                        indexH = bounds.height();
                                        index = currentPosition;
                                    }
                                    currentPosition++;
                                }
                                FirebaseVisionFace face = faces.get(index);
//                                Util.logd(TAG, "detectFace: " + face);
                                Rect bounds = face.getBoundingBox();
                                float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees
                                float angle = BitmapUtil.getRotation(rotZ);
                                if (!(angle == 90 || angle == -90 || angle == 0)) {
//                                if ((rotZ < -45 && rotZ >= -135 && !(rotZ < -90 + 20 && rotZ >= -90 - 20))
//                                        || (rotZ > 45 && rotZ < 135 && !(rotZ > 90 - 20 && rotZ < 90 + 20))) {
//                                if ((rotZ < -45 && rotZ >= -135) || (rotZ > 45 && rotZ < 135)) {
                                    this.callBack.onScannedSuccess(false, false);
                                } else {
                                    RectF dest = new RectF((int) bounds.left, (int) bounds.top, (int) bounds.right, (int) bounds.bottom);

//                                Matrix m = new Matrix();
////                                m.postRotate(rotZ);
//                                m.setRotate(rotZ, dest.centerX(), dest.centerY());
////                                m.mapRect(dest);
//                                RectF dst = new RectF();
//                                m.mapRect(dst, dest);
//                                FirebaseVisionFaceLandmark leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE);
//                                FirebaseVisionFaceLandmark rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE);
//                                if (leftEye != null) {
//                                    FirebaseVisionPoint point = leftEye.getPosition();
//                                    Util.logd("face landmark : ", "left " + point.getX() + "," + point.getY());
//                                }
//                                if (rightEye != null) {
//                                    FirebaseVisionPoint point = rightEye.getPosition();
//                                    Util.logd("face landmark : ", "rigth " + point.getX() + "," + point.getY());
//                                }
                                    float x = dest.left;
                                    float y = dest.top;
                                    float width = (dest.right - dest.left);
                                    float height = (dest.bottom - dest.top);

                                    float wX = width * 0.1f;
                                    float wY = height * 0.1f;
                                    float newX = x - wX;
                                    float newY = y - wY;
                                    float newWidth = (width + (wX * 2));
                                    float newHeight = (height + (wY * 2));
//                                    Util.logd(TAG, "detectFace: old" + x + "," + y + "," + width + "," + height);
//                                    Util.logd(TAG, "detectFace: new" + newX + "," + newY + "," + newWidth + "," + newHeight);
                                    if (newX < 0) {
                                        newWidth = newWidth + (int) (newX * 2);
                                        newX = 0;
                                    }
                                    if (newY < 0) {
                                        newHeight = newHeight + (int) (newY * 2);
                                        newY = 0;
                                    }

                                    if (newX + newWidth > image1.getWidth())
                                        newWidth = image1.getWidth() - newX;

                                    if (newY + newHeight > image1.getHeight())
                                        newHeight = image1.getHeight() - newY;

                                    try {
//                                        Util.logd(TAG, "detectFace: final" + newX + "," + newY + "," + newWidth + "," + newHeight);
                                        //                                    Bitmap faceBitmap = Bitmap.createBitmap(image1,
                                        //                                            (int) 0,
                                        //                                            (int) 0,
                                        //                                            (int) image1.getWidth(),
                                        //                                            (int) image1.getHeight(), m, true);
                                        //                                    Bitmap faceBitmap1 = Bitmap.createBitmap(faceBitmap,
                                        //                                            (int) newX,
                                        //                                            (int) newY,
                                        //                                            (int) newWidth,
                                        //                                            (int) newHeight);
                                        //                                    Bitmap faceBitmap1 = BitmapUtil.rotateBitmap(faceBitmap, rotZ);
                                        Bitmap faceBitmap1 = BitmapUtil.rotatedCropBitmap(image1,
                                                new Rect((int) newX, (int) newY, (int) (newWidth + newX), (int) (newHeight + newY)),
                                                angle);
//                                        Bitmap faceBitmap1 = BitmapUtil.rotateRectForOrientation((int) rotZ,
//                                                new Rect(0,0,image1.getWidth(),image1.getHeight()),
//                                                new Rect((int) newX, (int) newY, (int) (newWidth + newX), (int) (newHeight + newY)),
//                                                image1);
                                        Mat clone = new Mat();
                                        Utils.bitmapToMat(faceBitmap1, clone);
                                        String s = doFaceCheck(clone.getNativeObjAddr(), v);
                                        clone.release();
                                        try {
                                            if (s != null && !s.equals("")) {
                                                JSONObject jsonObject = new JSONObject(s);
                                                int ic = jsonObject.getInt("responseCode");
                                                if (ic == 1) {
                                                    if (ocrData != null) {
                                                        if (ocrData.getFaceImage() != null)
                                                            ocrData.getFaceImage().recycle();
                                                        ocrData.setFaceImage(faceBitmap1.copy(Config.ARGB_8888, false));
                                                    } else if (result != null) {
                                                        result.faceBitmap = faceBitmap1.copy(Config.ARGB_8888, false);
//                                                        result.recType = RecType.FACE;
//                                                        result.docFrontBitmap = image.copy(Config.ARGB_8888, false);
                                                        if (result.recType == RecType.MRZ) {
                                                            result.bRecDone = true;
                                                        }
                                                    }
                                                    if (scanListener != null) {
                                                        scanListener.onScannedSuccess(true, true);
                                                    }
                                                    image1.recycle();
                                                    image.recycle();
                                                    faceBitmap1.recycle();
                                                    // TODO Success
                                                } else if (ic == 10) {
                                                    image1.recycle();
                                                    image.recycle();
                                                    faceBitmap1.recycle();
                                                    String message = jsonObject.getString("responseMessage");
                                                    if (!message.isEmpty() && this.callBack != null) {
                                                        this.callBack.onUpdateProcess(message);
                                                        scanListener.onScannedFailed("");
                                                    }
                                                }
//                                                faceBitmap1.recycle();
                                            }
                                        } catch (JSONException e) {
                                            image1.recycle();
                                            image.recycle();
                                            faceBitmap1.recycle();
                                            scanListener.onScannedFailed("");
                                        }
                                    } catch (Exception e) {
                                        image1.recycle();
                                        image.recycle();
                                        Util.logd(TAG, "face  Failed");
                                        scanListener.onScannedFailed("");
                                    }
                                }
                            } else {
                                image1.recycle();
//                                image.recycle();
                                if (scanListener != null)
                                    scanListener.onScannedFailed("1");
//                                scanListener.onScannedSuccess(false, false);
                            }
//                            try {
//                                if (faceDetector != null) {
//                                    faceDetector.close();
//                                }
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
                        }).addOnFailureListener(e -> {
                    scanListener.onScannedFailed("");
//                    try {
//                        if (faceDetector != null) {
//                            faceDetector.close();
//                        }
//                    } catch (IOException e1) {
//                        e1.printStackTrace();
//                    }
                });
            } else {
                image1.recycle();
                image.recycle();
                scanListener.onScannedFailed("");
            }
        } else {
            image.recycle();
            scanListener.onScannedFailed("");
        }
    }

    // return true if mrz available
    private boolean CheckMRZisRequired(OcrData.MapData mapData) {
        if (mapData != null && mapData.getOcr_data() != null) {
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

    private Bitmap bitmapFromMat(Mat mat) {
        if (mat != null) {
            Bitmap bmp = Bitmap.createBitmap(mat.width(), mat.height(), Config.ARGB_8888);
            Utils.matToBitmap(mat, bmp);
            return bmp;
        }
        return null;
    }

    private int getAssetFile(String fileName, String fileName1) {

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

    private File loadClassifierData(Context context) {

        File faceClassifierFile;
        InputStream is;
        FileOutputStream os;


        try {
            is = context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);

//            faceClassifierFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            faceClassifierFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml");
//            System.out.println("cascade path  = " + faceClassifierFile.getAbsolutePath());

            if (!faceClassifierFile.exists()) {
                os = new FileOutputStream(faceClassifierFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

                is.close();
                os.close();
            }

        } catch (IOException e) {
            Util.logd("cascade", "Face cascade not found");
            return null;
        }

        return faceClassifierFile;
    }

    public PointF getCenterPoint(List<FirebaseVisionFace> faces) {
        PointF centerOfAllFaces = new PointF();

        final int totalFaces = faces.size();
        if (totalFaces > 0) {
            float sumX = 0f;
            float sumY = 0f;
            for (int i = 0; i < totalFaces; i++) {
                PointF faceCenter = new PointF();
                getFaceCenter(faces.get(i), faceCenter);
                sumX = sumX + faceCenter.x;
                sumY = sumY + faceCenter.y;
            }
            centerOfAllFaces.set(sumX / totalFaces, sumY / totalFaces);
        }

        return centerOfAllFaces;
    }

    /**
     * Calculates center of a given face
     *
     * @param face   Face
     * @param center Center of the face
     */
    private void getFaceCenter(FirebaseVisionFace face, PointF center) {
        Rect bounds = face.getBoundingBox();
        float x = bounds.left;
        float y = bounds.top;
        float width = (bounds.right - bounds.left);
        float height = (bounds.bottom - bounds.top);
        center.set(x + (width / 2), y + (height / 2)); // face center in original bitmap
    }

    public void transform(Bitmap original, PointF focusPoint,/*, FaceCenterCropListener faceCenterCropListener*/OcrData ocrData) {

        Util.logd("Time log", "Image cropping begins");

        Util.logd(TAG, "transform: ");

//        this.faceCenterCropListener=faceCenterCropListener;

        int height = 400;
        int width = 400;
        if (width == 0 || height == 0) {
            throw new IllegalArgumentException("width or height should not be zero!");
        }
        float scaleX = (float) width / original.getWidth();
        float scaleY = (float) height / original.getHeight();

        if (scaleX != scaleY) {

            Bitmap.Config config =
                    original.getConfig() != null ? original.getConfig() : Bitmap.Config.ARGB_8888;
            Bitmap result = Bitmap.createBitmap(width, height, config);

            float scale = Math.max(scaleX, scaleY);

            float left = 0f;
            float top = 0f;

            float scaledWidth = width, scaledHeight = height;

            if (scaleX < scaleY) {

                scaledWidth = scale * original.getWidth();

                float faceCenterX = scale * focusPoint.x;
                left = getLeftPoint(width, scaledWidth, faceCenterX);

            } else {

                scaledHeight = scale * original.getHeight();

                float faceCenterY = scale * focusPoint.y;
                top = getTopPoint(height, scaledHeight, faceCenterY);
            }

            RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);
            Canvas canvas = new Canvas(result);
            canvas.drawBitmap(original, null, targetRect, null);

            Util.logd("Time log", "Face cropping done");

            ocrData.setFaceImage(result);

        } else {
        }
    }

    private float getTopPoint(int height, float scaledHeight, float faceCenterY) {
        if (faceCenterY <= height / 2) { // Face is near the top edge
            return 0f;
        } else if ((scaledHeight - faceCenterY) <= height / 2) { // face is near bottom edge
            return height - scaledHeight;
        } else {
            return (height / 2) - faceCenterY;
        }
    }

    private float getLeftPoint(int width, float scaledWidth, float faceCenterX) {
        if (faceCenterX <= width / 2) { // face is near the left edge.
            return 0f;
        } else if ((scaledWidth - faceCenterX) <= width / 2) {  // face is near right edge
            return (width - scaledWidth);
        } else {
            return (width / 2) - faceCenterX;
        }
    }

    void closeEngine(int destroy) {
        try {
            if (detector != null) {
                detector.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (faceDetector != null) {
                faceDetector.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        closeOCR(destroy);
    }

    public enum RecType {
        INIT, BOTH, FACE, MRZ
    }
}
