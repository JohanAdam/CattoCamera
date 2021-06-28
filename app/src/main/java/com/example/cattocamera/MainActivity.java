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

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private Uri imageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

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
        new ImageUtils().getGalleryIntent(this);
    }

    private void cameraIntent() {
        imageUri = new ImageUtils().getCameraIntent(this);
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

                String dateTime;
                if (requestCode == RECEIPT_CAMERA) {
                    ImageUtils.checkBitmapRotationEXIF(this, imageUri, bitmap, returnBitmap -> binding.iv.setImageBitmap(returnBitmap));
                    dateTime = ImageUtils.getImageDateTime(this, imageUri.toString());
                } else {
                    Uri imageUrg = Uri.fromFile(new File(String.valueOf(data.getData())));

//                    ImageUtils.checkBitmapRotationEXIF(this, imageUrg, bitmap, bitbit -> binding.iv.setImageBitmap(bitbit));
                    int rotationGallery = getRotationFromGallery(this, data.getData());
                    Timber.e("onActivityResult : rotationGallery : %s", rotationGallery);
                    dateTime = "GGWP";
                }
                binding.tvImgDatetime.setText(dateTime);
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

    public static String getImageInfo(Context context, Uri photoUri) {

        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] {
                        MediaStore.Images.ImageColumns.ORIENTATION,
                        MediaStore.Images.ImageColumns.LATITUDE,
                        MediaStore.Images.ImageColumns.LONGITUDE,
                        MediaStore.Images.ImageColumns.DATE_TAKEN } , null, null, null);

        if (cursor.getCount() != 1) {
            return null;
        }

        cursor.moveToFirst();

        String dateTime = String.valueOf(cursor.getLong(3) / 1000);

//        ImageInfo i = new ImageInfo();
//        i.Orientation = cursor.getInt(0);
//        i.Lat = cursor.getDouble(1);
//        i.Lon = cursor.getDouble(2);
//        i.DateTakenUTC = cursor.getLong(3)/1000;

        cursor.close();

        return dateTime;
    }
}