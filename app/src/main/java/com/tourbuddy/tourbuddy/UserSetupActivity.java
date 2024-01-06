package com.tourbuddy.tourbuddy;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class UserSetupActivity extends AppCompatActivity {
    Spinner spinnerGender;
    Button btnContinue;
    EditText inputUsername;
    final short MAX_USERNAME_LENGTH = 16;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_setup);

        spinnerGender = findViewById(R.id.spinnerGender);
        btnContinue = findViewById(R.id.btnContinue);
        inputUsername = findViewById(R.id.inputUsername);

        // Use the array resource for gender options
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.gender_options,
                R.layout.custom_spinner  // Set the custom layout for regular items
        );

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item); // Set the custom layout for dropdown items

        // Apply the adapter to the spinner
        spinnerGender.setAdapter(adapter);

        // Handle item selection
        spinnerGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // Handle the selected gender here
                String selectedGender = getResources().getStringArray(R.array.gender_options)[position];

                // Enable the button if both conditions are met
                btnContinue.setEnabled(!inputUsername.getText().toString().isEmpty() && inputUsername.length() <= 16 && position > 0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing if nothing is selected
            }
        });

        inputUsername.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_USERNAME_LENGTH)});
        // Add TextWatcher to enable/disable the button based on username input
        inputUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Enable the button if the username input is not empty
                boolean isUsernameValid = !editable.toString().isEmpty() && editable.length() <= 16;

                // Check if a gender is selected
                boolean isGenderSelected = spinnerGender.getSelectedItemPosition() > 0;

                // Enable the button if both conditions are met
                btnContinue.setEnabled(isUsernameValid && isGenderSelected);
            }
        });
    }
}
