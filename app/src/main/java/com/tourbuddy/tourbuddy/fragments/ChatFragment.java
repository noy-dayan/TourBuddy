package com.tourbuddy.tourbuddy.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.adapters.chatCard;
import com.tourbuddy.tourbuddy.adapters.chatAdapter;
import com.tourbuddy.tourbuddy.R.drawable;


import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {
    RecyclerView recyclerView;
    List<chatCard> chatList;

    chatAdapter adapter;
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore db;
    StorageReference storageReference;

    String userId;
    View loadingOverlay;

    ProgressBar progressBar;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // Initialize loading overlay elements
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        progressBar = view.findViewById(R.id.progressBar);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(linearLayoutManager);


        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        userId = mUser.getUid();
        chatList = new ArrayList<>();


        showLoading(true);
        loadDataFromFirebase(chatList);



        adapter = new chatAdapter(chatList);
        recyclerView.setAdapter(adapter);

        return view;

    }
//    @Override
//    public View onUpdateView(){
//
//    }

    private void loadDataFromFirebase(List<chatCard> chatList) {
        DocumentReference userDocumentRef = db.collection("users").document(userId);
        userDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    if (!documentSnapshot.contains("chats")) {
                        return;
                    }

                    List<String> chatIds = (List<String>)documentSnapshot.get("chats");
                    List<Task<DocumentSnapshot>> tasks = new ArrayList<Task<DocumentSnapshot>>();

                    for (String chatId : chatIds) {
                        DocumentReference chatsCollectionRef = db.collection("chats").document(chatId);
                        tasks.add(chatsCollectionRef.get());
                        chatCard card = new chatCard(chatId);
                        chatList.add(card);
                    }
                    showLoading(false);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("DATABASE", "Error getting document", e);
            }
        });

    }

    private void showLoading(boolean show) {
        if (isAdded()) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}