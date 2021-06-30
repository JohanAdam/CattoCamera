package com.example.cattocamera;

import static com.example.cattocamera.ImageUtils.RECEIPT_CAMERA;
import static com.example.cattocamera.ImageUtils.getRealPathFromURI;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.DateFormat;
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

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

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

                Date lastModifiedDate;
                Bitmap resultBitmap = bitmap;
                if (requestCode == RECEIPT_CAMERA) {
                    //Get photo file path.
                    File file = imageUtils.getPhotoFile();

                    //Get file last modified date.
                    lastModifiedDate = new Date(file.lastModified());

                    Timber.e("onActivityResult : File is %s", file.exists());
                    Timber.e("onActivityResult : file date created is " + lastModifiedDate.toString());

                    //Set bitmap to UI.
                    resultBitmap = ImageUtils.checkBitmapRotationEXIF(this, imageUri, bitmap);
                } else {
                    //Get photo file path.
                    String fileUri = imageUtils.getImageFilePath(this, data.getData());
                    File file = new File(fileUri);

                    //Get file last modified date.
                    lastModifiedDate = new Date(file.lastModified());

                    Timber.e("onActivityResult : File is %s", file.exists());
                    Timber.e("onActivityResult : file date created is " + lastModifiedDate.toString());

                    //Set bitmap to UI.
//                    setImage(bitmap);
                }

                String date = DateFormat.format("dd MMM yyyy\nhh:mm:ss a", lastModifiedDate).toString();
                setImage(imageUtils.drawTextToBitmap(this, resultBitmap, "Nyan Nyan", date));
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

    private void setImage(Bitmap bitmap) {
        binding.iv.setImageBitmap(bitmap);
    }

}