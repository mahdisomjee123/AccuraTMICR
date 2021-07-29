package com.accurascan.ocr.mrz.util;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AccuraLog {
    private static boolean DEBUG = false;

    public static boolean isLogEnable() {
        return DEBUG;
    }

    public static void enableLogs(boolean isLogEnable) {
        AccuraLog.DEBUG = isLogEnable;
    }

    public static void loge(String tag, String s) {
        try {
            if (isLogEnable()) {
                logToFile("AccuraLog."+tag, "" + s);
            }
        } catch (Exception e) {
        }
    }

    public static void refreshLogfile(Context context) {
        // Refresh the data so it can seen when the device is plugged in a
        // computer. You may have to unplug and replug to see the latest
        // changes
        if (context != null) {
            try {
                File logFile = new File(Environment.getExternalStorageDirectory(), "AccuraLog.txt");

                if (!logFile.exists()) if (!logFile.createNewFile()) return;

                MediaScannerConnection.scanFile(context,
                        new String[]{logFile.toString()},
                        null,
                        null);
            } catch (IOException e) {
            }

        }
    }

    /**
     * Gets a stamp containing the current date and time to write to the log.
     * @return The stamp for the current date and time.
     */
    private static String getDateTimeStamp()
    {
        Date dateNow = Calendar.getInstance().getTime();
        // My locale, so all the log files have the same date and time format
        return (DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, Locale.UK).format(dateNow));
    }

    /**
     * Writes a message to the log file on the device.
     * @param logMessageTag A tag identifying a group of log messages.
     * @param logMessage The message to add to the log.
     */
    private static void logToFile(String logMessageTag, String logMessage)
    {
        try
        {
            // Gets the log file from the root of the primary storage. If it does
            // not exist, the file is created.
            File logFile = new File(Environment.getExternalStorageDirectory(), "AccuraLog.txt");
            if (!logFile.exists()) if (!logFile.createNewFile()) return;

            // Write the message to the log with a timestamp
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
            writer.write(String.format("%1s [%2s]:%3s\r\n", getDateTimeStamp(), logMessageTag, logMessage));
            writer.close();

        }
        catch (IOException e)
        {
        }
    }
}
