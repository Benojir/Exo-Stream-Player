package com.venomdino.exonetworkstreamer.helpers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.venomdino.exonetworkstreamer.R;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CustomMethods {

    private static final String TAG = "MADARA";

    public static String formatFileSize(long sizeInBytes) {
        if (sizeInBytes <= 0) {
            return "0 B";
        }

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(sizeInBytes) / Math.log10(1024));

        return String.format(Locale.getDefault(), "%.2f %s", sizeInBytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    public static String formatModifiedDate(long seconds) {
        Date date = new Date(seconds * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    public static String formatDuration(long duration) {
        // Convert milliseconds to seconds
        long seconds = duration / 1000;

        // Calculate hours, minutes, and remaining seconds
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        // Format the duration as "hh:mm:ss" with Locale.US
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }



    public static String getVersionName(Context context) {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
        return packageInfo.versionName;
    }

    public static void hideSoftKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static boolean isValidURL(String url) {

        String firstEightCharacters = url.substring(0, Math.min(url.length(), 8));

        if (firstEightCharacters.toLowerCase().startsWith("ftp://")){
            return true;
        } else if (firstEightCharacters.toLowerCase().startsWith("http://")){
            return true;
        }else if (firstEightCharacters.toLowerCase().startsWith("rtmp://")){
            return true;
        } else return firstEightCharacters.toLowerCase().startsWith("https://");
    }

    public static String getFileName(String url) {

        try{
            URL urlObj = new URL(url);
            String path = urlObj.getPath();
            int index = path.lastIndexOf("/");
            if (index != -1) {
                return path.substring(index + 1);
            } else {
                return null;
            }
        }
        catch (Exception e){
            return "Unknown";
        }
    }


    public static void errorAlert(Activity activity, String errorTitle, String errorBody, String actionButton, boolean shouldGoBack) {

        if (!activity.isFinishing()){
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(errorTitle);
            builder.setMessage(errorBody);
            builder.setIcon(R.drawable.warning_24);
            builder.setPositiveButton(actionButton, (dialogInterface, i) -> {
                if (shouldGoBack){
                    activity.finish();
                }
                else {
                    dialogInterface.dismiss();
                }
            });
            builder.setNegativeButton("Copy", (dialog, which) -> {

                ClipboardManager clipboardManager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("text", errorBody);
                clipboardManager.setPrimaryClip(clipData);

                new Handler().postDelayed(activity::finish,1000);
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }


    public static void hideSystemUI(Activity activity) {

        activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

//    ----------------------------------------------------------------------------------------------

    public static boolean deleteVideo(Activity activity, long videoId) {
        ContentResolver contentResolver = activity.getContentResolver();
        Uri videoUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId);
        try {
            int rowsDeleted = contentResolver.delete(videoUri, null, null);
            return rowsDeleted > 0;
        } catch (SecurityException e) {
            Log.e(TAG, "Error deleting video", e);
            return false;
        }
    }

    public static long getVideoIdFromPath(Activity activity, String videoPath) {
        long videoId = -1;
        ContentResolver contentResolver = activity.getContentResolver();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {MediaStore.Video.Media._ID};
        String selection = MediaStore.Video.Media.DATA + " = ?";
        String[] selectionArgs = {videoPath};

        Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, null);
        if (cursor != null && cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            videoId = cursor.getLong(idIndex);
            cursor.close();
        }

        return videoId;
    }
}
