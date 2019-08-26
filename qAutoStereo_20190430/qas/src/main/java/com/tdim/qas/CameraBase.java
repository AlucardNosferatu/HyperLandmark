package com.tdim.qas;

import android.Manifest;            //API level 1
import android.content.Context;     //API level 1
import android.graphics.ImageFormat;//API level 8
import androidx.annotation.NonNull;
import android.view.SurfaceHolder;  //API level 1

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

abstract class CameraBase {
    interface SessionListener {
        void onInit(int width, int height, int format);
        void onFrame(ByteBuffer data, int width, int height, int format);
    }
    class Resolution {
        private int width;
        private int height;
        Resolution() {this(0, 0);}
        Resolution(int width, int height) {
            this.width = width;
            this.height= height;
        }

        @NonNull
        @Override
        public String toString() {return width+"x"+height;}

        int getWidth() {return width;}
        int getHeight() {return height;}
        float getAspect() {return getAspect(false);}
        float getAspect(boolean rotated) {
            if(0<width&&0<height)
                return rotated?(height/(float)width):(width/(float)height);
            else
                return 1;
        }
        int getArea() {return width*height;}
    }
    private final Context context;
    SessionListener listener;
    private List<Resolution> supportedResolutions = new ArrayList<>();
    private int resolutionIndex = -1;
    private int cameraRotation;
    private int deviceRotation;
    private double focalLength;//in pixels

    CameraBase(Context context) {
        this.context = context;
    }

    void setSupportedResolutions(List<Resolution> resolutions) {
        supportedResolutions = null==resolutions?new ArrayList<Resolution>():resolutions;
        Collections.sort(supportedResolutions, new Comparator<Resolution>() {
            @Override
            public int compare(Resolution s1, Resolution s2) {
                int dArea = s1.width*s1.height-s2.width*s2.height;
                return 0==dArea?s1.width-s2.width:dArea;
            }
        });//sort from smallest to largest
        if(supportedResolutions.isEmpty())
            resolutionIndex = -1;
        else {
            int quality = Math.max(0, Math.min(100, new ASSettings(context).getCameraQuality()));
            resolutionIndex = supportedResolutions.size()*quality/101;
        }
    }
    Resolution getRequestedResolution() {return -1<resolutionIndex?supportedResolutions.get(resolutionIndex):new Resolution();}
    float getPreviewAspect() {return getRequestedResolution().getAspect();}
    float getRotatedAspect() {//TODO check logic
        Resolution res = getRequestedResolution();
        return res.getAspect(!RotatableSurfaceView.isPortrait(getPreviewRotation()));
    }

    final boolean hasCameraPermission() {return Util.hasPermission(context, Manifest.permission.CAMERA);}

    final void setSessionListener(SessionListener listener) {this.listener = listener;}

    final void setCameraRotation(int rotation) {cameraRotation = rotation;}
    final void setDeviceRotation(int rotation) {
        deviceRotation = rotation;
        rotatePreview();
    }
    final int getPreviewRotation() {return 0x03&(cameraRotation+deviceRotation);}
    final void setFocalLength(double pixels) {focalLength = pixels;}
    int getCameraRotation() {return cameraRotation;}
    double getFocalLength() {return focalLength;}

    abstract boolean hasCamera();
    abstract boolean isCapturing();
    abstract void startSession(SurfaceHolder holder);
    abstract void stopSession();
    abstract void rotatePreview();

    static String getName(int format) {
        switch(format) {
            default:
                return "Unknown format 0x"+Integer.toHexString(format);
            //API level 8
            case ImageFormat.RGB_565:   return "RGB 565";
            case ImageFormat.NV16:      return "NV16";
            case ImageFormat.NV21:      return "NV21";
            case ImageFormat.YUY2:      return "YUYV";
            //API level 9
            case ImageFormat.YV12:      return "YVU420p";
            //API level 19
            case ImageFormat.YUV_420_888:   return "YUV420p";
            //API level 21
            case ImageFormat.RAW_SENSOR:return "16-bit raw data";
            case ImageFormat.RAW10:     return "10-bit raw data";
            //API level 23
            case ImageFormat.RAW12:     return "12-bit raw data";
            case ImageFormat.DEPTH16:   return "16-bit Grayscale";
            case ImageFormat.YUV_422_888:   return "YUV422p";
            case ImageFormat.YUV_444_888:   return "YUV444p";
            case ImageFormat.FLEX_RGB_888:  return "RGBp";
            case ImageFormat.FLEX_RGBA_8888:return "RGBAp";
        }
    }
}
