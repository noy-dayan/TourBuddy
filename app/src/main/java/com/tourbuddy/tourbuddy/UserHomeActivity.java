package com.tourbuddy.tourbuddy;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tourbuddy.tourbuddy.databinding.ActivityUserHomeBinding;

public class UserHomeActivity extends AppCompatActivity {
    private ActivityUserHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set the default fragment when the activity is created
        replaceFragment(new ProfileFragment());

        // Set the listener for BottomNavigationView
        BottomNavigationView bottomNavigationView = binding.bottomNavigationView;
        // Set the default selected item to be "Profile"
        bottomNavigationView.setSelectedItemId(R.id.profile);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {

            if (item.getItemId() == R.id.search)
                replaceFragment(new SearchFragment());
            else if (item.getItemId() == R.id.chat)
                replaceFragment(new ChatFragment());
            else if (item.getItemId() == R.id.profile)
                replaceFragment(new ProfileFragment());
            else if (item.getItemId() == R.id.settings)
                replaceFragment(new SettingsFragment());

            return true;
        });
    }

    private void replaceFragment(Fragment selectedFragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, selectedFragment)
                .commit();
    }
}
