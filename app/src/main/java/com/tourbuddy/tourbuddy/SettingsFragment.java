package com.tourbuddy.tourbuddy;

import android.content.Context;
import android.content.SharedPreferences;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SettingsFragment extends Fragment {

    DataCache dataCache;


    ImageView profilePic;
    TextView username;
    Button btnEditProfile;

    String language;
    String[] languageCodes = {"en", "he"}; /* Add more language codes as needed */
    Spinner languageSpinner;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore db;

    StorageReference storageReference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        showLoading(true);
        dataCache = DataCache.getInstance();

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        profilePic = view.findViewById(R.id.profilePic);
        username = view.findViewById(R.id.username);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        // Attempt to load data from cache
        if (loadDataFromCache(view))
            // Data loaded from cache, no need to fetch from the server
            showLoading(false);
        else
            // Data not found in cache, fetch from the server
            loadDataFromFirebase(view);

        // Set a click listener for the "Edit Profile" button
        btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to the EditProfileFragment when the button is clicked
                switchFragment(new EditProfileFragment());
            }
        });

        return view;
    }

    private void setAppLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration configuration = new Configuration();
        configuration.locale = locale;

        getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
    }

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

    /*
    private boolean loadDataFromCache(View view) {
        // Check if data exists in cache
        if (prefs.contains("username") && prefs.contains("profilePicUrl") && prefs.contains("language")) {

            // Load data from cache
            username.setText(prefs.getString("username", ""));
            language = prefs.getString("language", "");
            // Load profile picture from cache (if available)
            Uri profilePicUrl = Uri.parse(prefs.getString("profilePicUrl", ""));
            if (profilePicUrl != null) {
                Glide.with(view)
                        .load(profilePicUrl)
                        .apply(RequestOptions.circleCropTransform())
                        .into(profilePic);
            }
            setupLanguageSpinner(view);
            return true; // Data loaded from cache
        }

        return false; // Data not found in cache
    }
*/

    private boolean loadDataFromCache(View view) {
        // Check if data exists in memory cache
        if (dataCache.get("username") != null  && dataCache.get("language") != null) {

            // Load data from memory cache
            username.setText((String) dataCache.get("username"));
            language = ((String) dataCache.get("language"));
            if (dataCache.get("profilePicUrl") != null){
                // Load profile picture from memory cache (if available)
                String profilePicUrl = (String) dataCache.get("profilePicUrl");
                if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                    Glide.with(view)
                            .load(Uri.parse(profilePicUrl))
                            .apply(RequestOptions.circleCropTransform())
                            .into(profilePic);
                }
            }

            setupLanguageSpinner(view);
            return true; // Data loaded from memory cache
        }

        return false; // Data not found in memory cache
    }

    private void saveDataToCache(Uri profilePicUri) {
        if (isAdded()) {
            // Save data to memory cache
            dataCache.put("username", username.getText().toString());
            dataCache.put("language", language);
            if(profilePicUri != null)
                dataCache.put("profilePicUrl", profilePicUri.toString());
        }
    }

    private void setupLanguageSpinner(View view) {
        // Populate the spinner with image resources
        languageSpinner = view.findViewById(R.id.languageSpinner);
        Integer[] languageImages = {
                R.drawable.english_icon,
                R.drawable.hebrew_icon,
                // Add more image resources as needed
        };

        SpinnerImageAdapter spinnerImageAdapter = new SpinnerImageAdapter(requireContext(), R.layout.spinner_language_layout, languageImages);
        spinnerImageAdapter.setDropDownViewResource(R.layout.spinner_language_dropdown_layout);
        languageSpinner.setAdapter(spinnerImageAdapter);

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
                setAppLocale(selectedLanguageCode);
                updateLanguageToFirebase(selectedLanguageCode);
                dataCache.clearCache();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });
    }

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
                                    // Refresh the fragment only if the language has changed
                                    switchFragment(new SettingsFragment());
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

    private void updateLanguageToCache(String languageCode) {
        if (isAdded()) {
            // Save data to memory cache
            dataCache.put("language", languageCode);
        }
    }

    private void switchFragment(Fragment fragment) {
        if(isAdded())
            // Replace the current fragment with the new one
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .commit();
    }

    // Method to show/hide the loading screen
    private void showLoading(boolean show) {
        View view = getView();  // Get the view

        if (view != null) {
            View loadingOverlay = view.findViewById(R.id.loadingOverlay);

            if (show)
                loadingOverlay.setVisibility(View.VISIBLE);
            else
                loadingOverlay.setVisibility(View.GONE);

        }
    }
}

