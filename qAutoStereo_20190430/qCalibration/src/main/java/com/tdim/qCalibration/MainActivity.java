package com.tdim.qCalibration;

import com.tdim.qas.ASAdjustmentView;
import com.tdim.qas.ASCalibrationView;
import com.tdim.qas.ASCalibrationView.CalibMode;
import com.tdim.qas.ASDetectionListener;
import com.tdim.qas.ASDeviceReversedListener;
import com.tdim.qas.ASPixelGeometry;
import com.tdim.qas.ASSettings;
import com.tdim.qas.FaceDetectorBase;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import static com.tdim.qas.ASAdjustmentView.AdjustMode.Bars;

public class MainActivity extends AppCompatActivity implements View.OnLongClickListener,
        ASDetectionListener, FaceDetectorBase.EyePositionListener, FaceDetectorBase.ViewOffsetListener {
    private static final int ICON_SIZE = 128;
    private static final float MM_PER_IN = 25.4f;
    private SharedPreferences prefs;

    String TAG = "MainActivity";

    private enum Param {
        Angle,
        Pitch,
        HPitch,
        VPitch,
        EyePosCoef;
        public float[] stepSizes() {
            switch(this) {
                case Angle: return new float[]{-0.01f, -0.05f, -0.1f, -1, -5, -10};
                case Pitch: return new float[]{1/4096f, 1/256f, 1/16f, 0.25f, 1};
                case HPitch:
                case VPitch:
                case EyePosCoef:
                    return new float[]{1/16384f, 1/4096f, 1/1024f, 1/256f, 1/64f, 1/16f};
                default: throw new IllegalArgumentException();
            }
        }
    }
    private static int orientation = 1;
    private static boolean showUI = true;
    private static CalibMode mode = CalibMode.Angle;
    private static Param param = Param.Angle;
    private static int[] stepIndices = new int[]{3, 4, 2, 2, 2};
    private static int[] pictures = new int[]{R.drawable.plane_sbs, R.drawable.dog1_sbs, R.drawable.dog2_sbs, R.drawable.bridge_sbs};
    private static int pictIndex = 0;

    private ASCalibrationView calib;
    private Button buttonRender, buttonParam, buttonSwitch, buttonIncParam, buttonDecParam;
    private ImageButton buttonPixGeom;
    private TextView textStep, textValue, textFace, textDistanceInfo;
    private LinearLayout layoutDistanceInfo;
    private float step, value;

    //private FaceDetectorBase detect;
    private ASAdjustmentView adjust;

    static File getExternalStorageDirectory() {
        return new File(Environment.getExternalStorageDirectory().getPath()+File.separator+"qCalibration");
    }
    private static int rotationOrientation() {
        switch(orientation) {
            case 0: return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            case 1: return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            case 2: return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            case 3: return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(rotationOrientation());
        setContentView(R.layout.activity_main);
        calib = findViewById(R.id.calib);
        buttonRender = findViewById(R.id.buttonRender);
        buttonParam = findViewById(R.id.buttonParam);
        buttonSwitch = findViewById(R.id.buttonSwitch);
        buttonPixGeom = findViewById(R.id.buttonPixgeom);
        textStep = findViewById(R.id.paramStep);
        textValue = findViewById(R.id.paramValue);
        layoutDistanceInfo = findViewById(R.id.layoutDistanceInfo);
        textFace = findViewById(R.id.textFace);
        textDistanceInfo = findViewById(R.id.textDistanceData);
        buttonIncParam = findViewById(R.id.buttonAdd);
        buttonDecParam = findViewById(R.id.buttonSub);

        buttonPixGeom.setOnLongClickListener(this);
        updateUIVisibility();


        buttonIncParam.setOnTouchListener(new RepeatListener(400, 100, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPressAdd(view);
            }
        }));


        buttonDecParam.setOnTouchListener(new RepeatListener(400, 100, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPressSubtract(view);
            }
        }));

        calib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showUI = !showUI;
                updateUIVisibility();
            }
        });
        calib.setDeviceReversedListener(new ASDeviceReversedListener() {
            @Override
            public void onDeviceReversed() {updatePixGeomIcon();}});
        calib.setBitmap(BitmapFactory.decodeResource(getResources(), pictures[pictIndex]));

        setMode(mode);
        setParam(param);
//        updatePixGeomIcon();

//        if(PackageManager.PERMISSION_DENIED== ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA))
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
//
        ASSettings settings = new ASSettings(getBaseContext());
        settings.setCameraQuality(30);
        if(!settings.isAdjusted()) {

        }
        adjust = findViewById(R.id.adjust);
        //adjust.setBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.basebat));
        adjust.setMode(Bars);
        adjust.useFaceDetection(true);
        adjust.setFaceDetectionListener(this);
        adjust.setOffsetListener(this);
        adjust.setEyeListener(this);

        if(PackageManager.PERMISSION_DENIED==ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA))
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(0<grantResults.length&&PackageManager.PERMISSION_GRANTED==grantResults[0])
            ;
    }

    private float getPixelSize() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        float dpi = (float)Math.hypot(dm.xdpi, dm.ydpi);
        return 0<dpi?MM_PER_IN/dpi:0;
    }

    @Override
    protected void onPause() {
        super.onPause();
        calib.save();
    }

    @Override
    public boolean onLongClick(View v) {
        if(R.id.buttonPixgeom==v.getId()) {
            final ManagmentDialog dialog = new ManagmentDialog();
            dialog.setGeometries(calib.getGeometries());
            dialog.setSelected(calib.getPixelGeometry());
            dialog.setScanListener(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {scanPG();}
            });
            dialog.setPGListener(new ManagmentDialog.PGListener() {
                @Override
                public void select(ASPixelGeometry pg) {setPixelGeometry(pg);}
                @Override
                public void delete(ASPixelGeometry pg) {calib.deleteCustom(pg);}
            });
            dialog.show(getFragmentManager(), "ManagementDialog");
            return true;
        } else
            return false;
    }

    private void scanPG() {
        int count = 0;
        boolean noError = true;
        for(File file: new ASSettings(getBaseContext()).scanRecursive(".pgd", getExternalStorageDirectory()))
            try {
                if(calib.addPattern(file))
                    count++;
            } catch(ASCalibrationView.ParserException e) {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                noError = false;
            }
        if(noError) {
            String message = 0<count?getString(R.string.info_newpatterns).replace("%1", Integer.toString(count)):getString(R.string.info_nonewpatterns);
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUIVisibility() {
        int vis = showUI?View.VISIBLE:View.GONE;
        findViewById(R.id.layoutControl).setVisibility(vis);
        findViewById(R.id.tableControl).setVisibility(vis);
    }

    public void onPressRenderMode(View view) {
        CalibMode[] modes = CalibMode.values();
        CalibMode next = modes[0];
        for(int i=0;i<modes.length;i++)
            if(modes[i]==mode) {
                if(modes.length>i+1)
                    next = modes[i+1];
                break;
            }
        setMode(next);
    }
    private void setMode(ASCalibrationView.CalibMode mode) {
        MainActivity.mode = mode;
        calib.setMode(mode);
        buttonRender.setText(getStringId(mode));
        buttonParam.setEnabled(CalibMode.Picture==mode);
        layoutDistanceInfo.setVisibility(CalibMode.EyePosCoef==mode?View.VISIBLE:View.GONE);
        buttonSwitch.setVisibility((CalibMode.Picture==mode||CalibMode.EyePosCoef==mode)?View.VISIBLE:View.INVISIBLE);
        switch(mode) {
            case Angle:
                setParam(Param.Angle);
                break;
            case Colors:
                setParam(Param.Pitch);
                break;
            case Bars:
                setParam(Param.VPitch);
                break;
            case Rainbow:
                setParam(Param.HPitch);
                break;
            case EyePosCoef:
                setParam(Param.EyePosCoef);
                break;
        }
    }
    private int getStringId(CalibMode mode) {
        switch(mode) {
            case Angle: return R.string.render_angle;
            case Bars: return R.string.render_bars;
            case Colors: return R.string.render_colors;
            case Rainbow: return R.string.render_rainbow;
            case Picture: return R.string.render_picture;
            case EyePosCoef: return R.string.eye_pos_coef;
            default: throw new IllegalArgumentException();
        }
    }

    public void onPressParam(View view) {
        Param[] params = Param.values();
        Param next = params[0];
        for(int i=0;i<params.length;i++)
            if(params[i]==param) {
                if(params.length>i+1)
                    next = params[i+1];
                break;
            }
        setParam(next);
    }
    private void setParam(Param param) {
        MainActivity.param = param;
        buttonParam.setText(getString(getStringId(param)));
        updateStep();
        setValue(getValue());
    }
    private int getStringId(Param param) {
        switch(param) {
            case HPitch: return R.string.param_hpitch;
            case VPitch: return R.string.param_vpitch;
            case Pitch: return R.string.param_pitch;
            case Angle: return R.string.param_angle;
            case EyePosCoef: return R.string.eye_pos_coef;

            default: throw new IllegalArgumentException();
        }
    }

    public void onPressSwitch(View view) {
        pictIndex = pictIndex<pictures.length-1?pictIndex+1:0;
        calib.setBitmap(BitmapFactory.decodeResource(getResources(), pictures[pictIndex]));
    }

    public void onPressOrientation(View view) {
        orientation = (orientation+1)&0x03;//clockwise
        setRequestedOrientation(rotationOrientation());
    }
    public void onPressSave(View view) {
        final SaveDialog dialog = new SaveDialog();
        String[] keys = new String[]{
                "Brand",
                "Model",
                Param.Pitch.toString(),
                Param.Angle.toString(),
                "Pixel Geometry"
        };
        String[] values = new String[]{
                Build.BRAND,
                Build.MODEL,
                Float.toString(calib.getPitch()),
                Float.toString(calib.getAngle()),
                calib.getPixelGeometry().getName()
        };
        dialog.setKeyValues(keys, values);
        final String path = getExternalStorageDirectory()+File.separator+brandModelFileName()+".qasc";
        dialog.setPositiveListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String pathWithVersion = path.replace(".qasc", dialog.getEditVersion()+".qasc" );
                if(saveExternal(pathWithVersion)) {
                    Uri uri = FileProvider.getUriForFile(MainActivity.this, getPackageName()+".provider", new File(pathWithVersion));
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("application/qasc");
                    share.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(share, getString(R.string.action_share)));
                }
            }
        });
        dialog.setNeutralListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInteface, int which) {
                String pathWithVersion = path.replace(".qasc", dialog.getEditVersion()+".qasc" );
                if(saveExternal(pathWithVersion))
                    Toast.makeText(MainActivity.this, getString(R.string.info_savepath).replace("%1", pathWithVersion), Toast.LENGTH_LONG).show();
            }
        });
        dialog.show(getFragmentManager(), "SaveDialog");
    }

    public void onPressImport(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose .qasc file to import:");

        final String path = Environment.getExternalStorageDirectory().toString()+"/qCalibration";
        final String filePath = "";
        File directory = new File(path);
        File[] files = directory.listFiles();
        ArrayList<String> qascFiles = new ArrayList<>();

        for (int i = 0; i < files.length; i++)
        {
            Log.d("Files", "FileName:" + files[i].getName());
            if (files[i].isFile() && files[i].getName().endsWith(".qasc")) {
                qascFiles.add(files[i].getName());
            }
        }
        CharSequence[] cs = qascFiles.toArray(new CharSequence[qascFiles.size()]);
        int checkedItem = 1; // cow
        builder.setSingleChoiceItems(cs, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // user checked an item

            }
        });

// add OK and Cancel buttons
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ListView lw = ((AlertDialog)dialog).getListView();
                Object checkedItem = lw.getAdapter().getItem(lw.getCheckedItemPosition());
                ASSettings settings = new ASSettings(getBaseContext());
                settings.setCameraQuality(30);
                File f = new File(path+"/"+checkedItem.toString());
                Log.i(TAG, "filename: " + f.getName());
                settings.setDisplayFile(f);

                onFileLoaded();

            }
        });
        builder.setNegativeButton("Cancel", null);

// create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void onFileLoaded(){
//        prefs = this.getSharedPreferences("com.tim.qas", Context.MODE_PRIVATE);
        prefs = ASSettings.getPrefs();
        float h = prefs.getFloat(getString(R.string.pref_hor_pitch), 0);
        float v = prefs.getFloat(getString(R.string.pref_ver_pitch), 0);
        int pgType = prefs.getInt(getString(R.string.pref_pixel_geom), 0);
        float angle = calib.getAngle();

        float angleError = prefs.getInt(getString(R.string.pref_angle_error), 0);
        float viewOffset = prefs.getInt(getString(R.string.pref_view_offset), 0);
        Log.i(TAG, "Preferences - h: " + h + " v:"+v+ " pgType:"+ pgType+ " pitch:"+calib.getPitch()+ " angle:"+angle);
        if(pgType != -1) {
            calib.setPixelGeometry(ASPixelGeometry.getPixelGeomtery(pgType));
        } else {
            calib.setPixelGeometry(ASPixelGeometry.getPixelGeomtery(0));
        }
        calib.setHorizontalPitch(h);
        calib.setVerticalPitch(v);
        calib.setPitch(calib.getPitch());
        calib.setAngle(calib.getAngle());
//        calib.setPixelGeometry(ASPixelGeometry.getPixelGeomtery(pgType));
//        calib.setMode(mode);
        textValue.setText(String.format(Locale.getDefault(), "%f", getValue()));
        buttonRender.setText(getStringId(mode));

    }

    private boolean saveExternal(String path) {
        if(calib.saveExternal(path))
            return true;
        else {
            Toast.makeText(MainActivity.this, R.string.error_save, Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    private static String brandModelFileName(){
        StringBuilder sb = new StringBuilder(Build.MODEL.length());
        for(char c: Build.BRAND.toLowerCase().toCharArray())
            sb.append(Character.isLetterOrDigit(c)?c:'_');
        if(sb.length()>0)
            sb.append("_");
        for(char c: Build.MODEL.toLowerCase().toCharArray())
            sb.append(Character.isLetterOrDigit(c)?c:'_');
        return sb.toString();
    }

    public void onPressPixelGeometry(View view) {setPixelGeometry(calib.nextPixelGeometry());}
    private void setPixelGeometry(ASPixelGeometry pg) {
        calib.setPixelGeometry(pg);
        updatePixGeomIcon();
    }
    private void updatePixGeomIcon() {
        final Bitmap bmp = getPixGeomIcon();
        final int rotation = calib.getDeviceRotation();
        Drawable drawable = new BitmapDrawable(getResources(), bmp) {
            @Override
            public void draw(Canvas canvas) {
                canvas.save();
                canvas.rotate(-90*rotation, 0.5f*bmp.getWidth(), 0.5f*bmp.getHeight());
                super.draw(canvas);
                canvas.restore();
            }
        };
        buttonPixGeom.setImageDrawable(drawable);
    }
    private Bitmap getPixGeomIcon() {
        switch(calib.getPixelGeometry().getType()) {
            case ASPixelGeometry.NONE:
                return BitmapFactory.decodeResource(getResources(), R.drawable.icon_fullpixel);
            case ASPixelGeometry.STRIPES:
                return BitmapFactory.decodeResource(getResources(), R.drawable.icon_stripes);
            case ASPixelGeometry.PT_DIAMOND:
                return BitmapFactory.decodeResource(getResources(), R.drawable.icon_ptdiamond);
        }
        //else custom icon
        Bitmap bmp = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888);
        Paint painter = new Paint();
        painter.setStyle(Paint.Style.FILL);
        Canvas canvas = new Canvas(bmp);
        painter.setColor(0xFF000000);
        canvas.drawRect(new Rect(0, 0, bmp.getWidth(), bmp.getHeight()), painter);
        ASPixelGeometry.Pixel pix = calib.getPixelGeometry().getPattern(0);
        float radius = 0.16f*ICON_SIZE;
        painter.setColor(0xFF0000FF);
        canvas.drawCircle((0.5f+pix.blue.x)*bmp.getWidth(), (0.5f+pix.blue.y)*bmp.getHeight(), radius, painter);
        painter.setColor(0xFFFF0000);
        canvas.drawCircle((0.5f+pix.red.x)*bmp.getWidth(), (0.5f+pix.red.y)*bmp.getHeight(), radius, painter);
        painter.setColor(0xFF00FF00);
        canvas.drawCircle((0.5f+pix.green.x)*bmp.getWidth(), (0.5f+pix.green.y)*bmp.getHeight(), radius, painter);
        return bmp;
    }

    public void onPressIncrement(View view) {
        final int max = param.stepSizes().length-1;
        int enumVal = param.ordinal();
        int index = stepIndices[enumVal];
        stepIndices[enumVal] = max>index?index+1:max;
        updateStep();
    }
    public void onPressDecrement(View view) {
        int enumVal = param.ordinal();
        int index = stepIndices[enumVal];
        stepIndices[enumVal] = 0<index?index-1:0;
        updateStep();
    }
    private void updateStep() {
        step = param.stepSizes()[stepIndices[param.ordinal()]];
        textStep.setText(String.format(Locale.getDefault(), "%f", step));
    }

    public void onPressAdd(View view) {setValue(value+step);}
    public void onPressSubtract(View view) {setValue(value-step);}
    private float getValue() {
        switch(param) {
            case HPitch: return calib.getHorizontalPitch();
            case VPitch: return calib.getVerticalPitch();
            case Pitch: return calib.getPitch();
            case Angle: return calib.getAngle();
            case EyePosCoef: return calib.getEyePosCoef();
            default:
                return Float.NaN;
        }
    }
    private void setValue(float value) {
        switch(param) {
            case HPitch:
                calib.setHorizontalPitch(value);
                break;
            case VPitch:
                calib.setVerticalPitch(value);
                break;
            case Pitch:
                calib.setPitch(value);
                break;
            case Angle:
                calib.setAngle(value);
                break;
            case EyePosCoef:
                calib.setEyePosCoef(value);
                break;

            default:
                return;
        }
        calib.requestRender();
        this.value = value;
        textValue.setText(String.format(Locale.getDefault(), "%f", value));
    }

    @Override
    public void onEyePosition(@Nullable PointF left, @Nullable PointF right) {
        StringBuilder sb = new StringBuilder();
        if(left!=null) {
            sb.append("L X: ").append(left.x);
            sb.append("\nL Y: ").append(left.y);
        }else{
            sb.append("left ------");
        }
        if(right!=null) {
            sb.append("\nR X: ").append(right.x);
            sb.append("\nR Y: ").append(right.y);
        }else{
            sb.append("\nright ------");
        }
        textDistanceInfo.setText(sb.toString());
    }

    @Override
    public void onViewOffset(float offset) {
        textFace.setText("offset "+offset);
    }

    @Override
    public void onFaceFound() {
        //textFace.setVisibility(View.VISIBLE);
    }

    @Override
    public void onFaceLost() {
        textFace.setText("");
        //textFace.setVisibility(View.INVISIBLE);
    }


}
