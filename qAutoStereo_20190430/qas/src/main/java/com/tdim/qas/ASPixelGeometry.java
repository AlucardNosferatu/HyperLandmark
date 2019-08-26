package com.tdim.qas;

import android.graphics.PointF;     //API level 1
import android.graphics.RectF;      //API level 1
import android.util.SparseIntArray; //API level 1

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class ASPixelGeometry {
    public static final int NONE    = 0;
    public static final int STRIPES = 1;
    public static final int PT_DIAMOND = 2;
    public static final int COUNT = 3;//number of predefined PGs, update when adding PG!
    public static final int CUSTOM  = -1;
    private static final int OFFSET_FACT = 1<<30;
    private final String name;
    private final int type;
    private int width, height;
    private int patternCount;
    private int[] matrix; //width*height indices
    private float[] patternsRGB;//topleft base rx, ry, gx, gy, bx, by

    public class Pixel {
        public final PointF red;
        public final PointF green;
        public final PointF blue;

        Pixel() {red=green=blue = new PointF(0, 0);}
        Pixel(float[] patterns, int offset) {
            red     = new PointF(patterns[offset+0], patterns[offset+1]);
            green   = new PointF(patterns[offset+2], patterns[offset+3]);
            blue    = new PointF(patterns[offset+4], patterns[offset+5]);
        }
    }

    public static ASPixelGeometry getPixelGeomtery(int type) {
        switch(type) {
            case NONE:
                return new ASPixelGeometry("Fullpixel", type);
            case STRIPES: {
                ASPixelGeometry stripes = new ASPixelGeometry("Stripes", type);
                stripes.setPattern(0, new float[]{-1f/3, 0, 0, 0, +1f/3, 0});
                return stripes;
            }
            case PT_DIAMOND: {
                ASPixelGeometry pt_diamond = new ASPixelGeometry("Diamond PenTile", type);
                pt_diamond.setMatrix(2, 2, new int[]{0, 1, 1, 0});
                pt_diamond.setPattern(0, new float[]{+0.5f, 0.5f, 0, 0, -0.5f, 0.5f});
                pt_diamond.setPattern(1, new float[]{-0.5f, 0.5f, 0, 0, +0.5f, 0.5f});
                pt_diamond.normalize();
                return pt_diamond;
            }
            default:
                throw new IllegalArgumentException("Unknown pixel geometry "+type);
        }
    }

    ASPixelGeometry(String name) {this(name, CUSTOM);}
    private ASPixelGeometry(String name, int type) {
        this.name = name;
        this.type = type;
        setMatrix(1, 1, null);
        setPatternCount(1);
    }

    public String getName() {return name;}
    public int getType() {return type;}
    public boolean isCustom() {return CUSTOM==type;}

    public int getWidth() {return width;}
    public int getHeight() {return height;}
    public int[] getMatrix() {
        int[] copy = new int[matrix.length];
        System.arraycopy(matrix, 0, copy, 0, matrix.length);
        return copy;
    }

    public int getPatternCount() {return patternCount;}
    public Pixel getPattern(int index) {return -1<index&&patternCount>index?new Pixel(patternsRGB, 6*index):new Pixel();}
    public float[] getRawPattern() {
        float[] copy = new float[patternsRGB.length];
        System.arraycopy(patternsRGB, 0, copy, 0, patternsRGB.length);
        return copy;
    }

    public float[] getDeltas() {
        float[] delta = new float[patternsRGB.length];
        for(int i=0;i<patternCount;i++) {
            delta[0*patternCount+2*i+0] = patternsRGB[6*i+0];
            delta[0*patternCount+2*i+1] = patternsRGB[6*i+1];
            delta[2*patternCount+2*i+0] = patternsRGB[6*i+2];
            delta[2*patternCount+2*i+1] = patternsRGB[6*i+3];
            delta[4*patternCount+2*i+0] = patternsRGB[6*i+4];
            delta[4*patternCount+2*i+1] = patternsRGB[6*i+5];
        }
        return delta;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ASPixelGeometry))
            return false;
        ASPixelGeometry other = (ASPixelGeometry)obj;
        if(other.width==width&&other.height==height&&Arrays.equals(other.matrix, matrix)&&other.patternsRGB.length==patternsRGB.length) {
            for(int i=0;i<patternsRGB.length;i++)
                if(0.001f<Math.abs(other.patternsRGB[i]-patternsRGB[i]))
                    return false;
            return true;
        } else
            return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        sb.append("\n");
        for(int y=0;y<height;y++) {
            for(int x=0;x<width;x++)
                sb.append(" ").append(matrix[y*width+x]);
            sb.append("\n");
        }
        for(int i=0;i<patternCount;i++) {
            sb.append("Pattern ").append(i).append("\n");
            sb.append(" R ").append(format(patternsRGB[6*i+0])).append(" ").append(format(patternsRGB[6*i+1])).append("\n");
            sb.append(" G ").append(format(patternsRGB[6*i+2])).append(" ").append(format(patternsRGB[6*i+3])).append("\n");
            sb.append(" B ").append(format(patternsRGB[6*i+4])).append(" ").append(format(patternsRGB[6*i+5])).append("\n");
        }
        return sb.toString();
    }
    private static String format(float value) {return String.format(Locale.getDefault(), "%+.3f", value);}

    void setMatrix(int w, int h, int[] src) {
        width = Math.max(1, w);
        height = Math.max(1, h);
        matrix = new int[width*height];
        setMatrix(src);
    }
    public void setMatrix(int[] src) {
        if(null==src||src.length<matrix.length)
            return;
        System.arraycopy(src, 0, matrix, 0, matrix.length);
        Set<Integer> patterns = new HashSet<>();
        for(int i: matrix)
            patterns.add(i);
        setPatternCount(patterns.size());
    }

    void setPatternCount(int count) {
        patternCount = Math.max(1, count);
        patternsRGB = new float[6*patternCount];
    }
    void setPattern(int index, float[] pattern) {setPattern(index, pattern, 0);}
    void setPattern(int index, float[] pattern, int offset) {
        if(-1<index&&patternCount>index&&6<=pattern.length)
            System.arraycopy(pattern, offset, patternsRGB, 6*index, 6);
    }

    void normalize() {
        SparseIntArray indexMap = new SparseIntArray();
        int count = 0;
        for(int id: matrix)
            if(-1==indexMap.get(id, -1))
                indexMap.put(id, count++);
        int[] oldMatrix = matrix;
        float[] oldPattern = patternsRGB;
        matrix = new int[matrix.length];
        setPatternCount(count);
        for(int i=0;i<matrix.length;i++) {
            int oldIndex = oldMatrix[i];
            int newIndex = indexMap.get(oldIndex);
            matrix[i] = newIndex;
            setPattern(newIndex, oldPattern, 6*oldIndex);
        }
        PointF center = new PointF(0, 0);
        for(int i=0;i<patternCount;i++) {
            RectF rect = new RectF(0.5f, 0.5f, -0.5f, -0.5f);
            for(int c=0;c<6;c+=2) {
                float x = patternsRGB[6*i+c+0];
                float y = patternsRGB[6*i+c+1];
                rect.left   = Math.min(x, rect.left);
                rect.top    = Math.min(y, rect.top);
                rect.right  = Math.max(x, rect.right);
                rect.bottom = Math.max(y, rect.bottom);
            }
            center.offset(0.5f*(rect.right+rect.left), 0.5f*(rect.bottom+rect.top));
        }
        center.x /= patternCount;
        center.y /= patternCount;
        for(int i=0;i<patternsRGB.length;i+=2) {
            patternsRGB[i+0] -= center.x;
            patternsRGB[i+1] -= center.y;
        }
    }

    static ASPixelGeometry load(InputStream is) {
        ASPixelGeometry pg = new ASPixelGeometry(Util.nextString(is));
        int width = Util.nextInt(is);
        int height = Util.nextInt(is);
        pg.setMatrix(width, height, null);
        for(int i=0;i<pg.matrix.length;i++)
            pg.matrix[i] = Util.nextInt(is);
        pg.setPatternCount(Util.nextInt(is));
        for(int i=0;i<pg.patternsRGB.length;i++)
            pg.patternsRGB[i] = Util.nextInt(is)/(float)OFFSET_FACT;
        return pg;
    }
    void save(OutputStream os) throws IOException {
        os.write(Util.toBytes(name));
        os.write(Util.toBytes(width));
        os.write(Util.toBytes(height));
        for(int i: matrix)
            os.write(Util.toBytes(i));
        os.write(Util.toBytes(patternCount));
        for(float f: patternsRGB)
            os.write(Util.toBytes((int)(OFFSET_FACT*f)));
    }
}
