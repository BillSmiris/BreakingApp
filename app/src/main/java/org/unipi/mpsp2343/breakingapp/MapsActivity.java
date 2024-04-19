package org.unipi.mpsp2343.breakingapp;

import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.unipi.mpsp2343.breakingapp.databinding.ActivityMapsBinding;
import org.unipi.mpsp2343.breakingapp.models.BrakingEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private List<BrakingEvent> events;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        try{
            events = (List<BrakingEvent>) getIntent().getSerializableExtra("EVENTS");
        }
        catch(Exception e) {
            events = new ArrayList<>();
            Toast.makeText(this, getResources().getString(R.string.data_retrieval_error), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if(events.isEmpty()) {
            Toast.makeText(this, getResources().getString(R.string.no_data_found), Toast.LENGTH_LONG).show();
            return;
        }

        LatLng latLng = new LatLng(0,0);
        for(BrakingEvent event : events) {
            latLng = new LatLng(event.getLat(), event.getLon());
            mMap.addMarker(new MarkerOptions().position(latLng).title(event.getTimestampFormatted()));
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,15f));
    }
}