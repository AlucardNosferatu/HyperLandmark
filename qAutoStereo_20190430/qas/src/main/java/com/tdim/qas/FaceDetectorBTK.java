package com.tdim.qas;

import android.content.Context;         //API level 1
import android.graphics.ImageFormat;    //API level 8
import android.graphics.PointF;         //API level 1
import android.util.Log;                //API level 1

import java.nio.ByteBuffer;

final class FaceDetectorBTK extends FaceDetectorBase {
    private static final String TAG = FaceDetectorBTK.class.getSimpleName();
    private static final int MAX_FACES = 1;
    private static long pDetector;
    private int rotation;
    private byte[] bufY;

    FaceDetectorBTK(Context context) {
        super(context);
        if(!isInitialized()) {
            byte[] data = Util.loadRaw(context, R.raw.btk_data);
            if(0>globalInit(data, MAX_FACES))
                Log.e(TAG, "Could not initialize detector");
        }
    }

    @Override
    boolean isOperational() {return true;}

    @Override
    void setPreviewRotation(int rotation) {
        boolean sameOrient = RotatableSurfaceView.isSameOrientation(this.rotation, rotation);
        this.rotation = rotation;
        if(sameOrient)
            synchronized(FaceDetectorBTK.class) {
                setRotation(pDetector, rotation);
            }
        else
            recreateDetector(inputWidth, inputHeight);//TODO somehow recreating on NDK side makes it slower
    }

    @Override
    public void onInit(int width, int height, int format) {
        super.onInit(width, height, format);
        switch(format) {
            case ImageFormat.YV12:
            case ImageFormat.NV16:
            case ImageFormat.NV21:
            case ImageFormat.YUV_420_888:
            case ImageFormat.YUV_422_888:
            case ImageFormat.YUV_444_888:
                break;
            default:
                throw new IllegalStateException("Can only use formats with luma plane");
        }
        recreateDetector(width, height);
    }
    private void recreateDetector(int width, int height) {
        synchronized(FaceDetectorBTK.class) {
            destroy(pDetector);
            pDetector = create(width, height, rotation);
            int min = (int)Math.floor(getFocalLength()*55/400);//smallest eye dist/farthest viewing dist
            int max = (int)Math.ceil( getFocalLength()*75/200);//largest eye dist/nearest viewing dist
            setEyeDistance(pDetector, min, max);
            Log.i(TAG, "minmax eye distance "+min+"/"+max);
        }
    }

    @Override
    void release() {
        bufY = null;
        synchronized(FaceDetectorBTK.class) {
            destroy(pDetector);
            pDetector = 0;
        }
    }

    @Override
    public void onFrame(ByteBuffer data, int width, int height, int format) {
        float[] posLR = new float[4];
        int count = 0;
        synchronized(FaceDetectorBTK.class) {
            count = detect(pDetector, getArray(data), posLR);
        }
        setFaceFound(0<count);
        if(0<count)
            onEyesDetected(new PointF(posLR[0], posLR[1]), new PointF(posLR[2], posLR[3]));
        else
            onEyesDetected(null, null);
    }
    private byte[] getArray(ByteBuffer buf) {
        if(buf.hasArray())
            return buf.array();
        if(null==bufY)
            bufY = new byte[buf.capacity()];
        buf.get(bufY);
        return bufY;
    }

    private static native boolean isInitialized();
    private static native int globalInit(byte[] moduleParam, int maxFaces);
    private static native void globalFini();
    private static native long create(int w, int h, int r);
    private static native void setRotation(long pDetector, int rotation);
    private static native int detect(long pDetector, byte[] data, float[] posLR);
    private static native void destroy(long pDetector);
    private static native void setEyeDistance(long pDetector, int min, int max);
}
