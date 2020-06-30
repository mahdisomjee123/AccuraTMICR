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
     *
     * @param result is scanned card data
     *  result instance of {@link OcrData} if recog type is {@link com.docrecog.scan.RecogType#OCR}
     *              or {@link com.docrecog.scan.RecogType#DL_PLATE}
     *  result instance of {@link RecogResult} if recog type is {@link com.docrecog.scan.RecogType#MRZ}
     *  result instance of {@link PDF417Data} if recog type is {@link com.docrecog.scan.RecogType#PDF417}
     *  result instance of {@link String} if recog type is {@link com.docrecog.scan.RecogType#BARCODE}
     *
     */
    void onScannedComplete(Object result);

    /**
     * To get update message for user interaction which is called continuously
     * @param titleCode to display scan card message ontop of border Frame
     *
     * @param errorMessage to display process message.
     *                is null if message is not available
     * @param isFlip to set your customize animation after complete front scan
     *               and then scan back side. true if front and back side available in cards.
     */
    void onProcessUpdate(int titleCode, String errorMessage, boolean isFlip);

    /**
     * call this method if error on getting data from sdk
     * @param errorMessage
     */
    void onError(String errorMessage);

}