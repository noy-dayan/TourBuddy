package com.tourbuddy.tourbuddy.adapters;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.tourbuddy.tourbuddy.fragments.TourPackageCreationFragment;
import com.tourbuddy.tourbuddy.fragments.TourPackageViewFragment;

import java.util.List;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class TourPackageRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_PACKAGE = 1;
    private static final int TYPE_FOOTER = 2;
    private static final int MAIN_IMAGE_CORNER_RADIUS = 70;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();

    Activity activity;
    String userId;
    List<String> tourPackagesIdList;

    OnLoadCompleteListener onLoadCompleteListener;

    boolean showFooter;

    public TourPackageRecyclerViewAdapter(Activity activity, String userId, List<String> tourPackagesIdList, boolean showFooter, OnLoadCompleteListener onLoadCompleteListener) {
        this.tourPackagesIdList = tourPackagesIdList;
        this.userId = userId;
        this.activity = activity;
        this.onLoadCompleteListener = onLoadCompleteListener;
        this.showFooter = showFooter;

    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_PACKAGE) {
            View itemView = inflater.inflate(R.layout.recyclerview_tour_package_item_layout, parent, false);
            return new PackageViewHolder(itemView, this);
        } else {
            View footerView = inflater.inflate(R.layout.recyclerview_tour_package_item_footer_layout, parent, false);
            return new FooterViewHolder(footerView, this);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof PackageViewHolder) {
            String tourPackageId = tourPackagesIdList.get(position);
            loadDataFromFirebase(userId, tourPackageId, (PackageViewHolder) holder, position);
        }
    }

    @Override
    public int getItemCount() {
        return showFooter ? tourPackagesIdList.size() + 1 : tourPackagesIdList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return (showFooter && position == tourPackagesIdList.size()) ? TYPE_FOOTER : TYPE_PACKAGE;
    }

    // ViewHolder for Tour Package Item
    public static class PackageViewHolder extends RecyclerView.ViewHolder {
        ImageView packageImage;
        TextView packageName;
        TourPackageRecyclerViewAdapter tourPackageRecyclerViewAdapter;
        public PackageViewHolder(@NonNull View itemView, TourPackageRecyclerViewAdapter tourPackageRecyclerViewAdapter) {
            super(itemView);
            packageImage = itemView.findViewById(R.id.packageImage);
            packageName = itemView.findViewById(R.id.packageNameInput);
            this.tourPackageRecyclerViewAdapter = tourPackageRecyclerViewAdapter;

            packageImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        String packageName = tourPackageRecyclerViewAdapter.tourPackagesIdList.get(position);
                        String userId = tourPackageRecyclerViewAdapter.userId;

                        TourPackageViewFragment tourPackageViewFragment = new TourPackageViewFragment();
                        Bundle args = new Bundle();
                        args.putString("userId", userId);
                        args.putString("packageName", packageName);
                        tourPackageViewFragment.setArguments(args);

                        // Call the non-static switchFragment method on the UserRecyclerViewAdapter instance
                        tourPackageRecyclerViewAdapter.switchFragment(tourPackageViewFragment);
                        //tourPackageRecyclerViewAdapter.tourPackagesIdList.clear();
                    }
                }
            });
        }


    }

    // ViewHolder for Footer Item
    public static class FooterViewHolder extends RecyclerView.ViewHolder {
        ImageButton btnAddTourPackage;
        TourPackageRecyclerViewAdapter tourPackageRecyclerViewAdapter;

        public FooterViewHolder(@NonNull View itemView, TourPackageRecyclerViewAdapter tourPackageRecyclerViewAdapter) {
            super(itemView);
            btnAddTourPackage = itemView.findViewById(R.id.btnAddTourPackage);
            this.tourPackageRecyclerViewAdapter = tourPackageRecyclerViewAdapter;

            btnAddTourPackage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tourPackageRecyclerViewAdapter.switchFragment(new TourPackageCreationFragment());
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
    private void loadDataFromFirebase(String userId, String tourPackageId, @NonNull PackageViewHolder holder, int position) {
        if (userId != null) {
            DocumentReference userTourPackageDocumentRef = db.collection("users").document(userId)
                    .collection("tourPackages").document(tourPackageId);

            userTourPackageDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        holder.packageName.setText(documentSnapshot.getId());
                        if(documentSnapshot.get("packageColor") != null)
                            holder.packageName.setTextColor(documentSnapshot.getLong("packageColor").intValue());
                        storageReference.child("images/" + userId + "/tourPackages/" + documentSnapshot.getId() + "/packageCoverImage")
                                .getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        Glide.with(holder.itemView.getContext())
                                                .load(uri)
                                                .apply(RequestOptions.centerCropTransform())
                                                .apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(MAIN_IMAGE_CORNER_RADIUS, 0)))
                                                .into(holder.packageImage);

                                        // Check if it's the last item
                                        if (position == tourPackagesIdList.size() - 1)
                                            // Trigger the callback when all elements are loaded
                                            onLoadCompleteListener.onLoadComplete();

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        Log.e("Glide", "Error loading image", exception);
                                    }
                                });
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("DATABASE", "Error getting document for tour package ID: " + tourPackageId, e);
                }
            });
        }
    }

    public interface OnLoadCompleteListener {
        void onLoadComplete();
    }
}
