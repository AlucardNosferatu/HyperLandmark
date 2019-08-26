package com.tdim.qas;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@androidx.annotation.RequiresApi(api=Build.VERSION_CODES.LOLLIPOP)
final class Camera2 extends CameraBase {
    private static final String TAG = Camera2.class.getSimpleName();
    private static final int FORMAT = ImageFormat.YUV_420_888;
    private final CameraManager manager;
    private String cameraId;
    private HandlerThread bgThread;
    private Handler bgHandler;
    private CameraDevice device;
    private CameraCaptureSession capture;

    Camera2(Context context) {
        super(context);
        manager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);

        try {
            List<String> cameras = new ArrayList<>();
            if(null!=manager)
                for(String id: manager.getCameraIdList()) {
                    CameraCharacteristics cc = manager.getCameraCharacteristics(id);
                    Integer lensFacing = cc.get(CameraCharacteristics.LENS_FACING);
                    if(null!=lensFacing&&CameraCharacteristics.LENS_FACING_FRONT==lensFacing)
                        cameras.add(id);
                }
            if(!cameras.isEmpty()) {
                cameraId = cameras.get(0);
                CameraCharacteristics cc = manager.getCameraCharacteristics(cameraId);
                Integer orient = cc.get(CameraCharacteristics.SENSOR_ORIENTATION);
                setCameraRotation(null==orient?0:orient/90);

                StreamConfigurationMap map = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                List<Resolution> resolutions = new ArrayList<>();
                if(null!=map)
                    for(Size size: map.getOutputSizes(FORMAT))
                        resolutions.add(new Resolution(size.getWidth(), size.getHeight()));
                setSupportedResolutions(resolutions);
            }
        } catch(CameraAccessException | SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    void rotatePreview() {//TODO
    }

    @Override
    boolean hasCamera() {return null!=cameraId;}
    @Override
    boolean isCapturing() {return null!=bgThread;}

    @Override
    void startSession(final SurfaceHolder holder) throws SecurityException {//may throw if no camera permission
        if(isCapturing()||!hasCamera()||!hasCameraPermission())
            return;
        bgThread = new HandlerThread("Camera");
        bgThread.start();
        bgHandler = new Handler(bgThread.getLooper());
        CameraDevice.StateCallback cameraCB = new CameraDevice.StateCallback() {
            private ImageReader reader;

            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                try {
                    CameraCharacteristics cc = manager.getCameraCharacteristics(camera.getId());
                    SizeF physSize = cc.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                    float[] focalLen = cc.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                    Resolution size = getRequestedResolution();
                    if(null==physSize||null==focalLen||0==focalLen.length)
                        setFocalLength(0);
                    else if(physSize.getWidth()*size.getHeight()<physSize.getHeight()*size.getWidth())
                        setFocalLength(focalLen[0]*size.getWidth()/physSize.getWidth());//camera view taller than image
                    else
                        setFocalLength(focalLen[0]*size.getHeight()/physSize.getHeight());//camera view wider than image

                    device = camera;
                    reader = ImageReader.newInstance(size.getWidth(), size.getHeight(), FORMAT, 2);
                    reader.setOnImageAvailableListener(new OnImageAvailableListener() {
                        @Override
                        public void onImageAvailable(ImageReader reader) {
                            Image img = reader.acquireLatestImage();
                            if(null==img)
                                return;
                            if(null!=listener)
                                listener.onFrame(img.getPlanes()[0].getBuffer(), img.getWidth(), img.getHeight(), FORMAT);
                            img.close();
                        }
                    }, bgHandler);
                    //final List<Surface> targets = Collections.singletonList(reader.getSurface());
                    final List<Surface> targets = null==holder
                            ?Collections.singletonList(reader.getSurface())
                            :Arrays.asList(reader.getSurface(), holder.getSurface());
                    CameraCaptureSession.StateCallback sessionCB = new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                CaptureRequest.Builder builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                for(Surface s: targets)
                                    builder.addTarget(s);
                                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                session.setRepeatingRequest(builder.build(), null, bgHandler);
                                capture = session;
                            } catch(Exception e) {
                                e.printStackTrace();
                                onConfigureFailed(session);
                            }
                        }
                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            session.close();
                        }

                        @Override
                        public void onClosed(@NonNull CameraCaptureSession session) {
                            if(session.equals(capture))
                                capture = null;
                            reader.getSurface().release();
                            Log.i(TAG, "Session closed");
                            super.onClosed(session);

                        }
                    };
                    if(null!=listener)
                        listener.onInit(size.getWidth(), size.getHeight(), FORMAT);
                    device.createCaptureSession(targets, sessionCB, bgHandler);
                } catch(Exception e) {//CameraAccessException, IllegalStateException
                    e.printStackTrace();
                    device.close();
                }
            }
            @Override
            public void onClosed(@NonNull CameraDevice camera) {
                Log.i(TAG, "Device closed");
                if(camera.equals(device)) {
                    reader.close();
                    reader = null;
                    device = null;
                }
                super.onClosed(camera);
            }
            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {camera.close();}
            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                Log.e(TAG, "Camera error "+error);
                camera.close();
            }
        };
        try {
            manager.openCamera(cameraId, cameraCB, bgHandler);//requires camera permission
        } catch(CameraAccessException e) {
            e.printStackTrace();
            stopSession();
        }
    }
    @Override
    void stopSession() {
        if(null!=capture)
            capture.close();//sets itself to null when done
        if(null!=device)
            device.close();//sets itself to null when done
        if(null!=bgThread)
            try {
                bgThread.quitSafely();
                bgThread.join();
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        bgThread = null;
        bgHandler = null;
    }
}
