package com.tourbuddy.tourbuddy.fragments;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.OnRangeSelectedListener;
import com.prolificinteractive.materialcalendarview.format.DateFormatTitleFormatter;
import com.squareup.timessquare.CalendarPickerView;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.adapters.TourPackageRecyclerViewAdapter;
import com.tourbuddy.tourbuddy.decorators.EventDecorator;
import com.tourbuddy.tourbuddy.decorators.RangeSelectionDecorator;
import com.tourbuddy.tourbuddy.utils.DataCache;
import org.threeten.bp.LocalDate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;


/**
 * Fragment for displaying user profile information.
 */
public class ThisProfileFragment extends Fragment implements TourPackageRecyclerViewAdapter.OnLoadCompleteListener {

    // DataCache instance for caching user data
    DataCache dataCache;
    CalendarPickerView calendar;

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


    final List<String> pinkDateList = Arrays.asList(
            "2024-01-01",
            "2024-01-03", "2024-01-04", "2024-01-05", "2024-01-06");
    final List<String> grayDateList = Arrays.asList(
            "2024-01-09", "2024-01-10", "2024-01-11",
            "2024-01-24", "2024-01-25", "2024-01-26", "2024-01-27", "2024-01-28", "2024-01-29", "2024-01-30", "2024-01-31", "2024-02-01", "2024-02-02");
    final LocalDate min = getLocalDate("2024-01-01");
    final LocalDate max = getLocalDate("2024-12-30");
    final String DATE_FORMAT = "yyyy-MM-dd";

    List<LocalDate> decoratedDatesList = new ArrayList<>(); // Define this globally

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

        // Attempt to load data from cache
        if (!loadDataFromCache(view)) {
            showLoading(true);
            // Data not found in cache, fetch from the server
            loadDataFromFirebase(view);
        }

        loadCalendarInBackground();

        // Instantiate the custom decorator with your custom drawable for selection
        Drawable selectedDrawable = getResources().getDrawable(R.drawable.date_decorator_start);
        Drawable selectedDrawable2 = getResources().getDrawable(R.drawable.date_decorator_center);
        Drawable selectedDrawable3 = getResources().getDrawable(R.drawable.date_decorator_end);

        RangeSelectionDecorator rangeDecorator = new RangeSelectionDecorator(selectedDrawable, selectedDrawable2, selectedDrawable3);

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


                    Log.d("TAG", calendarView.getSelectedDates().toString());
                    btnAddNewTour.setText("Add New Tour\n" + dates.get(0).getDate() + " To " + dates.get(dates.size() - 1).getDate());

                    btnAddNewTour.setEnabled(true);

                }
            }

        });
        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                if (decoratedDatesList.contains(date.getDate())) {
                    // Disable selection for decorated dates
                    btnAddNewTour.setEnabled(false);
                    calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_RANGE);
                    btnAddNewTour.setText("Add New Tour");

                } else {
                    if (calendarView.getSelectedDate() != null) {
                        btnAddNewTour.setEnabled(true);
                        btnAddNewTour.setText("Add New Tour\n" + date.getDate());

                    } else {
                        btnAddNewTour.setEnabled(false);
                        btnAddNewTour.setText("Add New Tour");


                    }

                }
            }
        });


        // Add the decorator to the MaterialCalendarView
        calendarView.addDecorator(rangeDecorator);

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
    private void loadCalendarInBackground() {
        new Thread(() -> {
            // Initialize calendar and set its attributes
            calendarView.setShowOtherDates(MaterialCalendarView.SHOW_ALL);
            calendarView.state().edit().setMinimumDate(min).setMaximumDate(max).commit();

            // Set the locale for the calendar view title formatter
            calendarView.setTitleFormatter(new DateFormatTitleFormatter());

            // Set up event decorators
            setEventDecorator(pinkDateList,-9568312);
            setEventDecorator(grayDateList, -10638632);
        }).start();
    }

    void setEventDecorator(List<String> dateList, int colorCode) {
        List<LocalDate> localDateList = convertToDateList(dateList);
        decoratedDatesList.addAll(localDateList); // Add decorated dates to the list
        List<CalendarDay> centerDates = new ArrayList<>();
        List<CalendarDay> startDates = new ArrayList<>();
        List<CalendarDay> endDates = new ArrayList<>();
        List<CalendarDay> independentDates = new ArrayList<>();

        for (LocalDate localDate : localDateList) {
            boolean hasLeftNeighbor = localDateList.contains(localDate.minusDays(1));
            boolean hasRightNeighbor = localDateList.contains(localDate.plusDays(1));
            if (hasLeftNeighbor && hasRightNeighbor)
                centerDates.add(CalendarDay.from(localDate));
            else if (hasLeftNeighbor)
                startDates.add(CalendarDay.from(localDate));
            else if (hasRightNeighbor)
                endDates.add(CalendarDay.from(localDate));
            else
                independentDates.add(CalendarDay.from(localDate));

        }

        Drawable dateDecoratorIndependent = createColoredDrawable(colorCode, R.drawable.date_decorator_independent);
        Drawable dateDecoratorCenter = createColoredDrawable(colorCode, R.drawable.date_decorator_center);
        Drawable dateDecoratorStart = createColoredDrawable(colorCode, R.drawable.date_decorator_start);
        Drawable dateDecoratorEnd = createColoredDrawable(colorCode, R.drawable.date_decorator_end);


        setDecor(independentDates, dateDecoratorIndependent);
        setDecor(centerDates, dateDecoratorCenter);

        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            setDecor(endDates, dateDecoratorStart);
            setDecor(startDates, dateDecoratorEnd);
        } else {
            setDecor(startDates, dateDecoratorStart);
            setDecor(endDates, dateDecoratorEnd);
        }

    }

    Drawable createColoredDrawable(int colorCode, int drawableResId) {
        Drawable originalDrawable = ContextCompat.getDrawable(requireContext(), drawableResId);
        Drawable.ConstantState constantState = originalDrawable.getConstantState();
        if (constantState != null) {
            Drawable drawable = constantState.newDrawable().mutate();
            drawable.setTint(colorCode);
            return drawable;
        } else {
            return null;
        }
    }

    List<LocalDate> convertToDateList(List<String> dateList) {
        List<LocalDate> localDateList = new ArrayList<>();
        for (String dateString : dateList) {
            LocalDate localDate = getLocalDate(dateString);
            if (localDate != null) {
                localDateList.add(localDate);
            }
        }
        return localDateList;
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

                        // Check if the document contains a collection "tourPackages"
                            Log.d("TAG1", "onSuccess: ");

                            // Get the reference to the "tourPackages" collection
                            CollectionReference tourPackagesRef = userDocumentRef.collection("tourPackages");

                            // Retrieve documents from the "tourPackages" collection
                            tourPackagesRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                        Log.d("TAG2", document.getId());
                                        tourPackagesIdList.add(document.getId());


                                    }
                                    tourPackageRecyclerViewAdapter.notifyDataSetChanged();



                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e("DATABASE", "Error getting tourPackages collection", e);
                                }
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


    @Override
    public void onLoadComplete() {

    }
}
