package com.example.cattocamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.sql.DataSource;

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
   * @param activity caller fragment.
   * @return uri for the image.
   */
  public Uri getCameraIntent(Activity activity) {
    Log.e(TAG,"getCameraIntent");

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
        showToast(activity, "Failed to create direction for store photos, please check your permission for this app.");
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
        showToast(activity, "Failed to create direction for store photos, please check your permission for this app.");
        return null;
      }
    } else {
      showToast(activity, "Failed to create direction for store photos, please check your permission for this app.");
      return null;
    }
  }

  /**
   * Use for call intent for gallery.
   * @param fragment caller fragment.
   */
  public void getGalleryIntent(Fragment fragment) {
    Log.d(TAG,"getGalleryIntent");

    Intent intent = new Intent();
    intent.setType("image/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
    fragment.startActivityForResult(intent, RECEIPT_GALLERY);
  }

  public static void checkBitmapRotationEXIF(Context context, Uri imageUri, Bitmap bitmap, ResizeImageCallback callback) {
    try {
      InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
      ExifInterface ei = new ExifInterface(inputStream);
      int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
              ExifInterface.ORIENTATION_UNDEFINED);
      Log.e(TAG, "onPostExecute : Orientation : " + orientation);

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

  private static void showToast(Context context, String msg) {
    Log.d(TAG,"showToast " + msg);
    Toast.makeText(
        context.getApplicationContext(),
        msg,
        Toast.LENGTH_SHORT).show();
  }

  /**
   * Check if the image is resized or not.
   */
  private static boolean isImageResized(Bitmap bitmap) {
    Log.d(TAG,"isImageResized");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
    byte[] imageInByte = stream.toByteArray();
    long lengthbmp = imageInByte.length / 1024; // SIZE IN MB

    if(lengthbmp < 400) {
      return true;
    }
    return false;
  }

  /**
   * Resize image to certain resolution.
   */
  public static class resizeImageTask extends AsyncTask<Void, Void, Bitmap> {

    private Bitmap bitmap;
    private ResizeImageCallback callback;

    public resizeImageTask(Bitmap bitmap, ResizeImageCallback callback) {
      this.bitmap = bitmap;
      this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
    }

    @Override
    protected Bitmap doInBackground(Void... voids) {

      if (Looper.myLooper() == null) {
        Looper.prepare();
      }

      while (!isImageResized(bitmap)) {
        bitmap = Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth() * 0.8f), (int)(bitmap.getHeight() * 0.8f), true);
      }

      return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
      callback.onReturn(bitmap);
    }
  }

  public void deleteFiles() {
    Log.e(TAG,"deleteFiles");
    try {
      if (fileDirs.isDirectory())
      {
        String[] children = fileDirs.list();
        for (int i = 0; i < children.length; i++)
        {
          new File(fileDirs, children[i]).delete();
          Log.e(TAG,"Delete item.");
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static Bitmap getResizedBitmap(Bitmap bitmap, int newWidth, int newHeight) {
    Log.e(TAG,"getResizedBitmap");
    Bitmap resizedBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

    float scaleX = newWidth / (float) bitmap.getWidth();
    float scaleY = newHeight / (float) bitmap.getHeight();
    float pivotX = 0;
    float pivotY = 0;

    Matrix scaleMatrix = new Matrix();
    scaleMatrix.setScale(scaleX, scaleY, pivotX, pivotY);

    Canvas canvas = new Canvas(resizedBitmap);
    canvas.setMatrix(scaleMatrix);
    canvas.drawBitmap(bitmap, 0, 0, new Paint(Paint.FILTER_BITMAP_FLAG));

    return resizedBitmap;
  }

}
