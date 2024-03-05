package com.tourbuddy.tourbuddy.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.LocaleList;
import android.widget.EditText;

import androidx.fragment.app.Fragment;

import com.tourbuddy.tourbuddy.R;

import java.util.Locale;
import java.util.Objects;

/**
 * AppUtils contains utility methods for handling general components of the application.
 */
public class AppUtils {

    /**
     * Checks if an EditText component is filled with text.
     *
     * @param editText The EditText component to check.
     * @return True if the EditText is filled with text, false otherwise.
     */
    public static boolean isEditTextFilled(EditText editText) {
        // Checks if the EditText's text is not null and has a length greater than 0
        return editText.getText() != null && editText.getText().length() > 0;
    }

    /**
     * Checks if the text in an EditText is a positive numeric value.
     *
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

    /**
     * Method to transition to the next activity with clearing task and adding flags
     *
     * @param currentActivity The current activity from which the transition is initiated.
     * @param targetActivity  The target activity to which the transition will be made.
     * @param anim            The animation type for the transition. Can be "rtl" (right to left) or "ltr" (left to right).
     */
    public static void switchActivity(Activity currentActivity, Class<?> targetActivity, String anim) {
        Intent intent = new Intent(currentActivity, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        currentActivity.startActivity(intent);

        if ("rtl".equals(anim))
            currentActivity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        else if ("ltr".equals(anim))
            currentActivity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /**
     * Method to transition to the next activity with clearing task and adding flags
     *
     * @param currentActivity The current activity from which the transition is initiated.
     * @param intent          The intent to launch the target activity.
     * @param anim            The animation type for the transition. Can be "rtl" (right to left) or "ltr" (left to right).
     */
    public static void switchActivity(Activity currentActivity, Intent intent, String anim) {
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        currentActivity.startActivity(intent);

        if ("rtl".equals(anim))
            currentActivity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        else if ("ltr".equals(anim))
            currentActivity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /**
     * Method to set the app locale based on the language code.
     *
     * @param context The context (Activity or Fragment) from which the method is called.
     * @param languageCode The language code to set the locale.
     */
    public static void setAppLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration configuration = new Configuration();
        configuration.locale = locale;

        context.getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());
    }

    /**
     * Method to switch to the next fragment.
     *
     * @param currentFragment The current fragment from which the switch is initiated.
     * @param targetFragment  The target fragment to which the switch will be made.
     */
    public static void switchFragment(Fragment currentFragment, Fragment targetFragment) {
        if (currentFragment.getActivity() != null && currentFragment.isAdded()) {
            currentFragment.getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frameLayout, targetFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    public static String getSubstringFromCountry(String country) {
        // Assuming the country has the format: "Country (AB)"
        int startIndex = country.lastIndexOf('(');
        int endIndex = country.lastIndexOf(')');

        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            // Extract the substring between '(' and ')'
            return country.substring(startIndex + 1, endIndex);
        } else {
            // Return the original country if the format is not as expected
            return country;
        }
    }
}
