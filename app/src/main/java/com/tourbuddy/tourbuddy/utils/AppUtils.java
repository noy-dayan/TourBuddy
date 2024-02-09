package com.tourbuddy.tourbuddy.utils;

import android.widget.EditText;

/**
 * AppUtils contains utility methods for handling general components of the application.
 */
public class AppUtils {

    /**
     * Checks if an EditText component is filled with text.
     * @param editText The EditText component to check.
     * @return True if the EditText is filled with text, false otherwise.
     */
    public static boolean isEditTextFilled(EditText editText) {
        // Checks if the EditText's text is not null and has a length greater than 0
        return editText.getText() != null && editText.getText().length() > 0;
    }

    /**
     * Checks if the text in an EditText is a positive numeric value.
     * @param editText The EditText component to check.
     * @return True if the text in the EditText is a positive numeric value, false otherwise.
     */
    public static boolean isPositiveNumeric(EditText editText) {
        // Retrieves the text from the EditText, removes leading and trailing whitespaces
        String text = editText.getText().toString().trim();
        try {
            // Attempts to parse the text as an integer
            int number = Integer.parseInt(text);
            // Checks if the parsed number is greater than 0
            return number > 0; // Returns true if the input is a positive numeric value
        } catch (NumberFormatException e) {
            // Catches the exception thrown if the input is not a valid integer
            return false; // Returns false if the input is not a valid integer
        }
    }
}
