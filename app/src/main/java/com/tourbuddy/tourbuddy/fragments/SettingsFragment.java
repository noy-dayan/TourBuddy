package com.tourbuddy.tourbuddy.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.activities.MainActivity;
import com.tourbuddy.tourbuddy.adapters.LanguageSpinnerAdapter;
import com.tourbuddy.tourbuddy.utils.AppUtils;
import com.tourbuddy.tourbuddy.utils.DataCache;

import org.checkerframework.checker.units.qual.A;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment for user settings.
 */
public class SettingsFragment extends Fragment {

    // DataCache instance for caching user data
    DataCache dataCache;

    // Firebase
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore db;
    StorageReference storageReference;

    // UI elements
    ImageView profilePic;
    TextView username;
    Button btnEditProfile;
    Spinner languageSpinner;
    View btnAbout, btnLogout;;

    // Language variables
    String language;
    String[] languageCodes = {"en", "he"}; /* Add more language codes as needed */

    // Loading overlay
    View loadingOverlay;
    ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize DataCache
        dataCache = DataCache.getInstance();

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Initialize UI elements
        profilePic = view.findViewById(R.id.profilePic);
        username = view.findViewById(R.id.username);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnAbout = view.findViewById(R.id.btnAbout);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Initialize loading overlay
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        progressBar = view.findViewById(R.id.progressBar);

        // Attempt to load data from cache
        if (!loadDataFromCache(view)) {
            showLoading(true);
            // Data not found in cache, fetch from the server
            loadDataFromFirebase(view);
        }

        // Set click listener for the "Edit Profile" button
        btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to the EditProfileFragment when the button is clicked
                AppUtils.switchFragment(SettingsFragment.this, new EditProfileFragment());

            }
        });

        // Set click listener for the "About" button
        btnAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppUtils.switchFragment(SettingsFragment.this, new AboutFragment());
            }
        });

        // Set click listener for the "Logout" button
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call the logout method when the button is clicked
                showLogoutDialog();
            }
        });

        return view;
    }

    /**
     * Logout the user from the system and navigate to the login activity.
     */
    private void logout() {
        // Use Firebase Auth to sign out the user
        mAuth.signOut();

        // Navigate to the login activity
        AppUtils.switchActivity(requireActivity(), MainActivity.class, "ltr");
        requireActivity().finish(); // Finish the current activity
    }

    /**
     * Open "Confirm Logout" dialog.
     */
    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(requireContext().getResources().getString(R.string.confirmLogout));
        builder.setMessage(requireContext().getResources().getString(R.string.confirmLogoutMessage));
        builder.setPositiveButton(requireContext().getResources().getString(R.string.logout), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                logout();
            }
        });

        builder.setNegativeButton(requireContext().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing, close the dialog
            }
        });

        builder.show();
    }


    /**
     * Load user data from Firebase Firestore.
     */
    private void loadDataFromFirebase(View view) {
        mUser = mAuth.getCurrentUser();
        if (mUser != null) {
            String userId = mUser.getUid();

            // Reference to the Firestore document
            DocumentReference userDocumentRef = db.collection("users").document(userId);

            // Retrieve data from Firestore
            userDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (isAdded() && documentSnapshot.exists()) {
                        // Check if the profilePic field exists in the Firestore document
                        if (documentSnapshot.contains("username"))
                            username.setText(documentSnapshot.getString("username"));
                        if (documentSnapshot.contains("language")) {
                            language = documentSnapshot.getString("language");

                            // Populate the spinner after loading the language
                            setupLanguageSpinner(view);
                            // Load the image into the ImageView using Glide
                            storageReference.child("images/" + mUser.getUid() + "/profilePic").getDownloadUrl()
                                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            // Got the download URL for 'users/me/profile.png'
                                            Glide.with(view)
                                                    .load(uri)
                                                    .apply(RequestOptions.circleCropTransform())
                                                    .into(profilePic);
                                            showLoading(false);
                                            saveDataToCache(uri); // Save profile picture URL);
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            // Handle failure to load image
                                            showLoading(false);
                                            saveDataToCache(null); // Save profile picture URL);
                                        }
                                    });

                        }
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
     * Load user data from the cache.
     */
    private boolean loadDataFromCache(View view) {
        showLoading(true);
        // Check if data exists in memory cache
        if (dataCache.get("username") != null && dataCache.get("language") != null) {

            // Load data from memory cache
            username.setText((String) dataCache.get("username"));
            language = ((String) dataCache.get("language"));

            if (dataCache.get("profilePicUrl") != null) {
                // Load profile picture from memory cache (if available)
                String profilePicUrl = (String) dataCache.get("profilePicUrl");
                if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                    Glide.with(view)
                            .load(Uri.parse(profilePicUrl))
                            .apply(RequestOptions.circleCropTransform())
                            .into(profilePic);
                    showLoading(false);

                }
            }

            setupLanguageSpinner(view);
            showLoading(false);
            return true; // Data loaded from memory cache
        }
        showLoading(false);
        return false; // Data not found in memory cache
    }

    /**
     * Save user data to the cache.
     */
    private void saveDataToCache(Uri profilePicUri) {
        if (isAdded()) {
            // Save data to memory cache
            dataCache.put("username", username.getText().toString());
            dataCache.put("language", language);
            if (profilePicUri != null)
                dataCache.put("profilePicUrl", profilePicUri.toString());
        }
    }

    /**
     * Set up the language spinner.
     */
    private void setupLanguageSpinner(View view) {
        // Populate the spinner with image resources
        languageSpinner = view.findViewById(R.id.languageSpinner);
        Integer[] languageImages = {
                R.drawable.english_icon,
                R.drawable.hebrew_icon,
                // Add more image resources as needed
        };

        String[] languageArray = getResources().getStringArray(R.array.languages);
        LanguageSpinnerAdapter languageSpinnerAdapter = new LanguageSpinnerAdapter(requireContext(), R.layout.spinner_language_layout, languageImages, languageArray);
        languageSpinnerAdapter.setDropDownViewResource(R.layout.spinner_language_dropdown_layout);
        languageSpinner.setAdapter(languageSpinnerAdapter);

        // Find the index of the loaded language code in the languageCodes array
        int initialSelectionPosition = Arrays.asList(languageCodes).indexOf(language);

        // Set the initial selection for the spinner
        languageSpinner.setSelection(initialSelectionPosition);

        // Set a listener to handle spinner item selection
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // Get the selected language code (assuming languageCodes array corresponds to the images)
                String selectedLanguageCode = languageCodes[position];

                // Set the app language based on the selected item
                AppUtils.setAppLocale(requireContext(), selectedLanguageCode);
                updateLanguageToFirebase(selectedLanguageCode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });
    }

    /**
     * Update user language in Firebase Firestore.
     */
    private void updateLanguageToFirebase(String languageCode) {
        mUser = mAuth.getCurrentUser();
        if (mUser != null) {
            String userId = mUser.getUid();

            Map<String, Object> userData = new HashMap<>();
            userData.put("language", languageCode);

            // Set the document name as the user ID
            DocumentReference userDocumentRef = db.collection("users").document(userId);

            // Retrieve the current language from Firestore
            userDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (isAdded() && documentSnapshot.exists() && documentSnapshot.contains("language")) {
                        String currentLanguage = documentSnapshot.getString("language");

                        // Check if the language has actually changed
                        if (!languageCode.equals(currentLanguage)) {
                            // Update the data in the Firestore document without replacing it
                            userDocumentRef.update(userData).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d("DATABASE", "Language updated for user ID: " + userId);

                                    updateLanguageToCache(languageCode);
                                    dataCache.clearCache();

                                    // Refresh the fragment only if the language has changed
                                    AppUtils.switchFragment(SettingsFragment.this, new SettingsFragment());
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w("DATABASE", "Error updating language", e);
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    /**
     * Update user language in the cache.
     */
    private void updateLanguageToCache(String languageCode) {
        if (isAdded())
            // Save data to memory cache
            dataCache.put("language", languageCode);
    }

    /**
     * Show or hide the loading screen.
     */
    private void showLoading(boolean show) {
        if (isAdded()) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
