package com.tourbuddy.tourbuddy;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;


import androidx.fragment.app.Fragment;

public class EditProfileFragment extends Fragment {


    ImageView btnBack;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        btnBack = view.findViewById(R.id.btnBack);



        // Set a click listener for the "Edit Profile" button
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to the EditProfileFragment when the button is clicked
                switchFragment(new SettingsFragment());
            }
        });

        return view;
    }

    private void switchFragment(Fragment fragment) {
        if(isAdded())
            // Replace the current fragment with the new one
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .commit();
    }
}