package com.tourbuddy.tourbuddy.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateLongClickListener;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.OnRangeSelectedListener;
import com.prolificinteractive.materialcalendarview.format.DateFormatTitleFormatter;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.adapters.ReviewRecyclerViewAdapter;
import com.tourbuddy.tourbuddy.adapters.TourPackageRecyclerViewAdapter;
import com.tourbuddy.tourbuddy.decorators.EventDecorator;
import com.tourbuddy.tourbuddy.decorators.RangeSelectionDecorator;
import com.tourbuddy.tourbuddy.utils.ActiveTour;
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
import java.util.UUID;

import com.tourbuddy.tourbuddy.utils.AppUtils;

/**
 * Fragment for displaying user profile information.
 */
public class ThisProfileFragment extends Fragment implements TourPackageRecyclerViewAdapter.OnLoadCompleteListener {
    // DataCache instance for caching user data
    DataCache dataCache;

    // UI elements
    ImageView profilePic;
    TextView username, bio, birthDate, gender, type,
            totalRating, noAvailableReviews, noAvailableTourPackages,
            tourPackagesORbookedToursHeadline, reviewsHeadline;;
    Button btnAddNewTour;
    com.chaek.android.RatingBar ratingBar;

    // Firebase
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore db;
    StorageReference storageReference;

    // SwipeRefreshLayout for pull-to-refresh functionality
    SwipeRefreshLayout swipeRefreshLayout;

    // Loading overlays
    RelativeLayout calendarLoadingOverlay, userDataLoadingOverlay;

    // RecyclerView for Tour Packages
    RecyclerView recyclerViewTours, recyclerViewReviews;
    TourPackageRecyclerViewAdapter tourPackageRecyclerViewAdapter;
    ReviewRecyclerViewAdapter reviewsRecyclerViewAdapter;

    List<String> tourPackagesIdList = new ArrayList<>();
    List<String> reviewsIdList = new ArrayList<>();

    String userId, userType;

    // Tour booking management
    String selectedStartDate = "", selectedEndDate = "";

    final LocalDate min = LocalDate.now(ZoneId.systemDefault()).plusDays(0);
    final LocalDate max = min.plusMonths(6);
    final String DATE_FORMAT = "yyyy-MM-dd";

    List<LocalDate> decoratedDatesList = new ArrayList<>(); // Define this globally
    List<ActiveTour> activeTours;
    List<DocumentReference> bookedToursRefs;
    MaterialCalendarView calendarView;
    ConstraintLayout totalRatingLayout;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_this_profile, container, false);
        if(isAdded()) {
            // Initialize DataCache
            dataCache = DataCache.getInstance();

            // Initialize UI elements
            swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
            profilePic = view.findViewById(R.id.profilePic);
            username = view.findViewById(R.id.username);
            bio = view.findViewById(R.id.bio);
            birthDate = view.findViewById(R.id.birthDate);
            gender = view.findViewById(R.id.gender);
            type = view.findViewById(R.id.type);
            btnAddNewTour = view.findViewById(R.id.btnAddNewTour);

            ratingBar = view.findViewById(R.id.ratingBar);
            totalRating = view.findViewById(R.id.totalRating);
            totalRatingLayout = view.findViewById(R.id.totalRatingLayout);

            noAvailableReviews = view.findViewById(R.id.noAvailableReviews);
            noAvailableTourPackages = view.findViewById(R.id.noAvailableTourPackages);

            tourPackagesORbookedToursHeadline = view.findViewById(R.id.tourPackagesORbookedToursHeadline);
            reviewsHeadline = view.findViewById(R.id.reviewsHeadline);

            // Initialize Firebase instances
            mAuth = FirebaseAuth.getInstance();
            mUser = mAuth.getCurrentUser();
            db = FirebaseFirestore.getInstance();
            FirebaseStorage storage = FirebaseStorage.getInstance();
            storageReference = storage.getReference();

            // Initialize loading overlay elements
            calendarLoadingOverlay = view.findViewById(R.id.calendarLoadingOverlay);
            userDataLoadingOverlay = view.findViewById(R.id.userDataLoadingOverlay);

            recyclerViewReviews = view.findViewById(R.id.recyclerViewReviews);
            recyclerViewTours = view.findViewById(R.id.recyclerViewTours);
            calendarView = view.findViewById(R.id.calendarView);

            mUser = mAuth.getCurrentUser();
            if (mUser != null)
                userId = mUser.getUid();

            // Initialize calendar and set its attributes
            calendarView.setShowOtherDates(MaterialCalendarView.SHOW_ALL);
            calendarView.state().edit().setMinimumDate(min).setMaximumDate(max).commit();

            // Attempt to load data from cache
            if (!loadDataFromCache(view)) {
                calendarLoadingOverlay.setVisibility(View.VISIBLE);
                userDataLoadingOverlay.setVisibility(View.VISIBLE);

                // Data not found in cache, fetch from the server
                loadDataFromFirebase(view);
            }

            DocumentReference userDocumentRef = db.collection("users").document(userId);

            // Retrieve userType from Firestore, duplicated from loadDataFromFirebase in case there's an asynchronous delay
            userDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot thisUserdocumentSnapshot) {
                    if (isAdded() && thisUserdocumentSnapshot.exists()) {
                        // Check if the user type exists in the Firestore document
                        if (thisUserdocumentSnapshot.contains("type")) {
                            userType = thisUserdocumentSnapshot.getString("type");
                            if (Objects.equals(userType, "Tour Guide")) {
                                tourGuide_tourBookingManager();
                                tourGuide_reviewsManager();
                                if (thisUserdocumentSnapshot.contains("language"))
                                     AppUtils.setAppLocale(requireContext(), thisUserdocumentSnapshot.getString("language"));
                            } else {
                                calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_SINGLE);
                                tourist_tourBookingManager();
                                tourist_reviewsManager();

                            }
                            // Set the locale for the calendar view title formatter
                            calendarView.setTitleFormatter(new DateFormatTitleFormatter());
                        }
                    }
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
            } else
                Log.e("ThisProfileFragment", "SwipeRefreshLayout is null");
        }
        return view;

    }


    private void tourGuide_reviewsManager() {
        if(isAdded()) {
            CollectionReference collectionReference = db.collection("users").document(userId).collection("reviews");

            // Clear the list before adding new items
            reviewsIdList.clear();

            collectionReference.get().addOnSuccessListener(queryDocumentSnapshots -> {
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots)
                    // Add document ID to the reviewsIdList
                    reviewsIdList.add(documentSnapshot.getId());

                if (reviewsIdList != null && !reviewsIdList.isEmpty())
                    // Move the current user's review to the top if it exists
                    if (reviewsIdList.contains(userId)) {
                        reviewsIdList.remove(userId);
                        reviewsIdList.add(0, userId);
                    }

                collectionReference.getParent().get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.contains("totalRating")) {
                            if (!reviewsIdList.isEmpty()) {
                                float totalRatingValue = (float) documentSnapshot.getLong("totalRating") / (float) reviewsIdList.size();
                                totalRating.setText(String.format("%.1f", totalRatingValue / 2.0));
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

                if(isAdded()) {
                    // Initialize and set up the adapter after fetching the data
                    reviewsRecyclerViewAdapter = new ReviewRecyclerViewAdapter(getActivity(), requireContext(), reviewsIdList, userId, false);
                    recyclerViewReviews.setAdapter(reviewsRecyclerViewAdapter);
                    recyclerViewReviews.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
                }
            }).addOnFailureListener(e -> {
                // Handle failure
            });
        }
    }
    private void tourist_reviewsManager(){
        if(isAdded()) {
            CollectionReference collectionReference = db.collection("users").document(userId).collection("reviews");

            // Clear the list before adding new items
            reviewsIdList.clear();

            collectionReference.get().addOnSuccessListener(queryDocumentSnapshots -> {
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots)
                    // Add document ID to the reviewsIdList
                    reviewsIdList.add(documentSnapshot.getId());

                if (!reviewsIdList.isEmpty())
                    noAvailableReviews.setVisibility(View.GONE);
                else
                    noAvailableReviews.setVisibility(View.VISIBLE);

                // Notify the adapter if needed
                if (reviewsRecyclerViewAdapter != null) {
                    reviewsRecyclerViewAdapter.notifyDataSetChanged();
                }

                if(isAdded()) {
                    // Initialize and set up the adapter after fetching the data
                    reviewsRecyclerViewAdapter = new ReviewRecyclerViewAdapter(getActivity(), requireContext(), reviewsIdList, userId, true);
                    recyclerViewReviews.setAdapter(reviewsRecyclerViewAdapter);
                    recyclerViewReviews.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
                }
            }).addOnFailureListener(e -> {
                // Handle failure
            });
        }
    }

    private void tourist_tourBookingManager(){
        btnAddNewTour.setVisibility(View.GONE);
        calendarView.setOnDateLongClickListener(new OnDateLongClickListener() {
            @Override
            public void onDateLongClick(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date) {
                LocalDate selectedDate = LocalDate.of(date.getYear(), date.getMonth(), date.getDay());

                for (DocumentReference tourRef : bookedToursRefs) {
                    // Handle failure to fetch color from Firestore
                    tourRef.get().addOnSuccessListener(tourDocumentSnapshot -> {
                        if (tourDocumentSnapshot.exists()) {
                            // Fetch tourPackageName from the referenced tourPackageRef document
                            LocalDate startDate, endDate;
                            if (tourDocumentSnapshot.contains("startDate")) {
                                startDate = getLocalDate(tourDocumentSnapshot.getString("startDate"));
                                if (tourDocumentSnapshot.contains("endDate"))
                                    endDate = getLocalDate(tourDocumentSnapshot.getString("endDate"));
                                else
                                    endDate = startDate;
                                if (selectedDate.equals(startDate) || selectedDate.equals(endDate) ||
                                        (selectedDate.isAfter(startDate) && selectedDate.isBefore(endDate))) {
                                    showConfirmTourCancellation(startDate.toString(), true);
                                }
                            }
                        }

                    }).addOnFailureListener(e -> {
                        // Handle failure to fetch tourRef document
                        Log.e("DATABASE", "Error getting tourRef document", e);
                    });
                }
            }
        });

        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                if (isAdded() && decoratedDatesList.contains(date.getDate())) {
                    // Disable selection for decorated dates
                    selectedStartDate = "";
                    selectedEndDate = "";
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

                                                        // Retrieve data from Firestore
                                                        int groupSizeInt = tourDocumentSnapshot.getLong("groupSize").intValue();
                                                        int bookedTouristsInt = tourDocumentSnapshot.getLong("bookedTouristsAmount").intValue();

                                                        // Populate AlertDialog
                                                        if (endDate != startDate)
                                                            tourDate.setText(startDate.toString() + requireContext().getResources().getString(R.string.to) + endDate.toString());
                                                        else
                                                            tourDate.setText(startDate.toString());

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
                                                                                if(!Objects.equals(userId, documentSnapshot.getId())){
                                                                                    Bundle args = new Bundle();
                                                                                    args.putString("userId", documentSnapshot.getId());
                                                                                    OtherProfileFragment otherProfileFragment = new OtherProfileFragment();
                                                                                    otherProfileFragment.setArguments(args);
                                                                                    AppUtils.switchFragment(ThisProfileFragment.this, otherProfileFragment);

                                                                                }
                                                                                else
                                                                                    AppUtils.switchFragment(ThisProfileFragment.this, new ThisProfileFragment());

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
                                                                Log.d("DATABASE", documentSnapshot.getString("username"));
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

                } else {
                    if (calendarView.getSelectedDate() != null) {
                        selectedStartDate = date.getDate().toString();
                        selectedEndDate = "";
                    } else {
                        selectedStartDate = "";
                        selectedEndDate = "";


                    }

                }
            }
        });
    }

    private void tourGuide_tourBookingManager() {
        if (isAdded()) {
            // Instantiate the custom decorator with your custom drawable for selection
            Drawable selectedDrawable = requireContext().getResources().getDrawable(R.drawable.date_decorator_start);
            Drawable selectedDrawable2 = requireContext().getResources().getDrawable(R.drawable.date_decorator_center);
            Drawable selectedDrawable3 = requireContext().getResources().getDrawable(R.drawable.date_decorator_end);

            RangeSelectionDecorator rangeDecorator = new RangeSelectionDecorator(selectedDrawable, selectedDrawable2, selectedDrawable3);

            // Add the decorator to the MaterialCalendarView
            calendarView.addDecorator(rangeDecorator);


            // Set the date range selection listener
            calendarView.setOnRangeSelectedListener(new OnRangeSelectedListener() {
                @Override
                public void onRangeSelected(@NonNull MaterialCalendarView widget, @NonNull List<CalendarDay> dates) {
                    // Check if any of the selected dates are in the list of decorated dates
                    boolean disableSelection = dates.stream()
                            .map(CalendarDay::getDate)
                            .anyMatch(decoratedDatesList::contains);

                    // Check if any of the selected dates are in the list of decorated dates
                    if (disableSelection || tourPackagesIdList.isEmpty()) {
                        // Disable range selection for decorated dates
                        btnAddNewTour.setEnabled(false);
                        selectedStartDate = "";
                        selectedEndDate = "";
                        calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_RANGE);
                        btnAddNewTour.setText(R.string.add_new_tour);
                    } else {
                        // Clear the existing selection
                        rangeDecorator.clearSelection();

                        // Update the range decorator with the selected dates
                        rangeDecorator.setSelectedDates(dates);

                        // Invalidate the decorators to trigger a redraw
                        calendarView.invalidateDecorators();

                        calendarView.getSelectedDates();


                        selectedStartDate = dates.get(0).getDate().toString();
                        selectedEndDate = dates.get(dates.size() - 1).getDate().toString();

                        if (isAdded()) {
                            btnAddNewTour.setText(requireContext().getResources().getString(R.string.add_new_tour) + "\n" + selectedStartDate + requireContext().getResources().getString(R.string.to) + selectedEndDate);
                            btnAddNewTour.setEnabled(true);
                        }


                    }
                }

            });

            calendarView.setOnDateLongClickListener(new OnDateLongClickListener() {
                @Override
                public void onDateLongClick(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date) {
                    LocalDate selectedDate = LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
                    // Iterate through active tours to find the corresponding tour for the selected date
                    for (ActiveTour tour : activeTours) {
                        LocalDate startDate = getLocalDate(tour.getStartDate());
                        LocalDate endDate;
                        if (tour.getEndDate().isEmpty())
                            endDate = startDate;
                        else
                            endDate = getLocalDate(tour.getEndDate());

                        // Check if the selected date is between the start and end dates of the tour
                        if (selectedDate.equals(startDate) || selectedDate.equals(endDate) ||
                                (selectedDate.isAfter(startDate) && selectedDate.isBefore(endDate)))
                            showConfirmTourCancellation(startDate.toString(), true);
                    }
                }
            });


            calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
                @Override
                public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                    if (isAdded()) {
                        if (decoratedDatesList.contains(date.getDate())) {
                            // Disable selection for decorated dates
                            btnAddNewTour.setEnabled(false);
                            selectedStartDate = "";
                            selectedEndDate = "";
                            calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_RANGE);
                            btnAddNewTour.setText(R.string.add_new_tour);

                            LocalDate selectedDate = LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
                            // Iterate through active tours to find the corresponding tour for the selected date
                            for (ActiveTour tour : activeTours) {
                                LocalDate startDate = getLocalDate(tour.getStartDate());
                                LocalDate endDate;
                                if (tour.getEndDate().isEmpty())
                                    endDate = startDate;
                                else
                                    endDate = getLocalDate(tour.getEndDate());

                                // Check if the selected date is between the start and end dates of the tour
                                if (selectedDate.equals(startDate) || selectedDate.equals(endDate) ||
                                        (selectedDate.isAfter(startDate) && selectedDate.isBefore(endDate))) {
                                    // Print the tour reference to the log
                                    Log.d("ActiveTourReference", "Tour reference: " + tour.getTourPackageRef());

                                    DocumentReference activeTourRef = db.collection("users")
                                            .document(userId)
                                            .collection("activeTours")
                                            .document(tour.getStartDate());


                                    activeTourRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            if (documentSnapshot.exists()) {
                                                if (isAdded()) {
                                                    // Create the dialog
                                                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                                                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_view_tour_details, null);
                                                    builder.setView(dialogView);
                                                    AlertDialog dialog = builder.create();

                                                    TextView tourDate = dialogView.findViewById(R.id.tourDate);
                                                    TextView tourPackage = dialogView.findViewById(R.id.tourPackage);
                                                    TextView groupSize = dialogView.findViewById(R.id.groupSize);
                                                    TextView bookedTouristsAmount = dialogView.findViewById(R.id.bookedTouristsAmount);


                                                    String tourPackageName = tour.getTourPackageRef().getId();
                                                    String startDate = tour.getStartDate();
                                                    String endDate = tour.getEndDate();

                                                    // Retrieve data from Firestore

                                                    int groupSizeInt = documentSnapshot.getLong("groupSize").intValue();
                                                    int bookedTouristsInt = documentSnapshot.getLong("bookedTouristsAmount").intValue();
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
                                                    if (availableSpots == 0) {
                                                        bookedTouristsAmount.setTextColor(getContext().getColor(R.color.red));
                                                    } else if (percentage < 25) {
                                                        bookedTouristsAmount.setTextColor(getContext().getColor(R.color.orange_primary)); // Orange color
                                                    } else if (percentage < 50) {
                                                        bookedTouristsAmount.setTextColor(getContext().getColor(R.color.yellow));
                                                    }

                                                    // Show the dialog
                                                    dialog.show();
                                                }
                                            } else {
                                                Log.d("DATABASE", "No such document");
                                            }
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d("DATABASE", "Error getting document", e);
                                        }
                                    });

                                }
                            }


                        } else {
                            if (calendarView.getSelectedDate() != null) {
                                if(tourPackagesIdList.isEmpty()){
                                    btnAddNewTour.setEnabled(false);
                                    selectedStartDate = "";
                                    selectedEndDate = "";
                                    btnAddNewTour.setText(R.string.add_new_tour);
                                }
                                selectedStartDate = date.getDate().toString();
                                selectedEndDate = "";
                                btnAddNewTour.setText(requireContext().getResources().getString(R.string.add_new_tour) + "\n" + selectedStartDate);
                                btnAddNewTour.setEnabled(true);
                            } else {
                                btnAddNewTour.setEnabled(false);
                                selectedStartDate = "";
                                selectedEndDate = "";
                                btnAddNewTour.setText(R.string.add_new_tour);


                            }

                        }
                    }
                }
            });

            btnAddNewTour.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LocalDate minStartDate = min.plusDays(4); // Minimum start date is 3 days from now
                    if (isAdded()) {
                        // Check if the selected start date is at least 3 days from the current date
                        if (LocalDate.parse(selectedStartDate).isBefore(minStartDate)) {
                            // Display an error message to the user
                            Toast.makeText(requireContext(), requireContext().getResources().getString(R.string.tours_must_be_set_up_at_least_3_days_in_advance), Toast.LENGTH_SHORT).show();
                            return; // Prevent further execution of tour setup
                        }


                        // Create the dialog
                        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_new_tour, null);
                        builder.setView(dialogView);
                        AlertDialog dialog = builder.create();

                        TextView tourDate = dialogView.findViewById(R.id.tourDate);
                        Button btnAddNewTour = dialogView.findViewById(R.id.btnAddNewTour);
                        EditText groupSize = dialogView.findViewById(R.id.groupSize);
                        Spinner spinner = dialogView.findViewById(R.id.tourSpinner);


                        // Initialize spinner
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                                R.layout.spinner_custom_layout, tourPackagesIdList);
                        adapter.setDropDownViewResource(R.layout.spinner_custom_dropdown_layout);
                        spinner.setAdapter(adapter);


                        if (!selectedEndDate.isEmpty())
                            tourDate.setText(requireContext().getResources().getString(R.string.tour_date) + " \n" + selectedStartDate + requireContext().getResources().getString(R.string.to) + selectedEndDate);
                        else
                            tourDate.setText(requireContext().getResources().getString(R.string.tour_date) + " " + selectedStartDate);


                        // Add a TextWatcher for each EditText to enable/disable the button dynamically
                        TextWatcher textWatcher = new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                            }

                            @Override
                            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                            }

                            @Override
                            public void afterTextChanged(Editable editable) {
                                btnAddNewTour.setEnabled(AppUtils.isEditTextFilled(groupSize) && AppUtils.isPositiveNumeric(groupSize));
                            }
                        };

                        groupSize.addTextChangedListener(textWatcher);

                        btnAddNewTour.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Handle adding tour
                                String tourPackageId = spinner.getSelectedItem().toString();
                                int groupSizeValue = Integer.parseInt(groupSize.getText().toString());
                                DocumentReference userTourPackageDocumentRef = db.collection("users")
                                        .document(userId)
                                        .collection("tourPackages")
                                        .document(tourPackageId);

                                // Create a map to store user data
                                Map<String, Object> tourData = new HashMap<>();
                                tourData.put("tourPackageRef", userTourPackageDocumentRef);
                                tourData.put("groupSize", groupSizeValue);
                                tourData.put("bookedTouristsAmount", 0);
                                tourData.put("startDate", selectedStartDate);
                                if (!selectedEndDate.isEmpty())
                                    tourData.put("endDate", selectedEndDate);

                                // Set the document name as the user ID
                                DocumentReference userActiveTourDocumentRef = db.collection("users").
                                        document(userId)
                                        .collection("activeTours")
                                        .document(selectedStartDate);

                                // Set the data to the Firestore document
                                userActiveTourDocumentRef.set(tourData).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d("DATABASE", "DocumentSnapshot added with ID: " + userId);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w("DATABASE", "Error adding document", e);
                                    }
                                });

                                dataCache.clearCache();
                                AppUtils.switchFragment(ThisProfileFragment.this, new ThisProfileFragment());

                                // Close the dialog
                                dialog.dismiss();
                            }
                        });


                        // Show the dialog
                        dialog.show();
                    }
                }
            });
        }
    }

    private void showConfirmTourCancellation(String startDate, boolean isCanceled) {
        LocalDate tourStartDate = getLocalDate(startDate);

        // Check if the tour start date has passed
        if (isAdded() && tourStartDate != null && tourStartDate.isAfter(min.plusDays(3))) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(requireContext().getResources().getString(R.string.confirmTourCancellation));
            builder.setMessage(requireContext().getResources().getString(R.string.confirmTourCancellationMessage));
            builder.setPositiveButton(requireContext().getResources().getString(R.string.cancel_tour), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    cancelTour(startDate, isCanceled);
                }
            });

            builder.setNegativeButton(requireContext().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing, close the dialog
                }
            });

            builder.show();
        } else {
            if(isAdded())
                // Tour is in progress or already passed, display a message indicating cancellation is not allowed
                Toast.makeText(requireContext(), requireContext().getResources().getString(R.string.cancellation_is_not_allowed_for_ongoing_or_close_occurring_tours), Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelTour(String startDate, boolean isCanceled) {
        if(Objects.equals(userType, "Tour Guide")) {
            // Query Firestore for documents with the selected date
            db.collection("users")
                    .document(userId)
                    .collection("activeTours")
                    .whereEqualTo("startDate", startDate) // Assuming date is stored as a string
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            if (!isCanceled) {
                                // Get data from the active tour document
                                Map<String, Object> tourData = documentSnapshot.getData();

                                // Create a reference to the document in the "completedTours" collection
                                db.collection("users")
                                        .document(userId)
                                        .collection("completedTours")
                                        .document(documentSnapshot.getId()) // Use the same document ID
                                        .set(tourData) // Copy data from active tour to completed tour
                                        .addOnSuccessListener(aVoid -> {
                                            // Document successfully copied to completedTours
                                            Log.d("DATABASE", "Active tour copied to completedTours");
                                        })
                                        .addOnFailureListener(e -> {
                                            // Handle errors
                                            Log.e("DATABASE", "Error copying active tour to completedTours", e);
                                        });

                            }
                            if(isAdded()) {
                                // Delete the document from the "activeTours" collection
                                db.collection("users")
                                        .document(userId)
                                        .collection("activeTours")
                                        .document(documentSnapshot.getId())
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            // Document successfully deleted from activeTours
                                            Log.d("DATABASE", "Active tour successfully deleted!");
                                            dataCache.clearCache();
                                            AppUtils.switchFragment(ThisProfileFragment.this, new ThisProfileFragment());
                                        })
                                        .addOnFailureListener(e -> {
                                            // Handle errors
                                            Log.e("DATABASE", "Error deleting active tour document", e);
                                        });
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle errors
                        Log.e("DATABASE", "Error getting active tour documents", e);
                    });
        }
        else {
            // Query Firestore for documents with the selected date
            db.collection("users")
                    .document(userId)
                    .collection("bookedTours")
                    .document(startDate) // Assuming date is stored as a string
                    .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (!isCanceled) {
                                // Get data from the active tour document
                                Map<String, Object> tourData = documentSnapshot.getData();

                                // Create a reference to the document in the "completedTours" collection
                                db.collection("users")
                                        .document(userId)
                                        .collection("completedTours")
                                        .document(documentSnapshot.getId()) // Use the same document ID
                                        .set(tourData) // Copy data from active tour to completed tour
                                        .addOnSuccessListener(aVoid -> {
                                            // Document successfully copied to completedTours
                                            Log.d("DATABASE", "Active tour copied to completedTours");
                                        })
                                        .addOnFailureListener(e -> {
                                            // Handle errors
                                            Log.e("DATABASE", "Error copying active tour to completedTours", e);
                                        });

                            }
                            if(isAdded()) {

                                // Delete the document from the "activeTours" collection
                                db.collection("users")
                                        .document(userId)
                                        .collection("bookedTours")
                                        .document(documentSnapshot.getId())
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            // Document successfully deleted from activeTours
                                            Log.d("DATABASE", "Booked tour successfully deleted!");
                                            DocumentReference tourDocRef = documentSnapshot.getDocumentReference("tourRef");
                                            tourDocRef.update("bookedTouristsAmount", FieldValue.increment(-1))
                                                    .addOnFailureListener(e -> Log.e("DATABASE", "Error decreasing bookedTouristsAmount", e));
                                            dataCache.clearCache();
                                            AppUtils.switchFragment(ThisProfileFragment.this, new ThisProfileFragment());
                                        })
                                        .addOnFailureListener(e -> {
                                            // Handle errors
                                            Log.e("DATABASE", "Error deleting active tour document", e);
                                        });
                            }
                        }

                    }).addOnFailureListener(e -> {
                        // Handle errors
                        Log.e("DATABASE", "Error getting active tour documents", e);
                    });
        }
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

        mUser = mAuth.getCurrentUser();
        if (isAdded() && mUser != null) {
            userId = mUser.getUid();
            // Reference to the Firestore document
            DocumentReference userDocumentRef = db.collection("users").document(userId);


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
                            if (Objects.equals(documentSnapshot.getString("type"), "Tourist")){
                                type.setText(requireContext().getResources().getString(R.string.tourist));
                                userType = "Tourist";
                            }
                            else {
                                type.setText(requireContext().getResources().getString(R.string.tour_guide));
                                userType = "Tour Guide";
                                // Initialize RecyclerView for Tour Packages
                                tourPackageRecyclerViewAdapter = new TourPackageRecyclerViewAdapter(getActivity(), userId, tourPackagesIdList, true, ThisProfileFragment.this);
                                recyclerViewTours.setAdapter(tourPackageRecyclerViewAdapter);
                                recyclerViewTours.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
                            }
                        }


                        // Load the image into the ImageView using Glide
                        storageReference.child("images/" + mUser.getUid() + "/profilePic").getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        // Got the download URL for 'users/me/profile.png'
                                        Glide.with(view)
                                                .load(uri)
                                                .apply(RequestOptions.circleCropTransform())
                                                .into(profilePic);
                                        userDataLoadingOverlay.setVisibility(View.GONE);
                                        saveDataToCache(uri); // Save profile picture URL);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        // Handle failure to load image
                                        userDataLoadingOverlay.setVisibility(View.GONE);
                                        saveDataToCache(null); // Save profile picture URL);
                                    }
                                });

                        if(Objects.equals(userType, "Tour Guide")){
                            // Get the reference to the "tourPackages" collection
                            CollectionReference tourPackagesRef = userDocumentRef.collection("tourPackages");

                            // Retrieve documents from the "tourPackages" collection
                            tourPackagesRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots)
                                        tourPackagesIdList.add(document.getId());
                                    tourPackageRecyclerViewAdapter.notifyDataSetChanged();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e("DATABASE", "Error getting tourPackages collection", e);
                                }
                            }).addOnCompleteListener(task -> calendarLoadingOverlay.setVisibility(View.GONE));

                            activeTours = new ArrayList<>();
                            db.collection("users")
                                    .document(userId)
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

                                            // Cancel the tour if the end date has passed or if the start date has passed (when end date is empty)
                                            if (endDate != null && !endDate.isEmpty()) {
                                                LocalDate tourEndDate = LocalDate.parse(endDate);
                                                if (tourEndDate.isBefore(min)) {
                                                    // The end date of the tour has passed, cancel the tour
                                                    cancelTour(startDate, false);
                                                    continue; // Skip to the next tour
                                                }
                                            } else {
                                                // End date is empty, check if start date has passed
                                                LocalDate tourStartDate = LocalDate.parse(startDate);
                                                if (tourStartDate.isBefore(min)) {
                                                    // The start date of the tour has passed, cancel the tour
                                                    cancelTour(startDate, false);
                                                    continue; // Skip to the next tour
                                                }
                                            }


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
                                                        int packageColor = 0;
                                                        // Assuming color is stored as an integer in the 'color' field
                                                        if (tourPackageDocumentSnapshot.contains("packageColor")) {
                                                            packageColor = tourPackageDocumentSnapshot.getLong("packageColor").intValue();
                                                            setEventDecorator(tour.getStartDate(), tour.getEndDate(), packageColor);
                                                            if (currentIndex == totalTours - 1)
                                                                calendarLoadingOverlay.setVisibility(View.GONE);

                                                        }
                                                        else {
                                                            if (isAdded()){
                                                                packageColor = requireContext().getResources().getColor(R.color.orange_primary);
                                                                setEventDecorator(tour.getStartDate(), tour.getEndDate(), packageColor);
                                                                // Check if this is the last tour and remove loading overlay
                                                                if (currentIndex == totalTours - 1)
                                                                    calendarLoadingOverlay.setVisibility(View.GONE);

                                                            }
                                                        }
                                                        tour.setColor(packageColor);
                                                    }
                                                }).addOnFailureListener(Throwable::printStackTrace);
                                            }
                                        }
                                        // Save Active Tours to the data cache
                                        dataCache.put("activeTours", activeTours);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("DATABASE", "Error getting active tours", e);
                                    });
                        }

                        else {
                            tourPackagesORbookedToursHeadline.setText(R.string.my_booked_tours);
                            bookedToursRefs = new ArrayList<>();
                            db.collection("users")
                                    .document(userId)
                                    .collection("bookedTours")
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                            DocumentReference tourRef = document.getDocumentReference("tourRef");
                                            bookedToursRefs.add(tourRef);
                                        }
                                        // Set up event decorators
                                        int index = 0; // Initialize an index counter before the loop
                                        int totalTours = bookedToursRefs.size(); // Get the total number of tours
                                        if(totalTours == 0)
                                            calendarLoadingOverlay.setVisibility(View.GONE);


                                        for (DocumentReference tourRef : bookedToursRefs) {
                                            final int currentIndex = index++; // Use a final variable to use inside the lambda expression

                                            // Handle failure to fetch color from Firestore
                                            tourRef.get().addOnSuccessListener(tourDocumentSnapshot -> {
                                                if (isAdded() && tourDocumentSnapshot.exists()) {
                                                    DocumentReference tourPackageRef = tourDocumentSnapshot.getDocumentReference("tourPackageRef");
                                                    if (isAdded() && tourPackageRef != null) {
                                                        tourPackageRef.get().addOnSuccessListener(tourPackageDocumentSnapshot -> {
                                                            if (isAdded() && tourPackageDocumentSnapshot.exists()) {
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
                                                                            // Check if this is the last tour and remove loading overlay
                                                                            if (currentIndex == totalTours - 1)
                                                                                calendarLoadingOverlay.setVisibility(View.GONE);

                                                                        }
                                                                        else {
                                                                            if (isAdded()){
                                                                                tourRef.delete().addOnSuccessListener(aVoid -> {
                                                                                            // Refresh the fragment after deleting the tour reference
                                                                                            AppUtils.switchFragment(ThisProfileFragment.this, new ThisProfileFragment());
                                                                                        })
                                                                                        .addOnFailureListener(e -> {
                                                                                            Log.e("DATABASE", "Error deleting tourRef document", e);
                                                                                        });

                                                                                db.collection("users")
                                                                                        .document(userId)
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
                                                                            // Check if this is the last tour and remove loading overlay
                                                                            if (currentIndex == totalTours - 1)
                                                                                calendarLoadingOverlay.setVisibility(View.GONE);

                                                                        }
                                                                        else {
                                                                            if(isAdded()) {

                                                                                tourRef.delete().addOnSuccessListener(aVoid -> {
                                                                                            // Refresh the fragment after deleting the tour reference
                                                                                            AppUtils.switchFragment(ThisProfileFragment.this, new ThisProfileFragment());
                                                                                        })
                                                                                        .addOnFailureListener(e -> {
                                                                                            Log.e("DATABASE", "Error deleting tourRef document", e);
                                                                                        });

                                                                                db.collection("users")
                                                                                        .document(userId)
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
                                                                    AppUtils.switchFragment(ThisProfileFragment.this, new ThisProfileFragment());
                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    Log.e("DATABASE", "Error deleting tourRef document", e);
                                                                });

                                                        db.collection("users")
                                                                .document(userId)
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

    /**
     * Load user data from cache.
     */
    private boolean loadDataFromCache(View view) {
        if(isAdded()) {
            // Check if data exists in memory cache
            calendarLoadingOverlay.setVisibility(View.VISIBLE);
            userDataLoadingOverlay.setVisibility(View.VISIBLE);

            if (dataCache.get("username") != null && dataCache.get("bio") != null
                    && dataCache.get("birthDate") != null && dataCache.get("gender") != null
                    && dataCache.get("type") != null && dataCache.get("userTypeStr") != null) {

                // Load data from memory cache
                username.setText((String) dataCache.get("username"));
                if (!dataCache.get("bio").equals("")) {
                    bio.setText((String) dataCache.get("bio"));
                    bio.setVisibility(View.VISIBLE);
                }

                birthDate.setText((String) dataCache.get("birthDate"));
                gender.setText((String) dataCache.get("gender"));
                type.setText((String) dataCache.get("type"));
                userType = (String) dataCache.get("userTypeStr"); // in case the type is in different language

                // Load the profilePic
                if (dataCache.get("profilePicUrl") != null) {
                    // Load profile picture from memory cache (if available)
                    String profilePicUrl = (String) dataCache.get("profilePicUrl");
                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        Glide.with(view)
                                .load(Uri.parse(profilePicUrl))
                                .apply(RequestOptions.circleCropTransform())
                                .into(profilePic);
                    }
                }
                userDataLoadingOverlay.setVisibility(View.GONE);

                if (Objects.equals(userType, "Tour Guide")) {
                    // Load the list of tour package IDs and set up the RecyclerView
                    tourPackagesIdList = (List<String>) dataCache.get("tourPackagesIdList");
                    if (isAdded() && tourPackagesIdList != null) {
                        tourPackageRecyclerViewAdapter = new TourPackageRecyclerViewAdapter(getActivity(), userId, tourPackagesIdList, true, this);
                        recyclerViewTours.setAdapter(tourPackageRecyclerViewAdapter);
                        recyclerViewTours.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
                    } else
                        return false;

                    // Load the active tours to the calendar
                    if (dataCache.get("activeTours") != null) {
                        activeTours = (List<ActiveTour>) dataCache.get("activeTours");

                        for (ActiveTour tour : activeTours)
                            setEventDecorator(tour.getStartDate(), tour.getEndDate(), tour.getColor());
                        calendarLoadingOverlay.setVisibility(View.GONE);

                    } else
                        return false;


                } else {
                    if (isAdded()) {
                        tourPackagesORbookedToursHeadline.setText(R.string.my_booked_tours);
                        // Load the active tours to the calendar
                        if (dataCache.get("bookedTours") != null) {
                            bookedToursRefs = (List<DocumentReference>) dataCache.get("bookedTours");
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
                                                if (isAdded() && tourPackageDocumentSnapshot.exists()) {
                                                    int packageColor = requireContext().getResources().getColor(R.color.orange_primary);
                                                    String startDate, endDate;
                                                    // Assuming color is stored as an integer in the 'packageColor' field
                                                    if (tourPackageDocumentSnapshot.contains("packageColor")) {
                                                        packageColor = tourPackageDocumentSnapshot.getLong("packageColor").intValue();
                                                    }

                                                    if (tourDocumentSnapshot.contains("startDate")) {
                                                        startDate = tourDocumentSnapshot.getString("startDate");
                                                        if (tourDocumentSnapshot.contains("endDate"))
                                                            endDate = tourDocumentSnapshot.getString("endDate");
                                                        else
                                                            endDate = "";
                                                        setEventDecorator(startDate, endDate, packageColor);
                                                    }

                                                    // New block to check if this is the last tour
                                                    if (currentIndex == totalTours - 1)
                                                        // This is the last tour, so remove the loading overlay
                                                        calendarLoadingOverlay.setVisibility(View.GONE);

                                                }
                                            }).addOnFailureListener(e -> {
                                                // Handle failure to fetch tourPackageRef document
                                                Log.e("DATABASE", "Error getting tourPackageRef document", e);
                                            });
                                        } else
                                            Log.e("DATABASE", "tourPackageRef is null");
                                    }
                                }).addOnFailureListener(e -> {
                                    // Handle failure to fetch tourRef document
                                    Log.e("DATABASE", "Error getting tourRef document", e);
                                });
                            }
                        }
                    }
                }

                return true; // Data loaded from memory cache
            }
        }
        return false; // Data not found in memory cache
    }

    /**
     * Save user data to cache.
     */
    private void saveDataToCache(Uri profilePicUri) {
        if (isAdded()) {
            // Save data to memory cache
            dataCache.put("username", username.getText().toString());
            dataCache.put("bio", bio.getText().toString());
            dataCache.put("birthDate", birthDate.getText().toString());
            dataCache.put("gender", gender.getText().toString());
            dataCache.put("type", type.getText().toString());
            dataCache.put("userTypeStr", userType); // in case the type is in different language

            if (profilePicUri != null)
                dataCache.put("profilePicUrl", profilePicUri.toString());
            if(Objects.equals(userType, "Tour Guide"))
                dataCache.put("tourPackagesIdList", tourPackagesIdList);
        }
    }

    @Override
    public void onLoadComplete() {

    }
}
