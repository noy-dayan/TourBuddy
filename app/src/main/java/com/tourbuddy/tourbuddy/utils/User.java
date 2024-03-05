package com.tourbuddy.tourbuddy.utils;

public class User {
    private String userId;
    private String username;
    private String bio;
    private String birthDate;
    private String gender;
    private String language;
    private String type;

    // Empty constructor needed for Firestore
    public User() {}

    public User(String userId, String username, String bio, String birthDate, String gender, String language, String type) {
        this.userId = userId;
        this.username = username;
        this.bio = bio;
        this.birthDate = birthDate;
        this.gender = gender;
        this.language = language;
        this.type = type;
    }

    public User(User user) {
        //TODO create User copy constructor;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
