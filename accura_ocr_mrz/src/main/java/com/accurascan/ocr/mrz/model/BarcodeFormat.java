package com.accurascan.ocr.mrz.model;

import androidx.annotation.Keep;

import com.google.android.gms.vision.barcode.Barcode;

import java.util.ArrayList;
import java.util.List;

@Keep
public class BarcodeFormat {
    public String barcodeTitle;
    public boolean isSelected;
    public int formatsType;
    public static final int BARCODE_ALL_FORMATS = Barcode.ALL_FORMATS;
    public static final int BARCODE_CODE_128 = Barcode.CODE_128;
    public static final int BARCODE_CODE_39 = Barcode.CODE_39;
    public static final int BARCODE_CODE_93 = Barcode.CODE_93;
    public static final int BARCODE_CODABAR = Barcode.CODABAR;
    public static final int BARCODE_DATA_MATRIX = Barcode.DATA_MATRIX;
    public static final int BARCODE_EAN_13 = Barcode.EAN_13;
    public static final int BARCODE_EAN_8 = Barcode.EAN_8;
    public static final int BARCODE_ITF = Barcode.ITF;
    public static final int BARCODE_QR_CODE = Barcode.QR_CODE;
    public static final int BARCODE_UPC_A = Barcode.UPC_A;
    public static final int BARCODE_UPC_E = Barcode.UPC_E;
    public static final int BARCODE_PDF417 = Barcode.PDF417;
    public static final int BARCODE_AZTEC = Barcode.AZTEC;

    public BarcodeFormat(String barcodeTitle, boolean isSelected, int formatsType) {
        this.barcodeTitle = barcodeTitle;
        this.isSelected = isSelected;
        this.formatsType = formatsType;
    }

    public static List<BarcodeFormat> getList() {
        List<BarcodeFormat> CODE_NAMES = new ArrayList<>();
        CODE_NAMES.add(new BarcodeFormat("ALL FORMATS", true, Barcode.ALL_FORMATS));
        CODE_NAMES.add(new BarcodeFormat("AZTEC", false, Barcode.AZTEC));
        CODE_NAMES.add(new BarcodeFormat("CODABAR", false, Barcode.CODABAR));
        CODE_NAMES.add(new BarcodeFormat("CODE 39", false, Barcode.CODE_39));
        CODE_NAMES.add(new BarcodeFormat("CODE 93", false, Barcode.CODE_93));
        CODE_NAMES.add(new BarcodeFormat("CODE 128", false, Barcode.CODE_128));
        CODE_NAMES.add(new BarcodeFormat("DATA MATRIX", false, Barcode.DATA_MATRIX));
        CODE_NAMES.add(new BarcodeFormat("EAN 8", false, Barcode.EAN_8));
        CODE_NAMES.add(new BarcodeFormat("EAN 13", false, Barcode.EAN_13));
        CODE_NAMES.add(new BarcodeFormat("ITF", false, Barcode.ITF));
        CODE_NAMES.add(new BarcodeFormat("PDF417", false, Barcode.PDF417));
        CODE_NAMES.add(new BarcodeFormat("QR CODE", false, Barcode.QR_CODE));
        CODE_NAMES.add(new BarcodeFormat("UPC A", false, Barcode.UPC_A));
        CODE_NAMES.add(new BarcodeFormat("UPC E", false, Barcode.UPC_E));
        return CODE_NAMES;
    }
}
