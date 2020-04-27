package com.accurascan.ocr.mrz.interfaces;

import com.accurascan.ocr.mrz.model.OcrData;
import com.accurascan.ocr.mrz.model.PDF417Data;
import com.accurascan.ocr.mrz.model.RecogResult;

public interface OcrCallback {

    /**
     * Call this method to set border frame which is used in center of the device.
     * position your country id card to the frame.
     * width and height are according to card ratio.
     *
     * @param width
     * @param height
     */
    void onUpdateLayout(int width, int height);

    /**
     * call this method after scan complete
     * @param data is scanned card data if set {@link com.docrecog.scan.RecogType#OCR} else it is null
     * @param mrzData an mrz card data if set {@link com.docrecog.scan.RecogType#MRZ} else it is null
     * @param pdf417Data an barcode card data if set {@link com.docrecog.scan.RecogType#PDF417} else it is null
     */
    void onScannedComplete(OcrData data, RecogResult mrzData, PDF417Data pdf417Data);

    /**
     * To get update message for user interaction which is called continuously
     * @param title to display scan card message(is front/ back card of the @cardname)
     *              is null if data is not available.
     * @param message to display process message.
     *                is null if message is not available
     * @param isFlip to set your customize animation after complete front scan
     *               and then scan back side. true if front and back side available in cards.
     */
    void onProcessUpdate(String title, String message, boolean isFlip);

    /**
     * call this method if error on getting data from sdk
     * @param errorMessage
     */
    void onError(String errorMessage);

}