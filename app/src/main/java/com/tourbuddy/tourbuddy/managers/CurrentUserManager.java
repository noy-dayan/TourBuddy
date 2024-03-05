package com.tourbuddy.tourbuddy.managers;

import android.app.Application;
import android.content.Context;

public class CurrentUserManager {
    private static CurrentUserManager instance;
    private String currentUserId;
    private String currentUserName;
    //TODO: set the name of the user

    private CurrentUserManager() {
        // Private constructor to prevent instantiation from outside
    }

    public static synchronized CurrentUserManager getInstance() {
        if (instance == null) {
            instance = new CurrentUserManager();
        }
        return instance;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }

}
