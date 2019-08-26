package com.tdim.qCalibration;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.tdim.qas.ASLicensing;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class InitTask extends AsyncTask<Void, Void, Void> {
    private static final int SPLASH_DURATION = 1000; //in ms

    interface LicenseListener {
        void setLicenseState(ASLicensing.LicenseState state);
    }

	private WeakReference<Activity> activityRef;

	InitTask(Activity startActivity) {activityRef = new WeakReference<>(startActivity);}

	@Override
	protected Void doInBackground(Void... params) {
		final Activity activity = null==activityRef?null:activityRef.get();
		if(null==activity)
			return null;

        List<File> recurseList = new ArrayList<>();
        recurseList.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        File qDir = MainActivity.getExternalStorageDirectory();
        if(qDir.exists()||qDir.mkdir())
            recurseList.add(qDir);

        final ASLicensing lm = new ASLicensing(activity);
		boolean licenseFound = ASLicensing.LicenseState.Activated==ASLicensing.getLicenseState()
                ||lm.scanDirectory(null);//search external root directory /sdcard
        Iterator<File> iter = recurseList.iterator();
        while(!licenseFound&&iter.hasNext())
            licenseFound = lm.scanRecursive(iter.next());

        Handler handler = new Handler(Looper.getMainLooper());
		if(activity instanceof LicenseListener)
            handler.post(new Runnable() {
				@Override
				public void run() {((LicenseListener)activity).setLicenseState(ASLicensing.getLicenseState());}
			});
		if(licenseFound)
            handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					activity.startActivity(new Intent(activity, MainActivity.class));
					activity.finish();
				}
			}, SPLASH_DURATION);
		return null;
	}
}
