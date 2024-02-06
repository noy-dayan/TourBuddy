package com.tourbuddy.tourbuddy.fragments;

import static com.prolificinteractive.materialcalendarview.MaterialCalendarView.SELECTION_MODE_RANGE;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.archit.calendardaterangepicker.customviews.DateRangeCalendarView;
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
import com.prolificinteractive.materialcalendarview.format.DateFormatTitleFormatter;
import com.prolificinteractive.materialcalendarview.format.TitleFormatter;
import com.squareup.timessquare.CalendarPickerView;
import com.tourbuddy.tourbuddy.R;
import com.tourbuddy.tourbuddy.adapters.TourPackageRecyclerViewAdapter;
import com.tourbuddy.tourbuddy.adapters.UserRecyclerViewAdapter;
import com.tourbuddy.tourbuddy.decorators.EventDecorator;
import com.tourbuddy.tourbuddy.utils.DataCache;
import org.threeten.bp.LocalDate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import android.os.Bundle;



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
            "2024-01-24", "2024-01-25", "2024-01-26", "2024-01-27", "2024-01-28", "2024-01-29");
    final LocalDate min = getLocalDate("2024-01-01");
    final LocalDate max = getLocalDate("2024-12-30");
    final String DATE_FORMAT = "yyyy-MM-dd";

    int pink = 0;
    int gray = 1;

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

        swipeRefreshLayout.setProgressBackgroundColor(R.color.dark_primary);
        swipeRefreshLayout.setColorScheme(R.color.orange_primary);

        calendarView = view.findViewById(R.id.calendarView);

        // Initialize calendar and set its attributes
        calendarView.setShowOtherDates(MaterialCalendarView.SHOW_ALL);
        calendarView.state().edit().setMinimumDate(min).setMaximumDate(max).commit();

        // Set up event decorators
        setEventDecorator(pinkDateList, R.drawable.p_center, R.drawable.p_left, R.drawable.p_right, R.drawable.p_independent);
        setEventDecorator(grayDateList, R.drawable.g_center, R.drawable.g_left, R.drawable.g_right, R.drawable.g_independent);
        // Set the locale for the calendar view title formatter
        calendarView.setTitleFormatter(new DateFormatTitleFormatter());

        mUser = mAuth.getCurrentUser();
        if (mUser != null)
            userId = mUser.getUid();

        // Attempt to load data from cache
        if (!loadDataFromCache(view)) {
            showLoading(true);
            // Data not found in cache, fetch from the server
            loadDataFromFirebase(view);
        }



        // Set up pull-to-refresh functionality
        if (swipeRefreshLayout != null) {
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

    void setEventDecorator(List<String> dateList, int centerDrawable, int leftDrawable, int rightDrawable, int independentDrawable) {
        List<LocalDate> localDateList = convertToDateList(dateList);
        List<CalendarDay> centerDates = new ArrayList<>();
        List<CalendarDay> startDates = new ArrayList<>();
        List<CalendarDay> endDates = new ArrayList<>();
        List<CalendarDay> independentDates = new ArrayList<>();

        for (LocalDate localDate : localDateList) {
            boolean hasLeftNeighbor = localDateList.contains(localDate.minusDays(1));
            boolean hasRightNeighbor = localDateList.contains(localDate.plusDays(1));

            if (hasLeftNeighbor && hasRightNeighbor) {
                centerDates.add(CalendarDay.from(localDate));
            } else if (hasLeftNeighbor) {
                startDates.add(CalendarDay.from(localDate));
            } else if (hasRightNeighbor) {
                endDates.add(CalendarDay.from(localDate));
            } else {
                independentDates.add(CalendarDay.from(localDate));
            }
        }

        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            setDecor(centerDates, centerDrawable);
            setDecor(endDates, leftDrawable);
            setDecor(startDates, rightDrawable);
        } else {
            setDecor(centerDates, centerDrawable);
            setDecor(startDates, leftDrawable);
            setDecor(endDates, rightDrawable);
        }

        setDecor(independentDates, independentDrawable);
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

    void setDecor(List<CalendarDay> calendarDayList, int drawable) {
        calendarView.addDecorators(new EventDecorator(requireContext()
                , drawable
                , calendarDayList));
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
