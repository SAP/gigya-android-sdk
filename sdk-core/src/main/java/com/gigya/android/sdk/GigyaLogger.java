package com.gigya.android.sdk;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Gigya custom Android logger class.
 * Logs are issued only when DEBUG is set to TRUE.
 */
public class GigyaLogger {

    private static final String LOG_TAG = "GigyaSDK";
    private static boolean DEBUG = false;
    public static boolean IOC = false;

    public static void setDebugMode(boolean debugModeEnabled) {
        DEBUG = debugModeEnabled;
    }

    public static boolean isDebug() {
        return DEBUG;
    }

    public static void debug(String tag, String message) {
        if (DEBUG) {
            final String logMessage = logMessage(tag, message);
            Log.d(LOG_TAG, logMessage);
            if (smartLogEnabled()) {
                appendLog(logMessage);
            }
        }
    }

    public static void error(String tag, String message) {
        if (DEBUG) {
            final String logMessage = logMessage(tag, message);
            Log.e(LOG_TAG, logMessage);
            if (smartLogEnabled()) {
                appendLog(logMessage);
            }
        }
    }

    public static void info(String tag, String message) {
        if (DEBUG) {
            final String logMessage = logMessage(tag, message);
            Log.i(LOG_TAG, logMessage);
            if (smartLogEnabled()) {
                appendLog(logMessage);
            }
        }
    }

    public static void ioc(String tag, String message) {
        if (DEBUG && IOC) {
            Log.d(LOG_TAG, logMessage(tag, message));
        }
    }

    private static String logMessage(String classTAG, String message) {
        return "<< " + classTAG + " *** " + message + " >>";
    }


    private static String _smartLoggerPath;

    public static void enableSmartLog(Context appContext) {
        _smartLoggerPath = appContext.getCacheDir().getAbsolutePath() + "/gsLog.file";
    }

    private static boolean smartLogEnabled() {
        return _smartLoggerPath != null;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void appendLog(String text) {
        File logFile = new File(_smartLoggerPath);

        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            int fileSize = Integer.parseInt(String.valueOf(logFile.length() / 1024));
            if (fileSize > 35) {
                logFile.delete();
                logFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("GigyaLogger", "Failed to create new Gigya log file");
        }

        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            Log.e("GigyaLogger", "Failed to write to Gigya log file");
            e.printStackTrace();
        }
    }

    public static void exportSmartLog(Context context) {
        final int externalStoragePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (externalStoragePermission == PackageManager.PERMISSION_GRANTED && _smartLoggerPath != null) {
            File to = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/gsLog.txt");
            File from = new File(_smartLoggerPath);
            try {
                FileInputStream inStream = new FileInputStream(from);
                FileOutputStream outStream = new FileOutputStream(to);
                FileChannel inChannel = inStream.getChannel();
                FileChannel outChannel = outStream.getChannel();
                inChannel.transferTo(0, inChannel.size(), outChannel);
                inStream.close();
                outStream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}
