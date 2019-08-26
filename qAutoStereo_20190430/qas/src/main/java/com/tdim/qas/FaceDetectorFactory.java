package com.tdim.qas;

import android.content.Context; //API level 1

public final class FaceDetectorFactory {
    public static FaceDetectorBase createFaceDetector(Context context) {
        if(FaceDetectorMV.hasPlayServices(context))
            return new FaceDetectorMV(context);
        else
            return new FaceDetectorBTK(context);
    }
}
