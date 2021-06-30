package com.example.cattocamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
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
  private File photoFile = null;
  public Uri getCameraIntent(Activity activity) {
    Timber.e("getCameraIntent");

    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    // Ensure that there's a camera activity to handle the intent
    if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
      // Create the File where the photo should go
//      File photoFile = null;
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

  public File getPhotoFile() {
    return photoFile;
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

  public static Bitmap checkBitmapRotationEXIF(Context context, Uri imageUri, Bitmap bitmap) {
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
//      callback.onReturn(rotatedBitmap);
      return rotatedBitmap;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return bitmap;
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

  public Bitmap drawTextToBitmap(Context context,
                               Bitmap bitmap,
                               String textBottomLeft,
                               String textBottomRight) {
    Resources resources = context.getResources();
    float scale = resources.getDisplayMetrics().density;
    android.graphics.Bitmap.Config bitmapConfig =
            bitmap.getConfig();
    if (bitmapConfig == null) {
      bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
    }
    bitmap = bitmap.copy(bitmapConfig, true);
    Canvas canvas = new Canvas(bitmap);
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setColor(resources.getColor(R.color.white));
//        paint.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/DS-DIGI.TTF"));
    paint.setTextSize((int) (14 * scale));
    paint.setShadowLayer(10f, 1f, 1f, resources.getColor(R.color.black));
    Rect bounds = new Rect();
    paint.getTextBounds(textBottomRight, 0, textBottomRight.length(), bounds);

    //Center.
//        int x = (bitmap.getWidth() - bounds.width()) / 2;
//        int y = (bitmap.getHeight() + bounds.height()) / 2;

    //Left bottom.
//        int x;
//        int y;
//        if (isRight) {
    //Right bottom.
    int horizontalSpacing = 24;
    int verticalSpacing = 36;
    int xRight = (bitmap.getWidth() - bounds.width()) - horizontalSpacing;//(bitmap.getWidth() - bounds.width()) / 2;
    int yRight = bitmap.getHeight() - verticalSpacing;//(bitmap.getHeight() + bounds.height()) / 2;
//        } else {
//            int horizontalSpacing = 24;
//            int verticalSpacing = 36;
    int xLeft = horizontalSpacing;//(bitmap.getWidth() - bounds.width()) / 2;
    int yLeft = bitmap.getHeight()-verticalSpacing;
//        }
    canvas.drawText(textBottomRight, xRight, yRight, paint);
    canvas.drawText(textBottomLeft, xLeft, yLeft, paint);

    return bitmap;
  }

  public String getImageFilePath(Context context, Uri uri) {

    File file = new File(uri.getPath());
    String[] filePath = file.getPath().split(":");
    String image_id = filePath[filePath.length - 1];

    Cursor cursor = context.getContentResolver().query(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, MediaStore.Images.Media._ID + " = ? ", new String[]{image_id}, null);
    if (cursor != null) {
      cursor.moveToFirst();
      String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));

      cursor.close();
      return imagePath;
    }
    return null;
  }

}
