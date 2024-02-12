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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.auth.User;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.fragments.OtherProfileFragment;
import com.tourbuddy.tourbuddy.fragments.ThisProfileFragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ReviewRecyclerViewAdapter extends RecyclerView.Adapter<ReviewRecyclerViewAdapter.ViewHolder> {

    List<String> reviewerIdList;
    String otherUserId;
    Activity activity;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();
    static FirebaseAuth mAuth = FirebaseAuth.getInstance();
    Boolean isTourist;

    public ReviewRecyclerViewAdapter(Activity activity, List<String> reviewerIdList, String otherUserId, boolean isTourist) {
        this.activity = activity;
        this.reviewerIdList = reviewerIdList;
        this.otherUserId = otherUserId;
        this.isTourist = isTourist;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_review_item_layout, parent, false);
        return new ViewHolder(view, this);

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String reviewerId = reviewerIdList.get(position);
        if(isTourist)
            loadTouristDataFromFirebase(reviewerId, holder);
        else
            loadTourGuideDataFromFirebase(reviewerId, holder);

    }

    @Override
    public int getItemCount() {
        return reviewerIdList != null ? reviewerIdList.size() : 0;
    }

    public static String getTimePassed(String inputDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date inputDate = sdf.parse(inputDateStr);
            Date currentDate = new Date();

            long durationInMillis = Math.abs(currentDate.getTime() - inputDate.getTime());
            long days = TimeUnit.MILLISECONDS.toDays(durationInMillis);
            long hours = TimeUnit.MILLISECONDS.toHours(durationInMillis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(durationInMillis);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(durationInMillis);

            if (days >= 365) {
                return (days / 365) + " years ago";
            } else if (days >= 1) {
                return days + " days ago";
            } else if (hours >= 1) {
                return hours + " hours ago";
            } else if (minutes >= 1) {
                return minutes + " minutes ago";
            } else if (seconds < 60) {
                return "Just now";
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }

        return "";
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView reviewerUsername, tourGuide, tourPackage, review, timePassed;
        ImageView userProfilePic;
        com.chaek.android.RatingBar ratingBar;
        ReviewRecyclerViewAdapter reviewRecyclerViewAdapter;
        LinearLayout reviewLayout;

        public ViewHolder(@NonNull View itemView, ReviewRecyclerViewAdapter reviewRecyclerViewAdapter) {
            super(itemView);
            this.reviewRecyclerViewAdapter = reviewRecyclerViewAdapter;
            userProfilePic = itemView.findViewById(R.id.userProfilePic);
            reviewerUsername = itemView.findViewById(R.id.reviewerUsername);
            //tourGuide = itemView.findViewById(R.id.tourGuide);
            timePassed = itemView.findViewById(R.id.timePassed);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            review = itemView.findViewById(R.id.review);
            reviewLayout = itemView.findViewById(R.id.reviewLayout);

        }
    }



    void loadTouristDataFromFirebase(String reviewerId, @NonNull ViewHolder holder) {
        if (reviewerId != null) {
            DocumentReference reviewDocumentRef = db.collection("users").document(otherUserId).collection("reviews").document(reviewerId);

            reviewDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        if(documentSnapshot.contains("tourGuide"))
                            holder.reviewerUsername.setText(documentSnapshot.getString("tourGuide"));

                        if(documentSnapshot.contains("review")) {
                            holder.review.setText(documentSnapshot.getString("review"));
                            if(holder.review.getText().equals(""))
                                holder.reviewLayout.setVisibility(View.GONE);

                        }
                        if(documentSnapshot.contains("rating"))
                            holder.ratingBar.setScore(documentSnapshot.getLong("rating"));

                        if(documentSnapshot.contains("time&date"))
                            holder.timePassed.setText(getTimePassed(documentSnapshot.getString("time&date")));
                    }
                }
            });

            // Load the image into the ImageView using Glide
            storageReference.child("images/" + reviewerId + "/profilePic").getDownloadUrl()
                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            // Got the download URL for 'users/me/profile.png'
                            Glide.with(holder.itemView)
                                    .load(uri)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(holder.userProfilePic);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle failure to load image
                        }
                    });

            holder.userProfilePic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FirebaseUser mUser = mAuth.getCurrentUser();
                    if(mUser.getUid().equals(reviewerId))
                        switchFragment(new ThisProfileFragment());
                    else {
                        Bundle args = new Bundle();
                        args.putString("userId", reviewerId);
                        OtherProfileFragment otherProfileFragment = new OtherProfileFragment();
                        otherProfileFragment.setArguments(args);
                        switchFragment(otherProfileFragment);
                    }
                }
            });
        }
    }


    void loadTourGuideDataFromFirebase(String reviewerId, @NonNull ViewHolder holder) {
        if (reviewerId != null) {
            DocumentReference reviewDocumentRef = db.collection("users").document(reviewerId).collection("reviews").document(otherUserId);

            reviewDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        if(documentSnapshot.contains("reviewerUsername"))
                            holder.reviewerUsername.setText(documentSnapshot.getString("reviewerUsername"));

                        //if(documentSnapshot.contains("tourGuide"))
                            //holder.tourGuide.setText(documentSnapshot.getString("tourGuide"));

                        //if(documentSnapshot.contains("tourPackage"))
                            //holder.tourPackage.setText(documentSnapshot.getString("tourPackage"));

                        if(documentSnapshot.contains("review")) {
                            holder.review.setText(documentSnapshot.getString("review"));
                            if(holder.review.getText().equals(""))
                                holder.reviewLayout.setVisibility(View.GONE);

                        }
                        if(documentSnapshot.contains("rating"))
                            holder.ratingBar.setScore(documentSnapshot.getLong("rating"));

                        if(documentSnapshot.contains("time&date"))
                            holder.timePassed.setText(getTimePassed(documentSnapshot.getString("time&date")));
                    }
                }
            });

            // Load the image into the ImageView using Glide
            storageReference.child("images/" + reviewerId + "/profilePic").getDownloadUrl()
                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            // Got the download URL for 'users/me/profile.png'
                            Glide.with(holder.itemView)
                                    .load(uri)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(holder.userProfilePic);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle failure to load image
                        }
                    });

            holder.userProfilePic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FirebaseUser mUser = mAuth.getCurrentUser();
                    if(mUser.getUid().equals(reviewerId))
                        switchFragment(new ThisProfileFragment());
                    else {
                        Bundle args = new Bundle();
                        args.putString("userId", reviewerId);
                        OtherProfileFragment otherProfileFragment = new OtherProfileFragment();
                        otherProfileFragment.setArguments(args);
                        switchFragment(otherProfileFragment);
                    }
                }
            });
        }
    }

    /**
     * Switch fragment function.
     */
    private void switchFragment(Fragment fragment) {
        if (activity instanceof AppCompatActivity) {
            ((AppCompatActivity) activity).getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}
