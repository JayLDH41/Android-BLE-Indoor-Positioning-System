package my.edu.utar.fyp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class EstimateLocation extends AppCompatActivity {

    //based on offline RSSI, implement knn method to estimate user location
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_estimate_location);
    }
}