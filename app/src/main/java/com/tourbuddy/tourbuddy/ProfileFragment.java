package com.tourbuddy.tourbuddy;


import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

import java.util.Locale;
import java.util.Objects;

public class ProfileFragment extends Fragment {

    DataCache dataCache;

    ImageView profilePic;
    TextView username, bio, birthDate, gender, type;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore db;
    StorageReference storageReference;

    SwipeRefreshLayout swipeRefreshLayout;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        showLoading(true);
        dataCache = DataCache.getInstance();

        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        profilePic = view.findViewById(R.id.profilePic);
        username = view.findViewById(R.id.username);
        bio = view.findViewById(R.id.bio);
        birthDate = view.findViewById(R.id.birthDate);
        gender = view.findViewById(R.id.gender);
        type = view.findViewById(R.id.type);

        // Attempt to load data from cache
        if (loadDataFromCache(view))
            // Data loaded from cache, no need to fetch from the server
            showLoading(false);
        else
            // Data not found in cache, fetch from the server
            loadDataFromFirebase(view);


        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadDataFromFirebase(view);
                swipeRefreshLayout.setRefreshing(false);
            });
        } else {
            Log.e("ProfileFragment", "SwipeRefreshLayout is null");
        }

        return view;
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
                        if (documentSnapshot.contains("bio"))
                            bio.setText(documentSnapshot.getString("bio"));
                        if (documentSnapshot.contains("birthDate"))
                            birthDate.setText(documentSnapshot.getString("birthDate"));
                        if (documentSnapshot.contains("gender")){
                            String[] genderOptions = getContext().getResources().getStringArray(R.array.gender_options);

                            if (Objects.equals(documentSnapshot.getString("gender"), "Male"))
                                gender.setText(genderOptions[1]);
                            else if (Objects.equals(documentSnapshot.getString("gender"), "Male"))
                                gender.setText(genderOptions[2]);
                            else
                                gender.setText(genderOptions[3]);

                        }
                        if (documentSnapshot.contains("type")){
                            if (Objects.equals(documentSnapshot.getString("type"), "Tourist"))
                                type.setText(getResources().getString(R.string.tourist));
                            else
                                type.setText(getResources().getString(R.string.tour_guide));
                        }

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
                                        showLoading(false);
                                        saveDataToCache(null);
                                    }
                                });

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

    private boolean loadDataFromCache(View view) {
        // Check if data exists in memory cache
        if (dataCache.get("username") != null && dataCache.get("bio") != null
                && dataCache.get("birthDate") != null && dataCache.get("gender") != null
                && dataCache.get("type") != null) {

            // Load data from memory cache
            username.setText((String) dataCache.get("username"));
            bio.setText((String) dataCache.get("bio"));
            birthDate.setText((String) dataCache.get("birthDate"));
            gender.setText((String) dataCache.get("gender"));
            type.setText((String) dataCache.get("type"));

            if(dataCache.get("profilePicUrl") != null) {
                // Load profile picture from memory cache (if available)
                String profilePicUrl = (String) dataCache.get("profilePicUrl");
                if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                    Glide.with(view)
                            .load(profilePicUrl)
                            .apply(RequestOptions.circleCropTransform())
                            .into(profilePic);
                }
            }
            return true; // Data loaded from memory cache
        }

        return false; // Data not found in memory cache
    }

    private void saveDataToCache(Uri profilePicUri) {
        if (isAdded()) {
            // Save data to memory cache
            dataCache.put("username", username.getText().toString());
            dataCache.put("bio", bio.getText().toString());
            dataCache.put("birthDate", birthDate.getText().toString());
            dataCache.put("gender", gender.getText().toString());
            dataCache.put("type", type.getText().toString());
            if (profilePicUri != null)
                dataCache.put("profilePicUri", profilePicUri.toString());
        }
    }

    // Method to show/hide the loading screen
    private void showLoading(boolean show) {
        View view = getView();  // Get the view

        if (view != null) {
            View loadingOverlay = view.findViewById(R.id.loadingOverlay);
            View mainOverlay = view.findViewById(R.id.mainOverlay);

            if (show) {
                loadingOverlay.setVisibility(View.VISIBLE);
                mainOverlay.setVisibility(View.GONE);
            } else {
                loadingOverlay.setVisibility(View.GONE);
                mainOverlay.setVisibility(View.VISIBLE);
            }
        }
    }

}