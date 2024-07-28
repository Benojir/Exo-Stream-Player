package com.venomdino.exonetworkstreamer.helpers;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;

import com.venomdino.exonetworkstreamer.models.VideoInfoModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VideoUtil {

    public static List<VideoInfoModel> getAllVideos(Context context) {

        List<VideoInfoModel> videos = new ArrayList<>();

        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.DURATION
        };

        String sortOrder = MediaStore.Video.Media.DATE_MODIFIED + " DESC";

        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
        )) {
            if (cursor != null) {
                int videoID = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int pathDataIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                int titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
                int sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
                int dateModifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED);
                int durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);

                while (cursor.moveToNext()) {

                    VideoInfoModel videoInfo = new VideoInfoModel();

                    videoInfo.setVideoPath(cursor.getString(pathDataIndex));
                    videoInfo.setVideoTitle(cursor.getString(titleIndex));
                    videoInfo.setVideoSize(cursor.getLong(sizeIndex));
                    videoInfo.setModifiedDate(cursor.getLong(dateModifiedIndex));
//                    videoInfo.setVideoDuration(getVideoDuration(cursor.getString(pathDataIndex)));
                    videoInfo.setVideoDuration(cursor.getLong(durationIndex));

                    videos.add(videoInfo);
                }
            }
        } catch (Exception e) {
            Log.e("VideoUtil", "Error retrieving videos", e);
        }

        return videos;
    }


    // Method for retrieving video duration but not in use as it is too slow
    private static long getVideoDuration(String videoPath) {
        long duration = 0;
        MediaMetadataRetriever retriever = null;

        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoPath);
            String durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

            if (durationString != null) {
                duration = Long.parseLong(durationString);
            }
        } catch (Exception e) {
            Log.e("VideoUtil", "Error getting video duration", e);
        } finally {
            if (retriever != null) {
                try {
                    retriever.release();
                } catch (Exception e) {
                    Log.e("VideoUtil", "Error releasing MediaMetadataRetriever", e);
                }
            }
        }

        return duration;
    }

    // Method defined but not in use
    private static Bitmap getVideoThumbnail(Context context, String videoPath) throws IOException {

        Size mSize = new Size(96,96);
        CancellationSignal ca = new CancellationSignal();
        Bitmap bitmapThumb;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bitmapThumb = context.getContentResolver().loadThumbnail(Uri.parse(videoPath), mSize, ca);

        } else {
            bitmapThumb = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MINI_KIND);
        }

        return bitmapThumb;
    }


    // Method defined but not in use
    private static long getVideoId(Context context, String videoPath) {
        String[] projection = {MediaStore.Video.Media._ID};
        String selection = MediaStore.Video.Media.DATA + "=?";
        String[] selectionArgs = {videoPath};

        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                return cursor.getLong(idIndex);
            }
        } catch (Exception e) {
            Log.e("VideoUtil", "Error retrieving video ID", e);
        }

        return -1;
    }
}