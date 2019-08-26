package com.tdim.qas;

import android.annotation.SuppressLint;
import android.content.Context;             //API level 1
import android.content.SharedPreferences;   //API level 1
import android.os.Environment;              //API level 1
import android.provider.Settings.Secure;    //API level 1
import android.util.Log;                    //API level 1

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

final public class ASLicensing {
    private static final String TAG = ASLicensing.class.getSimpleName();
    private static final long MS_PER_HOUR = 1000*60*60;
	private static final String LICENSE_FILE_NAME = "license.qlic";
    private static final String SHARED_PREF_NAME = "com.tdim.qas.LICENSE";
    private static final String EXTENSION = ".qlic";
    public enum LicenseState {
        Locked,
        Demo,
        Activated
    }

    private static boolean initialized = false;
    private static String androidID = "";
    private static String expirationID;
    private static volatile LicenseState license = LicenseState.Activated; //LicenseState.Locked
    private static volatile Date expiration;

    private final Context context;
    private final PublicKey publicKey;

    private static String getCanonicalName() {return ASLicensing.class.getCanonicalName();}
    private static String getSimpleName() {return ASLicensing.class.getSimpleName();}

	public ASLicensing(Context context) {
        this.context = context;
        publicKey = generatePublicKey(context);
        initialize(context, publicKey);
	}

    public static void initialize(Context context) {initialize(context, null);}
    @SuppressLint("HardwareIds")
    private static void initialize(Context context, PublicKey publicKey) {
        if(initialized)
            return;
        androidID = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
        if(null==publicKey)
            publicKey = generatePublicKey(context);
        initialized = true;
        new File(context.getFilesDir(), LICENSE_FILE_NAME).delete();
        checkLicense(context, publicKey, Util.loadInternal(context, LICENSE_FILE_NAME));
    }

    private static void checkInitialized() {
        if(!initialized) {
            String className = getSimpleName();
            throw new IllegalStateException(getCanonicalName()+": State not initialized. Call "+className+".initialize(context) or new "+className+"(context)");
        }
    }
    public static String getAndroidID() {
        checkInitialized();
        return androidID;
    }
    public static LicenseState getLicenseState() {
        checkInitialized();
        return license;
    }
    public static int getTrialHoursLeft() {
        checkInitialized();
        if(null==expiration)
            return -1;
        else {
            long msdiff = expiration.getTime()-new Date().getTime();
            return 0<msdiff?(int)(msdiff/MS_PER_HOUR):-1;
        }
    }

    public boolean scanDirectory(File dir) {
        for(File file: Util.scanDirectory(EXTENSION, dir))
            if(installLicense(file))
                break;
        return LicenseState.Locked!=getLicenseState();
    }
	public boolean scanRecursive(File dir) {
        Util.scanRecursive(EXTENSION, dir, new Util.FileFoundListener() {
            @Override
            public boolean onFileFound(File file) {return installLicense(file);}
        });
        return LicenseState.Locked!=getLicenseState();
	}
	public boolean installLicense(File file) {
        Log.i(TAG, "Reading "+ file.getAbsolutePath()+"...");
        boolean success = installLicense(Util.loadExternal(file));
        Log.i(TAG, success?"Successfully installed license":"Invalid license file");
        return success;
	}
    public boolean installLicense(byte[] data) {
        return checkLicense(context, publicKey, data)&&Util.saveInternal(context, LICENSE_FILE_NAME, data);
    }

    static boolean isValid(Context context) {
        if(LicenseState.Locked==license) {
            return false;
        } else if(null==expiration)//permanent license
            return true;
        else {//limited license or demo
            String pref_key_lastcheck = "fini_"+expirationID;
            SharedPreferences pref = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            final Date now = new Date();
            if(now.before(new Date(pref.getLong(pref_key_lastcheck, now.getTime()+1)))) {//system time manipulation
                editor.remove(pref_key_lastcheck);
                expiration = now;
            } else
                editor.putLong(pref_key_lastcheck, now.getTime());
            editor.apply();
            return now.before(expiration);
        }
    }
/*
    private static byte[] encodeLic(Context context) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(Util.toBytes(sha.digest(context.getString(R.string.appId).getBytes())));
            bos.write(Util.toBytes(sha.digest(getAndroidID().getBytes())));
            String trialId = "123456789ABCDEF";
            bos.write(Util.toBytes(trialId));
            bos.write(Util.toBytes(1));//trial days
            byte[] data = bos.toByteArray();

            byte[] key = Util.loadRaw(context, R.raw.key_w);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key);
            KeyFactory kf = KeyFactory.getInstance("DSA");
            Signature signature = Signature.getInstance("DSA");
            signature.initSign(kf.generatePrivate(keySpec));
            signature.update(data);
            byte[] sign = signature.sign();

            bos.reset();
            bos.write("QLIC".getBytes());
            bos.write(Util.toBytes(1));//version
            bos.write(Util.toBytes(sign));
            bos.write(Util.toBytes(data));
            return bos.toByteArray();
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }*/
	private static boolean checkLicense(Context context, PublicKey publicKey, byte[] input) {
        if(null==input||0==input.length)
            return false;

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(input);
            if(!Arrays.equals("QLIC".getBytes(), Util.nextBytes(bis, 4)))
                return false;//not a license file
            int version = Util.nextInt(bis);
            if(1!=version)
                return false;//unsupported version
            byte[] sign = Util.nextChunk(bis);
            byte[] data = Util.nextChunk(bis);

            Signature signature = Signature.getInstance("DSA");
            signature.initVerify(publicKey);
            signature.update(data);
            if(!signature.verify(sign))
                return false;//file has been manipulated

            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            bis = new ByteArrayInputStream(data);
            if(!Arrays.equals(Util.nextChunk(bis), sha.digest(context.getString(R.string.appId).getBytes())))
                return false;//wrong app id
            if(!Arrays.equals(Util.nextChunk(bis), sha.digest(getAndroidID().getBytes())))
                return false;//wrong android id
            String trialId = Util.nextString(bis);
            int trialDays = Util.nextInt(bis);
            if(trialId.isEmpty()||0>trialDays)
                return false; //no unlimited licences

            Date init = getActivationDate(context, trialId);//is null if invalid, e.g. clock reset
            if(null!=init) {
                Calendar exp = Calendar.getInstance();
                exp.setTime(init);
                exp.add(Calendar.DAY_OF_MONTH, trialDays);
                expiration = exp.getTime();
                expirationID = trialId;
                if(new Date().before(expiration))
                    license = LicenseState.Activated;
            }
            return LicenseState.Locked!=license;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }

    }

	private static Date getActivationDate(Context context, String id) {
        if(null==id||id.isEmpty())
            return null;
        String pref_key_activate = "init_"+id;
        String pref_key_lastcheck ="fini_"+id;
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        final Date now = new Date();
        Date start = null;
        if(!pref.contains(pref_key_activate)) {//initial activation
            if(checkIdExternal(id)) {//was already activated before data delete
                Log.e(TAG, "License file already used.");
                editor.putLong(pref_key_activate, 0);
            } else {
                Calendar base = Calendar.getInstance();
                base.setTime(now);//set to current date, then reset to midnight
                base.set(Calendar.HOUR_OF_DAY, 0);
                base.clear(Calendar.MINUTE);
                base.clear(Calendar.SECOND);
                base.clear(Calendar.MILLISECOND);
                start = base.getTime();
                editor.putLong(pref_key_activate, start.getTime());
                editor.putLong(pref_key_lastcheck, now.getTime());
            }
        } else if(pref.contains(pref_key_lastcheck)) {
            Date last = new Date(pref.getLong(pref_key_lastcheck, now.getTime()+1));
            if(now.before(last)) {//system time manipulation -> remove pref
                Log.e(TAG, "System clock has been manipulated. Removing license.");
                editor.remove(pref_key_lastcheck);
            } else {
                start = new Date(pref.getLong(pref_key_activate, 0));
                editor.putLong(pref_key_lastcheck, now.getTime());
            }
        } //else clock manipulated -> leads to pref removal
        if(!checkIdExternal(id))
            Util.saveExternal(getIdExternalFile(), true, (id+"\n").getBytes());
        editor.apply();
        return start;
    }

    private static String getIdExternalDirectory() {return Environment.getExternalStorageDirectory()+File.separator+"Dim3D";}
    private static String getIdExternalFile() {return getIdExternalDirectory()+File.separator+"."+getAndroidID();}
    private static boolean checkIdExternal(String id) {
        byte[] data = Util.loadExternal(getIdExternalFile());
        for(String line: new String(data).split("\n"))
            if(line.equals(id))
                return true;
        return false;
    }

    private static PublicKey generatePublicKey(Context context) {
        try {
            byte[] key = Util.loadRaw(context, R.raw.key_r);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
            KeyFactory kf = KeyFactory.getInstance("DSA");
            return kf.generatePublic(keySpec);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
