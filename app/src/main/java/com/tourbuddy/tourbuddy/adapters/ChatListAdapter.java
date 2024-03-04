package com.tourbuddy.tourbuddy.adapters;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.tourbuddy.tourbuddy.R;
//import com.tourbuddy.tourbuddy.managers.NamesManager;
import com.tourbuddy.tourbuddy.utils.Chat;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder> {
    private List<Chat> chatList;
//    private Context context;
    private OnChatItemClickListener listener;

    //private NamesManager namesManagerInstance;
    private String userId;

    // Constructor to initialize the adapter with a list of chats
    public ChatListAdapter(List<Chat> chatList, OnChatItemClickListener listener) {
        this.chatList = chatList;
        this.listener = listener;
        //namesManagerInstance = NamesManager.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
//        preprocessChatData();
    }

    //private void preprocessChatData() {
//        // Pre-process chat data here
//        for (Chat chat : chatList) {
//            for (String participant : chat.getParticipants()) {
//                String userName = namesManagerInstance.addUser(participant);
//                if (!userId.equals(participant)) {
//                    chat.setOtherUserName(userName);
//                }
//            }
//        }
//    }

    public List<Chat> getChats() {
        return chatList;
    }

    // ViewHolder class to hold references to views in chat item layout
    public static class ChatListViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewChatTitle;
        public TextView textViewTimeStamp;
        public TextView textViewLastMessage;

        public ChatListViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewChatTitle = itemView.findViewById(R.id.textViewChatTitle);
            textViewTimeStamp = itemView.findViewById(R.id.textViewTimeStamp);
            textViewLastMessage = itemView.findViewById(R.id.textViewLastMessage);
        }
    }

    @NonNull
    @Override
    public ChatListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the chat item layout and create ViewHolder
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item, parent, false);
        return new ChatListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatListViewHolder holder, int position) {
        // Bind chat data to views in ViewHolder
        Chat chat = chatList.get(position);
        holder.textViewChatTitle.setText(chat.getOtherUserName());
        holder.textViewTimeStamp.setText(chat.getTimeStamp().toString());
        holder.textViewLastMessage.setText(chat.getLastMessage());

        // Set click listener for the chat item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatItemClick(chat);

            }
        });
    }


    @Override
    public int getItemCount() {
        return chatList.size();
    }

    // Interface for handling click events on chat items
    public interface OnChatItemClickListener {
        void onChatItemClick(Chat chat);

    }
}
