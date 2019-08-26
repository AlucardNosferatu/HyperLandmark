package com.tdim.qas;

import android.content.Context;         //API level 1
import android.graphics.Point;          //API level 1
import android.hardware.SensorManager;  //API level 1
import android.opengl.GLSurfaceView;    //API level 3
import android.os.Build;                //API level 1
import androidx.annotation.NonNull;
import android.util.AttributeSet;       //API level 1
import android.util.Log;                //API level 1
import android.view.Display;            //API level 1
import android.view.OrientationEventListener;   //API level 3
import android.view.ViewTreeObserver;   //API level 1
import android.view.WindowManager;      //API level 1

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

abstract class RotatableSurfaceView extends GLSurfaceView implements ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = RotatableSurfaceView.class.getSimpleName();
    private static final int VERSION = 2;
    private static final int LANDSCAPE_MASK = 0x01;
    private static final int PITCH_FACT = 1<<29;
    static class DisplayParameters {
        final float h, v;
        final float error, offset;
        final ASPixelGeometry pg;
        DisplayParameters(float h, float v, ASPixelGeometry pg){this(h, v, pg, 0, 0);}
        DisplayParameters(float h, float v, ASPixelGeometry pg, float error, float offset) {
            this.h = h;
            this.v = v;
            this.pg = pg;
            this.error = error;
            this.offset = offset;
        }
    }
    static boolean isPortrait(int rotation) {return 0==(LANDSCAPE_MASK&rotation);}
    static boolean isSameOrientation(int rotation0, int rotation1) {//true if both are portrait or both are landscape
        return (LANDSCAPE_MASK&rotation0)==(LANDSCAPE_MASK&rotation1);
    }

    private final Display device;
    private final OrientationEventListener rotationListener;
    private final int defaultRotation;//TODO default orientation
    private int rotation;
    private ASDeviceReversedListener listener;
    private com.tdim.qas.Renderer renderer;

    public RotatableSurfaceView(Context context) {this(context, null);}
    public RotatableSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        defaultRotation = getResources().getConfiguration().orientation;
        device = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        rotationListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                int currRotation = device.getRotation();
                if(currRotation!=rotation&&isSameOrientation(currRotation, rotation)) {
                    rotation = currRotation;
                    onRotation(rotation);
                    if(null!=listener)
                        listener.onDeviceReversed();
                }
            }
        };
        rotation = device.getRotation();
        Log.i(TAG, "Rotation "+(90*rotation)+" (default "+(90*defaultRotation)+")");
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    public int getDeviceRotation() {return rotation;}
    public void setDeviceReversedListener(ASDeviceReversedListener listener) {this.listener = listener;}
    public boolean isPortrait() {return RotatableSurfaceView.isPortrait(rotation);}
    public boolean isLandscape() {return !isPortrait();}

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        rotation = device.getRotation();
        onRotation(rotation);
        rotationListener.enable();
        setDisplay(new ASSettings(getContext()).getDisplay());
    }
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        rotationListener.disable();
    }

    @Override
    public void setRenderer(Renderer renderer) {
        super.setRenderer(renderer);
        if(renderer instanceof com.tdim.qas.Renderer)
            this.renderer = (com.tdim.qas.Renderer)renderer;
        else
            throw new IllegalArgumentException();
    }

    @Override
    public void onGlobalLayout() {
        int[] tl = new int[2];
        Point wh = new Point();
        getLocationOnScreen(tl);
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.JELLY_BEAN_MR1)
            device.getSize(wh);
        else
            device.getRealSize(wh);
        renderer.setScreenMargins(new int[]{tl[0], tl[1], wh.x-getWidth()-tl[0], wh.y-getHeight()-tl[1]});
    }

    protected void onRotation(int rotation) {
        renderer.setRotation(rotation);
        onGlobalLayout(); //if reversed, pg matrix offset may change
    }

    private static byte[] transform(Context context, int opmode, byte[] input) {
        try {
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] key = Util.loadRaw(context, R.raw.aeskey);
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] iv = Arrays.copyOf(sha.digest(context.getString(R.string.calibIO).getBytes()), c.getBlockSize());
            c.init(opmode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return c.doFinal(input);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected byte[] saveExternal(Context context, float h, float v, ASPixelGeometry pg) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PixelPitch pitches = new PixelPitch(h, v);
        if(isLandscape())
            pitches.rotate();
        try {
            os.write("QASC".getBytes());
            os.write(Util.toBytes(VERSION));
            os.write(Util.toBytes(Build.BRAND));
            os.write(Util.toBytes(Build.MODEL));
            os.write(Util.toBytes((int)(PITCH_FACT*pitches.h())));
            os.write(Util.toBytes((int)(PITCH_FACT*pitches.v())));
            os.write(Util.toBytes(pg.getType()));
            if(pg.isCustom())
                pg.save(os);
            return transform(context, Cipher.ENCRYPT_MODE, os.toByteArray());
        } catch(IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static int checkHeader(InputStream is) {
        if(!"QASC".equals(new String(Util.nextBytes(is, 4)))) {
            Log.e(TAG, "Not a valid qasc file");
            return -1;
        }
        int version = Util.nextInt(is);
        if(VERSION<version) {
            Log.e(TAG, "Qasc file version "+version+" newer than SDK version "+VERSION);
            return -1;
        }
        return version;
    }
    protected static String readModel(Context context, byte[] buf) {
        byte[] contents = transform(context, Cipher.DECRYPT_MODE, buf);
        if(null==contents) //decryption error
            return null;
        ByteArrayInputStream is = new ByteArrayInputStream(contents);
        if(0>checkHeader(is))
            return null;
        String brand = Util.nextString(is);
        String model = Util.nextString(is);
        return model;
    }
    protected static DisplayParameters readParameters(Context context, byte[] buf) {
        byte[] contents = transform(context, Cipher.DECRYPT_MODE, buf);
        if(null==contents) //decryption error
            return null;
        ByteArrayInputStream is = new ByteArrayInputStream(contents);
        int version = checkHeader(is);
        if(0>version)
            return null;
        String brand = Util.nextString(is);
        String model = Util.nextString(is);
        if(!Build.BRAND.equals(brand)||!Build.MODEL.equals(model))
            Log.w(TAG, "Qasc device "+brand+" "+model+" doesn't match this device "+brand+" "+model);

        float h = Util.nextInt(is)/(float)PITCH_FACT;
        float v = Util.nextInt(is)/(float)PITCH_FACT;
        int pgType = Util.nextInt(is);
        ASPixelGeometry pg;
        if(1<version&&ASPixelGeometry.CUSTOM==pgType) //custom
            pg = ASPixelGeometry.load(is);
        else
            try {
                pg = ASPixelGeometry.getPixelGeomtery(pgType);
            } catch(IllegalArgumentException e) {
                Log.w(TAG, "Unknown pixel geometry");
                pg = ASPixelGeometry.getPixelGeomtery(ASPixelGeometry.NONE);
            }
        return new DisplayParameters(h, v, pg);
    }
    abstract void setDisplay(@NonNull DisplayParameters params);
}
