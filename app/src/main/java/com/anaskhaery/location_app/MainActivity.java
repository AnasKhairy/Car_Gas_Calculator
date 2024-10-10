package com.anaskhaery.location_app;

import static androidx.core.location.LocationManagerCompat.getCurrentLocation;

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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
    private boolean isLocationButtonClicked = false;
    private double lat, lon,fuelConsumptionPerKm,fuelPricePerLiter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String selectedCar = getIntent().getStringExtra("selected car");
        String selectedGas = getIntent().getStringExtra("selected gas");
        //Toast.makeText(this, selectedCar + selectedGas, Toast.LENGTH_SHORT).show();


// Variables to store the fuel consumption and fuel price
        fuelConsumptionPerKm = 0.0; // Fuel consumption in liters per km
        fuelPricePerLiter = 0.0;    // Fuel price in EGP per liter

// Logic for setting fuel price based on selected gas type
        if (selectedGas != null) {
            switch (selectedGas) {
                case "80":
                    fuelPricePerLiter = 12.25;
                    break;
                case "90":
                    fuelPricePerLiter = 13.75;
                    break;
                case "92":
                    fuelPricePerLiter = 15.00;
                    break;
                case "95":
                    fuelPricePerLiter = 17.00; // Example value for gas 95
                    break;
                case "solar":
                    fuelPricePerLiter = 11.50;
                    break;
                case "gas":
                    fuelPricePerLiter = 3.75;
                    break;
                default:
                    fuelPricePerLiter = 0.0; // Default value in case gas type is not recognized
                    break;
            }
        }

// Logic for setting fuel consumption based on selected car type
        if (selectedCar != null) {
            switch (selectedCar) {
                case "Toyota":
                    fuelConsumptionPerKm = 5.9 / 100; // 5.9 L/100km
                    break;
                case "Hyundai":
                    fuelConsumptionPerKm = 6.5 / 100; // Example: 6.5 L/100km for Honda
                    break;
                case "BMW":
                    fuelConsumptionPerKm = 8.0 / 100; // Example: 8.0 L/100km for BMW
                    break;
                case "Mercedes":
                    fuelConsumptionPerKm = 12.0 / 100; // Example: 12.0 L/100km for BMW
                    break;
                default:
                    fuelConsumptionPerKm = 0.0; // Default in case car type is not recognized
                    break;
            }
        }

        locationContainer = findViewById(R.id.locationContainer);
        distanceText = findViewById(R.id.distanceText);

        geocoder = new Geocoder(this);

        airLocation = new AirLocation(this, this, true, 0, "");
//        airLocation.start();

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
                // function --> String "s"
                // isLocationButtonClicked=false;
                // airLocation.start
                // all in background Thread
            }
        });

        locationContainer.addView(newEditText);
        locationEditTexts.add(newEditText);
    }

    public void calculate(View view) {
        isLocationButtonClicked = false;
        airLocation.start();
    }

    @Override
    public void onSuccess(@NonNull ArrayList<Location> locations) {

        lat = locations.get(0).getLatitude();
        lon = locations.get(0).getLongitude();

        if (isLocationButtonClicked) {
            getCurrentLocation(lat, lon);
        } else {
            new GeocodeTask().execute();
        }
    }

    private void getCurrentLocation(double lat, double lon) {

        try {
            List<Address> location = geocoder.getFromLocation(lat, lon, 1);
            if (location != null && !location.isEmpty()) {
                String locationName = location.get(0).getSubAdminArea();

                // Get the currently focused EditText, if none, use the first one
                EditText currentEditText = getCurrentFocus() instanceof EditText ?
                        (EditText) getCurrentFocus() : locationEditTexts.get(0);

                currentEditText.setText(locationName);
                currentEditText.setSelection(currentEditText.getText().length()); // Move cursor to the end
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error fetching location", Toast.LENGTH_SHORT).show();
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
        locationContainer.removeAllViews();
        locationEditTexts.clear();
        distanceText.setText("");
        addNewLocationEditText();
    }

    public void getCurrentLocationButtonClicked(View view) {
        isLocationButtonClicked = true;
        airLocation.start();
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
                // Step 1: Collect user-entered locations (maintain order)
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

                // Step 2: Calculate for the order user entered
                result.append("Results for the order you entered:\n");
                String userEnteredResult = calculateRoute(coordinates);
                result.append(userEnteredResult);
                result.append(" ----------------------- ");

                // Step 3: Calculate for the optimized (nearest) route
                result.append("\nResults for the suggested nearest route:\n");
                List<LocationCoordinate> optimalRoute = calculateOptimalRoute(new ArrayList<>(coordinates));
                String suggestedResult = calculateRoute(optimalRoute);
                result.append(suggestedResult);

            } catch (IOException e) {
                return "Geocoder failed";
            } catch (Exception e) {
                return "Unexpected error occurred";
            }

            return result.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            distanceText.setText(result);  // Display both sets of results
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

    private String calculateRoute(List<LocationCoordinate> route) {
        StringBuilder result = new StringBuilder();
        double totalCost = 0;

        for (int i = 0; i < route.size() - 1; i++) {
            LocationCoordinate loc1 = route.get(i);
            LocationCoordinate loc2 = route.get(i + 1);

            // Calculate distance between locations
            float distance = calculateDistance(loc1, loc2);
            // Estimate fuel cost (example: fuel consumption rate of 8 liters per 100 km and fuel cost of 15 EGP per liter)
//            double fuelCost = (distance * 0.08) * 15; // 0.08 is liters/km, 15 is cost per liter in EGP
            double fuelCost = (distance * fuelConsumptionPerKm) * fuelPricePerLiter;
//            System.out.println(fuelConsumptionPerKm+" "+fuelPricePerLiter+"");
            totalCost += fuelCost;
            // Append details for this leg of the journey
            result.append(String.format("Distance between %s and %s: %.2f km\n", loc1.getName(), loc2.getName(), distance));
            result.append(String.format("Estimated fuel cost: %.2f EGP\n", fuelCost));
        }
        // Append the suggested order
        result.append("\nSuggested Order:\n");
        for (int i = 0; i < route.size(); i++) {
            result.append(String.format("Visit location %d: %s\n", i + 1, route.get(i).getName()));
        }
        // Append the total cost
        result.append(String.format("\nTotal Estimated Cost: %.2f EGP\n", totalCost));

        return result.toString();
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
