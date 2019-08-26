package com.tdim.qas;

import android.graphics.Bitmap;     //API level 1
import android.opengl.GLES20;       //API level 8
import android.opengl.GLSurfaceView;//API level 3

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

abstract class CalibratableRenderer extends Renderer {
    private static final float BARS_WIDTH = 1f/120;//=1/width in pixels
    private static final float STRIPE_WIDTH = 1f/32;//=1/width in pixels
    int program;
    boolean updateProgram = true;
    private int vbo;
    private int texture, textureMatrix;
    private Mode mode = Mode.Bars;
    private int uniloc = -1;//of params uniform
    private int[] unilocDeltas = new int[]{-1, -1, -1};//of RGB deltas
    private Bitmap bmp;

    enum Mode {
        Angle,
        Colors,
        Bars,
        Rainbow,
        Picture,
        EyePosCoef
    }

    CalibratableRenderer(GLSurfaceView view) {super(view);}

    void setPixelGeometry(ASPixelGeometry pg) {
        updateProgram = true;
        super.setPixelGeometry(pg);
    }

    void setBitmap(Bitmap bmp) {
        this.bmp = bmp;
        updateProgram |= isModeWithImages();
        requestRender();
    }
    void setMode(Mode mode) {
        if(this.mode==mode)
            return;
        this.mode = mode;
        updateProgram = true;
        requestRender();
    }

    void updateProgram(GL10 glUnused) {
        if(0<program)
            GLES20.glDeleteProgram(program);
        program = GLES20.glCreateProgram();
        String vert = loadResource(modeVertex());
        String frag = loadResource(modeFragment());
        frag = frag.replace("$AS_Ph", pgSource().replace("c_P", "u_P"));
        if(createProgram(glUnused, program, vert, frag)) {
            uniloc = GLES20.glGetUniformLocation(program, "u_P");
            unilocDeltas[0] = GLES20.glGetUniformLocation(program, "u_R");
            unilocDeltas[1] = GLES20.glGetUniformLocation(program, "u_G");
            unilocDeltas[2] = GLES20.glGetUniformLocation(program, "u_B");
            GLES20.glUseProgram(program);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "u_pattern"), 1);
            updateUniforms(glUnused);
            GLES20.glUseProgram(0);
        } else {
            GLES20.glDeleteProgram(program);
            program = 0;
        }
    }
    private String modeVertex() {return isModeWithImages()?"xyst_v":"xy___v";}
    private String modeFragment() {
        switch(mode) {
            case Angle:     return "angl_f";
            case Colors:    return "clrs_f";
            case Bars:      return "bars_f";
            case Rainbow:   return "rain_f";
            case Picture:   return "pict_f";
            case EyePosCoef:  return "pict_f";
            default:
                throw new IllegalArgumentException();
        }
    }

    private boolean isModeWithImages(){
        return ((Mode.Picture == mode) || (Mode.EyePosCoef == mode) );
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig eglConfig) {
        vbo = initVBO(glUnused);
        texture = genTexture(glUnused);
        textureMatrix = genTexture(glUnused);
        GLES20.glClearColor(0, 0, 0, 1);
        checkGLError(glUnused);
    }
    @Override
    public void onDrawFrame(GL10 glUnused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        if(null!=bmp) {
            loadTexture2D(glUnused, bmp);
            bmp = null;
        }
        if(updateProgram) {
            updateProgram = false;
            updateProgram(glUnused);
        }
        if(0==program)
            return;

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(0, 4, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glUseProgram(program);
        PixelPitch pitches = getPitches();
        float h, v;
        switch(mode) {
            default:
                h = pitches.h();
                v = pitches.v();
                break;
            case Bars:
                h = pitches.h()+BARS_WIDTH;
                v = pitches.v();
                break;
            case Angle: {
                double angle = pitches.getAngle();
                h = STRIPE_WIDTH*(float)Math.cos(angle);
                v = STRIPE_WIDTH*(float)Math.sin(angle);
            }
            break;
        }
        GLES20.glUniform2f(uniloc, h, v);

        ASPixelGeometry pg = getPixelGeometry();
        float[] deltas = subpixelDelta();
        int count = pg.getPatternCount();
        GLES20.glUniform2fv(unilocDeltas[0], count, deltas, 0*count);
        GLES20.glUniform2fv(unilocDeltas[1], count, deltas, 2*count);
        GLES20.glUniform2fv(unilocDeltas[2], count, deltas, 4*count);
        if(1<count)
            loadTexture(pg);
        setUniforms(glUnused);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
        checkGLError(glUnused);
    }
    private void loadTexture(ASPixelGeometry pg) {
        int[] base = BLBase(getRotation());
        float[] rmat = RMatrix(getRotation());
        GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "u_base"), base[0], base[1]);
        GLES20.glUniformMatrix2fv(GLES20.glGetUniformLocation(program, "u_pixrot"), 1, false, rmat, 0);

        int w = pg.getWidth();
        int h = pg.getHeight();
        int stride = (~0x03)&(3*w+0x03);//3 components, 4-byte aligned
        byte[] data = new byte[stride*h];
        byte[] map = new byte[pg.getPatternCount()];
        for(int i=0;i<map.length;i++)
            map[i] = (byte)(256*i/pg.getPatternCount());
        int[] matrix = pg.getMatrix();
        for(int y=0;y<h;y++) {
            for(int x=0;x<w;x++) {
                int index = y*stride+3*x;
                data[index+0]=data[index+1]=data[index+2] = map[matrix[y*w+x]];
            }
        }
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureMatrix);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, w, h, 0,
                GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, ByteBuffer.allocateDirect(data.length).put(data).position(0));
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
    }
    private int[] BLBase(int rotation){
        switch(0x03&rotation) {
            default:
            case 0: return new int[]{screenMargins[0], screenMargins[1]+getHeight()-1};
            case 1: return new int[]{screenMargins[3], screenMargins[0]};
            case 2: return new int[]{screenMargins[2]+getWidth()-1, screenMargins[3]};
            case 3: return new int[]{screenMargins[1]+getWidth()-1, screenMargins[2]+getHeight()-1};
        }
    }
    private static float[] RMatrix(int rotation) {
        switch(0x03&rotation) {
            default:
            case 0: return new float[]{1,0, 0,-1};
            case 1: return new float[]{0,1, 1,0};
            case 2: return new float[]{-1,0, 0,1};
            case 3: return new float[]{0,-1, -1,0};
        }
    }

    void updateUniforms(GL10 glUnused) {}
    void setUniforms(GL10 glUnused) {}
}
