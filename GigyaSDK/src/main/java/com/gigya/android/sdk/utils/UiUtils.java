package com.gigya.android.sdk.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Pair;

public class UiUtils {

    /**
     * Convert dp unit to equivalent pixels, depending on device density.
     */
    public static float dpToPixel(float dp, Context context) {
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * Convert device specific pixels to density independent pixels.
     */
    public static float pixelsToDp(float px, Context context) {
        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * Get device screen size in pixels.
     *
     * @param context Activity context.
     * @return A pair of width & height pixel dimensions.
     */
    public static Pair<Integer, Integer> getScreenSize(Activity context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return new Pair<>(displayMetrics.widthPixels, displayMetrics.heightPixels);
    }

    public static boolean isPortrait(Context context) {
        final int orientation = context.getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    public static Bitmap bitmapFromBase64(String encodedImage) {
        byte[] decodedString = Base64.decode(encodedImage.split(",")[1], Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }
}
