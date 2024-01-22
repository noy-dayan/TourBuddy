package com.tourbuddy.tourbuddy.fragments;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.adapters.UserRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for learning about the app and it's developers.
 */
public class SearchFragment extends Fragment {

    RecyclerView recyclerView;
    UserRecyclerViewAdapter userRecyclerViewAdapter;

    // Initialize userList with user data
    List<String> userIdList = new ArrayList<String>();

    // Firebase
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore db;
    StorageReference storageReference;

    // Loading overlay
    View loadingOverlay;
    ProgressBar progressBar;

    // Filter UI
    ImageButton btnFilter;
    View filterLayout, onlyTourGuideFilterLayout;


    // Search Filters
    String countryFilter, typeFilter, usernameFilter, genderFilter;

    // Spinner Filters
    Spinner spinnerType, spinnerGender, spinnerCountry;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Initialize filter button
        btnFilter = view.findViewById(R.id.btnFilter);
        filterLayout = view.findViewById(R.id.filterLayout);
        onlyTourGuideFilterLayout = view.findViewById(R.id.onlyTourGuideFilterLayout);

        spinnerType = view.findViewById(R.id.spinnerType);
        spinnerGender = view.findViewById(R.id.spinnerGender);
        spinnerCountry = view.findViewById(R.id.spinnerCountry);

        genderInputManager(spinnerGender);
        typeInputManager(spinnerType);
        countryInputManager(spinnerCountry);

        // Initialize loading overlay elements
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        progressBar = view.findViewById(R.id.progressBar);

        btnFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (filterLayout.getVisibility() == View.VISIBLE) {
                    filterLayout.setVisibility(View.GONE);
                    setRecyclerViewTopMargin(70);
                } else {
                    filterLayout.setVisibility(View.VISIBLE);
                    setRecyclerViewTopMargin(0);


                    spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                            // Get the selected type
                            String selectedType = parentView.getItemAtPosition(position).toString();

                            // Enable or disable the country spinner based on the selected type
                            onlyTourGuideFilterLayout.setVisibility(getContext().getResources().getString(R.string.tour_guide).equals(selectedType)? View.VISIBLE : View.GONE);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parentView) {
                            // Do nothing
                        }
                    });
                }
            }
        });

        loadDataFromFirebase(view);

        // Other initialization code...

        return view;
    }

    // Function to set the top margin of the RecyclerView
    private void setRecyclerViewTopMargin(int topMarginInDp) {
        // Convert dp to pixels
        float density = getResources().getDisplayMetrics().density;
        int topMarginInPixels = (int) (topMarginInDp * density);

        // Get the current layout parameters of the RecyclerView
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) recyclerView.getLayoutParams();

        // Set the top margin
        layoutParams.topMargin = topMarginInPixels;

        // Apply the updated layout parameters to the RecyclerView
        recyclerView.setLayoutParams(layoutParams);
    }

    private void genderInputManager(Spinner spinnerGender) {
        // Gender spinner setup
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.genders_filter,
                R.layout.spinner_custom_layout
        );

        adapter.setDropDownViewResource(R.layout.spinner_custom_dropdown_layout);

        spinnerGender.setAdapter(adapter);

        spinnerGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }

    private void countryInputManager(Spinner spinnerCountry) {
        // Gender spinner setup
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.countries_filter,
                R.layout.spinner_custom_layout
        );

        adapter.setDropDownViewResource(R.layout.spinner_custom_dropdown_layout);

        spinnerCountry.setAdapter(adapter);

        spinnerCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }

    private void typeInputManager(Spinner spinnerType) {

        // Gender spinner setup
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.types_filter,
                R.layout.spinner_custom_layout
        );

        adapter.setDropDownViewResource(R.layout.spinner_custom_dropdown_layout);

        spinnerType.setAdapter(adapter);

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }

    /**
     * Fetch user data from Firebase Firestore.
     */
    private void loadDataFromFirebase(View view) {
        countryFilter = "All";
        typeFilter = "All";
        usernameFilter = "All";
        genderFilter = "All";

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        userRecyclerViewAdapter = new UserRecyclerViewAdapter(getActivity() ,getContext(), loadingOverlay, progressBar, userIdList, countryFilter, typeFilter, usernameFilter, genderFilter);
        recyclerView.setAdapter(userRecyclerViewAdapter);
        mUser = mAuth.getCurrentUser();
        if (mUser != null) {
            showLoading(true);
            String currUserId = mUser.getUid();

            // Reference to the Firestore collection "users"
            CollectionReference userCollectionRef = db.collection("users");

            // Query the first 30 documents (user IDs)
            userCollectionRef.limit(30).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    if (isAdded() && !queryDocumentSnapshots.isEmpty()) {
                        // Process the user IDs
                        List<DocumentSnapshot> documents = queryDocumentSnapshots.getDocuments();
                        for (DocumentSnapshot document : documents) {
                            if(userIdList != null)
                                if(!currUserId.equals(document.getId())) {
                                    // Add userId to your list or update UI as needed
                                    userIdList.add(document.getId());
                                }
                        }
                        // Notify the adapter that the data has changed
                        userRecyclerViewAdapter.notifyDataSetChanged();
                        //userRecyclerViewAdapter = new UserRecyclerViewAdapter(getContext(), userIdList);
                        //recyclerView.setAdapter(userRecyclerViewAdapter);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DATABASE", "Error getting documents", e);
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
}