package com.example.cattocamera;

import android.content.Context;
import android.widget.Toast;

import timber.log.Timber;

public class Utils {

    public static void showToast(Context context, String msg) {
        Timber.d("showToast : ");
        Toast.makeText(
                context.getApplicationContext(),
                msg,
                Toast.LENGTH_SHORT).show();
    }
}
