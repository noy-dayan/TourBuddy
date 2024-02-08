package com.tourbuddy.tourbuddy.utils;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.tourbuddy.tourbuddy.R;

import java.util.Date;

public class ActiveTour {
    String startDate;
    String endDate;
    DocumentReference  tourPackageRef;
    int color;

    public ActiveTour(String startDate, String endDate, DocumentReference tourPackageRef) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.tourPackageRef = tourPackageRef;
    }

    public DocumentReference getTourPackageRef() {
        return tourPackageRef;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setColor(int color){
        this.color = color;
    }

    public int getColor(){
        return color;
    }

}
