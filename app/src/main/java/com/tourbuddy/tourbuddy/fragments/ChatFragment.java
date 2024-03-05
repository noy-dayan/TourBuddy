package com.tourbuddy.tourbuddy.fragments;

import android.os.Bundle;
import android.util.Log;
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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.adapters.MessageListAdapter;
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
    private static final String TAG = "ChatFragment";

    private String chatId;

    public ChatFragment(Chat chat) {
        this.chat = chat;
    }

    public static ChatFragment newInstance(Chat chat) {
        return new ChatFragment(chat);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            chatId = getArguments().getString("chatId");
        }
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
        editTextMessage = rootView.findViewById(R.id.edittext_chat_message);
        sendButton = rootView.findViewById(R.id.sendBTN);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        fetchMessages(); // Fetch initial messages and set up real-time listener
        return rootView;
    }

    private void fetchMessages() {
        CollectionReference messagesRef = db.collection("chats").document(chat.getChatId()).collection("messages");
        messagesRef.orderBy("timestamp").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                messageList.clear();
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    Message message = document.toObject(Message.class);
                    message.setSenderName(chat.getParticipantName(message.getSenderId()));
                    messageList.add(message);
                }
                Collections.sort(messageList, new Comparator<Message>() {
                    @Override
                    public int compare(Message message1, Message message2) {
                        return message1.getTimestamp().compareTo(message2.getTimestamp());
                    }
                });
                adapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Error fetching messages", e);
            }
        });

        messagesRef.orderBy("timestamp").addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed", e);
                return;
            }
            for (DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                switch (dc.getType()) {
                    case ADDED:
                        Message message = dc.getDocument().toObject(Message.class);
                        message.setSenderName(chat.getParticipantName(message.getSenderId()));
                        messageList.add(message);
                        adapter.notifyItemInserted(messageList.size() - 1);
                        recyclerView.scrollToPosition(messageList.size() - 1);
                        break;
                    case MODIFIED:
                        // Handle modified message
                        break;
                    case REMOVED:
                        // Handle removed message
                        break;
                }
            }
        });
    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString().trim();
        if (!messageText.isEmpty()) {
            Message message = new Message();
            message.setSenderId(userId);
            message.setContent(messageText);
            message.setTimestamp(new Date());
            adapter.notifyDataSetChanged();
            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            editTextMessage.setText("");
            sendMessageToFirestore(message);
        }
    }

    private void sendMessageToFirestore(Message message) {
        CollectionReference chatMessagesRef = db.collection("chats").document(chat.getChatId()).collection("messages");
        chatMessagesRef.add(message).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                // Message added successfully
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Error occurred while adding the message
            }
        });
    }
}
