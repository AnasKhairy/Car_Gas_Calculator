package com.anaskhaery.location_app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mumayank.com.airlocationlibrary.AirLocation;

public class MainActivity extends AppCompatActivity implements AirLocation.Callback {

    private LinearLayout locationContainer;
    private List<EditText> locationEditTexts;
    private TextView distanceText;
    private AirLocation airLocation;
    private Geocoder geocoder;
    private GeocodeTask currentTask;
    private boolean isCalculateButtonPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        locationContainer = findViewById(R.id.locationContainer);
        distanceText = findViewById(R.id.distanceText);

        geocoder = new Geocoder(this);

        locationEditTexts = new ArrayList<>();
        addNewLocationEditText();
    }

    private void addNewLocationEditText() {
        EditText newEditText = new EditText(this);
        newEditText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        newEditText.setHint("Enter location");
        newEditText.setInputType(android.text.InputType.TYPE_CLASS_TEXT);

        newEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().trim().isEmpty() &&
                        locationEditTexts.indexOf(newEditText) == locationEditTexts.size() - 1) {
                    addNewLocationEditText();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        locationContainer.addView(newEditText);
        locationEditTexts.add(newEditText);
    }

    public void calculate(View view) {
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        isCalculateButtonPressed = true;
        airLocation = new AirLocation(this, this, false, 0, "");
        airLocation.start();
    }

    @Override
    public void onSuccess(@NonNull ArrayList<Location> locations) {
        if (isCalculateButtonPressed) {
            currentTask = new GeocodeTask();
            currentTask.execute();
            isCalculateButtonPressed = false;
        }
    }

    @Override
    public void onFailure(@NonNull AirLocation.LocationFailedEnum locationFailedEnum) {
        Toast.makeText(this, "Error getting your location", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        airLocation.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        airLocation.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void clear(View view) {
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        locationContainer.removeAllViews();
        locationEditTexts.clear();
        distanceText.setText("");
        addNewLocationEditText();
    }

    private class GeocodeTask extends AsyncTask<Void, Void, String> {

        @RequiresApi(api = Build.VERSION_CODES.N)
        @SuppressLint("DefaultLocale")
        @Override
        protected String doInBackground(Void... voids) {
            List<LocationCoordinate> coordinates = new ArrayList<>();
            Set<String> uniqueLocations = new HashSet<>();
            StringBuilder result = new StringBuilder();

            try {
                for (EditText editText : locationEditTexts) {
                    String location = editText.getText().toString().trim();
                    if (!location.isEmpty() && uniqueLocations.add(location)) {
                        List<Address> addresses = geocoder.getFromLocationName(location, 1);
                        if (addresses == null || addresses.isEmpty()) {
                            return "Location not found: " + location;
                        }
                        Address address = addresses.get(0);
                        coordinates.add(new LocationCoordinate(location, address.getLatitude(), address.getLongitude()));
                    }
                }

                if (coordinates.size() < 2) {
                    return "Please enter at least two valid locations.";
                }
                ArrayList<String> money = new ArrayList<>();
                List<LocationCoordinate> optimalRoute = calculateOptimalRoute(coordinates);
                for (int i = 0; i < optimalRoute.size() - 1; i++) {
                    LocationCoordinate loc1 = optimalRoute.get(i);
                    LocationCoordinate loc2 = optimalRoute.get(i + 1);
                    float distance = calculateDistance(loc1, loc2);
                    double fuelCost = (distance * 0.08) * 15; // liters per kilometer + cost
                    result.append(String.format("Distance between %s and %s: %.2f km\n", loc1.getName(), loc2.getName(), distance));
                    result.append(String.format("Estimated fuel cost: %.2f EGP\n", fuelCost));
                    money.add(String.valueOf(fuelCost));
                }

                result.append("\nSuggested Order:\n");
                for (int i = 0; i < optimalRoute.size(); i++) {
                    result.append(String.format("Visit location %d: %s\n", i + 1, optimalRoute.get(i).getName()));
                }
                float totalCost = (float) money.stream().mapToDouble(Double::parseDouble).sum();
                result.append("\n Nearly Cost: ").append(totalCost).append(" EGP");

            } catch (IOException e) {
                return "Geocoder failed";
            } catch (Exception e) {
                return "Unexpected error occurred";
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            distanceText.setText(result);
        }
    }

    private float calculateDistance(LocationCoordinate loc1, LocationCoordinate loc2) {
        float[] results = new float[1];
        Location.distanceBetween(loc1.getLatitude(), loc1.getLongitude(), loc2.getLatitude(), loc2.getLongitude(), results);
        return results[0] / 1000 * 1.25f; // Convert to km and add 25%
    }

    private List<LocationCoordinate> calculateOptimalRoute(List<LocationCoordinate> coordinates) {
        List<LocationCoordinate> optimalRoute = new ArrayList<>();
        LocationCoordinate start = coordinates.get(0);
        optimalRoute.add(start);
        coordinates.remove(start);

        while (!coordinates.isEmpty()) {
            LocationCoordinate nearest = null;
            float shortestDistance = Float.MAX_VALUE;

            for (LocationCoordinate loc : coordinates) {
                assert start != null;
                float distance = calculateDistance(start, loc);
                if (distance < shortestDistance) {
                    shortestDistance = distance;
                    nearest = loc;
                }
            }

            optimalRoute.add(nearest);
            coordinates.remove(nearest);
            start = nearest;
        }

        return optimalRoute;
    }

    private class LocationCoordinate {
        private String name;
        private double latitude;
        private double longitude;

        public LocationCoordinate(String name, double latitude, double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getName() {
            return name;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }
}
