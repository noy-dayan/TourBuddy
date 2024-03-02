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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MultiSpinner is a custom spinner allowing multiple selection of items.
 */
public class MultiSpinner extends androidx.appcompat.widget.AppCompatSpinner implements
        DialogInterface.OnMultiChoiceClickListener, DialogInterface.OnCancelListener {

    List<String> items; // List of items to be displayed in the spinner
    boolean[] selected; // Boolean array to track which items are selected
    String defaultText; // Default text to display when no items are selected
    MultiSpinnerListener listener; // Listener for item selection events

    public MultiSpinner(Context context) {
        super(context);
    }

    public MultiSpinner(Context arg0, AttributeSet arg1) {
        super(arg0, arg1);
    }

    public MultiSpinner(Context arg0, AttributeSet arg1, int arg2) {
        super(arg0, arg1, arg2);
    }

    /**
     * Handles click events on the dialog's checkboxes.
     */
    @Override
    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        selected[which] = isChecked;
    }

    /**
     * Handles cancellation of the dialog.
     */
    @Override
    public void onCancel(DialogInterface dialog) {
        // Refresh text on spinner
        updateSpinner();
    }

    /**
     * Overrides the performClick method to show a custom dialog for multi-selection.
     */
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

    /**
     * Updates the spinner text based on the selected items.
     */
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

        // Notify listener about selected items
        listener.onItemsSelected(selected);
    }

    /**
     * Gets the list of items in the spinner.
     */
    public List<String> getItems() {
        return items;
    }

    /**
     * Sets the items in the spinner along with the default text and listener.
     */
    public void setItems(List<String> items, String allText, MultiSpinnerListener listener) {
        this.items = items;
        this.defaultText = allText;
        this.listener = listener;

        // All items are not selected by default
        selected = new boolean[items.size()];
        Arrays.fill(selected, false);

        // Set default text on the spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                R.layout.spinner_custom_centered_layout, new String[]{defaultText});

        setAdapter(adapter);
    }

    /**
     * Interface for listening to item selection events.
     */
    public interface MultiSpinnerListener {
        void onItemsSelected(boolean[] selected);
    }

    /**
     * Selects the items from ArrayList of substrings.
     */
    public void setSelectedItemsByStartSubstring(ArrayList<String> substrings) {
        Arrays.fill(selected, false); // Clear previously selected items

        for (int i = 0; i < items.size(); i++) {
            for (String substring : substrings) {
                if (items.get(i).startsWith(substring)) {
                    selected[i] = true; // Set the corresponding item as selected
                    break; // Move to the next item in the spinner
                }
            }
        }

        updateSpinner(); // Update the spinner to reflect the changes
    }

}
