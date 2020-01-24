package com.accurascan.accuraocr.sample;

import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.docrecog.scan.OcrCallback;
import com.docrecog.scan.OcrData;
import com.docrecog.scan.OcrFragment;
import com.docrecog.scan.RecogResult;


public class OcrActivityCustom extends AppCompatActivity implements OcrCallback {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.ocr_activity);
        OcrFragment fragment = new OcrFragment();
        Bundle args = new Bundle();
        args.putString("countryname", "Malaysia");
        args.putInt("card_code", 36);
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction().replace(R.id.ocr_root, fragment).commit();
    }


    @Override
    public void onScannedSuccess(OcrData data, RecogResult result) {
        if (data != null && result != null) {
            Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
    }
}