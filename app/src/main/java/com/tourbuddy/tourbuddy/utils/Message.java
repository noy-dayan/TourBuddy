package com.tourbuddy.tourbuddy.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentId;
//import com.tourbuddy.tourbuddy.managers.NamesManager;

import java.util.Date;
import java.util.Dictionary;

public class Message {
    @DocumentId
    private String messageId;
    private String senderId;
    private String content;
    private Date timestamp;
    private String senderName;

    private boolean sentByCurrentUser;

    // Empty constructor needed for Firestore
    public Message() {}

    public Message(String messageId, String senderId, String content, Date timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setSentByCurrentUser(boolean sentByCurrentUser) {
        this.sentByCurrentUser = sentByCurrentUser;
    }
    public boolean isSentByCurrentUser() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getCurrentUser().getUid();
        if (userId.equals(senderId))
        {
            return true;
        }
        return false;
    }
}
