package my.edu.utar.fyp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    Button btnExportRss;
    Button btnTrackRss;
    DeviceListAdapter adapter;
    ListView lv;
    Map<String, Integer> hm = new HashMap<String, Integer>();
    DatabaseHandler handler = new DatabaseHandler(this);

    private float x = 0;
    private float y = 0;
    private String[] rowid = new String[]{};
    private int rowCounter = 1;
    private int trackRSSI = 0;


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //setting up TextViews, Buttons here
        btnStartScan = findViewById(R.id.btnStartScan);
        btnStopScan = findViewById(R.id.btnStopScan);
        btnRecordRss = findViewById(R.id.btnRecordRSS);
        btnExportRss = findViewById(R.id.btnExportRssi);
        btnTrackRss = findViewById(R.id.btnTrackRSS);
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

        //click on this button to stop scanning
        btnStopScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopScan();
                adapter.clearList();
                adapter.notifyDataSetChanged();
            }
        });

        //click on this button to snapshot and store rssi value
//        btnRecordRss.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                storeRSSI();
//            }
//        });

        //click on this button to start tracking rssi values for exporting them into csv file
        btnTrackRss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeTrackingStatus();
            }
        });

        //click on this button to export the rssi values into a csv file
        btnExportRss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptInputFileName();
            }
        });

    }


    //receives scan results
    KalmanFilterHelper kfHelper;
    KalmanFilterHelper kfHelper2;
    ArrayList<Integer> filteredRssis;
    ArrayList<Integer> meanKFRssis;
    int inputCounter;
    int totalInputVal;
    int mean;
    int finalmean;

    private final ScanCallback leScanCallback = new  ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            BluetoothDevice device = result.getDevice();
            int rssi = result.getRssi();
            @SuppressLint("MissingPermission") String deviceName = device.getName();

            if (deviceName != null) {

                int smoothedRssi = 0;
                int meanAndKFRssi = 0;
                finalmean = 0;

                //perform mean filter and kalman filter on RAW rssi values
                //if input counter has not reached 10, adds raw rssi into the totalInputVal
                totalInputVal += rssi;
                inputCounter++;

                //if reaches 10 inputs, perform mean filtering and reset variables
                //then perform kalman filtering on the mean filtered values as well
                if(inputCounter == 10) {

                    //perform mean filtering
                    mean = totalInputVal / inputCounter;
                    finalmean = mean;

                    //perform kalman filtering on mean values
                    //if list is empty -> means first input -> create a state in kfhelper
                    if(meanKFRssis.isEmpty()) {
                        meanAndKFRssi = kfHelper.smoothenRssiFirstTime(mean);
                        meanKFRssis.add(meanAndKFRssi);
                    }

                    //if list has values -> means not first input -> use the same state within the kfhelper
                    else {
                        meanAndKFRssi = kfHelper.smoothenRssi(mean);
                        meanKFRssis.add(meanAndKFRssi);
                    }

                    //resets all related variables
                    totalInputVal = 0;
                    inputCounter = 0;
                    mean = 0;
                }

                //working on raw rssi values directly
                //if filtered rssi is empty, smoothen the RAW rssi value and put it inside
                //because the state can only be initialized with an rssi value, so have to create another function to make sure the state does not get reset
                if (filteredRssis.isEmpty()) {
                    smoothedRssi = kfHelper2.smoothenRssiFirstTime(rssi);
                    filteredRssis.add(smoothedRssi);
                }

                //if filtered rssi is not empty, take the latest RAW rssi value and filter the rssi value
                //uses the same kalmanfilterhelper object to make sure it is correct
                else {
                    smoothedRssi = kfHelper2.smoothenRssi(rssi);
                    filteredRssis.add(smoothedRssi);
                }

                //updates the textviews in the list to display to users
                adapter.addDevice(device, rssi, smoothedRssi, meanAndKFRssi, finalmean);
                adapter.notifyDataSetChanged();

                //if enabled (by pressing the button), record to list for exporting to csv
                if (trackRSSI == 1) recordToList(deviceName, rssi, smoothedRssi, finalmean, meanAndKFRssi);

                Log.i("leScanCallback", "Device name: " + deviceName + " " + "Device RSSI: "+ rssi + " " + "Mean RSSI: " + finalmean + " " + "MFKF Filtered RSSI: " + meanAndKFRssi);

                //log device and its rssi received
//                hm.put(deviceName, rssi);
//                for (Map.Entry<String,Integer> entry : hm.entrySet())
//                    Log.i("leScanCallback", " HashMap result after onScanResult(): Key: " + entry.getKey() + " Value: " + entry.getValue());
            }
        }
    };


    //lists that are used to track down data for exporting to csv file
    //stop scanning will not clear these values -> so that user can click on "export to csv file" button after stop scanning
    //but if want to scan RSSI values again, will clear these values -> means that track new data for exporting
    ArrayList<String> beaconNameList;
    ArrayList<Integer> rssiList;
    ArrayList<Integer> kfRssiList;
    ArrayList<String> mfRssiList;
    ArrayList<String> mfkfRssiList;
    ArrayList<String> timestampList;

    //method to record down beacon name, rssi and timestamp into a csv file
    public void recordToList (String beaconName, int rssi, int kfRssi, int mfRssi, int mfkfRssi) {
        beaconNameList.add(beaconName);
        rssiList.add(rssi);
        kfRssiList.add(kfRssi);

        if (mfRssi == 0) mfRssiList.add("");
        else mfRssiList.add(Integer.toString(mfRssi));

        if (mfkfRssi == 0) mfkfRssiList.add("");
        else mfkfRssiList.add(Integer.toString(mfkfRssi));

        long curTimeMillis = System.currentTimeMillis();
        Date curDate = new Date(curTimeMillis);
        String dateFormat = "yyyy-MM-dd HH:mm:ss.SSS";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.ENGLISH);
        String formattedTimestamp = sdf.format(curDate);
        timestampList.add(formattedTimestamp);

        Log.i("Record to List", "Beacon: " + beaconName + "; RSSI: " + rssi + "; KF Rssi:" + kfRssi + "; MF Rssi: " + mfRssi + "; MFKF Rssi: " + mfkfRssi + "; Timestamp: " + formattedTimestamp);
    }

    //method to export the beacon names, rssi values and timestamp into csv file
    //only runs after the alert dialog box appears for user to input file name
    //after exporting, will still remain data for exporting again into different file name
    public void exportToCSV (String dialogValue) {

        String fName = dialogValue;
        String fileName = fName + ".csv";
        String folderName = "myCSVFolder";
        Log.i("fName", fName);

        if(isExternalStorageWritable()) {

            File folder = new File(MainActivity.this.getExternalFilesDir(null), folderName);

            if(!folder.exists()) folder.mkdirs();

            File csvFile = new File(folder, fileName);
            try {
                FileWriter writer = new FileWriter(csvFile);
                writer.write("Beacon,RSSI,KF Rssi,MF Rssi,MFKF Rssi,Timestamp\n");

                for(int i = 0; i < beaconNameList.size(); i++) {
                    String line = beaconNameList.get(i) + "," + rssiList.get(i) + "," + kfRssiList.get(i) + "," + mfRssiList.get(i) + "," + mfkfRssiList.get(i) + "," +  timestampList.get(i) + "\n";
                    writer.write(line);
                }

                writer.close();
                Log.i("CSV writing", "CSV file written");
                Toast toast = Toast.makeText(MainActivity.this, "CSV file exported", Toast.LENGTH_SHORT);
                toast.show();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //creates an alert dialog for user to input file name for csv file
    //after prompting file name -> will proceed to next function to write data into csv file (either using default name or user input as file name)
    EditText edittextinput;
    public void promptInputFileName() {

        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);

        alert.setTitle("Enter File Name");
        alert.setMessage("Enter name for your csv file");
        edittextinput = new EditText(MainActivity.this);
        alert.setView(edittextinput);

        //if "submit", gets input from edittext and write data to csv file
        alert.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String value = edittextinput.getText().toString();
                exportToCSV(value);
                dialogInterface.cancel();
            }
        });

        //if "use default naming", use default name for csv file and write data to it
        alert.setNegativeButton("Use default naming", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String value = "defaultdata";
                exportToCSV(value);
                dialogInterface.cancel();
            }
        });

        AlertDialog alertDialog = alert.create();
        alertDialog.show();

    }

    //change tracking status to enabled or disabled -- to enable the system to track the RSSI values for exporting later
    public void changeTrackingStatus() {
        if(trackRSSI == 0) {
            trackRSSI = 1;
            Toast toast = Toast.makeText(MainActivity.this, "Start tracking RSSI values", Toast.LENGTH_SHORT);
            toast.show();
        }

        else {
            trackRSSI = 0;
            Toast toast = Toast.makeText(MainActivity.this, "Stop tracking RSSI values", Toast.LENGTH_SHORT);
            toast.show();
        }

    }

    int rssi1 = 0, rssi2 = 0, rssi3 = 0;
    int avgrssi1 = 0, avgrssi2 = 0, avgrssi3 = 0;

    //method to store RSSI values from all 3 beacons into database
    //but need to decide which to use
    public void storeRSSI() {
        Toast toast1 = Toast.makeText(MainActivity.this, "Point " + rowCounter + "logging", Toast.LENGTH_SHORT);
        toast1.show();
        Log.i("storeRSSI(): ", "running");
        Log.i("storeRSSI(): ", "Logging Point " + rowCounter);
        rssi1=0;
        rssi2=0;
        rssi3=0;
        avgrssi1=0;
        avgrssi2=0;
        avgrssi3=0;

        //
        if(rowCounter == 26) {
            Toast toast = Toast.makeText(MainActivity.this, "All 25 rows recorded!", Toast.LENGTH_SHORT);
            Log.i("storeRSSI()", "All points logged");
            toast.show();
            return;
        }

        final int delay = 2000;
        final Handler eventHandler = new Handler();

        eventHandler.postDelayed(new Runnable() {
            int count = 0;
            @Override
            public void run() {
                rssi1 += hm.getOrDefault("Beacon1", 0);
                rssi2 += hm.getOrDefault("Beacon2", 0);
                rssi3 += hm.getOrDefault("Beacon3", 0);
                Log.i("storeRSSI():",  "RSSI Logged " + count + " Values " + hm.getOrDefault("Beacon1", rssi1) + " " + hm.getOrDefault("Beacon2", rssi2) + " " + hm.getOrDefault("Beacon3", rssi3));
                count++;
                if(count < 10) eventHandler.postDelayed(this, delay);
                if (count == 10) {
                    avgrssi1 = rssi1/10;
                    avgrssi2 = rssi2/10;
                    avgrssi3 = rssi3/10;

                    Log.i("storeRSSI()", "Total values of RSSI " + count + " Values " + rssi1 + " " + rssi2 + " " + rssi3);

                    Date date = new Date();
                    String dateFormat = "yyyy-MM-dd HH:mm:ss";
                    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
                    String dateStr = sdf.format(date);

                    rowid = new String[] {Integer.toString(rowCounter)};

                    Log.i("storeRSSI()", "Total values of RSSI " + count + " Values " + avgrssi1 + " " + avgrssi2 + " " + avgrssi3);

                    handler.updateRows(rowid, avgrssi1, avgrssi2, avgrssi3, dateStr);

                    Toast toast = Toast.makeText(MainActivity.this, "Point " + rowCounter + "loaded", Toast.LENGTH_SHORT);
                    toast.show();
                    rowCounter++;
                }
            }
        }, delay);
    }
    //list adapter for displaying ble devices on list view
    public static class DeviceListAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<BluetoothDevice> devices;
        private ArrayList<Integer> rssis;
        private ArrayList<Integer> kfRssis;
        private ArrayList<Integer> mfkfRssis;
        private ArrayList<Integer> mfRssis;

        public DeviceListAdapter(Context context) {
            super();
            this.context = context;
            devices = new ArrayList<>();
            rssis = new ArrayList<>();
            kfRssis = new ArrayList<>();
            mfkfRssis = new ArrayList<>();
            mfRssis = new ArrayList<>();
        }

        public void addDevice(BluetoothDevice device, int rssi, int kfRssi, int mfkfRssi, int mfRssi) {
            if(!devices.contains(device)) {
                devices.add(device);
                rssis.add(rssi);
                kfRssis.add(kfRssi);
                mfkfRssis.add(mfkfRssi);
                mfRssis.add(mfRssi);
            }

            else {
                int position = devices.indexOf(device);
                if (position != -1) {
                    rssis.set(position, rssi);
                    kfRssis.set(position, kfRssi);
                    mfkfRssis.set(position, mfkfRssi);
                    mfRssis.set(position, mfRssi);
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
            TextView tvDeviceKFRssi = view.findViewById(R.id.tvDeviceKFRssi);
            TextView tvDeviceMFRssi = view.findViewById(R.id.tvDeviceMFRssi);
            TextView tvDeviceMFKFRssi = view.findViewById(R.id.tvDeviceMFKFRssi);

            tvDeviceName.setText(devices.get(pos).getName());
            tvDeviceRSSI.setText("Raw RSSI value: " + rssis.get(pos).toString() + "dbm");
            tvDeviceKFRssi.setText("Kalman Filtered RSSI value: " + kfRssis.get(pos).toString() + "dbm");
            tvDeviceMFRssi.setText("Mean filtered RSSI value: " + mfRssis.get(pos).toString() + "dbm");
            tvDeviceMFKFRssi.setText("Mean & Kalman Filtered RSSI value: " + mfkfRssis.get(pos).toString() + "dbm");

            return view;
        }

    }

    //start scanning for signals - only get the RSSI from the 3 beacons
    //resets all global variables before starting as this is the first function to be called -> if not will use previous data for next scanning activity
    public void startScan() {
        Log.i("startScan()", "Start scanning for BLE signals");
        Toast toast = Toast.makeText(this, "Start scanning for BLE signals",Toast.LENGTH_SHORT);
        toast.show();

        //creates a new kalman filter object each time start scanning
        kfHelper = new KalmanFilterHelper();
        kfHelper2 = new KalmanFilterHelper();

        //re-initialize all global variables
        filteredRssis = new ArrayList<>();
        meanKFRssis = new ArrayList<>();
        inputCounter = 0;
        totalInputVal = 0;
        mean = 0;
        finalmean = 0;
        beaconNameList = new ArrayList<>();
        rssiList = new ArrayList<>();
        timestampList = new ArrayList<>();
        mfkfRssiList = new ArrayList<>();
        kfRssiList = new ArrayList<>();
        mfRssiList = new ArrayList<>();

        //preparing filters for scanning BLE signals
        List<ScanFilter> filters = null;
        filters = new ArrayList<>();

        //uses MAC address to filter out BLE beacons
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

    //stop scanning for ble signals
    //resetting variables is not done here as still need data for exporting to csv later if required
    @SuppressLint("MissingPermission")
    public void stopScan() {

        //stop tracking RSSI values if it has been enabled
        if (trackRSSI == 1) changeTrackingStatus();

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
                Manifest.permission.WRITE_EXTERNAL_STORAGE
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

    //check whether external storage is writable or not
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
