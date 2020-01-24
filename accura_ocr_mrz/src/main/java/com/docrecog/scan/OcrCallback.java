package com.docrecog.scan;

public interface OcrCallback {
    void onScannedSuccess(OcrData data,RecogResult result);
}