package com.tourbuddy.tourbuddy.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

/**
 * Fragment for editing user profile information.
 */
public class EditProfileFragment extends Fragment {

    // DataCache instance for caching user data
    DataCache dataCache;

    // Constants
    static final short MAX_USERNAME_LENGTH = 12;
    boolean isChanged; // Flag to track changes
    String originalUsername, originalBio, originalGender, originalBirthDate; // Original user data for comparison

    // UI elements
    Spinner spinnerGender;
    Button btnDone;
    EditText inputUsername, inputBirthDate, inputBio;
    ImageView profilePic, btnBack;

    // Calendar for birth date selection
    Calendar calendar;
    DatePickerDialog datePickerDialog;

    // Firebase
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore db;
    FirebaseStorage storage;
    StorageReference storageReference;

    // Image picker
    ActivityResultLauncher<Intent> imagePickLauncher;
    Uri selectedImageUri;

    // Loading overlay
    View loadingOverlay;
    ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        isChanged = false;
        dataCache = DataCache.getInstance();

        // Initialize UI elements
        btnBack = view.findViewById(R.id.btnBack);
        profilePic = view.findViewById(R.id.profilePic);
        inputUsername = view.findViewById(R.id.inputUsername);
        inputBio = view.findViewById(R.id.inputBio);
        spinnerGender = view.findViewById(R.id.spinnerGender);
        inputBirthDate = view.findViewById(R.id.inputBirthDate);
        btnDone = view.findViewById(R.id.btnDone);

        // Initialize calendar for birth date selection
        calendar = Calendar.getInstance();

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Loading overlay
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        progressBar = view.findViewById(R.id.progressBar);

        loadDataFromFirebase(view);

        // Set up input managers
        imageInputManager();
        usernameInputManager();
        bioInputManager();
        genderInputManager();
        birthDateInputManager();

        // Set click listeners for buttons
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isChanged)
                    showDiscardChangesDialog();
                else
                    AppUtils.switchFragment(EditProfileFragment.this, new SettingsFragment());

            }
        });

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfirmChangesDialog();
            }
        });

        return view;
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
                            Glide.with(EditProfileFragment.this)
                                    .load(selectedImageUri)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(profilePic);
                        }
                    }
                });

        // Set click listener for profile picture
        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch image picker
                ImagePicker.with(EditProfileFragment.this).cropSquare().compress(512).maxResultSize(512, 512).
                        createIntent(new Function1<Intent, Unit>() {
                            @Override
                            public Unit invoke(Intent intent) {
                                imagePickLauncher.launch(intent);

                                // Move the condition to this part to properly check if the username is valid
                                boolean isUsernameValid = !inputUsername.getText().toString().isEmpty() && inputUsername.length() <= 16;
                                boolean isGenderSelected = spinnerGender.getSelectedItemPosition() > 0;
                                boolean isBirthDateSelected = !inputBirthDate.getText().toString().isEmpty();
                                isChanged = true;  // Move this here

                                // Enable/Disable Done button based on input validity
                                btnDone.setEnabled(isUsernameValid && isGenderSelected && isBirthDateSelected);
                                return null;
                            }
                        });
            }
        });
    }

    /**
     * Username user input manager.
     */
    private void usernameInputManager() {
        // Username input length filter
        inputUsername.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_USERNAME_LENGTH)});

        // Username text watcher for enabling/disabling the done button
        inputUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (originalUsername != null) {
                    boolean isUsernameValid = !editable.toString().trim().isEmpty()
                            && editable.toString().trim().length() <= MAX_USERNAME_LENGTH
                            && editable.toString().trim().matches("^(?=.*[a-zA-Z0-9])[a-zA-Z0-9 ]+$");
                    boolean isGenderSelected = spinnerGender.getSelectedItemPosition() > 0;
                    boolean isBirthDateSelected = !inputBirthDate.getText().toString().isEmpty();

                    if (!Objects.equals(originalUsername, editable.toString().trim()))
                        isChanged = true;

                    // Enable/Disable Done button based on input validity
                    btnDone.setEnabled(isUsernameValid && isGenderSelected && isBirthDateSelected && isChanged);
                }
            }
        });
    }


    /**
     * Bio user input manager.
     */
    private void bioInputManager() {
        inputBio.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (originalBio != null) {
                    boolean isUsernameValid = !inputUsername.toString().isEmpty()
                            && inputUsername.length() <= MAX_USERNAME_LENGTH
                            && inputUsername.getText().toString().matches("^(?=.*[a-zA-Z0-9])[a-zA-Z0-9 ]+$");
                    boolean isGenderSelected = spinnerGender.getSelectedItemPosition() > 0;
                    boolean isBirthDateSelected = !inputBirthDate.getText().toString().isEmpty();
                    if (!Objects.equals(originalBio, inputBio.getText().toString()))
                        isChanged = true;

                    // Enable/Disable Done button based on input validity
                    btnDone.setEnabled(isUsernameValid && isGenderSelected && isBirthDateSelected && isChanged);
                }
            }
        });
    }

    /**
     * Gender user input manager.
     */
    private void genderInputManager() {
        // Gender spinner setup
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.genders,
                R.layout.spinner_custom_layout
        );

        adapter.setDropDownViewResource(R.layout.spinner_custom_dropdown_layout);

        spinnerGender.setAdapter(adapter);

        spinnerGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                boolean isUsernameValid = !inputUsername.getText().toString().isEmpty()
                        && inputUsername.length() <= MAX_USERNAME_LENGTH
                        && inputUsername.getText().toString().matches("^(?=.*[a-zA-Z0-9])[a-zA-Z0-9 ]+$");
                boolean isGenderSelected = position > 0;
                boolean isBirthDateSelected = !inputBirthDate.getText().toString().isEmpty();

                if (!Objects.equals(originalGender, spinnerGender.getSelectedItem().toString()))
                    isChanged = true;

                // Enable/Disable Done button based on input validity
                btnDone.setEnabled(isUsernameValid && isGenderSelected && isBirthDateSelected && isChanged);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }

    /**
     * BirthDate user input manager.
     */
    private void birthDateInputManager() {
        // Date picker dialog setup
        DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                updateCalendar();

                boolean isUsernameValid = !inputUsername.getText().toString().isEmpty()
                        && inputUsername.length() <= MAX_USERNAME_LENGTH
                        && inputUsername.getText().toString().matches("^(?=.*[a-zA-Z0-9])[a-zA-Z0-9 ]+$");
                boolean isGenderSelected = spinnerGender.getSelectedItemPosition() > 0;
                boolean isBirthDateSelected = !inputBirthDate.getText().toString().isEmpty();

                if (!Objects.equals(originalBirthDate, inputBirthDate.getText().toString()))
                    isChanged = true;

                // Enable/Disable Done button based on input validity
                btnDone.setEnabled(isUsernameValid && isGenderSelected && isBirthDateSelected && isChanged);
            }

            private void updateCalendar() {
                String format = "dd/MM/yyyy";
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
                inputBirthDate.setText(sdf.format(calendar.getTime()));
            }
        };

        datePickerDialog = new DatePickerDialog(getContext(),
                android.R.style.Theme_Holo_Dialog_NoActionBar_MinWidth,
                date, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        // Set date picker limits
        Calendar maxDateCalendar = Calendar.getInstance();
        maxDateCalendar.add(Calendar.YEAR, -16);
        datePickerDialog.getDatePicker().setMaxDate(maxDateCalendar.getTimeInMillis());

        Calendar minDateCalendar = Calendar.getInstance();
        minDateCalendar.add(Calendar.YEAR, -100);
        datePickerDialog.getDatePicker().setMinDate(minDateCalendar.getTimeInMillis());

        // InputBirthDate click listener to show date picker
        inputBirthDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePickerDialog.show();
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
     * Save user data to Firebase Firestore.
     */
    private void saveUserDataToFirebase() {
        mUser = mAuth.getCurrentUser();
        if (mUser != null) {
            String userId = mUser.getUid();
            String username = inputUsername.getText().toString().trim();
            String gender;
            if (spinnerGender.getSelectedItemPosition() == 1)
                gender = "Male";
            else if (spinnerGender.getSelectedItemPosition() == 2)
                gender = "Female";
            else
                gender = "Other";

            String birthDate = inputBirthDate.getText().toString().trim();
            String bio = inputBio.getText().toString().trim();

            // Create a map to store user data
            Map<String, Object> userData = new HashMap<>();
            userData.put("username", username);
            if (!bio.isEmpty()) userData.put("bio", bio);
            else userData.put("bio", "");
            userData.put("gender", gender);
            userData.put("birthDate", birthDate);
            if (selectedImageUri != null) uploadImage(selectedImageUri, userId, "profilePic");

            // Set the document name as the user ID
            DocumentReference userDocumentRef = db.collection("users").document(userId);

            // Set the data to the Firestore document
            userDocumentRef.update(userData).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("DATABASE", "DocumentSnapshot added with ID: " + userId);
                    saveDataToCache(selectedImageUri);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w("DATABASE", "Error adding document", e);
                }
            });
        }
    }

    /**
     * Load user data from Firebase Firestore.
     */
    private void loadDataFromFirebase(View view) {
        showLoading(true);
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
                        if (documentSnapshot.contains("username")) {
                            originalUsername = documentSnapshot.getString("username");
                            inputUsername.setText(documentSnapshot.getString("username"));
                        }
                        if (documentSnapshot.contains("bio")) {
                            originalBio = documentSnapshot.getString("bio");
                            inputBio.setText(documentSnapshot.getString("bio"));
                        }
                        if (documentSnapshot.contains("birthDate")) {
                            originalBirthDate = documentSnapshot.getString("birthDate");
                            inputBirthDate.setText(documentSnapshot.getString("birthDate"));
                        }

                        if (documentSnapshot.contains("gender")) {
                            String gender = documentSnapshot.getString("gender");
                            if (gender != null) {
                                if (gender.equals("Male"))
                                    spinnerGender.setSelection(1);
                                else if (gender.equals("Female"))
                                    spinnerGender.setSelection(2);
                                else
                                    spinnerGender.setSelection(3);

                                originalGender = spinnerGender.getSelectedItem().toString();
                            }
                        }
                        isChanged = false;

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
                                        isChanged = false;

                                    }

                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        showLoading(false);
                                        isChanged = false;

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
     * Save user data to data cache.
     */
    private void saveDataToCache(Uri profilePicUri) {
        if (isAdded()) {
            // Save data to memory cache
            dataCache.put("username", inputUsername.getText().toString().trim());
            dataCache.put("bio", inputBio.getText().toString().trim());
            dataCache.put("birthDate", inputBirthDate.getText().toString());
            dataCache.put("gender", spinnerGender.getSelectedItem().toString());
            if (profilePicUri != null)
                dataCache.put("profilePicUri", profilePicUri.toString());
        }
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
                AppUtils.switchFragment(EditProfileFragment.this, new SettingsFragment());
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
                isChanged = false;
                btnDone.setEnabled(false);
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
     * Enable/Disable loading screen.
     */
    private void showLoading(boolean show) {
        if (isAdded()) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}