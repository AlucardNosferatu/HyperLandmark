package com.tdim.qCalibration;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.tdim.qas.ASPixelGeometry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ManagmentDialog extends DialogFragment {
    private static final int ICON_SIZE = 128;
    private static final int PIX_COUNT = 4;
    interface PGListener {
        void select(ASPixelGeometry pg);
        void delete(ASPixelGeometry pg);
    }

    private List<ASPixelGeometry> geometries = new ArrayList<>();
    private List<View> pgViews = new ArrayList<>();
    private ViewGroup scroll;
    private DialogInterface.OnClickListener listener;
    private PGListener pgListener;
    private int selected = -1;

    public void setGeometries(@NonNull List<ASPixelGeometry> list) {
        if(null==scroll)
            geometries = list;
    }
    public void setScanListener(DialogInterface.OnClickListener listener) {this.listener = listener;}
    public void setPGListener(PGListener listener) {pgListener = listener;}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_pgmgmt, null);
        scroll = view.findViewById(R.id.layoutScroll);
        for(final ASPixelGeometry pg: geometries)
            addItem(pg);
        if(-1<selected&&geometries.size()>selected)
            setSelected(geometries.get(selected));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view).setTitle(R.string.title_pgmanagement);
        builder.setNeutralButton("+", listener);
        builder.setPositiveButton(R.string.button_close, null);
        return builder.create();
    }

    public void setSelected(ASPixelGeometry pg) {
        if(-1<selected&&pgViews.size()>selected)
            pgViews.get(selected).setBackgroundColor(getResources().getColor(R.color.colorListNormal));
        selected = geometries.indexOf(pg);
        if(-1<selected&&pgViews.size()>selected) {
            pgViews.get(selected).setBackgroundColor(getResources().getColor(R.color.colorListSelected));
            if(null!=pgListener)
                pgListener.select(pg);
        }
    }
    private void deletePG(ASPixelGeometry pg) {
        int index = geometries.indexOf(pg);
        if(-1<index&&pgViews.size()>index) {
            pgViews.get(index).setVisibility(View.GONE);
            if(selected==index)
                for(int i=0;i<pgViews.size();i++) {
                    int j = (i+index)%pgViews.size();
                    if(View.VISIBLE==pgViews.get(j).getVisibility()) {
                        setSelected(geometries.get(j));
                        break;
                    }
                }
            if(null!=pgListener)
                pgListener.delete(pg);
        }
    }

    private void addItem(final ASPixelGeometry pg) {
        final View item = View.inflate(scroll.getContext(), R.layout.item_pgmgmt, null);
        ImageView img = item.findViewById(R.id.imagePattern);
        TextView txt = item.findViewById(R.id.textPattern);
        img.setImageBitmap(drawPattern(pg));
        txt.setText(pg.getName());
        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {setSelected(pg);}
        });
        if(pg.isCustom())
            item.findViewById(R.id.imagePGDel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                    TextView text = new TextView(v.getContext(), null);
                    text.setText(getString(R.string.prompt_delete_pattern).replace("%1", pg.getName()));
                    builder.setTitle(R.string.title_delete);
                    builder.setView(text);
                    builder.setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {deletePG(pg);}
                    });
                    builder.setNegativeButton(R.string.button_no, null);
                    builder.create().show();
                }
            });
        else
            item.findViewById(R.id.imagePGDel).setVisibility(View.GONE);
        pgViews.add(item);
        scroll.addView(item);
    }
    private Bitmap getIcon(int type) {
        switch(type) {
            default: return null;
            case ASPixelGeometry.NONE:
                return BitmapFactory.decodeResource(getResources(), R.drawable.icon_fullpixel);
            case ASPixelGeometry.STRIPES:
                return BitmapFactory.decodeResource(getResources(), R.drawable.icon_stripes);
        }
    }
    private Bitmap drawPattern(ASPixelGeometry pg) {
        Bitmap bmp = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888);
        Paint painter = new Paint();
        painter.setStyle(Paint.Style.FILL);
        Canvas canvas = new Canvas(bmp);
        canvas.drawARGB(255, 0, 0, 0);
        float pixsize = ICON_SIZE/(float)PIX_COUNT;
        Bitmap icon = getIcon(pg.getType());
        if(null==icon) {
            float radius = 0.16f*pixsize;
            Set<PointF> red = new HashSet<>();
            Set<PointF> green = new HashSet<>();
            Set<PointF> blue = new HashSet<>();
            int[] matrix = pg.getMatrix();
            for(int y=0;y<=PIX_COUNT;y++) {
                int[] row = new int[pg.getWidth()];
                System.arraycopy(matrix, pg.getWidth()*(y%pg.getHeight()), row, 0, row.length);
                for(int x=0;x<=PIX_COUNT;x++) {
                    ASPixelGeometry.Pixel pix = pg.getPattern(row[x%row.length]);
                    red.add(new PointF((x+pix.red.x)*pixsize, (y+pix.red.y)*pixsize));
                    green.add(new PointF((x+pix.green.x)*pixsize, (y+pix.green.y)*pixsize));
                    blue.add(new PointF((x+pix.blue.x)*pixsize, (y+pix.blue.y)*pixsize));
                }
            }
            painter.setColor(0xFF0000FF);
            for(PointF c : blue)
                canvas.drawCircle(c.x, c.y, radius, painter);
            painter.setColor(0xFFFF0000);
            for(PointF c : red)
                canvas.drawCircle(c.x, c.y, radius, painter);
            painter.setColor(0xFF00FF00);
            for(PointF c : green)
                canvas.drawCircle(c.x, c.y, radius, painter);
        } else {
            Rect src = new Rect(0, 0, icon.getWidth(), icon.getHeight());
            for(int y=0;y<=PIX_COUNT;y++)
                for(int x=0;x<=PIX_COUNT;x++) {
                    float xoff = x*pixsize;
                    float yoff = y*pixsize;
                    canvas.drawBitmap(icon, src, new RectF(xoff, yoff, xoff+pixsize, yoff+pixsize), painter);
                }
        }
        return bmp;
    }
}
