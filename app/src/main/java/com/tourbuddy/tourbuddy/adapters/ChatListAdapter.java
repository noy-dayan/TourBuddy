package com.tourbuddy.tourbuddy.adapters;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.fragments.ChatFragment;
import com.tourbuddy.tourbuddy.fragments.OtherProfileFragment;
import com.tourbuddy.tourbuddy.utils.AppUtils;
import com.tourbuddy.tourbuddy.utils.Chat;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder> {
    List<Chat> chatList;
    OnChatItemClickListener listener;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();
    String userId;
    Activity activity;

    // Constructor to initialize the adapter with a list of chats
    public ChatListAdapter(Activity activity, List<Chat> chatList, OnChatItemClickListener listener) {
        this.chatList = chatList;
        this.listener = listener;
        this.activity = activity;
        userId = mAuth.getCurrentUser().getUid();
        sortChatListByTimestamp();
    }

    private void sortChatListByTimestamp() {
        Collections.sort(chatList, (chat1, chat2) -> {
            // Sort in descending order
            return chat2.getTimeStamp().compareTo(chat1.getTimeStamp());
        });
    }
    public List<Chat> getChats() {
        return chatList;
    }

    // ViewHolder class to hold references to views in chat item layout
    public static class ChatListViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewChatTitle, textViewTimeStamp, textViewLastMessage;
        ImageView profilePic;


        public ChatListViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewChatTitle = itemView.findViewById(R.id.textViewChatTitle);
            textViewTimeStamp = itemView.findViewById(R.id.textViewTimeStamp);
            textViewLastMessage = itemView.findViewById(R.id.lastMessage);
            profilePic = itemView.findViewById(R.id.profilePic);


        }
    }

    @NonNull
    @Override
    public ChatListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the chat item layout and create ViewHolder
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_chat_item, parent, false);
        return new ChatListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatListViewHolder holder, int position) {
        // Bind chat data to views in ViewHolder
        Chat chat = chatList.get(position);
        holder.textViewChatTitle.setText(chat.getOtherUserName());

        // Format the timestamp
        Timestamp timestamp = chat.getTimeStamp();
        Date date = new Date(timestamp.getSeconds() * 1000); // Convert seconds to milliseconds
        SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm dd MMMM yyyy", Locale.getDefault()); // Corrected the time format to hh:mm
        String formattedDate = dateFormat.format(date);

        holder.textViewTimeStamp.setText(formattedDate);
        if (Objects.equals(chat.getLastMessage(), "null"))
            holder.textViewLastMessage.setText("");
        else
            holder.textViewLastMessage.setText(chat.getLastMessage());

        String otherUserId = "";
        List<String> participants = chat.getParticipants();
        for(String participantId : participants)
            if(!Objects.equals(participantId, userId))
                otherUserId = participantId;

        String finalOtherUserId = otherUserId;
        holder.profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    OtherProfileFragment otherProfileFragment = new OtherProfileFragment();
                    Bundle args = new Bundle();
                    args.putString("userId", finalOtherUserId);
                    otherProfileFragment.setArguments(args);

                    // Call the non-static switchFragment method on the UserRecyclerViewAdapter instance
                    switchFragment(otherProfileFragment);

            }
        });


        storageReference.child("images/" + otherUserId + "/profilePic").getDownloadUrl()
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Glide.with(holder.itemView.getContext())
                                .load(R.drawable.profilepic_default)
                                .into(holder.profilePic);
                        Log.e("Glide", "Error loading profileImage", exception);
                    }
                }).addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Glide.with(holder.itemView.getContext())
                                .load(uri)
                                .apply(RequestOptions.circleCropTransform())
                                .into(holder.profilePic);
                        Log.d("Glide", "Success loading profileImage");

                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        // Set click listener for the chat item
                        holder.itemView.setOnClickListener(v -> {
                            if (listener != null) {
                                listener.onChatItemClick(chat);
                            }
                        });
                    }
                });
    }


    @Override
    public int getItemCount() {
        return chatList.size();
    }

    private void switchFragment(Fragment fragment) {
        if (activity instanceof AppCompatActivity) {
            ((AppCompatActivity) activity).getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
    // Interface for handling click events on chat items
    public interface OnChatItemClickListener {
        void onChatItemClick(Chat chat);

    }
}
