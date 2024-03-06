package com.tourbuddy.tourbuddy.fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.chaek.android.RatingBar;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateLongClickListener;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.format.DateFormatTitleFormatter;
import com.taufiqrahman.reviewratings.BarLabels;
import com.taufiqrahman.reviewratings.RatingReviews;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.adapters.ReviewRecyclerViewAdapter;
import com.tourbuddy.tourbuddy.adapters.TourPackageRecyclerViewAdapter;
import com.tourbuddy.tourbuddy.decorators.EventDecorator;
import com.tourbuddy.tourbuddy.utils.ActiveTour;
import com.tourbuddy.tourbuddy.utils.AppUtils;
import com.tourbuddy.tourbuddy.utils.Chat;
import com.tourbuddy.tourbuddy.utils.DataCache;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class OtherProfileFragment extends Fragment implements TourPackageRecyclerViewAdapter.OnLoadCompleteListener {

    // Initialize DataCache
    DataCache dataCache;

    // UI elements
    ImageView profilePic;
    TextView username, bio, birthDate, gender, type,
            totalRating, noAvailableReviews, noAvailableTourPackages,
            tourPackagesORbookedToursHeadline, reviewsHeadline;
    Button btnAddReview, btnDM;
    com.chaek.android.RatingBar ratingBar;


    // Firebase
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    StorageReference storageReference;

    // SwipeRefreshLayout for pull-to-refresh functionality
    SwipeRefreshLayout swipeRefreshLayout;

    // Loading overlay
    RelativeLayout userDataLoadingOverlay, calendarLoadingOverlay;

    // RecyclerView for Tour Packages
    RecyclerView recyclerViewTours, recyclerViewReviews;
    TourPackageRecyclerViewAdapter tourPackageRecyclerViewAdapter;
    ReviewRecyclerViewAdapter reviewsRecyclerViewAdapter;
    List<String> tourPackagesIdList;
    List<String> reviewsIdList;

    String otherUserId, thisUserId, thisUserType = "Tour Guide", otherUserType;

    final LocalDate min = LocalDate.now(ZoneId.systemDefault());
    final LocalDate max = min.plusMonths(6);
    final String DATE_FORMAT = "yyyy-MM-dd";

    List<LocalDate> decoratedDatesList;
    List<ActiveTour> activeTours;
    List<DocumentReference> bookedToursRefs;
    MaterialCalendarView calendarView;
    ConstraintLayout totalRatingLayout;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_other_profile, container, false);
        dataCache = DataCache.getInstance();
        // Initialize UI elements
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        profilePic = view.findViewById(R.id.profilePic);
        username = view.findViewById(R.id.username);
        bio = view.findViewById(R.id.bio);
        birthDate = view.findViewById(R.id.birthDate);
        gender = view.findViewById(R.id.gender);
        type = view.findViewById(R.id.type);
        btnAddReview = view.findViewById(R.id.btnAddReview);
        btnDM = view.findViewById(R.id.btnDM);

        ratingBar = view.findViewById(R.id.ratingBar);
        totalRating = view.findViewById(R.id.totalRating);
        totalRatingLayout = view.findViewById(R.id.totalRatingLayout);

        noAvailableReviews = view.findViewById(R.id.noAvailableReviews);
        noAvailableTourPackages = view.findViewById(R.id.noAvailableTourPackages);

        tourPackagesORbookedToursHeadline = view.findViewById(R.id.tourPackagesORbookedToursHeadline);
        reviewsHeadline = view.findViewById(R.id.reviewsHeadline);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Initialize loading overlay elements
        userDataLoadingOverlay = view.findViewById(R.id.userDataLoadingOverlay);
        calendarLoadingOverlay = view.findViewById(R.id.calendarLoadingOverlay);

        recyclerViewReviews = view.findViewById(R.id.recyclerViewReviews);
        recyclerViewTours = view.findViewById(R.id.recyclerViewTours);
        calendarView = view.findViewById(R.id.calendarView);

        swipeRefreshLayout.setProgressBackgroundColor(R.color.dark_primary);
        swipeRefreshLayout.setColorScheme(R.color.orange_primary);

        tourPackagesIdList = new ArrayList<>();
        reviewsIdList = new ArrayList<>();
        decoratedDatesList = new ArrayList<>();

        // Retrieve user ID from arguments
        if (getArguments() != null)
            otherUserId = getArguments().getString("userId");

        FirebaseUser mUser = mAuth.getCurrentUser();
        if (mUser != null)
            thisUserId = mUser.getUid();

        // Initialize calendar and set its attributes
        calendarView.setShowOtherDates(MaterialCalendarView.SHOW_ALL);
        calendarView.state().edit().setMinimumDate(min).setMaximumDate(max).commit();

        // Set the locale for the calendar view title formatter
        calendarView.setTitleFormatter(new DateFormatTitleFormatter());

        userDataLoadingOverlay.setVisibility(View.VISIBLE);
        calendarLoadingOverlay.setVisibility(View.VISIBLE);

        btnDM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Code to be executed when the button is clicked
                // For example, you can call the function to create a new chat here
                enterChat(thisUserId, otherUserId, new OnChatCreatedListener() {
                    @Override
                    public void onChatCreated(Chat chat) {
                        // Chat created successfully, perform actions with chatId
                        Log.d("DATABASE", "Chat created successfully with ID: " + chat.getChatId());
                        // Replace fragment with ChatFragment
                        if (getActivity() != null) {
                            AppUtils.switchFragment(OtherProfileFragment.this, ChatFragment.newInstance(chat));
                        }
                    }

                    @Override
                    public void onChatExists(Chat chat) {
                        // Error occurred while creating chat, handle failure
                        if (getActivity() != null) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.frameLayout, ChatFragment.newInstance(chat))
                                    .addToBackStack(null)
                                    .commit();
                        }
                    }

                    @Override
                    public void onChatCreateFailed(Exception e) {
                        // Error occurred while creating chat, handle failure
                        Log.e("DATABASE", "Error creating chat", e);
                    }
                });
            }
        });


        DocumentReference otherUserDocumentRef = db.collection("users").document(otherUserId);

        // Retrieve userType from Firestore, duplicated from loadDataFromFirebase in case there's an asynchronous delay
        otherUserDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot otherUserdocumentSnapshot) {
                if (isAdded() && otherUserdocumentSnapshot.exists()) {
                    // Check if the user type exists in the Firestore document
                    if (otherUserdocumentSnapshot.contains("type")) {
                        otherUserType = otherUserdocumentSnapshot.getString("type");
                        if (Objects.equals(otherUserType, "Tour Guide")) {
                            tourGuide_tourBookingManager();
                            tourGuide_reviewsManager();
                        }
                        else {
                            calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_SINGLE);
                            tourPackagesORbookedToursHeadline.setText(R.string.my_booked_tours);
                            tourist_tourBookingManager();
                            tourist_reviewsManager();

                        }
                    }
                }
            }
        }).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentReference thisUserDocumentRef = db.collection("users").document(thisUserId);

                // Retrieve data from Firestore
                thisUserDocumentRef.get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot thisUserdocumentSnapshot) {
                                if (isAdded() && thisUserdocumentSnapshot.exists()) {

                                    thisUserDocumentRef.collection("reviews").document(otherUserId).get()
                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot reviewsDocumentSnapshot) {
                                            if(reviewsDocumentSnapshot.exists())
                                                btnAddReview.setVisibility(View.GONE);
                                            else {
                                                // Check if the user type exists in the Firestore document
                                                if (thisUserdocumentSnapshot.contains("type")) {
                                                    thisUserType = thisUserdocumentSnapshot.getString("type");
                                                    if (Objects.equals(thisUserType, "Tourist"))
                                                        btnAddReview.setVisibility(View.VISIBLE);
                                                    else
                                                        btnAddReview.setVisibility(View.GONE);
                                                }
                                                else
                                                    btnAddReview.setVisibility(View.GONE);

                                            }
                                        }
                                    });

                                }
                            }
                        }).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if(Objects.equals(thisUserType, "Tour Guide"))
                                    btnAddReview.setVisibility(View.GONE);

                                if(Objects.equals(thisUserType, "Tourist"))
                                    if(Objects.equals(otherUserType, "Tour Guide"))
                                        btnAddReview.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                if(isAdded()) {
                                                    // Create the dialog
                                                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                                                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_review, null);
                                                    builder.setView(dialogView);
                                                    AlertDialog dialog = builder.create();
                                                    final int[] rating = {9};
                                                    EditText review = dialogView.findViewById(R.id.review);
                                                    com.chaek.android.RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
                                                    Button btnAddReview = dialogView.findViewById(R.id.btnAddReview);

                                                    ratingBar.setRatingBarListener(new RatingBar.RatingBarListener() {
                                                        @Override
                                                        public void setRatingBar(int i) {
                                                            if(i == 0)
                                                                rating[0] = 1;
                                                            else
                                                                rating[0] = i;


                                                        }
                                                    });

                                                    btnAddReview.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            // Assuming you have the necessary variables available
                                                            String reviewText = String.valueOf(review.getText());
                                                            final String[] reviewerUsername = new String[1];
                                                            final String[] tourGuide = new String[1];
                                                            reviewerUsername[0] = "";
                                                            tourGuide[0] = "";

                                                            thisUserDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                                @Override
                                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                                    if(documentSnapshot.contains("username"))
                                                                        reviewerUsername[0] = documentSnapshot.getString("username");
                                                                }
                                                            }).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                    otherUserDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                                        @Override
                                                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                                            if(documentSnapshot.contains("username"))
                                                                                tourGuide[0] = documentSnapshot.getString("username");
                                                                        }
                                                                    }).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                            Calendar calendar = Calendar.getInstance();
                                                                            Date currentTime = calendar.getTime();

                                                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                                                            String currentTimeAndDate = sdf.format(currentTime);
                                                                            final long[] oldRating = new long[1];
                                                                            oldRating[0] = -1;
                                                                            // Create a new review object
                                                                            Map<String, Object> reviewData = new HashMap<>();
                                                                            reviewData.put("tourGuide", tourGuide[0]);
                                                                            reviewData.put("rating", rating[0]);
                                                                            reviewData.put("review", reviewText);
                                                                            reviewData.put("reviewerUsername", reviewerUsername[0]);
                                                                            reviewData.put("time&date", currentTimeAndDate);

                                                                            // Reference to the reviews collection for the current user
                                                                            DocumentReference userReviewsRef = db.collection("users").document(thisUserId).collection("reviews").document(otherUserId);
                                                                            userReviewsRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                                                @Override
                                                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                                                    if (documentSnapshot.exists())
                                                                                        oldRating[0] = documentSnapshot.getLong("rating");
                                                                                }
                                                                            }).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                                                @Override
                                                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                                                    // Add the review to Firestore
                                                                                    userReviewsRef.set(reviewData)
                                                                                            .addOnSuccessListener(documentReference -> {
                                                                                                //Log.d("DATABASE", "Review added with ID: " + otherUserId);
                                                                                                DocumentReference tourGuideReviewsRef = db.collection("users").document(otherUserId).collection("reviews").document(thisUserId);
                                                                                                Map<String, Object> reviewRef = new HashMap<>();
                                                                                                reviewRef.put("reviewRef", userReviewsRef);
                                                                                                tourGuideReviewsRef.set(reviewRef);


                                                                                                otherUserDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                                                                    @Override
                                                                                                    public void onSuccess(DocumentSnapshot otherUserDocumentSnapshot) {

                                                                                                        float currentTotalRating = 0;
                                                                                                        if(otherUserDocumentSnapshot.contains("totalRating"))
                                                                                                            currentTotalRating = otherUserDocumentSnapshot.getLong("totalRating");

                                                                                                        if(oldRating[0] != -1)
                                                                                                            currentTotalRating -= oldRating[0];

                                                                                                        currentTotalRating+= rating[0];

                                                                                                        Map<String, Object> totalRating = new HashMap<>();
                                                                                                        totalRating.put("totalRating", currentTotalRating);
                                                                                                        otherUserDocumentRef.update(totalRating);

                                                                                                        Bundle args = new Bundle();
                                                                                                        args.putString("userId", otherUserDocumentRef.getId());
                                                                                                        OtherProfileFragment otherProfileFragment = new OtherProfileFragment();
                                                                                                        otherProfileFragment.setArguments(args);
                                                                                                        AppUtils.switchFragment(OtherProfileFragment.this, otherProfileFragment);
                                                                                                        dialog.dismiss();
                                                                                                    }
                                                                                                });
                                                                                            })
                                                                                            .addOnFailureListener(e -> {
                                                                                                Log.w("DATABASE", "Error adding review", e);
                                                                                                dialog.dismiss();
                                                                                                // Handle errors if review addition fails
                                                                                            });
                                                                                }
                                                                            });

                                                                        }
                                                                    });
                                                                }
                                                            });
                                                        }
                                                    });
                                                    dialog.show();
                                                }
                                            }
                                        });

                                    else
                                        btnAddReview.setVisibility(View.GONE);

                            }
                        }).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                loadDataFromFirebase(view);
                            }
                        });
            }
        });

        // Set up pull-to-refresh functionality
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setProgressBackgroundColor(R.color.dark_primary);
            swipeRefreshLayout.setColorScheme(R.color.orange_primary);

            swipeRefreshLayout.setOnRefreshListener(() -> {
                tourPackagesIdList = new ArrayList<>();

                // Remove all existing decorators
                calendarView.removeDecorators();
                // Clear the list of decorated dates
                decoratedDatesList.clear();

                loadDataFromFirebase(view);
                swipeRefreshLayout.setRefreshing(false);
            });
        } else {
            Log.e("ThisProfileFragment", "SwipeRefreshLayout is null");
        }

        return view;
    }


    private void tourGuide_reviewsManager() {
        CollectionReference collectionReference = db.collection("users").document(otherUserId).collection("reviews");

        // Clear the list before adding new items
        reviewsIdList.clear();

        collectionReference.get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots)
                // Add document ID to the reviewsIdList
                reviewsIdList.add(documentSnapshot.getId());

            if (reviewsIdList != null) {
                // Move the current user's review to the top if it exists
                if (reviewsIdList.contains(thisUserId)) {
                    btnAddReview.setVisibility(View.GONE);
                    reviewsIdList.remove(thisUserId);
                    reviewsIdList.add(0, thisUserId);
                }
                else
                    btnAddReview.setVisibility(View.VISIBLE);


            }
            collectionReference.getParent().get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.contains("totalRating")) {
                        if(!reviewsIdList.isEmpty()){
                            float totalRatingValue = (float) documentSnapshot.getLong("totalRating") / (float) reviewsIdList.size();
                            totalRating.setText(String.format("%.1f", totalRatingValue/2.0));
                            ratingBar.setScore((Math.round(totalRatingValue)));
                            noAvailableReviews.setVisibility(View.GONE);
                            totalRatingLayout.setVisibility(View.VISIBLE);

                        }
                    }
                }
            });



            // Notify the adapter if needed
            if (reviewsRecyclerViewAdapter != null) {
                reviewsRecyclerViewAdapter.notifyDataSetChanged();
            }

            // Initialize and set up the adapter after fetching the data
            reviewsRecyclerViewAdapter = new ReviewRecyclerViewAdapter(getActivity(), requireContext(), reviewsIdList, otherUserId, false);
            recyclerViewReviews.setAdapter(reviewsRecyclerViewAdapter);
            recyclerViewReviews.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        }).addOnFailureListener(e -> {
            // Handle failure
        });
    }
    private void tourist_reviewsManager(){
        CollectionReference collectionReference = db.collection("users").document(otherUserId).collection("reviews");

        // Clear the list before adding new items
        reviewsIdList.clear();

        collectionReference.get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                // Add document ID to the reviewsIdList
                reviewsIdList.add(documentSnapshot.getId());
            }

            if(!reviewsIdList.isEmpty())
                noAvailableReviews.setVisibility(View.GONE);
            else
                noAvailableReviews.setVisibility(View.VISIBLE);

            // Notify the adapter if needed
            if (reviewsRecyclerViewAdapter != null) {
                reviewsRecyclerViewAdapter.notifyDataSetChanged();
            }

            // Initialize and set up the adapter after fetching the data
            reviewsRecyclerViewAdapter = new ReviewRecyclerViewAdapter(getActivity(), requireContext(), reviewsIdList, otherUserId, true);
            recyclerViewReviews.setAdapter(reviewsRecyclerViewAdapter);
            recyclerViewReviews.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        }).addOnFailureListener(e -> {
            // Handle failure
        });
    }
    private void tourist_tourBookingManager(){
        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                if (decoratedDatesList.contains(date.getDate())) {
                    // Disable selection for decorated dates
                    calendarView.clearSelection();
                    LocalDate selectedDate = LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
                    // Set up event decorators
                    for (DocumentReference tourRef : bookedToursRefs) {
                        // Handle failure to fetch color from Firestore
                        tourRef.get().addOnSuccessListener(tourDocumentSnapshot -> {
                            if (tourDocumentSnapshot.exists()) {
                                DocumentReference tourPackageRef = tourDocumentSnapshot.getDocumentReference("tourPackageRef");
                                if (tourPackageRef != null) {
                                    tourPackageRef.get().addOnSuccessListener(tourPackageDocumentSnapshot -> {
                                        if (tourPackageDocumentSnapshot.exists()) {
                                            // Fetch tourPackageName from the referenced tourPackageRef document
                                            LocalDate startDate, endDate;
                                            if (tourDocumentSnapshot.contains("startDate")) {
                                                startDate = getLocalDate(tourDocumentSnapshot.getString("startDate"));
                                                if(tourDocumentSnapshot.contains("endDate"))
                                                    endDate = getLocalDate(tourDocumentSnapshot.getString("endDate"));
                                                else
                                                    endDate = startDate;
                                                if (selectedDate.equals(startDate) || selectedDate.equals(endDate) ||
                                                        (selectedDate.isAfter(startDate) && selectedDate.isBefore(endDate))) {
                                                    if(isAdded()) {
                                                        // Create the dialog
                                                        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                                                        View dialogView = getLayoutInflater().inflate(R.layout.dialog_view_tour_details, null);
                                                        builder.setView(dialogView);
                                                        AlertDialog dialog = builder.create();

                                                        TextView tourDate = dialogView.findViewById(R.id.tourDate);
                                                        TextView tourPackage = dialogView.findViewById(R.id.tourPackage);
                                                        TextView groupSize = dialogView.findViewById(R.id.groupSize);
                                                        TextView bookedTouristsAmount = dialogView.findViewById(R.id.bookedTouristsAmount);


                                                        tourPackageRef.getParent().getParent().get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                            @Override
                                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                                if (documentSnapshot.exists()) { // Check if the query snapshot is not empty
                                                                    // Assuming there's only one document in the result (parent document)

                                                                    String tourGuideName = documentSnapshot.getString("username");

                                                                    if (tourGuideName != null) {
                                                                        ConstraintLayout touristOnlyLayout = dialogView.findViewById(R.id.touristOnlyLayout);
                                                                        touristOnlyLayout.setVisibility(View.VISIBLE);
                                                                        TextView tourGuide = dialogView.findViewById(R.id.tourGuide);
                                                                        tourGuide.setText(tourGuideName);

                                                                        touristOnlyLayout.setOnClickListener(new View.OnClickListener() {
                                                                            @Override
                                                                            public void onClick(View v) {
                                                                                dialog.dismiss();
                                                                                if(!Objects.equals(thisUserId, documentSnapshot.getId())){
                                                                                    Bundle args = new Bundle();
                                                                                    args.putString("userId", documentSnapshot.getId());
                                                                                    OtherProfileFragment otherProfileFragment = new OtherProfileFragment();
                                                                                    otherProfileFragment.setArguments(args);
                                                                                    AppUtils.switchFragment(OtherProfileFragment.this, otherProfileFragment);
                                                                                }
                                                                                else
                                                                                    AppUtils.switchFragment(OtherProfileFragment.this, new ThisProfileFragment());
                                                                            }
                                                                        });
                                                                    } else {
                                                                        // Handle case where username field is not found or null
                                                                        Log.e("DATABASE", "Username field is null or not found in parent document");
                                                                    }
                                                                } else {
                                                                    // Handle case where no parent document is found
                                                                    Log.e("DATABASE", "No parent document found for tourPackageRef");
                                                                }
                                                            }

                                                        });


                                                        // Retrieve data from Firestore
                                                        int groupSizeInt = tourDocumentSnapshot.getLong("groupSize").intValue();
                                                        int bookedTouristsInt = tourDocumentSnapshot.getLong("bookedTouristsAmount").intValue();

                                                        // Populate AlertDialog
                                                        if (endDate != startDate)
                                                            tourDate.setText(startDate.toString() + requireContext().getResources().getString(R.string.to) + endDate.toString());
                                                        else
                                                            tourDate.setText(startDate.toString());

                                                        // Set the tour package name in the dialog
                                                        tourPackage.setText(tourPackageRef.getId()); // Set the tour package name

                                                        DocumentReference userDocument = tourPackageRef.getParent().getParent();

                                                        groupSize.setText(" / " + groupSizeInt);
                                                        int availableSpots = groupSizeInt - bookedTouristsInt;
                                                        bookedTouristsAmount.setText(String.valueOf(availableSpots));

                                                        // Calculate the percentage of spots left relative to group size
                                                        double percentage = (double) availableSpots / groupSizeInt * 100;

                                                        // Set the color of the text based on the percentage
                                                        if (availableSpots == 0) {
                                                            bookedTouristsAmount.setTextColor(getContext().getColor(R.color.red));
                                                        } else if (percentage < 25) {
                                                            bookedTouristsAmount.setTextColor(getContext().getColor(R.color.orange_primary)); // Orange color
                                                        } else if (percentage < 50) {
                                                            bookedTouristsAmount.setTextColor(getContext().getColor(R.color.yellow));
                                                        }

                                                        userDocument.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                            @Override
                                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                                // Show the dialog
                                                                dialog.show();

                                                            }
                                                        }).addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Log.e("DATABASE", "Error getting tourPackageRef parent document", e);
                                                            }
                                                        });

                                                    }
                                                }
                                            }

                                        }
                                    }).addOnFailureListener(e -> {
                                        // Handle failure to fetch tourPackageRef document
                                        Log.e("DATABASE", "Error getting tourPackageRef document", e);
                                    });
                                } else {
                                    Log.e("DATABASE", "tourPackageRef is null");
                                }
                            }
                        }).addOnFailureListener(e -> {
                            // Handle failure to fetch tourRef document
                            Log.e("DATABASE", "Error getting tourRef document", e);
                        });

                    }

                }
            }
        });
    }
    private void tourGuide_tourBookingManager(){
        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                calendarView.clearSelection();
                if (decoratedDatesList.contains(date.getDate())) {
                    LocalDate selectedDate = LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
                    // Iterate through active tours to find the corresponding tour for the selected date
                    for (ActiveTour tour : activeTours) {
                        LocalDate startDate = getLocalDate(tour.getStartDate());
                        LocalDate endDate;
                        if(tour.getEndDate().isEmpty())
                            endDate = startDate;
                        else
                            endDate = getLocalDate(tour.getEndDate());

                        // Check if the selected date is between the start and end dates of the tour
                        if (selectedDate.equals(startDate) || selectedDate.equals(endDate) ||
                                (selectedDate.isAfter(startDate) && selectedDate.isBefore(endDate))){
                            DocumentReference activeTourRef = db.collection("users")
                                    .document(otherUserId)
                                    .collection("activeTours")
                                    .document(tour.getStartDate());

                            activeTourRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot activeTourDocumentSnapshot) {
                                    if (activeTourDocumentSnapshot.exists()) {
                                        if(isAdded()) {
                                            // Retrieve data from Firestore
                                            // Reference to the Firestore document
                                            DocumentReference thisUserDocumentRef = db.collection("users").document(thisUserId);

                                            // Retrieve data from Firestore
                                            thisUserDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                @Override
                                                public void onSuccess(DocumentSnapshot thisUserdocumentSnapshot) {
                                                    if (isAdded() && thisUserdocumentSnapshot.exists()) {
                                                        // Check if the user type exists in the Firestore document
                                                        if (thisUserdocumentSnapshot.contains("type"))
                                                            thisUserType = thisUserdocumentSnapshot.getString("type");

                                                        // Create the dialog
                                                        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                                                        View dialogView = getLayoutInflater().inflate(R.layout.dialog_view_tour_details, null);
                                                        builder.setView(dialogView);
                                                        AlertDialog dialog = builder.create();

                                                        TextView tourDate = dialogView.findViewById(R.id.tourDate);
                                                        TextView tourPackage = dialogView.findViewById(R.id.tourPackage);
                                                        TextView groupSize = dialogView.findViewById(R.id.groupSize);
                                                        TextView bookedTouristsAmount = dialogView.findViewById(R.id.bookedTouristsAmount);
                                                        Button btnBookTour = dialogView.findViewById(R.id.btnBookTour);
                                                        btnBookTour.setVisibility(View.VISIBLE);


                                                        String tourPackageName = tour.getTourPackageRef().getId();
                                                        String startDate = tour.getStartDate();
                                                        String endDate = tour.getEndDate();

                                                        int groupSizeInt = activeTourDocumentSnapshot.getLong("groupSize").intValue();
                                                        int bookedTouristsInt = activeTourDocumentSnapshot.getLong("bookedTouristsAmount").intValue();
                                                        // Populate AlertDialog

                                                        if (endDate != null)
                                                            tourDate.setText(startDate + requireContext().getResources().getString(R.string.to) + endDate);
                                                        else
                                                            tourDate.setText(startDate);

                                                        tourPackage.setText(tourPackageName);
                                                        groupSize.setText(" / " + groupSizeInt);
                                                        int availableSpots = groupSizeInt - bookedTouristsInt;
                                                        bookedTouristsAmount.setText(String.valueOf(availableSpots));

                                                        // Calculate the percentage of spots left relative to group size
                                                        double percentage = (double) availableSpots / groupSizeInt * 100;

                                                        // Set the color of the text based on the percentage
                                                        if (availableSpots == 0)
                                                            bookedTouristsAmount.setTextColor(getContext().getColor(R.color.red));
                                                        else if (percentage < 25)
                                                            bookedTouristsAmount.setTextColor(getContext().getColor(R.color.orange_primary)); // Orange color
                                                        else if (percentage < 50)
                                                            bookedTouristsAmount.setTextColor(getContext().getColor(R.color.yellow));

                                                        if(Objects.equals(thisUserType, "Tour Guide"))
                                                            btnBookTour.setVisibility(View.GONE);

                                                        else if (availableSpots == 0)
                                                            btnBookTour.setEnabled(false);




                                                        btnBookTour.setOnClickListener(new View.OnClickListener() {
                                                            @Override
                                                            public void onClick(View v) {
                                                                // Reference to the booked tours of the current user
                                                                CollectionReference bookedToursRef = db.collection("users")
                                                                        .document(thisUserId).collection("bookedTours");
                                                                final boolean[] isOverlap = {false};
                                                                bookedToursRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
                                                                    final AtomicInteger completedQueries = new AtomicInteger(0);
                                                                    final int totalQueries = queryDocumentSnapshots.size();
                                                                    for (QueryDocumentSnapshot bookedTour : queryDocumentSnapshots) {
                                                                        String bookedStartDate = bookedTour.getId();
                                                                        final String[] bookedEndDate = {null};

                                                                        DocumentReference tourRef = bookedTour.getDocumentReference("tourRef");

                                                                        tourRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                                            @Override
                                                                            public void onSuccess(DocumentSnapshot tourDocumentSnapshot) {
                                                                                if (isAdded() && tourDocumentSnapshot.exists()) {
                                                                                    // Check if booked tour has an end date
                                                                                    if (tourDocumentSnapshot.contains("endDate")) {
                                                                                        bookedEndDate[0] = tourDocumentSnapshot.getString("endDate");
                                                                                    }
                                                                                }
                                                                            }
                                                                        }).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                                // Check for overlap
                                                                                if (checkOverlappingTours(tour.getStartDate(), tour.getEndDate(), bookedStartDate, bookedEndDate[0])) {
                                                                                    // A tour is already booked within the given date range
                                                                                    Toast.makeText(requireContext(), requireContext().getResources().getString(R.string.you_have_already_booked_a_tour_during_this_period), Toast.LENGTH_SHORT).show();
                                                                                    isOverlap[0] = true;
                                                                                }
                                                                                completedQueries.incrementAndGet();
                                                                                if (completedQueries.get() == totalQueries && !isOverlap[0]) {
                                                                                    // No overlapping tour found, proceed to book the tour
                                                                                    bookTour(tour.getStartDate(), activeTourDocumentSnapshot);
                                                                                    dialog.dismiss();
                                                                                }
                                                                            }
                                                                        });
                                                                    }
                                                                    if (totalQueries == 0 || completedQueries.get() == totalQueries && !isOverlap[0]) {
                                                                        // No booked tours found or all queries completed with no overlap
                                                                        bookTour(tour.getStartDate(), activeTourDocumentSnapshot);
                                                                        dialog.dismiss();
                                                                    }
                                                                }).addOnFailureListener(e -> {
                                                                    // Handle failure
                                                                    Log.e("DATABASE", "Error checking booked tours", e);
                                                                });
                                                            }
                                                        });


                                                        // Show the dialog
                                                        dialog.show();
                                                    }

                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.e("DATABASE", "Error getting document", e);
                                                }
                                            });
                                        }
                                    } else
                                        Log.d("DATABASE", "No such document");

                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d("DATABASE", "Error getting document", e);
                                }
                            });
                        }

                    }
                }
            }
        });
    }


    // Method to check overlap between two date ranges
    private boolean checkOverlappingTours(String newStartDate, String newEndDate, String existingStartDate, String existingEndDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
            Date newStart = sdf.parse(newStartDate);
            Date newEnd = sdf.parse(newEndDate);
            Date existingStart = sdf.parse(existingStartDate);
            Date existingEnd = existingEndDate != null ? sdf.parse(existingEndDate) : null;

            // Check if any day between the start and end dates of the new tour falls within existing tour's date range
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(newStart);
            while (!calendar.getTime().after(newEnd)) {
                Date currentDate = calendar.getTime();
                if ((existingStart == null || !currentDate.before(existingStart)) && (existingEnd == null || !currentDate.after(existingEnd)))
                    return true; // Overlap found

                calendar.add(Calendar.DAY_OF_MONTH, 1); // Move to the next day
            }

            return false; // No overlap found
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void bookTour(String startDate, DocumentSnapshot tourRef) {
        // Create a new booked tour document
        Map<String, Object> bookedTourData = new HashMap<>();
        bookedTourData.put("tourRef", tourRef.getReference()); // Use getData() to retrieve all data from DocumentSnapshot

        // Add the booked tour to the user's collection of booked tours
        db.collection("users").document(thisUserId).collection("bookedTours").document(startDate)
                .set(bookedTourData)
                .addOnSuccessListener(aVoid -> {
                    // Tour successfully booked

                    Toast.makeText(requireContext(), requireContext().getResources().getString(R.string.tour_booked_successfully), Toast.LENGTH_SHORT).show();
                    DocumentReference tourDocRef = tourRef.getReference();
                    tourDocRef.update("bookedTouristsAmount", FieldValue.increment(1))
                            .addOnFailureListener(e -> Log.e("DATABASE", "Error incrementing bookedTouristsAmount", e));
                    dataCache.clearCache();
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                    Log.e("DATABASE", "Error booking tour", e);
                });
    }

    void setEventDecorator(String startDate, String endDate, int colorCode) {
        if(isAdded()){
            LocalDate startLocalDate = getLocalDate(startDate);
            LocalDate endLocalDate = getLocalDate(endDate);

            if (startLocalDate == null)
                return;

            if(endLocalDate == null){
                Drawable dateDecoratorIndependent = createColoredDrawable(colorCode, R.drawable.date_decorator_independent);
                setDecor(Collections.singletonList(CalendarDay.from(startLocalDate)), dateDecoratorIndependent);
                decoratedDatesList.add(startLocalDate);
                return;
            }

            // Add start, end, and middle dates to the decorated dates list
            LocalDate currentDate = startLocalDate.plusDays(1); // Start with the day after the start date
            List<CalendarDay> centerDates = new ArrayList<>();

            while (currentDate.isBefore(endLocalDate)) {
                decoratedDatesList.add(currentDate);
                centerDates.add(CalendarDay.from(currentDate));
                currentDate = currentDate.plusDays(1);
            }
            decoratedDatesList.add(startLocalDate);
            decoratedDatesList.add(endLocalDate);

            // Create decorators for start, end, and middle dates
            Drawable dateDecoratorStart  = createColoredDrawable(colorCode, R.drawable.date_decorator_start);
            Drawable dateDecoratorEnd = createColoredDrawable(colorCode, R.drawable.date_decorator_end);
            Drawable dateDecoratorCenter  = createColoredDrawable(colorCode, R.drawable.date_decorator_center);


            // Add decorators to the MaterialCalendarView
            if (requireContext().getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                setDecor(Collections.singletonList(CalendarDay.from(startLocalDate)), dateDecoratorStart);
                setDecor(Collections.singletonList(CalendarDay.from(endLocalDate)), dateDecoratorEnd);
            } else {
                setDecor(Collections.singletonList(CalendarDay.from(endLocalDate)), dateDecoratorStart);
                setDecor(Collections.singletonList(CalendarDay.from(startLocalDate)), dateDecoratorEnd);
            }
            setDecor(centerDates, dateDecoratorCenter);
        }
    }

    Drawable createColoredDrawable(int colorCode, int drawableResId) {
        if(isAdded()) {
            Drawable originalDrawable = ContextCompat.getDrawable(requireContext(), drawableResId);
            Drawable.ConstantState constantState = originalDrawable.getConstantState();
            if (constantState != null) {
                Drawable drawable = constantState.newDrawable().mutate();
                drawable.setTint(colorCode);
                return drawable;
            } else
                return null;
        }
        return null;
    }

    void setDecor(List<CalendarDay> calendarDayList, Drawable drawable) {
        calendarView.addDecorators(new EventDecorator(drawable, calendarDayList));
    }

    LocalDate getLocalDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        try {
            Date input = sdf.parse(date);
            Calendar cal = Calendar.getInstance();
            cal.setTime(Objects.requireNonNull(input));
            return LocalDate.of(cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH));
        } catch (ParseException | NullPointerException e) {
            return null;
        }
    }



    /**
     * Fetch user data from Firebase Firestore.
     */
    private void loadDataFromFirebase(View view) {
        // Initialize RecyclerView for Tour Packages

        if (otherUserId != null) {
            tourPackageRecyclerViewAdapter = new TourPackageRecyclerViewAdapter(getActivity(), otherUserId, tourPackagesIdList, false, this);
            recyclerViewTours.setAdapter(tourPackageRecyclerViewAdapter);
            recyclerViewTours.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));


            // Reference to the Firestore document
            DocumentReference userDocumentRef = db.collection("users").document(otherUserId);

            // Retrieve data from Firestore
            userDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (isAdded() && documentSnapshot.exists()) {
                        // Check if the user fields exists in the Firestore document
                        if (documentSnapshot.contains("username"))
                            username.setText(documentSnapshot.getString("username"));

                        if (documentSnapshot.contains("bio"))
                            if(!Objects.equals(documentSnapshot.getString("bio"), "")){
                                bio.setText(documentSnapshot.getString("bio"));
                                bio.setVisibility(View.VISIBLE);
                            }

                        if (documentSnapshot.contains("birthDate"))
                            birthDate.setText(documentSnapshot.getString("birthDate"));

                        if (documentSnapshot.contains("gender")) {
                            String[] genderOptions = requireContext().getResources().getStringArray(R.array.genders);
                            if (Objects.equals(documentSnapshot.getString("gender"), "Male"))
                                gender.setText(genderOptions[1]);
                            else if (Objects.equals(documentSnapshot.getString("gender"), "Female"))
                                gender.setText(genderOptions[2]);
                            else
                                gender.setText(genderOptions[3]);
                        }

                        if (documentSnapshot.contains("type")) {
                            otherUserType = documentSnapshot.getString("type");
                            if (Objects.equals(documentSnapshot.getString("type"), "Tourist")) {
                                type.setText(requireContext().getResources().getString(R.string.tourist));

                            }
                            else {
                                type.setText(requireContext().getResources().getString(R.string.tour_guide));
                            }
                        }

                        // Load the image into the ImageView using Glide
                        storageReference.child("images/" + otherUserId + "/profilePic").getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        // Got the download URL for 'users/me/profile.png'
                                        Glide.with(view)
                                                .load(uri)
                                                .apply(RequestOptions.circleCropTransform())
                                                .into(profilePic);
                                        userDataLoadingOverlay.setVisibility(View.GONE);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        // Handle failure to load image
                                        userDataLoadingOverlay.setVisibility(View.GONE);
                                    }
                                });

                        if(Objects.equals(otherUserType, "Tour Guide")) {
                            // Get the reference to the "tourPackages" collection
                            CollectionReference tourPackagesRef = userDocumentRef.collection("tourPackages");

                            // Retrieve documents from the "tourPackages" collection
                            tourPackagesRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots)
                                        tourPackagesIdList.add(document.getId());
                                    if(tourPackagesIdList.isEmpty())
                                        noAvailableTourPackages.setVisibility(View.VISIBLE);
                                    tourPackageRecyclerViewAdapter.notifyDataSetChanged();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e("DATABASE", "Error getting tourPackages collection", e);
                                }
                            });

                            activeTours = new ArrayList<>();
                            db.collection("users")
                                    .document(otherUserId)
                                    .collection("activeTours")
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                            String endDate = "", startDate = "";
                                            DocumentReference tourPackageRef = null;

                                            // Assuming ActiveTour is a model class representing the active tours
                                            if(document.contains("endDate"))
                                                endDate = document.get("endDate").toString();

                                            if(document.contains("startDate"))
                                                startDate = document.get("startDate").toString();

                                            if(document.contains("tourPackageRef"))
                                                tourPackageRef = document.getDocumentReference("tourPackageRef");

                                            ActiveTour activeTour = new ActiveTour(startDate, endDate, tourPackageRef);
                                            activeTours.add(activeTour);
                                        }

                                        int index = 0; // Initialize an index counter before the loop
                                        int totalTours = activeTours.size(); // Get the total number of tours
                                        if(totalTours == 0)
                                            calendarLoadingOverlay.setVisibility(View.GONE);

                                        // Set up event decorators
                                        for (ActiveTour tour : activeTours) {
                                            final int currentIndex = index++; // Use a final variable to use inside the lambda expression
                                            if (tour.getTourPackageRef() != null) {
                                                // Handle failure to fetch color from Firestore
                                                tour.getTourPackageRef().get().addOnSuccessListener(tourPackageDocumentSnapshot -> {
                                                    if (tourPackageDocumentSnapshot.exists()) {
                                                        int packageColor;
                                                        // Assuming color is stored as an integer in the 'color' field
                                                        if (tourPackageDocumentSnapshot.contains("packageColor")) {
                                                            packageColor = tourPackageDocumentSnapshot.getLong("packageColor").intValue();
                                                            setEventDecorator(tour.getStartDate(), tour.getEndDate(), packageColor);

                                                        }
                                                        else {
                                                            packageColor = requireContext().getResources().getColor(R.color.orange_primary);
                                                            setEventDecorator(tour.getStartDate(), tour.getEndDate(), packageColor);
                                                        }
                                                        // New block to check if this is the last tour
                                                        if (currentIndex == totalTours - 1)
                                                            // This is the last tour, so remove the loading overlay
                                                            calendarLoadingOverlay.setVisibility(View.GONE);
                                                        tour.setColor(packageColor);
                                                    }
                                                }).addOnFailureListener(Throwable::printStackTrace);
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("DATABASE", "Error getting active tours", e);
                                    });

                        }

                        else {
                            bookedToursRefs = new ArrayList<>();
                            db.collection("users")
                                    .document(otherUserId)
                                    .collection("bookedTours")
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                            DocumentReference tourRef = document.getDocumentReference("tourRef");
                                            bookedToursRefs.add(tourRef);
                                        }
                                        int index = 0; // Initialize an index counter before the loop
                                        int totalTours = bookedToursRefs.size(); // Get the total number of tours
                                        if(totalTours == 0)
                                            calendarLoadingOverlay.setVisibility(View.GONE);

                                        // Set up event decorators
                                        for (DocumentReference tourRef : bookedToursRefs) {
                                            final int currentIndex = index++; // Use a final variable to use inside the lambda expression
                                            // Handle failure to fetch color from Firestore
                                            tourRef.get().addOnSuccessListener(tourDocumentSnapshot -> {
                                                if (tourDocumentSnapshot.exists()) {
                                                    DocumentReference tourPackageRef = tourDocumentSnapshot.getDocumentReference("tourPackageRef");
                                                    if (tourPackageRef != null) {
                                                        tourPackageRef.get().addOnSuccessListener(tourPackageDocumentSnapshot -> {
                                                            if (tourPackageDocumentSnapshot.exists()) {
                                                                int packageColor = requireContext().getResources().getColor(R.color.orange_primary);
                                                                String startDate, endDate;
                                                                // Assuming color is stored as an integer in the 'packageColor' field
                                                                if (tourPackageDocumentSnapshot.contains("packageColor")) {
                                                                    packageColor = tourPackageDocumentSnapshot.getLong("packageColor").intValue();
                                                                }

                                                                if (tourDocumentSnapshot.contains("startDate")) {
                                                                    startDate = tourDocumentSnapshot.getString("startDate");
                                                                    if(tourDocumentSnapshot.contains("endDate"))
                                                                        endDate = tourDocumentSnapshot.getString("endDate");
                                                                    else
                                                                        endDate = "";

                                                                    if(Objects.equals(endDate, "")) {
                                                                        if (!getLocalDate(startDate).isBefore(min)) {
                                                                            setEventDecorator(startDate, endDate, packageColor);
                                                                            if (currentIndex == totalTours - 1)
                                                                                // This is the last tour, so remove the loading overlay
                                                                                calendarLoadingOverlay.setVisibility(View.GONE);
                                                                        }
                                                                        else{
                                                                            if(isAdded()) {

                                                                                tourRef.delete().addOnSuccessListener(aVoid -> {
                                                                                            // Refresh the fragment after deleting the tour reference
                                                                                            tourPackagesIdList = new ArrayList<>();

                                                                                            // Remove all existing decorators
                                                                                            calendarView.removeDecorators();
                                                                                            // Clear the list of decorated dates
                                                                                            decoratedDatesList.clear();

                                                                                            loadDataFromFirebase(view);
                                                                                        })
                                                                                        .addOnFailureListener(e -> {
                                                                                            Log.e("DATABASE", "Error deleting tourRef document", e);
                                                                                        });

                                                                                db.collection("users")
                                                                                        .document(otherUserId)
                                                                                        .collection("bookedTours").document(tourRef.getId())
                                                                                        .delete().addOnFailureListener(new OnFailureListener() {
                                                                                            @Override
                                                                                            public void onFailure(@NonNull Exception e) {
                                                                                                Log.e("DATABASE", "Error deleting tour document", e);

                                                                                            }
                                                                                        });
                                                                            }

                                                                        }
                                                                    }
                                                                    else{
                                                                        if(!getLocalDate(endDate).isBefore(min)) {
                                                                            setEventDecorator(startDate, endDate, packageColor);
                                                                            if (currentIndex == totalTours - 1)
                                                                                // This is the last tour, so remove the loading overlay
                                                                                calendarLoadingOverlay.setVisibility(View.GONE);
                                                                        }
                                                                        else {
                                                                            if(isAdded()) {

                                                                                tourRef.delete().addOnSuccessListener(aVoid -> {
                                                                                            // Refresh the fragment after deleting the tour reference
                                                                                            tourPackagesIdList = new ArrayList<>();

                                                                                            // Remove all existing decorators
                                                                                            calendarView.removeDecorators();
                                                                                            // Clear the list of decorated dates
                                                                                            decoratedDatesList.clear();

                                                                                            loadDataFromFirebase(view);
                                                                                        })
                                                                                        .addOnFailureListener(e -> {
                                                                                            Log.e("DATABASE", "Error deleting tourRef document", e);
                                                                                        });

                                                                                db.collection("users")
                                                                                        .document(otherUserId)
                                                                                        .collection("bookedTours").document(tourRef.getId())
                                                                                        .delete().addOnFailureListener(new OnFailureListener() {
                                                                                            @Override
                                                                                            public void onFailure(@NonNull Exception e) {
                                                                                                Log.e("DATABASE", "Error deleting tour document", e);

                                                                                            }
                                                                                        });
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }).addOnFailureListener(e -> {
                                                            // Handle failure to fetch tourPackageRef document
                                                            Log.e("DATABASE", "Error getting tourPackageRef document", e);
                                                        });
                                                    } else {
                                                        Log.e("DATABASE", "tourPackageRef is null");
                                                    }
                                                }
                                                else{
                                                    if(isAdded()) {

                                                        tourRef.delete().addOnSuccessListener(aVoid -> {
                                                                    // Refresh the fragment after deleting the tour reference
                                                                    tourPackagesIdList = new ArrayList<>();

                                                                    // Remove all existing decorators
                                                                    calendarView.removeDecorators();
                                                                    // Clear the list of decorated dates
                                                                    decoratedDatesList.clear();

                                                                    loadDataFromFirebase(view);
                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    Log.e("DATABASE", "Error deleting tourRef document", e);
                                                                });

                                                        db.collection("users")
                                                                .document(otherUserId)
                                                                .collection("bookedTours").document(tourRef.getId())
                                                                .delete().addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        Log.e("DATABASE", "Error deleting tour document", e);

                                                                    }
                                                                });
                                                    }
                                                }
                                            }).addOnFailureListener(e -> {
                                                // Handle failure to fetch tourRef document
                                                Log.e("DATABASE", "Error getting tourRef document", e);
                                            });
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("DATABASE", "Error getting active tours", e);
                                    });
                            // Save Active Tours to the data cache
                            dataCache.put("bookedTours", bookedToursRefs);
                        }

                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DATABASE", "Error getting document", e);
                }
            });
        }
    }

    @Override
    public void onLoadComplete() {

    }

    public void enterChat(String userId1, String userId2, final OnChatCreatedListener listener) {
        // Check if userId1 is in any chat
        db.collection("chats")
                .whereArrayContains("participants", userId1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots1 -> {
                    // Check if userId2 is in any chat
                    db.collection("chats")
                            .whereArrayContains("participants", userId2)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots2 -> {
                                // Check if userId1 and userId2 are both in the same chat
                                for (DocumentSnapshot doc1 : queryDocumentSnapshots1.getDocuments()) {
                                    String chatId1 = doc1.getId();
                                    for (DocumentSnapshot doc2 : queryDocumentSnapshots2.getDocuments()) {
                                        String chatId2 = doc2.getId();
                                        if (chatId1.equals(chatId2)) {
                                            // Chat already exists, retrieve the existing chat and notify listener
                                            Chat existingChat = doc1.toObject(Chat.class);
                                            db.collection("users").document(thisUserId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                @Override
                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                    if(documentSnapshot.contains("username")){
                                                        existingChat.addParticipantName(thisUserId, documentSnapshot.getString("username"));
                                                        existingChat.addParticipantName(otherUserId, username.getText().toString());

                                                        listener.onChatExists(existingChat);
                                                    }
                                                }
                                            });

                                            return; // Exit method
                                        }
                                    }
                                }
                                // If userId1 and userId2 are not in the same chat, create a new chat
                                createNewChatDocument(userId1, userId2, listener);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("DATABASE", "Error checking chat existence for userId2", e);
                                listener.onChatCreateFailed(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("DATABASE", "Error checking chat existence for userId1", e);
                    listener.onChatCreateFailed(e);
                });
    }

    private void createNewChatDocument(String userId1, String userId2, final OnChatCreatedListener listener) {
        // Create a new chat document in Firestore
        Map<String, Object> chatData = new HashMap<>();
        List<String> participants = new ArrayList<>();
        participants.add(userId1);
        participants.add(userId2);
        Timestamp timeStamp = Timestamp.now();
        String lastMessage = "null";
        String lastMessageId = "null";

        chatData.put("participants", participants);
        chatData.put("timeStamp", timeStamp);
        chatData.put("lastMessage", lastMessage);
        chatData.put("lastMessageId", lastMessageId);

        db.collection("chats")
                .add(chatData)
                .addOnSuccessListener(documentReference -> {
                    String chatId = documentReference.getId();

                    // Retrieve the newly created chat document from Firestore
                    db.collection("chats").document(chatId)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                // Get the data from the Firestore document
                                Chat chat = documentSnapshot.toObject(Chat.class);

                                // Notify listener about chat creation
                                listener.onChatCreated(chat);


                            })
                            .addOnFailureListener(e -> {
                                Log.e("DATABASE", "Error fetching chat document", e);
                                listener.onChatCreateFailed(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("DATABASE", "Error creating chat", e);
                    listener.onChatCreateFailed(e);
                });
    }




    public interface OnChatCreatedListener {
        void onChatCreated(Chat chat);
        void onChatExists(Chat chat);
        void onChatCreateFailed(Exception e);



    }

}
