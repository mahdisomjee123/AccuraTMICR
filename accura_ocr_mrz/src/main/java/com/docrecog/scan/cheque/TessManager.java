//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.docrecog.scan.cheque;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TessManager {
    private static String DATA_PATH;

    public TessManager() {
    }

    static String getLanguage() {
        return "mcr";
    }

    static String getDataPath() {
        return DATA_PATH;
    }

    public static boolean ensureTesseractData(Context activity) {
        DATA_PATH = activity.getFilesDir()+ File.separator + "check" + File.separator;
        String dataSubdir = DATA_PATH + "tessdata" + File.separator;
        return activity != null && ensureDirectoryExists(DATA_PATH) && ensureDirectoryExists(dataSubdir) && ensureTrainedDataInAssetDirectory(activity.getAssets());
    }

    private static boolean ensureDirectoryExists(String path) {
        File directory = new File(path);
        return directory.exists() || directory.mkdirs();
    }

    private static boolean ensureTrainedDataInAssetDirectory(AssetManager assetManager) {
        String internalPath = "mcr.traineddata";
        String externalPath = DATA_PATH + "tessdata" + File.separator + internalPath;
        if (!(new File(externalPath)).exists()) {
            try {
                InputStream inputStream = assetManager.open(internalPath);
                OutputStream outputStream = new FileOutputStream(externalPath);
                byte[] buffer = new byte[1024];

                int length;
                while((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                inputStream.close();
                outputStream.close();
            } catch (IOException var7) {
                return false;
            }
        }

        return true;
    }

}
