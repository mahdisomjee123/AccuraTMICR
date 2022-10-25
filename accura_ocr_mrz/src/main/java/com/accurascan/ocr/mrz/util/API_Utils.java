package com.accurascan.ocr.mrz.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.accurascan.ocr.mrz.model.OcrData;
import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.List;

public class API_Utils {
    private static final String TAG = API_Utils.class.getSimpleName();
    private static SCAN_RESULT mCallback;
    private ProgressDialog pd;
    private boolean done1;
    private boolean done2;

    public interface SCAN_RESULT {

        void onSuccess(String id, String message);

        void onFailed(String s);
    }

    private static final API_Utils ourInstance = new API_Utils();

    public static API_Utils getInstance(SCAN_RESULT callback) {
        mCallback = callback;
        return ourInstance;
    }

    private API_Utils() {
    }

/*
    public void checkData(int b, final Context context, final String countryCode, final OcrData ocrData, boolean isEnable, String api_key) {
        boolean isApiFEnabled = false, isApiBEnabled = false;
        done1 = done2 = false;
        if (ocrData.getFrontData() != null && ocrData.getFrontData().getIsApi()) {
            isApiFEnabled = true;
        }
        if (ocrData.getBackData() != null && ocrData.getBackData().getIsApi()) {
            isApiBEnabled = true;
        }
        done1 = !isApiFEnabled;
        done2 = !isApiBEnabled;
        if (isEnable && (isApiFEnabled || isApiBEnabled)) {
            if (!isNetworkAvailable(context)) {
                if (mCallback != null) {
                    mCallback.onFailed("Please check your internet connection");
                }
                return;
            }
            initialize(context);
            if (isApiFEnabled) getResultf(true,countryCode, ocrData, api_key, 0);
            if (isApiBEnabled) getResultf(false,countryCode, ocrData, api_key, 0);
        } else if (mCallback != null) {
            mCallback.onSuccess("1", "Failed");
        }

    }
*/
    public void checksssssData(int b, final Context context, final String countryCode, final OcrData ocrData, boolean isEnable, String api_key) {
        done1 = done2 = false;
        boolean isApiFEnabled = false, isApiBEnabled = false;
        if (b == 0 || b == 1)
            if (ocrData.getFrontData() != null && ocrData.getFrontData().getIsApi()) {
                isApiFEnabled = true;
            }
        if (b == 0 || b == 2)
            if (ocrData.getBackData() != null && ocrData.getBackData().getIsApi()) {
                isApiBEnabled = true;
            }
        if (isEnable && (isApiFEnabled || isApiBEnabled)) {
            if (!isNetworkAvailable(context)) {
                if (mCallback != null) {
                    if (isApiFEnabled) ocrData.getFrontData().getOcr_data().clear();
                    if (isApiBEnabled) ocrData.getBackData().getOcr_data().clear();
                    mCallback.onFailed("Please check your internet connection");
                }
                return;
            }
            if (TextUtils.isEmpty(countryCode)) {
                if (mCallback != null) {
                    if (isApiFEnabled) ocrData.getFrontData().getOcr_data().clear();
                    if (isApiBEnabled) ocrData.getBackData().getOcr_data().clear();
                    mCallback.onFailed("No Data found for this card");
                }
                return;
            }
            if (b == 0 || (ocrData.getFrontData() != null && ocrData.getBackData() != null)) initialize(context);
            done1 = !isApiFEnabled;
            done2 = !isApiBEnabled;
            if (isApiFEnabled) getResultf(true,countryCode, ocrData, api_key, 0);
            if (isApiBEnabled) getResultf(false,countryCode, ocrData, api_key, 0);
        } else if (mCallback != null) {
            mCallback.onSuccess("1", "Success");
        }

    }

    private void initialize(Context context) {

        if (pd == null) {
            pd = new ProgressDialog(context);
            pd.setCancelable(false);
            pd.setCanceledOnTouchOutside(false);
            pd.setMessage("Loading");
        }
        try {
            if (pd != null && !pd.isShowing()) {
                pd.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkConnection(final Context context, final boolean isFront, final String countryCode, final OcrData ocrData, final String url, final String api_key) {

        if (isNetworkAvailable(context)) {
            getResultf(isFront,countryCode, ocrData, api_key,0);
        } else {
            if (mCallback != null) {
                mCallback.onFailed("Please check your internet connection");
            }
        }
    }

    private boolean isNetworkAvailable(Context mContext) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private String base64FromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

/*
    private void getResult(boolean isFront, String countryCode, OcrData ocrData, String url, String api_key) {
        Log.e(TAG, "getResult: " + isFront + done1 + done2);
        List<OcrData.MapData.ScannedData> scannedDataList;
        String cardCode;
        Bitmap bmp;
        if (isFront){
            bmp = ocrData.getFrontimage();
            scannedDataList = ocrData.getFrontData().getOcr_data();
            cardCode = ocrData.getFrontData().getCard_code();
        } else {
            bmp = ocrData.getBackimage();
            scannedDataList = ocrData.getBackData().getOcr_data();
            cardCode = ocrData.getBackData().getCard_code();
        }
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        bmp.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
//        byte[] byteArray = byteArrayOutputStream.toByteArray();
//        String encodedImage = Base64.encodeToString(byteArray, Base64.NO_WRAP);
        String encodedImage = base64FromBitmap(bmp);

        if (encodedImage != null) {
            AndroidNetworking.post(url)
                    .addHeaders("Api-Key", api_key)
                    .addBodyParameter("country_code", countryCode)
                    .addBodyParameter("card_code", cardCode)
                    .addBodyParameter("scan_image_base64", encodedImage)
                    .setPriority(Priority.HIGH)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {

                        @Override
                        public void onResponse(JSONObject response) {
                            scannedDataList.clear();

                            String status = "success";
                            try {
                                status = response.getString("Status");
                            } catch (JSONException e) {
                                AccuraLog.loge(TAG, Log.getStackTraceString(e));
                            }
                            if (status.equalsIgnoreCase("Success")) {
                                try {
                                    JSONObject res2 = response.getJSONObject("data");
                                    JSONObject posts = res2.getJSONObject("OCRdata");
                                    String key, value;
                                    Iterator<String> listKEY = posts.keys();
                                    do {
                                        key = listKEY.next();
                                        try {
                                            value = posts.getString(key);
                                            if (!key.equalsIgnoreCase("mrz") && !key.equals("face") && !key.equals("signature")) {
                                                try {
                                                    JSONObject jsonObject = new JSONObject();
                                                    jsonObject.put("type", 1);
                                                    jsonObject.put("key", key);
                                                    jsonObject.put("key_data", value);
                                                    OcrData.MapData.ScannedData scannedData = new Gson().fromJson(jsonObject.toString(), OcrData.MapData.ScannedData.class);
                                                    scannedDataList.add(scannedData);
                                                } catch (JSONException e) {
                                                    AccuraLog.loge(TAG, Log.getStackTraceString(e));
                                                }
                                            }
                                        } catch (JSONException e) {
                                            AccuraLog.loge(TAG, Log.getStackTraceString(e));
                                        }
                                    } while (listKEY.hasNext());
                                } catch (JSONException e) {
                                    AccuraLog.loge(TAG, Log.getStackTraceString(e));
                                }
                                if (isFront){
                                    done1 = true;
                                    if (!isApiBEnabled) done2 = true;
                                }
                                else {
                                    done2 = true;
                                    if (!isApiFEnabled) done1 = true;
                                }
                                if (done1 && done2) {
                                    hidePD();
                                    if (mCallback != null) {
                                        mCallback.onSuccess("1", "Success");
                                    }
                                }
                            } else {
                                if (isFront) done1 = true;
                                else done2 = true;
                                if (done1 && done2) {
                                    hidePD();
                                    if (mCallback != null) {
                                        mCallback.onFailed("Template Did Not Match" + (isFront ? "Front" : "Back") +", Please Try Again");
                                    }
                                }
                            }
                        }

                        @Override
                        public void onError(ANError error) {
                            Log.e(TAG, "onError: " + isFront + "," + rF + ", " + rB);
                            if (isFront) done1 = rF++ > 1;
                            else done2 = rB++ > 1;
                            Log.e(TAG, "onError: " + isFront + "," + rF + ", " + rB + ", " + done1 + ", " + done2);
                            if (done1 && done2) {
                                hidePD();
                                if (mCallback != null) {
                                    mCallback.onFailed(error.getErrorDetail());
                                }
                            } else getResult(isFront, countryCode, ocrData, url, api_key);
                        }
                    });
        }
    }
*/
    private void getResultf(boolean isFront, String countryCode, OcrData ocrData, String api_key, final int i) {
        List<OcrData.MapData.ScannedData> scannedDataList;
        String cardCode;
        Bitmap bmp;
        if (isFront){
            bmp = ocrData.getFrontimage();
            scannedDataList = ocrData.getFrontData().getOcr_data();
            cardCode = ocrData.getFrontData().getCard_code();
        } else {
            bmp = ocrData.getBackimage();
            scannedDataList = ocrData.getBackData().getOcr_data();
            cardCode = ocrData.getBackData().getCard_code();
        }

        String encodedImage = base64FromBitmap(bmp);

        if (encodedImage != null) {
            OcrData.MapData.ScannedData signatureImage = null;
            for (OcrData.MapData.ScannedData data:scannedDataList) {
                if (data!=null && data.type == 2 && data.key.equalsIgnoreCase("signature")) {
                    signatureImage = data;
                    break;
                }
            }
            OcrData.MapData.ScannedData finalSignatureImage = signatureImage;
            scannedDataList.clear();
            AndroidNetworking.post("https://accurascan.com/api/v4/ocr")
                    .addHeaders("Api-Key", api_key)
                    .addBodyParameter("country_code", countryCode)
                    .addBodyParameter("card_code", cardCode)
                    .addBodyParameter("scan_image_base64", encodedImage)
                    .setPriority(Priority.HIGH)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {

                        @Override
                        public void onResponse(JSONObject response) {
                            String status = "success";
                            try {
                                status = response.getString("Status");
                            } catch (JSONException e) {
                                AccuraLog.loge(TAG, Log.getStackTraceString(e));
                            }
                            if (status.equalsIgnoreCase("Success")) {
                                try {
                                    JSONObject res2 = response.getJSONObject("data");
                                    JSONObject posts = res2.getJSONObject("OCRdata");
                                    String key, value;
                                    Iterator<String> listKEY = posts.keys();
                                    do {
                                        key = listKEY.next();
                                        try {
                                            value = posts.getString(key);
                                            if (!key.equalsIgnoreCase("mrz") && !key.equals("face") && !key.equals("signature")) {
                                                try {
                                                    JSONObject jsonObject = new JSONObject();
                                                    jsonObject.put("type", 1);
                                                    jsonObject.put("key", convertString(key));
                                                    jsonObject.put("key_data", value);
                                                    OcrData.MapData.ScannedData scannedData = new Gson().fromJson(jsonObject.toString(), OcrData.MapData.ScannedData.class);
                                                    scannedDataList.add(scannedData);
                                                } catch (JSONException e) {
                                                    AccuraLog.loge(TAG, Log.getStackTraceString(e));
                                                }
                                            }
                                        } catch (JSONException e) {
                                            AccuraLog.loge(TAG, Log.getStackTraceString(e));
                                        }
                                    } while (listKEY.hasNext());
                                    if (finalSignatureImage != null) {
                                        scannedDataList.add(finalSignatureImage);
                                    }
                                } catch (JSONException e) {
                                    AccuraLog.loge(TAG, Log.getStackTraceString(e));
                                }
                                if (isFront) done1 = true;
                                else done2 = true;
                                if (done1 && done2) {
                                    hidePD();
                                    if (mCallback != null) {
                                        mCallback.onSuccess("1", "Success");
                                    }
                                }
                            } else {
                                if (isFront) done1 = true;
                                else done2 = true;
                                if (done1 && done2) {
                                    hidePD();
                                    if (mCallback != null) {
                                        try {
                                            if (status.equalsIgnoreCase("Fail") && response.has("Message") && !TextUtils.isEmpty(response.getString("Message"))) {
                                                mCallback.onFailed(response.getString("Message"));
                                                return;
                                            }
                                        } catch (JSONException e) {
                                        }
                                        mCallback.onFailed("Template Did Not Match" + (isFront ? "Front" : "Back") + ", Please Try Again");
                                    }
                                }
                            }
                        }
                        String convertString( String s )
                        {
                            try {
                                int n = s.length( ) ;
                                char[] ch = s.toCharArray( ) ;
                                int c = 0 ;
                                for ( int i = 0; i < n; i++ )
                                {
                                    if( i == 0 ) ch[ i ] = Character.toUpperCase( ch[ i ] ) ;
                                    // as we need to replace all the '_' by spaces in between, we check for '_'
                                    if ( ch[ i ] == '_' || ch[ i ] == ' ' )
                                    {
                                        ch[ c++ ] = ' ' ;
                                        // converting the letter immediately after the space to upper case
                                        ch[ c ] = Character.toUpperCase( ch[ i + 1] ) ;
                                    }
                                    else ch[ c++ ] = ch[ i ] ;
                                }
                                return String.valueOf( ch, 0, n) ;
                            } catch (Exception e) {
                                return s;
                            }
                        }

                        @Override
                        public void onError(ANError error) {
                            AccuraLog.loge(TAG, Log.getStackTraceString(error));
                            if (isFront) done1 = i > 1;
                            else done2 = i > 1;
                            if (done1 && done2) {
                                hidePD();
                                if (mCallback != null) {
                                    mCallback.onFailed(error.getErrorDetail());
                                }
                            } else if (i < 2) getResultf(isFront, countryCode, ocrData, api_key, i + 1);
                        }
                    });
        }
    }

    private void hidePD() {
        if (pd != null && pd.isShowing()) {
            try {
                pd.dismiss();
            } catch (Exception e) {
            }
            try {
                pd.cancel();
            } catch (Exception e) {
            }
            pd = null;
        }
    }
}