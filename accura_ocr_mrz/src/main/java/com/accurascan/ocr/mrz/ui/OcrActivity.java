package com.accurascan.ocr.mrz.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;

import com.accurascan.ocr.mrz.R;
import com.docrecog.scan.OcrData;
import com.docrecog.scan.OcrFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;


public class OcrActivity extends AppCompatActivity {

    private OcrFragment fragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.ocr_activity);
        fragment = new OcrFragment();
        Bundle args = new Bundle();
        if (getIntent().hasExtra("card_code")) {
            args.putInt("card_code", getIntent().getIntExtra("card_code", 0));
            args.putString("countryname", getIntent().getStringExtra("countryname"));
        }
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction().replace(R.id.ocr_root, fragment).commit();

    }


    // implement method for custom result Activity
//    @Override
//    public void onScannedSuccess(OcrData data, RecogResult result) {
//        if (data != null && result != null) {
//            Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
//        } else
//            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
//    }

}