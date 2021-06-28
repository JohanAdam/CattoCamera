package com.example.cattocamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import timber.log.Timber;

public class ImageUtils {

  private static final String TAG = "ImageUtils";

  public static final int RECEIPT_CAMERA = 0;
  public static final int RECEIPT_GALLERY = 1;

  private File fileDirs = new File(Environment.getExternalStorageDirectory() + "/test/temp/ggwp_");
  private File file = new File(
      Environment.getExternalStorageDirectory() + "/test/temp/ggwp_", "t002_" +
      System.currentTimeMillis() / 1000 + ".jpeg"); //it is image file name.

  public ImageUtils() {
    //public constructor.
  }

  public boolean createDirectory() {
    if (!fileDirs.exists()) {
      return fileDirs.mkdirs();
    } else {
      return true;
    }
  }

  /**
   * Create file in internal memory.
   * Any file type.
   */
  public enum FILE_EXTENSION {
    PDF,
    JPG
  }
  public static File createFile(Context context, FILE_EXTENSION fileExtension, String fileName) throws IOException {
    String fileExtensionString = "";
    String enviromentDirectory = "";
    if (fileExtension.equals(FILE_EXTENSION.PDF)) {
      fileExtensionString = ".pdf";
      enviromentDirectory = Environment.DIRECTORY_DOWNLOADS;
    } else if (fileExtension.equals(FILE_EXTENSION.JPG)) {
      fileExtensionString = ".jpg";
      enviromentDirectory = Environment.DIRECTORY_PICTURES;
    }


    // Create an file name
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String fileNameWithTimeStamp = fileName + timeStamp + "_";
    File storageDir = context.getExternalFilesDir(enviromentDirectory);
    File file = File.createTempFile(
            fileNameWithTimeStamp,  /* prefix */
            fileExtensionString,    /* suffix */
            storageDir      /* directory */
    );

    // Save a file: path for use with ACTION_VIEW intents
//    String currentFilePath = image.getAbsolutePath();
    return file;
  }

  /**
   * Use for call intent for camera.
   * @param activity caller activity.
   * @return uri for the image.
   */
  public Uri getCameraIntent(Activity activity) {
    Timber.e("getCameraIntent");

    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    // Ensure that there's a camera activity to handle the intent
    if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
      // Create the File where the photo should go
      File photoFile = null;
      try {
        photoFile = createFile(activity, FILE_EXTENSION.JPG, "JPEG_");
      } catch (IOException ex) {
        // Error occurred while creating the File
        ex.printStackTrace();
        Utils.showToast(activity, "Failed to create direction for store photos, please check your permission for this app.");
        return null;
      }
      // Continue only if the File was successfully created
      if (photoFile != null) {
        Uri photoURI = FileProvider.getUriForFile(activity,
            "com.example.cattocamera.fileprovider",
            photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        activity.startActivityForResult(takePictureIntent, RECEIPT_CAMERA);

        return photoURI;
      } else {
        Utils.showToast(activity, "Failed to create direction for store photos, please check your permission for this app.");
        return null;
      }
    } else {
      Utils.showToast(activity, "Failed to create direction for store photos, please check your permission for this app.");
      return null;
    }
  }

  /**
   * Use for call intent for gallery.
   */
  public void getGalleryIntent(Activity activity) {
    Timber.d("getGalleryIntent");

    Intent intent = new Intent();
    intent.setType("image/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
    activity.startActivityForResult(intent, RECEIPT_GALLERY);
  }

  public static void checkBitmapRotationEXIF(Context context, Uri imageUri, Bitmap bitmap, ResizeImageCallback callback) {
    try {
      InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
      ExifInterface ei = new ExifInterface(inputStream);

      int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
              ExifInterface.ORIENTATION_UNDEFINED);
      Timber.e("onPostExecute : Orientation : %s", orientation);

      Bitmap rotatedBitmap;
      switch(orientation) {

        case ExifInterface.ORIENTATION_ROTATE_90:
          rotatedBitmap = rotateImage(bitmap, 90);
          break;

        case ExifInterface.ORIENTATION_ROTATE_180:
          rotatedBitmap = rotateImage(bitmap, 180);
          break;

        case ExifInterface.ORIENTATION_ROTATE_270:
          rotatedBitmap = rotateImage(bitmap, 270);
          break;

        case ExifInterface.ORIENTATION_NORMAL:
        default:
          rotatedBitmap = bitmap;
      }
      callback.onReturn(rotatedBitmap);
    } catch (Exception e) {
      e.printStackTrace();
      callback.onReturn(bitmap);
    }
  }

  public static Bitmap rotateImage(Bitmap source, float angle) {
    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
            matrix, true);
  }

  public interface ResizeImageCallback {
    void onReturn(Bitmap bitmap);
  }

  public static String getRealPathFromURI(Context context, Uri contentUri) {
    Cursor cursor = context.getContentResolver().query(contentUri, null, null, null, null);
    cursor.moveToFirst();
    String document_id = cursor.getString(0);
    document_id = document_id.substring(document_id.lastIndexOf(":")+1);
    cursor.close();

    cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,null
            , MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
    cursor.moveToFirst();
    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
    cursor.close();

    return path;
  }

  public static String getImageDateTime(Context context, String imagesFilePath) {
    Timber.d("getImageDateTime : ");
    try {
      Timber.d("getImageDateTime : YAY");
      InputStream inputStream = context.getContentResolver().openInputStream(Uri.parse(imagesFilePath));
      ExifInterface ei = new ExifInterface(inputStream);

      String dateTime = ei.getAttribute(ExifInterface.TAG_DATETIME);
      Timber.e("getImageDateTime : dateTime return is %s", dateTime);
      return dateTime;
    } catch (Exception e) {
      Timber.e("getImageDateTime : ERROR");
      e.printStackTrace();
      return "ERROR " + e.getMessage();
    }
  }

}
