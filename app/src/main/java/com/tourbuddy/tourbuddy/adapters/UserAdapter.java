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
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.fragments.OtherProfileFragment;

import java.util.List;
import java.util.Objects;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private List<String> userIdList;
    private Activity activity;
    private Context context;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageReference = storage.getReference();
    private String countryFilter, typeFilter, usernameFilter, genderFilter;
    private View loadingOverlay;
    private ProgressBar progressBar;
    private int loadedItemCount = 0; // Counter for loaded items

    public UserAdapter(Activity activity, Context context, View loadingOverlay, ProgressBar progressBar,
                       List<String> userIdList, String countryFilter, String typeFilter, String usernameFilter, String genderFilter) {
        this.activity = activity;
        this.context = context;
        this.userIdList = userIdList;
        this.countryFilter = countryFilter;
        this.genderFilter = genderFilter;
        this.typeFilter = typeFilter;
        this.usernameFilter = usernameFilter;
        this.loadingOverlay = loadingOverlay;
        this.progressBar = progressBar;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item_layout, parent, false);
        return new ViewHolder(view, userIdList, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String userId = userIdList.get(position);
        loadDataFromFirebase(userId, holder);
    }

    @Override
    public int getItemCount() {
        return userIdList != null ? userIdList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profilePic;
        TextView username, type;
        UserAdapter userAdapter;

        public ViewHolder(@NonNull View itemView, List<String> userIdList, UserAdapter userAdapter) {
            super(itemView);
            this.userAdapter = userAdapter;
            profilePic = itemView.findViewById(R.id.userProfilePic);
            username = itemView.findViewById(R.id.username);
            type = itemView.findViewById(R.id.type);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        String clickedUserId = userIdList.get(position);

                        OtherProfileFragment otherProfileFragment = new OtherProfileFragment();
                        Bundle args = new Bundle();
                        args.putString("userId", clickedUserId);
                        otherProfileFragment.setArguments(args);

                        // Call the non-static switchFragment method on the UserAdapter instance
                        userAdapter.switchFragment(otherProfileFragment);
                        userIdList.clear();
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

    /**
     * Show/hide the loading screen.
     */
    private void showLoading(boolean show) {
        if (activity instanceof AppCompatActivity) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void loadDataFromFirebase(String userId, @NonNull ViewHolder holder) {
        if (userId != null) {
            DocumentReference userDocumentRef = db.collection("users").document(userId);

            userDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        if (documentSnapshot.contains("username"))
                            holder.username.setText(documentSnapshot.getString("username"));

                        if (documentSnapshot.contains("type")) {
                            if (Objects.equals(documentSnapshot.getString("type"), "Tourist"))
                                holder.type.setText(context.getResources().getString(R.string.tourist));
                            else
                                holder.type.setText(context.getResources().getString(R.string.tour_guide));
                        }

                        storageReference.child("images/" + userId + "/profilePic").getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        Glide.with(holder.itemView.getContext())
                                                .load(uri)
                                                .apply(RequestOptions.circleCropTransform())
                                                .into(holder.profilePic);
                                        // Increment the loadedItemCount
                                        loadedItemCount++;
                                        // Check if all items have finished loading
                                        if (loadedItemCount == getItemCount()) {
                                            // All items have finished loading, hide loading screen
                                            showLoading(false);
                                        }
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        Log.e("Glide", "Error loading image", exception);
                                        // Increment the loadedItemCount
                                        loadedItemCount++;
                                        // Check if all items have finished loading
                                        if (loadedItemCount == getItemCount()) {
                                            // All items have finished loading, hide loading screen
                                            showLoading(false);
                                        }
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

                    // Check if all items have finished loading
                    if (loadedItemCount == getItemCount()) {
                        // All items have finished loading, hide loading screen
                        showLoading(false);
                    }
                }
            });
        }
    }
}
