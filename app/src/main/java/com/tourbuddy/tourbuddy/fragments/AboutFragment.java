package com.tourbuddy.tourbuddy.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.fragment.app.Fragment;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.utils.AppUtils;

/**
 * Fragment for learning about the app and it's developers.
 */
public class AboutFragment extends Fragment {
    ImageView btnBack;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        // Initialize UI elements
        btnBack = view.findViewById(R.id.btnBack);


        // Set click listeners for buttons
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppUtils.switchFragment(AboutFragment.this, new SettingsFragment());
            }
        });


        return view;
    }

}