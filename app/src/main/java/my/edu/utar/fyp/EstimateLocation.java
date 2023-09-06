package my.edu.utar.fyp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.ml.distance.EuclideanDistance;

public class EstimateLocation extends AppCompatActivity {

    Button btnStartEst, btnStopEst, btnGetCurRSSI;
    TextView tvEstLocation, tvKNearestRefPt, tvCurRssi;

    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    private final int REQUEST_ENABLE_BT = 1;


    //based on offline RSSI, implement knn method to estimate user location
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_estimate_location);

        //bind UI components
        btnStartEst = findViewById(R.id.btnStartEstimateLocation);
        btnStopEst = findViewById(R.id.btnStopEstimateLocation);
        btnGetCurRSSI = findViewById(R.id.btnGetCurRSSI);
        tvEstLocation = findViewById(R.id.tvShowEstimatedLocation);
        tvKNearestRefPt = findViewById(R.id.tvShowKNearestRefPoint);
        tvCurRssi = findViewById(R.id.tvShowCurrentRssi);

        //asking for permissions
        checkPermissions(this, this);

        //check BT turned on or not
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();
        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        //setup UI components
        btnGetCurRSSI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScan();
            }
        });

        btnStopEst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopScan();
                finish();
            }
        });

        btnStartEst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startEstimation();
            }
        });

    }


    //receives scan results and store into a hashmap
    HashMap<String, Integer> hm1 = new HashMap<>();
    KalmanFilterHelper kfBeacon1, kfBeacon2, kfBeacon3;
    ArrayList<Integer> meanKFRssis1, meanKFRssis2, meanKFRssis3;
    int inputCounter1, inputCounter2, inputCounter3;
    int totalInputVal1, totalInputVal2, totalInputVal3;
    int finalmean1, finalmean2, finalmean3;

    private final ScanCallback leScanCallback = new  ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            BluetoothDevice device = result.getDevice();
            int rssi = result.getRssi();
            @SuppressLint("MissingPermission") String deviceName = device.getName();

            //performs mean & kalman filtering here
            //filtering is independent as they have their own distribution
            if (deviceName != null) {

                switch (deviceName) {
                    case "Beacon1": {
                        int meanAndKFRssi = 0;
                        int mean;
                        finalmean1 = 0;

                        totalInputVal1 += rssi;
                        inputCounter1++;

                        if (inputCounter1 == 10) {

                            //mean filtering on RSSI Beacon 1
                            mean = totalInputVal1 / inputCounter1;
                            finalmean1 = mean;

                            //kalman filtering on mean RSSI Beacon 1

                            //1st time: create a new state for kalman filter
                            if (meanKFRssis1.isEmpty()) {
                                meanAndKFRssi = kfBeacon1.smoothenRssiFirstTime(rssi);
                                meanKFRssis1.add(meanAndKFRssi);
                            }

                            //not 1st time: use current state from particular kalman filter
                            else {
                                meanAndKFRssi = kfBeacon1.smoothenRssi(rssi);
                                meanKFRssis1.add(meanAndKFRssi);
                            }

                            //adds smoothened value to hashmap -- used as current RSSI snapshot for online phase
                            hm1.put("Beacon1", meanAndKFRssi);

                            //resets all variables for next calculation
                            totalInputVal1 = 0;
                            inputCounter1 = 0;
                        }

                        break;
                    }
                    case "Beacon2": {
                        int meanAndKFRssi = 0;
                        int mean;
                        finalmean2 = 0;

                        totalInputVal2 += rssi;
                        inputCounter2++;

                        if (inputCounter2 == 10) {

                            //mean filtering on RSSI Beacon 1
                            mean = totalInputVal2 / inputCounter2;
                            finalmean2 = mean;

                            //kalman filtering on mean RSSI Beacon 1

                            //1st time: create a new state for kalman filter
                            if (meanKFRssis2.isEmpty()) {
                                meanAndKFRssi = kfBeacon2.smoothenRssiFirstTime(rssi);
                                meanKFRssis2.add(meanAndKFRssi);
                            }

                            //not 1st time: use current state from particular kalman filter
                            else {
                                meanAndKFRssi = kfBeacon2.smoothenRssi(rssi);
                                meanKFRssis2.add(meanAndKFRssi);
                            }

                            //adds smoothened value to hashmap -- used as current RSSI snapshot for online phase
                            hm1.put("Beacon2", meanAndKFRssi);

                            //resets all variables for next calculation
                            totalInputVal2 = 0;
                            inputCounter2 = 0;
                        }

                        break;
                    }
                    case "Beacon3": {
                        int meanAndKFRssi = 0;
                        int mean;
                        finalmean3 = 0;

                        totalInputVal3 += rssi;
                        inputCounter3++;

                        if (inputCounter3 == 10) {

                            //mean filtering on RSSI Beacon 1
                            mean = totalInputVal3 / inputCounter3;
                            finalmean3 = mean;

                            //kalman filtering on mean RSSI Beacon 1

                            //1st time: create a new state for kalman filter
                            if (meanKFRssis3.isEmpty()) {
                                meanAndKFRssi = kfBeacon3.smoothenRssiFirstTime(rssi);
                                meanKFRssis3.add(meanAndKFRssi);
                            }

                            //not 1st time: use current state from particular kalman filter
                            else {
                                meanAndKFRssi = kfBeacon3.smoothenRssi(rssi);
                                meanKFRssis3.add(meanAndKFRssi);
                            }

                            //adds smoothened value to hashmap -- used as current RSSI snapshot for online phase
                            hm1.put("Beacon3", meanAndKFRssi);

                            //resets all variables for next calculation
                            totalInputVal3 = 0;
                            inputCounter3 = 0;
                        }

                        break;
                    }
                    default:
                        Toast toast = Toast.makeText(EstimateLocation.this, "Scanned device is not one of the beacons", Toast.LENGTH_SHORT);
                        toast.show();
                        break;
                }
                    tvCurRssi.setText("CURRENT RSSI VALUES: \nBeacon1: " + hm1.getOrDefault("Beacon1", 0) + "\nBeacon2: " + hm1.getOrDefault("Beacon2", 0) + "\nBeacon3: " + hm1.getOrDefault("Beacon3", 0));
            }
        }
    };

    //start estimation
    public void startEstimation() {

        //get latest rssi values (MFKF)
        int r1 = hm1.getOrDefault("Beacon1", 0);
        int r2 = hm1.getOrDefault("Beacon2", 0);
        int r3 = hm1.getOrDefault("Beacon3", 0);

        //current rssi screenshot
        double[] arrCurRssi = new double[] {r1, r2, r3};
        Log.d("current rssi", arrCurRssi[0] + " " + arrCurRssi[1] + " " + arrCurRssi[2]);

        //get offline rssis as array
        DatabaseHandler handler = new DatabaseHandler(this);
        double[][] offlineDataset = handler.getRssiArray();
        for (int i = 0; i < offlineDataset.length; i++)
            for (int j = 0; j < offlineDataset[i].length; j++)
                Log.d("Offline Dataset Array", "Element at [" + i + "][" + j + "]: " + offlineDataset[i][j]);


        // ================================================== Euclidean Distance Calculation ==================================================

        //now has int[] online and int[][] offline
        //use euclidean distance to calculate distance between online and offline rssi values
        ArrayList<Double> on_off_distances = new ArrayList<Double> ();
        for(double[] offlineRow : offlineDataset) {
            EuclideanDistance formula = new EuclideanDistance();
            on_off_distances.add(formula.compute(arrCurRssi, offlineRow));
        }

        //now we have the result euclidean distance between each offline rssis and online rssi
        //store index : distance into a hashmap to later labeling
        //hmLabel range depends on size of on_off_distances
        HashMap<Integer, Double> hmLabel = new HashMap<>();
        for(int i=0; i<on_off_distances.size(); i++) {
                hmLabel.put(i, on_off_distances.get(i));
            }
        Log.d("Index:Distance HM Result", hmLabel.toString());

        //sort the euclidean distance arraylist into ascending order
        //and get the first kth elements for localization
        ArrayList<Double> sortedDistances = new ArrayList<Double>();
        for(int i=0; i<on_off_distances.size(); i++)
            sortedDistances.add(on_off_distances.get(i));
        Collections.sort(sortedDistances);

        Log.d("Before sorting", on_off_distances.toString());
        Log.d("After sorting", sortedDistances.toString());

        // ================================================== Kth Nearest Points ==================================================

        //get the indices of the first 3 elements
        ArrayList<Integer> indexList = new ArrayList<>();
        int k = 3;
        for(int i=0; i<k; i++)
            for(Map.Entry<Integer, Double> entry : hmLabel.entrySet())
                if(entry.getValue() == sortedDistances.get(i))
                    indexList.add(entry.getKey());

        //displays the K nearest points from current location
        Log.d("K nearest neighbours", indexList.toString());
        tvKNearestRefPt.setText("3 nearest neighbour point: " + indexList.toString());

        // ================================================== Location Estimation ==================================================

        //now have to estimate online coordinates of user
        //get the coordinates from these indexes
        float[] coor1, coor2, coor3;
        DatabaseHandler db = new DatabaseHandler(EstimateLocation.this);
        int rownum;
        rownum = indexList.get(0) + 1 ;
        String row1 = Integer.toString(rownum);
        coor1 = db.getCoordinates(new String[] {row1});

        rownum = indexList.get(1) + 1;
        String row2 = Integer.toString(rownum);
        coor2 = db.getCoordinates(new String[] {row2});

        rownum = indexList.get(2) + 1;
        String row3 = Integer.toString(rownum);
        coor3 = db.getCoordinates(new String[] {row3});

        Log.d("1st coordinates", coor1[0] + " " + coor1[1]);
        Log.d("2nd coordinates", coor2[0] + " " + coor2[1]);
        Log.d("3rd coordinates", coor3[0] + " " + coor3[1]);

        //estimate current coordinates of user using WKNN method
        double estX, estY, totalWeight;
        totalWeight = sortedDistances.get(0) + sortedDistances.get(1) + sortedDistances.get(2);
        estX = (coor1[0] * (1/sortedDistances.get(0)) + coor2[0] * (1/sortedDistances.get(1)) + coor3[0] * (1/sortedDistances.get(2))) / totalWeight;
        estY = (coor1[1] * (1/sortedDistances.get(0)) + coor2[1] * (1/sortedDistances.get(1)) + coor3[1] * (1/sortedDistances.get(2))) / totalWeight;

        //displays estimated coordinates
        Log.d("Estimated Location", estX + ", " + estY);
        tvEstLocation.setText("Estimated Location: (" + estX + ", " + estY + ")");

        stopScan();
    }

    private final String[] deviceMAC = new String[] {"C4:F2:E9:8B:3F:22", "D0:2A:EE:E2:AB:CE", "EB:7A:2E:78:8E:21"};

    //start scanning for ble signals
    private void startScan() {
        Log.i("startScan()", "Start scanning for BLE signals");

        //creates a new kalman filter object each time start scanning
        kfBeacon1 = new KalmanFilterHelper();
        kfBeacon2 = new KalmanFilterHelper();
        kfBeacon3 = new KalmanFilterHelper();

        //re-initialize all global variables
        meanKFRssis1 = new ArrayList<>();
        meanKFRssis2 = new ArrayList<>();
        meanKFRssis3 = new ArrayList<>();

        inputCounter1 = 0;
        inputCounter2 = 0;
        inputCounter3 = 0;

        totalInputVal1 = 0;
        totalInputVal2 = 0;
        totalInputVal3 = 0;

        finalmean1 = 0;
        finalmean2 = 0;
        finalmean3 = 0;

        List<ScanFilter> filters = null;
        filters = new ArrayList<>();

        for (String mac : deviceMAC) {
            ScanFilter filter = new ScanFilter.Builder().setDeviceAddress(mac).build();
            filters.add(filter);
        }

        List<ScanFilter> finalFilters = filters;

        AsyncTask.execute(() -> {
            try {
                ScanSettings scanSettings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                        //.setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                        .setReportDelay(0L)
                        .build();
                btScanner.startScan(finalFilters, scanSettings, leScanCallback);
            }
            catch (SecurityException e) {
                Log.v("startScanning", e.toString());
            }
        });
    }

    //stop scanning
    @SuppressLint("MissingPermission")
    public void stopScan() {

        Log.i("BLE Scanner: ", "Stop Scanning");
        Toast toast = Toast.makeText(this, "Stop scanning",Toast.LENGTH_SHORT);
        toast.show();
        btScanner.stopScan(leScanCallback);
    }

    //check permissions
    public static void checkPermissions(Activity act, Context con) {
        int permission_all = 1;

        String[] PERMISSIONS = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
        };

        if (!hasPermissions(con, PERMISSIONS)) {
            ActivityCompat.requestPermissions(act, PERMISSIONS, permission_all);
        }
    }

    //check has permission or not
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}