package com.venomdino.exonetworkstreamer.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.dash.DashChunkSource;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.dash.DefaultDashChunkSource;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager;
import androidx.media3.exoplayer.drm.DrmSessionManager;
import androidx.media3.exoplayer.drm.FrameworkMediaDrm;
import androidx.media3.exoplayer.drm.HttpMediaDrmCallback;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.venomdino.exonetworkstreamer.R;
import com.venomdino.exonetworkstreamer.helpers.CustomMethods;
import com.venomdino.exonetworkstreamer.helpers.DoubleClickListener;

import java.io.File;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

@UnstableApi
public class PlayerActivity extends AppCompatActivity {

    private static final String TAG = "MADARA";
    private PlayerView playerView;
    private ProgressBar bufferProgressbar;
    private ExoPlayer exoPlayer;
    private DefaultTrackSelector defaultTrackSelector;
    private ArrayList<String> videoQualities;
    private TextView fileNameTV, brightVolumeTV;
    private LinearLayout brightnessVolumeContainer;
    private ImageView volumeIcon, brightnessIcon;
    private ImageButton screenRotateBtn, qualitySelectionBtn, backButton, fitScreenBtn, backward10, forward10;
    private Button doubleTapSkipBackwardIcon, doubleTapSkipForwardIcon;
    private int touchPositionX;
    private GestureDetectorCompat gestureDetectorCompat;
    private int brightness = 0;
    private int volume = 0;
    private AudioManager audioManager;
    private final int SHOW_MAX_BRIGHTNESS = 100;
    private final int SHOW_MAX_VOLUME = 50;
    private boolean shouldShowController = true;
    private int selectedQualityIndex = 0;
    private UUID drmScheme;
    private boolean playWhenReady = true;
    private boolean hasRetried = false;
    private long playbackPosition = C.TIME_UNSET;
    private final HashMap<String, String> requestProperties = new HashMap<>();
    private String mediaFileUrlOrPath, drmLicenceUrl, refererValue, originValue, userAgent;
    private boolean isLocalFile;
    private String mediaFileName = "";

    private static final CookieManager DEFAULT_COOKIE_MANAGER;

    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        setContentView(R.layout.activity_player);
        CustomMethods.hideSystemUI(this);


        Intent intent = getIntent();

        mediaFileUrlOrPath = intent.getStringExtra("mediaFileUrlOrPath");

        if (intent.hasExtra("drmLicenceUrl")) {

            drmLicenceUrl = intent.getStringExtra("drmLicenceUrl");

            if (drmLicenceUrl != null && drmLicenceUrl.equalsIgnoreCase("")) {
                drmLicenceUrl = getString(R.string.default_drm_licence_url);
            }
        }

        if (intent.hasExtra("refererValue")) {
            refererValue = intent.getStringExtra("refererValue");

            if (!Objects.equals(refererValue, "")){
                requestProperties.put("Referer", refererValue);
            }
        }

        if (intent.hasExtra("originValue")) {
            originValue = intent.getStringExtra("originValue");

            if (!Objects.equals(originValue, "")){
                requestProperties.put("Origin", originValue);
            }
        }

        if (intent.hasExtra("userAgent"))
            userAgent = intent.getStringExtra("userAgent");

        if (intent.hasExtra("mediaFileName"))
            mediaFileName = intent.getStringExtra("mediaFileName");


        if (intent.hasExtra("selectedDrmScheme")){

            int selectedDrmScheme = intent.getIntExtra("selectedDrmScheme", 0);

            if (selectedDrmScheme == 0) {
                drmScheme = C.WIDEVINE_UUID;
            } else if (selectedDrmScheme == 1) {
                drmScheme = C.PLAYREADY_UUID;
            } else if (selectedDrmScheme == 2) {
                drmScheme = C.CLEARKEY_UUID;
            } else {
                drmScheme = C.WIDEVINE_UUID;
            }
        }

        if (intent.hasExtra("isLocalFile")){
            isLocalFile = intent.getBooleanExtra("isLocalFile", true);
        }

        initVars();

        initializePlayer();

//        ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        /* This block of codes set the current device volume and brightness to the video on startup */
        brightness = (int) (getCurrentScreenBrightness() * 100);
        setVolumeVariable();

//        ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        screenRotateBtn.setOnClickListener(view -> {
            int currentOrientation = getResources().getConfiguration().orientation;

            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        });

//        ..........................................................................................
        qualitySelectionBtn.setOnClickListener(view -> {

            if (videoQualities != null) {

                if (videoQualities.size() > 0) {
                    getQualityChooserDialog(this, videoQualities);
                } else {
                    Toast.makeText(this, "No video quality found.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Wait until video start.", Toast.LENGTH_SHORT).show();
            }
        });
//        ..........................................................................................
        fitScreenBtn.setOnClickListener(v -> {

            if (playerView.getResizeMode() == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                fitScreenBtn.setImageResource(R.drawable.crop_5_4);
                Toast.makeText(this, "ZOOM", Toast.LENGTH_SHORT).show();
            } else if (playerView.getResizeMode() == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
                fitScreenBtn.setImageResource(R.drawable.fit_screen);
                Toast.makeText(this, "FILL", Toast.LENGTH_SHORT).show();
            } else if (playerView.getResizeMode() == AspectRatioFrameLayout.RESIZE_MODE_FILL) {
                playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                fitScreenBtn.setImageResource(R.drawable.crop_free);
                Toast.makeText(this, "FIT", Toast.LENGTH_SHORT).show();
            }
        });
//        ..........................................................................................
        backButton.setOnClickListener(view -> {

            if (exoPlayer != null) {
                exoPlayer.stop();
                exoPlayer.release();
            }
            onBackPressed();
        });
//        ..........................................................................................
        backward10.setOnClickListener(view -> exoPlayer.seekTo(exoPlayer.getCurrentPosition() - 10000));
        forward10.setOnClickListener(view -> exoPlayer.seekTo(exoPlayer.getCurrentPosition() + 10000));
//        ..........................................................................................
        playerView.setOnTouchListener((view, motionEvent) -> {

            gestureDetectorCompat.onTouchEvent(motionEvent);

            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                brightnessVolumeContainer.setVisibility(View.GONE);

                if (!shouldShowController) {

                    playerView.setUseController(false);

                    new Handler().postDelayed(() -> {
                        shouldShowController = true;
                        playerView.setUseController(true);
                    }, 500);
                }
            }

            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                touchPositionX = (int) motionEvent.getX();
            }
            return false;
        });
//        ..........................................................................................
        playerView.setOnClickListener(new DoubleClickListener(500, () -> {

            playerView.setUseController(false);
            new Handler().postDelayed(() -> playerView.setUseController(true), 500);

            int deviceWidth = Resources.getSystem().getDisplayMetrics().widthPixels;

            if (touchPositionX < deviceWidth / 2) {
                doubleTapSkipBackwardIcon.setVisibility(View.VISIBLE);
                new Handler().postDelayed(() -> doubleTapSkipBackwardIcon.setVisibility(View.GONE), 500);
                exoPlayer.seekTo(exoPlayer.getCurrentPosition() - 10000);
            } else {
                doubleTapSkipForwardIcon.setVisibility(View.VISIBLE);
                new Handler().postDelayed(() -> doubleTapSkipForwardIcon.setVisibility(View.GONE), 500);
                exoPlayer.seekTo(exoPlayer.getCurrentPosition() + 10000);
            }
        }));
//        ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //Gesture Detect Section//
//        ******************************************************************************************
        gestureDetectorCompat = new GestureDetectorCompat(this, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(@NonNull MotionEvent motionEvent) {
                return false;
            }

            @Override
            public void onShowPress(@NonNull MotionEvent motionEvent) {

            }

            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent motionEvent) {
                return false;
            }

            @Override
            public void onLongPress(@NonNull MotionEvent motionEvent) {

            }

            @Override
            public boolean onFling(MotionEvent motionEvent, @NonNull MotionEvent motionEvent1, float v, float v1) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent motionEvent, @NonNull MotionEvent motionEvent1, float distanceX, float distanceY) {

                int deviceWidth = Resources.getSystem().getDisplayMetrics().widthPixels;

                if (Math.abs(distanceY) > Math.abs(distanceX)) {

                    brightnessVolumeContainer.setVisibility(View.VISIBLE);

                    shouldShowController = false;

                    if (motionEvent.getX() < deviceWidth / 2) {

                        volumeIcon.setVisibility(View.GONE);
                        brightnessIcon.setVisibility(View.VISIBLE);

                        boolean increase = distanceY > 0;

                        int newValue = (increase) ? brightness + 1 : brightness - 1;

                        if (newValue >= 0 && newValue <= SHOW_MAX_BRIGHTNESS) {
                            brightness = newValue;
                        }

                        brightVolumeTV.setText(String.valueOf(brightness));
                        setScreenBrightness(brightness);
                    } else {

                        if (audioManager != null) {

                            volumeIcon.setVisibility(View.VISIBLE);
                            brightnessIcon.setVisibility(View.GONE);

                            boolean increase = distanceY > 0;

                            int newValue = (increase) ? volume + 1 : volume - 1;

                            if (newValue >= 0 && newValue <= SHOW_MAX_VOLUME) {
                                volume = newValue;
                            }

                            brightVolumeTV.setText(String.valueOf(volume));
                            setVolume(volume);
                        }
                    }
                }
                return true;
            }
        });

//        ******************************************************************************************
    }

    //______________________________________________________________________________________________

    private void initializePlayer() {

        @SuppressLint("UnsafeOptInUsageError")
        ExoTrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();

        defaultTrackSelector = new DefaultTrackSelector(this, videoTrackSelectionFactory);
        defaultTrackSelector.setParameters(defaultTrackSelector.getParameters().buildUpon()
                .setPreferredTextLanguage("en")
                .build());

        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this)
                .forceEnableMediaCodecAsynchronousQueueing()
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                        DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
                .build();

        //*********************************************************************************

        exoPlayer = new ExoPlayer.Builder(this, renderersFactory)
                .setTrackSelector(defaultTrackSelector)
                .setLoadControl(loadControl)
                .build();

        if (isLocalFile){
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(new File(mediaFileUrlOrPath))));
        } else {

            if (mediaFileUrlOrPath.toLowerCase().contains(".m3u8") || mediaFileUrlOrPath.toLowerCase().contains(".ts") || mediaFileUrlOrPath.toLowerCase().contains(".playlist")) {

                MediaSource mediaSource = buildHlsMediaSource(Uri.parse(mediaFileUrlOrPath), userAgent, drmLicenceUrl);
                exoPlayer.setMediaSource(mediaSource);

            } else if (mediaFileUrlOrPath.toLowerCase().contains(".mpd")) {

                MediaSource mediaSource = buildDashMediaSource(Uri.parse(mediaFileUrlOrPath), userAgent, drmLicenceUrl);
                exoPlayer.setMediaSource(mediaSource, true);

            } else {

                new Thread(() -> {

                    try {
                        URL url = new URL(mediaFileUrlOrPath);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("HEAD");  // Use HEAD request to check content type
                        conn.setRequestProperty("User-Agent", userAgent);
                        conn.setRequestProperty("Referer", refererValue);
                        conn.setRequestProperty("Origin", originValue);

                        // Enable following redirects
                        conn.setInstanceFollowRedirects(true);

                        if (conn.getResponseCode() == 200) {

                            String contentType = conn.getContentType();

                            if (contentType.equalsIgnoreCase("application/x-mpegURL")
                                    || contentType.equalsIgnoreCase("application/vnd.apple.mpegurl")
                                    || contentType.equalsIgnoreCase("video/mp2t")
                            ) {
                                MediaSource mediaSource = buildHlsMediaSource(Uri.parse(mediaFileUrlOrPath), userAgent, drmLicenceUrl);
                                new Handler(Looper.getMainLooper()).post(() -> exoPlayer.setMediaSource(mediaSource));
                            } else if (contentType.equalsIgnoreCase("application/dash+xml")) {
                                MediaSource mediaSource = buildDashMediaSource(Uri.parse(mediaFileUrlOrPath), userAgent, drmLicenceUrl);
                                new Handler(Looper.getMainLooper()).post(() -> exoPlayer.setMediaSource(mediaSource));
                            } else {
                                new Handler(Looper.getMainLooper()).post(() -> exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(mediaFileUrlOrPath))));
                            }
                        }
                        conn.disconnect();
                    } catch (Exception e) {
                        Log.e(TAG, "initializePlayer: ", e);
                        new Handler(Looper.getMainLooper()).post(() -> exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(mediaFileUrlOrPath))));
                    }
                }).start();
            }
        }


        exoPlayer.prepare();

        exoPlayer.setPlayWhenReady(playWhenReady);

        if (playbackPosition != C.TIME_UNSET) {
            exoPlayer.seekTo(playbackPosition);
        }
        exoPlayer.addListener(new Player.Listener() {

            @SuppressLint("SetTextI18n")
            @Override
            public void onPlaybackStateChanged(int playbackState) {

                if (playbackState == Player.STATE_BUFFERING) {
                    bufferProgressbar.setVisibility(View.VISIBLE);
                }
                if (playbackState == Player.STATE_READY) {

                    playerView.setVisibility(View.VISIBLE);

                    bufferProgressbar.setVisibility(View.GONE);

                    if (!Objects.equals(mediaFileName, "")) {
                        fileNameTV.setText(mediaFileName);
                    } else {
                        fileNameTV.setText(CustomMethods.getFileName(mediaFileUrlOrPath));
                    }

                    videoQualities = getVideoQualitiesTracks();
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {

                if (error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED && !hasRetried){

                    TrackSelectionParameters trackSelectionParameters = exoPlayer.getTrackSelectionParameters()
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                            .build();
                    exoPlayer.setTrackSelectionParameters(trackSelectionParameters);
                    exoPlayer.prepare();

                    Toast.makeText(PlayerActivity.this, "Trying again because playback error.", Toast.LENGTH_SHORT).show();
                    hasRetried = true;
                } else {
                    playerView.setVisibility(View.GONE);
                    bufferProgressbar.setVisibility(View.GONE);

                    exoPlayer.stop();
                    exoPlayer.release();

                    CustomMethods.errorAlert(PlayerActivity.this, "Error", error.getMessage() + "\n" + error.getErrorCodeName(), "Ok", true);
                }
            }
        });

        playerView.setPlayer(exoPlayer);
        playerView.setShowNextButton(false);
        playerView.setShowPreviousButton(false);
        playerView.setControllerShowTimeoutMs(2500);
    }

    //______________________________________________________________________________________________

    private DefaultDrmSessionManager buildDrmSessionManager(UUID uuid, String userAgent, String drmLicenseUrl) {

        HttpDataSource.Factory licenseDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setUserAgent(userAgent)
                .setDefaultRequestProperties(requestProperties);

        HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(drmLicenseUrl, true, licenseDataSourceFactory);

        return new DefaultDrmSessionManager.Builder()
                .setUuidAndExoMediaDrmProvider(uuid, FrameworkMediaDrm.DEFAULT_PROVIDER)
                .build(drmCallback);
    }

    //    ----------------------------------------------------------------------------------------------
    private DashMediaSource buildDashMediaSource(Uri uri, String userAgent, String drmLicenseUrl) {

        DataSource.Factory defaultHttpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(requestProperties)
                .setTransferListener(
                        new DefaultBandwidthMeter.Builder(PlayerActivity.this)
                                .setResetOnNetworkTypeChange(false)
                                .build()
                );

        DashChunkSource.Factory dashChunkSourceFactory = new DefaultDashChunkSource.Factory(defaultHttpDataSourceFactory);

        DataSource.Factory manifestDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setDefaultRequestProperties(requestProperties)
                .setAllowCrossProtocolRedirects(true);

        return new DashMediaSource.Factory(dashChunkSourceFactory, manifestDataSourceFactory)
                        .createMediaSource(
                                new MediaItem.Builder()
                                        .setUri(uri)
                                        // DRM Configuration
                                        .setDrmConfiguration(
                                                new MediaItem.DrmConfiguration.Builder(drmScheme)
                                                        .setLicenseUri(Uri.parse(drmLicenseUrl))
                                                        .build()
                                        )
                                        .setMimeType(MimeTypes.APPLICATION_MPD)
                                        .setTag(null)
                                        .build()
                        );
    }

    //    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    private HlsMediaSource buildHlsMediaSource(Uri uri, String userAgent, String drmLicenceUrl) {

        UUID drmSchemeUuid = Util.getDrmUuid(drmScheme.toString());

        DrmSessionManager drmSessionManager = buildDrmSessionManager(drmSchemeUuid, drmLicenceUrl, userAgent);

        DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this, new DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setDefaultRequestProperties(requestProperties)
                .setAllowCrossProtocolRedirects(true));

        return new HlsMediaSource.Factory(dataSourceFactory)
                .setDrmSessionManagerProvider(mediaItem -> drmSessionManager)
                .setAllowChunklessPreparation(true)
                .createMediaSource(
                        new MediaItem.Builder()
                                .setUri(uri)
                                .setMimeType(MimeTypes.APPLICATION_M3U8)
                                .build()
                );
    }

    //______________________________________________________________________________________________

    private ArrayList<String> getVideoQualitiesTracks() {

        ArrayList<String> videoQualities = new ArrayList<>();

        MappingTrackSelector.MappedTrackInfo renderTrack = defaultTrackSelector.getCurrentMappedTrackInfo();
        assert renderTrack != null;
        int renderCount = renderTrack.getRendererCount();

        for (int rendererIndex = 0; rendererIndex < renderCount; rendererIndex++) {

            if (isSupportedFormat(renderTrack, rendererIndex)) {

                int trackGroupType = renderTrack.getRendererType(rendererIndex);
                TrackGroupArray trackGroups = renderTrack.getTrackGroups(rendererIndex);
                int trackGroupsCount = trackGroups.length;

                if (trackGroupType == C.TRACK_TYPE_VIDEO) {

                    for (int groupIndex = 0; groupIndex < trackGroupsCount; groupIndex++) {

                        int videoQualityTrackCount = trackGroups.get(groupIndex).length;

                        for (int trackIndex = 0; trackIndex < videoQualityTrackCount; trackIndex++) {

                            boolean isTrackSupported = renderTrack.getTrackSupport(rendererIndex, groupIndex, trackIndex) == C.FORMAT_HANDLED;

                            if (isTrackSupported) {

                                TrackGroup track = trackGroups.get(groupIndex);

                                int videoWidth = track.getFormat(trackIndex).width;
                                int videoHeight = track.getFormat(trackIndex).height;

                                String quality = videoWidth + "x" + videoHeight;
                                videoQualities.add(quality);
                            }
                        }
                    }
                }
            }
        }

        return videoQualities;
    }

    private boolean isSupportedFormat(MappingTrackSelector.MappedTrackInfo mappedTrackInfo, int rendererIndex) {

        TrackGroupArray trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex);
        if (trackGroupArray.length == 0) {
            return false;
        } else {
            return mappedTrackInfo.getRendererType(rendererIndex) == C.TRACK_TYPE_VIDEO;
        }
    }
    //______________________________________________________________________________________________

    private void getQualityChooserDialog(Context context, ArrayList<String> arrayList) {

        CharSequence[] charSequences = new CharSequence[arrayList.size() + 1];
        charSequences[0] = "Auto";

        for (int i = 0; i < arrayList.size(); i++) {
            charSequences[i + 1] = arrayList.get(i); //.split("x")[1] + "p";
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("Select video quality:");
        builder.setSingleChoiceItems(charSequences, selectedQualityIndex, (dialogInterface, which) -> selectedQualityIndex = which);
        builder.setPositiveButton("Ok", (dialogInterface, i) -> {

            if (selectedQualityIndex == 0) {
                Toast.makeText(context, context.getText(R.string.app_name) + " will choose video resolution automatically.", Toast.LENGTH_SHORT).show();
                defaultTrackSelector.setParameters(defaultTrackSelector.buildUponParameters().setMaxVideoSizeSd());
            } else {
                String[] videoQualityInfo = arrayList.get(selectedQualityIndex - 1).split("x");

                Toast.makeText(context, "Video will be played with " + videoQualityInfo[1] + "p resolution.", Toast.LENGTH_SHORT).show();

                int videoWidth = Integer.parseInt(videoQualityInfo[0]);
                int videoHeight = Integer.parseInt(videoQualityInfo[1]);

                defaultTrackSelector.setParameters(
                        defaultTrackSelector
                                .buildUponParameters()
                                .setMaxVideoSize(videoWidth, videoHeight)
                                .setMinVideoSize(videoWidth, videoHeight)
                );
            }
        });
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());
        builder.show();
    }

    //______________________________________________________________________________________________

    /**
     * Save and Restore Playback State:
     * To maintain the playback state across different app states (minimized, restored),
     * you can save and restore the playback state. You can do this using the
     * onSaveInstanceState and onRestoreInstanceState methods. Here's an example:
     */

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("playWhenReady", exoPlayer.getPlayWhenReady());
        outState.putLong("playbackPosition", exoPlayer.getCurrentPosition());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        playWhenReady = savedInstanceState.getBoolean("playWhenReady");
        playbackPosition = savedInstanceState.getLong("playbackPosition", C.TIME_UNSET);
    }
    //______________________________________________________________________________________________

    @Override
    protected void onResume() {
        super.onResume();

        if (brightness > 0) {
            setScreenBrightness(brightness);
        }
        setVolume(volume);
    }

    @Override
    protected void onPause() {
        super.onPause();
        exoPlayer.setPlayWhenReady(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exoPlayer.release();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exoPlayer.stop();
        exoPlayer.release();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            setVolumeVariable();
        }
        return super.onKeyUp(keyCode, event);
    }

    //______________________________________________________________________________________________
    private void setScreenBrightness(int brightness1) {

        float d = 1.0f / SHOW_MAX_BRIGHTNESS;

        WindowManager.LayoutParams lp = getWindow().getAttributes();

        lp.screenBrightness = d * brightness1;

        getWindow().setAttributes(lp);
    }

    private float getCurrentScreenBrightness() {
        // Get the current screen brightness value
        int currentBrightness = 0;
        try {
            currentBrightness = Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS
            );
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "getCurrentScreenBrightness: ", e);
        }

        // Get the maximum brightness value supported by the device's screen
        int maxBrightness = 255; // Default value; you can get the actual maximum brightness using system APIs

        // Calculate the brightness value in the range [0, 1.0]
        float brightnessValue = (float) currentBrightness / maxBrightness;

        // Clamp the brightnessValue to the range [0, 1.0]
        brightnessValue = Math.max(0f, Math.min(1.0f, brightnessValue));

        return brightnessValue;
    }

    private void setVolume(int volume1) {

        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        float d = (maxVolume * 1.0f) / SHOW_MAX_VOLUME;

        int newVolume = (int) (d * volume1);

        if (newVolume > maxVolume) {
            newVolume = maxVolume;
        }
        if (volume1 == SHOW_MAX_VOLUME && newVolume < maxVolume) {
            newVolume = maxVolume;
        }

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
    }

    private void setVolumeVariable() {

        volume = (int) ((audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 1.0f) / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * SHOW_MAX_VOLUME);

        if (volume > SHOW_MAX_VOLUME) {
            volume = SHOW_MAX_VOLUME;
        }
    }

    //______________________________________________________________________________________________
    private void initVars() {
        playerView = findViewById(R.id.exo_player_view);
        bufferProgressbar = findViewById(R.id.buffer_progressbar);
        fileNameTV = findViewById(R.id.file_name_tv);
        qualitySelectionBtn = findViewById(R.id.quality_selection_btn);
        brightnessVolumeContainer = findViewById(R.id.brightness_volume_container);
        brightnessIcon = findViewById(R.id.brightness_icon);
        volumeIcon = findViewById(R.id.volume_icon);
        brightVolumeTV = findViewById(R.id.brightness_volume_tv);
        backButton = findViewById(R.id.back_button);
        fitScreenBtn = findViewById(R.id.fit_screen_btn);
        doubleTapSkipBackwardIcon = findViewById(R.id.double_tap_skip_backward_icon);
        doubleTapSkipForwardIcon = findViewById(R.id.double_tab_skip_forward_icon);
        backward10 = findViewById(R.id.backward_10);
        forward10 = findViewById(R.id.forward_10);
        screenRotateBtn = findViewById(R.id.screen_rotate_btn);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }
//  ------------------------------------------------------------------------------------------------
}