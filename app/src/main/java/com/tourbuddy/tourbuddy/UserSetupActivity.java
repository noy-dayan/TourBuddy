package com.tourbuddy.tourbuddy;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class UserSetupActivity extends AppCompatActivity {

    Spinner spinnerGender;
    Button btnDone;
    EditText inputUsername, inputBirthDate, inputBio;
    ImageView profilePic;

    Calendar calendar;
    DatePickerDialog datePickerDialog;

    final short MAX_USERNAME_LENGTH = 16;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore db;
    FirebaseStorage storage;
    StorageReference storageReference;

    RadioGroup toggleRadioGroup;
    String selectedRadioGroupChoice = "Tourist";

    ActivityResultLauncher<Intent> imagePickLauncher;
    Uri selectedImageUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_setup);

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

                }
                );

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        profilePic = findViewById(R.id.profilePic);
        inputUsername = findViewById(R.id.inputUsername);
        inputBio = findViewById(R.id.inputBio);
        spinnerGender = findViewById(R.id.spinnerGender);
        inputBirthDate = findViewById(R.id.inputBirthDate);
        toggleRadioGroup = findViewById(R.id.toggleSwitch);

        calendar = Calendar.getInstance();

        btnDone = findViewById(R.id.btnDone);

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

        // Set a listener for the RadioGroup to handle the selected choice
        toggleRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // Find which RadioButton is selected
                RadioButton selectedRadioButton = findViewById(checkedId);
                if (selectedRadioButton != null)
                    // Get the text of the selected RadioButton
                    selectedRadioGroupChoice = selectedRadioButton.getText().toString();
            }
        });

        DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                updateCalendar();

                boolean isUsernameValid = !inputUsername.getText().toString().isEmpty() && inputUsername.length() <= 16;
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

        Calendar maxDateCalendar = Calendar.getInstance();
        maxDateCalendar.add(Calendar.YEAR, -16);
        datePickerDialog.getDatePicker().setMaxDate(maxDateCalendar.getTimeInMillis());

        Calendar minDateCalendar = Calendar.getInstance();
        minDateCalendar.add(Calendar.YEAR, -100);
        datePickerDialog.getDatePicker().setMinDate(minDateCalendar.getTimeInMillis());

        inputBirthDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePickerDialog.show();
            }
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.gender_options,
                R.layout.custom_spinner
        );

        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);

        spinnerGender.setAdapter(adapter);

        spinnerGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                boolean isUsernameValid = !inputUsername.getText().toString().isEmpty() && inputUsername.length() <= 16;
                boolean isGenderSelected = position > 0;
                boolean isBirthDateSelected = !inputBirthDate.getText().toString().isEmpty();

                btnDone.setEnabled(isUsernameValid && isGenderSelected && isBirthDateSelected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        inputUsername.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_USERNAME_LENGTH)});

        inputUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                boolean isUsernameValid = !editable.toString().isEmpty() && editable.length() <= 16;
                boolean isGenderSelected = spinnerGender.getSelectedItemPosition() > 0;
                boolean isBirthDateSelected = !inputBirthDate.getText().toString().isEmpty();

                btnDone.setEnabled(isUsernameValid && isGenderSelected && isBirthDateSelected);
            }
        });

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserDataToFirebase();

                // After saving user data, start the UserProfileActivity
                Intent userProfileIntent = new Intent(UserSetupActivity.this, UserProfileActivity.class);
                startActivity(userProfileIntent);
                finish(); // Optional: Close the current activity if needed
            }
        });
    }

    private void saveUserDataToFirebase() {
        mUser = mAuth.getCurrentUser();
        if (mUser != null) {
            String userId = mUser.getUid();
            String username = inputUsername.getText().toString().trim();
            String gender = getResources().getStringArray(R.array.gender_options)[spinnerGender.getSelectedItemPosition()];
            String birthDate = inputBirthDate.getText().toString().trim();
            String bio = inputBio.getText().toString().trim();

            // Create a map to store user data
            Map<String, Object> userData = new HashMap<>();
            userData.put("username", username);
            if(!bio.isEmpty()) userData.put("bio", bio);
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

    private String getFileExtension(Uri uri){
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

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