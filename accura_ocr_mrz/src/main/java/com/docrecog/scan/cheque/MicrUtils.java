//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.docrecog.scan.cheque;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

import com.accurascan.ocr.mrz.util.AccuraLog;
import com.docrecog.scan.RecogEngine;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.util.regex.Pattern;

public class MicrUtils {
    private static final Pattern ROUTING_NUMBER_PATTERN = Pattern.compile("[A-D]{1}[0-9]{6}[A-D]{1}");
    private static final Pattern MICR_NUMBER_PATTERN = Pattern.compile("[0-9A-D]{10}");
    private final float closerPercentage = 0.35f;
    private final float awayParcentage = 0.75f;
    TessBaseAPI tessBaseAPI = new TessBaseAPI();
    String currentRoutingNumber;
    String currentAccountNumber;
    private boolean isRunning;

    public MicrUtils(Context context) {
        if (!TessManager.ensureTesseractData(context)) {
            AccuraLog.loge("TAG", "Failed to write Tesseract data.");
            return;
        }
        this.tessBaseAPI.setDebug(true);
        this.tessBaseAPI.init(TessManager.getDataPath(), TessManager.getLanguage());
        this.tessBaseAPI.setVariable("tessedit_char_whitelist", "0123456789ABCD");
        this.tessBaseAPI.setVariable("save_best_choices", "T");
    }

    public MicrOcrResult processBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return (new MicrOcrResult((String)null, (String)null, (String)null));
        } else {
            this.isRunning = true;
            Bitmap bitmap1 = Bitmap.createBitmap(bitmap, 0, (int) (bitmap.getHeight() * 0.70), bitmap.getWidth(), (int) (bitmap.getHeight() - (bitmap.getHeight() * 0.71)));

            this.tessBaseAPI.setImage(bitmap1);
            this.tessBaseAPI.getUTF8Text();
            MicrOcrResult result = /*this.produceExternalResult*/(this.extractMICRDetails(bitmap.getWidth()));
//            Log.e("TAG", "processBitmap: Extracted Data" + result.rawScan);
            this.isRunning = false;
            return result;
        }
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    public void clear() {
        this.tessBaseAPI.clear();
    }
    public void destroy() {
        this.tessBaseAPI.recycle();
        this.currentRoutingNumber = null;
        this.currentAccountNumber = null;
    }

    private MicrOcrResult extractMICRDetails(int width) {
        final ResultIterator iterator = this.tessBaseAPI.getResultIterator();
        int[] lastBoundingBox;
        iterator.begin();
        String message = "1";
        String routingNumber = null;
        String MICRNumber = null;
        float lastAcceptedConfidence = 0;
        do {
            lastBoundingBox = iterator.getBoundingBox(TessBaseAPI.PageIteratorLevel.RIL_WORD);
            Rect lastRectBox = new Rect(lastBoundingBox[0], lastBoundingBox[1],
                    lastBoundingBox[2], lastBoundingBox[3]);
            String lineText = iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD);
            float confidence = iterator.confidence(TessBaseAPI.PageIteratorLevel.RIL_WORD);
            try {
                if (lineText != null && confidence > 70 && confidence > lastAcceptedConfidence && lineText.length() > 20) {
                    lastAcceptedConfidence = confidence;
                    String strippedScan = (lineText.replaceAll(" ", "")).trim();
                    String alphabetLetters = lineText.replaceAll("[0-9]", "").trim();
//                    Log.e("TAG", "extractMICRDetails: " + height + ", " + lastRectBox.width() + ", " + lastRectBox.height() + ", " + lineText);
                    if (lastRectBox.width() < width*closerPercentage) {
                        message = RecogEngine.ACCURA_ERROR_CODE_CLOSER;// "-1";//"Move phone Closer";
                    } else if (lastRectBox.width() > width*awayParcentage) {
                        message = RecogEngine.ACCURA_ERROR_CODE_AWAY;//"-2";//"Move phone Away";
                    } else if (alphabetLetters.length()<4) {
                        message = RecogEngine.ACCURA_ERROR_CODE_MICR_IN_FRAME;//"-3";//"Move phone Away";
                    } else {
                        MICRNumber = strippedScan;
                        routingNumber = MICRNumber.substring(0, 7);
                    }
                }
            } catch (Exception e) {
            }
        } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD));
        iterator.delete();
        return new MicrOcrResult(message, routingNumber, MICRNumber);
    }

    public static class MicrOcrResult {
        public String rawScan;
        public String routingNumber;
        public String MICRNumber;

        public MicrOcrResult(String rawScan, String routingNumber, String MICRNumber) {
            this.rawScan = rawScan;
            this.routingNumber = routingNumber;
            this.MICRNumber = MICRNumber;
        }

        @Override
        public String toString() {
            return "MicrOcrResult{" +
                    "rawScan='" + rawScan + '\'' +
                    ", routingNumber='" + routingNumber + '\'' +
                    ", MICRNumber='" + MICRNumber + '\'' +
                    '}';
        }
    }
}
