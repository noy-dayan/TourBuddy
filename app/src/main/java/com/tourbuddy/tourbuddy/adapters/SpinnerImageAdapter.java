package com.tourbuddy.tourbuddy.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tourbuddy.tourbuddy.R;

public class SpinnerImageAdapter extends ArrayAdapter<Integer> {

    public SpinnerImageAdapter(Context context, int resource, Integer[] objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
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
}
