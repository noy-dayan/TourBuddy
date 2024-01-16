package com.tourbuddy.tourbuddy.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tourbuddy.tourbuddy.R;

public class LanguageSpinnerAdapter extends ArrayAdapter<Integer> {
    private String[] languageNames; // Array of language names

    public LanguageSpinnerAdapter(Context context, int resource, Integer[] objects, String[] languageNames) {
        super(context, resource, objects);
        this.languageNames = languageNames;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomDropdownView(position, convertView, parent);
    }

    private View getCustomView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.spinner_language_layout, parent, false);
        }

        ImageView languageIcon = convertView.findViewById(R.id.languageIcon);

        Integer currentImageResource = getItem(position);

        if (currentImageResource != null) {
            languageIcon.setImageResource(currentImageResource);
        }

        return convertView;
    }

    private View getCustomDropdownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.spinner_language_dropdown_layout, parent, false);
        }

        ImageView languageIcon = convertView.findViewById(R.id.languageIcon);
        TextView languageName = convertView.findViewById(R.id.languageName);

        Integer currentImageResource = getItem(position);

        if (currentImageResource != null) {
            languageIcon.setImageResource(currentImageResource);
            languageName.setText(languageNames[position]); // Set language name
        }

        return convertView;
    }
}
