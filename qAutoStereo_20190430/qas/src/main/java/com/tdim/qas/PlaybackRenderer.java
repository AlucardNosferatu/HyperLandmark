package com.tdim.qas;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;         //API level 1
import android.graphics.SurfaceTexture; //API level 11
import android.opengl.GLES20;           //API level 8
import android.util.Log;                //API level 1

import com.tdim.qas.ASConstants.InputFormat;

final class PlaybackRenderer extends Renderer implements SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = PlaybackRenderer.class.getSimpleName();
    private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
    private static final int MAX_VIEWS = 12;//theorectially getMaxSupportedViews
    private static final int FLAG_TEXTURE = 0;
    private static final int FLAG_OFFSCREEN = 1;
    private static final int FLAG_UNIFORM = 2;
    private static final int FLAG_PROGRAM = 3;
    private static final int FLAG_MASTER = 4;
    private static final int LOGO_DUR_INVISIBLE = 5000;
    private static final int LOGO_DUR_FADE      = 3000;
    private static final int LOGO_DUR_OPAQUE    = 2000;

    interface SurfaceTextureListener {
        void onCreated(SurfaceTexture surfaceTexture);
    }

    private enum PlaybackStatus {
        Stop, Play, Pause
    }

    private static String glVendor;
    private static String glRenderer;
    private static String glVersion;
    private static String glSLVersion;
    private static int maxGLValues[] = new int[]{64, 0, 8, 128, 8, 8, 0, 8, 16, 16, 1};
    private static int viewportW, viewportH;

    private final ASPlaybackView playbackView;
    private final boolean[] updateFlags = new boolean[5];
    private final float[] matTR = new float[]{1,0,0,1, 0,0};//rotation, translation
    private final float[] matRS = new float[]{1,0,0,1};//scaling, rotation

    private SurfaceTexture surface;
    private SurfaceTextureListener listener;
    private Bitmap image, logo;
    private PlaybackStatus status = PlaybackStatus.Stop;
    private boolean playPending = true;
    private long pendTime = System.currentTimeMillis();
    private float viewBase, viewOffset;
    private float logoAspect = 1;
    private int logoViewport[] = new int[4];
    private long logoBasetime;

    private InputFormat format = InputFormat.None;
    private int textureW, textureH;

    private int vbo;
    private int target;
    private int textureExternal, texture2D;
    private int program, programLogo;
    private int uniloc[] = new int[]{
            -1, //0 u_M: texcoord transform rotation + tileformat scales
            -1, //1 u_T: texcoord transform translation
            -1, //2 u_O: tileformat offset of second view
            -1, //3 u_Z: zeroplane shift
            -1, //4 u_view: view offset
            -1, //5 u_C: #views, <=#tiles. Set to lower for less disparity
            -1, //6 u_R: deltas of red channel
            -1, //7 u_G: deltas of red channel
            -1, //8 u_B: deltas of red channel
    };
    private int unilogoloc = -1;
    private boolean autostereo = true; //true if there is 3D

    static String getVendor() {return glVendor;}
    static String getRenderer() {return glRenderer;}
    static String getGLVersion() {return glVersion;}
    static String getSLVersion() {return glSLVersion;}
    static int getMaxTextureSize() {return maxGLValues[0];}
    static int getMaxSupportedViews() {return maxGLValues[8]-4;}
    static int getViewportW() {return viewportW;}
    static int getViewportH() {return viewportH;}

    PlaybackRenderer(ASPlaybackView view) {
        super(view);
        playbackView = view;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surface) {//main thread
        setFlag(FLAG_TEXTURE);
        if(isPaused())
            playbackView.pause();
        playbackView.onAdvance();
        requestRender();
    }
    private synchronized boolean isPaused() {return PlaybackStatus.Pause==status;}

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        Log.i(TAG, GLES20.glGetString(GLES20.GL_VENDOR)+" "+GLES20.glGetString(GLES20.GL_RENDERER)+" "+GLES20.glGetString(GLES20.GL_VERSION));

        vbo = initVBO(glUnused);

        texture2D = genTexture(glUnused);
        textureExternal = genTexture(glUnused);
        surface = new SurfaceTexture(textureExternal);
        surface.setOnFrameAvailableListener(this);
        if(null!=listener)
            listener.onCreated(surface);
        Log.i(TAG, "PlaybackRenderer context: "+this);
        target = 0;

        program = 0;
        setFlag(FLAG_PROGRAM);

        glVendor = GLES20.glGetString(GLES20.GL_VENDOR);
        glRenderer = GLES20.glGetString(GLES20.GL_RENDERER);
        glVersion = GLES20.glGetString(GLES20.GL_VERSION);
        glSLVersion = GLES20.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION);

        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE,                 maxGLValues, 0);//min 64
        GLES20.glGetIntegerv(GLES20.GL_MAX_VIEWPORT_DIMS,                maxGLValues, 1);//min screensize
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS,               maxGLValues, 2);//min 8
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_UNIFORM_VECTORS,       maxGLValues, 3);//min 128
        GLES20.glGetIntegerv(GLES20.GL_MAX_VARYING_VECTORS,              maxGLValues, 4);//min 8
        GLES20.glGetIntegerv(GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, maxGLValues, 5);//min 8
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS,   maxGLValues, 6);//min 0
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS,          maxGLValues, 7);//min 8
        GLES20.glGetIntegerv(GLES20.GL_MAX_FRAGMENT_UNIFORM_VECTORS,     maxGLValues, 8);//min 16
        GLES20.glGetIntegerv(GLES20.GL_MAX_CUBE_MAP_TEXTURE_SIZE,        maxGLValues, 9);//min 16
        GLES20.glGetIntegerv(GLES20.GL_MAX_RENDERBUFFER_SIZE,            maxGLValues,10);//min 1

        GLES20.glClearColor(0, 0, 0, 1);

        if(null!=logo) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, genTexture(glUnused));
            loadTexture2D(glUnused, logo);
            logoAspect = 0<logo.getWidth()&&0<logo.getHeight()?logo.getWidth()/(float)logo.getHeight():1;
            logo = null;
            logoBasetime = System.nanoTime();
            programLogo = GLES20.glCreateProgram();
            if(createProgram(glUnused, programLogo, loadResource("logo_v"), toTargetFrag(loadResource("logo_f")))) {
                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                unilogoloc = GLES20.glGetUniformLocation(programLogo, "u_A");
                GLES20.glUseProgram(programLogo);
                GLES20.glUniform1i(GLES20.glGetUniformLocation(programLogo, "u_D"), 2);
            } else {
                GLES20.glDeleteProgram(programLogo);
                programLogo = 0;
            }
        }
    }
    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        viewportW = width;
        viewportH = height;
        logoViewport[2] = 3*viewportW/4;
        logoViewport[3] = 3*viewportH/4;
        if(viewportH*logoAspect<viewportW) //screen wider than logo
            logoViewport[2] = Math.round(logoViewport[3]*logoAspect);
        else //screen taller than logo
            logoViewport[3] = Math.round(logoViewport[2]/logoAspect);
        logoViewport[0] = (viewportW-logoViewport[2])/2;
        logoViewport[1] = (viewportH-logoViewport[3])/2;
        setFlag(FLAG_OFFSCREEN);
        requestRender();
    }
    @Override
    public void onDrawFrame(GL10 glUnused) {
        boolean pending;
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        if(popFlag(FLAG_TEXTURE)) {
            updateInputTexture(glUnused);
            synchronized(this) {
                if(playPending && PlaybackStatus.Stop!=status)
                    Log.i(TAG, "Was pending for "+(System.currentTimeMillis()-pendTime)+"ms");
                playPending &= PlaybackStatus.Stop==status;
                pending = playPending;
            }
            setFlag(FLAG_MASTER);
        } else if(0==program)
            return;
        else
            synchronized(this) {pending = playPending;}

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(0, 4, GLES20.GL_FLOAT, false, 0, 0);
        if(pending) {
            GLES20.glViewport(0, 0, viewportW, viewportH);
            GLES20.glUseProgram(program);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
            drawLogo(glUnused);
            return;
        }

        if(popFlag(FLAG_PROGRAM))
            updateRenderProgram(glUnused);
        drawGrid(glUnused);
        drawLogo(glUnused);
    }

    private void drawLogo(GL10 glUnused) {
        if(0<programLogo) {
            GLES20.glViewport(logoViewport[0], logoViewport[1], logoViewport[2], logoViewport[3]);
            GLES20.glUseProgram(programLogo);
            GLES20.glUniform1f(unilogoloc, calcLogoAlpha());
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
        }
    }

    private float calcLogoAlpha() {
        int msDiff = (int)((System.nanoTime()-logoBasetime)/1000000);
        int interval = msDiff%(LOGO_DUR_OPAQUE+LOGO_DUR_INVISIBLE+2*LOGO_DUR_FADE);
        if(LOGO_DUR_INVISIBLE>interval)
            return 0;
        else if(LOGO_DUR_INVISIBLE+LOGO_DUR_FADE>interval)
            return (interval-LOGO_DUR_INVISIBLE)/(float)LOGO_DUR_FADE;
        else if(LOGO_DUR_INVISIBLE+LOGO_DUR_FADE+LOGO_DUR_OPAQUE>interval)
            return 1;
        else
            return (LOGO_DUR_INVISIBLE+LOGO_DUR_OPAQUE+2*LOGO_DUR_FADE-interval)/(float)LOGO_DUR_FADE;
    }

    private void drawGrid(GL10 glUnused) {
        GLES20.glViewport(0, 0, viewportW, viewportH);
        GLES20.glUseProgram(program);
        if(popFlag(FLAG_UNIFORM)) {
            GLES20.glUniformMatrix2fv(uniloc[0], 1, false, matRS, 0);//TRS rotation
            GLES20.glUniform2f(uniloc[1], matTR[4], matTR[5]);//TRS translation
            int cols = getCols(format);
            int rows = getRows(format);
            updateRenderUniforms(glUnused, cols, rows, cols*rows, matTR);
        }
        //GLES20.glUniform1f(uniloc[3], 0);//zeroplane
        GLES20.glUniform1f(uniloc[4], viewBase+viewOffset);//view ofset
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
        checkGLError(glUnused);
    }

    private void updateRenderUniforms(GL10 glUnused, int cols, int rows, int count, float[] rot) {
        float s0 = 1/(float)cols;
        float t0 = 1-1/(float)rows;
        float s, t;
        if(null==rot) { //assume identity matrix
            s = s0;
            t = t0;
        } else {
            s = rot[0]*s0+rot[2]*t0;
            t = rot[1]*s0+rot[3]*t0;
        }
        GLES20.glUniform2f(uniloc[2], s, t);//tile offsets
        GLES20.glUniform1f(uniloc[5], count);//offset array count
        float[] deltas = subpixelDelta();
        int types = deltas.length/6;
        GLES20.glUniform2fv(uniloc[6], types, deltas, 0*types);
        GLES20.glUniform2fv(uniloc[7], types, deltas, 2*types);
        GLES20.glUniform2fv(uniloc[8], types, deltas, 4*types);
        checkGLError(glUnused);
    }

    void play() {setPlaybackStatus(PlaybackStatus.Play);}
    void pause() {setPlaybackStatus(PlaybackStatus.Pause);}
    void stop() {setPlaybackStatus(PlaybackStatus.Stop);}
    private void setPlaybackStatus(PlaybackStatus status) {
        synchronized(this) {
            this.status = status;
            if(!playPending && PlaybackStatus.Stop==status)
                pendTime = System.currentTimeMillis();
            playPending |= PlaybackStatus.Stop==status;
        }
        requestRender();
    }

    void setSurfaceTextureListener(SurfaceTextureListener listener) {this.listener = listener;}
    void setImage(Bitmap bmp) {
        if(null==bmp)
            return;
        synchronized(this) {image = bmp;}
        setFlag(FLAG_TEXTURE);
        setInputSize(bmp.getWidth(), bmp.getHeight());
        requestRender();
    }
    void setWatermark(Bitmap bmp) {logo = bmp;}

    void set3DMode(boolean state) {
        autostereo = state;
        setFlag(FLAG_PROGRAM);
        requestRender();
    }
    void setDisplay(float h, float v, ASPixelGeometry pg) {
        setFlag(FLAG_PROGRAM);
        setPitches(new PixelPitch(h, v));
        setPixelGeometry(pg);
    }
    void setViewBase(float b) {
        viewBase = (float)(b-Math.floor(b));
        requestRender();
    }
    void setViewOffset(float b) {
        viewOffset = b;
        requestRender();
    }
    @Override
    void setRotation(int rotation) {
        super.setRotation(rotation);
        if(autostereo) {
            setFlag(FLAG_PROGRAM);
            requestRender();
        }
    }

    void setInputSize(int width, int height) {
        if(width==textureW && height==textureH)
            return;
        textureW = width;
        textureH = height;
        requestRender();
    }

    void setInputFormat(InputFormat format) {
        this.format = format;
        updateRSmat();
        requestRender();
    }


    private void updateInputTexture(GL10 glUnused) {
        float[] trafo = new float[]{1,0,0,0, 0,-1,0,0, 0,0,1,0, 0,1,0,1};
        Bitmap img;
        synchronized(this) {
            img = image;
            image = null;
        }
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureExternal);
        surface.updateTexImage();
        if(null==img) {
            surface.getTransformMatrix(trafo);
            if(GL_TEXTURE_EXTERNAL_OES!=target) {
                target = GL_TEXTURE_EXTERNAL_OES;
                setFlag(FLAG_PROGRAM);
            }
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture2D);
            loadTexture2D(glUnused, img);
            if(GLES20.GL_TEXTURE_2D!=target) {
                target = GLES20.GL_TEXTURE_2D;
                setFlag(FLAG_PROGRAM);
            }
        }
        if(trafo[12]!=matTR[4] || trafo[13]!=matTR[5]) {//translation change implies rotation change
            System.arraycopy(trafo, 0, matTR, 0, 2);
            System.arraycopy(trafo, 4, matTR, 2, 2);
            System.arraycopy(trafo,12, matTR, 4, 2);
            updateRSmat();
        }
        checkGLError(glUnused);
    }
    private void updateRSmat() {
        float s = 1f/getCols(format);
        float t = 1f/getRows(format);
        matRS[0] = s*matTR[0];
        matRS[1] = s*matTR[1];
        matRS[2] = t*matTR[2];
        matRS[3] = t*matTR[3];
        setFlag(FLAG_UNIFORM);
    }

    private void updateRenderProgram(GL10 glUnused) {
        if(0<program)
            GLES20.glDeleteProgram(program);
        program = GLES20.glCreateProgram();
        createGrid(glUnused);
        setFlag(FLAG_UNIFORM);
        checkGLError(glUnused);
    }
    private void createGrid(GL10 glUnused) {
        PixelPitch pitches = getPitches();
        String vert = loadResource("grid_v");
        String frag = loadResource("grid_f");
        if(autostereo&&pitches.has3D())
            frag = frag
                    .replace("$AS_F", loadResource("mix__3"))
                    .replace("$AS_H", Float.toString(pitches.h()))
                    .replace("$AS_V", Float.toString(pitches.v()))
                    .replace("$AS_Ph", pgSource());
        else //2D rendering
            frag = frag.replace("$AS_F", loadResource("mix__0"));
        if(createProgram(glUnused, program, vert, toTargetFrag(frag))) {
            uniloc[0] = GLES20.glGetUniformLocation(program, "u_M");
            uniloc[1] = GLES20.glGetUniformLocation(program, "u_T");
            uniloc[2] = GLES20.glGetUniformLocation(program, "u_O");
            uniloc[3] = GLES20.glGetUniformLocation(program, "u_Z");
            uniloc[4] = GLES20.glGetUniformLocation(program, "u_view");
            uniloc[5] = GLES20.glGetUniformLocation(program, "u_C");
            uniloc[6] = GLES20.glGetUniformLocation(program, "u_R");
            uniloc[7] = GLES20.glGetUniformLocation(program, "u_G");
            uniloc[8] = GLES20.glGetUniformLocation(program, "u_B");
            // THIS LINE WAS CRASH 3d RENDERING IN PLAYER !!!
            //GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "u_pattern"), 1);
            int loc = GLES20.glGetUniformLocation(this.program, "u_pattern");
            if (-1 < loc) {
                GLES20.glUseProgram(this.program);
                GLES20.glUniform1i(loc, 1);
                GLES20.glUseProgram(0);
            }
        }
    }

    private String toTargetFrag(String frag) {
        return GL_TEXTURE_EXTERNAL_OES==target
                ?"#extension GL_OES_EGL_image_external : require\n"+frag.replace("sampler2D u_F", "samplerExternalOES u_F")
                :frag;
    }

    private void setFlag(int flag) {synchronized(updateFlags) {updateFlags[flag] = true;}}
    private boolean popFlag(int flag) {
        synchronized(updateFlags) {
            boolean value = updateFlags[flag];
            updateFlags[flag] = false;
            return value;
        }
    }

    private static int getCols(InputFormat format) {
        return InputFormat.SBS==format?2:1;
    }
    private static int getRows(InputFormat format) {
        return InputFormat.TB==format?2:1;
    }
}
