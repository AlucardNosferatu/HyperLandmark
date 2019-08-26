package com.tdim.qas;

import android.content.Context;         //API level 1
import android.graphics.ImageFormat;    //API level 8
import android.graphics.SurfaceTexture; //API level 11
import android.hardware.Camera;         //API level 11
import android.os.Handler;              //API level 1
import android.os.HandlerThread;        //API level 5
import android.view.SurfaceHolder;      //API level 1

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

final class CameraLegacy extends CameraBase {
    private static final int FORMAT = ImageFormat.NV21;//buffer may be smaller than expected for YV12?
    private int cameraId = -1;
    private HandlerThread bgThread;
    private Handler bgHandler;
    private Camera camera;

    CameraLegacy(Context context) {
        super(context);
        Camera.CameraInfo info = new Camera.CameraInfo();
        List<Integer> cameras = new ArrayList<>();
        for(int i=0;i<Camera.getNumberOfCameras();i++) {
            Camera.getCameraInfo(i, info);
            if(Camera.CameraInfo.CAMERA_FACING_FRONT==info.facing)
                cameras.add(i);
        }
        if(!cameras.isEmpty()) {
            cameraId = cameras.get(0);
            Camera.getCameraInfo(cameraId, info);
            setCameraRotation(info.orientation/90);
        }

        try {
            Camera camera = Camera.open(cameraId);
            Camera.Parameters params = camera.getParameters();
            camera.release();
            List<Resolution> resolutions = new ArrayList<>();
            for(Camera.Size size: params.getSupportedPreviewSizes())
                resolutions.add(new Resolution(size.width, size.height));
            setSupportedResolutions(resolutions);
        } catch(RuntimeException e) {
            e.printStackTrace();
        }
    }

    boolean hasCamera() {return -1<cameraId;}
    boolean isCapturing() {return null!=bgThread;}

    @Override
    void startSession(final SurfaceHolder holder) {
        if(isCapturing()||!hasCamera()||!hasCameraPermission())
            return;
        bgThread = new HandlerThread("Camera");
        bgThread.start();
        bgHandler = new Handler(bgThread.getLooper());
        bgHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    camera = Camera.open(cameraId);
                    Camera.Parameters params = camera.getParameters();//all devices support NV21(default) and YV12
                    double physWidth = 2*Math.tan(0.5*params.getHorizontalViewAngle()*Math.PI/180);
                    double physHeight= 2*Math.tan(0.5*params.getVerticalViewAngle()*Math.PI/180);
                    final Resolution size = getRequestedResolution();
                    params.setPreviewSize(size.getWidth(), size.getHeight());
                    if(physWidth*size.getWidth()<physHeight*size.getHeight())
                        setFocalLength(size.getWidth()/physWidth);//camera view taller than image
                    else
                        setFocalLength(size.getHeight()/physHeight);//camera view wider than image
                    params.setPreviewFormat(FORMAT);
                    camera.setParameters(params);
                    Camera.PreviewCallback previewCB = new Camera.PreviewCallback() {
                        @Override
                        public void onPreviewFrame(byte[] data, Camera camera) {
                            if(null==camera||null==data)
                                return;
                            if(null!=listener)
                                listener.onFrame(ByteBuffer.wrap(data), size.getWidth(), size.getHeight(), FORMAT);
                            camera.addCallbackBuffer(data);
                        }
                    };
                    int bufsize = (size.getArea()*ImageFormat.getBitsPerPixel(FORMAT))/8;
                    camera.addCallbackBuffer(new byte[bufsize]);
                    camera.addCallbackBuffer(new byte[bufsize]);
                    camera.setPreviewCallbackWithBuffer(previewCB);
                    if(null!=listener)
                        listener.onInit(size.getWidth(), size.getHeight(), FORMAT);
                    camera.setDisplayOrientation(getDisplayOrientation());
                    if(null==holder)
                        camera.setPreviewTexture(new SurfaceTexture(0));
                    else
                        camera.setPreviewDisplay(holder);
                    camera.startPreview();
                } catch(Exception e) {
                    e.printStackTrace();
                    stopSession();
                }
            }
        });
    }
    @Override
    void rotatePreview() {
        if(null!=bgHandler)
            bgHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(null!=camera)
                        camera.setDisplayOrientation(getDisplayOrientation());
                }
            });
    }
    private int getDisplayOrientation() {return 90*(0x03&-getPreviewRotation());}
    @Override
    void stopSession() {
        if(null!=bgHandler)
            bgHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(null!=camera) {
                        camera.stopPreview();
                        camera.release();
                    }
                    camera = null;
                    bgThread.quit();
                }
            });
        if(null!=bgThread)
            try {
                bgThread.join(0);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        bgThread = null;
        bgHandler = null;
    }
}
