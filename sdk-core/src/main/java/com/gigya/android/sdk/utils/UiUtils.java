package com.gigya.android.sdk.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Window;
import android.view.WindowManager;

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
     * Apply secure flag to given window (Activity).
     */
    public static void secureActivity(Window window) {
        if (window != null) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    /**
     * Get device screen size in pixels.
     *
     * @param context Activity context.
     * @return A pair of width and height pixel dimensions.
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

    public static Activity findActivity(Context context) {
        if (context == null)
            return null;
        else if (context instanceof Activity)
            return (Activity) context;
        else if (context instanceof ContextWrapper)
            return findActivity(((ContextWrapper) context).getBaseContext());
        return null;
    }
}
