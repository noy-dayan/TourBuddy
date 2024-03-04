package com.tourbuddy.tourbuddy.fragments;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.adapters.MessageListAdapter;
//import com.tourbuddy.tourbuddy.managers.NamesManager;
import com.tourbuddy.tourbuddy.utils.Chat;
import com.tourbuddy.tourbuddy.utils.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class ChatFragment extends Fragment {
    private RecyclerView recyclerView;
    private MessageListAdapter adapter;
    private List<Message> messageList;
    private String userId;
    private FirebaseFirestore db;
    private Chat chat;
    private EditText editTextMessage;
    private Button sendButton;


    //private NamesManager namesManagerInstance;
    private String chatId;

    // Constructor to accept Chat object
    public ChatFragment(Chat chat) {
        this.chat = chat;

    }

    // Static method to create a new instance of ChatFragment with Chat object
    public static ChatFragment newInstance(Chat chat) {
        return new ChatFragment(chat);
    }




    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get chat ID from arguments or wherever you store it
        if (getArguments() != null) {
            chatId = getArguments().getString("chatId");
        }
        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);
        recyclerView = rootView.findViewById(R.id.recycler_view_messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        messageList = new ArrayList<>();
        adapter = new MessageListAdapter(messageList);
        recyclerView.setAdapter(adapter);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        // Initialize EditText and Button
        editTextMessage = rootView.findViewById(R.id.edittext_chat_message);
        sendButton = rootView.findViewById(R.id.sendBTN);

        // Set OnClickListener for the send button
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        return rootView;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Fetch messages for the chat
        fetchMessages();
    }

    private void fetchMessages() {
        db.collection("chats").document(chat.getChatId()).collection("messages")
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        // Convert each document to a Message object and add it to the list
                        Message message = document.toObject(Message.class);
                        messageList.add(message);
                        message.setSenderName(chat.getParticipantName(message.getSenderId()));
                    }

                    // Sort the messageList based on timestamp
                    Collections.sort(messageList, new Comparator<Message>() {
                        @Override
                        public int compare(Message message1, Message message2) {
                            return message1.getTimestamp().compareTo(message2.getTimestamp());
                        }
                    });

                    adapter.notifyDataSetChanged(); // Notify adapter of changes
                    recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                })
                .addOnFailureListener(e -> {
                    // Handle error
                });
    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString().trim();
        if (!messageText.isEmpty()) {
            // Create a new Message object with senderId, content, and timestamp
            Message message = new Message();
            message.setSenderId(userId); // Set the senderId here
            message.setContent(messageText);
            message.setTimestamp(new Date()); // Set the current timestamp
            // Add the message to the messageList
            messageList.add(message);
            // Notify the adapter of changes
            adapter.notifyDataSetChanged();
            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            // Clear the EditText
            editTextMessage.setText("");
            // Optionally, send the message to Firestore here
            sendMessageToFirestore(message);
        }
    }
    private void sendMessageToFirestore(Message message) {
        // Get a reference to the Firestore collection for this chat
        CollectionReference chatMessagesRef = db.collection("chats").document(chat.getChatId()).collection("messages");

        // Add the message to Firestore
        chatMessagesRef.add(message)
                .addOnSuccessListener(documentReference -> {
                    // Message added successfully
                    // You can handle any additional logic here if needed
                })
                .addOnFailureListener(e -> {
                    // Error occurred while adding the message
                    // You can handle the error here
                });
    }

}
