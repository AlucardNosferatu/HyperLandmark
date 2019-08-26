package com.tdim.qCalibration;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import com.tdim.qas.ASLicensing;

public class SplashActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback, InitTask.LicenseListener {
	private static final int CODE_STORAGE_WRITE = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		ASLicensing.initialize(getApplicationContext());
        ((TextView)findViewById(R.id.paramMID)).setText(ASLicensing.getAndroidID());
        findViewById(R.id.layoutShare).setVisibility(View.INVISIBLE);

        if(PackageManager.PERMISSION_DENIED==ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, CODE_STORAGE_WRITE);
        else
            execStorageWrite();
	}
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if(CODE_STORAGE_WRITE==requestCode)  {
            if(0<grantResults.length&&PackageManager.PERMISSION_GRANTED==grantResults[0])
                execStorageWrite();
            else
                finish();
		}
	}
	private void execStorageWrite() {new InitTask(this).execute();}


	@Override
	public void setLicenseState(ASLicensing.LicenseState state) {
		int hours = ASLicensing.getTrialHoursLeft();
		String add = -1<hours?"("+hours+" hours left)":"";
		StringBuilder sb = new StringBuilder(getLicenseState(state));
		if(-1<hours)
			sb.append(getString(R.string.license_timeleft).replace("%1", Integer.toString(hours)));
		((TextView)findViewById(R.id.paramState)).setText(sb.toString());
		if(ASLicensing.LicenseState.Locked==state) {
            initShare(R.id.imageWechat, "com.tencent.mm");
            initShare(R.id.imageWhatsapp, "com.whatsapp");
            initShare(R.id.imageSkype, "com.skype.raider");
            findViewById(R.id.imageShare).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {startShareActivity(null);}
            });
            findViewById(R.id.layoutShare).setVisibility(View.VISIBLE);
        }
	}
	private String getLicenseState(ASLicensing.LicenseState state) {
		switch(state) {
			case Activated:	return getString(R.string.license_activated);
			case Demo:		return getString(R.string.license_demo);
			case Locked:	return getString(R.string.license_locked);
			default:
				throw new IllegalArgumentException();
		}
	}
	private void initShare(int id, final String app) {
		View button = findViewById(id);
		try {
			getPackageManager().getPackageInfo(app, PackageManager.GET_RECEIVERS);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {startShareActivity(app);}
			});
		} catch(PackageManager.NameNotFoundException e) {//app not installed
			button.setVisibility(View.GONE);
		}
	}
	private void startShareActivity(@Nullable String app) {
		Intent share = new Intent(Intent.ACTION_SEND);
		share.setType("text/plain");
		share.putExtra(Intent.EXTRA_TEXT, ASLicensing.getAndroidID());
		if(null==app)
			startActivity(Intent.createChooser(share, getString(R.string.action_share)));
		else {
			share.setPackage(app);
			startActivity(share);
		}
	}
}
