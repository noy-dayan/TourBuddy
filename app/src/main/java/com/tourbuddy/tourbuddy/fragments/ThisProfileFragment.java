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
import com.squareup.timessquare.CalendarPickerView;
import com.tourbuddy.tourbuddy.R;
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


/**
 * Fragment for displaying user profile information.
 */
public class ThisProfileFragment extends Fragment implements TourPackageRecyclerViewAdapter.OnLoadCompleteListener {
    // DataCache instance for caching user data
    DataCache dataCache;

    // UI elements
    ImageView profilePic;
    TextView username, bio, birthDate, gender, type;
    Button btnAddNewTour;

    // Firebase
    FirebaseAuth mAuth;
    FirebaseUser mUser;
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

    // Tour booking management
    String selectedStartDate = "", selectedEndDate = "";

    final LocalDate min = LocalDate.now(ZoneId.systemDefault());
    final LocalDate max = min.plusMonths(6);
    final String DATE_FORMAT = "yyyy-MM-dd";

    List<LocalDate> decoratedDatesList = new ArrayList<>(); // Define this globally
    List<ActiveTour> activeTours;
    MaterialCalendarView calendarView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_this_profile, container, false);

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

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Initialize loading overlay elements
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerViewTours = view.findViewById(R.id.recyclerViewTours);
        calendarView = view.findViewById(R.id.calendarView);

        mUser = mAuth.getCurrentUser();
        if (mUser != null)
            userId = mUser.getUid();

        // Initialize calendar and set its attributes
        calendarView.setShowOtherDates(MaterialCalendarView.SHOW_ALL);
        calendarView.state().edit().setMinimumDate(min).setMaximumDate(max).commit();

        // Set the locale for the calendar view title formatter
        calendarView.setTitleFormatter(new DateFormatTitleFormatter());

        // Attempt to load data from cache
        if (!loadDataFromCache(view)) {
            showLoading(true);
            // Data not found in cache, fetch from the server
            loadDataFromFirebase(view);
        }

        tourBookingManager();

        // Set up pull-to-refresh functionality
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setProgressBackgroundColor(R.color.dark_primary);
            swipeRefreshLayout.setColorScheme(R.color.orange_primary);

            swipeRefreshLayout.setOnRefreshListener(() -> {
                tourPackagesIdList = new ArrayList<>();
                loadDataFromFirebase(view);
                swipeRefreshLayout.setRefreshing(false);
            });
        } else {
            Log.e("ThisProfileFragment", "SwipeRefreshLayout is null");
        }


        return view;

    }


    private void tourBookingManager(){
        // Instantiate the custom decorator with your custom drawable for selection
        Drawable selectedDrawable = getResources().getDrawable(R.drawable.date_decorator_start);
        Drawable selectedDrawable2 = getResources().getDrawable(R.drawable.date_decorator_center);
        Drawable selectedDrawable3 = getResources().getDrawable(R.drawable.date_decorator_end);

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
                if (disableSelection) {
                    // Disable range selection for decorated dates
                    btnAddNewTour.setEnabled(false);
                    selectedStartDate = "";
                    selectedEndDate = "";
                    calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_RANGE);
                    btnAddNewTour.setText("Add New Tour");
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
                    btnAddNewTour.setText("Add New Tour\n" + selectedStartDate + " To " + selectedEndDate);
                    btnAddNewTour.setEnabled(true);

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
                    if(tour.getEndDate().isEmpty())
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
                if (decoratedDatesList.contains(date.getDate())) {
                    // Disable selection for decorated dates
                    btnAddNewTour.setEnabled(false);
                    selectedStartDate = "";
                    selectedEndDate = "";
                    calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_RANGE);
                    btnAddNewTour.setText("Add New Tour");

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
                        }
                    }


                } else {
                    if (calendarView.getSelectedDate() != null) {
                        selectedStartDate = date.getDate().toString();
                        selectedEndDate = "";
                        btnAddNewTour.setText("Add New Tour\n" + selectedStartDate);
                        btnAddNewTour.setEnabled(true);
                    } else {
                        btnAddNewTour.setEnabled(false);
                        selectedStartDate = "";
                        selectedEndDate = "";
                        btnAddNewTour.setText("Add New Tour");


                    }

                }
            }
        });




        btnAddNewTour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

                if(!selectedEndDate.isEmpty())
                    tourDate.setText("Tour Date: \n" + selectedStartDate +" To " + selectedEndDate);
                else
                    tourDate.setText("Tour Date: " + selectedStartDate);

                // Add a TextWatcher for each EditText to enable/disable the button dynamically
                TextWatcher textWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void afterTextChanged(Editable editable) {
                        btnAddNewTour.setEnabled(isEditTextFilled(groupSize) && isPositiveNumeric(groupSize));
                    }
                };

                groupSize.addTextChangedListener(textWatcher);

                btnAddNewTour.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Handle adding tour here
                        String tourPackageId = spinner.getSelectedItem().toString();
                        String groupSizeValue = groupSize.getText().toString();
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
                        if(!selectedEndDate.isEmpty())
                            tourData.put("endDate", selectedEndDate);

                        // Set the document name as the user ID
                        DocumentReference userActiveTourDocumentRef = db.collection("users").
                                document(userId)
                                .collection("activeTours")
                                .document(UUID.randomUUID().toString());

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
                        switchFragment(new ThisProfileFragment());
                        // Close the dialog
                        dialog.dismiss();
                    }
                });

                // Show the dialog
                dialog.show();
            }
        });
    }

    private void showConfirmTourCancellation(String startDate, boolean isCancled) {
        LocalDate tourStartDate = getLocalDate(startDate);

        // Check if the tour start date has passed
        if (tourStartDate != null && tourStartDate.isAfter(min.plusDays(1))) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(requireContext().getResources().getString(R.string.confirmTourCancellation));
            builder.setMessage(requireContext().getResources().getString(R.string.confirmTourCancellationMessage));
            builder.setPositiveButton(requireContext().getResources().getString(R.string.cancel_tour), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    cancelTour(startDate, isCancled);
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
            // Tour is in progress or already passed, display a message indicating cancellation is not allowed
            Toast.makeText(requireContext(), "Cancellation is not allowed for ongoing tours.", Toast.LENGTH_SHORT).show();
        }
    }
    private void cancelTour(String startDate, boolean isCancled) {
        // Query Firestore for documents with the selected date
        db.collection("users")
                .document(userId)
                .collection("activeTours")
                .whereEqualTo("startDate", startDate) // Assuming date is stored as a string
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        if(!isCancled){
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
                                        Log.d("Firestore", "Active tour copied to completedTours");
                                    })
                                    .addOnFailureListener(e -> {
                                        // Handle errors
                                        Log.e("Firestore", "Error copying active tour to completedTours", e);
                                    });

                        }

                        // Delete the document from the "activeTours" collection
                        db.collection("users")
                                .document(userId)
                                .collection("activeTours")
                                .document(documentSnapshot.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    // Document successfully deleted from activeTours
                                    Log.d("Firestore", "Active tour successfully deleted!");
                                    dataCache.clearCache();
                                    switchFragment(new ThisProfileFragment());
                                })
                                .addOnFailureListener(e -> {
                                    // Handle errors
                                    Log.e("Firestore", "Error deleting active tour document", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle errors
                    Log.e("Firestore", "Error getting active tour documents", e);
                });
    }

    private boolean isEditTextFilled(EditText editText) {
        return editText.getText() != null && editText.getText().length() > 0;
    }

    private boolean isPositiveNumeric(EditText editText) {
        String text = editText.getText().toString().trim();
        try {
            int number = Integer.parseInt(text);
            return number > 0; // Checks if the input is a positive numeric value
        } catch (NumberFormatException e) {
            return false; // Handles the case where the input is not a valid integer
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

        mUser = mAuth.getCurrentUser();
        if (mUser != null) {
            userId = mUser.getUid();
            tourPackageRecyclerViewAdapter = new TourPackageRecyclerViewAdapter(getActivity(), userId, tourPackagesIdList, true, this);
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

                        if (documentSnapshot.contains("bio")) {
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
                        storageReference.child("images/" + mUser.getUid() + "/profilePic").getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        // Got the download URL for 'users/me/profile.png'
                                        Glide.with(view)
                                                .load(uri)
                                                .apply(RequestOptions.circleCropTransform())
                                                .into(profilePic);
                                        showLoading(false);
                                        saveDataToCache(uri); // Save profile picture URL);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        // Handle failure to load image
                                        showLoading(false);
                                        saveDataToCache(null); // Save profile picture URL);
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
                                    // Save Active Tours to the data cache
                                    dataCache.put("activeTours", activeTours);
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
     * Load user data from cache.
     */
    private boolean loadDataFromCache(View view) {
        // Check if data exists in memory cache
        showLoading(true);
        if (dataCache.get("username") != null && dataCache.get("bio") != null
                && dataCache.get("birthDate") != null && dataCache.get("gender") != null
                && dataCache.get("type") != null) {

            // Load data from memory cache
            username.setText((String) dataCache.get("username"));
            if(!dataCache.get("bio").equals("")){
                bio.setText((String) dataCache.get("bio"));
                bio.setVisibility(View.VISIBLE);
            }
            birthDate.setText((String) dataCache.get("birthDate"));
            gender.setText((String) dataCache.get("gender"));
            type.setText((String) dataCache.get("type"));

            // Load the list of tour package IDs and set up the RecyclerView
            tourPackagesIdList = (List<String>) dataCache.get("tourPackagesIdList");
            if (tourPackagesIdList != null) {
                tourPackageRecyclerViewAdapter = new TourPackageRecyclerViewAdapter(getActivity(), userId, tourPackagesIdList, true, this);
                recyclerViewTours.setAdapter(tourPackageRecyclerViewAdapter);
                recyclerViewTours.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            }
            else
                return false;

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

            // Load the active tours to the calendar
            if(dataCache.get("activeTours") != null) {
                activeTours = (List<ActiveTour>) dataCache.get("activeTours");
                for (ActiveTour tour : activeTours)
                    setEventDecorator(tour.getStartDate(), tour.getEndDate(), tour.getColor());
            }
            else
                return false;

            showLoading(false);
            return true; // Data loaded from memory cache
        }
        showLoading(false);
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
            if (profilePicUri != null)
                dataCache.put("profilePicUrl", profilePicUri.toString());
            dataCache.put("tourPackagesIdList", tourPackagesIdList);
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

    /**
     * Switch fragment function.
     */
    private void switchFragment(Fragment fragment) {
        if (isAdded())
            // Replace the current fragment with the new one
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .commit();
    }

    @Override
    public void onLoadComplete() {

    }
}
