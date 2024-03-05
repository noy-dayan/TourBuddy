package com.tourbuddy.tourbuddy.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.utils.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageListAdapter extends RecyclerView.Adapter<MessageListAdapter.ViewHolder> {
    private static final int MESSAGE_TYPE_ME = 1;
    private static final int MESSAGE_TYPE_OTHER = 0;
    private static final int VIEW_TYPE_DATE = 2;

    private List<Message> messageList;
    private List<Date> datesWithHeaders;

    public MessageListAdapter(List<Message> messageList) {
        this.messageList = messageList;
        this.datesWithHeaders = new ArrayList<>();
        generateDateHeaders();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId;
        if (viewType == VIEW_TYPE_DATE) {
            layoutId = R.layout.item_chat_date;
        } else {
            layoutId = viewType == MESSAGE_TYPE_ME ? R.layout.item_chat_message_right :
                    R.layout.item_chat_message_left;
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
        private TextView senderTextView;
        private TextView contentTextView;
        private TextView timeStampTextView;
        private TextView dateHeaderTextView;

        public ViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            if (viewType == MESSAGE_TYPE_ME) {
                contentTextView = itemView.findViewById(R.id.text_gchat_message_me);
                timeStampTextView = itemView.findViewById(R.id.text_gchat_timestamp_me);
            } else if (viewType == MESSAGE_TYPE_OTHER) {
                senderTextView = itemView.findViewById(R.id.text_gchat_user_other);
                contentTextView = itemView.findViewById(R.id.text_gchat_message_other);
                timeStampTextView = itemView.findViewById(R.id.text_gchat_timestamp_other);
            } else {
                dateHeaderTextView = itemView.findViewById(R.id.text_gchat_date);
            }
        }

        public void bindMessage(Message message) {
            if (senderTextView != null) {
                senderTextView.setText(message.getSenderName());
            }
            contentTextView.setText(message.getContent());
            timeStampTextView.setText(formatTimeStamp(message.getTimestamp()));
        }

        public void bindDateHeader(Date date) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            dateHeaderTextView.setText(dateFormat.format(date));
        }

        private String formatTimeStamp(Date timestamp) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return timeFormat.format(timestamp);
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

    private boolean areDatesSameDay(Date date1, Date date2) {
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
