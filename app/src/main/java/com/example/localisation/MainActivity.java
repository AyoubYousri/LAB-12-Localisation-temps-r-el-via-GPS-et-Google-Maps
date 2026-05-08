package com.example.localisation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity_GPS";
    private static final int REQ_LOC = 100;
    private static final long MIN_TIME_MS = 60000;
    private static final float MIN_DISTANCE_M = 150;

    private final String insertUrl = "http://10.0.2.2/localisation/createPosition.php";

    private TextView tvLat;
    private TextView tvLon;
    private RequestQueue requestQueue;
    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLat = findViewById(R.id.tvLat);
        tvLon = findViewById(R.id.tvLon);
        Button btnMap = findViewById(R.id.btnMap);

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        btnMap.setOnClickListener(v -> {
            Log.d(TAG, "Ouverture de la carte (MapsActivity)");
            startActivity(new Intent(this, MapsActivity.class));
        });

        askLocationPermissionAndStart();
    }

    private void askLocationPermissionAndStart() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Demande de permission GPS...");
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOC);
            return;
        }
        startGpsUpdates();
    }

    @SuppressLint("MissingPermission")
    private void startGpsUpdates() {
        if (locationManager == null) return;

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Veuillez activer le GPS sur l'appareil", Toast.LENGTH_LONG).show();
        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();
                
                tvLat.setText("Latitude: " + lat);
                tvLon.setText("Longitude: " + lon);

                Log.d(TAG, "Nouvelle position GPS détectée : " + lat + ", " + lon);
                addPosition(lat, lon);
            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_MS, MIN_DISTANCE_M, locationListener);
    }

    private void addPosition(final double lat, final double lon) {
        final String imei = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        final String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Log.d(TAG, "URL Appelée : " + insertUrl);
        Log.d(TAG, "Paramètres envoyés : latitude=" + lat + ", longitude=" + lon + ", date=" + dateStr + ", imei=" + imei);

        StringRequest request = new StringRequest(Request.Method.POST, insertUrl,
                response -> {
                    Log.d(TAG, "Réponse PHP (createPosition) : " + response);
                    if (response.trim().equalsIgnoreCase("Position enregistrée")) {
                        Toast.makeText(getApplicationContext(), "Position enregistrée", Toast.LENGTH_SHORT).show();
                    } else {
                        // En cas de message d'erreur du PHP (ex: données manquantes)
                        Log.e(TAG, "Erreur retournée par PHP : " + response);
                    }
                },
                error -> {
                    String errorMsg = getVolleyErrorMessage(error);
                    Log.e(TAG, "Erreur Volley : " + errorMsg);
                    Toast.makeText(getApplicationContext(), "Erreur réseau : " + errorMsg, Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("latitude", String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                params.put("date", dateStr);
                params.put("imei", imei);
                return params;
            }
        };

        requestQueue.add(request);
    }

    private String getVolleyErrorMessage(VolleyError error) {
        if (error == null) return "inconnue";
        if (error.networkResponse != null) return "Code HTTP " + error.networkResponse.statusCode;
        return error.getClass().getSimpleName();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOC && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission GPS accordée.");
            startGpsUpdates();
        } else {
            Log.e(TAG, "Permission GPS refusée.");
            Toast.makeText(this, "Permission GPS refusée", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }
}
