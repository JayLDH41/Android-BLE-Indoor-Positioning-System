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
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    }



    //receives scan results
    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            BluetoothDevice device = result.getDevice();
            int rssi = result.getRssi();
            @SuppressLint("MissingPermission") String deviceName = device.getName();

            if (deviceName != null) {
                Log.i("leScanCallback", "Scan Result: " + deviceName + " " + "Device RSSI: " + rssi);
            }
        }
    };

    private final String[] deviceMAC = new String[] {"C4:F2:E9:8B:3F:22", "D0:2A:EE:E2:AB:CE", "EB:7A:2E:78:8E:21"};

    //start scanning for ble signals
    private void startScan() {
        Log.i("startScan()", "Start scanning for BLE signals");

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