package com.docrecog.scan;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.accurascan.ocr.mrz.R;

import com.accurascan.ocr.mrz.interfaces.OcrCallback;
import com.accurascan.ocr.mrz.model.CardDetails;
import com.accurascan.ocr.mrz.model.ContryModel;
import com.accurascan.ocr.mrz.model.InitModel;
import com.accurascan.ocr.mrz.model.OcrData;
import com.accurascan.ocr.mrz.model.RecogResult;
import com.accurascan.ocr.mrz.util.AccuraLog;
import com.accurascan.ocr.mrz.util.BitmapUtil;
import com.accurascan.ocr.mrz.util.Util;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.scottyab.rootbeer.RootBeer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

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
            AccuraLog.loge(RecogEngine.class.getSimpleName(), "Load success");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private native static void enableSDKLog(boolean isLogEnable);
    public static void _enableSDKLog(boolean isLogEnable) {
        enableSDKLog(isLogEnable);
    }

    @androidx.annotation.Keep
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

        void onFaceScanned(Bitmap bitmap){

        }

        /**
         * This is called on scanned failed.
         */
        void onScannedFailed(String s) {
        }

    }

    @androidx.annotation.Keep
    public class SDKModel {
        public int i;
        public boolean isMRZEnable = false;
        public boolean isOCREnable = false;
        public boolean isAllBarcodeEnable = false;
        public boolean isBankCardEnable = false;
        public String message = "Success";
    }

    public static final String VERSION = "5.6.0";

    public static final int SCAN_TITLE_OCR_FRONT = 1;
    public static final int SCAN_TITLE_OCR_BACK = 2;
    public static final int SCAN_TITLE_OCR = 3;
    public static final int SCAN_TITLE_MRZ_PDF417_FRONT = 4;
    public static final int SCAN_TITLE_MRZ_PDF417_BACK = 5;
    public static final int SCAN_TITLE_DLPLATE = 6;
    public static final int SCAN_TITLE_DEFAULT = -1;

    public static final String ACCURA_ERROR_CODE_MOTION = "0";
    public static final String ACCURA_ERROR_CODE_DOCUMENT_IN_FRAME = "1";
    public static final String ACCURA_ERROR_CODE_BRING_DOCUMENT_IN_FRAME = "2";
    public static final String ACCURA_ERROR_CODE_PROCESSING = "3";
    public static final String ACCURA_ERROR_CODE_BLUR_DOCUMENT = "4";
    public static final String ACCURA_ERROR_CODE_FACE_BLUR = "5";
    public static final String ACCURA_ERROR_CODE_GLARE_DOCUMENT = "6";
    public static final String ACCURA_ERROR_CODE_HOLOGRAM = "7";
    public static final String ACCURA_ERROR_CODE_DARK_DOCUMENT = "8";
    public static final String ACCURA_ERROR_CODE_PHOTO_COPY_DOCUMENT = "9";
    public static final String ACCURA_ERROR_CODE_FACE = "10";
    public static final String ACCURA_ERROR_CODE_MRZ = "11";
    public static final String ACCURA_ERROR_CODE_PASSPORT_MRZ = "12";
    public static final String ACCURA_ERROR_CODE_ID_MRZ = "13";
    public static final String ACCURA_ERROR_CODE_VISA_MRZ = "14";
    public static final String ACCURA_ERROR_CODE_UPSIDE_DOWN_SIDE = "15";
    public static final String ACCURA_ERROR_CODE_WRONG_SIDE = "16";

    private static final String TAG = "PassportRecog";
    private byte[] pDic = null;
    private int pDicLen = 0;
    private byte[] pDic1 = null;
    private int pDicLen1 = 0;
    private static String[] assetNames = {"mMQDF_f_Passport_bottom_Gray.dic", "mMQDF_f_Passport_bottom.dic"};
    private static TextRecognizer detector;
    private boolean findFace = false;
    private boolean isComplete = false;
    private ScanListener callBack;
//    // Added By Ankita20220616
//    public static final int TUN_CARD = 367;
//    public static final int TUN_COUN = 190;

    public int countryId;
    public int cardId;

//    static String nM;
    static float mT = 15;
    Boolean isMrzEnable = true;
    String countryCode;
    private String languageCode = "en";
    static float v = 5f;

    private static float[] fConf = new float[3]; //face detection confidence
    private static int[] faced = new int[3]; //value for detected face or not

    private static int[] intData = new int[3000];

    private static int NOR_W = 400;//1200;//1006;
    private static int NOR_H = 400;//750;//1451;

    private Context con;
    private Activity activity;
    private boolean displayDialog = true;

    public RecogEngine() {

    }

    public RecogEngine(Activity activity) {
        this.activity = activity;
    }

    void setCallBack(ScanListener scanListener, RecogType recogType) {
        this.callBack = scanListener;
        isComplete = false;
        if (recogType == RecogType.OCR) {
           // updateData("Back");
        }
    }

    void removeCallBack(ScanListener scanListener) {
        this.callBack = scanListener;
    }

    //This is SDK app calling JNI method
    private native int loadDictionary(Context activity, String s, byte[] img_Dic, int len_Dic, byte[] img_Dic1, int len_Dic1,/*, byte[] licenseKey*/AssetManager assets, int[] intData, boolean logEnable);
//    public native int loadDictionary(Context activity, byte[] img_Dic, int len_Dic, byte[] img_Dic1, int len_Dic1,/*, byte[] licenseKey*/AssetManager assets);

    //return value: 0:fail,1:success,correct document, 2:success,incorrect document
    private native int doRecogYuv420p(byte[] yuvdata, int width, int height, int facepick, int rot, int[] intData, Bitmap faceBitmap);

    public native String doCheckData(byte[] yuvdata, int width, int height);

    private native int doRecogBitmap(Bitmap bitmap, int facepick, int[] intData, Bitmap faceBitmap, int[] faced, boolean unknownVal, int documentType, String countries);

    private native int doFaceDetect(Bitmap bitmap, Bitmap faceBitmap, float[] fConf);

    private native String OpenCvFaceDetect(long l);

    private native String doFaceCheck(long l, float v);

    private native String doCheckDocument(long l, float v);

    private native String loadData(Context context, int[] i);

    /**
     * Set Blur Percentage to allow blur on document
     *
     * @param context        Activity context
     * @param blurPercentage is 0 to 100, 0 - clean document and 100 - Blurry document
     * @return 1 if success else 0
     */
    private native int setBlurPercentage(Context context, int blurPercentage, String errorMessage);

    /**
     * Set Blur Percentage to allow blur on detected Face
     *
     * @param context            Activity context
     * @param faceBlurPercentage is 0 to 100, 0 - clean face and 100 - Blurry face
     * @return 1 if success else 0
     */
    private native int setFaceBlurPercentage(Context context, int faceBlurPercentage, String errorMessage);

    /**
     * @param context
     * @param minPercentage
     * @param maxPercentage
     * @return 1 if success else 0
     */
    private native int setGlarePercentage(Context context, int minPercentage, int maxPercentage, String errorMessage);

    /**
     * Set CheckPhotoCopy to allow photocopy document or not
     *
     * @param context
     * @param isCheckPhotoCopy if true then reject photo copy document else vice versa
     * @return 1 if success else 0
     */
    private native int isCheckPhotoCopy(Context context, boolean isCheckPhotoCopy, String errorMessage);

    /**
     * set Hologram detection to allow hologram on face or not
     *
     * @param context
     * @param isDetectHologram if true then reject hologram is on face else it is allow .
     * @return 1 if success else 0
     */
    private native int SetHologramDetection(Context context, boolean isDetectHologram, String errorMessage);

    /**
     * set light tolerance to detect light on document if low light
     *
     * @param context
     * @param tolerance is 0 to 100, 0 - allow full dark document and 100 - allow full bright document
     * @return 1 if success else 0
     */
    private native int setLowLightTolerance(Context context, int tolerance, String errorMessage);

    /**
     * set motion threshold to detect motion on camera document
     *
     * @param context
     * @param motionThreshold
     * @return
     */
    private native int setMotionThreshold(Context context, int motionThreshold, @NonNull String message);

    private native String loadOCR(Context context, AssetManager assetManager, int countryid, int cardid, int widthPixels, int minFrame, int i);

    private native ImageOpencv checkDocument(long matInput, long matOut, float v);

    private native String recognizeData(long src, int[][] boxBoundsLTRB, String[] textElements, String[] lineElements, String s);

    private native String recognizeCard(String s, int r, int cB);

    public native int updateData(String s);

    private native String loadCard(Context context, int type);

    private native String loadScanner(Context context, AssetManager assetManager, int countryid);

    private native String loadNumberPlat(Context context, int countryid, int cardid);

    private native int doBlurCheck(long srcMat);

    private native String doLightCheck(long srcMat);

    private native int closeOCR(int i);

    private native int doDetectNumberPlate(String s, int[] intData, int id, int card_id);

    private native int extractData(String s, CardDetails cardDetails);

    public native String getSDKVersion();

    public String getVersion() {
        return "OCR Version : " + VERSION + "\n" +
                "SDK version : " + getSDKVersion();
    }

    public boolean checkRD(Context context){
        RootBeer rootBeer = new RootBeer(context);
        rootBeer.setLogging(false);
        if (rootBeer.isRooted()/*rootBeer.detectRootManagementApps() ||
                rootBeer.detectPotentiallyDangerousApps() ||
                rootBeer.detectRootCloakingApps() ||
                rootBeer.detectTestKeys() ||
                rootBeer.checkForBusyBoxBinary() ||
                rootBeer.checkForSuBinary() ||
                rootBeer.checkSuExists() ||
                rootBeer.checkForRWPaths() ||
                rootBeer.checkForDangerousProps() ||
                rootBeer.checkForRootNative()*/ /*||
                com.scottyab.rootbeer.util.Utils.isSelinuxFlagInEnabled() ||
                rootBeer.checkForMagiskBinary()*/) {

            //we found indication of root
            return true;
        } else {
            return false;
        }
    }

    public int setBlurPercentage(Context context, int blurPercentage) {
        return setBlurPercentage(context, blurPercentage,"");
    }

    public int setFaceBlurPercentage(Context context, int faceBlurPercentage) {
        return setFaceBlurPercentage(context, faceBlurPercentage,"");
    }

    public int setGlarePercentage(Context context, int minValue, int maxValue) {
        return setGlarePercentage(context, minValue, maxValue,"");
    }

    public int isCheckPhotoCopy(Context context, boolean isCheckPhotoCopy) {
        return isCheckPhotoCopy(context, isCheckPhotoCopy,"");
    }

    public int SetHologramDetection(Context context, boolean isDetectHologram) {
        return SetHologramDetection(context, isDetectHologram,"");
    }

    public int setLowLightTolerance(Context context, int tolerance) {
        return setLowLightTolerance(context, tolerance,"");
    }

    public int setMotionThreshold(Activity activity, int motionThreshold) {
        mT = motionThreshold;
//        nM = message;
        return setMotionThreshold(activity, motionThreshold, "");
    }


    public void setDialog(boolean displayDialog) {
        this.displayDialog = displayDialog;
    }

    /**
     * Must have to call initEngine on app open
     *
     * @param context
     * @return
     */
    public SDKModel initEngine(Context context) {

        if (checkRD(context)) {
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                    builder1.setMessage("Sorry, you can't use this app as we've detected that your device has been rooted");
                    builder1.setCancelable(true);
                    builder1.setPositiveButton(
                            "OK",
                            (dialog, id) -> {
                                dialog.cancel();
                                ((Activity) context).finishAndRemoveTask();
                            });
                    AlertDialog alert11 = builder1.create();
                    alert11.show();
                });
            } else
                Toast.makeText(context, "Sorry, you can't use this app as we've detected that your device has been rooted", Toast.LENGTH_SHORT).show();
            return null;
        }
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
        SDKModel sdkModel = new SDKModel();
        getAssetFile(assetNames[0], assetNames[1]);
        int[] ints = new int[5];
        File file = loadClassifierData(context);
        int ret = loadDictionary(context, file != null ? file.getAbsolutePath() : "", pDic, pDicLen, pDic1, pDicLen1, context.getAssets(),ints,AccuraLog.isLogEnable());
        AccuraLog.loge("recogPassport", "loadDictionary: " + ret);
//        nM = "Keep Document Steady";
        if (ret < 0) {
            String message = "";
            if (ret == -1) {
                message = "'key.license' Not Found";
            } else if (ret == -2) {
                message = "'key.license' Invalid Key";
            } else if (ret == -3) {
                message = "'key.license' Invalid Platform";
            } else if (ret == -4) {
                message = "'key.license' Invalid License";
            }
            sdkModel.message = message;
            if (displayDialog) {
                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(() -> {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                        builder1.setMessage(sdkModel.message);
                        builder1.setCancelable(true);
                        builder1.setPositiveButton(
                                "OK",
                                (dialog, id) -> dialog.cancel());
                        AlertDialog alert11 = builder1.create();
                        alert11.show();
                    });
                } else
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        } else {
            sdkModel.isMRZEnable = ints[0] == 1;//isMrzEnable;//ret == 1 || ret == 4 || ret == 6 || ret == 7;
            sdkModel.isOCREnable = ints[1] == 1 || ints[4] == 1;//isOcrEnable;//ret == 2 || ret == 4 || ret == 5 || ret == 7;
            sdkModel.isAllBarcodeEnable = ints[2] == 1;//isPDFEnable;//ret == 3 || ret == 5 || ret == 6 || ret == 7;
            sdkModel.isBankCardEnable = ints[3] == 1;//isBankCardEnable;//ret == 3 || ret == 5 || ret == 6 || ret == 7;
        }
        sdkModel.i = ret;
        return sdkModel;
    }

    private static File loadClassifierData(Context context) {

        File faceClassifierFile = null;
        InputStream is;
        FileOutputStream os;


        try {
            is = context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = context.getDir("cascade", context.MODE_PRIVATE);

            faceClassifierFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml");

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
            Util.logd(TAG, Log.getStackTraceString(e));
            return null;
        }

        return faceClassifierFile;
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
            AccuraLog.loge(TAG, Log.getStackTraceString(e));
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
     * @param minFrame     To compare data between 'minFrame' and send most validation front dates of qatar ID cards.
     * @param i
     * @return {@link InitModel}
     */
    // for failed -> responseCode = 0,
    // for success -> responseCode = 1
    protected InitModel initOcr(ScanListener scanListener, Context context, int countryId, int cardId, int minFrame, int i) {
        findFace = false;
        if (scanListener != null) {
            this.callBack = scanListener;
        } else {
            throw new RuntimeException(" must implement " + ScanListener.class.getName());
        }
        if (context instanceof Activity) {
            this.activity = (Activity) context;
        }
        isComplete = false;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        String s = loadOCR(context, context.getAssets(), countryId, cardId, dm.widthPixels, minFrame, i);
        try {
            if (s != null && !s.equals("")) {
                JSONObject jsonObject = new JSONObject(s);
                InitModel initModel = new Gson().fromJson(jsonObject.toString(), InitModel.class);
                if (initModel.getInitData() != null) {
                    if (jsonObject.getJSONObject("data").has("country_code")) {
                        countryCode = jsonObject.getJSONObject("data").getString("country_code");
                    }
                    if (jsonObject.getJSONObject("data").has("languageCode")) {
                        languageCode = jsonObject.getJSONObject("data").getString("languageCode");
                    }
                    init();
                }
                isMrzEnable = initModel.getInitData() != null && (initModel.getInitData().getMRZEnable() == 1 || initModel.getInitData().getMRZEnable() == 2);
                return initModel;
            }
        } catch (JSONException e) {
        }
        return null;
    }

    private void init() {
        if (languageCode.equals("ch")) {
            detector = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
        } else if (languageCode.equals("ja")) {
            detector = TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build());
        } else if (languageCode.equals("ko")) {
            detector = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
        } else if (detector == null) {
            detector = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        }
    }

    /**
     * Initialized MRZ or Bankcard
     *
     * @param context      is activity context
     * @param recogType    0 for MRZ and 1 for Bankcard
     * @return {@link InitModel}
     */
    protected InitModel initCard(Context context, int recogType){
        String s = loadCard(context, recogType);
        try {
            if (s != null && !s.equals("")) {
                JSONObject jsonObject = new JSONObject(s);
                InitModel initModel = new Gson().fromJson(jsonObject.toString(), InitModel.class);
                return initModel;
            }
        } catch (JSONException e) {
        }
        return null;
    }

    /**
     * Initialized scanner
     *
     * @param context   is activity context
     * @param countryId is country code
     * @return {@link InitModel}
     */
    // for failed -> responseCode = 0,
    // for success -> responseCode = 1
    InitModel initScanner(Context context, int countryId) {

        String s = loadScanner(context, context.getAssets(), countryId);
        AccuraLog.loge(TAG, "lSC : "+s );
        try {
            if (s != null && !s.equals("")) {
                JSONObject jsonObject = new JSONObject(s);
                InitModel initModel = new Gson().fromJson(jsonObject.toString(), InitModel.class);
                return initModel;
            }
        } catch (JSONException e) {
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
    InitModel initNumberPlat(Context context, int countryId, int cardId) {

        String s = loadNumberPlat(context, countryId, cardId);
        AccuraLog.loge(TAG, "lNP : "+s );
        try {
            if (s != null && !s.equals("")) {
                JSONObject jsonObject = new JSONObject(s);
                InitModel initModel = new Gson().fromJson(jsonObject.toString(), InitModel.class);
                return initModel;
            }
        } catch (JSONException e) {
        }
        return null;
    }

    boolean checkValid(Bitmap bitmap) {
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);
//        int i = doBlurCheck(src.getNativeObjAddr());
//        return i == 0;
        String s = doCheckDocument(src.getNativeObjAddr(), v);
        if (s != null && !s.equals("")) {
            src.release();
            try {
                JSONObject jsonObject = new JSONObject(s);
                int ic = jsonObject.getInt("responseCode");
                if (ic == 1) {
                    return true;
                } else {
                    String message = jsonObject.getString("responseMessage");
                    if (!message.isEmpty() && this.callBack != null) {
                        this.callBack.onUpdateProcess(message);
                    }
                    return false;
                }
            } catch (JSONException e) {
            }
        }
        src.release();
        return false;
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
//                    if (ret >= 0) {
//                        nM = RecogEngine.ACCURA_ERROR_CODE_MOTION;/*jsonObject.getString("responseMessage")*/
//                    }
                }
            } catch (Exception e) {
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
    private OcrData.MapData MapDataFunction(long src, Text text) {
        int[][] boxBoundsLTRB;
        String[] textElements;
        List<String> lineElements = new ArrayList<>();
        JSONArray jsonArray = new JSONArray();

        List<Text.Element> elementArrayList = new ArrayList<>();
        List<Text.TextBlock> textBlocks = text.getTextBlocks();
        if (TextUtils.isEmpty(text.getText())) return null;
        for (Text.TextBlock textBlock : textBlocks) {
            for (Text.Line line : textBlock.getLines()) {
                JSONObject object = new JSONObject();
                try {
                    object.put("text", line.getText());
                    object.put("block_l", line.getBoundingBox().left);
                    object.put("block_t", line.getBoundingBox().top);
                    object.put("block_r", line.getBoundingBox().right);
                    object.put("block_b", line.getBoundingBox().bottom);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JSONArray jsonElementArray = new JSONArray();
                for (Text.Element element : line.getElements()) {
                    if (element == null)
                        continue;
                    elementArrayList.add(element);
                    try {
                        JSONObject jsonElementObject = new JSONObject();
                        jsonElementObject.put("text", element.getText());
                        jsonElementObject.put("block_l", element.getBoundingBox().left);
                        jsonElementObject.put("block_t", element.getBoundingBox().top);
                        jsonElementObject.put("block_r", element.getBoundingBox().right);
                        jsonElementObject.put("block_b", element.getBoundingBox().bottom);
                        jsonElementArray.put(jsonElementObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    object.put("element", jsonElementArray);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonArray.put(object);
                lineElements.add(line.getText());
            }
        }
        boxBoundsLTRB = new int[elementArrayList.size()][];
        textElements = new String[elementArrayList.size()];
        int counter = 0;
        for (Text.Element element : elementArrayList) {
            Rect rect = element.getBoundingBox();
            if (rect != null) {
                boxBoundsLTRB[counter] = new int[]{rect.left, rect.top, rect.right, rect.bottom};
                textElements[counter] = element.getText();
                counter++;
            }
        }

        String mapResult = recognizeData(src, boxBoundsLTRB, textElements,
                lineElements.toArray(new String[lineElements.size()]), jsonArray.toString());
        try {
            if (mapResult != null && !mapResult.equals("")) {
                JSONObject jsonObject = new JSONObject(mapResult);
                int ic = jsonObject.getInt("responseCode");
                AccuraLog.loge(TAG, "Detect : "+mapResult );
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
//            AccuraLog.loge(TAG, Log.getStackTraceString(e));
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
            AccuraLog.loge(TAG, "CCIF is-"+ frames.message);
            if (!frames.message.isEmpty() && this.callBack != null) {
                this.callBack.onUpdateProcess(frames.message);
            }
            if (frames.isSucess) {
                frames.mat = new Mat();
                outMat.copyTo(frames.mat);
                this.callBack.onUpdateProcess(RecogEngine.ACCURA_ERROR_CODE_PROCESSING/*"Processing..."*/);
            } else {
                bmp.recycle();
                frames.mat = null;
            }
            return frames;
        } else {
            AccuraLog.loge(TAG, "CCIF Data is null");
            return null;
        }
    }

    /**
     * To get MRZ data from documnet
     *
     * @param bmCard document bitmap
     * @param result {@link RecogResult} to get data
     * @param documentType
     * @param countries
     * @return 0 if failed and >0 if success
     */
    int doRunData(Bitmap bmCard, int facepick, RecogResult result, MRZDocumentType documentType, String countries) {
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
        if (documentType == null) {
           documentType = MRZDocumentType.NONE;
        }
        ret = doRecogBitmap(bmCard, 0, intData, faceBmp, faced, true, documentType.value, countries);
        AccuraLog.loge(TAG, "GetM - " + documentType + "," + ret);
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

    public void detectFromCapturedImage(Context context, Bitmap bmCard, MRZDocumentType documentType, String countries, int detectFace, OcrCallback ocrCallback) {
        RecogResult result = new RecogResult();
        result.recType = RecogEngine.RecType.INIT;
        result.bRecDone = false;
        this.callBack = new ScanListener() {
            @Override
            void onScannedSuccess(boolean isDone, boolean isMRZRequired) {
                closeEngine(1);
                if (isDone || !result.lines.isEmpty() || result.faceBitmap != null) {
                    ocrCallback.onScannedComplete(result);
                } else {
                    ocrCallback.onError("Failed Recognition");
                }
            }

            @Override
            void onUpdateProcess(String s) {

            }

            @Override
            void onScannedFailed(String s) {
                closeEngine(1);
                ocrCallback.onError(s);
            }
        };

        InitModel initModel = initCard(context, 0);
        if (initModel == null) {
            this.callBack.onScannedFailed("Failed Initialization");
            return;
        } else if (initModel.getResponseCode() != 1) {
            this.callBack.onScannedFailed(initModel.getResponseMessage());
            return;
        }
        int ret;
        if (documentType == null) {
            documentType = MRZDocumentType.NONE;
        }
        if (countries == null || countries.isEmpty()) {
            countries = "all";
        }
        ret = doRecogBitmap(bmCard, 0, intData, null, faced, true, documentType.value, countries);

        if (detectFace > 0) {
            Mat matimage = new Mat();
            Utils.bitmapToMat(bmCard, matimage);
            int scaledWidth = 1000;
            float ratio = scaledWidth/(float)bmCard.getWidth();
            int scaledHeight = (int)(ratio * bmCard.getHeight());
            Imgproc.resize(matimage, matimage, new Size(scaledWidth, scaledHeight));

            String xyz = OpenCvFaceDetect(matimage.getNativeObjAddr());
            matimage.release();
            if (xyz.length() > 50) {
                byte[] decodedString = Base64.decode(xyz, Base64.DEFAULT);
                result.faceBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                result.recType = RecType.FACE;
            }
        }
        AccuraLog.loge(TAG, "GetM - " + documentType + "," + ret);

        if (ret > 0) {
            if (result.recType == RecType.INIT) {
                if (result.faceBitmap == null) {
                    result.recType = RecType.MRZ;
                } else {
                    result.recType = RecType.BOTH;
                    result.bRecDone = true;
                }
            } else if (result.recType == RecType.FACE) {
                result.recType = RecType.BOTH;
                result.bRecDone = true;
            }
            result.ret = ret;
            result.SetResult(intData);
        }
        this.callBack.onScannedSuccess(true, true);
    }

    public void detectOCRFromCapturedImage(Context context, Bitmap bmCard, MRZDocumentType documentType, int countryId, int cardId, int cardSide, OcrCallback ocrCallback) {
        OcrData ocrData = new OcrData();
        this.callBack = new ScanListener() {
            @Override
            void onScannedSuccess(boolean isDone, boolean isMRZRequired) {
                closeEngine(1);
                if (isDone || ocrData.getFrontData() != null || ocrData.getBackData() != null) {
                    ocrCallback.onScannedComplete(ocrData);
                } else {
                    ocrCallback.onError("Failed Recognition");
                }
            }

            @Override
            void onUpdateProcess(String s) {
                if (!s.equals(ACCURA_ERROR_CODE_PROCESSING)) {
                    closeEngine(1);
                }
                if (!s.equals(ACCURA_ERROR_CODE_PROCESSING) && !s.equals(ACCURA_ERROR_CODE_FACE) && !s.equals(ACCURA_ERROR_CODE_FACE_BLUR) && !s.equals(ACCURA_ERROR_CODE_HOLOGRAM)) {
                    ocrCallback.onProcessUpdate(-1, s, false);
                }
            }

            @Override
            void onScannedFailed(String s) {
                closeEngine(1);
                ocrCallback.onError(s);
            }
        };

        InitModel i1 = initOcr(this.callBack, context, countryId, cardId, 1, 1);
        if (i1 != null && i1.getInitData() != null) {
            if (cardSide > 0) {
                if (i1.getInitData().getIsbothavailable()) {
                    updateData("Front");
                } else {
                    this.callBack.onScannedFailed("Back Side not available");
                    return;
                }
            }
            ocrData.setCardname(i1.getInitData().getCardName());
        } else {
            this.callBack.onScannedFailed("Failed Initialization");
            return;
        }


        Bitmap bitmap = bmCard.copy(Bitmap.Config.ARGB_8888, false);
        if (bmCard.getWidth() > 650) {
            int scaledWidth = 650;
            float ratio = scaledWidth / (float) bmCard.getWidth();
            int scaledHeight = (int) (bmCard.getHeight() * ratio);
            bitmap = Bitmap.createScaledBitmap(bmCard, scaledWidth, scaledHeight, true);
        }
        if (countryId == 2 && (cardId == 402 || cardId == 396 || cardId == 72 || cardId == 163 || cardId == 65)) {
            if (isMrzEnable && cardId == 65) {
                RecogResult result = new RecogResult();
                result.recType = RecogEngine.RecType.INIT;
                result.bRecDone = false;
                int ret = doRecogBitmap(bmCard, 0, intData, null, faced, true, documentType.value, "all");
                if (ret > 0) {
                    result.recType = RecType.MRZ;
                    result.ret = ret;
                    result.SetResult(intData);
                    ocrData.setMrzData(result);
                }
            }
            bmCard.recycle();
            doRecognition(/*mReference,*/ bitmap, null, ocrData, false);
            return;
        }
        float tempResolution = v;
        v = 0;
        ImageOpencv imageOpencv = checkCard(bitmap);
        v = tempResolution;
        if (imageOpencv != null) {
            if (imageOpencv.isSucess && imageOpencv.mat != null) {
                Bitmap card = imageOpencv.getBitmap(bmCard, bitmap.getWidth(), bitmap.getHeight(), false);
                int ret;
                if (isMrzEnable) {
                    RecogResult result = new RecogResult();
                    result.recType = RecogEngine.RecType.INIT;
                    result.bRecDone = false;
                    ret = doRecogBitmap(bmCard, 0, intData, null, faced, true, documentType.value, "all");
                    if (ret > 0) {
                        result.recType = RecType.MRZ;
                        result.ret = ret;
                        result.SetResult(intData);
                        ocrData.setMrzData(result);
                    }
                }
                if (bmCard != card) {
                    bmCard.recycle();
                }
                doRecognition(card, imageOpencv.mat, ocrData, false);
            } else {
//                refreshPreview();
                bmCard.recycle();
                if (imageOpencv.message.isEmpty()) {
                    this.callBack.onUpdateProcess(ACCURA_ERROR_CODE_DOCUMENT_IN_FRAME);
                }
            }
        } else this.callBack.onScannedFailed("Failed Initialization");
        AccuraLog.loge(TAG, "GetM - " + documentType);
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
        AccuraLog.loge(TAG, "MF Detect");
        detectFace(bitmap.copy(Config.ARGB_8888, false), ocrData, result, new ScanListener() {
            @Override
            public void onUpdateProcess(String s) {

            }

            @Override
            public void onScannedSuccess(boolean isDone, boolean isMRZRequired) {
                scanListener.onScannedSuccess(true, true);
            }

            @Override
            void onFaceScanned(Bitmap bitmap) {
                scanListener.onFaceScanned(bitmap);
            }

            @Override
            public void onScannedFailed(String s) {
                if (s.equals("1")) {
//                    if (i % 2 == 0 && result != null) {
//                        doFaceDetect(1, BitmapUtil.rotateBitmap(bitmap, 180), ocrData, result, scanListener);
//                    } else {
                    callBack.onUpdateProcess(ACCURA_ERROR_CODE_FACE);
                    scanListener.onScannedSuccess(false, false);
//                    }
                } else {
                    scanListener.onScannedSuccess(false, false);
                }
            }
        });
    }

    /**
     * To detect qatar card is upside down or wrong documnent
     * @param bmCard       camera frame
     * @param scanListener
     */
    public void doCheckData(Bitmap bmCard, ScanListener scanListener, int i,final int cB) {
        if (detector == null) {
            detector = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        }
//        final Bitmap docBmp = bmCard.copy(Config.ARGB_8888, false);
        int scaledWidth = 1200;
        float ratio = scaledWidth/(float) bmCard.getWidth();
        int scaledHeight = (int) (bmCard.getHeight()*ratio);
        Bitmap docBmp = Bitmap.createScaledBitmap(bmCard, scaledWidth, scaledHeight, true);

        detector.process(InputImage.fromBitmap(docBmp, 0))
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text text) {
                        docBmp.recycle();
                        Util.logd(TAG, "ocr_text -> " + i + "-> " + "\n" + text.getText());
                        String s = recognizeCard(text.getText(),i,cB);

                        if (s != null && !TextUtils.isEmpty(s)) {
                            JSONObject jsonObject = null;
                            try {
                                jsonObject = new JSONObject(s);
                                Util.logd(TAG, s);
                                int ret = jsonObject.getInt("responseCode");
                                if (ret >= 0) {
                                    String message = jsonObject.getString("responseMessage");
                                    JSONObject data = null;
                                    if (!TextUtils.isEmpty(message) && !message.equals(ACCURA_ERROR_CODE_DOCUMENT_IN_FRAME)) {
                                        if (!bmCard.isRecycled()) bmCard.recycle();
                                        scanListener.onUpdateProcess(message);
                                        return;
                                    }
                                    try {
                                        data = jsonObject.getJSONObject("data");
                                    } catch (JSONException e) {
                                        data = null;
                                    }
                                    if (data != null && data.has("i1")) {
                                        int i1 = data.getInt("i1");
                                        if (i1 <= 0) {
                                            Util.logd(TAG, "(wxh) -> "+(scaledWidth / 2) + "x" +  (scaledHeight / 3));
                                            List<Text.TextBlock> textBlocks = text.getTextBlocks();
                                            for (Text.TextBlock element : textBlocks) {
//                                for (Text.Line line : textBlock.getLines())
//                                    for (Text.Element element : line.getElements())
                                                if (element == null || element.getBoundingBox() == null)
                                                    continue;
                                                Util.logd(TAG, "(box) -> "+(element.getBoundingBox()));
                                                if (element.getBoundingBox().left < (scaledWidth / 2)
                                                        && element.getBoundingBox().top < (scaledHeight / 3)
                                                        && element.getBoundingBox().right <= (scaledWidth / 2)
                                                        && element.getBoundingBox().bottom <= (scaledHeight / 3)) {
                                                    String elementText = element.getText().toLowerCase();
                                                    if ((elementText.contains("of qatar") || elementText.contains("state of")
                                                            || element.getText().contains("State Qatar") || element.getText().contains("Qatar")
                                                            || (elementText.contains("state") && elementText.contains("qatar")))
                                                            || (elementText.contains("residency permit")
                                                            || (/*elementText.contains("residency") ||*/ elementText.contains("permit")))
                                                            || (elementText.contains("id. card") || elementText.contains("id.card") || elementText.contains("1d. card") || elementText.contains("id card"))) {
                                                        i1++;
                                                        break;
                                                    } else if (elementText.contains("residency")) {
                                                        String s1 = text.getText().toLowerCase();
                                                        if (s1.contains("general") || (s1.contains("general") && s1.contains("director"))
                                                                ||(s1.contains("authority") && s1.contains("signature"))) {
                                                            if (cB <= 0) {
                                                                scanListener.onUpdateProcess(ACCURA_ERROR_CODE_WRONG_SIDE);
                                                            }
                                                        }else if (cB <= 0){
                                                            i1++;
                                                            break;
                                                        }
                                                    }
                                                } else if (element.getBoundingBox().bottom > (scaledHeight / 3)) {
                                                    Util.logd(TAG, "(text...) -> "+(element.getText()));
                                                    Util.logd(TAG, "(box...) -> "+(element.getBoundingBox()));
                                                    break;
                                                }
                                            }
                                            if (i1 > 0) {
                                                if (!bmCard.isRecycled()) bmCard.recycle();
                                                // Only check for front Card validation document
                                                if (cB > 0) {
                                                    scanListener.onUpdateProcess(ACCURA_ERROR_CODE_WRONG_SIDE);
                                                } else {
                                                    scanListener.onScannedSuccess(true, i > 0/*means rotate 180*/);
                                                }
                                                return;
                                            }
                                        }

                                        if (i == 0 && i1 > 0) {
                                            if (cB > 0 && message.equals(ACCURA_ERROR_CODE_DOCUMENT_IN_FRAME)) {
                                                if (!bmCard.isRecycled()) bmCard.recycle();
                                                scanListener.onUpdateProcess(message);
                                                return;
                                            }
                                            if (!bmCard.isRecycled()) bmCard.recycle();
                                            scanListener.onScannedSuccess(true, false);
                                        } else/* if (i > 0)*/{
//                                            docBmp.recycle();
                                            if (data.has("isRotate") && data.getBoolean("isRotate") && i == 0) {
                                                doCheckData(BitmapUtil.rotateBitmap(bmCard, 180), scanListener, 1, cB);
                                            } else if (!TextUtils.isEmpty(message)) {
                                                if (!bmCard.isRecycled()) bmCard.recycle();
                                                scanListener.onUpdateProcess(message);
                                            } else {
                                                if (!bmCard.isRecycled()) bmCard.recycle();
                                                scanListener.onScannedSuccess(true, i1 > 0);
                                            }
                                        } /*else {
                                            doCheckData(BitmapUtil.rotateBitmap(docBmp, 180), scanListener, 1);
                                        }*/
                                    } else {
                                        if (!bmCard.isRecycled()) bmCard.recycle();
                                        scanListener.onUpdateProcess(message);
                                    }
                                }
                            } catch (JSONException e) {
                                if (!bmCard.isRecycled()) bmCard.recycle();
                                if (!docBmp.isRecycled()) docBmp.recycle();
                                AccuraLog.loge(TAG, e.toString());
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (!bmCard.isRecycled()) bmCard.recycle();
                    if (e.getMessage() != null) {
                        AccuraLog.loge(TAG, e.toString());
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
            detector = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        }
        AccuraLog.loge(TAG, "Recognize Data");
//        final Bitmap docBmp = bmCard.copy(Config.ARGB_8888, false);
        int scaledWidth = 1200;
        float ratio = scaledWidth/(float) bmCard.getWidth();
        int scaledHeight = (int) (bmCard.getHeight()*ratio);
        Bitmap docBmp = Bitmap.createScaledBitmap(bmCard, scaledWidth, scaledHeight, true);
        detector.process(InputImage.fromBitmap(docBmp, 0))
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text text) {
                        result.lines = "";
                        StringBuilder MlKitOcr = new StringBuilder();
                        List<Text.TextBlock> textBlocks = text.getTextBlocks();
                        for (Text.TextBlock textBlock : textBlocks)
                            for (Text.Line line : textBlock.getLines())
                                for (Text.Element element : line.getElements()) {
                                    if (element == null)
                                        continue;
                                    if (element.getBoundingBox() != null) {
                                        int heightDifference = element.getBoundingBox().bottom - element.getBoundingBox().top;
//                                        Util.logd("ocr_log", "12% h, text -> " + scaledHeight*0.12 + " -- " + heightDifference + "," + element.getText());
                                        if (heightDifference >= scaledHeight*0.12) {
                                            MlKitOcr.append(element.getText());
                                        }
                                    }
                                }

                        docBmp.recycle();
                        if (!TextUtils.isEmpty(MlKitOcr)) {
                            int ret = doDetectNumberPlate(MlKitOcr.toString(), intData, countryId, cardId);
                            AccuraLog.loge(TAG, "DL - " + ret);
                            if (ret > 0) {
                                int i, k = 0, len;
                                len = intData[k++];
                                byte[] tmp = new byte[len+1];
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
                AccuraLog.loge(TAG, e.toString());
            }
        });

    }

    /**
     * To detect and recognize bank card
     * @param bmCard       camera frame
     * @param cardDetails
     * @param recogType
     */
    public void doRecognizeCard(Bitmap bmCard, CardDetails cardDetails, RecogType recogType) {
        if (detector == null) {
            detector = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        }
//        final Bitmap docBmp = bmCard.copy(Config.ARGB_8888, false);

        int scaledWidth = 1200;
        float ratio = scaledWidth/(float) bmCard.getWidth();
        int scaledHeight = (int) (bmCard.getHeight()*ratio);
        Bitmap docBmp = Bitmap.createScaledBitmap(bmCard, scaledWidth, scaledHeight, true);

        //<editor-fold desc="Convert bitmap to Gray scale">
        if (recogType == RecogType.BANKCARD) {
            Mat mat = new Mat();
            Utils.bitmapToMat(docBmp, mat);
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
            Utils.matToBitmap(mat,docBmp);
            mat.release();
        }
        //</editor-fold>
        InputImage image = InputImage.fromBitmap(docBmp, 0);

        detector.process(image).addOnSuccessListener(text -> {
            docBmp.recycle();
            int ret = extractData(text.getText(), cardDetails);
            Util.logd(TAG, "doCheckData: "+ ret + "\n" + cardDetails.toString() + text.getText());
            if (!TextUtils.isEmpty(cardDetails.getNumber()) && !TextUtils.isEmpty(cardDetails.getExpirationDate())) {
                cardDetails.setBitmap(bmCard);
                this.callBack.onScannedSuccess(true,false);
            } else {
                bmCard.recycle();
                this.callBack.onScannedSuccess(false, false);
            }
        }).addOnFailureListener(e -> {
            this.callBack.onScannedFailed(e.getMessage());
        });
    }

    /**
     * Call this method if document is valid after {@see checkCard(Bitmap bmp)}
     *
//     * @param scanListener to get scanned data
     * @param src
     * @param mat          pass met to retrieve ocr data.
     * @param ocrData      to fill data to this object
     * @param isQIDcard    true if qatar id card
     */
    void doRecognition(/*ScanListener scanListener, */Bitmap src, Mat mat, OcrData ocrData, boolean isQIDcard) {
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
        if(mat == null) mat = new Mat();
        detectText(src, mat, ocrData, isQIDcard);
//        }

    }

    private void detectText(Bitmap src, Mat mat, OcrData ocrData, boolean isQIDcard) {
        Bitmap imageBitmap = bitmapFromMat(mat);
        if (isQIDcard || imageBitmap == null) {
            if (imageBitmap!=null && !imageBitmap.isRecycled()) imageBitmap.recycle();
            imageBitmap = src.copy(Config.ARGB_8888, false);
        }
        Bitmap image = imageBitmap;
        if (detector == null) {
            init();
        }
        AccuraLog.loge(TAG, "Recognize Data");
        int scaledWidth;
        int scaledHeight;
        if (image.getWidth() > image.getHeight()) {
            scaledWidth = 1200;
            float ratio = scaledWidth/(float) image.getWidth();
            scaledHeight = (int) (image.getHeight()*ratio);
        } else {
            scaledHeight = 1200;
            float ratio = scaledHeight/(float) image.getHeight();
            scaledWidth = (int) (image.getWidth()*ratio);
        }
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(image, scaledWidth, scaledHeight, true);
        if (countryId == 2 && (cardId == 72 || cardId == 163)) {
            callBack.onUpdateProcess(ACCURA_ERROR_CODE_PROCESSING);
        }
        detector.process(InputImage.fromBitmap(scaledBitmap, 0))
                .addOnSuccessListener(visionText -> {
                    if (TextUtils.isEmpty(visionText.getText())) {
                        callBack.onUpdateProcess(ACCURA_ERROR_CODE_DOCUMENT_IN_FRAME);
                        callBack.onScannedSuccess(false, false);
                        src.recycle();
                        image.recycle();
                        mat.release();
                        return;
                    }
                    if (countryId == 2 && (cardId == 402 || cardId == 396 || cardId == 65)) {
                        callBack.onUpdateProcess(ACCURA_ERROR_CODE_PROCESSING);
                    }
                    Utils.bitmapToMat(scaledBitmap,mat);
                    OcrData.MapData mapData = MapDataFunction(mat.getNativeObjAddr(), visionText);
                    List<OcrData.MapData.ScannedData> result = null;
                    if (mapData != null) {
                        result = mapData.getOcr_data();
                        findFace = mapData.getFace();
                    }
                    //check data is not null and empty
                    boolean isdone = result != null && result.size() != 0;
                    boolean isFinalDone = isdone;
                    boolean isContinue = true;
                    Util.logd(TAG, "done - " + isdone);
                    if (isdone) {
                        if (result.size() > 0 && result.get(0).getKey().equals("key")) {
                            mapData.getOcr_data().clear();
                        }
                        if (mapData.getCardSide().toLowerCase().contains("front")) {
                            if (ocrData.getFrontimage() != null) {
//                                isdone = false;
                                ocrData.getFrontimage().recycle();
                            }
                            if (isQIDcard) {
                                Imgproc.cvtColor(mat,mat,Imgproc.COLOR_BGR2RGB);
                                ocrData.setFrontimage(bitmapFromMat(mat));
                            } else
                                ocrData.setFrontimage(src.copy(Config.ARGB_8888, false));
                            ocrData.setFrontData(mapData);
                        } else {
                            if (ocrData.getBackimage() != null) {
//                                isdone = false;
                                ocrData.getBackimage().recycle();
                            }
                            ocrData.setBackimage(src.copy(Config.ARGB_8888, false));
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
//                                        updateData(mapData.card_side);
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
                        } else if (isContinue && callBack != null) {
//                        if (isdone && ocrData.getFrontData() != null && ocrData.getBackData() == null) {
//                            updateData(mapData.card_side);
//                        }
                            callBack.onScannedSuccess(isdone, isMrzEnable && CheckMRZisRequired(mapData));
                            src.recycle();
                            image.recycle();
                            mat.release();
                        }
                    } catch (Exception e) {
                    }
//                    if (isContinue && callBack != null) {
////                        if (isdone && ocrData.getFrontData() != null && ocrData.getBackData() == null) {
////                            updateData(mapData.card_side);
////                        }
//                        callBack.onScannedSuccess(isdone, isMrzEnable && CheckMRZisRequired(mapData));
//                        src.recycle();
//                        image.recycle();
//                        mat.release();
//                    }
                })
                .addOnFailureListener(e -> {
                    src.recycle();
                    image.recycle();
                    mat.release();
                    if (callBack != null) {
                        callBack.onScannedFailed(e.getMessage());
                    }
                });
    }

    /**
     * Detect face using OpenCV.
     *
     * @param bitmap
     * @param ocrData
     * @param result
     * @param scanListener
     */
    public void detectFace(Bitmap bitmap, OcrData ocrData, RecogResult result, ScanListener scanListener) {
        if ((result != null && result.faceBitmap != null)) {
            if (scanListener != null) {
                scanListener.onScannedSuccess(true, true);
            }
            bitmap.recycle();
            return;
        }
        if (bitmap != null && !bitmap.isRecycled()) {
            Mat matimage = new Mat();
            Utils.bitmapToMat(bitmap, matimage);

            String xyz = OpenCvFaceDetect(matimage.getNativeObjAddr());
            byte[] decodedString = Base64.decode(xyz, Base64.DEFAULT);
            Bitmap faceBitmap1 = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            matimage.release();
            if (xyz.length() > 50) {
                if (ocrData == null && this.callBack != null) {
                    // To show Processing... msg for MRZ document
                    this.callBack.onUpdateProcess(RecogEngine.ACCURA_ERROR_CODE_PROCESSING);
                }
                xyz = "";
                Mat clone = new Mat();

                Utils.bitmapToMat(faceBitmap1, clone);

                String s = doFaceCheck(clone.getNativeObjAddr(), v);
                clone.release();

                try {
                    if (s != null && !s.equals("")) {
                        JSONObject jsonObject = new JSONObject(s);
                        int ic = jsonObject.getInt("responseCode");
                        AccuraLog.loge(TAG, "checkf" + ic);
                        if (ic == 1) {
                            if (ocrData != null) {
                                if (ocrData.getFaceImage() != null)
                                    ocrData.getFaceImage().recycle();
                                ocrData.setFaceImage(faceBitmap1.copy(Config.ARGB_8888, false));
                                if (scanListener != null) {
                                    scanListener.onScannedSuccess(true, true);
                                }
                            } else if (result != null) {
                                result.faceBitmap = faceBitmap1.copy(Config.ARGB_8888, false);
//                                                        result.recType = RecType.FACE;
//                                                        result.docFrontBitmap = image.copy(Config.ARGB_8888, false);
                                if (result.recType == RecType.MRZ) {
                                    result.bRecDone = true;
                                }
                                if (scanListener != null) {
                                    scanListener.onFaceScanned(faceBitmap1.copy(Config.ARGB_8888, false));
                                }
                            }
                            bitmap.recycle();
                            faceBitmap1.recycle();
                        } else if (ic == 10) {
                            AccuraLog.loge(TAG, "failed check: "+ic );
                            bitmap.recycle();
                            faceBitmap1.recycle();
                            String message = jsonObject.getString("responseMessage");
                            if (!message.isEmpty() && this.callBack != null) {
                                this.callBack.onUpdateProcess(message);
                                scanListener.onScannedFailed("");
                            }
                        }
                    } else scanListener.onScannedFailed("");
                    if (!faceBitmap1.isRecycled()) faceBitmap1.recycle();
                } catch (JSONException e) {
                    bitmap.recycle();
                    if (!faceBitmap1.isRecycled()) faceBitmap1.recycle();
                    scanListener.onScannedFailed("");
                    AccuraLog.loge(TAG, Log.getStackTraceString(e));
                }
            } else {
                bitmap.recycle();
                xyz = "";
                if (faceBitmap1 != null && !faceBitmap1.isRecycled()) faceBitmap1.recycle();
                if (scanListener != null) {
                    if (ocrData != null) this.callBack.onUpdateProcess(RecogEngine.ACCURA_ERROR_CODE_FACE);
                    scanListener.onScannedFailed("1");
                }
            }
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
        if (mat != null && mat.width() > 0 && mat.height() > 0) {
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
            AccuraLog.loge(TAG, e.toString());
        }

        try {
            InputStream is = this.con.getResources().getAssets().open(fileName1);
            size = is.available();
            pDic1 = new byte[size];
            pDicLen1 = size;
            is.read(pDic1);
            is.close();
        } catch (IOException e) {
            AccuraLog.loge(TAG, e.toString());
        }

        return size;
    }

    void closeEngine(int destroy) {
        try {
            if (detector != null) {
                detector.close();
                detector = null;
            }
        } catch (Exception e) {
        }
        closeOCR(destroy);
    }

    public enum RecType {
        INIT, BOTH, FACE, MRZ
    }
}
