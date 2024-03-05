package com.tourbuddy.tourbuddy.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.utils.Message;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private List<Message> messages;

    // Constructor
    public ChatAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate your item layout and create ViewHolder
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Bind data to your ViewHolder
        Message message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        // Return the size of your dataset
        return messages.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // ViewHolder should contain views to display a single item
        private TextView messageTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.card_gchat_message_me);
        }

        public void bind(Message message) {
            // Bind data to views
            messageTextView.setText(message.getContent());
        }
    }
}
