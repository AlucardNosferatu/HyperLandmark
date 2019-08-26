package com.tdim.demoplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.tdim.qas.ASConstants;
import com.tdim.qas.ASDetectionListener;
import com.tdim.qas.ASPlaybackView;
import com.tdim.qas.ASSettings;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ASPlaybackView.PlaybackListener, ASDetectionListener {
//    private ASPlaybackView player;
    private ASPlaybackView player;
    private boolean paused = false;
    private static boolean use3D = true;
    private SeekBar seekBar;
    private TextView textDuration;
    private Button buttonPlayPause, button3D, buttonGoBack, buttonStop;
    private LinearLayout layoutPlayControls;
    private ConstraintLayout layoutScreenComponents;

    public String TAG = "MainActivity";
    private boolean stoped = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "usao u main: " );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        ASSettings settings = new ASSettings(getBaseContext());
        settings.setCameraQuality(30);
        if(!settings.hasDisplay())
            settings.setDisplayFile(R.raw.mi_8);
        if(!settings.isAdjusted()) {

        }

        setContentView(R.layout.activity_main);
        seekBar = (SeekBar) findViewById(R.id.seekBarVideo);
        textDuration = (TextView) findViewById(R.id.textDuration);
        button3D = (Button)findViewById(R.id.button3D);
        buttonPlayPause = (Button)findViewById(R.id.buttonPlayPause);
        buttonStop = (Button)findViewById(R.id.buttonStop);
        buttonGoBack = (Button)findViewById(R.id.backButtonPlayer);
        layoutPlayControls = (LinearLayout)findViewById(R.id.layoutPlayControls);
        layoutScreenComponents = (ConstraintLayout) findViewById(R.id.constraintLayout);

        player = findViewById(R.id.player);

//        player.setView(findViewById(R.id.player));
        player.setPlaybackListener(this);

        player.setInputFormat(ASConstants.InputFormat.SBS);
        player.useFaceDetection(true);
        //if(PackageManager.PERMISSION_DENIED==ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA))
            //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);

        player.setOnClickListener(this);

        player.setFaceDetectionListener(this);

//        player.setImage(BitmapFactory.decodeResource(getResources(), R.drawable.basebat));
//        player.setVideo(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"+getPackageName()+"/raw/"+R.raw.baseball_sbs));

        button3D.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                use3D = !use3D;
                player.set3DMode(use3D);
                button3D.setText(use3D?"2D":"3D");
            }
        });

        buttonPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                paused = !paused;
                if(paused) {
                    player.pause();
                    buttonPlayPause.setText("Play");
                } else {
                    if(stoped){
                        stoped = false;
                    }
                    player.play();
                    buttonPlayPause.setText("Pause");
                }
            }
        });
        buttonGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

//        player.setVideo(Uri.parse("/storage/emulated/0/SOS_Planet.mp4"));
//        player.setVideo(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"+getPackageName()+"/raw/"+R.raw.baseball_sbs));
//        player.setVideo(Uri.parse("https://ks3-cn-beijing.ksyun.com/phantom/upload/m3u8/%E9%98%BF%E5%87%A1%E8%BE%BE/%E9%98%BF%E5%87%A1%E8%BE%BE.m3u8"));
        player.setVideo(Uri.parse("https://ks3-cn-beijing.ksyun.com/phantom/upload/flower/GIRLS%20GENERATION.2012.mp4"));

    }

    @Override
    public void onPrepared() {
        seekBar.setMax((int) player.getDuration());
        seekBar.setProgress(player.getCurrentPos());
        Log.i(TAG, "onPrepared "+seekBar.getProgress()+","+seekBar.getMax());
        int durationTotalSeconds = (int) (player.getDuration()/1000);
        String durationString = String.format("%02d:%02d:%02d", durationTotalSeconds/3600, (durationTotalSeconds % 3600) / 60, durationTotalSeconds%60);
        textDuration.setText("00:00:00/"+durationString);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Log.i(TAG, "onProgressChanged "+i+","+b);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.i(TAG, "onStartTrackingTouch "+seekBar.getProgress());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.i(TAG, "onStopTrackingTouch "+seekBar.getProgress());
                player.seekTo(seekBar.getProgress());
            }
        });
        buttonPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!player.isPlaying()) {
                    if(stoped){
                        stoped = false;
                    }
                    player.play();

                    buttonPlayPause.setText("pause");

                } else {
                    player.pause();
                    buttonPlayPause.setText("play");
                }
            }
        });
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopPressed();
            }
        });
        button3D.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                use3D = !use3D;
                player.set3DMode(use3D);
                button3D.setText(use3D?"2D":"3D");
            }
        });
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        player.onResume();
    }

    @Override
    protected void onPause() {
        player.onPause();
        super.onPause();
    }

    @Override
    public void onClick(View view) {
//        paused = !paused;
//        if(paused)
//            player.pause();
//        else
//            player.play();
        layoutScreenComponents.setVisibility(layoutScreenComponents.getVisibility()==View.VISIBLE?View.INVISIBLE:View.VISIBLE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for(int i=0;i<grantResults.length;i++)
            if(PackageManager.PERMISSION_GRANTED==grantResults[i]) {
                //if(Manifest.permission.CAMERA.equals(permissions[i])) player.useFaceDetection(true);
            }
    }


    private void stopPressed(){
        stoped = true;
        player.stop();
        player.seekTo(0);
        seekBar.setProgress(0);
        onProgressUpdate(0);
        player.onPause();
        buttonPlayPause.setText("play");
    }


    @Override
    public void onProgressUpdate(int ms) {
        seekBar.setProgress(ms);
        int progressTotalSeconds = ms/1000;
        int durationTotalSeconds = (int) (player.getDuration()/1000);
        String durationString = String.format("%02d:%02d:%02d", durationTotalSeconds/3600, (durationTotalSeconds % 3600) / 60, durationTotalSeconds%60);
        String progressDurationString =  String.format("%02d:%02d:%02d", progressTotalSeconds/3600, (progressTotalSeconds % 3600) / 60, progressTotalSeconds%60);
        textDuration.setText(progressDurationString + "/" + durationString);
    }
    @Override
    public void onFinished() {
//        player.setImage(BitmapFactory.decodeResource(getResources(), R.drawable.basebat));
        player.play();
    }

    @Override
    public void onFaceFound() {
        findViewById(R.id.textFace).setVisibility(View.VISIBLE);
    }
    @Override
    public void onFaceLost() {
        findViewById(R.id.textFace).setVisibility(View.INVISIBLE);
    }
}
