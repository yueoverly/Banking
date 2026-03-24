package com.example.myapplication.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * MapActivity - Hiển thị bản đồ với vị trí người dùng và các chi nhánh ngân hàng
 * Features:
 * - Định vị vị trí hiện tại
 * - Hiển thị các chi nhánh TDT Bank
 * - Tìm chi nhánh gần nhất
 * - Chỉ đường đi bộ đến chi nhánh
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final float DEFAULT_ZOOM = 15f;
    
    // Walking speed: ~5 km/h = ~83 m/min
    private static final double WALKING_SPEED_M_PER_MIN = 83.0;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    
    private FloatingActionButton fabMyLocation, fabFindNearest;
    private CardView cardBranchInfo;
    private TextView tvBranchName, tvBranchAddress, tvDistance, tvWalkingTime;
    private Button btnGetDirections;
    private ProgressBar progressBar;

    private LatLng currentLocation;
    private Marker userMarker;
    private Polyline currentRoute;
    private List<BankBranch> bankBranches = new ArrayList<>();
    private BankBranch selectedBranch;
    private BankBranch nearestBranch;

    // Bank Branch data class
    private static class BankBranch {
        String name;
        String address;
        LatLng location;
        Marker marker;

        BankBranch(String name, String address, double lat, double lng) {
            this.name = name;
            this.address = address;
            this.location = new LatLng(lat, lng);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        initViews();
        setupToolbar();
        initBankBranches();
        initLocationClient();
        initMap();
    }

    private void initViews() {
        fabMyLocation = findViewById(R.id.fabMyLocation);
        fabFindNearest = findViewById(R.id.fabFindNearest);
        cardBranchInfo = findViewById(R.id.cardBranchInfo);
        tvBranchName = findViewById(R.id.tvBranchName);
        tvBranchAddress = findViewById(R.id.tvBranchAddress);
        tvDistance = findViewById(R.id.tvDistance);
        tvWalkingTime = findViewById(R.id.tvWalkingTime);
        btnGetDirections = findViewById(R.id.btnGetDirections);
        progressBar = findViewById(R.id.progressBar);

        fabMyLocation.setOnClickListener(v -> moveToMyLocation());
        fabFindNearest.setOnClickListener(v -> findNearestBranch());
        btnGetDirections.setOnClickListener(v -> drawRouteToSelectedBranch());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * Khởi tạo danh sách các chi nhánh TDT Bank
     * Trong thực tế sẽ lấy từ API hoặc Firebase
     */
    private void initBankBranches() {
        // TDT Bank branches in Ho Chi Minh City
        bankBranches.add(new BankBranch(
            "TDT Bank - Hội sở chính",
            "19 Nguyễn Hữu Thọ, Tân Hưng, Quận 7",
            10.7326, 106.6997
        ));
        
        bankBranches.add(new BankBranch(
            "TDT Bank - Chi nhánh Quận 1",
            "123 Nguyễn Huệ, Bến Nghé, Quận 1",
            10.7731, 106.7030
        ));
        
        bankBranches.add(new BankBranch(
            "TDT Bank - Chi nhánh Quận 3",
            "456 Võ Văn Tần, Phường 5, Quận 3",
            10.7765, 106.6876
        ));
        
        bankBranches.add(new BankBranch(
            "TDT Bank - Chi nhánh Quận 7",
            "789 Nguyễn Thị Thập, Tân Phú, Quận 7",
            10.7380, 106.7218
        ));
        
        bankBranches.add(new BankBranch(
            "TDT Bank - Chi nhánh Phú Mỹ Hưng",
            "101 Nguyễn Lương Bằng, Tân Phú, Quận 7",
            10.7285, 106.7195
        ));
        
        bankBranches.add(new BankBranch(
            "TDT Bank - Chi nhánh Gò Vấp",
            "234 Phan Văn Trị, Phường 11, Gò Vấp",
            10.8326, 106.6654
        ));
        
        bankBranches.add(new BankBranch(
            "TDT Bank - Chi nhánh Bình Thạnh",
            "567 Xô Viết Nghệ Tĩnh, Phường 26, Bình Thạnh",
            10.8015, 106.7148
        ));
        
        bankBranches.add(new BankBranch(
            "TDT Bank - Chi nhánh Thủ Đức",
            "890 Võ Văn Ngân, Linh Chiểu, TP Thủ Đức",
            10.8506, 106.7718
        ));
    }

    private void initLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    updateUserMarker();
                }
            }
        };
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        
        // Map settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        
        // Add bank branch markers
        addBranchMarkers();
        
        // Marker click listener
        mMap.setOnMarkerClickListener(marker -> {
            for (BankBranch branch : bankBranches) {
                if (branch.marker != null && branch.marker.equals(marker)) {
                    showBranchInfo(branch);
                    return true;
                }
            }
            return false;
        });
        
        // Map click listener - hide info card
        mMap.setOnMapClickListener(latLng -> {
            cardBranchInfo.setVisibility(View.GONE);
            clearRoute();
        });
        
        // Check location permission and start
        checkLocationPermission();
    }

    private void addBranchMarkers() {
        for (BankBranch branch : bankBranches) {
            MarkerOptions options = new MarkerOptions()
                    .position(branch.location)
                    .title(branch.name)
                    .snippet(branch.address)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            
            branch.marker = mMap.addMarker(options);
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            enableMyLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Cần quyền định vị để sử dụng tính năng này", 
                        Toast.LENGTH_LONG).show();
                // Show default location (TDT University)
                LatLng defaultLocation = new LatLng(10.7326, 106.6997);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
            }
        }
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false); // We use custom button
            
            // Get current location
            progressBar.setVisibility(View.VISIBLE);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        progressBar.setVisibility(View.GONE);
                        if (location != null) {
                            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            updateUserMarker();
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM));
                            
                            // Auto find nearest branch
                            findNearestBranch();
                        } else {
                            requestLocationUpdates();
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Không thể lấy vị trí", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setMinUpdateIntervalMillis(2000)
                    .build();
            
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void updateUserMarker() {
        if (currentLocation == null || mMap == null) return;
        
        if (userMarker != null) {
            userMarker.setPosition(currentLocation);
        } else {
            MarkerOptions options = new MarkerOptions()
                    .position(currentLocation)
                    .title("Vị trí của bạn")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            userMarker = mMap.addMarker(options);
        }
    }

    private void moveToMyLocation() {
        if (currentLocation != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM));
        } else {
            Toast.makeText(this, "Đang định vị...", Toast.LENGTH_SHORT).show();
            enableMyLocation();
        }
    }

    /**
     * Tìm chi nhánh gần nhất
     */
    private void findNearestBranch() {
        if (currentLocation == null) {
            Toast.makeText(this, "Đang định vị vị trí của bạn...", Toast.LENGTH_SHORT).show();
            return;
        }

        double minDistance = Double.MAX_VALUE;
        nearestBranch = null;

        for (BankBranch branch : bankBranches) {
            double distance = calculateDistance(currentLocation, branch.location);
            if (distance < minDistance) {
                minDistance = distance;
                nearestBranch = branch;
            }
        }

        if (nearestBranch != null) {
            showBranchInfo(nearestBranch);
            
            // Zoom to show both user and nearest branch
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(currentLocation);
            builder.include(nearestBranch.location);
            LatLngBounds bounds = builder.build();
            
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
            
            // Highlight nearest branch marker
            if (nearestBranch.marker != null) {
                nearestBranch.marker.showInfoWindow();
            }
            
            Toast.makeText(this, "Chi nhánh gần nhất: " + nearestBranch.name, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Hiển thị thông tin chi nhánh
     */
    private void showBranchInfo(BankBranch branch) {
        selectedBranch = branch;
        
        tvBranchName.setText(branch.name);
        tvBranchAddress.setText(branch.address);
        
        if (currentLocation != null) {
            double distance = calculateDistance(currentLocation, branch.location);
            tvDistance.setText(formatDistance(distance));
            
            // Estimate walking time
            int walkingMinutes = (int) Math.ceil(distance / WALKING_SPEED_M_PER_MIN);
            tvWalkingTime.setText(walkingMinutes + " phút");
        } else {
            tvDistance.setText("-- m");
            tvWalkingTime.setText("-- phút");
        }
        
        cardBranchInfo.setVisibility(View.VISIBLE);
    }

    /**
     * Vẽ đường đi đến chi nhánh đã chọn
     */
    private void drawRouteToSelectedBranch() {
        if (currentLocation == null) {
            Toast.makeText(this, "Không thể xác định vị trí của bạn", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedBranch == null) {
            Toast.makeText(this, "Vui lòng chọn chi nhánh", Toast.LENGTH_SHORT).show();
            return;
        }

        // Clear old route
        clearRoute();
        
        // Draw simple straight line route (in real app, use Directions API)
        // For demo, we create a walking path simulation
        List<LatLng> routePoints = createWalkingPath(currentLocation, selectedBranch.location);
        
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(routePoints)
                .width(12)
                .color(Color.parseColor("#1E88E5"))
                .geodesic(true);
        
        currentRoute = mMap.addPolyline(polylineOptions);
        
        // Zoom to show entire route
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : routePoints) {
            builder.include(point);
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
        
        // Update button text
        btnGetDirections.setText("🚶 Đang hiển thị đường đi");
        
        Toast.makeText(this, "Đường đi bộ đến " + selectedBranch.name, Toast.LENGTH_SHORT).show();
    }

    /**
     * Tạo đường đi mô phỏng (trong thực tế sử dụng Google Directions API)
     */
    private List<LatLng> createWalkingPath(LatLng start, LatLng end) {
        List<LatLng> path = new ArrayList<>();
        path.add(start);
        
        // Create intermediate points for a more realistic path
        // Simulate walking on streets (not straight line)
        double latDiff = end.latitude - start.latitude;
        double lngDiff = end.longitude - start.longitude;
        
        // First go horizontal, then vertical (simulating street grid)
        LatLng midPoint1 = new LatLng(start.latitude, start.longitude + lngDiff * 0.3);
        LatLng midPoint2 = new LatLng(start.latitude + latDiff * 0.5, start.longitude + lngDiff * 0.3);
        LatLng midPoint3 = new LatLng(start.latitude + latDiff * 0.5, start.longitude + lngDiff * 0.7);
        LatLng midPoint4 = new LatLng(end.latitude, start.longitude + lngDiff * 0.7);
        
        path.add(midPoint1);
        path.add(midPoint2);
        path.add(midPoint3);
        path.add(midPoint4);
        path.add(end);
        
        return path;
    }

    private void clearRoute() {
        if (currentRoute != null) {
            currentRoute.remove();
            currentRoute = null;
        }
        btnGetDirections.setText("🚶 Chỉ đường đi bộ");
    }

    /**
     * Tính khoảng cách giữa 2 điểm (Haversine formula)
     * @return Khoảng cách tính bằng mét
     */
    private double calculateDistance(LatLng point1, LatLng point2) {
        final int R = 6371000; // Earth radius in meters
        
        double lat1 = Math.toRadians(point1.latitude);
        double lat2 = Math.toRadians(point2.latitude);
        double deltaLat = Math.toRadians(point2.latitude - point1.latitude);
        double deltaLng = Math.toRadians(point2.longitude - point1.longitude);
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    /**
     * Format khoảng cách cho hiển thị
     */
    private String formatDistance(double meters) {
        if (meters < 1000) {
            return String.format("%.0f m", meters);
        } else {
            return String.format("%.1f km", meters / 1000);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentLocation == null) {
            requestLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
