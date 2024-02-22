package com.tourbuddy.tourbuddy.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.chaek.android.RatingBar;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.fragments.OtherProfileFragment;
import com.tourbuddy.tourbuddy.fragments.ThisProfileFragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ReviewRecyclerViewAdapter extends RecyclerView.Adapter<ReviewRecyclerViewAdapter.ViewHolder> {

    List<String> reviewerIdList;
    String otherUserId, thisUserId, language;
    Activity activity;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();
    static FirebaseAuth mAuth = FirebaseAuth.getInstance();
    Boolean isTourist;

    public ReviewRecyclerViewAdapter(Activity activity, Context context,List<String> reviewerIdList, String otherUserId, boolean isTourist) {
        this.activity = activity;
        this.reviewerIdList = reviewerIdList;
        this.otherUserId = otherUserId;
        this.thisUserId =  mAuth.getCurrentUser().getUid();
        this.isTourist = isTourist;
        this.language = context.getResources().getConfiguration().locale.getLanguage();

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

        if (isTourist)
            loadTouristDataFromFirebase(reviewerId, holder);
        else
            loadTourGuideDataFromFirebase(reviewerId, holder);
    }

    @Override
    public int getItemCount() {
        return reviewerIdList != null ? reviewerIdList.size() : 0;
    }

    public String getTimePassed(String inputDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date inputDate = sdf.parse(inputDateStr);
            Date currentDate = new Date();

            long durationInMillis = Math.abs(currentDate.getTime() - inputDate.getTime());
            long days = TimeUnit.MILLISECONDS.toDays(durationInMillis);
            long hours = TimeUnit.MILLISECONDS.toHours(durationInMillis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(durationInMillis);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(durationInMillis);


            if(Objects.equals(language, "iw")){
                if (days >= 365)
                    return  activity.getString(R.string.ago) + " " + (days / 365) + " " + activity.getString(R.string.years);
                else if (days >= 1)
                    return activity.getString(R.string.ago) + " " + days + " " + activity.getString(R.string.days);
                else if (hours >= 1)
                    return activity.getString(R.string.ago) + " " + hours + " " + activity.getString(R.string.hours);
                else if (minutes >= 1)
                    return activity.getString(R.string.ago) + " " + minutes + " " + activity.getString(R.string.minutes);
                else if (seconds < 60)
                    return activity.getString(R.string.just_now);
            }
            else{
                if (days >= 365)
                    return (days / 365) + " " + activity.getString(R.string.years) + " " + activity.getString(R.string.ago);
                else if (days >= 1)
                    return days + " " + activity.getString(R.string.days) + " " + activity.getString(R.string.ago);
                else if (hours >= 1)
                    return hours + " " + activity.getString(R.string.hours) + " " + activity.getString(R.string.ago);
                else if (minutes >= 1)
                    return minutes + " " + activity.getString(R.string.minutes) + " " + activity.getString(R.string.ago);
                else if (seconds < 60)
                    return activity.getString(R.string.just_now);
            }

        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }

        return "";
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView reviewerUsername, review, timePassed;
        ImageView userProfilePic, btnEditReview;
        com.chaek.android.RatingBar ratingBar;
        ReviewRecyclerViewAdapter reviewRecyclerViewAdapter;
        LinearLayout reviewLayout;
        View lineView;

        public ViewHolder(@NonNull View itemView, ReviewRecyclerViewAdapter reviewRecyclerViewAdapter) {
            super(itemView);
            this.reviewRecyclerViewAdapter = reviewRecyclerViewAdapter;
            userProfilePic = itemView.findViewById(R.id.userProfilePic);
            reviewerUsername = itemView.findViewById(R.id.reviewerUsername);
            timePassed = itemView.findViewById(R.id.timePassed);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            review = itemView.findViewById(R.id.review);
            reviewLayout = itemView.findViewById(R.id.reviewLayout);
            lineView = itemView.findViewById(R.id.lineView);
            btnEditReview = itemView.findViewById(R.id.btnEditReview);



        }
    }



    private void loadTouristDataFromFirebase(String reviewerId, @NonNull ViewHolder holder) {
        if (reviewerId != null) {
            DocumentReference touristDocumentRef = db.collection("users").document(otherUserId);
            DocumentReference reviewerUserDocumentRef = db.collection("users").document(reviewerId);

            touristDocumentRef.collection("reviews").document(reviewerId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot reviewDocumentSnapshot) {
                    if (reviewDocumentSnapshot.exists()) {
                        if (reviewDocumentSnapshot.contains("tourGuide"))
                            holder.reviewerUsername.setText(reviewDocumentSnapshot.getString("tourGuide"));

                        if (reviewDocumentSnapshot.contains("review")) {
                            holder.review.setText(reviewDocumentSnapshot.getString("review"));
                            if (holder.review.getText().equals(""))
                                holder.reviewLayout.setVisibility(View.GONE);

                        }
                        if (reviewDocumentSnapshot.contains("rating"))
                            holder.ratingBar.setScore(reviewDocumentSnapshot.getLong("rating"));

                        if (reviewDocumentSnapshot.contains("time&date"))
                            holder.timePassed.setText(getTimePassed(reviewDocumentSnapshot.getString("time&date")));

                        if (thisUserId.equals(otherUserId)) {
                            holder.btnEditReview.setVisibility(View.VISIBLE);

                            holder.btnEditReview.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // Create the dialog
                                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                                    View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_review, null);
                                    builder.setView(dialogView);
                                    AlertDialog dialog = builder.create();

                                    EditText review = dialogView.findViewById(R.id.review);
                                    com.chaek.android.RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
                                    Button btnConfirmChanges = dialogView.findViewById(R.id.btnConfirmChanges);
                                    Button btnDeleteReview = dialogView.findViewById(R.id.btnDeleteReview);

                                    final int[] rating = {Math.toIntExact(reviewDocumentSnapshot.getLong("rating"))};

                                    review.setText(holder.review.getText());
                                    ratingBar.setScore(rating[0]);

                                    dialog.show();

                                    ratingBar.setRatingBarListener(new RatingBar.RatingBarListener() {
                                        @Override
                                        public void setRatingBar(int i) {
                                            if (i == 0)
                                                rating[0] = 1;
                                            else
                                                rating[0] = i;
                                        }
                                    });

                                    btnDeleteReview.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            tourist_showDeleteReviewDialog(dialog, touristDocumentRef, reviewerUserDocumentRef, reviewDocumentSnapshot, reviewerId);
                                        }
                                    });

                                    btnConfirmChanges.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            tourist_showConfirmChangesDialog(dialog,touristDocumentRef, reviewerUserDocumentRef, rating, review, reviewerId);
                                        }
                                    });
                                }
                            });
                        }
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
                    if(thisUserId.equals(reviewerId))
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

    private void loadTourGuideDataFromFirebase(String reviewerId, @NonNull ViewHolder holder) {
        if (reviewerId != null) {

            DocumentReference tourGuideDocumentRef = db.collection("users").document(otherUserId);
            DocumentReference reviewerUserDocumentRef = db.collection("users").document(reviewerId);

            reviewerUserDocumentRef.collection("reviews").document(otherUserId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot reviewDocumentSnapshot) {
                    if (reviewDocumentSnapshot.exists()) {
                        if(reviewDocumentSnapshot.contains("reviewerUsername"))
                            holder.reviewerUsername.setText(reviewDocumentSnapshot.getString("reviewerUsername"));

                        if(reviewDocumentSnapshot.contains("review")) {
                            holder.review.setText(reviewDocumentSnapshot.getString("review"));
                            if(holder.review.getText().equals(""))
                                holder.reviewLayout.setVisibility(View.GONE);

                        }
                        if(reviewDocumentSnapshot.contains("rating"))
                            holder.ratingBar.setScore(reviewDocumentSnapshot.getLong("rating"));

                        if(reviewDocumentSnapshot.contains("time&date"))
                            holder.timePassed.setText(getTimePassed(reviewDocumentSnapshot.getString("time&date")));

                        if(thisUserId.equals(reviewerId)){
                            holder.lineView.setVisibility(View.VISIBLE);
                            holder.btnEditReview.setVisibility(View.VISIBLE);

                            holder.btnEditReview.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // Create the dialog
                                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                                    View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_review, null);
                                    builder.setView(dialogView);
                                    AlertDialog dialog = builder.create();

                                    EditText review = dialogView.findViewById(R.id.review);
                                    com.chaek.android.RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
                                    Button btnConfirmChanges = dialogView.findViewById(R.id.btnConfirmChanges);
                                    Button btnDeleteReview = dialogView.findViewById(R.id.btnDeleteReview);

                                    final int[] rating = {Math.toIntExact(reviewDocumentSnapshot.getLong("rating"))};

                                    review.setText(holder.review.getText());
                                    ratingBar.setScore(rating[0]);

                                    dialog.show();

                                    ratingBar.setRatingBarListener(new RatingBar.RatingBarListener() {
                                        @Override
                                        public void setRatingBar(int i) {
                                            if (i == 0)
                                                rating[0] = 1;
                                            else
                                                rating[0] = i;
                                        }
                                    });

                                    btnDeleteReview.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            tourGuide_showDeleteReviewDialog(dialog,tourGuideDocumentRef, reviewerUserDocumentRef, reviewDocumentSnapshot, reviewerId);
                                        }
                                    });

                                    btnConfirmChanges.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            tourGuide_showConfirmChangesDialog(dialog,tourGuideDocumentRef, reviewerUserDocumentRef, rating, review);
                                        }
                                    });
                                }
                            });
                        }
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
                    if(thisUserId.equals(reviewerId))
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
     * Open "Confirm Changes" dialog.
     */
    private void tourGuide_showConfirmChangesDialog(AlertDialog dialog, DocumentReference tourGuideDocumentRef, DocumentReference reviewerUserDocumentRef, int[] rating, EditText review) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getResources().getString(R.string.confirmChanges));
        builder.setMessage(activity.getResources().getString(R.string.confirmChangesMessage));
        builder.setPositiveButton(activity.getResources().getString(R.string.apply), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface confirmDialog, int which) {
                Calendar calendar = Calendar.getInstance();
                Date currentTime = calendar.getTime();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                String currentTimeAndDate = sdf.format(currentTime);
                final long[] oldRating = new long[1];
                oldRating[0] = -1;
                // Create a new review object
                Map<String, Object> newReviewData = new HashMap<>();
                newReviewData.put("rating", rating[0]);
                newReviewData.put("review", review.getText().toString());
                newReviewData.put("time&date", currentTimeAndDate);

                // Reference to the reviews collection for the current user
                DocumentReference reviewsRef = reviewerUserDocumentRef.collection("reviews").document(otherUserId);
                reviewsRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot reviewDocumentSnapshot) {
                        if (reviewDocumentSnapshot.exists()){
                            oldRating[0] = reviewDocumentSnapshot.getLong("rating");
                            reviewsRef.update(newReviewData);
                            tourGuideDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot tourGuideDocumentSnapshot) {
                                    if (tourGuideDocumentSnapshot.exists()) {
                                        if (tourGuideDocumentSnapshot.contains("totalRating")) {
                                            int totalRating = Math.toIntExact(tourGuideDocumentSnapshot.getLong("totalRating"))
                                                    - Math.toIntExact(reviewDocumentSnapshot.getLong("rating")) + rating[0];
                                            Map<String, Object> totalRatingData = new HashMap<>();
                                            totalRatingData.put("totalRating", totalRating);
                                            tourGuideDocumentRef.update(totalRatingData);

                                        }
                                    }
                                }
                            });
                        }
                    }
                }).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Log.d("DATABASE", "Confirmed changes to review successfully");
                        confirmDialog.dismiss();
                        dialog.dismiss();
                        Bundle args = new Bundle();
                        args.putString("userId", tourGuideDocumentRef.getId());
                        OtherProfileFragment otherProfileFragment = new OtherProfileFragment();
                        otherProfileFragment.setArguments(args);
                        switchFragment(otherProfileFragment);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("DATABASE", "Error getting confirming changes to review", e);
                        confirmDialog.dismiss();
                        dialog.dismiss();
                    }
                });
            }
        });

        builder.setNegativeButton(activity.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing, close the dialog
            }
        });

        builder.show();
    }

    private void tourist_showConfirmChangesDialog(AlertDialog dialog, DocumentReference touristDocumentRef, DocumentReference reviewerUserDocumentRef, int[] rating, EditText review, String reviewerId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getResources().getString(R.string.confirmChanges));
        builder.setMessage(activity.getResources().getString(R.string.confirmChangesMessage));
        builder.setPositiveButton(activity.getResources().getString(R.string.apply), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface confirmDialog, int which) {
                Calendar calendar = Calendar.getInstance();
                Date currentTime = calendar.getTime();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                String currentTimeAndDate = sdf.format(currentTime);
                final long[] oldRating = new long[1];
                oldRating[0] = -1;
                // Create a new review object
                Map<String, Object> newReviewData = new HashMap<>();
                newReviewData.put("rating", rating[0]);
                newReviewData.put("review", review.getText().toString());
                newReviewData.put("time&date", currentTimeAndDate);

                // Reference to the reviews collection for the current user
                DocumentReference reviewsRef = touristDocumentRef.collection("reviews").document(reviewerId);
                reviewsRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot reviewDocumentSnapshot) {
                        if (reviewDocumentSnapshot.exists()){
                            oldRating[0] = reviewDocumentSnapshot.getLong("rating");
                            reviewsRef.update(newReviewData);
                            reviewerUserDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot tourGuideDocumentSnapshot) {
                                    if (tourGuideDocumentSnapshot.exists()) {
                                        if (tourGuideDocumentSnapshot.contains("totalRating")) {
                                            int totalRating = Math.toIntExact(tourGuideDocumentSnapshot.getLong("totalRating"))
                                                    - Math.toIntExact(reviewDocumentSnapshot.getLong("rating")) + rating[0];
                                            Map<String, Object> totalRatingData = new HashMap<>();
                                            totalRatingData.put("totalRating", totalRating);
                                            reviewerUserDocumentRef.update(totalRatingData);

                                        }
                                    }
                                }
                            });
                        }
                    }
                }).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Log.d("DATABASE", "Confirmed changes to review successfully");
                        confirmDialog.dismiss();
                        dialog.dismiss();
                        switchFragment(new ThisProfileFragment());
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("DATABASE", "Error getting confirming changes to review", e);
                        confirmDialog.dismiss();
                        dialog.dismiss();
                    }
                });
            }
        });

        builder.setNegativeButton(activity.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing, close the dialog
            }
        });

        builder.show();
    }

    /**
     * Open "Delete Review" dialog.
     */
    private void tourGuide_showDeleteReviewDialog(AlertDialog dialog, DocumentReference tourGuideDocumentRef, DocumentReference reviewerUserDocumentRef, DocumentSnapshot reviewDocumentSnapshot, String reviewerId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getResources().getString(R.string.delete_review));
        builder.setMessage(activity.getResources().getString(R.string.delete_review_message));
        builder.setPositiveButton(activity.getResources().getString(R.string.delete_review), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface confirmDialog, int which) {
                final int[] totalRating = new int[1];

                tourGuideDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot tourGuideDocumentSnapshot) {
                        if (tourGuideDocumentSnapshot.exists()) {
                            if (tourGuideDocumentSnapshot.contains("totalRating")) {
                                totalRating[0] = Math.toIntExact(tourGuideDocumentSnapshot.getLong("totalRating"))
                                        - Math.toIntExact(reviewDocumentSnapshot.getLong("rating"));
                                Map<String, Object> totalRatingData = new HashMap<>();
                                totalRatingData.put("totalRating", totalRating[0]);
                                tourGuideDocumentRef.update(totalRatingData);

                            }
                        }
                    }
                }).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        tourGuideDocumentRef.collection("reviews").document(reviewerId)
                                .delete()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // Review deleted successfully
                                        reviewerUserDocumentRef.collection("reviews").document(otherUserId)
                                                .delete()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        // Review deleted successfully
                                                        Log.d("DATABASE", "Review deleted successfully");
                                                        confirmDialog.dismiss();
                                                        dialog.dismiss();
                                                        Bundle args = new Bundle();
                                                        args.putString("userId", documentSnapshot.getId());
                                                        OtherProfileFragment otherProfileFragment = new OtherProfileFragment();
                                                        otherProfileFragment.setArguments(args);
                                                        switchFragment(otherProfileFragment);

                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        // Failed to delete the review
                                                        Log.w("DATABASE", "Error deleting review", e);
                                                        // Handle the error
                                                        confirmDialog.dismiss();
                                                        dialog.dismiss();

                                                    }
                                                });
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Failed to delete the review
                                        Log.w("DATABASE", "Error deleting review", e);
                                        // Handle the error
                                    }
                                });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("DATABASE", "Error getting tourGuideDocumentRef", e);

                    }
                });
            }
        });

        builder.setNegativeButton(activity.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing, close the dialog
            }
        });

        builder.show();
    }

    private void tourist_showDeleteReviewDialog(AlertDialog dialog, DocumentReference touristDocumentRef, DocumentReference reviewerUserDocumentRef, DocumentSnapshot reviewDocumentSnapshot, String reviewerId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getResources().getString(R.string.delete_review));
        builder.setMessage(activity.getResources().getString(R.string.delete_review_message));
        builder.setPositiveButton(activity.getResources().getString(R.string.delete_review), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface confirmDialog, int which) {
                final int[] totalRating = new int[1];

                reviewerUserDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot tourGuideDocumentSnapshot) {
                        if (tourGuideDocumentSnapshot.exists()) {
                            if (tourGuideDocumentSnapshot.contains("totalRating")) {
                                totalRating[0] = Math.toIntExact(tourGuideDocumentSnapshot.getLong("totalRating"))
                                        - Math.toIntExact(reviewDocumentSnapshot.getLong("rating"));
                                Map<String, Object> totalRatingData = new HashMap<>();
                                totalRatingData.put("totalRating", totalRating[0]);
                                reviewerUserDocumentRef.update(totalRatingData);

                            }
                        }
                    }
                }).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        touristDocumentRef.collection("reviews").document(reviewerId)
                                .delete()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // Review deleted successfully
                                        reviewerUserDocumentRef.collection("reviews").document(otherUserId)
                                                .delete()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        // Review deleted successfully
                                                        Log.d("DATABASE", "Review deleted successfully");
                                                        confirmDialog.dismiss();
                                                        dialog.dismiss();
                                                        switchFragment(new ThisProfileFragment());

                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        // Failed to delete the review
                                                        Log.w("DATABASE", "Error deleting review", e);
                                                        // Handle the error
                                                        confirmDialog.dismiss();
                                                        dialog.dismiss();

                                                    }
                                                });
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Failed to delete the review
                                        Log.w("DATABASE", "Error deleting review", e);
                                        // Handle the error
                                    }
                                });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("DATABASE", "Error getting tourGuideDocumentRef", e);

                    }
                });
            }
        });

        builder.setNegativeButton(activity.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing, close the dialog
            }
        });

        builder.show();
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
