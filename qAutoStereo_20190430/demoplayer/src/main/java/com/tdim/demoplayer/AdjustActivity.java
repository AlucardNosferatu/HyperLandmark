package com.tdim.demoplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.view.WindowManager;
import android.content.Intent;

import com.tdim.qas.ASAdjustmentView;
import com.tdim.qas.ASDetectionListener;
import com.tdim.qas.ASSettings;

public class AdjustActivity extends AppCompatActivity implements View.OnClickListener, ASDetectionListener {
    private static final float ERROR_RADS = (float)(Math.PI/(60*180));//arcmin
    private static final float ERROR_VIEW = 0.125f;
    private static ASAdjustmentView.AdjustMode mode = ASAdjustmentView.AdjustMode.Bars;
    private ASAdjustmentView adjust;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        setContentView(R.layout.activity_adjust);

        ASSettings settings = new ASSettings(getBaseContext());
        if(!settings.hasDisplay()) {
            settings.setDisplayFile(R.raw.redmi_note_5);
        }

        adjust = findViewById(R.id.adjust);
        adjust.setBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.basebat));
        adjust.setMode(mode);
        adjust.setOnClickListener(this);
        adjust.setFaceDetectionListener(this);

        if(PackageManager.PERMISSION_DENIED==ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA))
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(0<grantResults.length&&PackageManager.PERMISSION_GRANTED==grantResults[0])
            ;
    }

    @Override
    public void onFaceFound() {
        findViewById(R.id.textFace).setVisibility(View.VISIBLE);
    }
    @Override
    public void onFaceLost() {
        findViewById(R.id.textFace).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.errorInc:
                if(ASAdjustmentView.AdjustMode.Bars==mode)
                    adjust.setAngleError(adjust.getAngleError()+ERROR_RADS);
                else
                    adjust.setViewOffset(adjust.getViewOffset()+ERROR_VIEW);
                break;
            case R.id.errorDec:
                if(ASAdjustmentView.AdjustMode.Bars==mode)
                    adjust.setAngleError(adjust.getAngleError()-ERROR_RADS);
                else
                    adjust.setViewOffset(adjust.getViewOffset()-ERROR_VIEW);
                break;
            case R.id.MirrorButton:
                Intent mirrorIntent = new Intent(this, MirrorActivity.class);
                startActivity(mirrorIntent);
                break;


            default:
                mode = ASAdjustmentView.AdjustMode.Bars==mode? ASAdjustmentView.AdjustMode.Picture: ASAdjustmentView.AdjustMode.Bars;
                adjust.setMode(mode);
                break;
        }
    }
}
