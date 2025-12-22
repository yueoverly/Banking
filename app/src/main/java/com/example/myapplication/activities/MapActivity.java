package com.example.myapplication.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.myapplication.R;
import com.example.myapplication.models.BankBranch;
import com.example.myapplication.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Activity showing map with bank branches and navigation
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_CODE = 101;
    private static final float DEFAULT_ZOOM = 14f;

    private Toolbar toolbar;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private ProgressBar progressBar;

    // Bottom sheet views
    private LinearLayout bottomSheet;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private RecyclerView rvBranches;
    private TextView tvNearestBranch, tvBranchName, tvBranchAddress, tvBranchDistance;
    private TextView tvBranchPhone, tvBranchHours;
    private MaterialButton btnNavigate, btnCall;

    private FirebaseHelper firebaseHelper;
    private List<BankBranch> branchList;
    private BranchAdapter branchAdapter;
    
    private Location currentLocation;
    private BankBranch selectedBranch;
    private BankBranch nearestBranch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        initViews();
        setupToolbar();
        setupMap();
        setupBottomSheet();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progress_bar);
        bottomSheet = findViewById(R.id.bottom_sheet);
        rvBranches = findViewById(R.id.rv_branches);
        tvNearestBranch = findViewById(R.id.tv_nearest_branch);
        tvBranchName = findViewById(R.id.tv_branch_name);
        tvBranchAddress = findViewById(R.id.tv_branch_address);
        tvBranchDistance = findViewById(R.id.tv_branch_distance);
        tvBranchPhone = findViewById(R.id.tv_branch_phone);
        tvBranchHours = findViewById(R.id.tv_branch_hours);
        btnNavigate = findViewById(R.id.btn_navigate);
        btnCall = findViewById(R.id.btn_call);

        firebaseHelper = FirebaseHelper.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        branchList = new ArrayList<>();

        btnNavigate.setOnClickListener(v -> navigateToSelectedBranch());
        btnCall.setOnClickListener(v -> callSelectedBranch());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chi nhánh & ATM");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setPeekHeight(200);

        // Setup RecyclerView for branches
        branchAdapter = new BranchAdapter();
        rvBranches.setLayoutManager(new LinearLayoutManager(this));
        rvBranches.setAdapter(branchAdapter);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // Map settings
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Marker click listener
        googleMap.setOnMarkerClickListener(marker -> {
            BankBranch branch = (BankBranch) marker.getTag();
            if (branch != null) {
                selectBranch(branch);
            }
            return false;
        });

        if (checkLocationPermission()) {
            enableMyLocation();
            getCurrentLocation();
        } else {
            requestLocationPermission();
        }

        loadBranches();
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Cần cấp quyền vị trí để hiển thị chi nhánh gần nhất",
                        Toast.LENGTH_LONG).show();
                // Show default location (Ho Chi Minh City)
                LatLng hcmCity = new LatLng(10.7769, 106.7009);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hcmCity, DEFAULT_ZOOM));
            }
        }
    }

    private void enableMyLocation() {
        if (googleMap != null && checkLocationPermission()) {
            try {
                googleMap.setMyLocationEnabled(true);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    private void getCurrentLocation() {
        if (!checkLocationPermission()) return;

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            currentLocation = location;
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                            updateBranchDistances();
                        } else {
                            // Default to Ho Chi Minh City
                            LatLng hcmCity = new LatLng(10.7769, 106.7009);
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hcmCity, DEFAULT_ZOOM));
                        }
                    })
                    .addOnFailureListener(e -> {
                        LatLng hcmCity = new LatLng(10.7769, 106.7009);
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hcmCity, DEFAULT_ZOOM));
                    });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void loadBranches() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseHelper.getBankBranches(
                querySnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    branchList.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        BankBranch branch = doc.toObject(BankBranch.class);
                        if (branch != null) {
                            branch.setId(doc.getId());
                            branchList.add(branch);
                            addBranchMarker(branch);
                        }
                    }

                    // If no branches from Firebase, add sample data
                    if (branchList.isEmpty()) {
                        addSampleBranches();
                    }

                    branchAdapter.notifyDataSetChanged();
                    updateBranchDistances();
                },
                e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải danh sách chi nhánh", Toast.LENGTH_SHORT).show();
                    addSampleBranches();
                }
        );
    }

    private void addSampleBranches() {
        // Add sample branches in Ho Chi Minh City
        String[][] sampleData = {
                {"TDTU Bank - Hội sở chính", "19 Nguyễn Hữu Thọ, Tân Phong, Quận 7", "10.7326", "106.6977", "1900 1234", "08:00 - 17:00"},
                {"TDTU Bank - Chi nhánh Quận 1", "123 Nguyễn Huệ, Quận 1", "10.7731", "106.7010", "1900 1234", "08:00 - 17:00"},
                {"TDTU Bank - Chi nhánh Quận 3", "456 Điện Biên Phủ, Quận 3", "10.7833", "106.6850", "1900 1234", "08:00 - 17:00"},
                {"TDTU Bank - Chi nhánh Bình Thạnh", "789 Điện Biên Phủ, Bình Thạnh", "10.8000", "106.7100", "1900 1234", "08:00 - 17:00"},
                {"TDTU Bank - Chi nhánh Tân Bình", "321 Cộng Hòa, Tân Bình", "10.8050", "106.6520", "1900 1234", "08:00 - 17:00"}
        };

        for (String[] data : sampleData) {
            BankBranch branch = new BankBranch();
            branch.setName(data[0]);
            branch.setAddress(data[1]);
            branch.setLatitude(Double.parseDouble(data[2]));
            branch.setLongitude(Double.parseDouble(data[3]));
            branch.setPhoneNumber(data[4]);
            branch.setWorkingHours(data[5]);
            branch.setHasATM(true);

            branchList.add(branch);
            addBranchMarker(branch);
        }

        branchAdapter.notifyDataSetChanged();
    }

    private void addBranchMarker(BankBranch branch) {
        if (googleMap == null) return;

        LatLng position = new LatLng(branch.getLatitude(), branch.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .title(branch.getName())
                .snippet(branch.getAddress())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        Marker marker = googleMap.addMarker(markerOptions);
        if (marker != null) {
            marker.setTag(branch);
        }
    }

    private void updateBranchDistances() {
        if (currentLocation == null || branchList.isEmpty()) return;

        // Calculate distance for each branch
        for (BankBranch branch : branchList) {
            float[] results = new float[1];
            Location.distanceBetween(
                    currentLocation.getLatitude(), currentLocation.getLongitude(),
                    branch.getLatitude(), branch.getLongitude(),
                    results
            );
            branch.setDistanceFromUser(results[0]); // Store in meters
        }

        // Sort by distance
        Collections.sort(branchList, (b1, b2) -> Double.compare(b1.getDistanceFromUser(), b2.getDistanceFromUser()));
        branchAdapter.notifyDataSetChanged();

        // Find and show nearest branch
        if (!branchList.isEmpty()) {
            nearestBranch = branchList.get(0);
            selectBranch(nearestBranch);
            tvNearestBranch.setVisibility(View.VISIBLE);
        }
    }

    private void selectBranch(BankBranch branch) {
        selectedBranch = branch;

        tvBranchName.setText(branch.getName());
        tvBranchAddress.setText(branch.getAddress());
        
        if (branch.getDistanceFromUser() > 0) {
            tvBranchDistance.setText(branch.getFormattedDistance());
            tvBranchDistance.setVisibility(View.VISIBLE);
        } else {
            tvBranchDistance.setVisibility(View.GONE);
        }

        tvBranchPhone.setText(branch.getPhoneNumber());
        tvBranchHours.setText(branch.getWorkingHours());

        // Expand bottom sheet
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        // Move camera to selected branch
        LatLng position = new LatLng(branch.getLatitude(), branch.getLongitude());
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 16f));
    }

    private void navigateToSelectedBranch() {
        if (selectedBranch == null) return;

        // Open Google Maps for navigation
        String uri = String.format("google.navigation:q=%f,%f&mode=w",
                selectedBranch.getLatitude(), selectedBranch.getLongitude());
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            // Open in browser if Google Maps not installed
            String browserUri = String.format("https://www.google.com/maps/dir/?api=1&destination=%f,%f&travelmode=walking",
                    selectedBranch.getLatitude(), selectedBranch.getLongitude());
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(browserUri)));
        }
    }

    private void callSelectedBranch() {
        if (selectedBranch == null || selectedBranch.getPhoneNumber() == null) return;

        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + selectedBranch.getPhoneNumber()));
        startActivity(intent);
    }

    // Inner adapter class for branch list
    private class BranchAdapter extends RecyclerView.Adapter<BranchAdapter.BranchViewHolder> {

        @NonNull
        @Override
        public BranchViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_branch, parent, false);
            return new BranchViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BranchViewHolder holder, int position) {
            BankBranch branch = branchList.get(position);
            holder.bind(branch);
        }

        @Override
        public int getItemCount() {
            return branchList.size();
        }

        class BranchViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvAddress, tvDistance;

            BranchViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_branch_name);
                tvAddress = itemView.findViewById(R.id.tv_branch_address);
                tvDistance = itemView.findViewById(R.id.tv_branch_distance);
            }

            void bind(BankBranch branch) {
                tvName.setText(branch.getName());
                tvAddress.setText(branch.getAddress());
                
                if (branch.getDistanceFromUser() > 0) {
                    tvDistance.setText(branch.getFormattedDistance());
                    tvDistance.setVisibility(View.VISIBLE);
                } else {
                    tvDistance.setVisibility(View.GONE);
                }

                itemView.setOnClickListener(v -> selectBranch(branch));
            }
        }
    }
}
