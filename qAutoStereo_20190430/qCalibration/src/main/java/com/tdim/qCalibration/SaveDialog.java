package com.tdim.qCalibration;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class SaveDialog extends DialogFragment {
    private String[] keys, values;
    private DialogInterface.OnClickListener positiveListener, neutralListener, negativeListener;

    private EditText editVersion = null;

    public void setKeyValues(String[] keys, String[] values) {
        this.keys = keys;
        this.values = values;
    }

    public void setPositiveListener(DialogInterface.OnClickListener listener) {positiveListener = listener;}
    public void setNeutralListener(DialogInterface.OnClickListener listener) {neutralListener = listener;}
    public void setNegativeListener(DialogInterface.OnClickListener listener) {negativeListener = listener;}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if(null==keys||null==values||keys.length>values.length)
            return super.onCreateDialog(savedInstanceState);
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_save, null);
        TableLayout table = view.findViewById(R.id.tableParams);
        editVersion = (EditText) view.findViewById(R.id.editVersion);

        for(int i=0;i<keys.length;i++) {
            TableRow row = new TableRow(getActivity());
            TextView key = new TextView(getActivity());
            key.setText(keys[i]);
            row.addView(key);
            TextView value = new TextView(getActivity());
            value.setText(values[i]);
            row.addView(value);
            table.addView(row);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view).setTitle(R.string.title_save);
        builder.setPositiveButton(R.string.button_sharesave, positiveListener);
        builder.setNeutralButton(R.string.button_save, neutralListener);
        builder.setNegativeButton(R.string.button_cancel, negativeListener);
        return builder.create();
    }

    public String getEditVersion(){
        String editVersionString = "";
        if(this.editVersion != null || this.editVersion.getText() != null){
            String temp = editVersion.getText().toString();
            StringBuilder sb = new StringBuilder(temp.length());
            for(char c:temp.toLowerCase().toCharArray())
                sb.append('-'==c||Character.isLetterOrDigit(c)?c:'_');
            editVersionString = "_" + sb.toString();
        }
        return editVersionString;
    }
}
