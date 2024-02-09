package com.tourbuddy.tourbuddy.fragments;

import android.app.AlertDialog;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
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
import com.tourbuddy.tourbuddy.adapters.TourPackageRecyclerViewAdapter;
import com.tourbuddy.tourbuddy.decorators.EventDecorator;
import com.tourbuddy.tourbuddy.decorators.RangeSelectionDecorator;
import com.tourbuddy.tourbuddy.utils.ActiveTour;
import com.tourbuddy.tourbuddy.utils.AppUtils;
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

public class OtherProfileFragment extends Fragment implements TourPackageRecyclerViewAdapter.OnLoadCompleteListener {


    // UI elements
    ImageView profilePic;
    TextView username, bio, birthDate, gender, type;

    // Firebase
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    StorageReference storageReference;

    // SwipeRefreshLayout for pull-to-refresh functionality
    SwipeRefreshLayout swipeRefreshLayout;

    // Loading overlay
    View loadingOverlay;
    ProgressBar progressBar;

    // RecyclerView for Tour Packages
    RecyclerView recyclerViewTours;
    TourPackageRecyclerViewAdapter tourPackageRecyclerViewAdapter;
    List<String> tourPackagesIdList = new ArrayList<>();
    String userId;

    final LocalDate min = LocalDate.now(ZoneId.systemDefault());
    final LocalDate max = min.plusMonths(6);
    final String DATE_FORMAT = "yyyy-MM-dd";

    List<LocalDate> decoratedDatesList = new ArrayList<>(); // Define this globally
    List<ActiveTour> activeTours;
    MaterialCalendarView calendarView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_other_profile, container, false);

        // Initialize UI elements
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        profilePic = view.findViewById(R.id.profilePic);
        username = view.findViewById(R.id.username);
        bio = view.findViewById(R.id.bio);
        birthDate = view.findViewById(R.id.birthDate);
        gender = view.findViewById(R.id.gender);
        type = view.findViewById(R.id.type);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Initialize loading overlay elements
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerViewTours = view.findViewById(R.id.recyclerViewTours);
        calendarView = view.findViewById(R.id.calendarView);

        swipeRefreshLayout.setProgressBackgroundColor(R.color.dark_primary);
        swipeRefreshLayout.setColorScheme(R.color.orange_primary);

        // Retrieve user ID from arguments
        if (getArguments() != null) {
            userId = getArguments().getString("userId");
        }

        // Initialize calendar and set its attributes
        calendarView.setShowOtherDates(MaterialCalendarView.SHOW_ALL);
        calendarView.state().edit().setMinimumDate(min).setMaximumDate(max).commit();

        // Set the locale for the calendar view title formatter
        calendarView.setTitleFormatter(new DateFormatTitleFormatter());

        showLoading(true);
        loadDataFromFirebase(view);

        tourBookingManager();

        // Set up pull-to-refresh functionality
        if (swipeRefreshLayout != null) {
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



    private void tourBookingManager(){
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
                                            Button btnBookTour = dialogView.findViewById(R.id.btnBookTour);
                                            btnBookTour.setVisibility(View.VISIBLE);


                                            String tourPackageName = tour.getTourPackageRef().getId();
                                            String startDate = tour.getStartDate();
                                            String endDate = tour.getEndDate();

                                            // Retrieve data from Firestore

                                            int groupSizeInt = documentSnapshot.getLong("groupSize").intValue();
                                            int bookedTouristsInt = documentSnapshot.getLong("bookedTouristsAmount").intValue();
                                            // Populate AlertDialog

                                            if (endDate != null)
                                                tourDate.setText(startDate + " To " + endDate);
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

                                            if (availableSpots == 0) {
                                                btnBookTour.setEnabled(false);
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


                }
            }
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
            if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
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

        if (userId != null) {
            tourPackageRecyclerViewAdapter = new TourPackageRecyclerViewAdapter(getActivity(), userId, tourPackagesIdList, false, this);
            recyclerViewTours.setAdapter(tourPackageRecyclerViewAdapter);
            recyclerViewTours.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));


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
                            if (Objects.equals(documentSnapshot.getString("type"), "Tourist"))
                                type.setText(getResources().getString(R.string.tourist));
                            else
                                type.setText(getResources().getString(R.string.tour_guide));
                        }

                        // Load the image into the ImageView using Glide
                        storageReference.child("images/" + userId + "/profilePic").getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        // Got the download URL for 'users/me/profile.png'
                                        Glide.with(view)
                                                .load(uri)
                                                .apply(RequestOptions.circleCropTransform())
                                                .into(profilePic);
                                        showLoading(false);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        // Handle failure to load image
                                        showLoading(false);
                                    }
                                });

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
                        });

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

                                        ActiveTour activeTour = new ActiveTour(startDate, endDate, tourPackageRef);
                                        activeTours.add(activeTour);
                                    }


                                    // Set up event decorators
                                    for (ActiveTour tour : activeTours) {
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
                                                        packageColor = getResources().getColor(R.color.orange_primary);
                                                        setEventDecorator(tour.getStartDate(), tour.getEndDate(), packageColor);
                                                    }
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
     * Show/hide the loading screen.
     */
    private void showLoading(boolean show) {
        if (isAdded()) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onLoadComplete() {

    }
}
