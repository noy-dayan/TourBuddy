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
    private DocumentReference chatDocumentRef;
    static {
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        userId = mUser.getUid();
        db = FirebaseFirestore.getInstance();
    }

    public chatCard(String chatId) {
        this.chatDocumentRef = db.collection("chats").document(chatId);
        setUsers();
        this.chatName = "chatName";
        this.lastMassage = "lastMassage";
        this.timeStamp = "00:00";
        this.chatId = chatId;
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

    public String getUser2IdName() {
        return user2Id;
    }

    public void setUser2Id(String user2Id) {
        this.user2Id = user2Id;
    }

    private void setUsers(){
        List<String> members  = new ArrayList<String>();
        chatDocumentRef.get().addOnSuccessListener(
                new OnSuccessListener<DocumentSnapshot>(){
                    List<String> memberList = new ArrayList<String>();
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        memberList = (List<String>) documentSnapshot.get("members");
                        if (memberList.get(0).equals(user1Id))
                        {
                            setUser2Id(memberList.get(1));
                        }
                        else
                        {
                            setUser1Id(memberList.get(0));
                        }
                    }
                }
        );
    }

    private void getUserName(String userid)
    {
        String chatName="";
        DocumentReference userDocumentRef = db.collection("users").document(userid);
        userDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                       @Override
                                                       public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                           String name="";
                                                           if (documentSnapshot.contains("username"))
                                                               setChatName(documentSnapshot.getString("username"));
                                                       }
                                                   }
        ).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

}
