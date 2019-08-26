package com.tdim.qas;

import android.content.Context;     //API level 1
import android.graphics.Bitmap;     //API level 1
import android.opengl.GLES20;       //API level 8
import androidx.annotation.NonNull;
import android.util.AttributeSet;   //API level 1

import javax.microedition.khronos.opengles.GL10;

public final class ASAdjustmentView extends RotatableSurfaceView implements FaceDetectorBase.ViewOffsetListener {
    public enum AdjustMode {
        Bars,
        Picture,
    }
    private class AdjustRenderer extends CalibratableRenderer {
        private int unilocView = -1;//float of view offset

        private float pitch;
        private double angle;
        private float angleError;
        private float baseOffset;//base view offset
        private float eyeOffset;//view offset due to viewer position

        AdjustRenderer(ASAdjustmentView view) {super(view);}

        @Override
        void updateUniforms(GL10 glUnused) {
            unilocView = GLES20.glGetUniformLocation(program, "u_view");
        }
        @Override
        void setUniforms(GL10 glUnused) {
            GLES20.glUniform1f(unilocView, baseOffset+eyeOffset);
        }

        void setDisplay(PixelPitch pitches, ASPixelGeometry pg) {
            setPixelGeometry(pg);
            pitch = pitches.getLenticularPitch();
            angle = pitches.getAngle();
            setAngleError(0);
        }

        void setMode(AdjustMode mode) {
            switch(mode) {
                default:
                    throw new IllegalArgumentException("Unsupported mode "+mode);
                case Bars:
                    super.setMode(Mode.Bars);
                    break;
                case Picture:
                    super.setMode(Mode.Picture);
                    break;
            }
        }

        void setAngleError(float error) {
            angleError = error;
            setPitches(PixelPitch.fromPitchAngle(pitch, angle+angleError));
        }
        void setViewOffset(float offset) {
            baseOffset = offset-(float)Math.floor(offset);
            requestRender();
        }
        void setEyeOffset(float offset) {
            eyeOffset = offset-(float)Math.floor(offset);
            requestRender();
        }

        float getAngleError() {return angleError;}
        float getViewOffset() {return baseOffset;}
    }

    private final AdjustRenderer renderer;
    private final ASSettings settings;
    private boolean useDetection = true;
    private FaceDetectorBase detect;

    public ASAdjustmentView(Context context) {this(context, null);}
    public ASAdjustmentView(Context context, AttributeSet attr) {
        super(context, attr);
        settings = new ASSettings(context);
        if(isInEditMode())
            renderer = null;
        else {
            setEGLContextClientVersion(2);
            setEGLConfigChooser(false);
            renderer = new AdjustRenderer(this);
            setRenderer(renderer);
            renderer.setRotation(getDeviceRotation());
            setRenderMode(RENDERMODE_WHEN_DIRTY);
            detect = FaceDetectorFactory.createFaceDetector(context);
            detect.setOffsetListener(this);
            detect.setDeviceRotation(getDeviceRotation());
        }
    }

    public void setEyeListener(FaceDetectorBase.EyePositionListener listener){
        detect.setEyeListener(listener);
    }
    public void setOffsetListener(FaceDetectorBase.ViewOffsetListener listener){
        detect.setOffsetListener(listener);
    }

    public void setFaceDetectionListener(ASDetectionListener listener) {detect.setFaceListener(listener);}
    public void useFaceDetection(boolean state) {
        useDetection = state;
        if(useDetection)
            detect.startDetection();
        else
            detect.stopDetection();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(useDetection) detect.startDetection();
    }
    @Override
    public void onResume() {
        super.onResume();
        //if(useDetection) detect.startDetection();
    }
    @Override
    public void onPause() {
        detect.stopDetection();
        //super.onPause();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        detect.stopDetection();
    }

    @Override
    protected void onRotation(int rotation) {
        super.onRotation(rotation);
        detect.setDeviceRotation(rotation);
    }

    @Override
    void setDisplay(@NonNull DisplayParameters params) {
        PixelPitch pitches = new PixelPitch(params.h, params.v);
        if(isLandscape())
            pitches.rotate();
        renderer.setDisplay(pitches, params.pg);
        renderer.setAngleError(params.error);
        renderer.setViewOffset(params.offset);
    }
    @Override
    public void onViewOffset(float offset) {renderer.setEyeOffset(offset);}

    public void setBitmap(Bitmap bmp) {renderer.setBitmap(bmp);}

    public void setMode(AdjustMode mode) {
        renderer.setMode(mode);
    }
    public void setAngleError(float rad) {
        renderer.setAngleError(rad);
        settings.setAngleError(rad);
    }
    public void setViewOffset(float offset) {
        renderer.setViewOffset(offset);
        settings.setViewOffset(offset);
    }

    public float getAngleError() {return renderer.getAngleError();}
    public float getViewOffset() {return renderer.getViewOffset();}
}
