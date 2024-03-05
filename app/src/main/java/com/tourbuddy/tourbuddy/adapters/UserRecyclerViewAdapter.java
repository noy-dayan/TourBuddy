package com.tourbuddy.tourbuddy.adapters;

import android.app.Activity;
import android.content.Context;
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
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.fragments.OtherProfileFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UserRecyclerViewAdapter extends RecyclerView.Adapter<UserRecyclerViewAdapter.ViewHolder> {

    List<String> userIdList;
    Activity activity;
    Context context;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();
    int loadedItemCount = 0; // Counter for loaded items
    List<ViewHolder> viewHolderList = new ArrayList<>(); // Initialize viewHolderList
    public UserRecyclerViewAdapter(Activity activity, Context context,
                                   List<String> userIdList) {
        this.activity = activity;
        this.context = context;
        this.userIdList = userIdList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_user_item_layout, parent, false);
        return new ViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.shimmerLayout.showShimmer(true);
        String userId = userIdList.get(position);
        viewHolderList.add(holder);
        loadDataFromFirebase(userId, holder);
    }

    @Override
    public int getItemCount() {
        return userIdList != null ? userIdList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profilePic;
        TextView username, type;
        UserRecyclerViewAdapter userRecyclerViewAdapter;
        ShimmerFrameLayout shimmerLayout;

        public ViewHolder(@NonNull View itemView, UserRecyclerViewAdapter userRecyclerViewAdapter) {
            super(itemView);
            this.userRecyclerViewAdapter = userRecyclerViewAdapter;
            profilePic = itemView.findViewById(R.id.userProfilePic);
            username = itemView.findViewById(R.id.username);
            type = itemView.findViewById(R.id.type);
            shimmerLayout = itemView.findViewById(R.id.shimmer_layout);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        String clickedUserId = userRecyclerViewAdapter.userIdList.get(position);

                        OtherProfileFragment otherProfileFragment = new OtherProfileFragment();
                        Bundle args = new Bundle();
                        args.putString("userId", clickedUserId);
                        otherProfileFragment.setArguments(args);

                        // Call the non-static switchFragment method on the UserRecyclerViewAdapter instance
                        userRecyclerViewAdapter.switchFragment(otherProfileFragment);
                        userRecyclerViewAdapter.userIdList.clear();
                    }
                }
            });
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

    private void loadDataFromFirebase(String userId, @NonNull ViewHolder holder) {
        if (userId != null) {
            holder.shimmerLayout.showShimmer(true);
            // Reset profile pic, username, and type to default or clear
            holder.profilePic.setImageResource(R.drawable.profilepic_default);
            holder.username.setText("▅▅▅▅▅▅▅▅▅▅▅▅");
            holder.type.setText("▅▅▅▅▅▅▅▅▅▅▅▅");

            DocumentReference userDocumentRef = db.collection("users").document(userId);
            userDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (context != null && documentSnapshot.exists()) {
                        holder.shimmerLayout.startShimmer();
                        storageReference.child("images/" + userId + "/profilePic").getDownloadUrl()
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
                                        if (documentSnapshot.contains("username"))
                                            holder.username.setText(documentSnapshot.getString("username"));

                                        if (documentSnapshot.contains("type")) {
                                            if (Objects.equals(documentSnapshot.getString("type"), "Tourist"))
                                                holder.type.setText(context.getResources().getString(R.string.tourist));
                                            else
                                                holder.type.setText(context.getResources().getString(R.string.tour_guide));
                                        }

                                        // Check if all items have finished loading
                                        holder.shimmerLayout.hideShimmer();

                                        // Increment the loadedItemCount
                                        loadedItemCount++;
                                    }
                                });
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DATABASE", "Error getting document", e);

                    // Increment the loadedItemCount
                    loadedItemCount++;
                    holder.shimmerLayout.startShimmer();

                }
            });
        }
    }
}
