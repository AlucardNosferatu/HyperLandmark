package com.tdim.qas;

import android.content.Context;         //API level 1
import android.graphics.Bitmap;         //API level 1
import android.opengl.GLES20;           //API level 8
import android.opengl.GLSurfaceView;    //API level 3
import android.opengl.GLUtils;          //API level 1
import androidx.annotation.Size;
import android.util.Log;                //API level 1

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

abstract class Renderer implements GLSurfaceView.Renderer {
    private static final String TAG = Renderer.class.getSimpleName();
    private static final float[] VERTICES = {-1,-1,0,0, 1,-1,1,0, 1,1,1,1, -1,1,0,1};

    final int[] screenMargins = new int[4];
    private final GLSurfaceView surfaceView;
    private int rotation;//of device, affects pitches and pix geom
    private PixelPitch pitches = new PixelPitch();//lenticular per pixel in h/v
    private ASPixelGeometry pixgeom = ASPixelGeometry.getPixelGeomtery(ASPixelGeometry.STRIPES);

    private float eyePosCoef;

    Renderer(GLSurfaceView view) {surfaceView = view;}

    void requestRender() {surfaceView.requestRender();}

    float[] subpixelDelta() {
        float[] deltas = pixgeom.getDeltas();
        float[] matrix = RMatrix(rotation);
        for(int i=0;i<deltas.length/2;i++) {
            float x = deltas[2*i+0];
            float y = deltas[2*i+1];
            deltas[2*i+0] = x*matrix[0]+y*matrix[1];
            deltas[2*i+1] = -(x*matrix[2]+y*matrix[3]);//from display to GL coordinate
        }
        return deltas;
    }
    private static float[] RMatrix(int rotation) {
        switch(0x03&rotation) {
            default:
            case 0: return new float[]{1,0, 0,1};
            case 1: return new float[]{0,1, -1,0};
            case 2: return new float[]{-1,0, 0,-1};
            case 3: return new float[]{0,-1, 1,0};
        }
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {}

    int initVBO(GL10 glUnused) {
        int[] buf = new int[1];
        GLES20.glGenBuffers(1, buf, 0);
        int vbo = buf[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        FloatBuffer v = ByteBuffer.allocateDirect(4*VERTICES.length).order(ByteOrder.nativeOrder()).asFloatBuffer();
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4*VERTICES.length, v.put(VERTICES).position(0), GLES20.GL_STATIC_DRAW);
        GLES20.glEnableVertexAttribArray(0);
        checkGLError(glUnused);
        return vbo;
    }

    int genTexture(GL10 glUnused) {
        int[] buf = new int[1];
        GLES20.glGenTextures(1, buf, 0);
        return buf[0];
    }

    void loadTexture2D(GL10 glUnused, Bitmap bmp) {
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        checkGLError(glUnused);
    }

    String loadResource(String name) {
        Context context = surfaceView.getContext();
        int id = context.getResources().getIdentifier(name, "raw", context.getPackageName());
        return new String(Util.loadRaw(context, id));
    }
    boolean createProgram(GL10 glUnused, int program, String vert, String frag) {
        int shaderV = compileShader(glUnused, GLES20.GL_VERTEX_SHADER, vert);
        int shaderF = compileShader(glUnused, GLES20.GL_FRAGMENT_SHADER, frag);
        GLES20.glAttachShader(program, shaderV);
        GLES20.glAttachShader(program, shaderF);
        GLES20.glBindAttribLocation(program, 0, "a_V");
        GLES20.glDeleteShader(shaderV);
        GLES20.glDeleteShader(shaderF);
        checkGLError(glUnused);
        return linkProgram(glUnused, program);
    }
    void checkGLError(GL10 glUnused) {
        int error;
        while(GLES20.GL_NO_ERROR!=(error=GLES20.glGetError()))
            Log.e(TAG, GLErrorName(error));
    }

    void setRotation(int rotation) {
        this.rotation = rotation;
        surfaceView.requestRender();
    }
    int getRotation() {return rotation;}

    void setScreenMargins(@Size(value=4) int[] margins) {
        System.arraycopy(margins, 0, screenMargins, 0, screenMargins.length);
        Log.i(TAG, "Screen margins "+margins[0]+","+margins[1]+","+margins[2]+","+margins[3]);
        surfaceView.requestRender();
    }
    String pgSource() {
        int count = pixgeom.getPatternCount();
        if(count>1) {
            String src = loadResource("view_n");
            src = src.replace("$AS_Pg", Integer.toString(count));
            src = src.replace("$AS_Pw", Float.toString(1f/pixgeom.getWidth()));
            src = src.replace("$AS_Ph", Float.toString(1f/pixgeom.getHeight()));
            return src;
        } else
            return loadResource("view_1");
    }
    private static String GLErrorName(int error) {
        switch(error) {
            case GLES20.GL_INVALID_ENUM:
                return "INVALID ENUM";
            case GLES20.GL_INVALID_VALUE:
                return "INVALID VALUE";
            case GLES20.GL_INVALID_OPERATION:
                return "INVALID OPERATION";
            case GLES20.GL_INVALID_FRAMEBUFFER_OPERATION:
                return "INVALID FRAMEBUFFER OPERATION";
            case GLES20.GL_OUT_OF_MEMORY:
                return "OUT OF MEMORY";
            default:
                return "UNKNOWN ERROR";
        }
    }

    private int compileShader(GL10 glUnused, int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if(GLES20.GL_FALSE==compiled[0])
            System.err.println(GLES20.glGetShaderInfoLog(shader));
        return shader;
    }
    private boolean linkProgram(GL10 glUnused, int program) {
        GLES20.glLinkProgram(program);
        int[] linked = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0);
        if(GLES20.GL_FALSE==linked[0])
            System.err.println(GLES20.glGetProgramInfoLog(program));
        return GLES20.GL_TRUE==linked[0];
    }

    //---------------PITCH INTERFACE---------------
    void setPitches(PixelPitch pitches) {
        this.pitches = pitches;
        surfaceView.requestRender();
    }
    void setHorizontalPitch(float pitch) {
        pitches.setH(pitch);
        surfaceView.requestRender();
    }
    void setVerticalPitch(float pitch) {
        pitches.setV(pitch);
        surfaceView.requestRender();
    }
    void setPitch(float pitch) {
        pitches.setLenticularPitch(pitch);
        surfaceView.requestRender();
    }
    void setAngle(float angle) {
        pitches.setAngle((float)(angle*Math.PI/180));
        surfaceView.requestRender();
    }
    void setPixelGeometry(ASPixelGeometry pg) {
        pixgeom = pg;
        surfaceView.requestRender();
    }
    void setEyePosCoef(float eyeCoef) {
        surfaceView.requestRender();
    }

    int getWidth() {return surfaceView.getWidth();}
    int getHeight() {return surfaceView.getHeight();}

    PixelPitch getPitches() {return new PixelPitch(pitches);}
    PixelPitch getRotatedPitches() {
        PixelPitch pitches = getPitches();
        pitches.rotate();
        return pitches;
    }
    float getHorizontalPitch() {return pitches.h();}
    float getVerticalPitch() {return pitches.v();}
    float getPitch() {return pitches.getLenticularPitch();}
    float getAngle() {return (float)(pitches.getAngle()*180/Math.PI);}
    float getEyePosCoef() {return eyePosCoef;}
    ASPixelGeometry getPixelGeometry() {return pixgeom;}
}
