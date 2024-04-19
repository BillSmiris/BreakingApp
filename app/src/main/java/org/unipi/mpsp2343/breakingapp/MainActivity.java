package org.unipi.mpsp2343.breakingapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.os.Vibrator;
import android.widget.Toast;

import org.unipi.mpsp2343.breakingapp.models.BrakingEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    //<editor-fold desc="Variables">
    //constants
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int ACCELERATION_THRESHOLD = -2;
    private static final int VIBRATION_PERIOD = 200;
    //UI
    TextView accelerationText;
    Button toggleButton;
    //state
    boolean accelerometerEnabled = false;
    boolean isInsideBrakingEvent = false;
    //computations
    float acceleration = 0f;
    Long currentMils = 0L;
    float currentSpeed = 0f;
    Long previousMils = 0L;
    float previousSpeed = 0f;
    //dependencies
    Vibrator vibrator;
    LocationManager locationManager;
    SQLiteDatabase database;
    LocationListener locationListener = location -> {
        currentMils = location.getTime();
        currentSpeed = location.getSpeed();
        acceleration = calculateAcceleration();
        accelerationText.setText(getString(R.string._0_m_s, acceleration));
        detectBrakingEvent(location);
        previousMils = currentMils;
        previousSpeed = currentSpeed;
    };
    //</editor-fold>

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //<editor-fold desc="default">
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //</editor-fold>
        //init UI
        accelerationText = findViewById(R.id.accelerationTextView);
        accelerationText.setText(getString(R.string._0_m_s, acceleration));
        toggleButton = findViewById((R.id.toggleAccelerometerButton));
        //get services
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        //init db
        database = openOrCreateDatabase("myDB.db",MODE_PRIVATE,null);
        database.execSQL("Create table if not exists BrakingEvents(" +
                "lat REAL," +
                "lon REAL," +
                "timestamp INT)");
    }

    //<editor-fold desc="Button clicks">
    public void toggleAccelerometer(View view){
        if(accelerometerEnabled){
            gpsOff();
            toggleButton.setText(getString(R.string.enable));
            accelerationText.setText(getString(R.string._0_m_s, acceleration));
            accelerometerEnabled = false;
            return;
        }
        if(gpsOn()){
            toggleButton.setText(getString(R.string.disable));
            accelerometerEnabled = true;
        }
    }

    public void openMap(View view) {
        Intent intent = new Intent(this,MapsActivity.class);
        List<BrakingEvent> events = getEvents();
        intent.putExtra("EVENTS", (Serializable) events);
        startActivity(intent);
    }
    //</editor-fold>

    //<editor-fold desc="GPS Toggling">
    public boolean gpsOn() {
        if(!accelerometerEnabled) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
                return false;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    0, 0, locationListener);
            return true;
        }
        return false;
    }

    public void gpsOff(){
        if(accelerometerEnabled){
            locationManager.removeUpdates(locationListener);
        }
    }
    //</editor-fold>

    //<editor-fold desc="Braking Event Handling">
    public float calculateAcceleration() {
        float deltaTimeMils = currentMils - previousMils;
        float deltaTimeSeconds = deltaTimeMils / 1000.0f;
        float deltaVelocity = currentSpeed - previousSpeed;

        return deltaVelocity - deltaTimeSeconds;
    }

    public void detectBrakingEvent(Location location) {
        if(!isInsideBrakingEvent) {
            if(acceleration <= ACCELERATION_THRESHOLD) {
                accelerationText.setTextColor(ContextCompat.getColor(this, R.color.red));
                vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_PERIOD, VibrationEffect.DEFAULT_AMPLITUDE));
                Toast.makeText(this, getResources().getString(R.string.braking_detection_toast_message), Toast.LENGTH_LONG).show();
                saveEvent(location);
                isInsideBrakingEvent = true;
            }
        }
        else {
            if(acceleration > ACCELERATION_THRESHOLD) {
                accelerationText.setTextColor(ContextCompat.getColor(this, R.color.white));
                isInsideBrakingEvent = false;
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="Database Operations">
    public void saveEvent(Location location) {
        database.execSQL("INSERT INTO BrakingEvents VALUES(" + location.getLatitude() + "," +  location.getLongitude() + "," + location.getTime() + ");");
    }

    public List<BrakingEvent> getEvents() {
        List<BrakingEvent> results = new ArrayList<>();
        Cursor cursor = database.rawQuery("SELECT * FROM BrakingEvents;", null);
        while (cursor.moveToNext()) {
            results.add(new BrakingEvent(
               cursor.getFloat(0),
               cursor.getFloat(1),
               cursor.getInt(2)
            ));
        }
        cursor.close();

        return results;
    }
    //</editor-fold>
}