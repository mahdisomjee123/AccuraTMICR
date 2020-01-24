package com.docrecog.scan;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Base64;

import java.util.ArrayList;
import java.util.List;

public class OcrData {
    OcrData ocrData;

    public OcrData getInstance(Activity cameraActivity) {
        if (ocrData == null) {
            ocrData = new OcrData();
        }
        return ocrData;
    }

    public enum OcrType {
        MENU_MODE_OCR, MENU_MODE_FACE, MENU_MODE_SCAN;

        public static String name = OcrType.class.getName();

        public void attachTo(Intent intent) {
            intent.putExtra(name, ordinal());
        }

        public static OcrType detachFrom(Intent intent) {
//            if (!intent.hasExtra(name)) throw new IllegalStateException();
            if (!intent.hasExtra(name)) MENU_MODE_OCR.attachTo(intent);
            return values()[intent.getIntExtra(name, -1)];
        }
    }

    public static OcrData ocrResult;

    public static OcrData getOcrResult() {
        OcrData ocrData = ocrResult;
        ocrResult = null;
        return ocrData;
    }

    public static void setOcrResult(OcrData ocrResult) {
        OcrData.ocrResult = ocrResult;
    }

    private Bitmap FaceBitmap;


    private int cardId;
    private int country_id;
    private String countryname;
    private String countryimage;
    private String cardname;

    private String backtemplete;
    private Bitmap CardBackimage;
    private Bitmap Backimage;
    private MapData BackData;

    private String fronttemplete;
    private Bitmap Frontimage;
    private Bitmap CardFrontimage;
    private MapData FrontData;

    public OcrData getOcrData() {
        return ocrData;
    }

    public void setOcrData(OcrData ocrData) {
        this.ocrData = ocrData;
    }

    public Bitmap getFaceBitmap() {
        return FaceBitmap;
    }

    public void setFaceBitmap(Bitmap faceBitmap) {
        FaceBitmap = faceBitmap;
    }

    public int getCardId() {
        return cardId;
    }

    public void setCardId(int cardId) {
        this.cardId = cardId;
    }

    public int getCountry_id() {
        return country_id;
    }

    public void setCountry_id(int country_id) {
        this.country_id = country_id;
    }

    public String getCountryname() {
        return countryname;
    }

    public void setCountryname(String countryname) {
        this.countryname = countryname;
    }

    public String getCountryimage() {
        return countryimage;
    }

    public void setCountryimage(String countryimage) {
        this.countryimage = countryimage;
    }

    public String getCardname() {
        return cardname;
    }

    public void setCardname(String cardname) {
        this.cardname = cardname;
    }

    public String getBacktemplete() {
        return backtemplete;
    }

    public void setBacktemplete(String backtemplete) {
        this.backtemplete = backtemplete;
    }

    public Bitmap getCardBackimage() {
        return CardBackimage;
    }

    public void setCardBackimage(Bitmap cardBackimage) {
        CardBackimage = cardBackimage;
    }

    public Bitmap getBackimage() {
        return Backimage;
    }

    public void setBackimage(Bitmap backimage) {
        Backimage = backimage;
    }

    public MapData getBackData() {
        return BackData;
    }

    public void setBackData(MapData backData) {
        BackData = backData;
    }

    public String getFronttemplete() {
        return fronttemplete;
    }

    public void setFronttemplete(String fronttemplete) {
        this.fronttemplete = fronttemplete;
    }

    public Bitmap getFrontimage() {
        return Frontimage;
    }

    public void setFrontimage(Bitmap frontimage) {
        Frontimage = frontimage;
    }

    public Bitmap getCardFrontimage() {
        return CardFrontimage;
    }

    public void setCardFrontimage(Bitmap cardFrontimage) {
        CardFrontimage = cardFrontimage;
    }

    public MapData getFrontData() {
        return FrontData;
    }

    public void setFrontData(MapData frontData) {
        FrontData = frontData;
    }

//    public float[] getFrontrefrence() {
//        return frontrefrence;
//    }
//
//    public void setFrontrefrence(float[] frontrefrence) {
//        this.frontrefrence = frontrefrence;
//    }


    public class MapData {

        public String card_side;
        public List<ScannedData> ocr_data;

        public String getCardSide() {
            return card_side;
        }

        public void setCardSide(String card_side) {
            this.card_side = card_side;
        }

        public List<ScannedData> getOcr_data() {
            return ocr_data;
        }

        public void setOcr_data(List<ScannedData> ocr_data) {
            this.ocr_data = ocr_data;
        }

        public class ScannedData {
            //  the value 'type' used the identify the data type
            // 1 - Text Code
            // 2 - Image code
            // 3 - Security code
            public int type;
            public String key;
            public String key_data;

            public int getType() {
                return type;
            }

            public void setType(int type) {
                this.type = type;
            }

            public String getKey() {
                return key;
            }

            public void setKey(String key) {
                this.key = key;
            }

            public String getKey_data() {
                return key_data;
            }

            public void setKey_data(String key_data) {
                this.key_data = key_data;
            }

            public Bitmap getImage() {
                if (type == 2 && !key_data.equals("") && !key_data.equals(" ")) {
                    byte[] decodedString = Base64.decode(key_data, Base64.DEFAULT);
                    return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                } else if (type == 3 && !key.equals("") && !key.equals(" ")) {
                    byte[] decodedString = Base64.decode(key, Base64.DEFAULT);
                    return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                }
                return null;
            }
        }
    }

}
