package com.tdim.qas;

import android.content.Context;     //API level 1
import android.graphics.PointF;     //API level 1
import android.os.Build;
import android.os.Handler;          //API level 1
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;            //API level 1
import android.view.SurfaceHolder;  //API level 1

import java.util.Arrays;

public abstract class FaceDetectorBase implements CameraBase.SessionListener {
    private static final float EYE_DISTANCE = 65;//in millimetres
    private static final String TAG = FaceDetectorBase.class.getSimpleName();
    public interface EyePositionListener {
        void onEyePosition(@Nullable PointF left, @Nullable PointF right);//coordinates in range [0,1]
    }
    public interface ViewOffsetListener {
        void onViewOffset(float offset);
    }
    static {System.loadLibrary("qas-native");}

    final Context context;
    private final Handler mainHandler;
    private final CameraBase cameraInput;
    private ASDetectionListener faceListener;
    private EyePositionListener eyeListener;
    private ViewOffsetListener offsetListener;
    private int orientation;
    private boolean hasFace;
    private float[] matrix = new float[3*2];//TRS matrix
    private float baseX, baseY;
    int inputWidth, inputHeight, inputFormat;

    FaceDetectorBase(Context context) {
        this.context = context;
        mainHandler = new Handler();
        //TODO fix camera2
        cameraInput = Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP?new CameraLegacy(context):new Camera2(context);
        //cameraInput = new CameraLegacy(context);
        cameraInput.setSessionListener(this);
    }

    final void setFaceListener(ASDetectionListener listener) {faceListener = listener;}
    final void setEyeListener(EyePositionListener listener) {eyeListener = listener;}
    final void setOffsetListener(ViewOffsetListener listener) {offsetListener = listener;}

    final void setDeviceOrientation(int orientation) {
        this.orientation = orientation;//TODO if rotation locked
        updateRotation();
    }
    final void setDeviceRotation(int rotation) {
        cameraInput.setDeviceRotation(rotation);
        updateRotation();
    }

    @Override
    public void onInit(int width, int height, int format) {
        inputWidth = width;
        inputHeight= height;
        inputFormat= format;
        Log.i(TAG, "Preview "+width+"x"+height+", format "+Integer.toHexString(format));
        updatePreviewParams(cameraInput.getPreviewRotation());
    }

    CameraBase.Resolution getResolution() {return cameraInput.getRequestedResolution();}
    float getCameraAspect() {return cameraInput.getRotatedAspect();}

    abstract boolean isOperational();
    abstract void setPreviewRotation(int rotation);//rotate face detection accordingly
    abstract void release();

    double getFocalLength() {return cameraInput.getFocalLength();}

    void startDetection() {startDetection(null);}
    void startDetection(SurfaceHolder holder) {
        cameraInput.startSession(holder);//updates camera rotation and focal length
        updateRotation();
    }
    void stopDetection() {cameraInput.stopSession();}

    private void updateRotation() {
        int rotation = cameraInput.getPreviewRotation();
        setPreviewRotation(rotation);
        updatePreviewParams(rotation);
        setFaceFound(false);
    }

    private void updatePreviewParams(int rotation) {
        int w, h;
        if(RotatableSurfaceView.isPortrait(rotation)) {
            w = inputWidth;
            h = inputHeight;
        } else {
            w = inputHeight;
            h = inputWidth;
        }
        baseX = 0.5f*w;
        baseY = 0.5f*h;
        float factX, factY;
        factX = 0<w?1.f/w:0;
        factY = 0<h?1.f/h:0;
        Arrays.fill(matrix, 0);
        matrix[0] = factX;
        matrix[4] = factY;
        /*
        switch(orientation) {
            case 0:
                matrix[0] = factX;
                matrix[4] = factY;
                break;
            case 1:
                matrix[1] = factX;
                matrix[3] = -factY;
                matrix[5] = 1;
                break;
            case 2:
                matrix[0] = -factX;
                matrix[4] = -factY;
                matrix[2]=matrix[5] = 1;
                break;
            case 3:
                matrix[1] = -factX;
                matrix[3] = factY;
                matrix[2] = 1;
                break;
        }*/
    }

    void setFaceFound(boolean state) {
        if(hasFace==state)
            return;
        hasFace = state;
        if(null!=faceListener) {
            if(hasFace)
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        faceListener.onFaceFound();}
                });
            else
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        faceListener.onFaceLost();}
                });
        }
    }
    void onEyesDetected(final PointF left, final PointF right) {//pixel coordinates
        if(null!=eyeListener)
            mainHandler.post(new Runnable() {
                                 @Override
                                 public void run() {eyeListener.onEyePosition(applyRS(left), applyRS(right));}
                             });
        if(null==offsetListener||null==left||null==right)
            return;

        float delta = (float)Math.hypot(left.x-right.x, left.y-right.y);
        if(1>delta)//eye distance less than 1 pixel
            return;

        float mm_px = EYE_DISTANCE/delta;//mm per px at eye depth
        //float faceDist = (float)(mm_px*getFocalLength());
        final float voff = mm_px*(baseX-0.5f*(left.x+right.x))/(2*EYE_DISTANCE);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                offsetListener.onViewOffset(voff);
            }
        });
    }
    private PointF applyRS(PointF point) {
        return null==point?null:new PointF(
                matrix[0]*point.x+matrix[1]*point.y+matrix[2],
                matrix[3]*point.x+matrix[4]*point.y+matrix[5]);
    }
}
