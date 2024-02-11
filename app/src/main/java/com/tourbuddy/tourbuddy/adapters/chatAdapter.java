package com.tourbuddy.tourbuddy.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tourbuddy.tourbuddy.R;

import java.util.ArrayList;
import java.util.List;

public class chatAdapter extends RecyclerView.Adapter<chatAdapter.chatHolder> {
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore db;
    StorageReference storageReference;

    TextView messages;

    static String userId;


    private List<chatCard> chatList;

    public chatAdapter(List<chatCard> chatList) {
        this.chatList = chatList;
    }

    @NonNull
    @Override
    public chatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View chatView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_item,parent,false);
        return new chatHolder(chatView);
    }

    @Override
    public void onBindViewHolder(@NonNull chatHolder holder, int position) {

        chatCard card = chatList.get(position);

        holder.imageView.setImageResource(R.drawable.profilepic_default);
        holder.timeStamp.setText(card.getTimeStamp());
        holder.lastMassage.setText(card.getLastMassage());
        holder.chatName.setText(card.getChatName());
    }



    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class chatHolder extends RecyclerView.ViewHolder{
        DocumentReference db;
        ImageView imageView;
        TextView chatName, lastMassage, timeStamp;
        public chatHolder(@NonNull View itemView){
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            chatName = itemView.findViewById(R.id.titleMassage);
            lastMassage = itemView.findViewById(R.id.lastMassage);
            timeStamp = itemView.findViewById(R.id.timeStamp);
        }

        public void setImageView(ImageView imageView) {
            this.imageView = imageView;
        }

        public void setChatName(TextView chatName) {
            this.chatName = chatName;
        }

        public void setLastMassage(TextView lastMassage) {
            this.lastMassage = lastMassage;
        }

        public void setTimeStamp(TextView timeStamp) {
            this.timeStamp = timeStamp;
        }
    }



}
