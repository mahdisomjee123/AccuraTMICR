package com.accurascan.accuraocr.sample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.accurascan.ocr.mrz.ui.OcrActivity;
import com.docrecog.scan.OcrData;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void AccuraOcrCustom(View view) {
        Intent intent = new Intent(this, OcrActivityCustom.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    public void AccuraOcr(View view) {
        Intent intent = new Intent(this, OcrActivity.class);
        switch (view.getId()) {
            case R.id.button_ocr_UAE:
                intent.putExtra("card_code", 41);
                intent.putExtra("countryname", "UAE");
                break;
            case R.id.button_ocr_PAN:
                intent.putExtra("countryname", "India");
                intent.putExtra("card_code", 27);
                break;
            case R.id.button_ocr_Aadhar:
                intent.putExtra("countryname", "India");
                intent.putExtra("card_code", 28);
                break;
            case R.id.button_ocr_Malaysia_id:
                intent.putExtra("countryname", "Malaysia");
                intent.putExtra("card_code", 36);
                break;

        }
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
}
