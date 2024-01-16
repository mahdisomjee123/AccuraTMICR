package com.accurascan.accuraocr.sample;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.accurascan.accuraocr.sample.download.DownloadListener;
import com.accurascan.accuraocr.sample.download.DownloadUtils;
import com.accurascan.ocr.mrz.interfaces.OcrCallback;
import com.accurascan.ocr.mrz.model.ContryModel;
import com.accurascan.ocr.mrz.model.OcrData;
import com.accurascan.ocr.mrz.model.RecogResult;
import com.accurascan.ocr.mrz.util.AccuraLog;
import com.accurascan.ocr.mrz.util.Util;
import com.androidnetworking.error.ANError;
import com.docrecog.scan.MRZDocumentType;
import com.docrecog.scan.RecogEngine;
import com.docrecog.scan.RecogType;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private ProgressDialog progressBar;
    private boolean isContinue = false;
    private RadioGroup swCardSide;

    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                if (activity.progressBar != null && activity.progressBar.isShowing()) {
                    activity.progressBar.dismiss();
                }
                activity.isContinue = true;
                if (msg.what == 1) {
                    if (activity.sdkModel.isMRZEnable) {
                        activity.btnIdMrz.setVisibility(View.VISIBLE);
                        activity.btnVisaMrz.setVisibility(View.VISIBLE);
                        activity.btnPassportMrz.setVisibility(View.VISIBLE);
                        activity.btnMrz.setVisibility(View.VISIBLE);
                        if (activity.isStaticOCR == 1) {
                            activity.btnCaptureMrz.setVisibility(View.VISIBLE);
                        }
                    }
                    if (activity.sdkModel.isBankCardEnable)
                        activity.btnBank.setVisibility(View.VISIBLE);
                    if (activity.sdkModel.isAllBarcodeEnable)
                        activity.btnBarcode.setVisibility(View.VISIBLE);
                    if (activity.sdkModel.isOCREnable && activity.modelList != null) {
                        activity.setCountryLayout();
                    }
                } else {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(activity);
                    builder1.setMessage(activity.responseMessage);
                    builder1.setCancelable(true);
                    builder1.setPositiveButton(
                            "OK",
                            (dialog, id) -> dialog.cancel());
                    AlertDialog alert11 = builder1.create();
                    alert11.show();
                }
            }
        }
    }

    RecogEngine recogEngine = new RecogEngine();
    private static class NativeThread extends Thread {
        private final WeakReference<MainActivity> mActivity;
        RecogEngine recogEngine;
        private String cardParams;
        private String licenseFilePath;

        public NativeThread(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
            recogEngine = activity.recogEngine;
            this.licenseFilePath = "";
        }

        public void setFilePath(String s) {
            this.licenseFilePath = s;
        }

        public void setCardParams(String s){
            this.cardParams = s;
        }
        @Override
        public void run() {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                try {
                    activity.isContinue = false;
                    AccuraLog.enableLogs(true); // make sure to disable logs in release mode
                    AccuraLog.refreshLogfile(activity);
                    AccuraLog.loge(TAG,recogEngine.getVersion());
                    recogEngine.setDialog(false); // setDialog(false) To set your custom dialog for license validation

                    if (!licenseFilePath.isEmpty()) {
                        activity.sdkModel = recogEngine.initEngine(activity, licenseFilePath);
                    } else
                        activity.sdkModel = recogEngine.initEngine(activity);
                    if (activity.sdkModel == null){
                        activity.handler.sendEmptyMessage(0);
                        return;
                    }
                    AccuraLog.loge(TAG, "SDK version" + recogEngine.getSDKVersion() + "\nInitialized Engine : " + activity.sdkModel.i + " -> " + activity.sdkModel.message);
                    activity.responseMessage = activity.sdkModel.message;

                    if (activity.sdkModel.i >= 0) {

                        // if OCR enable then get card list
                        if (activity.sdkModel.isOCREnable)
                            activity.modelList = recogEngine.getCardList(activity.getApplicationContext());

                        if (cardParams != null) {
                            JSONObject object = new JSONObject(cardParams);
                            try {
                                recogEngine.setBlurPercentage(activity, object.getInt("setBlurPercentage"));
                                recogEngine.setFaceBlurPercentage(activity, object.getInt("setFaceBlurPercentage"));
                                recogEngine.setGlarePercentage(activity, 6, 98);
                                recogEngine.isCheckPhotoCopy(activity, object.getBoolean("isCheckPhotoCopy"));
                                recogEngine.SetHologramDetection(activity, object.getBoolean("SetHologramDetection"));
                                recogEngine.setLowLightTolerance(activity, object.getInt("setLowLightTolerance"));
                                recogEngine.setMotionThreshold(activity, object.getInt("setMotionThreshold"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            recogEngine.setBlurPercentage(activity, 62);
                            recogEngine.setFaceBlurPercentage(activity, 70);
                            recogEngine.setGlarePercentage(activity, 6, 98);
                            recogEngine.isCheckPhotoCopy(activity, false);
                            recogEngine.SetHologramDetection(activity, true);
                            recogEngine.setLowLightTolerance(activity, 39);
                            recogEngine.setMotionThreshold(activity, 18);
                        }
                        activity.handler.sendEmptyMessage(1);
                    } else
                        activity.handler.sendEmptyMessage(0);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            super.run();
        }
    }

    private Handler handler = new MyHandler(this);
    private NativeThread nativeThread = new NativeThread(this);
    private RecyclerView rvCountry, rvCards;
    private LinearLayoutManager lmCountry, lmCard;
    private CardListAdpter countryAdapter, cardAdapter;
    private List<Object> contryList = new ArrayList<>();
    private List<Object> cardList = new ArrayList<>();
    private List<ContryModel> modelList;
    private int selectedPosition = -1;
    private ContryModel.CardModel _cardModel = null;
    private View btnCaptureMrz, btnMrz, btnPassportMrz, btnIdMrz, btnVisaMrz, btnBarcode,btnBank, lout_country;
    private RecogEngine.SDKModel sdkModel;
    private String responseMessage;
    private final String KEY_COUNTRY_VIEW_STATE = "country_state";
    private final String KEY_COUNTRY_SCROLL_VIEW_STATE = "country_scroll_state";
    private final String KEY_CARD_VIEW_STATE = "card_state";
    private final String KEY_VIEW_STATE = "view_state";
    private final String KEY_POSITION_STATE = "position_state";
    Parcelable listCountryState, listCardStart;
    private boolean isCardViewVisible;
    private int[] position;
    private NestedScrollView scrollView;
    final private int PICK_IMAGE = 1; // request code of select image from gallery
    final private int PICK_MRZ_IMAGE = 2; // request code of select image from gallery
    final private int isStaticOCR = 0;

    private void setCountryLayout() {
//        contryList = new ArrayList<>();
        contryList.clear();
        contryList.addAll(modelList);
        countryAdapter.notifyDataSetChanged();
        MainActivity.this.rvCountry.setVisibility(View.VISIBLE);
        MainActivity.this.rvCards.setVisibility(View.INVISIBLE);
        restoreInstantState();
    }

    public void downloadTextFile(Context context, final String link) {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (!(activeNetworkInfo != null && activeNetworkInfo.isConnected())) {
            onErrora("Please check your internet connection", null);
            return;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String oldVersion = sharedPreferences.getString(DownloadUtils.LICENSE_VERSION, "");
        String lastSavedFile = sharedPreferences.getString(DownloadUtils.LICENSE_NAME, "");

        File licenseDir = new File(context.getFilesDir().toString(), "accura");
        DownloadUtils.getInstance(new DownloadListener() {
            @Override
            public void onDownloadComplete(String licenseDetails) {

                String updatedVersion = "";
                String fileName = "";
                try {
                    JSONObject jsonObject = new JSONObject(licenseDetails);
                    if (!jsonObject.has("Android")) {
                        onErrora("Please add license details for Android", null);
                        return;
                    }
                    JSONObject object = jsonObject.getJSONObject("Android");
                    if (object.has("card_params") && !object.isNull("card_params")) {
                        nativeThread.setCardParams(object.getJSONObject("card_params").toString());
                    }
                    if (object.has("ocr_license") && !TextUtils.isEmpty(object.getString("ocr_license"))) {
                        fileName = object.getString("ocr_license");
                    }
                    if (jsonObject.has("version") && !TextUtils.isEmpty(jsonObject.getString("version"))) {
                        updatedVersion = jsonObject.getString("version");
                    }
                } catch (JSONException e) {
                    AccuraLog.loge(TAG, Log.getStackTraceString(e));
                }

                if (fileName.isEmpty()) {
                    onErrora("File details not valid", null);
                    return;
                }

                if (lastSavedFile.isEmpty() || oldVersion.isEmpty() || !updatedVersion.equals(oldVersion)) {
                    String finalUpdatedVersion = updatedVersion;
                    showDialog(progress_bar_type);
                    DownloadUtils.getInstance(new DownloadListener() {
                        @Override
                        public void onDownloadComplete(String fileName) {
                            sharedPreferences.edit().putString(DownloadUtils.LICENSE_VERSION, finalUpdatedVersion).apply();
                            sharedPreferences.edit().putString(DownloadUtils.LICENSE_NAME, fileName).apply();
                            nativeThread.setFilePath(licenseDir.getPath() + "/" + fileName);
                            if (pDialog != null && pDialog.isShowing()) {
                                pDialog.dismiss();
                            }
                            nativeThread.start();
                        }

                        @Override
                        public void onProgress(int progress) {
                            if (pDialog != null) {
                                pDialog.setProgress(progress);
                            }
                        }

                        @Override
                        public void onError(String s, ANError error) {
                            sharedPreferences.edit().putString(DownloadUtils.LICENSE_VERSION, "").apply();
                            onErrora(s, error);
                        }

                    }).downloadLicenseFile(licenseDir.getPath(), link.substring(0, link.lastIndexOf("/")+1)+ fileName, fileName, lastSavedFile);

                } else {
                    File licenseFilePath = null;
                    if (new File(licenseDir, fileName).exists()) {
                        licenseFilePath = new File(licenseDir, fileName);
                    } else if (new File(licenseDir, lastSavedFile).exists()) {
                        licenseFilePath = new File(licenseDir, lastSavedFile);
                    }
                    if (licenseFilePath != null) {
                        nativeThread.setFilePath(licenseFilePath.getPath());
                    } else {
                        onErrora(DownloadUtils.ERROR_CODE_FILE_NOT_FOUND, null);
                    }
                    nativeThread.start();
                }
            }

            @Override
            public void onError(String s, ANError error) {
                onErrora(s, error);
            }
        }).parseFile(licenseDir.getPath(), link, link.substring(link.lastIndexOf("/")));
    }
    private void download() {
        if (!isFinishing()) {
            progressBar.show();
        }
        downloadTextFile(this, "https://dev.accurascan.com/mahdiedit/accura-config1.json");
    }

    public void onErrora(String s, ANError error) {
        Toast.makeText(MainActivity.this, s+"\n"+Log.getStackTraceString(error), Toast.LENGTH_SHORT).show();
        if (pDialog != null && pDialog.isShowing()) {
            pDialog.dismiss();
        }
        nativeThread.start();
    }

    // Progress Dialog
    private ProgressDialog pDialog;
    public static final int progress_bar_type = 0;

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case progress_bar_type: // we set this to 0
                pDialog = new ProgressDialog(this);
                pDialog.setMessage("Downloading file. Please wait...");
                pDialog.setIndeterminate(false);
                pDialog.setMax(100);
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setCanceledOnTouchOutside(false);
                pDialog.setCancelable(false);
                pDialog.show();
                return pDialog;
            default:
                return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvVersion = findViewById(R.id.tv_version);
        tvVersion.setText("Version : " + BuildConfig.VERSION_NAME);
        swCardSide = findViewById(R.id.r_group);
        scrollView = findViewById(R.id.scroll_view);
        btnCaptureMrz = findViewById(R.id.lout_capture_mrz);
        btnCaptureMrz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, ""), PICK_MRZ_IMAGE);
            }
        });
        btnMrz = findViewById(R.id.lout_mrz);
        btnMrz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OcrActivity.class);
                RecogType.MRZ.attachTo(intent);
                MRZDocumentType.NONE.attachTo(intent);
                intent.putExtra("card_name", getResources().getString(R.string.other_mrz));
                intent.putExtra("app_orientation", getRequestedOrientation());
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        btnPassportMrz = findViewById(R.id.lout_passport_mrz);
        btnIdMrz = findViewById(R.id.lout_id_mrz);
        btnVisaMrz = findViewById(R.id.lout_visa_mrz);
        btnPassportMrz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OcrActivity.class);
                RecogType.MRZ.attachTo(intent);
                MRZDocumentType.PASSPORT_MRZ.attachTo(intent);
                intent.putExtra("card_name", getResources().getString(R.string.passport_mrz));
                intent.putExtra("app_orientation", getRequestedOrientation());
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });
        btnIdMrz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OcrActivity.class);
                RecogType.MRZ.attachTo(intent);
                MRZDocumentType.ID_CARD_MRZ.attachTo(intent);
                intent.putExtra("card_name", getResources().getString(R.string.id_mrz));
                intent.putExtra("app_orientation", getRequestedOrientation());
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });
        btnVisaMrz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OcrActivity.class);
                RecogType.MRZ.attachTo(intent);
                MRZDocumentType.VISA_MRZ.attachTo(intent);
                intent.putExtra("card_name", getResources().getString(R.string.visa_mrz));
                intent.putExtra("app_orientation", getRequestedOrientation());
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        btnBank = findViewById(R.id.lout_bank);
        btnBank.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OcrActivity.class);
                RecogType.BANKCARD.attachTo(intent);
                intent.putExtra("card_name", getResources().getString(R.string.bank_card));
                intent.putExtra("app_orientation", getRequestedOrientation());
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        btnBarcode = findViewById(R.id.lout_barcode);
        btnBarcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OcrActivity.class);
                RecogType.BARCODE.attachTo(intent);
                intent.putExtra("card_name", "Barcode");
                intent.putExtra("app_orientation", getRequestedOrientation());
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        lout_country = findViewById(R.id.lout_country);
        rvCountry = findViewById(R.id.rv_country);
        lmCountry = new LinearLayoutManager(this);
        rvCountry.setLayoutManager(lmCountry);
        countryAdapter = new CardListAdpter(this, contryList);
        rvCountry.setAdapter(countryAdapter);

        rvCards = findViewById(R.id.rv_card);
        lmCard = new LinearLayoutManager(this);
        rvCards.setLayoutManager(lmCard);
        cardAdapter = new CardListAdpter(this, cardList);
        rvCards.setAdapter(cardAdapter);
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", this)));
                    startActivityForResult(intent, 2296);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivityForResult(intent, 2296);
                }
            } else {
                doWork();
            }
        } else*/ if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Util.isPermissionsGranted(this)) {
            requestCameraPermission();
        } else {
            doWork();
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_land_port, menu);
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        final int orientation = display.getOrientation();
        MenuItem item = menu.findItem(R.id.item_land_port);
        switch (orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                item.setTitle("Portrait");
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                item.setTitle("Portrait");
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (!isContinue){
            return super.onOptionsItemSelected(item);
        }
        if (item.getItemId() == R.id.item_land_port) {

            if (item.getTitle().toString().toLowerCase().equals("landscape")) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                item.setTitle("Portrait");
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                item.setTitle("Landscape");
            }
        }
        return super.onOptionsItemSelected(item);
    }

    //requesting the camera permission
    public void requestCameraPermission() {
        int currentapiVersion = Build.VERSION.SDK_INT;
        if (currentapiVersion >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2296) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    doWork();
                }
            }
        } else if (requestCode == PICK_IMAGE || requestCode == PICK_MRZ_IMAGE) { //handle request code PICK_IMAGE used for selecting image from gallery

            if (data == null) // data contain result of selected image from gallery and other
                return;

            if (progressBar != null && !progressBar.isShowing()) {
                progressBar.setMessage("Processing...");
                progressBar.show();
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    final Uri[] selectedImageUri = {data.getData()};
                    // Get the path from the Uri
                    final String path = FileUtils.getPath(MainActivity.this, data.getData());//getPathFromURI(selectedImageUri[0]);
                    if (path != null) {
                        File f = new File(path);
                        selectedImageUri[0] = Uri.fromFile(f);
                        Bitmap selectedImageBitmap;
                        try {
                            selectedImageBitmap = MediaStore.Images.Media.getBitmap(MainActivity.this.getContentResolver(), selectedImageUri[0]);

                            if (requestCode == PICK_MRZ_IMAGE) {
                                recogEngine.detectFromCapturedImage(MainActivity.this, selectedImageBitmap, MRZDocumentType.NONE, "all",1, new OcrCallback(){
                                    @Override
                                    public void onUpdateLayout(int width, int height) {

                                    }

                                    @Override
                                    public void onScannedComplete(Object result) {
                                        if (progressBar != null && progressBar.isShowing()) {
                                            progressBar.dismiss();
                                        }
                                        ((RecogResult) result).docFrontBitmap = selectedImageBitmap;
                                        RecogResult.setRecogResult((RecogResult) result);
                                        Intent intent = new Intent(MainActivity.this, OcrResultActivity.class);
                                        RecogType.MRZ.attachTo(intent);
                                        intent.putExtra("app_orientation", getRequestedOrientation());
                                        startActivityForResult(intent, 101);
                                    }

                                    @Override
                                    public void onProcessUpdate(int titleCode, String errorMessage, boolean isFlip) {
                                        if (progressBar != null && progressBar.isShowing()) {
                                            progressBar.dismiss();
                                        }
                                        if (errorMessage.equals(RecogEngine.ACCURA_ERROR_CODE_DOCUMENT_IN_FRAME)) {
                                            Toast.makeText(MainActivity.this, "Card not Matched", Toast.LENGTH_LONG).show();
                                        } else Toast.makeText(MainActivity.this, "Message : "+errorMessage, Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onError(String errorMessage) {
                                        if (progressBar != null && progressBar.isShowing()) {
                                            progressBar.dismiss();
                                        }
                                        Toast.makeText(MainActivity.this, "Error Message : "+errorMessage, Toast.LENGTH_SHORT).show();
                                    }
                                });

                                return;
                            }

                            int country_id = ((ContryModel) MainActivity.this.contryList.get(selectedPosition)).getCountry_id();
                            int card_id = _cardModel.getCard_id();
                            String[] cardCodes = new String[]{"Front Side", "Back Side"};
                            Log.e(TAG, "cardCodes:  " + Arrays.toString(cardCodes));

                            int cardSide;
                            switch (swCardSide.getCheckedRadioButtonId()){
                                case R.id.rb_back:
                                    cardSide = 1;
                                    break;
                                default: cardSide = 0; break;
                            }
                            recogEngine.detectOCRFromCapturedImage(MainActivity.this, selectedImageBitmap, MRZDocumentType.NONE, country_id, card_id, cardSide, new OcrCallback() {
                                @Override
                                public void onUpdateLayout(int width, int height) {

                                }

                                @Override
                                public void onScannedComplete(Object result) {
                                    if (progressBar != null && progressBar.isShowing()) {
                                        progressBar.dismiss();
                                    }
                                    OcrData.setOcrResult((OcrData) result);
                                    Intent intent = new Intent(MainActivity.this, OcrResultActivity.class);
                                    RecogType.OCR.attachTo(intent);
                                    intent.putExtra("app_orientation", getRequestedOrientation());
                                    startActivity(intent);
                                }

                                @Override
                                public void onProcessUpdate(int titleCode, String errorMessage, boolean isFlip) {
                                    if (progressBar != null && progressBar.isShowing()) {
                                        progressBar.dismiss();
                                    }
                                    Toast.makeText(MainActivity.this, "Message : "+getErrorMessage(errorMessage), Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    if (progressBar != null && progressBar.isShowing()) {
                                        progressBar.dismiss();
                                    }
                                    Toast.makeText(MainActivity.this, "Error Message : "+errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (IOException e) {
                            if (progressBar != null && progressBar.isShowing()) {
                                progressBar.dismiss();
                            }
                            e.printStackTrace();
                        }
                    } else {
                        if (progressBar != null && progressBar.isShowing()) {
                            progressBar.dismiss();
                        }
                        Toast.makeText(MainActivity.this, "Image not found", Toast.LENGTH_SHORT).show();
                    }
                }
            }, 1000);
        }
    }
    private String getErrorMessage(String s) {
        switch (s) {
            case RecogEngine.ACCURA_ERROR_CODE_DOCUMENT_IN_FRAME:
                return "Card not Matched";
            default:
                return s;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
        }
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        doWork();
                    } catch (Exception e) {
                        //   e.printStackTrace();
                    }

                } else {
                    Toast.makeText(this, "You declined to allow the app to access your camera", Toast.LENGTH_LONG).show();
                }
        }
    }

    public void doWork() {
        progressBar = new ProgressDialog(this);
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.setMessage("Please wait...");
        progressBar.setCancelable(false);
        download();
//        if (!isFinishing()) {
//            progressBar.show();
//            nativeThread.start();
//        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntArray(KEY_COUNTRY_SCROLL_VIEW_STATE,
                new int[]{ scrollView.getScrollX(), scrollView.getScrollY()});
//        listCountryState = lmCountry.onSaveInstanceState();
//        outState.putParcelable(KEY_COUNTRY_VIEW_STATE, listCountryState); // get current recycle view position here.
        listCardStart = lmCard.onSaveInstanceState();
        outState.putParcelable(KEY_CARD_VIEW_STATE, listCardStart); // get current recycle view position here.
        outState.putBoolean(KEY_VIEW_STATE, rvCards.getVisibility() == View.VISIBLE); // get current recycle view position here.
        outState.putInt(KEY_POSITION_STATE, selectedPosition); // get current recycle view position here.
    }

    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
//        // Retrieve list state and list/item positions
        if (state != null) {
            position = state.getIntArray(KEY_COUNTRY_SCROLL_VIEW_STATE);
//            listCountryState = state.getParcelable(KEY_COUNTRY_VIEW_STATE);
            listCardStart = state.getParcelable(KEY_CARD_VIEW_STATE);
            isCardViewVisible = state.getBoolean(KEY_VIEW_STATE);
            selectedPosition = state.getInt(KEY_POSITION_STATE);
        }
    }

    protected void restoreInstantState() {
        if (contryList != null && contryList.size() > 0) {
//            if (listCountryState != null) {
//                lmCountry.onRestoreInstanceState(listCountryState);
//            }
            if(position != null)
                scrollView.post(new Runnable() {
                    public void run() {
                        scrollView.scrollTo(position[0], position[1]);
                    }
                });
            if (isCardViewVisible && listCardStart != null) {
                updateCardLayout((ContryModel) contryList.get(selectedPosition));
                lmCard.onRestoreInstanceState(listCardStart);
            }

        }
    }

    public class CardListAdpter extends RecyclerView.Adapter {

        private final Context context;
        private final List<Object> modelList;

        public CardListAdpter(Context context, List<Object> modelList) {
            this.context = context;
            this.modelList = modelList;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false));

        }

        @Override
        public int getItemViewType(int position) {
            if (this.modelList.get(position) instanceof ContryModel) {
                return 0;
            } else
                return 1;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, final int pos) {
            int position = viewHolder.getAdapterPosition();
            Holder holder = (Holder) viewHolder;
            if (this.modelList.get(position) instanceof ContryModel) {
                final ContryModel contryModel = (ContryModel) this.modelList.get(position);
                holder.txt_card_name.setText(contryModel.getCountry_name());
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectedPosition = position;
                        updateCardLayout(contryModel);
                    }
                });
            } else if (this.modelList.get(position) instanceof ContryModel.CardModel) {
                final ContryModel.CardModel cardModel = (ContryModel.CardModel) this.modelList.get(position);
                holder.txt_card_name.setText(cardModel.getCard_name());
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if ((isStaticOCR == 1 && (cardModel.getCard_type() == 1 || cardModel.getCard_type() == 2)) || isStaticOCR == 0) {
                            Intent intent = new Intent(CardListAdpter.this.context, OcrActivity.class);
                            intent.putExtra("country_id", ((ContryModel) MainActivity.this.contryList.get(selectedPosition)).getCountry_id());
                            intent.putExtra("card_id", cardModel.getCard_id());
                            intent.putExtra("card_name", cardModel.getCard_name());
                            if (cardModel.getCard_type() == 1) {
                                RecogType.PDF417.attachTo(intent);
                            } else if (cardModel.getCard_type() == 2) {
                                RecogType.DL_PLATE.attachTo(intent);
                            } else {
                                RecogType.OCR.attachTo(intent);
                            }
                            intent.putExtra("app_orientation", getRequestedOrientation());
                            startActivity(intent);
                            overridePendingTransition(0, 0);
                        } else {
                            _cardModel = cardModel;
                            Intent intent = new Intent();
                            intent.setType("*/*");
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            startActivityForResult(Intent.createChooser(intent, ""), PICK_IMAGE);
                        }
                    }
                });
            }

        }

        @Override
        public int getItemCount() {
            return this.modelList.size();
        }

        public class Holder extends RecyclerView.ViewHolder {
            TextView txt_card_name;

            public Holder(@NonNull View itemView) {
                super(itemView);
                txt_card_name = itemView.findViewById(R.id.tv_title);
            }
        }
    }

    private void updateCardLayout(ContryModel model) {
        MainActivity.this.cardList.clear();
        MainActivity.this.cardList.addAll(model.getCards());
        MainActivity.this.cardAdapter.notifyDataSetChanged();
        MainActivity.this.lout_country.setVisibility(View.INVISIBLE);
        MainActivity.this.rvCards.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (MainActivity.this.rvCards.getVisibility() == View.INVISIBLE) {
            super.onBackPressed();
        } else {
            selectedPosition = -1;
            MainActivity.this.lout_country.setVisibility(View.VISIBLE);
            MainActivity.this.rvCards.setVisibility(View.INVISIBLE);
        }
    }
}
