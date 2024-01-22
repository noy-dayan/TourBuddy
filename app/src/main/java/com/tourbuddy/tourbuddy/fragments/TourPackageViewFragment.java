package com.tourbuddy.tourbuddy.fragments;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tourbuddy.tourbuddy.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class TourPackageViewFragment extends Fragment {

    private static final int MAIN_IMAGE_CORNER_RADIUS = 70;
    // UI elements
    ImageView packageCoverImage;
    TextView packageName, includedCountries, tourDesc, itinerary, duration, meetingPoint, includedServices,
            excludedServices, price, groupSize, cancellationPolicy, specialRequirements, additionalInfo;

    Button btnEditPackage;
    // Firebase
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    StorageReference storageReference;

    // SwipeRefreshLayout for pull-to-refresh functionality
    SwipeRefreshLayout swipeRefreshLayout;

    // Loading overlay
    View loadingOverlay;
    ProgressBar progressBar;

    // User ID received from arguments
    String userId, userType;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tour_package_view, container, false);

        // Initialize UI elements
        packageName = view.findViewById(R.id.packageName);
        packageCoverImage = view.findViewById(R.id.packageCoverImage);
        includedCountries = view.findViewById(R.id.includedCountries);
        tourDesc = view.findViewById(R.id.tourDesc);
        itinerary = view.findViewById(R.id.itinerary);
        duration = view.findViewById(R.id.duration);
        meetingPoint = view.findViewById(R.id.meetingPoint);
        includedServices = view.findViewById(R.id.includedServices);
        excludedServices = view.findViewById(R.id.excludedServices);
        price = view.findViewById(R.id.price);
        groupSize = view.findViewById(R.id.groupSize);
        cancellationPolicy = view.findViewById(R.id.cancellationPolicy);
        specialRequirements = view.findViewById(R.id.specialRequirements);
        additionalInfo = view.findViewById(R.id.additionalInfo);

        btnEditPackage = view.findViewById(R.id.btnEditPackage);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Initialize loading overlay elements
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        progressBar = view.findViewById(R.id.progressBar);

        // Retrieve user ID from arguments
        if (getArguments() != null) {
            userId = getArguments().getString("userId");
            packageName.setText(getArguments().getString("packageName"));
        }

        showLoading(true);

        // Data not found in cache, fetch from the server
        loadDataFromFirebase(view);

        return view;
    }

   /**
     * Fetch user data from Firebase Firestore.
     */
   private void loadDataFromFirebase(View view) {
        if (userId != null) {
            // Reference to the Firestore document using the provided userId
            DocumentReference userDocumentRef = db.collection("users")
                    .document(userId);

            DocumentReference packageDocumentRef = userDocumentRef
                    .collection("tourPackages")
                    .document(packageName.getText().toString());


            userDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (isAdded() && documentSnapshot.exists())
                        // Check if the user fields exist in the Firestore document
                        if (documentSnapshot.contains("type"))
                            userType = documentSnapshot.getString("type");


                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DATABASE", "Error getting document", e);
                }
            });


            // Retrieve data from Firestore
            packageDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (isAdded() && documentSnapshot.exists()) {
                        // Check if the user fields exist in the Firestore document
                        if (documentSnapshot.contains("includedCountries")) {
                            ArrayList<String> includedCountriesString = (ArrayList<String>) documentSnapshot.get("includedCountries");

                            // Convert country codes to full names
                            List<String> includedCountriesFullNames = new ArrayList<>();
                            for (String countryCode : includedCountriesString) {
                                String fullName = getCountryFullName(requireContext(), countryCode);
                                includedCountriesFullNames.add(fullName);
                            }

                            includedCountries.setText(TextUtils.join(", ", includedCountriesFullNames));
                        }

                        if (documentSnapshot.contains("tourDesc"))
                            tourDesc.setText(documentSnapshot.getString("tourDesc"));

                        if (documentSnapshot.contains("itinerary"))
                            itinerary.setText(documentSnapshot.getString("itinerary"));

                        if (documentSnapshot.contains("duration"))
                            duration.setText(documentSnapshot.getString("duration"));

                        if (documentSnapshot.contains("meetingPoint"))
                            meetingPoint.setText(documentSnapshot.getString("meetingPoint"));

                        if (documentSnapshot.contains("includedServices"))
                            includedServices.setText(documentSnapshot.getString("includedServices"));

                        if (documentSnapshot.contains("excludedServices"))
                            excludedServices.setText(documentSnapshot.getString("excludedServices"));

                        if (documentSnapshot.contains("price"))
                            price.setText(documentSnapshot.getString("price"));

                        if (documentSnapshot.contains("groupSize"))
                            groupSize.setText(documentSnapshot.getString("groupSize"));

                        if (documentSnapshot.contains("cancellationPolicy"))
                            cancellationPolicy.setText(documentSnapshot.getString("cancellationPolicy"));

                        if (documentSnapshot.contains("specialRequirements"))
                            specialRequirements.setText(documentSnapshot.getString("specialRequirements"));

                        if (documentSnapshot.contains("additionalInfo"))
                            additionalInfo.setText(documentSnapshot.getString("additionalInfo"));



                        // Load the image into the ImageView using Glide
                        storageReference.child("images/" + userId + "/tourPackages/" + documentSnapshot.getId() + "/packageCoverImage").getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        // Got the download URL for 'users/me/profile.png'
                                        Glide.with(view)
                                                .load(uri)
                                                .apply(RequestOptions.centerCropTransform())
                                                .apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(MAIN_IMAGE_CORNER_RADIUS, 0)))
                                                .into(packageCoverImage);

                                        if(userType.equals("Tour Guide"))
                                            enableTourGuideLayout();
                                        else
                                            enableTouristLayout();

                                        showLoading(false);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        // Handle failure to load image
                                        if(userType.equals("Tour Guide"))
                                            enableTourGuideLayout();
                                        else
                                            enableTouristLayout();

                                        showLoading(false);
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

    private void enableTouristLayout(){
        btnEditPackage.setVisibility(View.VISIBLE);

    }

    private void enableTourGuideLayout(){
        btnEditPackage.setVisibility(View.VISIBLE);

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

    public static String getCountryFullName(Context context, String countryCode) {
        // Retrieve the array of country names with codes from resources
        Resources resources = context.getResources();
        String[] countriesArray = resources.getStringArray(R.array.countries_filter);

        // Loop through the array to find a match
        for (String countryInfo : countriesArray) {
            String[] parts = countryInfo.split("\\s+"); // Split by whitespace
            if (parts.length >= 2 && parts[parts.length - 1].equals("(" + countryCode + ")")) {
                // Extract the full name of the country
                StringBuilder fullName = new StringBuilder(parts[0]);
                for (int i = 1; i < parts.length - 1; i++) {
                    fullName.append(" ").append(parts[i]);
                }
                return fullName.toString();
            }
        }

        // If no match is found, return the original countryCode
        return countryCode;
    }
}
