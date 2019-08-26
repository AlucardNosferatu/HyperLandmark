package com.tdim.qas;

final class PixelPitch {
    private static final float PITCH_ERROR = 1/256f;
    private static final float ANGLE_ERROR = 1/65536f;
    private static float reciprocal(float value) {return PITCH_ERROR<Math.abs(value)?1/value:0;}
    static PixelPitch fromPitchAngle(float pitch, double angleR) {
        PixelPitch p = new PixelPitch();
        p.setLenticularPitch(pitch);
        p.setAngle(angleR);
        return p;
    }

    private float h, v;
    private double angle; //in rad

    PixelPitch() {this(0, 0);}
    PixelPitch(float h, float v) {setPitches(h, v);}
    PixelPitch(PixelPitch pitches) {
        h = pitches.h;
        v = pitches.v;
        angle = pitches.angle;
    }

    float h() {return h;}
    float v() {return v;}
    void setH(float h) {this.h = h; updateAngle();}
    void setV(float v) {this.v = v; updateAngle();}
    void setPitches(float h, float v) {
        this.h = h;
        this.v = v;
        updateAngle();
    }
    private void updateAngle() {
        boolean noH = PITCH_ERROR>Math.abs(h);
        boolean noV = PITCH_ERROR>Math.abs(v);
        if(noH&&noV)
            return; //if both are 0 leave angle as is
        angle = noV?0:(noH?0.5*Math.PI:Math.atan(v/h));
    }
    float getLenticularPitch() {return (float)Math.hypot(reciprocal(h), reciprocal(v));}
    double getAngle() {return angle;}
    void setLenticularPitch(float pitch) {
        float sin = (float)Math.sin(angle);
        float cos = (float)Math.cos(angle);
        if(ANGLE_ERROR>Math.abs(sin)) { //vertical angle
            h = reciprocal(pitch);
            v = 0;
        } else if(ANGLE_ERROR>Math.abs(cos)) { //horizontal angle
            h = 0;
            v = reciprocal(pitch);
        } else {
            float signH = reciprocal(pitch*sin);
            h = Math.abs(signH);
            v = (0f>signH?-1:1)*reciprocal(pitch*cos);
        }
    }
    void setAngle(double angle) {
        this.angle = angle;
        setLenticularPitch(getLenticularPitch());
    }
    void rotate() {
        float sign = 0f>v?-1:1;
        float negH = -h;
        h = Math.abs(v);
        v = sign*negH;
        if(PITCH_ERROR<getLenticularPitch()||ANGLE_ERROR<Math.abs(angle))
            angle = angle-(0<angle?1:-1)*0.5*Math.PI;
    }
    boolean has3D() {return PITCH_ERROR<Math.abs(h)&&PITCH_ERROR<Math.abs(v);}

    @Override
    public String toString() {return h+"/"+v;}
}
