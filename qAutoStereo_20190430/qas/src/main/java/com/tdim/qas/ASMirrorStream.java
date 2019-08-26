package com.tdim.qas;

import android.content.Context;         //API level 1
import android.graphics.Canvas;         //API level 1
import android.graphics.Paint;          //API level 1
import android.graphics.PointF;         //API level 1
import android.hardware.SensorManager;  //API level 1
import android.provider.Settings;       //API level 1
import androidx.annotation.Nullable;
import android.util.AttributeSet;       //API level 1
import android.util.Log;                //API level 1
import android.view.Display;            //API level 1
import android.view.OrientationEventListener;   //API level 3
import android.view.SurfaceHolder;      //API level 1
import android.view.SurfaceView;        //API level 1
import android.view.WindowManager;      //API level 1

public final class ASMirrorStream extends SurfaceView implements SurfaceHolder.Callback, FaceDetectorBase.EyePositionListener {
    private static final String TAG = ASMirrorStream.class.getSimpleName();
    private final Display device;
    private final OrientationEventListener rotationListener;
    private final Paint paint = new Paint();
    private int rotation;
    private ASDeviceReversedListener listener;
    private FaceDetectorBase detect;
    private PointF left, right;

    public ASMirrorStream(Context context) {this(context, null);}
    public ASMirrorStream(Context context, AttributeSet attr) {
        super(context, attr);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(FaceDetectorMV.hasPlayServices(context)?0xFFFF0000:0xFF0000FF);
        setWillNotDraw(false);
        getHolder().addCallback(this);
        detect = FaceDetectorFactory.createFaceDetector(context);
        detect.setEyeListener(this);
        device = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        if(isRotationLocked(context))
            rotationListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
                private static final int THRESHOLD = 10;
                private int angle = 0;
                @Override
                public void onOrientationChanged(int degrees) {
                    if(45+THRESHOLD<getDiff(angle, degrees)) {
                        int rot = Math.round(degrees/90f);
                        angle = 90*rot;//to nearest multiple of 90
                        detect.setDeviceOrientation(0x03&-rot);
                    }
                }
                private int getDiff(int angle1, int angle2) {
                    int diff = Math.abs(angle1-angle2);
                    return 180<diff?360-diff:diff;
                }
            };
        else
            rotationListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
                @Override
                public void onOrientationChanged(int orientation) {
                    int currRotation = device.getRotation();
                    if(currRotation!=rotation&&RotatableSurfaceView.isSameOrientation(currRotation, rotation)) {
                        rotation = currRotation;
                        detect.setDeviceRotation(rotation);
                        if(null!=listener)
                            listener.onDeviceReversed();
                    }
                }
            };
        rotation = device.getRotation();
        detect.setDeviceRotation(rotation);
    }
    private boolean isRotationLocked(Context context) {
        try {
            return 0==Settings.System.getInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
        } catch(Settings.SettingNotFoundException e) {
            return true;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        int fx = MeasureSpec.getMode(widthMeasureSpec);
        int fy = MeasureSpec.getMode(heightMeasureSpec);
        Log.i(TAG, "Measure "+w+"x"+h);
        float aspect = detect.getCameraAspect();
        boolean cameraWider = w<h*aspect;
        if(cameraWider&&MeasureSpec.EXACTLY!=fy)
            h = (int)(w/aspect);
        else if(!cameraWider&&MeasureSpec.EXACTLY!=fx)
            w = (int)(h*aspect);
        Log.i(TAG, "Measure "+w+"x"+h+", content aspect "+aspect+" rotation "+rotation);
        setMeasuredDimension(w, h);
    }

    public int getDeviceRotation() {return rotation;}
    public void setDeviceReversedListener(ASDeviceReversedListener listener) {this.listener = listener;}
    public void setFaceDetectionListener(ASDetectionListener listener) {detect.setFaceListener(listener);}

    @Override
    public void onEyePosition(@Nullable PointF left, @Nullable PointF right) {
        this.left = left;
        this.right = right;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawPoint(canvas, left);
        drawPoint(canvas, right);
    }
    private void drawPoint(Canvas canvas, PointF point) {
        if(null==point)
            return;
        canvas.drawCircle((1-point.x)*getMeasuredWidth(), point.y*getMeasuredHeight(), 25, paint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        rotationListener.enable();
    }
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        rotationListener.disable();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        CameraBase.Resolution res = detect.getResolution();
        holder.setFixedSize(res.getWidth(), res.getHeight());
        detect.startDetection(holder);
        rotation = device.getRotation();
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "Surface "+width+"x"+height+", format "+format);
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {detect.stopDetection();}
}
