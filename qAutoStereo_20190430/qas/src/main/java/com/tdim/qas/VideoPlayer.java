package com.tdim.qas;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;             //API level 1
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
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
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import static com.google.android.exoplayer2.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING;
import static com.google.android.exoplayer2.upstream.DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS;
import static com.google.android.exoplayer2.upstream.DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS;

 public class VideoPlayer extends Activity {
    private static final String TAG = "VideoPlayer";
    private final ASPlaybackView surfaceView;
    private boolean error;
    private boolean prepared;
    private SimpleExoPlayer player;
    private ExoPlayerFactory playerFactory;
    private DefaultHttpDataSourceFactory dataSourceFactory;
    private PlayerView playerView;
    private String uri;

     VideoPlayer(ASPlaybackView view) {
        surfaceView = view;
//        setPlayerView(view);
//        setOnInfoListener(new OnInfoListener() {
//            @Override
//            public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {return false;}
//        });
//        setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//            @Override
//            public void onPrepared(MediaPlayer mp) {
//                prepared = true;
//                surfaceView.onPrepared();
//            }
//        });
//        setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(MediaPlayer mp) {surfaceView.onCompletion();}
//        });
//         startPlayer();
    }
//     public void setPlayerView (View view){
//         playerView = (PlayerView) view;
//     }


    public void setUri (String videoUri){
         uri = videoUri;
    }

     public void pausePlayer(){
         player.setPlayWhenReady(false);
         player.getPlaybackState();
     }
     public void startPlayer(){
         player.setPlayWhenReady(true);
         player.getPlaybackState();
     }

    public void playVideo( ) {
//        initializePlayer();
//        String uri = getIntent().getStringExtra("urlOrPath");
        if (uri != null) {
            Log.i(TAG, "Media uri: " + uri);

            ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
            dataSourceFactory = newDataSourceFactory();

            MediaSource mediaSource = buildMediaSource(Uri.parse(uri));
            initializePlayer();
            player.prepare(mediaSource);

//        if(null!=player) {
//            player.playVideo(uri);
//            renderer.setInputSize(player.getWidth(), player.getHeight());
//            renderer.play();
//        }
//        playing = true;
        }

    }

     public void initializePlayer() {

        player = playerFactory.newSimpleInstance(this);
         if (player !=null) {
             player.setPlayWhenReady(true);
             if (playerView != null){
                 playerView.setPlayer(player);
             }
             playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
             player.setVideoScalingMode(VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);

         }
//        fullScreencall();
//        player = (ExoPlayer) ExoPlayerFactory.newSimpleInstance(this, renderersFactory, trackSelector, loadControl );
     }


    private DefaultHttpDataSourceFactory newDataSourceFactory(){
        return new DefaultHttpDataSourceFactory(
                getUserAgent(this, "com.stereo.videos"),
                null,
                DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DEFAULT_READ_TIMEOUT_MILLIS,
                true
        );
    }

    private DefaultDataSourceFactory newDefaultDataSourceFactory(){
        return new DefaultDataSourceFactory(this,
                getUserAgent(this, "com.stereo.videos"));
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
                dashMediaSource = new DashMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
                return dashMediaSource;
            case C.TYPE_SS:
                ssMediaSource = new SsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
                return ssMediaSource;
            case C.TYPE_HLS:
                hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
                return hlsMediaSource;
            case C.TYPE_OTHER:
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

    private void stopPressed(){
//        stoped = true;
        player.seekTo(0);
//        seekBar.setProgress(0);
//        onProgressUpdate(0);
        player.stop();
//        buttonPlayPause.setText("play");
    }



//    @Override
//    public Looper getPlaybackLooper() {
//        return null;
//    }
//
//    @Override
//    public void retry() {
//
//    }
//
//    @Override
//    public void prepare(MediaSource mediaSource) {
//
//    }
//
//    @Override
//    public void prepare(MediaSource mediaSource, boolean resetPosition, boolean resetState) {
//
//    }
//
//    @Override
//    public PlayerMessage createMessage(PlayerMessage.Target target) {
//        return null;
//    }
//
//    @Override
//    public void sendMessages(ExoPlayerMessage... messages) {
//
//    }



//    @Override
//    public void setForegroundMode(boolean foregroundMode) {
//
//    }
//
//    @Nullable
//    @Override
//    public AudioComponent getAudioComponent() {
//        return null;
//    }
//
//    @Nullable
//    @Override
//    public VideoComponent getVideoComponent() {
//        return null;
//    }
//
//    @Nullable
//    @Override
//    public TextComponent getTextComponent() {
//        return null;
//    }
//
//    @Nullable
//    @Override
//    public MetadataComponent getMetadataComponent() {
//        return null;
//    }
//
//    @Override
//    public Looper getApplicationLooper() {
//        return null;
//    }
//
//    @Override
//    public void addListener(EventListener listener) {
//
//    }
//
//    @Override
//    public void removeListener(EventListener listener) {
//
//    }
//
//    @Override
//    public int getPlaybackState() {
//        return 0;
//    }
//
//    @Nullable
//    @Override
//    public ExoPlaybackException getPlaybackError() {
//        return null;
//    }
//
//    @Override
//    public void setPlayWhenReady(boolean playWhenReady) {
//
//    }
//
//    @Override
//    public boolean getPlayWhenReady() {
//        return false;
//    }
//
//    @Override
//    public void setRepeatMode(int repeatMode) {
//
//    }
//
//    @Override
//    public int getRepeatMode() {
//        return REPEAT_MODE_OFF;
//    }
//
//    @Override
//    public void setShuffleModeEnabled(boolean shuffleModeEnabled) {
//
//    }
//
//    @Override
//    public boolean getShuffleModeEnabled() {
//        return false;
//    }
//
//    @Override
//    public boolean isLoading() {
//        return false;
//    }
//
//    @Override
//    public void seekToDefaultPosition() {
//
//    }
//
//    @Override
//    public void seekToDefaultPosition(int windowIndex) {
//
//    }
//
//    @Override
//    public void seekTo(long positionMs) {
//
//    }
//
//    @Override
//    public void seekTo(int windowIndex, long positionMs) {
//
//    }
//
//    @Override
//    public boolean hasPrevious() {
//        return false;
//    }
//
//    @Override
//    public void previous() {
//
//    }
//
//    @Override
//    public boolean hasNext() {
//        return false;
//    }
//
//    @Override
//    public void next() {
//
//    }
//
//    @Override
//    public void setPlaybackParameters(@Nullable PlaybackParameters playbackParameters) {
//
//    }
//
//    @Override
//    public PlaybackParameters getPlaybackParameters() {
//        return null;
//    }
//
//    @Override
//    public void stop(boolean reset) {
//
//    }
//
//    @Override
//    public int getRendererCount() {
//        return 0;
//    }
//
//    @Override
//    public int getRendererType(int index) {
//        return 0;
//    }
//
//    @Override
//    public TrackGroupArray getCurrentTrackGroups() {
//        return null;
//    }
//
//    @Override
//    public TrackSelectionArray getCurrentTrackSelections() {
//        return null;
//    }
//
//    @Nullable
//    @Override
//    public Object getCurrentManifest() {
//        return null;
//    }
//
//    @Override
//    public Timeline getCurrentTimeline() {
//        return null;
//    }
//
//    @Override
//    public int getCurrentPeriodIndex() {
//        return 0;
//    }
//
//    @Override
//    public int getCurrentWindowIndex() {
//        return 0;
//    }
//
//    @Override
//    public int getNextWindowIndex() {
//        return 0;
//    }
//
//    @Override
//    public int getPreviousWindowIndex() {
//        return 0;
//    }
//
//    @Nullable
//    @Override
//    public Object getCurrentTag() {
//        return null;
//    }
//
//     @Override
//     public long getDuration() {
//         return 0;
//     }
//
//     @Override
//    public long getCurrentPosition() {
//        return 0;
//    }
//
//
//    @Override
//    public long getBufferedPosition() {
//        return 0;
//    }
//
//    @Override
//    public int getBufferedPercentage() {
//        return 0;
//    }
//
//    @Override
//    public long getTotalBufferedDuration() {
//        return 0;
//    }
//
//    @Override
//    public boolean isCurrentWindowDynamic() {
//        return false;
//    }
//
//    @Override
//    public boolean isCurrentWindowSeekable() {
//        return false;
//    }
//
//    @Override
//    public boolean isPlayingAd() {
//        return false;
//    }
//
//    @Override
//    public int getCurrentAdGroupIndex() {
//        return 0;
//    }
//
//    @Override
//    public int getCurrentAdIndexInAdGroup() {
//        return 0;
//    }
//
//    @Override
//    public long getContentDuration() {
//        return 0;
//    }
//
//    @Override
//    public long getContentPosition() {
//        return 0;
//    }
//
//    @Override
//    public long getContentBufferedPosition() {
//        return 0;
//    }
//
//
//     @Override
//     public void play() {
//        playVideo();
//     }
//
//     @Override
//    public void stop() {
//
//    }
//
//     @Override
//     public void pause() {
//
//     }
//
//     @Override
//     public boolean isPlaying() {
//         return false;
//     }
//
//     @Override
//     public void setUri(Uri uri) {
//
//     }
//
//     @Override
//     public int getWidth() {
//         return 0;
//     }
//
//     @Override
//     public int getHeight() {
//         return 0;
//     }
//
//     @Override
//     public void setSurface(Surface surface) {
//
//     }
//
//
//     @Override
//    public void release() {
//
//    }
//
//     @Override
//     public void setPosition(int ms) {
//
//     }
//
//     @Override
//     public void setDuration(int ms) {
//
//     }
//
//     @Override
//     public int getPosition() {
//         return 0;
//     }
//
//
//
////    @Override
////    public void setUri(Uri uri) {
////        try {
////            setDataSource(surfaceView.getContext(), uri);
////            prepareAsync();
////        } catch(IOException e) {
////            error = true;
////        }
////    }
////
////    @Override
////    public int getWidth() {return prepared?getVideoWidth():0;}
////    @Override
////    public int getHeight() {return prepared?getVideoHeight():0;}
////
////    @Override
////    public void setPosition(int msec) {
////        if(!prepared)
////            return;
////        seekTo(msec);
////        if(!isPlaying())
////            play();
////    }
////    @Override
////    public int getPosition() {return prepared?getCurrentPosition():0;}
////    @Override
////    public void setDuration(int msec) {}
////
////    @Override
////    public void play() {
////        if(error)
////            surfaceView.onCompletion();
////        else if(prepared)
////            start();
////    }
////
////    @Override
////    public void release() {
////        stop();
////        setSurface(null);
////        reset();
////        super.release();
////    }
}
