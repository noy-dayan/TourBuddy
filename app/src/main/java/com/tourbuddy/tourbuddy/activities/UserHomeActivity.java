package com.tourbuddy.tourbuddy.activities;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.databinding.ActivityUserHomeBinding;
import com.tourbuddy.tourbuddy.fragments.ChatFragment;
import com.tourbuddy.tourbuddy.fragments.ChatListFragment;
import com.tourbuddy.tourbuddy.fragments.ThisProfileFragment;
import com.tourbuddy.tourbuddy.fragments.SearchFragment;
import com.tourbuddy.tourbuddy.fragments.SettingsFragment;
import com.tourbuddy.tourbuddy.utils.AppUtils;
import com.tourbuddy.tourbuddy.utils.DataCache;

import java.util.Locale;

/**
 * Activity for the user's home screen, managing fragment transactions and bottom navigation.
 */
public class UserHomeActivity extends AppCompatActivity {
    //Data cache instance for caching data
    DataCache dataCache;

    //Binding instance for the activity layout
    ActivityUserHomeBinding binding;

    // Firebase authentication instance
    FirebaseAuth mAuth;

    // Firebase user instance
    FirebaseUser mUser;

    // Firestore database instance
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        dataCache = DataCache.getInstance();
        dataCache.clearCache();

        // Load the language from Firestore and set the app locale
        loadLanguageData();
    }

    /**
     * Replaces the current fragment with the selected fragment.
     *
     * @param selectedFragment The fragment to be displayed.
     */
    private void replaceFragment(Fragment selectedFragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, selectedFragment)
                .commit();
    }

    /**
     * Loads language data from Firestore and sets the app locale accordingly.
     */
    private void loadLanguageData() {
        mUser = mAuth.getCurrentUser();
        if (mUser != null) {
            String userId = mUser.getUid();

            // Reference to the Firestore document
            DocumentReference userDocumentRef = db.collection("users").document(userId);

            // Retrieve data from Firestore
            userDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        if (documentSnapshot.contains("language"))
                            AppUtils.setAppLocale(UserHomeActivity.this, documentSnapshot.getString("language"));
                        fragmentManager();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DATABASE", "Error getting document", e);
                }
            });
        }
    }

    /**
     * Manages the fragment transactions and bottom navigation.
     */
    private void fragmentManager() {
        // Inflate the layout after setting the locale
        binding = ActivityUserHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set the default fragment when the activity is created
        replaceFragment(new ThisProfileFragment());

        // Set the listener for BottomNavigationView
        BottomNavigationView bottomNavigationView = binding.bottomNavigationView;
        // Set the default selected item to be "Profile"
        bottomNavigationView.setSelectedItemId(R.id.profile);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            // Check if the selected item is already the one corresponding to the current fragment
            if (item.getItemId() == R.id.search && !(getSupportFragmentManager().findFragmentById(R.id.frameLayout) instanceof SearchFragment))
                replaceFragment(new SearchFragment());
            else if (item.getItemId() == R.id.chat && !(getSupportFragmentManager().findFragmentById(R.id.frameLayout) instanceof ChatListFragment))
                replaceFragment(new ChatListFragment());
            else if (item.getItemId() == R.id.profile && !(getSupportFragmentManager().findFragmentById(R.id.frameLayout) instanceof ThisProfileFragment))
                replaceFragment(new ThisProfileFragment());
            else if (item.getItemId() == R.id.settings && !(getSupportFragmentManager().findFragmentById(R.id.frameLayout) instanceof SettingsFragment))
                replaceFragment(new SettingsFragment());

            return true;
        });
    }
}
