package com.tourbuddy.tourbuddy.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

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
import com.tourbuddy.tourbuddy.utils.AppUtils;
import com.tourbuddy.tourbuddy.utils.DataCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class TourPackageViewFragment extends Fragment {

    private static final int MAIN_IMAGE_CORNER_RADIUS = 70;
    DataCache dataCache;

    // UI elements
    ImageView packageCoverImage, btnBack;
    TextView packageName, includedCountries, tourDesc, itinerary, duration, meetingPoint, includedServices,
            excludedServices, price, cancellationPolicy, specialRequirements, additionalInfo;
    int packageColor;
    Button btnEditPackage, btnDeletePackage;

    // Firebase
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    StorageReference storageReference;

    // Loading overlay
    View loadingOverlay;
    ProgressBar progressBar;

    // User ID received from arguments
    String userId, userType;
    ArrayList<String> includedCountriesFullNames;
    Uri packageCoverImageUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tour_package_view, container, false);
        dataCache = DataCache.getInstance();

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
        cancellationPolicy = view.findViewById(R.id.cancellationPolicy);
        specialRequirements = view.findViewById(R.id.specialRequirements);
        additionalInfo = view.findViewById(R.id.additionalInfo);

        btnEditPackage = view.findViewById(R.id.btnEditPackage);
        btnDeletePackage = view.findViewById(R.id.btnDeletePackage);
        btnBack = view.findViewById(R.id.btnBack);

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

            if(Objects.equals(mAuth.getUid(), userId)) {
                btnEditPackage.setVisibility(View.VISIBLE);
                btnDeletePackage.setVisibility(View.VISIBLE);

            }
        }

        showLoading(true);

        // Data not found in cache, fetch from the server
        loadDataFromFirebase(view);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Objects.equals(mAuth.getUid(), userId))
                    AppUtils.switchFragment(TourPackageViewFragment.this, new ThisProfileFragment());
                else{
                    OtherProfileFragment otherProfileFragment = new OtherProfileFragment();
                    Bundle args = new Bundle();
                    args.putString("userId", userId);
                    otherProfileFragment.setArguments(args);
                    AppUtils.switchFragment(TourPackageViewFragment.this, otherProfileFragment);
                }

            }
        });

        btnDeletePackage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPackageDeletionDialog();
            }
        });

        btnEditPackage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TourPackageEditFragment tourPackageEditFragment = new TourPackageEditFragment();
                Bundle args = new Bundle();
                args.putString("userId", userId);
                args.putString("packageName", packageName.getText().toString());
                args.putString("packageCoverImageUri", packageCoverImageUri.toString());
                args.putStringArrayList("includedCountries", includedCountriesFullNames);
                args.putString("tourDesc", tourDesc.getText().toString());
                args.putString("itinerary", itinerary.getText().toString());
                args.putString("duration", duration.getText().toString());
                args.putString("meetingPoint", meetingPoint.getText().toString());
                args.putString("includedServices", includedServices.getText().toString());
                args.putString("excludedServices", excludedServices.getText().toString());
                args.putString("price", price.getText().toString());
                args.putString("cancellationPolicy", cancellationPolicy.getText().toString());
                args.putString("specialRequirements", specialRequirements.getText().toString());
                args.putString("additionalInfo", additionalInfo.getText().toString());
                args.putInt("packageColor", packageColor);
                tourPackageEditFragment.setArguments(args);
                AppUtils.switchFragment(TourPackageViewFragment.this, tourPackageEditFragment);


            }
        });

        return view;
    }


    /**
     * Open "Discard Changes" dialog.
     */
    private void showPackageDeletionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(requireContext().getResources().getString(R.string.confirmPackageDeletion));
        builder.setMessage(requireContext().getResources().getString(R.string.confirmPackageDeletionMessage));
        builder.setPositiveButton(requireContext().getResources().getString(R.string.delete_package), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkForActiveToursBeforeDeletion();
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

    private void checkForActiveToursBeforeDeletion() {
        if (userId != null && packageName.getText() != null) {
            // Reference to the Firestore document using the provided userId
            DocumentReference packageDocumentRef = db.collection("users")
                    .document(userId)
                    .collection("tourPackages")
                    .document(packageName.getText().toString());

            // Query active tours to check if there are any associated with this package
            db.collection("users")
                    .document(userId)
                    .collection("activeTours")
                    .whereEqualTo("tourPackageRef", packageDocumentRef)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // Active tours found, notify the user and prevent deletion
                            showActiveToursExistDialog();
                        } else {
                            // No active tours found, proceed with deletion
                            deletePackage();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle failure to query active tours
                        Log.e("DATABASE", "Error checking for active tours", e);
                    });
        }
    }
    private void showActiveToursExistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Active Tours Exist");
        builder.setMessage("There are active tours associated with this package.\nYou cannot delete it until all tours are completed or canceled.");
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Dismiss the dialog
            }
        });
        builder.show();
    }

    private void deletePackage() {
        if (userId != null && packageName.getText() != null) {
            // Reference to the Firestore document using the provided userId
            DocumentReference packageDocumentRef = db.collection("users")
                    .document(userId)
                    .collection("tourPackages")
                    .document(packageName.getText().toString());

            // Reference to the image file in Firebase Storage
            StorageReference imageRef = storageReference.child("images/" + userId + "/tourPackages/" + packageName.getText().toString() + "/packageCoverImage");

            // Delete the package document and image
            packageDocumentRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    // Package document deleted successfully

                    // Delete the image file
                    imageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Image file deleted successfully
                            dataCache.clearCache();
                            // Navigate to the appropriate fragment
                            if (Objects.equals(mAuth.getUid(), userId))
                                AppUtils.switchFragment(TourPackageViewFragment.this, new ThisProfileFragment());
                            else {
                                OtherProfileFragment profileOtherFragment = new OtherProfileFragment();
                                Bundle args = new Bundle();
                                args.putString("userId", userId);
                                profileOtherFragment.setArguments(args);
                                AppUtils.switchFragment(TourPackageViewFragment.this, profileOtherFragment);

                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Handle the failure to delete the image file
                            Log.e("STORAGE", "Error deleting image file", e);
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Handle the failure to delete the package document
                    Log.e("DATABASE", "Error deleting package document", e);
                }
            });
        }
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
                            ArrayList<String> includedCountriesStringCode = (ArrayList<String>) documentSnapshot.get("includedCountries");

                            // Convert country codes to full names
                            includedCountriesFullNames = new ArrayList<>();
                            for (String countryCode : includedCountriesStringCode) {
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

                        if (documentSnapshot.contains("cancellationPolicy"))
                            cancellationPolicy.setText(documentSnapshot.getString("cancellationPolicy"));

                        if (documentSnapshot.contains("specialRequirements"))
                            specialRequirements.setText(documentSnapshot.getString("specialRequirements"));

                        if (documentSnapshot.contains("additionalInfo"))
                            additionalInfo.setText(documentSnapshot.getString("additionalInfo"));

                        if (documentSnapshot.contains("packageColor"))
                            packageColor = Integer.parseInt(String.valueOf(documentSnapshot.get("packageColor")));

                            // Load the image into the ImageView using Glide
                        storageReference.child("images/" + userId + "/tourPackages/" + documentSnapshot.getId() + "/packageCoverImage").getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        // Got the download URL for 'users/me/profile.png'
                                        packageCoverImageUri = uri;
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

    }

    private void enableTourGuideLayout(){

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