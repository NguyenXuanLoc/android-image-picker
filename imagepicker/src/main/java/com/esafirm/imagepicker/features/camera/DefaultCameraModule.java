package com.esafirm.imagepicker.features.camera;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.esafirm.imagepicker.features.ImagePickerConfigFactory;
import com.esafirm.imagepicker.features.common.BaseConfig;
import com.esafirm.imagepicker.helper.ImagePickerUtils;
import com.esafirm.imagepicker.helper.IpLogger;
import com.esafirm.imagepicker.model.ImageFactory;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.core.content.FileProvider;

public class DefaultCameraModule implements CameraModule, Serializable {

    private static final String TAG = "DefaultCameraModule";
    private String currentImagePath;
//
//    public Intent getCameraIntent(Context context) {
//        return getCameraIntent(context, ImagePickerConfigFactory.createDefault(context));
//    }

    @Override
    public Intent getCameraIntent(Context context, BaseConfig config, boolean isPhoto) {
        Intent intent = new Intent(isPhoto ? MediaStore.ACTION_IMAGE_CAPTURE : MediaStore.ACTION_VIDEO_CAPTURE);
        File imageFile = ImagePickerUtils.createImageFile(config.getImageDirectory(), isPhoto);
        Log.d(TAG,"directory: "+config.getImageDirectory().getPath());
        Log.d(TAG,"imageFile: "+imageFile.getAbsolutePath());
        if (imageFile != null) {
            Context appContext = context.getApplicationContext();
            String providerName = String.format(Locale.ENGLISH, "%s%s", appContext.getPackageName(), ".imagepicker.provider");
            Uri uri =getOutputMediaFileUri(isPhoto? MEDIA_TYPE_IMAGE: MEDIA_TYPE_VIDEO,context,providerName);/* FileProvider.getUriForFile(appContext, providerName, imageFile);*/
            currentImagePath = "file:" + imageFile.getAbsolutePath();
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            ImagePickerUtils.grantAppPermission(context, intent, uri);

            return intent;
        }
        return null;
    }

    private Uri getOutputMediaFileUri(int type,Context context,String providerName) {
        File outputMediaFile = getOutputMediaFile(type);
        if (outputMediaFile == null) {
            return null;
        }
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context,
                    providerName,
                    getOutputMediaFile(type));
        } else {
            return Uri.fromFile(getOutputMediaFile(type));
        }
    }

    private File getOutputMediaFile(int type) {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        String fileName;
        String filePath;
        if (type == MEDIA_TYPE_IMAGE) {
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM), "Polsat_News");
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return null;
                }
            }

            fileName = "IMG_" + timeStamp + ".jpg";
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + fileName);
            filePath = mediaFile.getAbsolutePath();
        } else if (type == MEDIA_TYPE_VIDEO) {
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM), "Polsat_News");
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return null;
                }
            }

            fileName = "VID_" + timeStamp + ".mp4";
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + fileName);
            filePath = mediaFile.getAbsolutePath();
        } else {
            return null;
        }

        return mediaFile;
    }

    @Override
    public void getImage(final Context context, Intent intent, final OnImageReadyListener imageReadyListener) {
        if (imageReadyListener == null) {
            throw new IllegalStateException("OnImageReadyListener must not be null");
        }

        if (currentImagePath == null) {
            IpLogger.getInstance().w("currentImagePath null. " +
                    "This happen if you haven't call #getCameraIntent() or the activity is being recreated");
            imageReadyListener.onImageReady(null);
            return;
        }

        final Uri imageUri = Uri.parse(currentImagePath);
        if (imageUri != null) {
            MediaScannerConnection.scanFile(context.getApplicationContext(),
                    new String[]{imageUri.getPath()}, null, (path, uri) -> {

                        IpLogger.getInstance().d("File " + path + " was scanned successfully: " + uri);

                        if (path == null) {
                            IpLogger.getInstance().d("This should not happen, go back to Immediate implemenation");
                            path = currentImagePath;
                        }

                        imageReadyListener.onImageReady(ImageFactory.singleListFromPath(path));
                        ImagePickerUtils.revokeAppPermission(context, imageUri);
                    });
        }
    }

    @Override
    public void removeImage() {
        if (currentImagePath != null) {
            File file = new File(currentImagePath);
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
