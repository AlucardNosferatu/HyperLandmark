package com.tdim.qas;

import android.Manifest;            //API level 1
import android.content.Context;     //API level 1
import android.content.SharedPreferences;   //API level 1

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ASSettings {
    private static final String TAG = RotatableSurfaceView.class.getSimpleName();
    private static final String PG_FILE_NAME = "custompg.bin";
    private final Context context;
    private final SharedPreferences prefs;
    private static SharedPreferences prefs2;

    public ASSettings(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(context.getString(R.string.appId), Context.MODE_PRIVATE);
        prefs2 = context.getSharedPreferences(context.getString(R.string.appId), Context.MODE_PRIVATE);
    }

    public List<File> scanDirectory(String extension, File dir) {return Util.scanDirectory(extension, dir);}
    public List<File> scanRecursive(String extension, File dir) {
        final List<File> list = new ArrayList<>();
        if(Util.hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE))
            Util.scanRecursive(extension, dir, new Util.FileFoundListener() {
                @Override
                public boolean onFileFound(File file) {
                    list.add(file);
                    return false;
                }
            });
        return list;
    }

    public String getDisplayModel(int id) {return getModel(Util.loadRaw(context, id));}
    public String getDisplayModel(File file) {return getModel(Util.loadExternal(file));}
    private String getModel(byte[] data) {
        String model = RotatableSurfaceView.readModel(context, data);
        return null==model?"":model;
    }
    public boolean setDisplayFile(int id) {return setDisplayFile(Util.loadRaw(context, id));}
    public boolean setDisplayFile(File file) {return setDisplayFile(Util.loadExternal(file));}
    private boolean setDisplayFile(byte[] data) {
        RotatableSurfaceView.DisplayParameters params = RotatableSurfaceView.readParameters(context, data);
        if(null!=params) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putFloat(context.getString(R.string.pref_hor_pitch), params.h);
            edit.putFloat(context.getString(R.string.pref_ver_pitch), params.v);
            edit.putInt(context.getString(R.string.pref_pixel_geom), params.pg.getType());
            if(params.pg.isCustom())
                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bos.write(Util.toBytes(params.pg.getType()));
                    params.pg.save(bos);
                    Util.saveInternal(context, PG_FILE_NAME, bos.toByteArray());
                } catch(IOException e) {
                    e.printStackTrace();
                }
            edit.apply();
        }
        return null!=params;
    }
    public void setCameraQuality(int quality) {prefs.edit().putInt(context.getString(R.string.pref_cam_quality), quality).apply();}
    void setAngleError(float error) {prefs.edit().putFloat(context.getString(R.string.pref_angle_error), error).apply();}
    void setViewOffset(float offset) {prefs.edit().putFloat(context.getString(R.string.pref_view_offset), offset).apply();}

    public int getCameraQuality() {return prefs.getInt(context.getString(R.string.pref_cam_quality), 50);}
    public float getAngleError() {return prefs.getFloat(context.getString(R.string.pref_angle_error), 0);}
    public float getViewOffset() {return prefs.getFloat(context.getString(R.string.pref_view_offset), 0);}
    RotatableSurfaceView.DisplayParameters getDisplay() {
        float h = prefs.getFloat(context.getString(R.string.pref_hor_pitch), 0);
        float v = prefs.getFloat(context.getString(R.string.pref_ver_pitch), 0);
        int pgType = prefs.getInt(context.getString(R.string.pref_pixel_geom), 0);
        ASPixelGeometry pg;
        if(ASPixelGeometry.CUSTOM==pgType) {
            ByteArrayInputStream bis = new ByteArrayInputStream(Util.loadInternal(context, PG_FILE_NAME));
            int type = Util.nextInt(bis);
            pg = ASPixelGeometry.load(bis);
        } else
            try {
                pg = ASPixelGeometry.getPixelGeomtery(pgType);
            } catch(IllegalArgumentException e) {
                prefs.edit().remove(context.getString(R.string.pref_pixel_geom)).apply();
                pg = ASPixelGeometry.getPixelGeomtery(ASPixelGeometry.NONE);
            }
        return new RotatableSurfaceView.DisplayParameters(h, v, pg, getAngleError(), getViewOffset());
    }

    public static SharedPreferences getPrefs(){
        return prefs2;
    }

    public boolean hasDisplay() {return hasKey(R.string.pref_hor_pitch)&&hasKey(R.string.pref_ver_pitch)&&hasKey(R.string.pref_pixel_geom);}
    public boolean isAdjusted() {return hasKey(R.string.pref_angle_error)&&hasKey(R.string.pref_view_offset);}
    private boolean hasKey(int id) {return prefs.contains(context.getString(id));}
}
