package com.tdim.qas;

import android.opengl.GLES20;   //API level 8

import java.nio.Buffer;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

final class CalibrationRenderer extends CalibratableRenderer {
    private static final float[] FACTORS = {0,1,0, -1,0,0, 0,0,1, 0,-1,0, 1,0,0, 0,0,-1};
    private static final float[] OFFSETS = {1,0,0, 1,1,0, 0,1,0, 0,1,1, 0,0,1, 1,0,1};

    private boolean activated;
    private int texGradient;

    CalibrationRenderer(ASCalibrationView view) {super(view);}

    void setActivated(boolean state) {
        activated = state;
        updateProgram = true;
    }

    void setMode(ASCalibrationView.CalibMode mode) {
        try {
            super.setMode(Mode.values()[mode.ordinal()]);
        } catch(ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Unsupported mode "+mode);
        }
    }

    void setRotation(int rotation) {
        if(!RotatableSurfaceView.isSameOrientation(rotation, getRotation()))
            setPitches(getRotatedPitches());//toggled between portrait/landscape
        super.setRotation(rotation);
    }

    private Buffer createGradient() {//TODO
        final int w = 2;
        final int h = 1;
        int cap = (~0x03)&(w*h*3+0x03);//RGB
        ByteBuffer data = ByteBuffer.allocateDirect(cap);
        data.put((byte)0x00);
        data.put((byte)0xFF);
        data.put((byte)0xFF);

        data.put((byte)0xFF);
        data.put((byte)0x00);
        data.put((byte)0x00);

        return data.position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        super.onSurfaceCreated(glUnused, config);

        texGradient = genTexture(glUnused);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texGradient);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB,
                2, 1, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, createGradient());
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    void updateProgram(GL10 glUnused) {
        if(0<program) {
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        if(activated)
            super.updateProgram(glUnused);
    }

    @Override
    void updateUniforms(GL10 glUnused) {
        GLES20.glUniform3fv(GLES20.glGetUniformLocation(program, "u_factor"), 6, FACTORS, 0);//rainbow constants
        GLES20.glUniform3fv(GLES20.glGetUniformLocation(program, "u_offset"), 6, OFFSETS, 0);//rainbow constants
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "u_D"), 2);//textureGradient
    }
}
