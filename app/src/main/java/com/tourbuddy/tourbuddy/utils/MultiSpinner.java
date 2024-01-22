package com.tourbuddy.tourbuddy.utils;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Layout;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.tourbuddy.tourbuddy.R;

import java.util.Arrays;
import java.util.List;

public class MultiSpinner extends androidx.appcompat.widget.AppCompatSpinner implements
        DialogInterface.OnMultiChoiceClickListener, DialogInterface.OnCancelListener {

    private List<String> items;
    private boolean[] selected;
    private String defaultText;
    private MultiSpinnerListener listener;

    public MultiSpinner(Context context) {
        super(context);
    }

    public MultiSpinner(Context arg0, AttributeSet arg1) {
        super(arg0, arg1);
    }

    public MultiSpinner(Context arg0, AttributeSet arg1, int arg2) {
        super(arg0, arg1, arg2);
    }

    @Override
    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        selected[which] = isChecked;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // refresh text on spinner
        updateSpinner();
    }

    @Override
    public boolean performClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setMultiChoiceItems(items.toArray(new CharSequence[items.size()]), selected, this);

        builder.setPositiveButton(android.R.string.ok,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

        builder.setNegativeButton(R.string.clearAll,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Clear all selections
                    Arrays.fill(selected, false);
                    // Update the spinner without closing the dialog
                    updateSpinner();
                }
            });

        builder.setOnCancelListener(this);
        builder.show();
        return true;
    }

    private void updateSpinner() {
        StringBuffer spinnerBuffer = new StringBuffer();
        boolean someSelected = false;
        for (int i = 0; i < items.size(); i++)
            if (selected[i]) {
                if(someSelected)
                    spinnerBuffer.append(", \n");
                spinnerBuffer.append(items.get(i));

                someSelected = true;
            }

        String spinnerText;
        ArrayAdapter<String> adapter;
        if (someSelected) {
            spinnerText = spinnerBuffer.toString();
            adapter = new ArrayAdapter<>(getContext(), R.layout.spinner_custom_layout, new String[]{spinnerText});
        }
        else {
            spinnerText = defaultText;
             adapter = new ArrayAdapter<>(getContext(), R.layout.spinner_custom_centered_layout, new String[]{spinnerText});
        }
        setAdapter(adapter);

        listener.onItemsSelected(selected);
    }

    public List<String> getItems() {
        return items;
    }
    public void setItems(List<String> items, String allText,
                         MultiSpinnerListener listener) {
        this.items = items;
        this.defaultText = allText;
        this.listener = listener;

        // all not selected by default
        selected = new boolean[items.size()];
        Arrays.fill(selected, false);


        // all text on the spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                R.layout.spinner_custom_centered_layout, new String[]{defaultText});


        setAdapter(adapter);
    }

    public interface MultiSpinnerListener {
        void onItemsSelected(boolean[] selected);
    }
}
