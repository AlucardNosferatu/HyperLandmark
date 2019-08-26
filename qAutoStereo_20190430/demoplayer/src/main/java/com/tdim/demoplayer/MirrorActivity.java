package com.tdim.demoplayer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.view.WindowManager;

import com.tdim.qas.ASMirrorStream;

public class MirrorActivity extends AppCompatActivity implements View.OnClickListener {
    private ASMirrorStream mirror;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_mirror);
        mirror = findViewById(R.id.mirror);

        mirror.setOnClickListener(this);

        if(PackageManager.PERMISSION_DENIED== ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA))
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
    }


    @Override
    public void onClick(View view) {
        switch(view.getId()) {


            default:
                Intent mirrorIntent = new Intent(this, AdjustActivity.class);
                startActivity(mirrorIntent);
                break;
        }
    }


}