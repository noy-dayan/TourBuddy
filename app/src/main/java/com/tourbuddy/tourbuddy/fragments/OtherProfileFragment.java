package com.tourbuddy.tourbuddy.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.adapters.TourPackageRecyclerViewAdapter;
import com.tourbuddy.tourbuddy.utils.DataCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OtherProfileFragment extends Fragment implements TourPackageRecyclerViewAdapter.OnLoadCompleteListener {


    // UI elements
    ImageView profilePic;
    TextView username, bio, birthDate, gender, type;

    // Firebase
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    StorageReference storageReference;

    // SwipeRefreshLayout for pull-to-refresh functionality
    SwipeRefreshLayout swipeRefreshLayout;

    // Loading overlay
    View loadingOverlay;
    ProgressBar progressBar;

    // RecyclerView for Tour Packages
    RecyclerView recyclerViewTours;
    TourPackageRecyclerViewAdapter tourPackageRecyclerViewAdapter;
    List<String> tourPackagesIdList = new ArrayList<>();
    String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_other_profile, container, false);

        // Initialize UI elements
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        profilePic = view.findViewById(R.id.profilePic);
        username = view.findViewById(R.id.username);
        bio = view.findViewById(R.id.bio);
        birthDate = view.findViewById(R.id.birthDate);
        gender = view.findViewById(R.id.gender);
        type = view.findViewById(R.id.type);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Initialize loading overlay elements
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerViewTours = view.findViewById(R.id.recyclerViewTours);

        swipeRefreshLayout.setProgressBackgroundColor(R.color.dark_primary);
        swipeRefreshLayout.setColorScheme(R.color.orange_primary);

        // Retrieve user ID from arguments
        if (getArguments() != null) {
            userId = getArguments().getString("userId");
        }

        showLoading(true);
        loadDataFromFirebase(view);


        // Set up pull-to-refresh functionality
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                tourPackagesIdList = new ArrayList<>();
                loadDataFromFirebase(view);
                swipeRefreshLayout.setRefreshing(false);
            });
        } else {
            Log.e("ThisProfileFragment", "SwipeRefreshLayout is null");
        }

        return view;
    }


    /**
     * Fetch user data from Firebase Firestore.
     */
    private void loadDataFromFirebase(View view) {
        // Initialize RecyclerView for Tour Packages
        if (getArguments() != null) {
            tourPackageRecyclerViewAdapter = new TourPackageRecyclerViewAdapter(getActivity(), userId, tourPackagesIdList, false, this);
            recyclerViewTours.setAdapter(tourPackageRecyclerViewAdapter);
            recyclerViewTours.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));


            // Reference to the Firestore document
            DocumentReference userDocumentRef = db.collection("users").document(userId);

            // Retrieve data from Firestore
            userDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (isAdded() && documentSnapshot.exists()) {
                        // Check if the user fields exists in the Firestore document
                        if (documentSnapshot.contains("username"))
                            username.setText(documentSnapshot.getString("username"));

                        if (documentSnapshot.contains("bio")) {
                            bio.setText(documentSnapshot.getString("bio"));
                            bio.setVisibility(View.VISIBLE);
                        }
                        if (documentSnapshot.contains("birthDate"))
                            birthDate.setText(documentSnapshot.getString("birthDate"));

                        if (documentSnapshot.contains("gender")) {
                            String[] genderOptions = requireContext().getResources().getStringArray(R.array.genders);
                            if (Objects.equals(documentSnapshot.getString("gender"), "Male"))
                                gender.setText(genderOptions[1]);
                            else if (Objects.equals(documentSnapshot.getString("gender"), "Female"))
                                gender.setText(genderOptions[2]);
                            else
                                gender.setText(genderOptions[3]);
                        }

                        if (documentSnapshot.contains("type")) {
                            if (Objects.equals(documentSnapshot.getString("type"), "Tourist"))
                                type.setText(getResources().getString(R.string.tourist));
                            else
                                type.setText(getResources().getString(R.string.tour_guide));
                        }

                        // Load the image into the ImageView using Glide
                        storageReference.child("images/" + userId + "/profilePic").getDownloadUrl()
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
                                        // Handle failure to load image
                                        showLoading(false);
                                    }
                                });

                        // Check if the document contains a collection "tourPackages"
                        Log.d("TAG1", "onSuccess: ");

                        // Get the reference to the "tourPackages" collection
                        CollectionReference tourPackagesRef = userDocumentRef.collection("tourPackages");

                        // Retrieve documents from the "tourPackages" collection
                        tourPackagesRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    Log.d("TAG2", document.getId());
                                    tourPackagesIdList.add(document.getId());


                                }
                                tourPackageRecyclerViewAdapter.notifyDataSetChanged();



                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("DATABASE", "Error getting tourPackages collection", e);
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

    /**
     * Show/hide the loading screen.
     */
    private void showLoading(boolean show) {
        if (isAdded()) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onLoadComplete() {

    }
}
