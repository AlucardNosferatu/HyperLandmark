package com.tdim.qas;

import android.content.Context;     //API level 1
import android.graphics.Bitmap;     //API level 1
import android.media.MediaScannerConnection;    //API level 8
import android.util.AttributeSet;   //API level 1

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public final class ASCalibrationView extends RotatableSurfaceView {
    private static final String[] COMPONENTS = new String[]{"red", "green", "blue"};
    private static final String PG_FILE_NAME = "pixgeom.bin";
    public enum CalibMode {
        Angle,
        Colors,
        Bars,
        Rainbow,
        Picture,
        EyePosCoef
    }
    public class ParserException extends Exception {
        ParserException(String message) {super(message);}
        ParserException(Throwable cause) {super(cause);}
    }
    private final CalibrationRenderer renderer;
    private List<ASPixelGeometry> geometries = new ArrayList<>();
    private ASPixelGeometry currentGeometry;

    public ASCalibrationView(Context context) {this(context, null);}
    public ASCalibrationView(final Context context, AttributeSet attrs) {
        super(context, attrs);
        initGeometries();
        if(isInEditMode())
            renderer = null;
        else {
            setEGLContextClientVersion(2);
            setEGLConfigChooser(false);
            renderer = new CalibrationRenderer(this);
            setRenderer(renderer);
            ASLicensing.initialize(context);
            renderer.setActivated(ASLicensing.isValid(context));
            renderer.setRotation(getDeviceRotation());
            setRenderMode(RENDERMODE_WHEN_DIRTY);
            restore();
        }
    }
    private void initGeometries() {
        geometries.add(ASPixelGeometry.getPixelGeomtery(ASPixelGeometry.NONE));
        geometries.add(ASPixelGeometry.getPixelGeomtery(ASPixelGeometry.STRIPES));
        geometries.add(ASPixelGeometry.getPixelGeomtery(ASPixelGeometry.PT_DIAMOND));
        loadCustonPG();
    }

    private static final String CALIB_FILE_NAME = "calib.cfg";
    private static final String JSON_VERSION = "version";
    private static final String JSON_ANGLE = "angle";
    private static final String JSON_HPITCH = "hpitch";
    private static final String JSON_VPITCH = "vpitch";
    private static final String JSON_PGTYPE = "pixgeom";
    private static final String JSON_PGNAME = "pgname";
    private static final String JSON_PGWIDTH = "pgwidth";
    private static final String JSON_PGHEIGHT = "pgheight";
    private static final String JSON_PGMATRIX = "pgmatrix";
    private static final String JSON_PGCOUNT = "pgcount";
    private static final String JSON_PGPATTERN = "pgpattern";
    public boolean save() {
        JSONObject document = new JSONObject();
        PixelPitch pitches = renderer.getPitches();
        if(isLandscape())
            pitches.rotate();
        try {
            document.put(JSON_VERSION, 1);
            document.put(JSON_HPITCH, pitches.h());
            document.put(JSON_VPITCH, pitches.v());
            document.put(JSON_ANGLE, pitches.getAngle());
            ASPixelGeometry pg = renderer.getPixelGeometry();
            document.put(JSON_PGTYPE, pg.getType());
            if(pg.isCustom()) {
                document.put(JSON_PGNAME, pg.getName());
                document.put(JSON_PGWIDTH, pg.getWidth());
                document.put(JSON_PGHEIGHT, pg.getHeight());
                JSONArray matrix = new JSONArray();
                for(int i: pg.getMatrix())
                    matrix.put(i);
                document.put(JSON_PGMATRIX, matrix);
                document.put(JSON_PGCOUNT, pg.getPatternCount());
                JSONArray patterns = new JSONArray();
                for(float f: pg.getRawPattern())
                    patterns.put(f);
                document.put(JSON_PGPATTERN, patterns);
            }
            byte[] data = document.toString().getBytes(Charset.forName("UTF-8"));
            return Util.saveInternal(getContext(), CALIB_FILE_NAME, data);
        } catch(JSONException e) {
            e.printStackTrace();
            return false;
        }
    }
    public void restore() {
        byte[] data = Util.loadInternal(getContext(), CALIB_FILE_NAME);
        if(0==data.length)
            return;
        try {
            JSONObject document = new JSONObject(new String(data, Charset.forName("UTF-8")));
            int version = document.getInt(JSON_VERSION);
            if(1==version) {
                PixelPitch pitches = new PixelPitch();
                pitches.setAngle(document.getDouble(JSON_ANGLE));
                pitches.setPitches((float)document.getDouble(JSON_HPITCH), (float)document.getDouble(JSON_VPITCH));
                if(isLandscape())
                    pitches.rotate();
                renderer.setPitches(pitches);
                int pgType = document.getInt(JSON_PGTYPE);
                ASPixelGeometry pg;
                if(ASPixelGeometry.CUSTOM==pgType) {
                    pg = new ASPixelGeometry(document.getString(JSON_PGNAME));
                    int w = document.getInt(JSON_PGWIDTH);
                    int h = document.getInt(JSON_PGHEIGHT);
                    int[] rawMatrix = new int[w*h];
                    JSONArray matrix = document.getJSONArray(JSON_PGMATRIX);
                    for(int i=0;i<rawMatrix.length;i++)
                        rawMatrix[i] = matrix.getInt(i);
                    pg.setMatrix(w, h, rawMatrix);
                    int count = document.getInt(JSON_PGCOUNT);
                    pg.setPatternCount(count);
                    float[] rawPattern = new float[6*count];
                    JSONArray pattern = document.getJSONArray(JSON_PGPATTERN);
                    for(int i=0;i<rawPattern.length;i++)
                        rawPattern[i] = (float)pattern.getDouble(i);
                    for(int i=0;i<count;i++)
                        pg.setPattern(i, rawPattern, 6*i);
                } else {
                    try {
                        pg = ASPixelGeometry.getPixelGeomtery(pgType);
                    } catch(IllegalArgumentException e) {
                        pg = ASPixelGeometry.getPixelGeomtery(ASPixelGeometry.STRIPES);
                    }
                }
                currentGeometry = pg;
                if(0>geometries.indexOf(pg))
                    geometries.add(pg);
                renderer.setPixelGeometry(pg);
            }
        } catch(JSONException e) {
            e.printStackTrace();
        }
    }

    public void deleteCustom(ASPixelGeometry pg) {
        if(pg.isCustom()&&geometries.remove(pg)) {
            saveCustomPG();
            if(pg.equals(currentGeometry))
                currentGeometry = geometries.get(0);
        }
    }
    public boolean addPattern(File file) throws ParserException {
        byte[] data = Util.loadExternal(file);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Node root = null;
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(data));
            for(int i=0;i<doc.getChildNodes().getLength();i++) {
                Node node = doc.getChildNodes().item(i);
                if("PixelGeometryDescriptor".equalsIgnoreCase(node.getNodeName())) {
                    root = node;
                    break;
                }
            }
        } catch(SAXException e) {
            throw new ParserException(e);
        } catch(ParserConfigurationException | IOException e) {
            e.printStackTrace();
        }
        if(null==root)
            throw new ParserException("PixelGeometryDescriptor not found");
        Node nameNode = root.getAttributes().getNamedItem("name");
        if(null==nameNode)
            throw new ParserException("PixelGeometryDescriptor missing name attribute");
        Node verNode = root.getAttributes().getNamedItem("version");
        if(null==verNode)
            throw new ParserException("PixelGeometryDescriptor missing version attribute");
        int version;
        try {
            version = Integer.parseInt(verNode.getTextContent());
        } catch(NumberFormatException e) {
            throw new ParserException("PixelGeometryDescriptor version "+verNode.getTextContent()+" not a valid number");
        }

        NodeList rows = null;
        NodeList patterns = null;
        for(int i=0;i<root.getChildNodes().getLength();i++) {
            Node node = root.getChildNodes().item(i);
            if("patterns".equalsIgnoreCase(node.getNodeName()))
                patterns = node.getChildNodes();
            else if("matrix".equalsIgnoreCase(node.getNodeName()))
                rows = node.getChildNodes();
        }
        ASPixelGeometry pg = new ASPixelGeometry(nameNode.getTextContent());
        extractMatrix(rows, pg);
        extractPatterns(patterns, pg);
        pg.normalize();
        if(geometries.contains(pg))
            return false;
        else {
            geometries.add(pg);
            saveCustomPG();
            return true;
        }
    }
    private void loadCustonPG() {
        byte[] data = Util.loadInternal(getContext(), PG_FILE_NAME);
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        while(ASPixelGeometry.CUSTOM==Util.nextInt(bis, 0))
            geometries.add(ASPixelGeometry.load(bis));
    }
    private void saveCustomPG() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            for(ASPixelGeometry pg: geometries)
                if(pg.isCustom()) {
                    bos.write(Util.toBytes(pg.getType()));
                    pg.save(bos);
                }
            Util.saveInternal(getContext(), PG_FILE_NAME, bos.toByteArray());
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    private void extractMatrix(NodeList rowList, ASPixelGeometry pg) throws ParserException {
        if(null==rowList)
            return;
        int offset = 0;
        int cols = -1;
        int rows = 0;
        int[] matrix = null;
        for(int i=0;i<rowList.getLength();i++)
            if("row".equalsIgnoreCase(rowList.item(i).getNodeName()))
                rows++;
        for(int i=0;i<rowList.getLength();i++) {
            Node row = rowList.item(i);
            if("row".equalsIgnoreCase(row.getNodeName())) {
                String[] values = row.getTextContent().trim().split("\\s+");
                if(null==matrix) {
                    cols = values.length;
                    matrix = new int[cols*rows];
                } else if(cols!=values.length)
                    throw new ParserException("Matrix row "+(i+1)+" contains "+values.length+" values, expected "+cols);
                for(String v : values)
                    try {
                        matrix[offset++] = Integer.parseInt(v);
                    } catch(NumberFormatException e) {
                        throw new ParserException("Matrix row "+(i+1)+", value "+v+" is not a valid number");
                    }
            }
        }
        pg.setMatrix(cols, rows, matrix);
    }
    private void extractPatterns(NodeList patterns, ASPixelGeometry pg) throws ParserException {
        if(null==patterns)
            throw new ParserException("No Patterns node found under PixelGeometryDescriptor");
        if(0==patterns.getLength())
            throw new ParserException("Patterns node cannot be empty");

        Set<Integer> ids = new HashSet<>();
        for(int i=0;i<patterns.getLength();i++) {
            Node pattern = patterns.item(i);
            if("pixel".equalsIgnoreCase(pattern.getNodeName())) {
                Node idNode = pattern.getAttributes().getNamedItem("id");
                if(null==idNode)
                    throw new ParserException("Pixel missing id attribute");
                String value = idNode.getTextContent();
                try {
                    if(!ids.add(Integer.parseInt(value)))
                        throw new ParserException("Multiple Pixels with id "+value);
                } catch(NumberFormatException e) {
                    throw new ParserException("Pixel id "+value+", is not a valid number");
                }
            }
        }
        int max = 0;
        for(int i: ids)
            max = Math.max(i, max);
        pg.setPatternCount(max+1);
        for(int i=0;i<patterns.getLength();i++) {
            Node pattern = patterns.item(i);
            if("pixel".equalsIgnoreCase(pattern.getNodeName())) {
                Node[] rgbNodes = new Node[COMPONENTS.length];
                for(int j=0;j<pattern.getChildNodes().getLength();j++) {
                    Node comp = pattern.getChildNodes().item(j);
                    if("component".equalsIgnoreCase(comp.getNodeName())) {
                        Node typeNode = comp.getAttributes().getNamedItem("type");
                        if(null==typeNode)
                            throw new ParserException("Component missing type attribute");
                        String type = comp.getAttributes().getNamedItem("type").getTextContent();
                        for(int k=0;k<COMPONENTS.length;k++)
                            if(COMPONENTS[k].equalsIgnoreCase(type)) {
                                rgbNodes[k] = comp;
                                break;
                            }
                    }
                }
                float[] rgb = new float[6];
                int id = Integer.parseInt(pattern.getAttributes().getNamedItem("id").getTextContent());
                for(int j=0;j<COMPONENTS.length;j++) {
                    if(null==rgbNodes[j])
                        throw new ParserException("Pixel "+id+" missing "+COMPONENTS[j]+" component");
                    String values[] = rgbNodes[j].getTextContent().trim().split("\\s+");
                    if(2!=values.length)
                        throw new ParserException("Pixel "+id+" component "+COMPONENTS[j]+" has "+values.length+" coordinates, expected 2");
                    for(int k=0;k<values.length;k++)
                        try {
                            rgb[j*2+k] = Float.parseFloat(values[k]);
                        } catch(NumberFormatException e) {
                            throw new ParserException("Pixel "+id+" component "+COMPONENTS[j]+", "+values[k]+" not a valid value");
                        }

                }
                pg.setPattern(id, rgb);
            }
        }
    }

    public boolean saveExternal(String path) {
        byte[] data = saveExternal(getContext(), getHorizontalPitch(), getVerticalPitch(), getPixelGeometry());
        if(null!=data&&Util.saveExternal(path, false, data)) {
            MediaScannerConnection.scanFile(getContext(), new String[]{path}, null, null);
            return true;
        } else
            return false;
    }

    @Override
    void setDisplay(@NonNull DisplayParameters params) {}//display not set like this in calibration

    public void setMode(CalibMode mode) {renderer.setMode(mode);}
    public void setBitmap(Bitmap bmp) {renderer.setBitmap(bmp);}

    public void setPitch(float pitch) {renderer.setPitch(pitch);}
    public void setAngle(float deg) {renderer.setAngle(deg);}
    public void setHorizontalPitch(float pitch) {renderer.setHorizontalPitch(pitch);}
    public void setVerticalPitch(float pitch) {renderer.setVerticalPitch(pitch);}
    public void setEyePosCoef(float eyeCoef) {renderer.setEyePosCoef(eyeCoef);}

    public void setPixelGeometry(ASPixelGeometry pg) {
        if(-1<geometries.indexOf(currentGeometry)) {
            currentGeometry = pg;
            renderer.setPixelGeometry(currentGeometry);
        }
    }
    public ASPixelGeometry nextPixelGeometry() {return nextPixelGeometry(currentGeometry);}
    public ASPixelGeometry nextPixelGeometry(ASPixelGeometry pg) {
        int index = geometries.indexOf(pg);
        return 0>index?pg:geometries.get((index+1)%geometries.size());
    }

    public float getPitch() {return renderer.getPitch();}
    public float getAngle() {return renderer.getAngle();}
    public float getHorizontalPitch() {return renderer.getHorizontalPitch();}
    public float getVerticalPitch() {return renderer.getVerticalPitch();}
    public float getEyePosCoef() {return renderer.getEyePosCoef();}
    public ASPixelGeometry getPixelGeometry() {return currentGeometry;}
    public List<ASPixelGeometry> getGeometries() {return new ArrayList<>(geometries);}
    public int getPixelGeometryCount() {return geometries.size();}
}
