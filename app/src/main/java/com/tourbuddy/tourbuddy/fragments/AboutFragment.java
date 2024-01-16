package com.tourbuddy.tourbuddy.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.fragment.app.Fragment;
import com.tourbuddy.tourbuddy.R;

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
                switchFragment(new SettingsFragment());
            }
        });


        return view;
    }

    /**
     * Switch fragment function.
     */
    private void switchFragment(Fragment fragment) {
        if (isAdded())
            // Replace the current fragment with the new one
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .commit();
    }

}