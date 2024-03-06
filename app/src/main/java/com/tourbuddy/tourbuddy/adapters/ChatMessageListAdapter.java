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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.fragments.OtherProfileFragment;
import com.tourbuddy.tourbuddy.utils.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageListAdapter extends RecyclerView.Adapter<ChatMessageListAdapter.ViewHolder> {
    static final int MESSAGE_TYPE_ME = 1;
    static final int MESSAGE_TYPE_OTHER = 0;
    static final int VIEW_TYPE_DATE = 2;

    List<Message> messageList;
    List<Date> datesWithHeaders;
    static Activity activity;
    public ChatMessageListAdapter(Activity activity, List<Message> messageList) {
        this.messageList = messageList;
        this.datesWithHeaders = new ArrayList<>();
        ChatMessageListAdapter.activity = activity;
        generateDateHeaders();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId;
        if (viewType == VIEW_TYPE_DATE) {
            layoutId = R.layout.cardview_chat_date_item_layout;
        } else {
            layoutId = viewType == MESSAGE_TYPE_ME ? R.layout.cardview_this_chat_message_item_layout :
                    R.layout.cardview_other_chat_message_item_layout;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_DATE) {
            // If the item is a date header, bind it
            holder.bindDateHeader(datesWithHeaders.get(position));
        } else {
            // Otherwise, bind the message
            Message message = messageList.get(position);
            holder.bindMessage(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        return message.isSentByCurrentUser() ? MESSAGE_TYPE_ME : MESSAGE_TYPE_OTHER;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView senderTextView, contentTextView, timeStampTextView, dateHeaderTextView;
        ImageView profilePic;

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();

        public ViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            if (viewType == MESSAGE_TYPE_ME) {
                contentTextView = itemView.findViewById(R.id.text_gchat_message_me);
                timeStampTextView = itemView.findViewById(R.id.text_gchat_timestamp_me);
            } else if (viewType == MESSAGE_TYPE_OTHER) {
                senderTextView = itemView.findViewById(R.id.usernameOther);
                contentTextView = itemView.findViewById(R.id.messageOther);
                timeStampTextView = itemView.findViewById(R.id.timestampOther);
                profilePic = itemView.findViewById(R.id.profilePicOther);

            } else {
                dateHeaderTextView = itemView.findViewById(R.id.text_gchat_date);
            }


        }

        public void bindMessage(Message message) {
            if (senderTextView != null)
                senderTextView.setText(message.getSenderName());
            if(profilePic != null) {

                storageReference.child("images/" + message.getSenderId() + "/profilePic").getDownloadUrl()
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                Glide.with(itemView.getContext())
                                        .load(R.drawable.profilepic_default)
                                        .into(profilePic);
                                Log.e("Glide", "Error loading profileImage", exception);
                            }
                        }).addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Glide.with(itemView.getContext())
                                        .load(uri)
                                        .apply(RequestOptions.circleCropTransform())
                                        .into(profilePic);
                                Log.d("Glide", "Success loading profileImage");

                            }
                        });
                profilePic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        OtherProfileFragment otherProfileFragment = new OtherProfileFragment();
                        Bundle args = new Bundle();
                        args.putString("userId", message.getSenderId());
                        otherProfileFragment.setArguments(args);
                        switchFragment(otherProfileFragment);
                    }
                });

            }
            contentTextView.setText(message.getContent());
            timeStampTextView.setText(formatTimeStamp(message.getTimestamp()));


        }

        public void bindDateHeader(Date date) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            dateHeaderTextView.setText(dateFormat.format(date));
        }

        private String formatTimeStamp(Date timestamp) {
            SimpleDateFormat todayFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat otherFormat = new SimpleDateFormat("dd MMMM HH:mm", Locale.getDefault());
            Date today = new Date();

            if (areDatesSameDay(timestamp, today)) {
                return todayFormat.format(timestamp);
            } else {
                return otherFormat.format(timestamp);
            }
        }

        private void switchFragment(Fragment fragment) {
            if (activity instanceof AppCompatActivity) {
                ((AppCompatActivity) activity).getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frameLayout, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        }
    }

    private void generateDateHeaders() {
        if (messageList.size() > 0) {
            Date currentDate = messageList.get(0).getTimestamp();
            datesWithHeaders.add(currentDate);

            for (int i = 1; i < messageList.size(); i++) {
                Date previousDate = messageList.get(i - 1).getTimestamp();
                currentDate = messageList.get(i).getTimestamp();

                if (!areDatesSameDay(previousDate, currentDate)) {
                    datesWithHeaders.add(currentDate);
                }
            }
        }
    }

    public static boolean areDatesSameDay(Date date1, Date date2) {
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MM", Locale.getDefault());
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());

        String day1 = dayFormat.format(date1);
        String month1 = monthFormat.format(date1);
        String year1 = yearFormat.format(date1);

        String day2 = dayFormat.format(date2);
        String month2 = monthFormat.format(date2);
        String year2 = yearFormat.format(date2);

        return day1.equals(day2) && month1.equals(month2) && year1.equals(year2);
    }
}
