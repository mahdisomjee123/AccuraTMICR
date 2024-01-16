package com.accurascan.accuraocr.sample.download;

import com.androidnetworking.error.ANError;

@androidx.annotation.Keep
public abstract class DownloadListener {

    public abstract void onDownloadComplete(String filePath);
    public void onProgress(int progress){}
    public abstract void onError(String s, ANError error);
}
