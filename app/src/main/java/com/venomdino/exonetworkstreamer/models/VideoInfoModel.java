package com.venomdino.exonetworkstreamer.models;

public class VideoInfoModel {

    private String videoPath;
    private String videoTitle;
    private long videoSize;
    private long modifiedDate;
    private String thumbnailPath;
    private long videoDuration;

    public VideoInfoModel() {
    }

    public VideoInfoModel(String videoPath, String videoTitle, long videoSize, long modifiedDate, String thumbnailPath, long videoDuration) {
        this.videoPath = videoPath;
        this.videoTitle = videoTitle;
        this.videoSize = videoSize;
        this.modifiedDate = modifiedDate;
        this.thumbnailPath = thumbnailPath;
        this.videoDuration = videoDuration;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }

    public long getVideoSize() {
        return videoSize;
    }

    public void setVideoSize(long videoSize) {
        this.videoSize = videoSize;
    }

    public long getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(long modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public long getVideoDuration() {
        return videoDuration;
    }

    public void setVideoDuration(long videoDuration) {
        this.videoDuration = videoDuration;
    }
}
