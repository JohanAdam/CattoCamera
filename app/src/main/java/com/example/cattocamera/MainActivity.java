package com.example.cattocamera;

import static com.example.cattocamera.ImageUtils.RECEIPT_CAMERA;
import static com.example.cattocamera.ImageUtils.getRealPathFromURI;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

import com.example.cattocamera.databinding.ActivityMainBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Date;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private ImageUtils imageUtils = null;

    private ActivityMainBinding binding;
    private Uri imageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        imageUtils = new ImageUtils();

        binding.btnCamera.setOnClickListener(v -> {
            //Check permission.
            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) &&
                    (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                //Ask for permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 66);
                return;
            }

            new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_MaterialComponents)
                    .setMessage("Select Source")
                    .setNegativeButton("Gallery", (dialog, which) -> galleryIntent())
                    .setPositiveButton("Camera", (dialog, which) -> cameraIntent())
                    .show();
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 66 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Timber.e("onRequestPermissionsResult: Permission Granted");
            Utils.showToast(this, "onRequestPermissionsResult: Permission Granted");
        } else {
            Timber.e("onRequestPermissionsResult: PERMISSION DENIED");
            Utils.showToast(this, "onRequestPermissionsResult: PERMISSION DENIED!");
        }
    }

    private void galleryIntent() {
        imageUtils.getGalleryIntent(this);
    }

    private void cameraIntent() {
        imageUri = imageUtils.getCameraIntent(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Timber.d("onActivityResult: RESULT_OK ");
            try {

                Bitmap bitmap;

                if (requestCode == RECEIPT_CAMERA) {
                    Timber.d("onActivityResult : CAMERA");
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                } else {
                    Timber.d("onActivityResult : GALLERY " + data.getData());
                    Timber.d("onActivityResult : GALLERY " + getRealPathFromURI(this, data.getData()));
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                }

                String lastModifiedDate = "";
                if (requestCode == RECEIPT_CAMERA) {
                    ImageUtils.checkBitmapRotationEXIF(this, imageUri, bitmap, returnBitmap -> binding.iv.setImageBitmap(returnBitmap));
                    File file = imageUtils.getPhotoFile();

                    lastModifiedDate = new Date(file.lastModified()).toString();

                    Timber.e("onActivityResult : File is %s", file.exists());
                    Timber.e("onActivityResult : file date created is " + lastModifiedDate);

                } else {
                    String fileUri = getImageFilePathGGWP(data.getData());
                    File file = new File(fileUri);

                    lastModifiedDate = new Date(file.lastModified()).toString();

                    Timber.e("onActivityResult : File is %s", file.exists());
                    Timber.e("onActivityResult : file date created is " + lastModifiedDate);

                    int rotationGallery = getRotationFromGallery(this, data.getData());
                    Timber.e("onActivityResult : rotationGallery : %s", rotationGallery);
                }
                binding.tvImgDatetime.setText(lastModifiedDate);
            } catch (IOException e) {
                Timber.e("onActivityResult : ERROR");
                e.printStackTrace();
                Utils.showToast(this, e.getMessage());
            }
        } else {
            Timber.d("onActivityResult: RESULT_NOT_OK");
            Utils.showToast(this, "RESULT NOT OK!");
        }
    }

    /**
     * getting the rotated image using Exif taken from gallery
     *
     * @param context  context of the activity where you want to receive result
     * @param imageUri uri of the file to be rotated
     * @return orientation of the image
     */
    public static int getRotationFromGallery(Context context, Uri imageUri) {
        int result = 0;
        String[] columns = {MediaStore.Images.Media.DATE_TAKEN};
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(imageUri, columns, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int orientationColumnIndex = cursor.getColumnIndex(columns[0]);
                result = cursor.getInt(orientationColumnIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
            //Do nothing
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }//End of try-catch block
        return result;
    }

    public String getImageFilePathGGWP(Uri uri) {

        File file = new File(uri.getPath());
        String[] filePath = file.getPath().split(":");
        String image_id = filePath[filePath.length - 1];

        Cursor cursor = getContentResolver().query(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, MediaStore.Images.Media._ID + " = ? ", new String[]{image_id}, null);
        if (cursor != null) {
            cursor.moveToFirst();
            String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));

            cursor.close();
            return imagePath;
        }
        return null;
    }
}