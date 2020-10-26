package com.gigya.android.sdk.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class FileUtils {
    private Context _context;

    public FileUtils(Context context) {
        _context = context;
    }

    public boolean containsFile(String fileName) {
        final AssetManager am = _context.getAssets();
        try {
            final String[] list = am.list("");
            if (list != null) {
                return Arrays.asList(list).contains(fileName);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /*
    Load JSON configuration file from application Assets folder.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public String loadFile(String fileName) throws IOException {
        return assetJsonFileToString(_context, fileName);
    }

    public Bundle getMetaData() {
        try {
            ApplicationInfo appInfo = _context.getPackageManager().getApplicationInfo(_context.getPackageName(), PackageManager.GET_META_DATA);
            return appInfo.metaData;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Nullable
    public String stringFromMetaData(String fieldName) {
        Bundle metaData = getMetaData();
        if (metaData == null) {
            return null;
        } else {
            // Avoiding hard coded values and not string references to cause class cast exceptions.
            if (metaData.get(fieldName) instanceof String)
                return (String) metaData.get(fieldName);
            else if (metaData.get(fieldName) instanceof Float || metaData.get(fieldName) instanceof Integer) {
                return String.valueOf(fieldName);
            } else return null;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static String assetJsonFileToString(Context appContext, String fileName) throws IOException {
        InputStream is = appContext.getAssets().open(fileName);
        return streamToString(is);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "CharsetObjectCanBeUsed"})
    public static String streamToString(InputStream is) throws IOException {
        final int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        return new String(buffer, "UTF-8");
    }

    @SuppressLint("SimpleDateFormat")
    public static File createImageFile() throws IOException {
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final String imageFileName = "JPEG_" + timeStamp + "_";
        final File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }
}
