package com.gigya.android.sdk.ui.plugin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.utils.FileUtils;

import java.io.File;
import java.lang.ref.WeakReference;

import static android.app.Activity.RESULT_OK;

public class GigyaPluginFileChooser extends WebChromeClient {

    private final WeakReference<Fragment> _fragmentRef;

    GigyaPluginFileChooser(Fragment fragment) {
        _fragmentRef = new WeakReference<>(fragment);
    }

    private Fragment getFragment() {
        return _fragmentRef.get();
    }

    private static final String LOG_TAG = "GigyaPluginFileChooser";

    static final int FIRE_ACCESS_PERMISSION_REQUEST_CODE = 14;
    static final int FILE_CHOOSER_MEDIA_REQUEST_CODE = 15;
    private String _cameraTempImagePath;
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

        final int externalStoragePermission = ContextCompat.checkSelfPermission(webView.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (externalStoragePermission == PackageManager.PERMISSION_GRANTED) {
            sendImageChooserIntent();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getFragment().requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, FIRE_ACCESS_PERMISSION_REQUEST_CODE);
        } else {
            GigyaLogger.error(LOG_TAG, "Application must include Manifest.permission.WRITE_EXTERNAL_STORAGE in order to communicate with file system");
            return false;
        }
        return true;
    }

    private void sendImageChooserIntent() {
        if (getFragment() == null) {
            return;
        }
        GigyaLogger.debug(LOG_TAG, "Sending image chooser intent.");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getFragment().getActivity().getPackageManager()) != null) {
            File imageFile = null;
            try {
                imageFile = FileUtils.createImageFile();
                takePictureIntent.putExtra("imagePath", _cameraTempImagePath);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (imageFile != null) {
                _cameraTempImagePath = "file:" + imageFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
            } else {
                takePictureIntent = null;
            }
        }

        // Set up an image file chooser intent.
        final Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("image/*");

        final Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, takePictureIntent != null ?
                new Intent[]{takePictureIntent} : new Intent[0]);
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
            else if (_cameraTempImagePath != null) {
                results = new Uri[]{Uri.parse(_cameraTempImagePath)};
            }
        }
        _imagePathCallback.onReceiveValue(results);
        _imagePathCallback = null;
    }

    void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        GigyaLogger.debug(LOG_TAG, "onRequestPermissionsResult:");
        if (requestCode == FIRE_ACCESS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                GigyaLogger.debug(LOG_TAG, "External storage permission explicitly granted.");
                if (_imagePathCallback != null) {
                    sendImageChooserIntent();
                }
            } else {
                // Permission denied by the user.
                GigyaLogger.debug(LOG_TAG, "External storage permission explicitly denied.");
            }
        }
    }
}
