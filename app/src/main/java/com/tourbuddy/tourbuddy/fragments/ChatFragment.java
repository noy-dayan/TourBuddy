package com.tourbuddy.tourbuddy.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
import com.tourbuddy.tourbuddy.activities.MainActivity;
import com.tourbuddy.tourbuddy.adapters.ChatMessageListAdapter;
import com.tourbuddy.tourbuddy.utils.AppUtils;
import com.tourbuddy.tourbuddy.utils.Chat;
import com.tourbuddy.tourbuddy.utils.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class ChatFragment extends Fragment {
    RecyclerView recyclerView;
    ChatMessageListAdapter adapter;
    List<Message> messageList;
    String userId;
    FirebaseFirestore db;
    Chat chat;
    EditText editTextMessage;
    String chatId;
    ImageView btnBack, btnSend;

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
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        if (!isAdded())
            return view;

        recyclerView = view.findViewById(R.id.recycler_view_messages);
        editTextMessage = view.findViewById(R.id.edittext_chat_message);
        btnSend = view.findViewById(R.id.btnSend);
        btnBack = view.findViewById(R.id.btnBack);


        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        messageList = new ArrayList<>();
        adapter = new ChatMessageListAdapter(requireActivity(), messageList);
        recyclerView.setAdapter(adapter);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        // Add OnClickListener to btnBack
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to previous fragment
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
        editTextMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty())
                    // Text is empty
                    btnSend.setColorFilter(ContextCompat.getColor(requireContext(), R.color.grey)); // Change color back to default
                else
                    // Text is not empty
                    btnSend.setColorFilter(ContextCompat.getColor(requireContext(), R.color.orange_primary)); // Change color to active color

            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        messageList.clear();
        fetchMessages(); // Fetch initial messages and set up real-time listener
        return view;
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
                messageList.sort(new Comparator<Message>() {
                    @Override
                    public int compare(Message message1, Message message2) {
                        int minuteComparison = message1.getTimestamp().compareTo(message2.getTimestamp());
                        if (minuteComparison != 0) {
                            return minuteComparison;
                        } else {
                            // If timestamps have the same minute, compare by seconds
                            return message1.getTimestamp().getSeconds() - message2.getTimestamp().getSeconds();
                        }
                    }
                });
                adapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("DATABASE", "Error fetching messages", e);
            }
        });

        messagesRef.orderBy("timestamp").addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null) {
                Log.e("DATABASE", "Listen failed", e);
                return;
            }
            for (DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                switch (dc.getType()) {
                    case ADDED:
                        // Only add the message if it's not already in the messageList
                        Message newMessage = dc.getDocument().toObject(Message.class);
                        boolean messageExists = false;
                        for (Message existingMessage : messageList) {
                            if (existingMessage.getMessageId().equals(newMessage.getMessageId())) {
                                messageExists = true;
                                break;
                            }
                        }
                        if (!messageExists) {
                            newMessage.setSenderName(chat.getParticipantName(newMessage.getSenderId()));
                            messageList.add(newMessage);
                            adapter.notifyItemInserted(messageList.size() - 1);
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        }
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
