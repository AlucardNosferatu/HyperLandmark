package com.tdim.qas;

import android.content.Context;     //API level 1
import android.graphics.ImageFormat;//API level 8
import android.graphics.PointF;     //API level 1
import android.util.Log;            //API level 1
import android.util.SparseArray;    //API level 1

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.nio.ByteBuffer;

public final class FaceDetectorMV extends FaceDetectorBase {
    private static final String TAG = FaceDetectorMV.class.getSimpleName();
    private final Frame.Builder builder = new Frame.Builder();
    private FaceDetector tracker;
    private int faceId = -1;//currently tracked face

    FaceDetectorMV(Context context) {
        super(context);
        tracker = new FaceDetector.Builder(context)
                .setTrackingEnabled(true)
                .setProminentFaceOnly(true)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();
        if(!tracker.isOperational()) {
            Log.e(TAG, "Cannot initialize Mobile Vision FaceDetector");
            release();
        }
    }

    @Override
    boolean isOperational() {return null!=tracker;}
    @Override
    void setPreviewRotation(int rotation) {builder.setRotation(rotation);}
    @Override
    void release() {
        if(null!=tracker) {
            tracker.release();
            tracker = null;
        }
    }

    @Override
    public void onFrame(ByteBuffer bufY, int width, int height, int format) {//supports NV16, NV21, YV12
        if(null==tracker)
            return;
        Frame frame = builder.setImageData(bufY, width, height, ImageFormat.YV12).build();
        SparseArray<Face> faces = tracker.detect(frame);
        int id = -1;
        PointF left=null, right=null;
        if(0<faces.size()) {
            Face face = faces.valueAt(0);
            id = face.getId();
            for(Landmark lm: face.getLandmarks()) {
                if(Landmark.RIGHT_EYE==lm.getType())
                    right = lm.getPosition();
                else if(Landmark.LEFT_EYE==lm.getType())
                    left = lm.getPosition();
            }
        }

        setFaceFound(-1<id);
        if(id!=faceId) {
            if(-1<id)
                try {//TODO can throw if new detector created
                    tracker.setFocus(id);//track this face
                } catch(NullPointerException e) {
                    Log.e(TAG, "Could not track face "+id+" (previously "+faceId+")");
                }
            faceId = id;
        }
        onEyesDetected(left, right);
    }

    private static String serviceStatusName(int service) {
        switch(service) {
            default:
                return "Unknown 0x"+Integer.toHexString(service);
            case ConnectionResult.SUCCESS:          return "Available";
            case ConnectionResult.SERVICE_UPDATING: return "Updating";
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:  return "Update required";
            case ConnectionResult.SERVICE_MISSING:  return "Missing";
            case ConnectionResult.SERVICE_INVALID:  return "Invalid";
            case ConnectionResult.SERVICE_DISABLED: return "Disabled";
        }
    }
    static boolean hasPlayServices(Context context) {
        int service = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
        String msg = "Play Services Status: "+serviceStatusName(service);
        switch(service) {
            default:
            case ConnectionResult.SERVICE_MISSING:
            case ConnectionResult.SERVICE_INVALID:
            case ConnectionResult.SERVICE_DISABLED:
                Log.e(TAG, msg);
                return false;
            case ConnectionResult.SUCCESS:
            case ConnectionResult.SERVICE_UPDATING:
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                Log.i(TAG, msg);
                return true;
        }
    }
}
