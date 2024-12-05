//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.checkem;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MicrOcrUtil {
    private static final int ROUTING_NUMBER_HISTORY_LENGTH = 8;
    private static final int ROUTING_NUMBER_THRESHOLD = 3;
    private static final int ACCOUNT_NUMBER_HISTORY_LENGTH = 8;
    private static final int ACCOUNT_NUMBER_THRESHOLD = 3;
    private static final Pattern ROUTING_NUMBER_PATTERN = Pattern.compile("[A-D]{1}[0-9]{6}[A-D]{1}");
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("[0-9d]{6,10}C");
    TessBaseAPI tessBaseAPI = new TessBaseAPI();
    String currentRoutingNumber;
    List<String> routingNumberHistory = new ArrayList();
    String currentAccountNumber;
    List<String> accountNumberHistory = new ArrayList();
    private boolean isRunning;

    public MicrOcrUtil(Context micrScanActivity) {
        if (!TesseractDataManager.ensureTesseractData(micrScanActivity)) {
            Log.e("TAG", "Failed to write Tesseract data.");
            return;
        }
//        TesseractDataManager.setDataPath(micrScanActivity);
        this.tessBaseAPI.setDebug(true);
        this.tessBaseAPI.init(TesseractDataManager.getDataPath(), TesseractDataManager.getLanguage());
        this.tessBaseAPI.setVariable("tessedit_char_whitelist", "0123456789ABCD");
        this.tessBaseAPI.setVariable("save_best_choices", "T");
    }

    public MicrOcrResult processBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return this.produceExternalResult(new MicrOcrResult((String)null, (String)null, (String)null));
        } else {
            Log.e("TAG", "processBitmap: " + bitmap.getWidth());
            this.isRunning = true;
            this.tessBaseAPI.setImage(bitmap);
            MicrOcrResult result = /*this.produceExternalResult*/(this.produceInternalResult(this.tessBaseAPI.getUTF8Text()));
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
        this.routingNumberHistory.clear();
        this.currentAccountNumber = null;
        this.accountNumberHistory.clear();
    }

    private MicrOcrResult produceInternalResult(String rawScan) {
        String routingNumber = null;
        String accountNumber = null;
        String[] replaced = (rawScan.replaceAll(" ", "")).split("\n");
        int i = replaced.length;
        while (i > 0) {
            String strippedScan = replaced[--i];
            Log.e("TAG", "strippedScan: " +strippedScan);
            Matcher routingNumberMatcher = ROUTING_NUMBER_PATTERN.matcher(strippedScan);
            if (routingNumberMatcher.find()) {
                Log.e("TAG", "produceInternalResult: " + routingNumberMatcher.start() + ", " + routingNumberMatcher.end());
                routingNumber = routingNumberMatcher.group();
//                routingNumber = routingNumber.substring(0, 7);
                Matcher accountNumberMatcher = ACCOUNT_NUMBER_PATTERN.matcher(strippedScan.substring(routingNumberMatcher.end()));
                if (accountNumberMatcher.find()) {
                    accountNumber = accountNumberMatcher.group();
//                    accountNumber = accountNumber.substring(1, accountNumber.length() - 1);
//                    accountNumber = accountNumber.replace("d", "-");
                    if (accountNumber.length() - accountNumber.replace("-", "").length() > 1) {
                        accountNumber = null;
                    }
                }
                break;
            }

        }


        return new MicrOcrResult(rawScan, routingNumber, accountNumber);
    }

    private MicrOcrResult produceExternalResult(MicrOcrResult internalResult) {
        String newRoutingNumber = null;
        String newAccountNumber = null;
        int matchCount;
        Iterator var5;
        String pastAccountNumber;
        if (internalResult.routingNumber != null) {
            if (this.routingNumberHistory.isEmpty()) {
                newRoutingNumber = internalResult.routingNumber;
            } else {
                matchCount = 0;
                var5 = this.routingNumberHistory.iterator();

                while(var5.hasNext()) {
                    pastAccountNumber = (String)var5.next();
                    if (internalResult.routingNumber.equals(pastAccountNumber)) {
                        ++matchCount;
                    }
                }

                if (matchCount >= 3) {
                    newRoutingNumber = internalResult.routingNumber;
                }

                if (this.routingNumberHistory.size() >= 8) {
                    this.routingNumberHistory.remove(0);
                }
            }

            this.routingNumberHistory.add(internalResult.routingNumber);
            if (this.currentRoutingNumber == null || newRoutingNumber != null) {
                this.currentRoutingNumber = internalResult.routingNumber;
            }
        }

        if (internalResult.accountNumber != null) {
            if (this.accountNumberHistory.isEmpty()) {
                newAccountNumber = internalResult.accountNumber;
            } else {
                matchCount = 0;
                var5 = this.accountNumberHistory.iterator();

                while(var5.hasNext()) {
                    pastAccountNumber = (String)var5.next();
                    if (internalResult.accountNumber.equals(pastAccountNumber)) {
                        ++matchCount;
                    }
                }

                if (matchCount >= 3) {
                    newAccountNumber = internalResult.accountNumber;
                }

                if (this.accountNumberHistory.size() >= 8) {
                    this.accountNumberHistory.remove(0);
                }
            }

            this.accountNumberHistory.add(internalResult.accountNumber);
            if (this.currentAccountNumber == null || newAccountNumber != null) {
                this.currentAccountNumber = internalResult.accountNumber;
            }
        }

        return new MicrOcrResult(internalResult.rawScan, this.currentRoutingNumber, this.currentAccountNumber);
    }

    public static class MicrOcrResult {
        public String rawScan;
        public String routingNumber;
        public String accountNumber;

        public MicrOcrResult(String rawScan, String routingNumber, String accountNumber) {
            this.rawScan = rawScan;
            this.routingNumber = routingNumber;
            this.accountNumber = accountNumber;
        }

        @Override
        public String toString() {
            return "MicrOcrResult : " +
//                    "\nExtracted Text ='" + rawScan + '\'' +
                    "\nroutingNumber ='" + routingNumber + '\'' +
                    "\naccountNumber ='" + accountNumber + '\'';
        }
    }
}
