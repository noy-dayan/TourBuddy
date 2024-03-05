package com.tourbuddy.tourbuddy.fragments;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.chaek.android.RatingBar;

import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.adapters.UserRecyclerViewAdapter;
import com.tourbuddy.tourbuddy.utils.AppUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    int rateFilterStart, rateFilterEnd;

    // Spinner Filters
    Spinner spinnerType, spinnerGender, spinnerCountry;

    RatingBar ratingBarStart, ratingBarEnd;

    SearchView userNameSearchView;
    boolean isInit = true;

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

        userNameSearchView = view.findViewById(R.id.searchView);
        spinnerType = view.findViewById(R.id.spinnerType);
        spinnerGender = view.findViewById(R.id.spinnerGender);
        spinnerCountry = view.findViewById(R.id.spinnerCountry);
        ratingBarStart = view.findViewById(R.id.ratingBarStart);
        ratingBarEnd = view.findViewById(R.id.ratingBarEnd);

        usernameFilter = "";
        typeFilter = "All";
        genderFilter = "All";

        countryFilter = "All";
        rateFilterStart = 0;
        rateFilterEnd = 10;

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        userRecyclerViewAdapter = new UserRecyclerViewAdapter(getActivity(), getContext(), userIdList);
        recyclerView.setItemViewCacheSize(50);
        recyclerView.setAdapter(userRecyclerViewAdapter);

        userNameInputManager();
        genderInputManager();
        typeInputManager();
        countryInputManager();
        rateInputManager();

        btnFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (filterLayout.getVisibility() == View.VISIBLE)
                    filterLayout.setVisibility(View.GONE);
                else
                    filterLayout.setVisibility(View.VISIBLE);
            }
        });

        filterLayout.setOnClickListener(v -> {}); //
        loadDataFromFirebase();
        isInit = false;

        return view;
    }

    private void genderInputManager() {
        // Gender spinner setup
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.genders_filter,
                R.layout.spinner_custom_layout
        );

        adapter.setDropDownViewResource(R.layout.spinner_custom_dropdown_layout);

        spinnerGender.setAdapter(adapter);

        spinnerGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // Update the gender filter
                if(position == 1)
                    genderFilter = "Male";
                else if(position == 2)
                    genderFilter = "Female";
                else if(position == 3)
                    genderFilter = "Other";
                else
                    genderFilter = "All";

                // Reload search results with the new filter
                if(!isInit)
                    loadDataFromFirebase();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // do nothing
            }
        });
    }

    private void rateInputManager() {
        ratingBarStart.setRatingBarListener(new RatingBar.RatingBarListener() {
            @Override
            public void setRatingBar(int i) {
                // Apply filter based on the selected rating
                rateFilterStart = i;
                userIdList.clear();
                if(!isInit)
                    loadDataFromFirebase();
            }
        });

        ratingBarEnd.setRatingBarListener(new RatingBar.RatingBarListener() {
            @Override
            public void setRatingBar(int i) {
                // Apply filter based on the selected rating
                rateFilterEnd = i;
                userIdList.clear();
                if(!isInit)
                    loadDataFromFirebase();
            }
        });

        if(!isInit)
            loadDataFromFirebase();
    }

    private void countryInputManager() {
        // Gender spinner setup
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.countries_filter_for_searching,
                R.layout.spinner_custom_layout
        );

        adapter.setDropDownViewResource(R.layout.spinner_custom_dropdown_layout);

        spinnerCountry.setAdapter(adapter);

        spinnerCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // Update the country filter
                if(position > 0)
                    countryFilter = AppUtils.getSubstringFromCountry(parentView.getItemAtPosition(position).toString());
                else
                    countryFilter = "All";


                // Reload search results with the new filter
                if(!isInit)
                    loadDataFromFirebase();            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // do nothing
            }
        });
    }

    private void typeInputManager() {

        // Gender spinner setup
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.types_filter,
                R.layout.spinner_custom_layout
        );

        adapter.setDropDownViewResource(R.layout.spinner_custom_dropdown_layout);

        spinnerType.setAdapter(adapter);

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // Get the selected type
                onlyTourGuideFilterLayout.setVisibility(position == 2 ? View.VISIBLE : View.GONE);

                // Update the type filter
                if(position == 1)
                    typeFilter = "Tourist";
                else if(position == 2)
                    typeFilter = "Tour Guide";
                else
                    typeFilter = "All";

                if(!isInit)
                    loadDataFromFirebase();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });

    }

    private void userNameInputManager() {
        userNameSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Filter the data based on the new text input
                usernameFilter = newText.trim();
                loadDataFromFirebase();
                return true;
            }
        });


    }

    /**
     * Fetch user data from Firebase Firestore.
     */

    private void loadDataFromFirebase() {
        mUser = mAuth.getCurrentUser();
        if (isAdded() && mUser != null) {
            String currUserId = mUser.getUid();
            userIdList.clear(); // Clear the list before adding filtered users

            // Build the base query
            CollectionReference usersRef = db.collection("users");
            Query query = usersRef;
            query = query.whereNotEqualTo(FieldPath.documentId(), currUserId);

            // Apply type filter if not "All"
            if (!typeFilter.equals("All"))
                query = query.whereEqualTo("type", typeFilter);

            // Apply gender filter if not "All"
            if (!genderFilter.equals("All"))
                query = query.whereEqualTo("gender", genderFilter);

            final int[] limit = {50};
            // Execute the query
            query.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Process the user IDs
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            if(limit[0] > 0){
                                String userId = document.getId();
                                if(!countryFilter.equals("All") && Objects.equals(typeFilter, "Tour Guide")) {
                                    document.getReference().collection("tourPackages").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                        @Override
                                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                            for (DocumentSnapshot tourPackageDocument : queryDocumentSnapshots.getDocuments()) {
                                                if (tourPackageDocument.exists()) {
                                                    ArrayList<String> includedCountries = (ArrayList<String>) tourPackageDocument.get("includedCountries");
                                                    if (includedCountries.contains(countryFilter)) {
                                                        if((0 < rateFilterStart || rateFilterEnd < 10) && Objects.equals(typeFilter, "Tour Guide"))
                                                            ratingQuery(document, limit);
                                                        else {
                                                            if (!usernameFilter.isEmpty())
                                                                usernameQuery(document, limit);
                                                            else
                                                                // Add userId to userIdList
                                                                if (!userIdList.contains(userId)) {
                                                                    // Add userId to userIdList
                                                                    userIdList.add(userId);
                                                                    limit[0]--;
                                                                }
                                                        }
                                                        userRecyclerViewAdapter.notifyDataSetChanged();

                                                    }
                                                }
                                            }
                                        }
                                    });
                                } else {
                                    if((0 < rateFilterStart || rateFilterEnd < 10)&& Objects.equals(typeFilter, "Tour Guide"))
                                        ratingQuery(document, limit);

                                    else {
                                        if (!usernameFilter.isEmpty())
                                            usernameQuery(document, limit);
                                        else
                                            // Add userId to userIdList
                                            if (!userIdList.contains(userId)) {
                                                // Add userId to userIdList
                                                userIdList.add(userId);
                                                limit[0]--;
                                            }
                                    }
                                    userRecyclerViewAdapter.notifyDataSetChanged();
                                }
                            }
                            else
                                break;
                        }
                    }
                    // Notify adapter that data set has changed
                    userRecyclerViewAdapter.notifyDataSetChanged();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Handle failure
                    Log.e("DATABASE", "Error getting documents: " + e.getMessage());
                }
            });
            userRecyclerViewAdapter.notifyDataSetChanged();

        }

    }

    private void usernameQuery(DocumentSnapshot document, final int[] limit){
        String username = document.getString("username");
        if (username != null && username.toLowerCase().startsWith(usernameFilter.toLowerCase())) {
            // Add userId to userIdList
            String userId = document.getId();
            if (!userIdList.contains(userId)) {
                userIdList.add(userId);
                limit[0]--;
            }
        }
        else if(username != null && !username.toLowerCase().startsWith(usernameFilter.toLowerCase())) {
            String userId = document.getId();
            if (!userIdList.contains(userId)) {
                userIdList.remove(userId);
                limit[0]++;
            }
        }
    }

    private void ratingQuery(DocumentSnapshot document, final int[] limit){
        String userId = document.getId();
        document.getReference().collection("reviews").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                int reviewsCount = queryDocumentSnapshots.size();
                if (reviewsCount == 0)
                    if (userIdList.contains(userId)) {
                        userIdList.remove(userId);
                        limit[0]++;
                        userRecyclerViewAdapter.notifyDataSetChanged();
                        return;
                    }

                Long totalRating = document.getLong("totalRating");
                if (totalRating != null) {
                    long rating = totalRating / reviewsCount;
                    if (rateFilterStart <= rating && rating <= rateFilterEnd) {
                        // Add userId to userIdList
                        if (!usernameFilter.isEmpty())
                            usernameQuery(document, limit);
                        else if (!userIdList.contains(userId)) {
                            userIdList.add(userId);
                            limit[0]--;

                        }
                    }
                }
                userRecyclerViewAdapter.notifyDataSetChanged();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (userIdList.contains(userId)) {
                    userIdList.remove(userId);
                    limit[0]++;
                    userRecyclerViewAdapter.notifyDataSetChanged();
                }
            }
        });
    }

}