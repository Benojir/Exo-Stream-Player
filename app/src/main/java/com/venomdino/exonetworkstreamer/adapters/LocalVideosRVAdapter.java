package com.venomdino.exonetworkstreamer.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.venomdino.exonetworkstreamer.R;
import com.venomdino.exonetworkstreamer.activities.PlayerActivity;
import com.venomdino.exonetworkstreamer.helpers.CustomMethods;
import com.venomdino.exonetworkstreamer.interfaces.DeleteMedia;
import com.venomdino.exonetworkstreamer.models.VideoInfoModel;
import com.venomdino.exonetworkstreamer.others.Statics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@UnstableApi
public class LocalVideosRVAdapter extends RecyclerView.Adapter<LocalVideosRVAdapter.MyCustomViewHolder> implements Filterable, DeleteMedia {

    private final Activity activity;
    private List<VideoInfoModel> videos;
    private final List<VideoInfoModel> allVideosBackup;
    private final String TAG = "MADARA";
    private int lastDeleteTryPos;

    public LocalVideosRVAdapter(Activity activity, List<VideoInfoModel> videos){
        this.activity = activity;
        this.videos = videos;
        this.allVideosBackup = videos;
    }

    @NonNull
    @Override
    public MyCustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(activity);
        View view = layoutInflater.inflate(R.layout.sample_local_videos_item, parent, false);
        return new MyCustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyCustomViewHolder holder, int position) {

        VideoInfoModel video = videos.get(holder.getBindingAdapterPosition());

        Glide.with(activity.getApplicationContext())
                .load(video.getVideoPath())
                .placeholder(R.drawable.sample_thumbnail)
                .into(holder.thumbnailView);

        holder.videoTitleTV.setText(video.getVideoTitle());
        holder.videoDurationTV.setText(CustomMethods.formatDuration(video.getVideoDuration()));
        holder.videoSizeTV.setText(CustomMethods.formatFileSize(video.getVideoSize()));
        holder.modifiedDateTV.setText(CustomMethods.formatModifiedDate(video.getModifiedDate()));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(activity, PlayerActivity.class);
            intent.putExtra("mediaFileUrlOrPath", video.getVideoPath());
            intent.putExtra("mediaFileName", video.getVideoTitle());
            intent.putExtra("isLocalFile", true);
            activity.startActivity(intent);
        });

        holder.itemView.setOnLongClickListener(v -> {

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Delete");
            builder.setMessage("Do you want to delete this media file?");
            builder.setPositiveButton("Delete", (dialog, which) -> {

                lastDeleteTryPos = holder.getBindingAdapterPosition();
                String videoPath = video.getVideoPath();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                    Cursor cursor = activity.getContentResolver().query(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            new String[]{MediaStore.Video.Media._ID},
                            MediaStore.Video.Media.DATA + "=?",
                            new String[]{videoPath},
                            null
                    );

                    if (cursor != null && cursor.moveToFirst()) {
                        @SuppressLint("Range") long videoId = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media._ID));
                        Uri videoUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId);
                        List<Uri>  uriList = new ArrayList<>();
                        uriList.add(videoUri);
                        cursor.close();

                        PendingIntent pi = MediaStore.createDeleteRequest(activity.getContentResolver(), uriList);

                        try {
                            activity.startIntentSenderForResult(pi.getIntentSender(), Statics.DELETE_VIDEO_REQUEST_CODE, null, 0, 0, 0);
                        } catch (Exception e) {
                            Toast.makeText(activity, "Failed to delete this video.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "onBindViewHolder: ", e);
                        }

                    } else {
                        // Handle case where video not found in MediaStore
                        Log.e(TAG, "Video not found in MediaStore");
                        Toast.makeText(activity, "Failed to delete this video. Not found.", Toast.LENGTH_SHORT).show();
                    }

                } else {

                    File mediaFile = new File(videoPath);

                    if (mediaFile.delete()){
                        Toast.makeText(activity, "Deleted", Toast.LENGTH_SHORT).show();
                        notifyItemRemoved(position);
                    } else {
                        Toast.makeText(activity, "Something went wrong.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            AlertDialog alertDialog = builder.create();
            alertDialog.show();

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    @Override
    public Filter getFilter() {
        return mediaFilter;
    }

    private final Filter mediaFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {

            List<VideoInfoModel> filteredVideos = new ArrayList<>();

            if (charSequence == null || charSequence.length() == 0){
                filteredVideos = allVideosBackup;
                videos = allVideosBackup;
            }
            else{
                String searchTerm = charSequence.toString().trim().toLowerCase();

                for (int i = 0; i < allVideosBackup.size(); i++){

                    if (allVideosBackup.get(i).getVideoTitle().toLowerCase().contains(searchTerm)){
                        filteredVideos.add(allVideosBackup.get(i));
                    }
                }
            }

            FilterResults filterResults = new FilterResults();
            filterResults.values = filteredVideos;
            filterResults.count = filteredVideos.size();

            return filterResults;
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            videos = (List<VideoInfoModel>) filterResults.values;
            notifyDataSetChanged();
        }
    };

    @Override
    public void onSuccess() {
        Toast.makeText(activity, "Deleted", Toast.LENGTH_SHORT).show();
        allVideosBackup.remove(lastDeleteTryPos);
        videos.remove(lastDeleteTryPos);
        notifyItemRemoved(lastDeleteTryPos);
    }

    @Override
    public void onFailed() {
        Toast.makeText(activity, "Failed to delete", Toast.LENGTH_SHORT).show();
    }

    public static class MyCustomViewHolder extends RecyclerView.ViewHolder{

        ImageView thumbnailView;
        TextView videoTitleTV, videoSizeTV, modifiedDateTV, videoDurationTV;

        public MyCustomViewHolder(@NonNull View itemView) {
            super(itemView);

            thumbnailView = itemView.findViewById(R.id.thumbnailView);
            videoTitleTV = itemView.findViewById(R.id.videoTitleTV);
            videoSizeTV = itemView.findViewById(R.id.videoSizeTV);
            modifiedDateTV = itemView.findViewById(R.id.modifiedDateTV);
            videoDurationTV = itemView.findViewById(R.id.videoDurationTV);
        }
    }
}
