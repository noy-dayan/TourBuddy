package com.tourbuddy.tourbuddy.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.utils.AppUtils;
import com.tourbuddy.tourbuddy.utils.DataCache;
import com.tourbuddy.tourbuddy.utils.MultiSpinner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;


/*
*
* TODO:
*  - Auto select the current selected countries in the package to be shown in the multi-spinner.
*
*/
public class TourPackageEditFragment extends Fragment implements MultiSpinner.MultiSpinnerListener{

    private static final int MAIN_IMAGE_CORNER_RADIUS = 70;
    DataCache dataCache;
    ImageView packageCoverImage, btnBack;
    Button btnSaveChanges;
    EditText tourDescInput, itineraryInput, durationInput, meetingPointInput,
            includedServicesInput, excludedServicesInput, priceInput,
            cancellationPolicyInput, specialRequirementsInput, additionalInfoInput;
    int packageColor;
    String packageName, userId;

    // Firebase
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore db;
    FirebaseStorage storage;
    StorageReference storageReference;

    // Image picker
    ActivityResultLauncher<Intent> imagePickLauncher;
    Uri selectedImageUri;

    // Multi spinner
    MultiSpinner countryMultiSpinner;
    List<String> selectedCountries;

    // Loading overlay
    View loadingOverlay;
    ProgressBar progressBar;

    // Color picker
    ConstraintLayout colorPicker;
    View colorView;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tour_package_edit, container, false);
        dataCache = DataCache.getInstance();

        // Initialize UI elements
        btnBack = view.findViewById(R.id.btnBack);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);
        packageCoverImage = view.findViewById(R.id.packageCoverImage);
        tourDescInput = view.findViewById(R.id.tourDescInput);
        itineraryInput = view.findViewById(R.id.itineraryInput);
        durationInput = view.findViewById(R.id.durationInput);
        meetingPointInput = view.findViewById(R.id.meetingPointInput);
        includedServicesInput = view.findViewById(R.id.includedServicesInput);
        excludedServicesInput = view.findViewById(R.id.excludedServicesInput);
        priceInput = view.findViewById(R.id.priceInput);
        cancellationPolicyInput = view.findViewById(R.id.cancellationPolicyInput);
        specialRequirementsInput = view.findViewById(R.id.specialRequirementsInput);
        additionalInfoInput = view.findViewById(R.id.additionalInfoInput);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Loading overlay
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        progressBar = view.findViewById(R.id.progressBar);
        countryMultiSpinner = view.findViewById(R.id.countryMultiSpinner);
        colorPicker = view.findViewById(R.id.colorPicker);
        colorView = view.findViewById(R.id.colorView);
        packageColor = R.color.orange_primary;

        //Setting Multi Selection Spinner image.
        String[] countryArray = getResources().getStringArray(R.array.countries_filter);
        List<String> countryList = Arrays.asList(countryArray);
        countryMultiSpinner.setItems(countryList, getString(R.string.selectCountries), this);

        // Set up arguments manager
        argumentsManager();


        // Set up input managers
        editTextInputManager();
        imageInputManager();


        colorPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorPickerDialogBuilder
                        .with(requireContext())
                        .lightnessSliderOnly()
                        .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                        .density(8)
                        .setOnColorSelectedListener(new OnColorSelectedListener() {
                            @Override
                            public void onColorSelected(int selectedColor) {
                                btnSaveChanges.setEnabled(true);
                            }
                        })
                        .setPositiveButton(R.string.confirm, new ColorPickerClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                                // Reset the colorView background tint to default
                                colorView.setBackgroundTintList(null);
                                colorView.setBackgroundTintList(ColorStateList.valueOf(selectedColor));
                                packageColor = selectedColor;
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .build()
                        .show();
            }
        });

        // Set click listeners for buttons
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnSaveChanges.isEnabled())
                    showDiscardChangesDialog();
                else {
                    TourPackageViewFragment tourPackageViewFragment = new TourPackageViewFragment();
                    Bundle args = new Bundle();
                    args.putString("userId", userId);
                    args.putString("packageName", packageName);
                    tourPackageViewFragment.setArguments(args);
                    AppUtils.switchFragment(TourPackageEditFragment.this, tourPackageViewFragment);
                }


            }
        });

        btnSaveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfirmChangesDialog();
            }
        });

        return view;
    }

    private void argumentsManager(){
        if (getArguments() != null) {
            userId = getArguments().getString("userId");
            selectedImageUri = Uri.parse(getArguments().getString("packageCoverImageUri"));
            Glide.with(TourPackageEditFragment.this)
                    .load(selectedImageUri)
                    .apply(RequestOptions.centerCropTransform())
                    .apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(MAIN_IMAGE_CORNER_RADIUS, 0)))
                    .into(packageCoverImage);

            packageName = getArguments().getString("packageName");
            ArrayList<String> includedCountriesStringCode = getArguments().getStringArrayList("includedCountries");
            countryMultiSpinner.setSelectedItemsByStartSubstring(includedCountriesStringCode);
            btnSaveChanges.setEnabled(false);
            tourDescInput.setText(getArguments().getString("tourDesc"));
            itineraryInput.setText(getArguments().getString("itinerary"));
            durationInput.setText(getArguments().getString("duration"));
            meetingPointInput.setText(getArguments().getString("meetingPoint"));
            includedServicesInput.setText(getArguments().getString("includedServices"));
            excludedServicesInput.setText(getArguments().getString("excludedServices"));
            priceInput.setText(getArguments().getString("price"));
            cancellationPolicyInput.setText(getArguments().getString("cancellationPolicy"));
            specialRequirementsInput.setText(getArguments().getString("specialRequirements"));
            additionalInfoInput.setText(getArguments().getString("additionalInfo"));
            packageColor = getArguments().getInt("packageColor");
            colorView.setBackgroundTintList(null);
            colorView.setBackgroundTintList(ColorStateList.valueOf(packageColor));


        }
    }

    /**
     * Save user data to Firebase Firestore.
     */
    private void saveUserDataToFirebase() {
        mUser = mAuth.getCurrentUser();
        if (mUser != null) {
            String userId = mUser.getUid();
            String tourDesc = tourDescInput.getText().toString().trim();
            String itinerary = itineraryInput.getText().toString().trim();
            String duration = durationInput.getText().toString().trim();
            String meetingPoint = meetingPointInput.getText().toString().trim();
            String includedServices = includedServicesInput.getText().toString().trim();
            String excludedServices = excludedServicesInput.getText().toString().trim();
            String price = priceInput.getText().toString().trim();
            String cancellationPolicy = cancellationPolicyInput.getText().toString().trim();
            String specialRequirements = specialRequirementsInput.getText().toString().trim();
            String additionalInfo = additionalInfoInput.getText().toString().trim();

            // Create a map to store user data
            Map<String, Object> packageData = new HashMap<>();
            packageData.put("tourDesc", tourDesc);
            packageData.put("itinerary", itinerary);
            packageData.put("duration", duration);
            packageData.put("meetingPoint", meetingPoint);
            packageData.put("includedServices", includedServices);
            packageData.put("excludedServices", excludedServices);
            packageData.put("price", price);
            packageData.put("cancellationPolicy", cancellationPolicy);
            packageData.put("specialRequirements", specialRequirements);
            packageData.put("additionalInfo", additionalInfo);
            packageData.put("packageColor", packageColor);

            if (selectedCountries != null) {
                packageData.put("includedCountries", selectedCountries);
                updateCountryPackagesCount(userId, selectedCountries);

            }
            if (selectedImageUri != null)
                uploadImage(selectedImageUri, userId, "/tourPackages/" + packageName + "/packageCoverImage");

            // Set the document name as the user ID
            DocumentReference userTourPackageDocumentRef = db.collection("users").
                    document(userId)
                    .collection("tourPackages")
                    .document(packageName);

            // Set the data to the Firestore document
            userTourPackageDocumentRef.update(packageData).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("DATABASE", "DocumentSnapshot added with ID: " + userId);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w("DATABASE", "Error adding document", e);
                }
            });
        }
    }

    private void updateCountryPackagesCount(String userId, List<String> selectedCountries) {
        for (String country : selectedCountries) {
            // Increment the count of packages for the selected country in the new collection
            incrementCountryCount(userId, country);
        }
    }

    private void incrementCountryCount(String userId, String country) {
        // Reference to the new collection
        DocumentReference countryDocumentRef = db.collection("users").document(userId)
                .collection("countryCount").document(country);

        countryDocumentRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // The document already exists, update the count
                        long count = (long) document.get("countIncludedPackages");
                        countryDocumentRef.update("countIncludedPackages", ++count);
                    } else {
                        // The document does not exist, create a new document with count 1
                        Map<String, Object> data = new HashMap<>();
                        data.put("countIncludedPackages", 1);
                        countryDocumentRef.set(data);
                    }
                } else {
                    // Handle errors while querying the new collection
                    Log.e("DATABASE", "Error updating country count", task.getException());
                }
            }
        });
    }

    private boolean isPackageEditValid() {
        return selectedImageUri != null &&
                selectedCountries != null &&
                !selectedCountries.isEmpty() &&
                AppUtils.isEditTextFilled(tourDescInput) &&
                AppUtils.isEditTextFilled(itineraryInput) &&
                AppUtils.isEditTextFilled(durationInput) &&
                AppUtils.isEditTextFilled(meetingPointInput) &&
                AppUtils.isEditTextFilled(includedServicesInput) &&
                AppUtils.isEditTextFilled(excludedServicesInput) &&
                AppUtils.isEditTextFilled(priceInput) &&
                AppUtils.isEditTextFilled(cancellationPolicyInput) &&
                AppUtils.isEditTextFilled(specialRequirementsInput) &&
                AppUtils.isEditTextFilled(additionalInfoInput);


    }

    private void editTextInputManager() {
        // Add a TextWatcher for each EditText to enable/disable the button dynamically
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                btnSaveChanges.setEnabled(isPackageEditValid());
            }
        };

        // Set the TextWatcher for each EditText
        tourDescInput.addTextChangedListener(textWatcher);
        itineraryInput.addTextChangedListener(textWatcher);
        durationInput.addTextChangedListener(textWatcher);
        meetingPointInput.addTextChangedListener(textWatcher);
        includedServicesInput.addTextChangedListener(textWatcher);
        excludedServicesInput.addTextChangedListener(textWatcher);
        priceInput.addTextChangedListener(textWatcher);
        cancellationPolicyInput.addTextChangedListener(textWatcher);
        specialRequirementsInput.addTextChangedListener(textWatcher);
        additionalInfoInput.addTextChangedListener(textWatcher);
    }

    /**
     * Image user input manager.
     */
    private void imageInputManager() {
        // Initialize image picker launcher
        imagePickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            selectedImageUri = data.getData();
                            // Load the image with Glide and apply transformations
                            Glide.with(TourPackageEditFragment.this)
                                    .load(selectedImageUri)
                                    .apply(RequestOptions.centerCropTransform())
                                    .apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(MAIN_IMAGE_CORNER_RADIUS, 0)))
                                    .into(packageCoverImage);
                            btnSaveChanges.setEnabled(true);
                        }
                    }
                });

        // Set click listener for profile picture
        packageCoverImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch image picker
                ImagePicker.with(TourPackageEditFragment.this).crop(4, 3).
                        createIntent(new Function1<Intent, Unit>() {
                            @Override
                            public Unit invoke(Intent intent) {
                                imagePickLauncher.launch(intent);
                                return null;
                            }
                        });
            }
        });
    }


    /**
     * Upload the user profile image to Firebase Storage.
     */
    private void uploadImage(Uri imageUri, String userId, String imageType) {
        StorageReference imageRef = storageReference.child("images/" + userId + "/" + imageType);

        imageRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d("STORAGE", "Image added to the user " + userId + "as " + imageType);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("STORAGE", "Error adding image to the user " + userId + "as " + imageType);
            }
        });
    }

    /**
     * Open "Discard Changes" dialog.
     */
    private void showDiscardChangesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(requireContext().getResources().getString(R.string.confirmDiscardChanges));
        builder.setMessage(requireContext().getResources().getString(R.string.confirmDiscardChangesMessage));
        builder.setPositiveButton(requireContext().getResources().getString(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TourPackageViewFragment tourPackageViewFragment = new TourPackageViewFragment();
                Bundle args = new Bundle();
                args.putString("userId", userId);
                args.putString("packageName", packageName);
                tourPackageViewFragment.setArguments(args);
                AppUtils.switchFragment(TourPackageEditFragment.this, tourPackageViewFragment);
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
     * Open "Confirm Changes" dialog.
     */
    private void showConfirmChangesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(requireContext().getResources().getString(R.string.confirmChanges));
        builder.setMessage(requireContext().getResources().getString(R.string.confirmChangesMessage));
        builder.setPositiveButton(requireContext().getResources().getString(R.string.apply), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveUserDataToFirebase();
                btnSaveChanges.setEnabled(false);
                dataCache.clearCache();
                AppUtils.switchFragment(TourPackageEditFragment.this, new ThisProfileFragment());

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

    @Override
    public void onItemsSelected(boolean[] selected) {
        selectedCountries = getSelectedItemsWithSubstring(countryMultiSpinner.getItems(), selected);
        btnSaveChanges.setEnabled(isPackageEditValid());
    }

    private List<String> getSelectedItemsWithSubstring(List<String> items, boolean[] selected) {
        List<String> selectedItems = new ArrayList<>();
        for (int i = 0; i < selected.length; i++) {
            if (selected[i]) {
                String transformedItem = AppUtils.getSubstringFromCountry(items.get(i));
                selectedItems.add(transformedItem);
            }
        }
        return selectedItems;
    }

}