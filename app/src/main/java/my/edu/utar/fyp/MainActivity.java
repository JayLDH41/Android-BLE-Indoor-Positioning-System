package my.edu.utar.fyp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
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
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_ENABLE_BT = 1;
    private ArrayList<String> resultListName = new ArrayList<>();
    private final String[] deviceMAC = new String[] {"C4:F2:E9:8B:3F:22", "D0:2A:EE:E2:AB:CE", "EB:7A:2E:78:8E:21"};
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    Button btnStartScan;
    Button btnStopScan;
    Button btnRecordRss;
    DeviceListAdapter adapter;
    ListView lv;
    Map<String, Integer> hm = new HashMap<String, Integer>();
    DatabaseHandler handler = new DatabaseHandler(this);

    private float x = 0;
    private float y = 0;
    private String[] rowid = new String[]{};
    private int rowCounter = 1;


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //setting up TextViews, Buttons here
        btnStartScan = findViewById(R.id.btnStartScan);
        btnStopScan = findViewById(R.id.btnStopScan);
        btnRecordRss = findViewById(R.id.btnRecordRSS);
        lv = findViewById(R.id.lvScanResults);

        //setting up list adapter
        adapter = new DeviceListAdapter(this);
        lv.setAdapter(adapter);

        //asking for permissions
        checkPermissions(this, this);

        //check bluetooth turned on or not
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();
        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        //set a button to start scanning for signal
        btnStartScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btScanner != null) {
                    startScan();
                }
            }
        });

        //set a button to stop scanning for signal
        btnStopScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopScan();
                adapter.clearList();
                adapter.notifyDataSetChanged();
            }
        });

        btnRecordRss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                storeRSSI();
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
                Log.i("Adding device to list adapter & hashmap: ", "Device name: " + deviceName + " " + "Device RSSI: " + rssi);
                adapter.addDevice(device, rssi);
                adapter.notifyDataSetChanged();
                hm.put(deviceName, rssi);
                for (Map.Entry<String,Integer> entry : hm.entrySet())
                    Log.i("HashMap result after onScanResult(): ", "Key: " + entry.getKey() + " Value: " + entry.getValue());
            }
        }
    };

    //method to store RSSI values into database
    public void storeRSSI() {
        Log.i("storeRefPoint(): ", "storing RSSI values into database");

        if(rowCounter == 25) {
            Toast toast = Toast.makeText(MainActivity.this, "All 25 rows recorded!", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        int rssi1, rssi2, rssi3;

        rssi1 = hm.getOrDefault("Beacon1", 0);
        rssi2 = hm.getOrDefault("Beacon2", 0);
        rssi3 = hm.getOrDefault("Beacon3", 0);

        Log.i("RSSI values: ", "Beacon1: " + rssi1 + " Beacon2: " + rssi2 + " Beacon3: " + rssi3);

        Date date = new Date();
        String dateFormat = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        String dateStr = sdf.format(date);

        rowid = new String[] {Integer.toString(rowCounter)};

        handler.updateRows(rowid, rssi1, rssi2, rssi3, dateStr);

        rowCounter++;


    }

    //list adapter for displaying ble devices
    public static class DeviceListAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<BluetoothDevice> devices;
        private ArrayList<Integer> rssis;

        public DeviceListAdapter(Context context) {
            super();
            this.context = context;
            devices = new ArrayList<>();
            rssis = new ArrayList<>();
        }

        public void addDevice(BluetoothDevice device, int rssi) {
            if(!devices.contains(device)) {
                devices.add(device);
                rssis.add(rssi);
            }

            else {
                int position = devices.indexOf(device);
                if (position != -1) {
                    rssis.set(position, rssi);
                }
            }
        }

        public BluetoothDevice getDevice(int pos) {
            return devices.get(pos);
        }

        public void clearList() {
            devices.clear();
            rssis.clear();
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int pos) {
            return devices.get(pos);
        }

        @Override
        public long getItemId(int pos) {
            return pos;
        }

        @SuppressLint("MissingPermission")
        @Override
        public View getView(int pos, View convertView, ViewGroup viewGroup) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.activity_list_device, viewGroup, false);

            TextView tvDeviceName = view.findViewById(R.id.tvDeviceName);
            TextView tvDeviceRSSI = view.findViewById(R.id.tvDeviceRSSI);

            tvDeviceName.setText(devices.get(pos).getName());
            tvDeviceRSSI.setText(rssis.get(pos).toString() + "dbm");

            return view;
        }

    }

    //start scanning for signals
    public void startScan() {
        Log.i("BLE Scanner: ", "Start scanning");

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