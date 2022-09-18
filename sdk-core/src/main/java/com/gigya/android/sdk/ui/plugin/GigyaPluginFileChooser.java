package com.gigya.android.sdk.ui.plugin;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.fragment.app.Fragment;

import com.gigya.android.sdk.GigyaLogger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class GigyaPluginFileChooser extends WebChromeClient {

    private final WeakReference<Fragment> _fragmentRef;

    GigyaPluginFileChooser(Fragment fragment) {
        _fragmentRef = new WeakReference<>(fragment);
    }

    private Fragment getFragment() {
        return _fragmentRef.get();
    }

    private static final String LOG_TAG = "GigyaPluginFileChooser";

    static final int FILE_CHOOSER_MEDIA_REQUEST_CODE = 15;
    private Bitmap _captureBitmap;
    private ValueCallback<Uri[]> _imagePathCallback;

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        GigyaLogger.debug(LOG_TAG, "onShowFileChooser: ");
        if (getFragment() == null) {
            return false;
        }

        if (_imagePathCallback != null) {
            _imagePathCallback.onReceiveValue(null);
        }
        _imagePathCallback = filePathCallback;

        sendImageChooserIntent();
        return true;
    }

    public void clearCachedImage() {
        if (_captureBitmap != null) {
            _captureBitmap.recycle();
        }
    }

    private void sendImageChooserIntent() {
        if (getFragment() == null) {
            return;
        }
        Intent capture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        capture.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent select = new Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE);
        select.setType("image/*");
        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, select);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{capture});
        getFragment().startActivityForResult(chooserIntent, FILE_CHOOSER_MEDIA_REQUEST_CODE);
    }

    void onActivityResult(int resultCode, Intent data) {
        Uri[] results = null;
        if (resultCode == RESULT_OK) {
            if (data != null && data.getDataString() != null) {
                String dataString = data.getDataString();
                results = new Uri[]{Uri.parse(dataString)};
            }

            // If there is not data, then we may have taken a photo
            else if (data != null && data.getExtras() != null && data.getExtras().get("data") != null) {
                if (getFragment() == null) {
                    return;
                }
                _captureBitmap = (Bitmap) data.getExtras().get("data");
                if (_captureBitmap == null) {
                    return;
                }

                try {
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        File file = new File(getFragment().getContext().getFilesDir(), "gigya_profile_temp.png");
                        if (file.exists()) file.delete();
                        file.createNewFile();
                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                        _captureBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                        fileOutputStream.flush();
                        fileOutputStream.close();
                        results = new Uri[]{Uri.fromFile(file)};
                    } else {
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        _captureBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                        String path = MediaStore.Images.Media.insertImage(getFragment().getActivity().getContentResolver(), _captureBitmap, "Title", null);
                        results = new Uri[]{Uri.parse(path)};
                        bytes.flush();
                        bytes.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    _imagePathCallback.onReceiveValue(results);
                    _imagePathCallback = null;
                }
            }
        }
        _imagePathCallback.onReceiveValue(results);
        _imagePathCallback = null;
    }

}
