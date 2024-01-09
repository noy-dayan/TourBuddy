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

public class ProfileFragment extends Fragment {

    private ImageView profilePic;
    private TextView username, bio, birthDate, gender, type;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseFirestore db;
    private StorageReference storageReference;

    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        showLoading(true);
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
        loadProfileData(view);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadProfileData(view);
                swipeRefreshLayout.setRefreshing(false);
            });
        } else {
            Log.e("ProfileFragment", "SwipeRefreshLayout is null");
        }

        return view;
    }

    private void loadProfileData(View view) {
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
                        // Check if the profilePic field exists in the Firestore document
                        if (documentSnapshot.contains("username"))
                            username.setText(documentSnapshot.getString("username"));
                        if (documentSnapshot.contains("bio"))
                            bio.setText(documentSnapshot.getString("bio"));
                        if (documentSnapshot.contains("birthDate"))
                            birthDate.setText(documentSnapshot.getString("birthDate"));
                        if (documentSnapshot.contains("gender"))
                            gender.setText(documentSnapshot.getString("gender"));
                        if (documentSnapshot.contains("type"))
                            type.setText(documentSnapshot.getString("type"));

                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DATABASE", "Error getting document", e);
                }
            });

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

                        }

                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            showLoading(false);
                        }
                    });

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
