package com.tdim.qas;

import android.content.Context;         //API level 1
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;         //API level 1
import android.graphics.SurfaceTexture; //API level 11
import android.net.Uri;                 //API level 1
import android.os.Build;
import android.os.Looper;
import android.util.AttributeSet;       //API level 1
import android.util.Log;
import android.view.Surface;            //API level 14
import android.view.TextureView;
import android.view.View;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.PlayerMessage;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import static com.google.android.exoplayer2.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING;
import static com.google.android.exoplayer2.upstream.DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS;
import static com.google.android.exoplayer2.upstream.DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS;

public final class ASPlaybackView extends RotatableSurfaceView implements ExoPlayer, FaceDetectorBase.ViewOffsetListener {
    private static final String TAG = ASPlaybackView.class.getSimpleName();
    private Object MainThread;

    @Override
    public Looper getPlaybackLooper() {
        return null;
    }

    @Override
    public void retry() {

    }

    @Override
    public void prepare(MediaSource mediaSource) {

    }


    @Override
    public void prepare(MediaSource mediaSource, boolean resetPosition, boolean resetState) {

    }

    @Override
    public PlayerMessage createMessage(PlayerMessage.Target target) {
        return null;
    }

    @Override
    public void sendMessages(ExoPlayerMessage... messages) {

    }

    @Override
    public void blockingSendMessages(ExoPlayerMessage... messages) {

    }

    @Override
    public void setSeekParameters(@Nullable SeekParameters seekParameters) {

    }

    @Override
    public SeekParameters getSeekParameters() {
        return null;
    }

    @Override
    public void setForegroundMode(boolean foregroundMode) {

    }

    public interface PlaybackListener {
        /**
         * Called after ASPlaybackView.setVideo, when the player has prepared the video.
         */
        void onPrepared();

        /**
         * Called when new frame is ready.
         * @param ms Timestamp of the current frame in milliseconds
         */
        void onProgressUpdate(int ms);

        /**
         * Called when the video has finished playing.
         */
        void onFinished();
    }


    private final PlaybackRenderer renderer;
    private PlaybackListener playListener;
    private Surface surface;
    private TextureView textureView;
    private Uri videoUri;
    private SimpleExoPlayer player;
    private boolean playing;
    private boolean useDetection = true;
    private FaceDetectorBase detect;
    private PlayerView playerView;
    private ExoPlayerFactory playerFactory;
    private DefaultHttpDataSourceFactory dataSourceFactory;

    public ASPlaybackView(Context context) {this(context, null);}
    public ASPlaybackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if(isInEditMode())
            renderer = null;
        else {
            setEGLContextClientVersion(2);
            setEGLConfigChooser(false);
            renderer = new PlaybackRenderer(this);
            //renderer.setWatermark(BitmapFactory.decodeResource(context.getResources(), R.drawable.logo_demo));
            renderer.setSurfaceTextureListener(new PlaybackRenderer.SurfaceTextureListener() {
                @Override
                public void onCreated(SurfaceTexture surfaceTexture) {
                    Log.i(TAG, "surfaceTexture: "+ surfaceTexture);
                    textureView = new TextureView(context);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        textureView.setSurfaceTexture(surfaceTexture);
                    }
                    surface = new Surface(surfaceTexture);

                    if(null!=videoUri)
                        Log.i(TAG, "setVideoUriCall With uri: "+ videoUri);

                    setVideo(videoUri);
                }
            });
            setRenderer(renderer);
            renderer.setRotation(getDeviceRotation());
            setRenderMode(RENDERMODE_CONTINUOUSLY);
            detect = FaceDetectorFactory.createFaceDetector(context);
            detect.setOffsetListener(this);
        }
        Log.i(TAG, "Constructor AsPlabackView: ");

    }

    @Override
    protected void onRotation(int rotation) {
        super.onRotation(rotation);
        detect.setDeviceRotation(rotation);
    }



    @Override
    void setDisplay(@NonNull DisplayParameters params) {
        PixelPitch pitches = new PixelPitch(params.h, params.v);
        if(isLandscape())
            pitches.rotate();
        pitches.setAngle(pitches.getAngle()+params.error);
        renderer.setDisplay(pitches.h(), pitches.v(), params.pg);
        renderer.setViewBase(params.offset);
    }
    @Override
    public void onViewOffset(float offset) {renderer.setViewOffset(offset);}

    public void useFaceDetection(boolean state) {
        useDetection = state;
        if(useDetection)
            detect.startDetection();
        else
            detect.stopDetection();
    }
    @Override
    public void onResume() {//TODO save current BMP and set again
        super.onResume();
        play();
        if(useDetection) detect.startDetection();
    }
    @Override
    public void onPause() {
        super.onPause();
        detect.stopDetection();
        pause();
    }

    private void playVideo() {
        Log.i(TAG, "Media uri: " + videoUri);

//        initializePlayer();
        if (videoUri != null) {
//            String uri = getIntent().getStringExtra("urlOrPath");
            Log.i(TAG, "Media uri: " + videoUri);

            ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
            dataSourceFactory = newDataSourceFactory();
            Log.i(TAG, "dataSource Factory: " + dataSourceFactory);
            initializePlayer();

            MediaSource mediaSource = buildMediaSource(videoUri);
            player.prepare(mediaSource);
            player.setPlayWhenReady(true);
            player.getPlaybackState();
        }

    }

    private void initializePlayer() {
        Log.i(TAG, "initializePlayer");

        DefaultTrackSelector trackSelector = new DefaultTrackSelector();
        DefaultLoadControl loadControl = new DefaultLoadControl();
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(getContext());

        player = playerFactory.newSimpleInstance(getContext());
        player.getApplicationLooper();
        Log.i(TAG, "initializePlayer - context: "+ player.getApplicationLooper());

        if (player !=null) {
//            if (surface != null){
            if (textureView != null){
                player.setVideoTextureView(textureView);
//                player.setVideoSurface(surface);
                Log.i(TAG, "initialize player - surface settled: "+ textureView);

            }
            player.setPlayWhenReady(true);
//            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
            player.setVideoScalingMode(VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);

        }
        Log.i(TAG, "initializePlayer 2 - surface: "+surface);
        Log.i(TAG, "initializePlayer 2 - texture: "+textureView);

//        fullScreencall();
//        player = (ExoPlayer) ExoPlayerFactory.newSimpleInstance(this, renderersFactory, trackSelector, loadControl );
    }

    private DefaultHttpDataSourceFactory newDataSourceFactory(){
        return new DefaultHttpDataSourceFactory(
                getUserAgent(getContext(), "com.stereo.videos"),
                null,
                DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DEFAULT_READ_TIMEOUT_MILLIS,
                true
        );
    }



    private DefaultDataSourceFactory newDefaultDataSourceFactory(){
        String userAgent = getUserAgent(getContext(), "com.stereo.videos");
        return new DefaultDataSourceFactory(getContext(), userAgent);
    }

    private MediaSource buildMediaSource(Uri uri) {
        @C.ContentType int type = Util.inferContentType(uri);
        HlsMediaSource hlsMediaSource;
        DashMediaSource dashMediaSource;
        SsMediaSource ssMediaSource;
        MediaSource mediaSource;
        DefaultHttpDataSourceFactory dataSourceFactory = newDataSourceFactory();
        DefaultDataSourceFactory defaultDataSourceFactory = newDefaultDataSourceFactory();
        switch (type) {
            case C.TYPE_DASH:
                Log.i(TAG, "Media DASH: " + videoUri);

                dashMediaSource = new DashMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
                return dashMediaSource;
            case C.TYPE_SS:
                Log.i(TAG, "Media SS: " + videoUri);

                ssMediaSource = new SsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
                return ssMediaSource;
            case C.TYPE_HLS:
                Log.i(TAG, "Media HLS: " + videoUri);

                hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
                return hlsMediaSource;
            case C.TYPE_OTHER:
                Log.i(TAG, "Media OTHER: " + videoUri);

                mediaSource = new ExtractorMediaSource.Factory(defaultDataSourceFactory).createMediaSource(uri);
                return mediaSource;
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    public static String getUserAgent(Context context, String applicationName) {
        String versionName;
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "?";
        }
        return applicationName + "/" + versionName + " (Linux;Android " + Build.VERSION.RELEASE
                + ") " + ExoPlayerLibraryInfo.VERSION_SLASHY;
    }


    /**
     * Set 3D format of the picture/video. If you do not set the format when changing the media,
     * the current format will be used. The initial format is InputFormat.None
     * @param format Input format of the picture/video.
     */
    public void setInputFormat(ASConstants.InputFormat format) {renderer.setInputFormat(format);}

    /**
     * Whether to render in 3D or 2D.
     * @param use3D true if media should be rendered for autostereoscopy, false to show in 2D.
     */
    public void set3DMode(boolean use3D) {renderer.set3DMode(use3D);}

    public void setViewOffset(float b) {renderer.setViewBase(b);}

    public void setFaceDetectionListener(ASDetectionListener listener) {detect.setFaceListener(listener);}

    /**
     * Set listener for playback events.
     * @param listener Listener to be informed of playback events.
     */
    public void setPlaybackListener(PlaybackListener listener) {this.playListener = listener;}

    /**
     * Set Image to show on screen. Configure 3D format with setInputFormat.
     * @param bmp Image to show
     */
    public void setImage(Bitmap bmp) {
        releasePlayer();
        setBitmap(bmp);
        this.renderer.setInputSize(bmp.getWidth(), bmp.getHeight());
        renderer.play();
    }

    /**
     * Set Video to load. Will be prepared asynchronously, set a PlaybackListener to be informed
     * when the player is ready. getCurrentPos and getDuration are only available when the player
     * is prepared.
     * Configure 3D format with setInputFormat.
     * @param uri Uri to the video to load.
     */
    public void setVideo(Uri uri) {
        if(null==uri)
            throw new NullPointerException();
        releasePlayer();
        videoUri = uri;
        Log.i(TAG, "setVideo - uri: " + videoUri);
        if(null==surface)
            return;
        initializePlayer();
//        player = new VideoPlayer(this);
//        player.setVideoSurface(surface);
//        player.(videoUri);
        if(surface != null) {
            playVideo();
            if (player.getVideoFormat() != null) {
//                this.renderer.setInputSize(this.player.getVideoFormat().width, this.player.getVideoFormat().height);
                this.renderer.setInputSize(500, 200);

                Log.i(TAG, "player width : " + this.player.getVideoFormat().width + "   height: "+ this.player.getVideoFormat().height);
            }
            renderer.play();
        }
        Log.i(TAG, "player instance : " + player);
    }

    //----------------------------------------PLAYBACK----------------------------------------------

    /**
     * Start playing a new video or resume playback after call to pause().
     */
    public void play() {
        if(null!=player) {
            startPlayer();
            renderer.play();
        }
        playing = true;
    }

    /**
     * Pause playback. While the player is paused, the frame will not be updated, but rendering
     * parameter changes will be visible (setInputFormat, set3DMode, setDisplayParameter). Resume
     * with play().
     */
    public void pause() {
        if(null!=player) {
            pausePlayer();
            renderer.pause();
        }
        playing = false;
    }

    @Nullable
    @Override
    public AudioComponent getAudioComponent() {
        return null;
    }

    @Nullable
    @Override
    public VideoComponent getVideoComponent() {
        return null;
    }

    @Nullable
    @Override
    public TextComponent getTextComponent() {
        return null;
    }

    @Nullable
    @Override
    public MetadataComponent getMetadataComponent() {
        return null;
    }

    @Override
    public Looper getApplicationLooper() {
        return null;
    }

    @Override
    public void addListener(EventListener listener) {

    }

    @Override
    public void removeListener(EventListener listener) {

    }

    @Override
    public int getPlaybackState() {
        return 0;
    }

    @Nullable
    @Override
    public ExoPlaybackException getPlaybackError() {
        return null;
    }

    @Override
    public void setPlayWhenReady(boolean playWhenReady) {

    }

    @Override
    public boolean getPlayWhenReady() {
        return false;
    }

    @Override
    public void setRepeatMode(int repeatMode) {

    }

    @Override
    public int getRepeatMode() {
        return REPEAT_MODE_OFF;
    }

    @Override
    public void setShuffleModeEnabled(boolean shuffleModeEnabled) {

    }

    @Override
    public boolean getShuffleModeEnabled() {
        return false;
    }

    @Override
    public boolean isLoading() {
        return false;
    }

    @Override
    public void seekToDefaultPosition() {

    }

    @Override
    public void seekToDefaultPosition(int windowIndex) {

    }

    @Override
    public void seekTo(long positionMs) {

    }

    @Override
    public void seekTo(int windowIndex, long positionMs) {

    }

    @Override
    public boolean hasPrevious() {
        return false;
    }

    @Override
    public void previous() {

    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public void next() {

    }

    @Override
    public void setPlaybackParameters(@Nullable PlaybackParameters playbackParameters) {

    }

    @Override
    public PlaybackParameters getPlaybackParameters() {
        return null;
    }

    /**
     * Stop video playback completely. After the player is stopped, the frame will not be updated,
     * and rendering parameter changes will be not be visible (setInputFormat, set3DMode,
     * setDisplayParameter). Use setImage to change image or setVideo and play().
     */
    public void stop() {
//        releasePlayer();
        playing = false;
//        videoUri = null;
    }

    @Override
    public void stop(boolean reset) {

    }

    @Override
    public void release() {

    }

    @Override
    public int getRendererCount() {
        return 0;
    }

    @Override
    public int getRendererType(int index) {
        return 0;
    }

    @Override
    public TrackGroupArray getCurrentTrackGroups() {
        return null;
    }

    @Override
    public TrackSelectionArray getCurrentTrackSelections() {
        return null;
    }

    @Nullable
    @Override
    public Object getCurrentManifest() {
        return null;
    }

    @Override
    public Timeline getCurrentTimeline() {
        return null;
    }

    @Override
    public int getCurrentPeriodIndex() {
        return 0;
    }

    @Override
    public int getCurrentWindowIndex() {
        return 0;
    }

    @Override
    public int getNextWindowIndex() {
        return 0;
    }

    @Override
    public int getPreviousWindowIndex() {
        return 0;
    }

    @Nullable
    @Override
    public Object getCurrentTag() {
        return null;
    }

    /**
     * Check if a video player is currently playing.
     * @return true if a video is playing, false if it is paused or there is no active video.
     */
    public boolean isPlaying() {return playing;}

    /**
     * Seek to a different position when playing a video. Requires player to be in prepared state
     * (wait for PlaybackListener.onPrepared after setVideo).
     * @param msec Position in milliseconds
     */
    public void seekTo(int msec) {
        if(null!=player)
            player.seekTo(msec);
    }

    /**
     * Get duration of the current video. Requires player to be in prepared state
     * (wait for PlaybackListener.onPrepared after setVideo).
     * @return Duration in milliseconds of the current video, 0 if there is no video playing.
     */
    public long getDuration() {return null==player?0: (int) player.getDuration();}

    @Override
    public long getCurrentPosition() {
        return 0;
    }

    @Override
    public long getBufferedPosition() {
        return 0;
    }

    @Override
    public int getBufferedPercentage() {
        return 0;
    }

    @Override
    public long getTotalBufferedDuration() {
        return 0;
    }

    @Override
    public boolean isCurrentWindowDynamic() {
        return false;
    }

    @Override
    public boolean isCurrentWindowSeekable() {
        return false;
    }

    @Override
    public boolean isPlayingAd() {
        return false;
    }

    @Override
    public int getCurrentAdGroupIndex() {
        return 0;
    }

    @Override
    public int getCurrentAdIndexInAdGroup() {
        return 0;
    }

    @Override
    public long getContentDuration() {
        return 0;
    }

    @Override
    public long getContentPosition() {
        return 0;
    }

    @Override
    public long getContentBufferedPosition() {
        return 0;
    }

    /**
     * Get position of the current video. Requires player to be in prepared state
     * (wait for PlaybackListener.onPrepared after setVideo).
     * @return Position in milliseconds of the current video, 0 if there is no video playing.
     */
    public int getCurrentPos() {return null==player?0: (int) player.getCurrentPosition();}

    //-------------------------------------------------------------------------
    void setBitmap(Bitmap bmp) {renderer.setImage(bmp);}

    public void pausePlayer(){
        player.setPlayWhenReady(false);
        player.getPlaybackState();
    }

    public void startPlayer(){
        player.setPlayWhenReady(true);
        player.getPlaybackState();
    }

    void onPrepared() {
        if (this.playing) {
            this.play();
        }

        if (null != this.playListener) {
            this.playListener.onPrepared();
        }

    }
    void onAdvance() {
        if(null!=playListener)
            playListener.onProgressUpdate(getCurrentPos());
    }
    void onCompletion() {
        stop();
        if(null!=playListener)
            playListener.onFinished();
    }

    private void releasePlayer() {
        if(null==player)
            return;
        player.release();
        player = null;
        videoUri = null;
        renderer.stop();
    }
}
