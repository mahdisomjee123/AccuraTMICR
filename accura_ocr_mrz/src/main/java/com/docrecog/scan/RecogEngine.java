package com.docrecog.scan;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Rect;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.accurascan.ocr.mrz.R;
import com.accurascan.ocr.mrz.model.ContryModel;
import com.accurascan.ocr.mrz.model.InitModel;
import com.accurascan.ocr.mrz.model.OcrData;
import com.accurascan.ocr.mrz.model.RecogResult;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
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

    Boolean isMrzEnable = true;

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

    public interface ScanListener {
        /**
         * This is called to get scanned processed message.
         */
        void onUpdateProcess(String s);


        /**
         * This is called after scanned success.
         *
         * @param isDone
         * @param isMRZRequired
         */
        void onScannedSuccess(boolean isDone, boolean isMRZRequired);

        /**
         * This is called on scanned failed.
         */
        void onScannedFailed(String s);

    }

    private static final String TAG = "PassportRecog";
    private byte[] pDic = null;
    private int pDicLen = 0;
    private byte[] pDic1 = null;
    private int pDicLen1 = 0;
    private static String[] assetNames = {"mMQDF_f_Passport_bottom_Gray.dic", "mMQDF_f_Passport_bottom.dic"};
    public static FirebaseVisionTextRecognizer detector;
    private ScanListener callBack;
    private String newMessage = "";

    private static float[] fConf = new float[3]; //face detection confidence
    private static int[] faced = new int[3]; //value for detected face or not

    private static int[] intData = new int[3000];

    private static int NOR_W = 400;//1200;//1006;
    private static int NOR_H = 400;//750;//1451;

    private Context con;
    private Activity activity;

    public RecogEngine() {

    }

    //This is SDK app calling JNI method
    private native int loadDictionary(Context activity, String s, byte[] img_Dic, int len_Dic, byte[] img_Dic1, int len_Dic1,/*, byte[] licenseKey*/AssetManager assets);

    //return value: 0:fail,1:success,correct document, 2:success,incorrect document
    private native int doRecogYuv420p(byte[] yuvdata, int width, int height, int facepick, int rot, int[] intData, Bitmap faceBitmap, boolean unknownVal);

    private native int doRecogBitmap(Bitmap bitmap, int facepick, int[] intData, Bitmap faceBitmap, int[] faced, boolean unknownVal);

    private native int doFaceDetect(Bitmap bitmap, Bitmap faceBitmap, float[] fConf);

    private native String loadData(Context context, int[] i);


    /**
     * Set Blur Percentage to allow blur on document
     *
     * @param context        Activity context
     * @param blurPercentage is 0 to 100, 0 - clean document and 100 - Blurry document
     * @return 1 if success else 0
     */
    public native int setBlurPercentage(Context context, int blurPercentage);

    /**
     * Set Blur Percentage to allow blur on detected Face
     *
     * @param context            Activity context
     * @param faceBlurPercentage is 0 to 100, 0 - clean face and 100 - Blurry face
     * @return 1 if success else 0
     */
    public native int setFaceBlurPercentage(Context context, int faceBlurPercentage);

    /**
     * @param context
     * @param minPercentage
     * @param maxPercentage
     * @return 1 if success else 0
     */
    public native int setGlarePercentage(Context context, int minPercentage, int maxPercentage);

    /**
     * Set CheckPhotoCopy to allow photocopy document or not
     *
     * @param context
     * @param isCheckPhotoCopy if true then reject photo copy document else vice versa
     * @return 1 if success else 0
     */
    public native int isCheckPhotoCopy(Context context, boolean isCheckPhotoCopy);

    /**
     * set Hologram detection to allow hologram on face or not
     *
     * @param context
     * @param isDetectHologram if true then reject hologram is on face else it is allow .
     * @return 1 if success else 0
     */
    public native int SetHologramDetection(Context context, boolean isDetectHologram);

    private native String loadOCR(Context context, AssetManager assetManager, int countryid, int cardid, int widthPixels);

    private native ImageOpencv checkDocument(long matInput, long matOut);

    private native String recognizeData(long src, int[][] boxBoundsLTRB, String[] textElements);

    private native String loadScanner(Context context, AssetManager assetManager, int countryid);

    private native int doBlurCheck(long srcMat);

    /**
     * Initialized ocr
     *
     * @param scanListener to get ocr update
     * @param context      is activity context
     * @param countryId    is country code
     * @param cardId       is Card Code
     * @return {@link InitModel}
     */
    // for failed -> responseCode = 0,
    // for success -> responseCode = 1
    protected InitModel initOcr(ScanListener scanListener, Context context, int countryId, int cardId) {
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

    boolean checkValid(Bitmap bitmap) {
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);
        int i = doBlurCheck(src.getNativeObjAddr());
        return i == 0;
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
                        newMessage = message;
                        this.callBack.onUpdateProcess(message);
                    }
                    final Runnable runnable = () -> {
                        try {
                            if (newMessage.equals(message) || !message.contains("Process")) {
                                callBack.onUpdateProcess("");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    };
                    Runnable runnable1 = () -> new Handler().postDelayed(runnable, 1000);
                    if (activity != null) {
                        activity.runOnUiThread(runnable1);
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
        frames = checkDocument(clone.getNativeObjAddr(), outMat.getNativeObjAddr());
        if (frames != null) {
            if (!frames.message.isEmpty() && this.callBack != null) {
                newMessage = frames.message;
                this.callBack.onUpdateProcess(frames.message);
                final Runnable runnable = () -> {
                    try {
                        if (newMessage.equals(frames.message) || !frames.message.contains("Process")) {
                            callBack.onUpdateProcess("");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                };
                Runnable runnable1 = () -> new Handler().postDelayed(runnable, 1000);
                if (activity != null) {
                    activity.runOnUiThread(runnable1);
                }
            }
            if (frames.isSucess) {
                frames.mat = new Mat();
                outMat.copyTo(frames.mat);
                this.callBack.onUpdateProcess("Processing...");
            } else {
                frames.mat = null;
            }
            return frames;
        } else {
            return null;
        }
    }

    private Bitmap bitmapFromMat(Mat mat) {
        if (mat != null) {
            Bitmap bmp = Bitmap.createBitmap(mat.width(), mat.height(), Config.ARGB_8888);
            Utils.matToBitmap(mat, bmp);
            return bmp;
        }
        return null;
    }

    public class SDKModel {
        public int i;
        public boolean isMRZEnable = false;
        public boolean isOCREnable = false;
        public boolean isPDF417Enable = false;
    }

    /**
     * Must have to call initEngine on app open
     *
     * @param context
     * @return
     */
    public SDKModel initEngine(Context context) {

        //call Sdk  method InitEngine
        // this method will return the integer value
        //  the return value by initEngine used the identify the particular error
        // -1 - No key found
        // -2 - Invalid Key
        // -3 - Invalid Platform
        // -4 - Invalid License

        // the return value by initEngine used the identify the license is valid for ocr, mrz or both
        // 1 - for MRZ only
        // 2 - for Ocr only
        // 3 - for ocr + mrz both

        /*
           initialized sdk by InitEngine from thread
          The return value by initEngine used the identify
          Return i < 0 if license not valid
          -1 - No key found
          -2 - Invalid Key
          -3 - Invalid Platform
          -4 - Invalid License

          Return i > 0 if license is valid
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
        File file = loadClassifierData(context);
        int ret = loadDictionary(context, file != null ? file.getAbsolutePath() : "", pDic, pDicLen, pDic1, pDicLen1, context.getAssets());
        Log.i("recogPassport", "loadDictionary: " + ret);
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
                AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                builder1.setMessage(message);

                builder1.setCancelable(true);

                builder1.setPositiveButton(
                        "OK",
                        (dialog, id) -> dialog.cancel());

                AlertDialog alert11 = builder1.create();
                alert11.show();
            }
        }
        SDKModel sdkModel = new SDKModel();
        sdkModel.isMRZEnable = ret == 1 || ret == 4 || ret == 6 || ret == 7;
        sdkModel.isOCREnable = ret == 2 || ret == 4 || ret == 5 || ret == 7;
        sdkModel.isPDF417Enable = ret == 3 || ret == 5 || ret == 6 || ret == 7;
        sdkModel.i = ret;
        return sdkModel;
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
            System.out.println("cascade path  = " + faceClassifierFile.getAbsolutePath());

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
            Log.i("cascade", "Face cascade not found");
            return null;
        }

        return faceClassifierFile;
    }

    /**
     * To get MRZ data from documnet
     *
     * @param bmCard document bitmap
     * @param result {@link RecogResult} to get data
     * @return 0 if failed and >0 if success
     */
    int doRunData(Bitmap bmCard, int facepick, RecogResult result) {

        //If fail, empty string.
        // both => 0
        // only face => 1
        // only mrz => 2
        Bitmap faceBmp = null;
        if (facepick == 1) {
            faceBmp = Bitmap.createBitmap(NOR_W, NOR_H, Config.ARGB_8888);
        }
        int ret = doRecogBitmap(bmCard, facepick, intData, faceBmp, faced, true);

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
        return ret;
    }

    int doRunFaceDetect(Bitmap bmImg, RecogResult result) {
        if (result.faceBitmap != null) {
            return 1;

        }
        result.faceBitmap = Bitmap.createBitmap(NOR_W, NOR_H, Config.ARGB_8888);
        int ret = doFaceDetect(bmImg, result.faceBitmap, fConf);

        //ret > 0 => detect face ok
        if (ret <= 0) result.faceBitmap = null;
        else if (fConf[1] < 400 || fConf[2] < 400)
            result.faceBitmap = Bitmap.createBitmap(result.faceBitmap, 0, 0, (int) fConf[1], (int) fConf[2]);

        if (ret > 0 && result.recType == RecType.MRZ)
            result.bRecDone = true;

        return ret;
    }

    /**
     * Call this method if document is valid after {@see checkCard(Bitmap bmp)}
     *
     * @param scanListener to get scanned data
     * @param mat          pass met to retrieve ocr data.
     * @param ocrData      to fill data to this object
     */
    void doRecognition(ScanListener scanListener, Bitmap src, Mat mat, OcrData ocrData) {
        RecogEngine.this.callBack = scanListener;

        Bitmap image = bitmapFromMat(mat);
        System.out.println("Loaddata++");

        if (detector == null) {
            detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        }

        detector.processImage(FirebaseVisionImage.fromBitmap(image))
                .addOnSuccessListener(visionText -> {

                    OcrData.MapData mapData = MapDataFunction(mat.getNativeObjAddr(), visionText);
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
                            ocrData.setFrontimage(src);
                        } else {
                            if (ocrData.getBackData() != null)
                                isdone = false;
                            ocrData.setBackData(mapData);
                            ocrData.setBackimage(src);
                        }
                        System.out.println("MappingDone");
                    }
                    if (callBack != null) {
                        callBack.onScannedSuccess(isdone, isMrzEnable && CheckMRZisRequired(mapData));
                    }
                })
                .addOnFailureListener(e -> {
                    if (callBack != null) {
                        callBack.onScannedFailed(e.getMessage());
                    }
                });
    }

    //    return true if mrz available
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

    void closeEngine() {
        try {
            if (detector != null) {
                detector.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public enum RecType {
        INIT, BOTH, FACE, MRZ
    }
}
