package com.tourbuddy.tourbuddy.managers;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.tourbuddy.tourbuddy.utils.Chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FirebaseManager {

    private static final String TAG = "FirebaseManager";
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private String userId;

    public FirebaseManager() {
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        }
    }

    public String getUserId() {
        return userId;
    }

    public interface OnChatsAndUsernamesFetchListener {
        void onChatsAndUsernamesFetched(List<Chat> chats, Map<String, String> userIdToUsernameMap);
        void onChatsAndUsernamesFetchFailed(Exception e);
    }

    public void fetchChatsAndUsernames(final OnChatsAndUsernamesFetchListener listener) {
        fetchChats(new OnChatsFetchListener() {
            @Override
            public void onChatsFetched(List<Chat> chats) {
                List<String> userIds = extractUserIdsFromChats(chats);
                fetchUsernames(userIds, new OnUsernamesFetchListener() {
                    @Override
                    public void onUsernamesFetched(Map<String, String> userIdToUsernameMap) {
                        listener.onChatsAndUsernamesFetched(chats, userIdToUsernameMap);
                    }

                    @Override
                    public void onUsernamesFetchFailed(Exception e) {
                        listener.onChatsAndUsernamesFetchFailed(e);
                    }
                }, chats);

            }

            @Override
            public void onChatsFetchFailed(Exception e) {
                listener.onChatsAndUsernamesFetchFailed(e);
            }
        });
    }

    private void fetchChats(final OnChatsFetchListener listener) {
        mFirestore.collection("chats")
                .whereArrayContains("participants", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Chat> chats = processChats(task.getResult().getDocuments());
                        fetchLastMessages(chats, listener); // Fetch last messages for each chat
                    } else {
                        listener.onChatsFetchFailed(task.getException());
                        Log.e(TAG, "Error fetching chats: ", task.getException());
                    }
                });
    }

    private List<Chat> processChats(List<DocumentSnapshot> documents) {
        List<Chat> chats = new ArrayList<>();
        for (DocumentSnapshot document : documents) {
            Chat chat = document.toObject(Chat.class);
            if (chat != null) {
                chats.add(chat);
            } else {
                Log.e(TAG, "Error converting Firestore document to Chat object: " + document.getId());
            }
        }
        return chats;
    }

    private List<String> extractUserIdsFromChats(List<Chat> chats) {
        List<String> userIds = new ArrayList<>();
        for (Chat chat : chats) {
            for (String participant : chat.getParticipants()) {
                if (!userIds.contains(participant)) { // Check if participant is not already in the list
                    userIds.add(participant);
                }
            }
        }
        return userIds;
    }

    private void fetchLastMessages(List<Chat> chats, OnChatsFetchListener listener) {
        for (Chat chat : chats) {
            mFirestore.collection("chats")
                    .document(chat.getChatId())
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            // Retrieve the last message document
                            DocumentSnapshot lastMessageDoc = task.getResult().getDocuments().get(0);
                            // Get the message content
                            String lastMessage = lastMessageDoc.getString("content");
                            // Update the chat object with the last message
                            chat.setLastMessage(lastMessage);
                            // Notify the listener when all last messages are fetched
                            if (chats.indexOf(chat) == chats.size() - 1) {
                                listener.onChatsFetched(chats);
                            }
                        } else {
                            // Handle failure to fetch last message
                            Log.e(TAG, "Error fetching last message for chat: " + chat.getChatId());
                            listener.onChatsFetchFailed(task.getException());
                        }
                    });
        }
    }


    private void fetchUsernames(List<String> userIds, final OnUsernamesFetchListener listener, List<Chat> chats) {
        Map<String, String> userIdToUsernameMap = new HashMap<>(); // Initialize the map to hold userIds and usernames
        for (String userId : userIds) {
            mFirestore.collection("users")
                    .document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            String username = document.getString("username");
                            if (username != null) {
                                userIdToUsernameMap.put(userId, username); // Add userId and username to the map
                                // Add username to participantsNames of each chat containing userId
                                for (Chat chat : chats) {
                                    if (chat.getParticipants().contains(userId)) {
                                        chat.addParticipantName(userId, username);
                                    }
                                }
                            }
                            // Check if all usernames have been fetched
                            if (userIdToUsernameMap.size() == userIds.size()) { // Check if all usernames have been fetched
                                listener.onUsernamesFetched(userIdToUsernameMap); // Pass userIdToUsernameMap to the listener
                            }
                        } else {
                            Log.e(TAG, "Error fetching username for userId: " + userId);
                            listener.onUsernamesFetchFailed(task.getException());
                        }
                    });
        }
    }



    public interface OnChatsFetchListener {
        void onChatsFetched(List<Chat> chats);
        void onChatsFetchFailed(Exception e);
    }

    public interface OnUsernamesFetchListener {
        void onUsernamesFetched(Map<String, String> userIdToUsernameMap);
        void onUsernamesFetchFailed(Exception e);
    }


}
