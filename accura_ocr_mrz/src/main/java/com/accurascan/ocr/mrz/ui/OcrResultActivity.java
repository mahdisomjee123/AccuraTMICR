package com.accurascan.ocr.mrz.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.accurascan.ocr.mrz.R;
import com.docrecog.scan.OcrData;
import com.docrecog.scan.RecogEngine;
import com.docrecog.scan.RecogResult;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OcrResultActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "OcrResultActivity";
    Bitmap face1;
    String mrzdata = "";

    private ImageView ivUserProfile;
    private TextView  tvCancel1;

    //custom
    ImageView iv_holder_image;
    RecyclerView ry_cardresult;
    TableLayout mrz_table_layout, front_table_layout, back_table_layout, security_table_layout, barcode_table_layout;
    OcrData application;
    ImageView iv_frontside, iv_backside;
    LinearLayout ly_back, ly_front;
    String type;
    private static RecogEngine mCardScanner;
    View ly_mrz_container, ly_front_container, ly_back_container, ly_security_container, ly_barcode_container;
    OcrData.MapData Frontdata;
    OcrData.MapData Backdata;
    int isdonecount = 0;


    // convert from bitmap to byte array
    public static byte[] getBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        return stream.toByteArray();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_result);
        application = OcrData.getOcrResult();


        if (mCardScanner == null) {
            mCardScanner = new RecogEngine();
            mCardScanner.initEngine(this);
        }
        initUI();
        getDocumentdata();
    }

    private void getDocumentdata() {

        Frontdata = application.getFrontData();
        Backdata = application.getBackData();
        if (Frontdata != null && Backdata != null) {
            ly_front_container.setVisibility(View.VISIBLE);
            ly_back_container.setVisibility(View.VISIBLE);
            View layoutt = (View) LayoutInflater.from(this).inflate(R.layout.table_row, null);
            TextView tv_keyy = layoutt.findViewById(R.id.tv_key);
            TextView tv_valuey = layoutt.findViewById(R.id.tv_value);
            tv_keyy.setTextSize(16);
            tv_keyy.setTextColor(getResources().getColor(R.color.black));
            tv_keyy.setText("Country");
            tv_valuey.setText(application.getCountryname());
            front_table_layout.addView(layoutt);

            View layout2 = (View) LayoutInflater.from(this).inflate(R.layout.table_row, null);
            TextView tv_key2 = layout2.findViewById(R.id.tv_key);
            TextView tv_value2 = layout2.findViewById(R.id.tv_value);
            tv_key2.setTextSize(16);
            tv_key2.setTextColor(getResources().getColor(R.color.black));
            tv_key2.setText("Card Type");
            tv_value2.setText(application.getCardname());
            type = tv_value2.getText().toString();
            front_table_layout.addView(layout2);


        } else if (Frontdata != null) {
            ly_front_container.setVisibility(View.VISIBLE);

            View layoutt = (View) LayoutInflater.from(this).inflate(R.layout.table_row, null);
            TextView tv_keyy = layoutt.findViewById(R.id.tv_key);
            TextView tv_valuey = layoutt.findViewById(R.id.tv_value);
            tv_keyy.setTextSize(16);
            tv_keyy.setTextColor(getResources().getColor(R.color.black));
            tv_keyy.setText("Country");
            tv_valuey.setText(application.getCountryname());
            front_table_layout.addView(layoutt);

            View layout2 = (View) LayoutInflater.from(this).inflate(R.layout.table_row, null);
            TextView tv_key2 = layout2.findViewById(R.id.tv_key);
            TextView tv_value2 = layout2.findViewById(R.id.tv_value);
            tv_key2.setTextSize(16);
            tv_key2.setTextColor(getResources().getColor(R.color.black));
            tv_key2.setText("Card Type");
            // get card name
            tv_value2.setText(application.getCardname());
            type = tv_value2.getText().toString();
            front_table_layout.addView(layout2);


        } else if (Backdata != null) {
            ly_back_container.setVisibility(View.VISIBLE);
            View layoutt = (View) LayoutInflater.from(this).inflate(R.layout.table_row, null);
            TextView tv_keyy = layoutt.findViewById(R.id.tv_key);
            TextView tv_valuey = layoutt.findViewById(R.id.tv_value);
            tv_keyy.setTextSize(16);
            tv_keyy.setTextColor(getResources().getColor(R.color.black));
            tv_keyy.setText("Country");
            tv_valuey.setText(application.getCountryname());
            back_table_layout.addView(layoutt);
            View layout2 = (View) LayoutInflater.from(this).inflate(R.layout.table_row, null);
            TextView tv_key2 = layout2.findViewById(R.id.tv_key);
            TextView tv_value2 = layout2.findViewById(R.id.tv_value);
            tv_key2.setTextSize(16);
            tv_key2.setTextColor(getResources().getColor(R.color.black));
            tv_key2.setText("Card Type");
            // get card name
            tv_value2.setText(application.getCardname());
            type = tv_value2.getText().toString();
            back_table_layout.addView(layout2);


        }
        if (Build.VERSION.SDK_INT >= 11) {
            //--post GB use serial executor by default --
            new SetUpAllFrontData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[]{});
        } else {
            //--GB uses ThreadPoolExecutor by default--
            new SetUpAllFrontData().execute(new Void[]{});
        }
        if (Build.VERSION.SDK_INT >= 11) {
            //--post GB use serial executor by default --
            new SetUpAllBackData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[]{});
        } else {
            //--GB uses ThreadPoolExecutor by default--
            new SetUpAllBackData().execute(new Void[]{});
        }

//        setData(); //set the result data
    }

    public class SetUpAllFrontData extends AsyncTask<Void, Void, String> {


        public SetUpAllFrontData() {

        }

        @Override
        protected void onPreExecute() {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    progress_container.setVisibility(View.GONE);
//
//                }
//            });

            super.onPreExecute();
//            showProgressDialog();

        }

        @Override
        protected String doInBackground(Void... params) {
            if (Frontdata != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ly_front_container.setVisibility(View.VISIBLE);

                    }
                });
                final Bitmap frontBitmap = application.getFrontimage();

                for (int i = 0; i < Frontdata.getOcr_data().size(); i++) {

//                TableRow tr =  new TableRow(this);
                    final OcrData.MapData.ScannedData array = Frontdata.getOcr_data().get(i);

                    Runnable runnable = new Runnable() {
                        public void run() {
                            if (array != null) {
                                final int data_type = array.getType();
                                final String key = array.getKey();
                                //                key.replace("img11", "");
                                final String value = array.getKey_data();
                                final View layout = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
                                //            TableRow tableRow = layout.findViewById(R.id.table_row);
                                final TextView tv_key = layout.findViewById(R.id.tv_key);
                                final TextView tv_value = layout.findViewById(R.id.tv_value);
                                ImageView imageView = layout.findViewById(R.id.iv_image);

                                tv_key.setTextSize(16);
                                tv_key.setTextColor(getResources().getColor(R.color.black));
                                if (data_type == 1) {
                                    if (!key.toLowerCase().contains("mrz")) {
                                        if (!value.equalsIgnoreCase("") && !value.equalsIgnoreCase(" ")) {
                                            tv_key.setText(key + ":");
//                                            if (!value.contains(".png")) {
                                            tv_value.setText(value);
                                            imageView.setVisibility(View.GONE);
                                            front_table_layout.addView(layout);
//                                            }
                                        }
                                    } else if (key.toLowerCase().contains("mrz")) {
                                        setMRZData();
                                    }
                                } else if (data_type == 2) {
                                    // get rect for image
                                    if (!value.equalsIgnoreCase("") && !value.equalsIgnoreCase(" ")) {
                                        try {
                                            if (key.toLowerCase().contains("face")) {
                                                try {
                                                    face1 = application.getFaceBitmap();
                                                    if (face1 == null) {
                                                        face1 = RecogEngine.g_recogResult.faceBitmap;
                                                    }

                                                    if (face1 == null) {
                                                        RecogResult result = new RecogResult();
                                                        int faceret = mCardScanner.doRunFaceDetect(frontBitmap, result);
                                                        if (result.faceBitmap != null) {
                                                            face1 = result.faceBitmap;
                                                        }
                                                    }
                                                    if (face1 == null) {
//                                                        File imgFile = new File(value);
//                                                        if (imgFile.exists()) {
                                                        face1 = array.getImage();
//                                                        }
                                                    }

                                                } catch (Exception e) {
                                                    //                                                        face1 = bitmap;
                                                    e.printStackTrace();
                                                }
                                            } else {
                                                tv_key.setText(key + ":");
//                                                File imgFile = new File(value);
//                                                if (imgFile.exists()) {
                                                Bitmap myBitmap = array.getImage();
                                                if (myBitmap != null) {
                                                    imageView.setImageBitmap(myBitmap);
                                                    tv_value.setVisibility(View.GONE);
                                                    front_table_layout.addView(layout);
                                                }
//                                                }
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                tv_value.setText(value);
                                                imageView.setVisibility(View.GONE);
                                                front_table_layout.addView(layout);
                                            }
                                        });


                                    }
                                } else if (data_type == 3) {
                                    ly_security_container.setVisibility(View.VISIBLE);
                                    View layout11 = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row_primery, null);
                                    ImageView iv_primory = layout11.findViewById(R.id.iv_primory);
                                    ImageView check_primory = layout11.findViewById(R.id.check_primory);
//                                    File file = new File(key);
                                    Bitmap bitmap = array.getImage();
                                    boolean ithas = Boolean.valueOf(value);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            iv_primory.setImageBitmap(bitmap);
                                            if (ithas) {
                                                System.out.println("CheckAvaibiltyofPromorydata Success");
                                                check_primory.setImageDrawable(getResources().getDrawable(R.drawable.tick));
                                            } else {
                                                System.out.println("CheckAvaibiltyofPromorydata false");
                                                check_primory.setImageDrawable(getResources().getDrawable(R.drawable.close));
                                            }
                                            security_table_layout.addView(layout11);

                                        }
                                    });


                                }
                            }
                        }
                    };
                    runOnUiThread(runnable);
                    if (frontBitmap != null && !frontBitmap.isRecycled()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                iv_frontside.setImageBitmap(frontBitmap);

                            }
                        });
                    }

                }
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ly_front.setVisibility(View.GONE);

                    }
                });
            }


            return "successful";
        }

        @Override
        protected void onPostExecute(String s) {
            isdonecount++;
            if (isdonecount == 2) {
                setData(); //set the result data
            }

            super.onPostExecute(s);

        }
    }

    private void setMRZData() {
        ly_mrz_container.setVisibility(View.VISIBLE);
        //get data from mrz
        int ret = 0;
        int faceret = 0; // detecting face return value
        Bitmap bmCard;
        int mRecCnt = 0;
        int mDisplayRotation = 0;
        if (!TextUtils.isEmpty(RecogEngine.g_recogResult.lines)) {
            View layout1 = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
            TextView tv_key1 = layout1.findViewById(R.id.tv_key);
            TextView tv_value1 = layout1.findViewById(R.id.tv_value);
            tv_key1.setText("MRZ" + ":");
            tv_value1.setText(RecogEngine.g_recogResult.lines);
            mrz_table_layout.addView(layout1);

        }
        if (!TextUtils.isEmpty(RecogEngine.g_recogResult.docType)) {
            View layout5 = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
            TextView tv_key5 = layout5.findViewById(R.id.tv_key);
            TextView tv_value5 = layout5.findViewById(R.id.tv_value);
            tv_key5.setText("DOC TYPE" + ":");
            String str = "Document" + ":" + RecogEngine.g_recogResult.docType + "<br/>";
            mrzdata = mrzdata + str;
            tv_value5.setText(RecogEngine.g_recogResult.docType);
            mrz_table_layout.addView(layout5);

        }

        if (!TextUtils.isEmpty(RecogEngine.g_recogResult.surname)) {
            View layout1 = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
            TextView tv_key1 = layout1.findViewById(R.id.tv_key);
            TextView tv_value1 = layout1.findViewById(R.id.tv_value);
            tv_key1.setText("Last Name" + ":");
            String str = "LAST NAME" + ":" + RecogEngine.g_recogResult.surname + "<br/>";
            mrzdata = mrzdata + str;

            tv_value1.setText(RecogEngine.g_recogResult.surname);
            mrz_table_layout.addView(layout1);

        }

        if (!TextUtils.isEmpty(RecogEngine.g_recogResult.givenname)) {
            View layout3 = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
            TextView tv_key3 = layout3.findViewById(R.id.tv_key);
            TextView tv_value3 = layout3.findViewById(R.id.tv_value);
            tv_key3.setText("FIRST NAME" + ":");
            String str = "First Name" + ":" + RecogEngine.g_recogResult.givenname + "<br/>";
            mrzdata = mrzdata + str;
            tv_value3.setText(RecogEngine.g_recogResult.givenname);
            mrz_table_layout.addView(layout3);

        }
        if (!TextUtils.isEmpty(RecogEngine.g_recogResult.docnumber)) {
            View layout5 = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
            TextView tv_key5 = layout5.findViewById(R.id.tv_key);
            TextView tv_value5 = layout5.findViewById(R.id.tv_value);
            tv_key5.setText("DOC NUMBER" + ":");
            String str = "Document No" + ":" + RecogEngine.g_recogResult.docnumber + "<br/>";
            mrzdata = mrzdata + str;
            tv_value5.setText(RecogEngine.g_recogResult.docnumber);
            mrz_table_layout.addView(layout5);

        }
        if (!TextUtils.isEmpty(RecogEngine.g_recogResult.docchecksum)) {
            View layout5 = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
            TextView tv_key5 = layout5.findViewById(R.id.tv_key);
            TextView tv_value5 = layout5.findViewById(R.id.tv_value);
            tv_key5.setText("DOC CHECKSUM" + ":");
            String str = "Document Check Number" + ":" + RecogEngine.g_recogResult.docchecksum + "<br/>";
            mrzdata = mrzdata + str;
            tv_value5.setText(RecogEngine.g_recogResult.docchecksum);
            mrz_table_layout.addView(layout5);

        }

        if (!TextUtils.isEmpty(RecogEngine.g_recogResult.country)) {
            View layout4 = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
            TextView tv_key4 = layout4.findViewById(R.id.tv_key);
            TextView tv_value4 = layout4.findViewById(R.id.tv_value);
            tv_key4.setText("COUNTRY" + ":");
            String str = "Country" + ":" + RecogEngine.g_recogResult.country + "<br/>";
            mrzdata = mrzdata + str;
            tv_value4.setText(RecogEngine.g_recogResult.country);
            //                            scanData.setCountry(RecogEngine.g_recogResult.country);
            mrz_table_layout.addView(layout4);

        }

        if (!TextUtils.isEmpty(RecogEngine.g_recogResult.nationality)) {
            View layout5 = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
            TextView tv_key5 = layout5.findViewById(R.id.tv_key);
            TextView tv_value5 = layout5.findViewById(R.id.tv_value);
            tv_key5.setText("NATIONALITY" + ":");
            String str = "Nationality" + ":" + RecogEngine.g_recogResult.nationality + "<br/>";
            mrzdata = mrzdata + str;
            tv_value5.setText(RecogEngine.g_recogResult.nationality);
            mrz_table_layout.addView(layout5);

        }
        if (!TextUtils.isEmpty(RecogEngine.g_recogResult.sex)) {
            View layout6 = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
            TextView tv_key6 = layout6.findViewById(R.id.tv_key);
            TextView tv_value6 = layout6.findViewById(R.id.tv_value);
            if (RecogEngine.g_recogResult.sex.equalsIgnoreCase("F")) {
                tv_key6.setText("SEX" + ":");
                String str = "Sex" + ":" + getString(R.string.text_female) + "<br/>";
                mrzdata = mrzdata + str;
                tv_value6.setText(getString(R.string.text_female));
                //                                scanData.setGender(getString(R.string.text_female));
                mrz_table_layout.addView(layout6);

            } else {
                tv_key6.setText("SEX" + ":");

                tv_value6.setText(getString(R.string.text_male));
                String str = "SEX" + ":" + getString(R.string.text_male) + "<br/>";
                mrzdata = mrzdata + str;
                //                                scanData.setGender(getString(R.string.text_male));
                mrz_table_layout.addView(layout6);

            }
        }

        DateFormat date = new SimpleDateFormat("yymmdd", Locale.getDefault());
        SimpleDateFormat newDateFormat = new SimpleDateFormat("dd-mm-yy", Locale.getDefault());
        if (!TextUtils.isEmpty(RecogEngine.g_recogResult.birth)) {
            try {
                View layout7 = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
                TextView tv_key7 = layout7.findViewById(R.id.tv_key);
                TextView tv_value7 = layout7.findViewById(R.id.tv_value);
                tv_key7.setText("DATE OF BIRTH" + ":");

                Date birthDate = date.parse(RecogEngine.g_recogResult.birth.replace("<", ""));
                tv_value7.setText(newDateFormat.format(birthDate));
                String str = "Date of Birth" + ":" + newDateFormat.format(birthDate) + "<br/>";
                mrzdata = mrzdata + str;
                //                                scanData.setDateOfBirth(newDateFormat.format(birthDate));
                mrz_table_layout.addView(layout7);

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (!TextUtils.isEmpty(RecogEngine.g_recogResult.birthchecksum)) {
            View layout5 = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
            TextView tv_key5 = layout5.findViewById(R.id.tv_key);
            TextView tv_value5 = layout5.findViewById(R.id.tv_value);
            tv_key5.setText("BIRTH CHECKSUM" + ":");
            String str = "Birth Check Number" + ":" + RecogEngine.g_recogResult.birthchecksum + "<br/>";
            mrzdata = mrzdata + str;
            tv_value5.setText(RecogEngine.g_recogResult.birthchecksum);
            mrz_table_layout.addView(layout5);

        }
        if (!TextUtils.isEmpty(RecogEngine.g_recogResult.expirationchecksum)) {
            View layout5 = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
            TextView tv_key5 = layout5.findViewById(R.id.tv_key);
            TextView tv_value5 = layout5.findViewById(R.id.tv_value);
            tv_key5.setText("EXPIRATION CHECKSUM" + ":");
            String str = "Expiration Check Number" + ":" + RecogEngine.g_recogResult.expirationchecksum + "<br/>";
            mrzdata = mrzdata + str;
            tv_value5.setText(RecogEngine.g_recogResult.expirationchecksum);
            mrz_table_layout.addView(layout5);

        }
        if (!TextUtils.isEmpty(RecogEngine.g_recogResult.expirationdate)) {
            try {
                View layout8 = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
                TextView tv_key8 = layout8.findViewById(R.id.tv_key);
                TextView tv_value8 = layout8.findViewById(R.id.tv_value);
                tv_key8.setText("EXPIRY DATE" + ":");

                Date expiryDate = date.parse(RecogEngine.g_recogResult.expirationdate);
                tv_value8.setText(newDateFormat.format(expiryDate));
                String str = "Date Of Expiry" + ":" + newDateFormat.format(expiryDate) + "<br/>";
                mrzdata = mrzdata + str;
                //                                scanData.setDateOfExpiry(newDateFormat.format(expiryDate));
                mrz_table_layout.addView(layout8);

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        if (!TextUtils.isEmpty(RecogEngine.g_recogResult.otherid)) {
            View layout5 = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
            TextView tv_key5 = layout5.findViewById(R.id.tv_key);
            TextView tv_value5 = layout5.findViewById(R.id.tv_value);
            tv_key5.setText("OTHER ID" + ":");
            String str = "OTHER ID" + ":" + RecogEngine.g_recogResult.otherid + "<br/>";
            mrzdata = mrzdata + str;
            tv_value5.setText(RecogEngine.g_recogResult.otherid);
            mrz_table_layout.addView(layout5);

        }

        if (!TextUtils.isEmpty(RecogEngine.g_recogResult.otheridchecksum)) {
            View layout5 = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
            TextView tv_key5 = layout5.findViewById(R.id.tv_key);
            TextView tv_value5 = layout5.findViewById(R.id.tv_value);
            tv_key5.setText("OTHER ID CHECKSUM" + ":");
            String str = "Other ID Check" + ":" + RecogEngine.g_recogResult.otheridchecksum + "<br/>";
            mrzdata = mrzdata + str;
            tv_value5.setText(RecogEngine.g_recogResult.otheridchecksum);
            mrz_table_layout.addView(layout5);

        }

        if (!TextUtils.isEmpty(RecogEngine.g_recogResult.secondrowchecksum)) {
            View layout5 = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
            TextView tv_key5 = layout5.findViewById(R.id.tv_key);
            TextView tv_value5 = layout5.findViewById(R.id.tv_value);
            tv_key5.setText("SECOND ROW CHECKSUM" + ":");
            String str = "Second Row Check Number " + ":" + RecogEngine.g_recogResult.secondrowchecksum + "<br/>";
            mrzdata = mrzdata + str;
            tv_value5.setText(RecogEngine.g_recogResult.secondrowchecksum);
            mrz_table_layout.addView(layout5);

        }
        View layout6 = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
        TextView tv_key5 = layout6.findViewById(R.id.tv_key);
        TextView tv_value5 = layout6.findViewById(R.id.tv_value);
        tv_key5.setText("Result" + ":");

        if (RecogEngine.g_recogResult.ret == 0) {
            tv_value5.setText(getString(R.string.failed));
            String str = "Result " + ":" + getString(R.string.failed) + "<br/>";
            mrzdata = mrzdata + str;
        } else if (RecogEngine.g_recogResult.ret == 1 || RecogEngine.g_recogResult.ret == 2) {
            tv_value5.setText(getString(R.string.correct_mrz));
            String str = "Result" + ":" + getString(R.string.correct_mrz) + "<br/>";
            mrzdata = mrzdata + str;
        } else if (RecogEngine.g_recogResult.ret == 0) {
            tv_value5.setText(getString(R.string.incorrect_mrz));
            String str = "Result " + ":" + getString(R.string.incorrect_mrz) + "<br/>";
            mrzdata = mrzdata + str;
        }
        mrz_table_layout.addView(layout6);
    }

    public class SetUpAllBackData extends AsyncTask<Void, Void, String> {

        public SetUpAllBackData() {

        }

        @Override
        protected void onPreExecute() {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    progress_container.setVisibility(View.GONE);
//
//                }
//            });

            super.onPreExecute();
//            showProgressDialog();

        }

        @Override
        protected String doInBackground(Void... params) {
            if (Backdata != null) {
                final Bitmap BackImage = application.getBackimage();

                Runnable runnable = new Runnable() {
                    public void run() {
                        for (int i = 0; i < Backdata.getOcr_data().size(); i++) {
                            View layout = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row, null);
                            //            TableRow tableRow = layout.findViewById(R.id.table_row);
                            TextView tv_key = layout.findViewById(R.id.tv_key);
                            TextView tv_value = layout.findViewById(R.id.tv_value);
                            ImageView imageView = layout.findViewById(R.id.iv_image);
                            OcrData.MapData.ScannedData array = Backdata.getOcr_data().get(i);

                            if (array != null) {
                                //                    back_table_layout.setVisibility(View.VISIBLE);
                                int data_type = array.getType();
                                String key = array.getKey();
                                //if element is image then key = key+"img11" so remove it
                                String value = array.getKey_data();
                                if (data_type == 1) {
                                    if (!key.equalsIgnoreCase("mrz")) {
                                        if (!value.equalsIgnoreCase("") && !value.equalsIgnoreCase(" ")) {
                                            tv_key.setTextSize(16);
                                            tv_key.setTextColor(getResources().getColor(R.color.black));
                                            tv_key.setText(key + ":");
//                                            if (!value.contains(".png")) {
                                            //                TextView tv_value =new TextView(this);
                                            tv_value.setText(value);
                                            imageView.setVisibility(View.GONE);
                                            back_table_layout.addView(layout);
//                                            }
                                        }
                                    } else {
                                        setMRZData();
                                    }
                                } else if (data_type == 2) {
                                    if (!value.equalsIgnoreCase("") && !value.equalsIgnoreCase(" ")) {
                                        try {
                                            tv_key.setText(key + ":");
                                            Bitmap myBitmap = array.getImage();
                                            if (myBitmap != null) {
                                                imageView.setImageBitmap(myBitmap);
                                                tv_value.setVisibility(View.GONE);
                                                back_table_layout.addView(layout);
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        tv_value.setText(value);
                                        imageView.setVisibility(View.GONE);
                                        back_table_layout.addView(layout);

                                    }
                                } else if (data_type == 3) {
                                    ly_security_container.setVisibility(View.VISIBLE);
                                    View layout11 = (View) LayoutInflater.from(OcrResultActivity.this).inflate(R.layout.table_row_primery, null);
                                    ImageView iv_primory = layout11.findViewById(R.id.iv_primory);
                                    ImageView check_primory = layout11.findViewById(R.id.check_primory);
//                                    File file = new File(key);
                                    Bitmap bitmap = array.getImage();
                                    boolean ithas = Boolean.valueOf(value);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            iv_primory.setImageBitmap(bitmap);
                                            if (ithas) {
                                                System.out.println("CheckAvaibiltyofPromorydata Success");
                                                check_primory.setImageDrawable(getResources().getDrawable(R.drawable.tick));
                                            } else {
                                                System.out.println("CheckAvaibiltyofPromorydata false");
                                                check_primory.setImageDrawable(getResources().getDrawable(R.drawable.close));
                                            }
                                            security_table_layout.addView(layout11);

                                        }
                                    });

                                }

                            }
                        }
                    }
                };
                runOnUiThread(runnable);
                if (BackImage != null && !BackImage.isRecycled()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            iv_backside.setImageBitmap(BackImage);

                        }
                    });
                }

            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ly_back.setVisibility(View.GONE);
                    }
                });
            }


            return "successful";
        }

        @Override
        protected void onPostExecute(String s) {
            isdonecount++;
            if (isdonecount == 2) {
                setData(); //set the result data
            }
            super.onPostExecute(s);

        }
    }

    private void initUI() {
        //initialize the UI
        ivUserProfile = findViewById(R.id.ivUserProfile);
        tvCancel1 = findViewById(R.id.tvCancel1);
        tvCancel1.setOnClickListener(this);

        ly_back = findViewById(R.id.ly_back);
        ly_front = findViewById(R.id.ly_front);
        iv_holder_image = findViewById(R.id.iv_holder_image);
        iv_frontside = findViewById(R.id.iv_frontside);
        iv_backside = findViewById(R.id.iv_backside);
        ry_cardresult = findViewById(R.id.ry_cardresult);
//        table_layout = findViewById(R.id.table_layout);
        mrz_table_layout = findViewById(R.id.mrz_table_layout);
        front_table_layout = findViewById(R.id.front_table_layout);
        back_table_layout = findViewById(R.id.back_table_layout);
        security_table_layout = findViewById(R.id.security_table_layout);
        barcode_table_layout = findViewById(R.id.barcode_table_layout);

        ly_mrz_container = findViewById(R.id.ly_mrz_container);
        ly_front_container = findViewById(R.id.ly_front_container);
        ly_back_container = findViewById(R.id.ly_back_container);
        ly_security_container = findViewById(R.id.ly_security_container);
        ly_barcode_container = findViewById(R.id.ly_barcode_container);


    }

    private void setData() {
        System.out.println("set data called");
        //set the result data
        if (face1 != null) {
            ivUserProfile.setImageBitmap(face1);
            ivUserProfile.setVisibility(View.VISIBLE);
        }
    }

    //handle click of different view
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.tvCancel1) {
            onBackPressed();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    //checked for user permission
    private boolean checkReadWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(OcrResultActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 111);
                return false;
            }
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 111) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                //resume tasks needing this permission
//                callEnroll();
            } else if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                //resume tasks needing this permission
                checkReadWritePermission();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Runtime.getRuntime().gc();
    }

    @Override
    public void onBackPressed() {

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        startActivity(new Intent(OcrResultActivity.this, OcrActivity.class));
////        startActivity(new Intent(OcrResultActivity.this, TestCameraActivity.class));
        finish();
//        super.onBackPressed();
    }

}
