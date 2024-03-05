package com.tourbuddy.tourbuddy.adapters;


import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tourbuddy.tourbuddy.R;

import java.util.concurrent.CountDownLatch;
import java.util.ArrayList;
import java.util.List;

public class chatCard {
    private static FirebaseUser mUser;
    private static FirebaseFirestore db;
    private static  String userId;

    private int image;
    private String chatId;

    private String user1Id,user2Id;
    private String chatName, lastMassage,
            timeStamp;
    private CountDownLatch latch;
    private DocumentReference chatDocumentRef;
    static {
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        userId = mUser.getUid();
        db = FirebaseFirestore.getInstance();
    }

    public chatCard(String chatId) {
        this.chatId = chatId;
        this.db = FirebaseFirestore.getInstance();
        fetchChatDetails();
        this.chatName = "chatName";
        this.lastMassage = "lastMassage";
        this.timeStamp = "00:00";
        this.chatId = chatId;
    }

    public chatCard copy()
    {
        return new chatCard(new String(this.chatId));
    }


    public int getImage() {
        return image;
    }

    public void setImage() {

    }

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {

    }

    public String getLastMassage() {
        return lastMassage;
    }

    public void setLastMassage(String lastMassage) {
        this.lastMassage = lastMassage;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getUser1Id() {
        return user1Id;
    }

    public void setUser1Id(String user1Id) {



        this.user1Id = user1Id;
    }

    public String getUser2Id() {

        return user2Id;
    }

    public void setUser2Id(String user2Id) {
        this.user2Id = user2Id;
    }

    private void fetchChatDetails() {
        DocumentReference chatRef = db.collection("chats").document(chatId);
        chatRef.get().addOnSuccessListener(documentSnapshot -> {
            List<String> members = (List<String>) documentSnapshot.get("members");
            if (members != null && members.size() == 2) {
                String memberId1 = members.get(0);
                String memberId2 = members.get(1);
                if (mUser.getUid().equals(memberId1)) {
                    user1Id = memberId1;
                    user2Id = memberId2;
                } else {
                    user1Id = memberId2;
                    user2Id = memberId1;
                }
                fetchUsername(user1Id);
            }
        }).addOnFailureListener(e -> {
            // Handle failure
        });
    }

    private void fetchUsername(String userId) {
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.contains("username")) {
                chatName = documentSnapshot.getString("username");
                // Update UI or notify listeners here
            }
        }).addOnFailureListener(e -> {
            // Handle failure
        });
    }

}
