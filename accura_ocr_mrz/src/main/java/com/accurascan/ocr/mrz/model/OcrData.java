package com.accurascan.ocr.mrz.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import androidx.annotation.Keep;

import java.util.List;

@Keep
public class OcrData {

    private String cardname;
    private Bitmap faceImage;
    private Bitmap Backimage;
    private MapData BackData;
    private Bitmap Frontimage;
    private MapData FrontData;
    private RecogResult mrzData;

    public String getCardname() {
        return cardname;
    }

    public void setCardname(String cardname) {
        this.cardname = cardname;
    }

    public Bitmap getFaceImage() {
        return faceImage;
    }

    public void setFaceImage(Bitmap faceImage) {
        this.faceImage = faceImage;
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

    public Bitmap getFrontimage() {
        return Frontimage;
    }

    public void setFrontimage(Bitmap frontimage) {
        Frontimage = frontimage;
    }

    public MapData getFrontData() {
        return FrontData;
    }

    public void setFrontData(MapData frontData) {
        FrontData = frontData;
    }

    public RecogResult getMrzData() {
        return mrzData;
    }

    public void setMrzData(RecogResult mrzData) {
        this.mrzData = mrzData;
    }

    @Keep
    public class MapData {

        public String card_side;
        public int is_face;
        public List<ScannedData> ocr_data;

        public String getCardSide() {
            return card_side;
        }

        public void setCardSide(String card_side) {
            this.card_side = card_side;
        }

        public boolean getFace() {
            return is_face == 1;
        }

        public void setIsFace(int isFace) {
            this.is_face = isFace;
        }

        public List<ScannedData> getOcr_data() {
            return ocr_data;
        }

        public void setOcr_data(List<ScannedData> ocr_data) {
            this.ocr_data = ocr_data;
        }

        @Keep
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
                if (!key_data.equals("") && !key_data.equals(" ")) {
                    byte[] decodedString = Base64.decode(key_data, Base64.DEFAULT);
                    return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                }
                return null;
            }
        }
    }

    private static OcrData ocrResult;

    public static OcrData getOcrResult() {
//        OcrData ocrData = ocrResult;
//        ocrResult = null;
        return ocrResult;
    }

    public static void setOcrResult(OcrData ocrResult) {
        OcrData.ocrResult = ocrResult;
    }

}
