package com.example.localisation;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";
    private final String showUrl = "http://10.0.2.2/localisation/showPositions.php";

    private GoogleMap mMap;
    private RequestQueue requestQueue;
    private TextView tvMapMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        tvMapMessage = findViewById(R.id.tvMapMessage);
        requestQueue = Volley.newRequestQueue(getApplicationContext());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapContainer);
        
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mapContainer, mapFragment)
                    .commit();
        }
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "Carte prête, chargement des positions...");
        setUpMap();
    }

    private void setUpMap() {
        Log.d(TAG, "Appel URL: " + showUrl);
        
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                showUrl,
                null,
                response -> {
                    Log.d(TAG, "Réponse JSON reçue: " + response.toString());
                    try {
                        JSONArray positions = response.getJSONArray("positions");
                        LatLng lastPosition = null;

                        mMap.clear(); // Effacer les anciens markers

                        for (int i = 0; i < positions.length(); i++) {
                            JSONObject position = positions.getJSONObject(i);

                            double lat = position.getDouble("latitude");
                            double lon = position.getDouble("longitude");
                            String imei = position.optString("imei", "N/A");
                            String date = position.optString("date_position", "N/A");

                            LatLng loc = new LatLng(lat, lon);
                            lastPosition = loc;

                            mMap.addMarker(new MarkerOptions()
                                    .position(loc)
                                    .title("IMEI: " + imei)
                                    .snippet("Date: " + date + " | Lat: " + lat + " | Lon: " + lon));
                        }

                        if (lastPosition != null) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastPosition, 12f));
                        }

                        Toast.makeText(
                                this,
                                String.format(getString(R.string.positions_loaded), positions.length()),
                                Toast.LENGTH_SHORT
                        ).show();

                    } catch (JSONException e) {
                        Log.e(TAG, "Erreur parsing JSON: " + e.getMessage());
                        showMapMessage("Erreur JSON: " + e.getMessage());
                    }
                },
                error -> {
                    String errorMsg = "Erreur sur: " + showUrl;
                    if (error.networkResponse != null) {
                        errorMsg += "\nCode HTTP: " + error.networkResponse.statusCode;
                    }
                    Log.e(TAG, "Erreur Volley (showPositions): " + error.toString());
                    showMapMessage(errorMsg + "\nType: " + error.getClass().getSimpleName());
                }
        );

        requestQueue.add(jsonObjectRequest);
    }

    private void showMapMessage(String message) {
        if (tvMapMessage == null) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            return;
        }

        tvMapMessage.setText(message);
        tvMapMessage.setVisibility(View.VISIBLE);
    }
}
