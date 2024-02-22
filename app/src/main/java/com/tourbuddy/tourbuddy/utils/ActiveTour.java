package com.tourbuddy.tourbuddy.utils;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.tourbuddy.tourbuddy.R;

import java.util.Date;

/**
 * ActiveTour represents a tour that is currently active or ongoing.
 */
public class ActiveTour {
    String startDate; // Start date of the tour
    String endDate; // End date of the tour
    DocumentReference tourPackageRef; // Reference to the tour package in Firestore
    int color; // Color associated with the tour

    /**
     * Constructor for creating an ActiveTour object.
     * @param startDate The start date of the tour.
     * @param endDate The end date of the tour.
     * @param tourPackageRef Reference to the tour package in Firestore.
     */
    public ActiveTour(String startDate, String endDate, DocumentReference tourPackageRef) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.tourPackageRef = tourPackageRef;
    }

    /**
     * Get the reference to the tour package in Firestore.
     * @return The DocumentReference object representing the tour package.
     */
    public DocumentReference getTourPackageRef() {
        return tourPackageRef;
    }

    /**
     * Get the start date of the tour.
     * @return The start date of the tour.
     */
    public String getStartDate() {
        return startDate;
    }

    /**
     * Get the end date of the tour.
     * @return The end date of the tour.
     */
    public String getEndDate() {
        return endDate;
    }

    /**
     * Set the color associated with the tour.
     * @param color The color value to set.
     */
    public void setColor(int color){
        this.color = color;
    }

    /**
     * Get the color associated with the tour.
     * @return The color value associated with the tour.
     */
    public int getColor(){
        return color;
    }
}
