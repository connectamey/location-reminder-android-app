package com.example.final_project;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.LocationBias;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.Firebase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    //private static final int REQUEST_LOCATION_PERMISSION = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private PlaceAutocompleteAdapter adapter;
    private double userLatitude = 0.0;
    private double userLongitude = 0.0;
    private TextView locationPrompt;

    private static final String TAG = "MainActivity";

    private EditText reminderEditText;
    private AutoCompleteTextView locationAutoCompleteTextView;
    private Button saveButton;
    private List<String> reminders = new ArrayList<>();
    private FirebaseDatabase mDatabase;
    private DatabaseReference reference;
    private RecyclerView recyclerView;
    private MyAdapter mMyadapter;
    private  List<Task>  tasks;
    private PlacesClient placesClient;

    private GeofencingClient geofencingClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate started");

        // Initialize UI components
        reminderEditText = findViewById(R.id.reminderEditText);
        locationAutoCompleteTextView = findViewById(R.id.locationAutoCompleteTextView);
        saveButton = findViewById(R.id.saveButton);
        recyclerView = findViewById(R.id.task_recycler_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        mDatabase = FirebaseDatabase.getInstance();
        reference = mDatabase.getReference("tasks");

        geofencingClient = LocationServices.getGeofencingClient(this);

        // Initialize Google Places

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyBICjwSXFIUEO6e6borzyUzlMeHMsF-WUE"); // Replace with your actual API key
        }
        placesClient = Places.createClient(this);

        // Initialize the PlaceAutocompleteAdapter
        adapter = new PlaceAutocompleteAdapter(this, placesClient);
        locationAutoCompleteTextView.setAdapter(adapter);

        // Check for location permission and initialize location client
        if (checkLocationPermission()) {
            initializeLocationClient();
            enableLocation();
        }

        // Set up item click listener for the AutoCompleteTextView
        locationAutoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AutocompletePrediction prediction = adapter.getItem(position);
                String primaryText = prediction.getPrimaryText(null).toString();
                locationAutoCompleteTextView.setText(primaryText);
            }
        });

        // Set up save button listener
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveReminder();
            }
        });
        locationPrompt = findViewById(R.id.locationPrompt);
        checkLocationServicesAndPrompt();


// Create an empty list to store the tasks
        tasks = new ArrayList<>();

// Create a RecyclerView adapter
        mMyadapter = new MyAdapter(tasks);

// Set the adapter to the RecyclerView
        recyclerView.setAdapter(mMyadapter);

// Read all tasks from the database
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // Clear existing tasks
                tasks.clear();

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    String reminderText = childSnapshot.child("reminder").getValue(String.class);
                    String location = childSnapshot.child("location").getValue(String.class);

                    // Add a new task to the list
                    tasks.add(new Task(reminderText, location));
                }

                // Notify the adapter that the data has changed
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });
        if (recyclerView.getAdapter().getItemCount() > 0) {
            recyclerView.smoothScrollToPosition(0);
        }
        mMyadapter.notifyDataSetChanged();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, enable location
                startLocationUpdates();
            } else {
                // Permission denied, handle the denial
                Toast.makeText(this, "Location permission is necessary for this feature", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void initializeLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    userLatitude = location.getLatitude();
                    userLongitude = location.getLongitude();
                    if (userLatitude != 0.0 && userLongitude != 0.0) {
                        Log.d(TAG, "onLocationResult: " + userLatitude + " " + userLongitude);
                        updateLocationBias(userLatitude, userLongitude);
                    }
                }
            }
        };
        startLocationUpdates();
    }



    private void checkLocationPermissionAndStartLocationUpdates () {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                startLocationUpdates();
            }
        }

        private void startLocationUpdates () {
            if (fusedLocationClient == null) {
                Log.e(TAG, "FusedLocationProviderClient is not initialized");
                return;
            }

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(5000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            }
        }

        private void updateLocationBias(double latitude, double longitude){
            double radiusDegrees = 0.1; // Roughly equal to 10 km
            LatLng southwest = new LatLng(latitude - radiusDegrees, longitude - radiusDegrees);
            LatLng northeast = new LatLng(latitude + radiusDegrees, longitude + radiusDegrees);

            if (getSupportFragmentManager().findFragmentById(R.id.locationAutoCompleteTextView) instanceof AutocompleteSupportFragment) {
                AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                        getSupportFragmentManager().findFragmentById(R.id.locationAutoCompleteTextView);

                LocationBias bias = RectangularBounds.newInstance(southwest, northeast);
                autocompleteFragment.setLocationBias(bias);

            }
        }


        private void saveReminder () {
            String reminderText = reminderEditText.getText().toString();
            String location = locationAutoCompleteTextView.getText().toString();

            if (!reminderText.isEmpty() && !location.isEmpty()) {
                reminders.add(reminderText + " at " + location);

                // Write a message to the database

                Map<String, Object> task = new HashMap<>();
                task.put("reminder", reminderText);
                task.put("location", location);

                // Generate unique key
                String key = reference.push().getKey();

                // Add task to database
                reference.child(key).setValue(task);

                // Clear input fields
                reminderEditText.setText("");
                locationAutoCompleteTextView.setText("");
                Toast.makeText(MainActivity.this, "Reminder Saved!", Toast.LENGTH_SHORT).show();
                mMyadapter.notifyDataSetChanged();
            }
            else {
                Toast.makeText(MainActivity.this, "Please enter both reminder and location", Toast.LENGTH_SHORT).show();
            }
    }

    private void enableLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Prompt the user to enable GPS
            new AlertDialog.Builder(this)
                    .setMessage("Enable GPS")
                    .setPositiveButton("Settings", (dialogInterface, i) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                    .show();
        } else {
        }
    }



    @Override
        protected void onResume () {
            super.onResume();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            }
        checkLocationServicesAndPrompt();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient  != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }


    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }
    private void checkLocationServicesAndPrompt() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        Guideline topGuideline = findViewById(R.id.top_guideline);
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) topGuideline.getLayoutParams();

        if (!gpsEnabled) {
            locationPrompt.setVisibility(View.VISIBLE);
            locationPrompt.setOnClickListener(v -> showLocationServicesAlert());
            // Adjust the Guideline to be below the location prompt
            params.guideBegin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, getResources().getDisplayMetrics());
        } else {
            locationPrompt.setVisibility(View.GONE);
            // Adjust the Guideline to be at the top of the parent
            params.guideBegin = 0;
        }

        topGuideline.setLayoutParams(params); // Apply the new layout params
    }
    private void showLocationServicesAlert() {
        new AlertDialog.Builder(this)
                .setMessage("For a better experience, turn on device location, which uses Google's location service.")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("No, thanks", (dialog, which) -> dialog.dismiss())
                .show();
    }

    }


    // Getter and setter methods for reminderText and location


