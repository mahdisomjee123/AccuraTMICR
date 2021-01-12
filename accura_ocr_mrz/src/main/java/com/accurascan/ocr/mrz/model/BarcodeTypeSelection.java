package com.accurascan.ocr.mrz.model;

import com.google.android.gms.vision.barcode.Barcode;

import java.util.ArrayList;
import java.util.List;

//@Keep // 20210111 remove barcode
public class BarcodeTypeSelection {
    public String barcodeTitle;
    public boolean isSelected;
    public int formatsType;
    public static List<BarcodeTypeSelection> CODE_NAMES = new ArrayList<>();

    public BarcodeTypeSelection(String barcodeTitle, boolean isSelected, int formatsType) {
        this.barcodeTitle = barcodeTitle;
        this.isSelected = isSelected;
        this.formatsType = formatsType;
    }

    static {
        CODE_NAMES.add(new BarcodeTypeSelection("ALL_FORMATS", true, Barcode.ALL_FORMATS));
        CODE_NAMES.add(new BarcodeTypeSelection("QR CODE", false, Barcode.QR_CODE));
        CODE_NAMES.add(new BarcodeTypeSelection("PDF417", false, Barcode.PDF417));
        CODE_NAMES.add(new BarcodeTypeSelection("DATA_MATRIX", false, Barcode.DATA_MATRIX));
        CODE_NAMES.add(new BarcodeTypeSelection("CODABAR", false, Barcode.CODABAR));
        CODE_NAMES.add(new BarcodeTypeSelection("CODE_39", false, Barcode.CODE_39));
        CODE_NAMES.add(new BarcodeTypeSelection("CODE_93", false, Barcode.CODE_93));
        CODE_NAMES.add(new BarcodeTypeSelection("CODE_128", false, Barcode.CODE_128));
        CODE_NAMES.add(new BarcodeTypeSelection("AZTEC", false, Barcode.AZTEC));
        CODE_NAMES.add(new BarcodeTypeSelection("CALENDAR_EVENT", false, Barcode.CALENDAR_EVENT));
        CODE_NAMES.add(new BarcodeTypeSelection("EAN_8", false, Barcode.EAN_8));
        CODE_NAMES.add(new BarcodeTypeSelection("EAN_13", false, Barcode.EAN_13));
        CODE_NAMES.add(new BarcodeTypeSelection("EMAIL", false, Barcode.EMAIL));
        CODE_NAMES.add(new BarcodeTypeSelection("PHONE", false, Barcode.PHONE));
        CODE_NAMES.add(new BarcodeTypeSelection("CONTACT_INFO", false, Barcode.CONTACT_INFO));
        CODE_NAMES.add(new BarcodeTypeSelection("GEO", false, Barcode.GEO));
        CODE_NAMES.add(new BarcodeTypeSelection("SMS", false, Barcode.SMS));
        CODE_NAMES.add(new BarcodeTypeSelection("DRIVER_LICENSE", false, Barcode.DRIVER_LICENSE));
        CODE_NAMES.add(new BarcodeTypeSelection("URL", false, Barcode.URL));
        CODE_NAMES.add(new BarcodeTypeSelection("ISBN", false, Barcode.ISBN));
        CODE_NAMES.add(new BarcodeTypeSelection("WIFI", false, Barcode.WIFI));
        CODE_NAMES.add(new BarcodeTypeSelection("TEXT", false, Barcode.TEXT));
        CODE_NAMES.add(new BarcodeTypeSelection("UPC_A", false, Barcode.UPC_A));
        CODE_NAMES.add(new BarcodeTypeSelection("UPC_E", false, Barcode.UPC_E));
        CODE_NAMES.add(new BarcodeTypeSelection("ITF", false, Barcode.ITF));
        CODE_NAMES.add(new BarcodeTypeSelection("PRODUCT", false, Barcode.PRODUCT));
    }
}
