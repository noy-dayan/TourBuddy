package com.tourbuddy.tourbuddy.utils;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;



public class Chat {
//    private static FirebaseAuth mAuth = FirebaseAuth.getInstance();
//    private static FirebaseUser currentUser = mAuth.getCurrentUser();
//    private static String currUserId = currentUser.getUid();

    @DocumentId
    private String chatId;

    private String lastMessageId;
    private List<String> participants; // List of user IDs participating in the chat
    private String lastMessage;
    private Timestamp TimeStamp;

    private String otherUserName;
    private Dictionary<String, String> participantsNames = new Hashtable<>();

    public Chat() {
        // Default constructor required for Firestore serialization
    }

    public Chat(String chatId, List<String> participants,
                String lastMessage, Timestamp TimeStamp,
                String lastMessageId) {
        this.chatId = chatId;
        this.participants = new ArrayList<>();
        for (String participant : participants)
        {
            this.participants.add(new String(participant));
        }
        this.lastMessage = lastMessage;
        this.TimeStamp = TimeStamp;
        this.lastMessage = lastMessageId;
    }

    public Chat(Chat chat)
    {
        this.chatId = new String(chat.getChatId());
        this.TimeStamp = new Timestamp(chat.getTimeStamp().toDate());
        setParticipants(chat.getParticipants());
        this.lastMessage = new String(chat.getLastMessage());
    }


    public String getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(String lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    // Getters and setters
    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = new ArrayList<>();
        for (String participant : participants)
        {
            this.participants.add(new String(participant));
        }
    }

    public void addParticipantName(String id, String name){
        if (participantsNames.get(id) == null)
        {
            participantsNames.put(id,name);
        }
    }

    public String getParticipantName(String id)
    {
        if (participantsNames.get(id) == null)
        {
            return "null";
        }
        return participantsNames.get(id);
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setOtherUserName(String name)
    {
        otherUserName = name;
    }

    public String getOtherUserName()
    {
        return otherUserName;
    }

    public Timestamp getTimeStamp() {

        return TimeStamp;
    }

    public void setTimeStamp(Timestamp timeStamp) {
        this.TimeStamp = timeStamp;
    }

    // Method to get the title of the chat (e.g., concatenating participant usernames)
    public String getTitleChat() {
        // Here you can implement logic to create a title for the chat
        // For example, concatenate participant usernames
        StringBuilder titleBuilder = new StringBuilder();
        for (String participantId : participants) {
            // Retrieve username for participantId and append it to the titleBuilder
            // Assuming you have a method to get username by user ID
            // Replace getUsernameById with your actual method to get username
            String username = getUsernameById(participantId);
            if (username != null) {
                titleBuilder.append(username).append(", ");
            }
        }
        // Remove trailing comma and space
        if (titleBuilder.length() > 0) {
            titleBuilder.setLength(titleBuilder.length() - 2);
        }
        return titleBuilder.toString();
    }

    // Method to get username by user ID (Replace this with your actual implementation)
    private String getUsernameById(String userId) {
        // Assuming you have a method to get username by user ID
        // Replace this with your actual method to get username
        return "User " + userId;
    }

    public String getParticipantNames() {
        StringBuilder participantNames = new StringBuilder();
        for (int i = 0; i < participants.size(); i++) {
            participantNames.append(participants.get(i));
            if (i < participants.size() - 1) {
                participantNames.append(", ");
            }
        }
        return participantNames.toString();
    }


    public String getTitle() {
        //TODO
        return "title";
    }
}
