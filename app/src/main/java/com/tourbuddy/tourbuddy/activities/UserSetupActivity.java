package com.tourbuddy.tourbuddy.activities;

import android.app.Activity;
import android.app.DatePickerDialog;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.utils.AppUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * Activity for user setup, allowing users to input profile information.
 */
public class UserSetupActivity extends AppCompatActivity {

    // Constants
    private static final short MAX_USERNAME_LENGTH = 12;

    // UI elements
    Spinner spinnerGender;
    Button btnDone;
    EditText inputUsername, inputBirthDate, inputBio;
    ImageView profilePic, btnBack;
    RadioGroup toggleRadioGroup;

    // Calendar for birth date selection
    Calendar calendar;
    DatePickerDialog datePickerDialog;

    // Firebase
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore db;
    FirebaseStorage storage;
    StorageReference storageReference;

    // Radio button selection
    String selectedRadioGroupChoice = "Tourist";

    // Image picker
    ActivityResultLauncher<Intent> imagePickLauncher;
    Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_setup);

        // Initialize image picker launcher
        imagePickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        if(data != null && data.getData() != null) {
                            selectedImageUri = data.getData();
                            Glide.with(UserSetupActivity.this)
                                    .load(selectedImageUri)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(profilePic);
                        }
                    }
                });

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Initialize UI elements
        profilePic = findViewById(R.id.profilePic);
        inputUsername = findViewById(R.id.inputUsername);
        inputBio = findViewById(R.id.inputBio);
        spinnerGender = findViewById(R.id.spinnerGender);
        inputBirthDate = findViewById(R.id.inputBirthDate);
        toggleRadioGroup = findViewById(R.id.toggleSwitch);

        // Initialize calendar for birth date selection
        calendar = Calendar.getInstance();

        btnDone = findViewById(R.id.btnDone);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppUtils.switchActivity(UserSetupActivity.this, MainActivity.class, "rtl");
            }
        });

        // Retrieve the data from the intent
        Intent intent = getIntent();
        if (intent != null) {
            String username = intent.getStringExtra("username");
            Uri userProfilePicUri = intent.getParcelableExtra("userProfilePicUri");

            // Check if the data is received successfully
            if (username != null && userProfilePicUri != null) {
                // Data received successfully
                selectedImageUri = userProfilePicUri;
                Glide.with(UserSetupActivity.this)
                        .load(selectedImageUri)
                        .apply(RequestOptions.circleCropTransform())
                        .into(profilePic);

                inputUsername.setText(username);
            }

        } else
            // Intent is null, handle the error condition
            AppUtils.switchActivity(UserSetupActivity.this, MainActivity.class, "rtl");


        // Profile picture click listener
        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImagePicker.with(UserSetupActivity.this).cropSquare().compress(512).maxResultSize(512, 512).
                        createIntent(new Function1<Intent, Unit>(){
                            @Override
                            public Unit invoke(Intent intent){
                                imagePickLauncher.launch(intent);
                                return null;
                            }
                        });
            }
        });


        // RadioGroup listener to handle selected choice
        toggleRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton selectedRadioButton = findViewById(checkedId);
                if (selectedRadioButton != null)
                    selectedRadioGroupChoice = selectedRadioButton.getText().toString();
            }
        });

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

                btnDone.setEnabled(isUsernameValid && isGenderSelected && isBirthDateSelected);
            }

            private void updateCalendar() {
                String format = "dd/MM/yyyy";
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
                inputBirthDate.setText(sdf.format(calendar.getTime()));
            }
        };

        datePickerDialog = new DatePickerDialog(UserSetupActivity.this,
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

        // Gender spinner setup
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
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

                btnDone.setEnabled(isUsernameValid && isGenderSelected && isBirthDateSelected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

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
                boolean isUsernameValid = !editable.toString().trim().isEmpty()
                        && editable.toString().trim().length() <= MAX_USERNAME_LENGTH
                        && editable.toString().trim().matches("^(?=.*[a-zA-Z0-9])[a-zA-Z0-9 ]+$");
                boolean isGenderSelected = spinnerGender.getSelectedItemPosition() > 0;
                boolean isBirthDateSelected = !inputBirthDate.getText().toString().isEmpty();

                btnDone.setEnabled(isUsernameValid && isGenderSelected && isBirthDateSelected);
            }
        });

        // Done button click listener
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserDataToFirebase();

                // After saving user data, start the UserHomeActivity
                AppUtils.switchActivity(UserSetupActivity.this, UserHomeActivity.class, "ltr");
                finish();
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
            String gender = getResources().getStringArray(R.array.genders)[spinnerGender.getSelectedItemPosition()];
            String birthDate = inputBirthDate.getText().toString().trim();
            String bio = inputBio.getText().toString().trim();

            // Create a map to store user data
            Map<String, Object> userData = new HashMap<>();
            userData.put("language", "en");
            userData.put("username", username);
            if(!bio.isEmpty()) userData.put("bio", bio); else userData.put("bio", "");
            userData.put("gender", gender);
            userData.put("birthDate", birthDate);
            userData.put("type", selectedRadioGroupChoice.trim());
            if(selectedImageUri != null) uploadImage(selectedImageUri, userId, "profilePic");

            // Set the document name as the user ID
            DocumentReference userDocumentRef = db.collection("users").document(userId);

            // Set the data to the Firestore document
            userDocumentRef.set(userData).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("DATABASE","DocumentSnapshot added with ID: " + userId);
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
     * Upload the user profile image to Firebase Storage.
     */
    private void uploadImage(Uri imageUri ,String userId, String imageType){
        StorageReference imageRef = storageReference.child("images/" + userId + "/" + imageType);

        imageRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d("STORAGE","Image added to the user " + userId + "as " + imageType);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("STORAGE","Error adding image to the user " + userId + "as " + imageType);
            }
        });
    }
}
