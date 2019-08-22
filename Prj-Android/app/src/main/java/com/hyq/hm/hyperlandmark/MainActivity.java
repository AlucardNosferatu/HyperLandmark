package com.hyq.hm.hyperlandmark;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLES20;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraAccessException;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;

import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.zeusee.zmobileapi.STUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import zeusees.tracking.Face;
import zeusees.tracking.FaceTracking;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private String[] denied;
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA};
    private FaceTracking mMultiTrack106 = null;
    private boolean mTrack106 = false;

    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private byte[] mNv21Data;
    private CameraOverlap cameraOverlap;
    private final Object lockObj = new Object();
    private SurfaceView mSurfaceView;
    private EGLUtils mEglUtils;
    private GLFramebuffer mFramebuffer;
    private GLFrame mFrame;
    private GLPoints mPoints;
    private GLBitmap mBitmap;
    private SeekBar seekBarA;
    private SeekBar seekBarB;
    private SeekBar seekBarC;

    private boolean landscape = false;
    private SensorManager SM;

    public int x;
    public int y;
    public int h;
    public int w;
    public float x_norm_left;
    public float y_norm_left;
    public float x_norm_right;
    public float y_norm_right;
    public float face_distance;
    public float eyes_distance;
    public float distances_product;
    public boolean output_distance;
    public String distance_string;
    public TextView coordinates;
    public double focalLength;
    public float horizontalViewAngle;
    public float verticalViewAngle;
    public double sensorHeight;
    public double sensorWidth;
    public CameraManager CM;
    public Size size;
    public float[] points;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SM =(SensorManager)getSystemService(Context.SENSOR_SERVICE);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> list = new ArrayList<>();
            for (int i = 0; i < permissions.length; i++) {
                if (PermissionChecker.checkSelfPermission(this, permissions[i]) == PackageManager.PERMISSION_DENIED) {
                    list.add(permissions[i]);
                }
            }
            if (list.size() != 0) {
                denied = new String[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    denied[i] = list.get(i);
                }
                ActivityCompat.requestPermissions(this, denied, 5);
            } else {
                init();
            }
        } else {
            init();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SM.registerListener((SensorEventListener) this,SM.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SM.unregisterListener((SensorEventListener) this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 5) {
            boolean isDenied = false;
            for (int i = 0; i < denied.length; i++) {
                String permission = denied[i];
                for (int j = 0; j < permissions.length; j++) {
                    if (permissions[j].equals(permission)) {
                        if (grantResults[j] != PackageManager.PERMISSION_GRANTED) {
                            isDenied = true;
                            break;
                        }
                    }
                }
            }
            if (isDenied) Toast.makeText(this, "请开启权限", Toast.LENGTH_SHORT).show();
            else init();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //实现接口必须重写所有方法，不想写也得留空
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //判断传感器类别(deprecated)
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ORIENTATION:
                if(event.values[1]>0) landscape = true;
                else landscape = false;
                break;
            default:
                break;
        }
    }

    private void init(){
        CM=(CameraManager)getSystemService(CAMERA_SERVICE);
        points = new float[4];
        InitModelFiles();
        sensorHeight=0;
        sensorWidth=0;
        face_distance=32;
        eyes_distance=70;
        distance_string="";
        output_distance=false;
        h=CameraOverlap.PREVIEW_HEIGHT;
        w=CameraOverlap.PREVIEW_WIDTH;
        coordinates = findViewById(R.id.coordinates);
        mMultiTrack106 = new FaceTracking("/sdcard/ZeuseesFaceTracking/models");
        cameraOverlap = new CameraOverlap(this);
        mNv21Data = new byte[CameraOverlap.PREVIEW_WIDTH * CameraOverlap.PREVIEW_HEIGHT * 2];
        mFramebuffer = new GLFramebuffer();
        mFrame = new GLFrame();
        mPoints = new GLPoints();
        mBitmap = new GLBitmap(this,R.drawable.ic_logo);
        mHandlerThread = new HandlerThread("DrawFacePointsThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        cameraOverlap.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, final Camera camera) {
                focalLength=camera.getParameters().getFocalLength();
                verticalViewAngle=camera.getParameters().getVerticalViewAngle();
                horizontalViewAngle=camera.getParameters().getHorizontalViewAngle();
                sensorHeight=Math.tan(3.14*(0.5*horizontalViewAngle)/180)*focalLength;
                sensorWidth=Math.tan(3.14*(0.5*verticalViewAngle)/180)*focalLength;
                synchronized (lockObj) {
                    System.arraycopy(data, 0, mNv21Data, 0, data.length);
                    if(landscape){
                        mNv21Data=rotateYUV420Degree90(mNv21Data,cameraOverlap.PREVIEW_WIDTH,cameraOverlap.PREVIEW_HEIGHT);
                    }
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(mEglUtils == null){
                            return;
                        }
                        mFrame.setS(seekBarA.getProgress()/100.0f);
                        mFrame.setH(seekBarB.getProgress()/360.0f);
                        mFrame.setL(seekBarC.getProgress()/100.0f - 1);
                        if(mTrack106){
                            if(landscape) mMultiTrack106.FaceTrackingInit(mNv21Data,CameraOverlap.PREVIEW_WIDTH,CameraOverlap.PREVIEW_HEIGHT);
                            else mMultiTrack106.FaceTrackingInit(mNv21Data,CameraOverlap.PREVIEW_HEIGHT,CameraOverlap.PREVIEW_WIDTH);
                            mTrack106 = !mTrack106;
                        }else {
                            if(landscape) mMultiTrack106.Update(mNv21Data,CameraOverlap.PREVIEW_WIDTH,CameraOverlap.PREVIEW_HEIGHT);
                            else mMultiTrack106.Update(mNv21Data,CameraOverlap.PREVIEW_HEIGHT,CameraOverlap.PREVIEW_WIDTH);
                        }

                        boolean rotate270 = cameraOverlap.getOrientation() == 270;
                        List<Face> faceActions = mMultiTrack106.getTrackingInfo();
                        float[] p = null;
                        //float[] points = null;
                        DecimalFormat decimalFormat=new DecimalFormat("0.00");
                        for (Face r : faceActions) {
                            points = new float[106*2];
                            Rect rect=new Rect(CameraOverlap.PREVIEW_HEIGHT - r.left,r.top,CameraOverlap.PREVIEW_HEIGHT - r.right,r.bottom);
                            for(int i = 0 ; i < 106 ; i++) {
//                                if(i==72||i==105){
                                    if (landscape) {
                                        if (rotate270) y = r.landmarks[i * 2];
                                        else y = CameraOverlap.PREVIEW_WIDTH - r.landmarks[i * 2];
                                        x = CameraOverlap.PREVIEW_HEIGHT - r.landmarks[i * 2 + 1];
                                    } else {
                                        if (rotate270) x = r.landmarks[i * 2];
                                        else x = CameraOverlap.PREVIEW_HEIGHT - r.landmarks[i * 2];
                                        y = r.landmarks[i * 2 + 1];
                                    }
                                    if(i==72) {
                                        x_norm_left = (float) x / (float) CameraOverlap.PREVIEW_HEIGHT;
                                        y_norm_left = (float) y / (float) CameraOverlap.PREVIEW_WIDTH;
//                                        points[0] = view2openglX(x, CameraOverlap.PREVIEW_HEIGHT);
//                                        points[1] = view2openglY(y, CameraOverlap.PREVIEW_WIDTH);
                                        points[i * 2] = view2openglX(x, CameraOverlap.PREVIEW_HEIGHT);
                                        points[i * 2 + 1] = view2openglY(y, CameraOverlap.PREVIEW_WIDTH);
                                    }
                                    else if(i==105){
                                        x_norm_right = (float) x / (float) CameraOverlap.PREVIEW_HEIGHT;
                                        y_norm_right = (float) y / (float) CameraOverlap.PREVIEW_WIDTH;
//                                        points[2] = view2openglX(x, CameraOverlap.PREVIEW_HEIGHT);
//                                        points[3] = view2openglY(y, CameraOverlap.PREVIEW_WIDTH);
                                        points[i * 2] = view2openglX(x, CameraOverlap.PREVIEW_HEIGHT);
                                        points[i * 2 + 1] = view2openglY(y, CameraOverlap.PREVIEW_WIDTH);
                                    }
                                    //eyes_distance=Math.abs(x_norm_left-x_norm_right);
                                    String x_n=decimalFormat.format((x_norm_left+x_norm_right)/2);
                                    String y_n=decimalFormat.format((y_norm_left+y_norm_right)/2);
                                    if(output_distance){
                                        //face_distance=distances_product/eyes_distance;
                                        //distance_string="   distance:"+face_distance;
                                        double realDistance_x=Math.abs(x_norm_left-x_norm_right)*2*sensorWidth;
                                        double realDistance_y=Math.abs(y_norm_left-y_norm_right)*2*sensorHeight;
                                        double realDistance=Math.sqrt(Math.pow(realDistance_x,2)+Math.pow(realDistance_y,2));
                                        realDistance=eyes_distance*focalLength/realDistance;
                                        String rd=decimalFormat.format(realDistance);
                                        distance_string="   distance:"+rd;
                                    }

                                    coordinates.setText("x:"+x_n+"   y:"+y_n+distance_string);
                                    //points[i * 2] = view2openglX(x, CameraOverlap.PREVIEW_HEIGHT);
                                    //points[i * 2 + 1] = view2openglY(y, CameraOverlap.PREVIEW_WIDTH);

//                                    if (i == 70) {
//                                        p = new float[8];
//                                        p[0] = view2openglX(x + 20, CameraOverlap.PREVIEW_HEIGHT);
//                                        p[1] = view2openglY(y - 20, CameraOverlap.PREVIEW_WIDTH);
//                                        p[2] = view2openglX(x - 20, CameraOverlap.PREVIEW_HEIGHT);
//                                        p[3] = view2openglY(y - 20, CameraOverlap.PREVIEW_WIDTH);
//                                        p[4] = view2openglX(x + 20, CameraOverlap.PREVIEW_HEIGHT);
//                                        p[5] = view2openglY(y + 20, CameraOverlap.PREVIEW_WIDTH);
//                                        p[6] = view2openglX(x - 20, CameraOverlap.PREVIEW_HEIGHT);
//                                        p[7] = view2openglY(y + 20, CameraOverlap.PREVIEW_WIDTH);
//                                    }
//                                }
//                                else if(i<=3){
//                                    int a=i-0;
//                                    int b;
//                                    int c;
//                                    switch(a){
//                                        case 0:
//                                            b=1;
//                                            c=1;
//                                            break;
//                                        case 1:
//                                            b=1;
//                                            c=3;
//                                            break;
//                                        case 2:
//                                            b=3;
//                                            c=1;
//                                            break;
//                                        case 3:
//                                            b=3;
//                                            c=3;
//                                            break;
//                                        default:
//                                            b=1;
//                                            c=1;
//                                    }
//                                    points[i*2]=view2openglX(b*h/4, CameraOverlap.PREVIEW_HEIGHT);
//                                    points[i*2+1]=view2openglY(c*w/4, CameraOverlap.PREVIEW_WIDTH);
//                                }
                            }
                            if(p != null){
                                break;
                            }
                        }
                        int tid = 0;
                        if(p != null){
                            mBitmap.setPoints(p);
                            tid = mBitmap.drawFrame();
                        }
                        mFrame.drawFrame(tid,mFramebuffer.drawFrameBuffer(),mFramebuffer.getMatrix());

//                        if((points != null)&&(points[0]*points[1]*points[2]*points[3]!=0)){
                          if(points != null){
                            mPoints.setPoints(points);
                            mPoints.drawPoints();
                        }
                        mEglUtils.swap();

                    }
                });
            }
        });
        mSurfaceView = findViewById(R.id.surface_view);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(final SurfaceHolder holder, int format, final int width, final int height) {
                Log.d("=============","surfaceChanged");
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(mEglUtils != null){
                            mEglUtils.release();
                        }
                        mEglUtils = new EGLUtils();
                        mEglUtils.initEGL(holder.getSurface());
                        mFramebuffer.initFramebuffer();
                        mFrame.initFrame();
                        mFrame.setSize(width,height, CameraOverlap.PREVIEW_HEIGHT,CameraOverlap.PREVIEW_WIDTH );
                        mPoints.initPoints();
                        mBitmap.initFrame(CameraOverlap.PREVIEW_HEIGHT,CameraOverlap.PREVIEW_WIDTH);
                        cameraOverlap.openCamera(mFramebuffer.getSurfaceTexture());
                    }
                });
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        cameraOverlap.release();
                        mFramebuffer.release();
                        mFrame.release();
                        mPoints.release();
                        mBitmap.release();
                        if(mEglUtils != null){
                            mEglUtils.release();
                            mEglUtils = null;
                        }
                    }
                });
            }
        });
        if(mSurfaceView.getHolder().getSurface()!= null &&mSurfaceView.getWidth() > 0){
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mEglUtils != null){
                        mEglUtils.release();
                    }
                    mEglUtils = new EGLUtils();
                    mEglUtils.initEGL(mSurfaceView.getHolder().getSurface());
                    mFramebuffer.initFramebuffer();
                    mFrame.initFrame();
                    mFrame.setSize(mSurfaceView.getWidth(),mSurfaceView.getHeight(), CameraOverlap.PREVIEW_HEIGHT,CameraOverlap.PREVIEW_WIDTH );
                    mPoints.initPoints();
                    mBitmap.initFrame(CameraOverlap.PREVIEW_HEIGHT,CameraOverlap.PREVIEW_WIDTH);
                    cameraOverlap.openCamera(mFramebuffer.getSurfaceTexture());
                }
            });
        }
        seekBarA = findViewById(R.id.seek_bar_a);
        seekBarB = findViewById(R.id.seek_bar_b);
        seekBarC = findViewById(R.id.seek_bar_c);
    }

    public void onCalibrate(View view){
        //distances_product=eyes_distance*face_distance;

        try {
            CameraCharacteristics CC = CM.getCameraCharacteristics(CM.getCameraIdList()[1]);
            size=CC.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
        }
        catch(CameraAccessException e){
            Log.e("GSSize has been fucked:", e.getMessage(), e);
        }
        output_distance=true;
    }

    public void InitModelFiles(){
        String assetPath = "ZeuseesFaceTracking";
        String sdcardPath = Environment.getExternalStorageDirectory() + File.separator + assetPath;
        copyFilesFromAssets(this, assetPath, sdcardPath);
    }

    public void copyFilesFromAssets(Context context, String oldPath, String newPath) {
        try {
            String[] fileNames = context.getAssets().list(oldPath);
            if (fileNames.length > 0) {
                // directory
                File file = new File(newPath);
                if (!file.mkdir()) Log.d("mkdir","can't make folder");
                for (String fileName : fileNames) copyFilesFromAssets(context, oldPath + "/" + fileName,newPath + "/" + fileName);
            }
            else {
                // file
                InputStream is = context.getAssets().open(oldPath);
                FileOutputStream fos = new FileOutputStream(new File(newPath));
                byte[] buffer = new byte[1024];
                int byteCount;
                while ((byteCount = is.read(buffer)) != -1) fos.write(buffer, 0, byteCount);
                fos.flush();
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private float view2openglX(int x,int width){
        float centerX = width/2.0f;
        float t = x - centerX;
        return t/centerX;
    }

    private float view2openglY(int y,int height){
        float centerY = height/2.0f;
        float s = centerY - y;
        return s/centerY;
    }

    public static byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth)
                        + (x - 1)];
                i--;
            }
        }
        return yuv;
    }
}
