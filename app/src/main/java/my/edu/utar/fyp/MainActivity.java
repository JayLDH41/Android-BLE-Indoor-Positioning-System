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
    Map<String, Integer> hm;
    DatabaseHandler dbHandler = new DatabaseHandler(this);

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
        btnRecordRss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                storeRSSI();
            }
        });

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
                exportToCSV();
            }
        });

    }


    //receives scan results
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

                            //resets all variables for next calculation
                            totalInputVal1 = 0;
                            inputCounter1 = 0;
                        }

                        //displays data to user
                        adapter.addDevice(device, rssi, finalmean1, meanAndKFRssi);
                        adapter.notifyDataSetChanged();

                        //if enabled tracking, record to list for exporting later
                        if (trackRSSI == 1)
                            recordToList(deviceName, rssi, finalmean1, meanAndKFRssi);

                        //adds smoothened value to hashmap -- for storing values into database later
                        if (meanAndKFRssi != 0) hm.put("Beacon1", meanAndKFRssi);

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

                            //resets all variables for next calculation
                            totalInputVal2 = 0;
                            inputCounter2 = 0;
                        }

                        //displays data to user
                        adapter.addDevice(device, rssi, finalmean2, meanAndKFRssi);
                        adapter.notifyDataSetChanged();

                        //if enabled tracking, record to list for exporting later
                        if (trackRSSI == 1)
                            recordToList(deviceName, rssi, finalmean2, meanAndKFRssi);

                        //stores latest final RSSI to hashmap for storing to database
                        if (meanAndKFRssi != 0) hm.put("Beacon2", meanAndKFRssi);

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

                            //resets all variables for next calculation
                            totalInputVal3 = 0;
                            inputCounter3 = 0;
                        }

                        //displays data to user
                        adapter.addDevice(device, rssi, finalmean3, meanAndKFRssi);
                        adapter.notifyDataSetChanged();

                        //if enabled tracking, record to list for exporting later
                        if (trackRSSI == 1)
                            recordToList(deviceName, rssi, finalmean3, meanAndKFRssi);

                        //stores latest final RSSI to hashmap for storing to database
                        if (meanAndKFRssi != 0) hm.put("Beacon3", meanAndKFRssi);
                        break;
                    }
                    default:
                        Toast toast = Toast.makeText(MainActivity.this, "Scanned device is not one of the beacons", Toast.LENGTH_SHORT);
                        toast.show();
                        break;
                }
            }
        }
    };


    //lists that are used to track down data for exporting to csv file
    //stop scanning will not clear these values -> so that user can click on "export to csv file" button after stop scanning
    //but if want to scan RSSI values again, will clear these values -> means that track new data for exporting
    ArrayList<String> beaconNameList1, beaconNameList2, beaconNameList3;
    ArrayList<Integer> rssiList1, rssiList2, rssiList3;
    ArrayList<String> mfRssiList1, mfRssiList2, mfRssiList3;
    ArrayList<String> mfkfRssiList1, mfkfRssiList2, mfkfRssiList3;
    ArrayList<String> timestampList1, timestampList2, timestampList3;

    //method to record down beacon name, rssi and timestamp into a csv file
    //3 beacons data store same lists -> only differentiate into different files when exporting to csv file
    public void recordToList (String beaconName, int rssi, int mfRssi, int mfkfRssi) {

        if (beaconName.equals("Beacon1")) {
            beaconNameList1.add(beaconName);
            rssiList1.add(rssi);

            if (mfRssi == 0) mfRssiList1.add("");
            else mfRssiList1.add(Integer.toString(mfRssi));

            if (mfkfRssi == 0) mfkfRssiList1.add("");
            else mfkfRssiList1.add(Integer.toString(mfkfRssi));

            long curTimeMillis = System.currentTimeMillis();
            Date curDate = new Date(curTimeMillis);
            String dateFormat = "yyyy-MM-dd HH:mm:ss.SSS";
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.ENGLISH);
            String formattedTimestamp = sdf.format(curDate);
            timestampList1.add(formattedTimestamp);

            Log.i("Record to List", "Beacon: " + beaconName + "; RSSI: " + rssi +  "; MF Rssi: " + mfRssi + "; MFKF Rssi: " + mfkfRssi + "; Timestamp: " + formattedTimestamp);
        }

        if (beaconName.equals("Beacon2")) {
            beaconNameList2.add(beaconName);
            rssiList2.add(rssi);

            if (mfRssi == 0) mfRssiList2.add("");
            else mfRssiList2.add(Integer.toString(mfRssi));

            if (mfkfRssi == 0) mfkfRssiList2.add("");
            else mfkfRssiList2.add(Integer.toString(mfkfRssi));

            long curTimeMillis = System.currentTimeMillis();
            Date curDate = new Date(curTimeMillis);
            String dateFormat = "yyyy-MM-dd HH:mm:ss.SSS";
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.ENGLISH);
            String formattedTimestamp = sdf.format(curDate);
            timestampList2.add(formattedTimestamp);

            Log.i("Record to List", "Beacon: " + beaconName + "; RSSI: " + rssi +  "; MF Rssi: " + mfRssi + "; MFKF Rssi: " + mfkfRssi + "; Timestamp: " + formattedTimestamp);
        }

        if (beaconName.equals("Beacon3")) {
            beaconNameList3.add(beaconName);
            rssiList3.add(rssi);

            if (mfRssi == 0) mfRssiList3.add("");
            else mfRssiList3.add(Integer.toString(mfRssi));

            if (mfkfRssi == 0) mfkfRssiList3.add("");
            else mfkfRssiList3.add(Integer.toString(mfkfRssi));

            long curTimeMillis = System.currentTimeMillis();
            Date curDate = new Date(curTimeMillis);
            String dateFormat = "yyyy-MM-dd HH:mm:ss.SSS";
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.ENGLISH);
            String formattedTimestamp = sdf.format(curDate);
            timestampList3.add(formattedTimestamp);

            Log.i("Record to List", "Beacon: " + beaconName + "; RSSI: " + rssi +  "; MF Rssi: " + mfRssi + "; MFKF Rssi: " + mfkfRssi + "; Timestamp: " + formattedTimestamp);
        }
    }

    //method to export the beacon names, rssi values and timestamp into csv file
    //export to 3 different files for different beacon
    public void exportToCSV () {

        String fileName1 = "beacon1.csv";
        String fileName2 = "beacon2.csv";
        String fileName3 = "beacon3.csv";

        String folderName = "myCSVFolder";

        if(isExternalStorageWritable()) {

            //create folder for storing files
            File folder = new File(MainActivity.this.getExternalFilesDir(null), folderName);
            if(!folder.exists()) folder.mkdirs();

            //beacon 1 exporting
            File file = new File(folder, fileName1);
            try {
                FileWriter writer = new FileWriter(file);
                writer.write("Beacon,RSSI,MF Rssi,MFKF Rssi,Timestamp\n");

                for(int i = 0; i < beaconNameList1.size(); i++) {
                    String line = beaconNameList1.get(i) + "," + rssiList1.get(i) +  "," + mfRssiList1.get(i) + "," + mfkfRssiList1.get(i) + "," +  timestampList1.get(i) + "\n";
                    writer.write(line);
                }

                writer.close();
                Log.i("CSV Beacon 1 writing", "CSV Beacon 1 file written");
                Toast toast = Toast.makeText(MainActivity.this, "CSV for Beacon 1 file exported", Toast.LENGTH_SHORT);
                toast.show();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            //beacon 2 exporting
            file = new File(folder, fileName2);
            try {
                FileWriter writer = new FileWriter(file);
                writer.write("Beacon,RSSI,MF Rssi,MFKF Rssi,Timestamp\n");

                for(int i = 0; i < beaconNameList2.size(); i++) {
                    String line = beaconNameList2.get(i) + "," + rssiList2.get(i) +  "," + mfRssiList2.get(i) + "," + mfkfRssiList2.get(i) + "," +  timestampList2.get(i) + "\n";
                    writer.write(line);
                }

                writer.close();
                Log.i("CSV Beacon 2 writing", "CSV Beacon 2 file written");
                Toast toast = Toast.makeText(MainActivity.this, "CSV for Beacon 2 file exported", Toast.LENGTH_SHORT);
                toast.show();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            //beacon 3 exporting
            file = new File(folder, fileName3);
            try {
                FileWriter writer = new FileWriter(file);
                writer.write("Beacon,RSSI,MF Rssi,MFKF Rssi,Timestamp\n");

                for(int i = 0; i < beaconNameList3.size(); i++) {
                    String line = beaconNameList3.get(i) + "," + rssiList3.get(i) +  "," + mfRssiList3.get(i) + "," + mfkfRssiList3.get(i) + "," +  timestampList3.get(i) + "\n";
                    writer.write(line);
                }

                writer.close();
                Log.i("CSV Beacon 3 writing", "CSV Beacon 3 file written");
                Toast toast = Toast.makeText(MainActivity.this, "CSV for Beacon 3 file exported", Toast.LENGTH_SHORT);
                toast.show();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
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

    //method to store RSSI values from all 3 beacons into database
    int totalrssi1 = 0, totalrssi2 = 0, totalrssi3 = 0;
    int avgrssi1 = 0, avgrssi2 = 0, avgrssi3 = 0;
    float xcoor, ycoor;
    public void storeRSSI() {
        Toast toast1 = Toast.makeText(MainActivity.this, "Point " + rowCounter + " logging", Toast.LENGTH_SHORT);
        toast1.show();
        //Log.i("storeRSSI(): ", "running");
        //Log.i("storeRSSI(): ", "Logging Point " + rowCounter);

        //resets all variables when the store button rssi is clicked
        avgrssi1 = 0;
        avgrssi2 = 0;
        avgrssi3 = 0;
        totalrssi1 = 0;
        totalrssi2 = 0;
        totalrssi3 = 0;
        xcoor = 0;
        ycoor = 0;

        //prepare Handler for running calculation and storing of RSSI values for the point
        //here takes up to 60 seconds to collect MFKF RSSI values -> calculate average -> store into DB
        final Handler handler = new Handler();
        final int delay = 1000;
        handler.postDelayed(new Runnable() {

            //aim: 60 counts, each count 1 second
            int count = 0;
            @Override
            public void run() {

                //adds the latest MFKF RSSI values to totalrssi
                totalrssi1 += hm.getOrDefault("Beacon1", 0);
                totalrssi2 += hm.getOrDefault("Beacon2", 0);
                totalrssi3 += hm.getOrDefault("Beacon3", 0);

                //Log.i("storeRSSI():",  "RSSI Logged " + count + " Values " + hm.getOrDefault("Beacon1", 0) + " " + hm.getOrDefault("Beacon2", 0) + " " + hm.getOrDefault("Beacon3", 0));
                //Log.d("Count " + count, totalrssi1 + " " + totalrssi2 + " " + totalrssi3);
                count++;

                //if haven't reached 60 seconds / entries, wait 1 second
                if(count < 60) handler.postDelayed(this, delay);

                // ================================================== Calculate Avg MFKF RSSI & Store it ==================================================

                //if reach 60 seconds / entries => calculate the average RSSI and store it into database
                if (count == 60) {
                    avgrssi1 = totalrssi1 / 60;
                    avgrssi2 = totalrssi2 / 60;
                    avgrssi3 = totalrssi3 / 60;

                    //Log.d("Total RSSI values for Point " + rowCounter, totalrssi1 + " " + totalrssi2 + " " + totalrssi3);
                    //Log.d("Avg RSSI values for Point " + rowCounter, avgrssi1 + " " + avgrssi2 + " " + avgrssi3);

                    Date date = new Date();
                    String dateFormat = "yyyy-MM-dd HH:mm:ss";
                    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
                    String dateStr = sdf.format(date);

                    // ================================================== Prompt X Y Coordinates from user ==================================================

                    //use alert dialog to prompt user to input X Y coordinates of current point for storing
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    final EditText input = new EditText(MainActivity.this);

                    builder.setView(input);
                    builder.setMessage("Enter X coordinate for this point");
                    builder.setTitle("Logging Point Coordinates");
                    builder.setCancelable(false);

                    builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String s = input.getText().toString();
                            xcoor = Float.parseFloat(s);
                            Toast toast = Toast.makeText(MainActivity.this, "X coordinate logged", Toast.LENGTH_SHORT);
                            toast.show();

                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder = new AlertDialog.Builder(MainActivity.this);
                            final EditText input2 = new EditText(MainActivity.this);

                            builder.setView(input2);
                            builder.setMessage("Enter Y Coordinate for this point");
                            builder.setTitle("Logging Point Coordinates");
                            builder.setCancelable(false);

                            builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    String s = input2.getText().toString();
                                    ycoor = Float.parseFloat(s);
                                    Toast  toast = Toast.makeText(MainActivity.this, "Y Coordinate logged", Toast.LENGTH_SHORT);
                                    toast.show();

                                    Log.d("X Y coordinates", xcoor + ", " + ycoor);

                                    //stores row into database
                                    rowid = new String[] {Integer.toString(rowCounter)};
                                    dbHandler.addRow(xcoor, ycoor, avgrssi1, avgrssi2, avgrssi3);

                                    Toast toast1 = Toast.makeText(MainActivity.this, "Point " + rowCounter + " loaded", Toast.LENGTH_SHORT);
                                    toast1.show();
                                    rowCounter++;
                                }
                            });
                            builder.show();
                        }
                    });

                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });

                    builder.show();

                    //no need to consider resetting variables here as I set it to reset whenever user clicks the store rssi button
                }
            }
        }, delay);
    }



    //list adapter for displaying ble devices on list view
    public static class DeviceListAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<BluetoothDevice> devices;
        private ArrayList<Integer> rssis;
        private ArrayList<Integer> mfkfRssis;
        private ArrayList<Integer> mfRssis;

        public DeviceListAdapter(Context context) {
            super();
            this.context = context;
            devices = new ArrayList<>();
            rssis = new ArrayList<>();
            mfkfRssis = new ArrayList<>();
            mfRssis = new ArrayList<>();
        }

        public void addDevice(BluetoothDevice device, int rssi, int mfkfRssi, int mfRssi) {
            if(!devices.contains(device)) {
                devices.add(device);
                rssis.add(rssi);
                mfkfRssis.add(mfkfRssi);
                mfRssis.add(mfRssi);
            }

            else {
                int position = devices.indexOf(device);
                if (position != -1) {
                    rssis.set(position, rssi);
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
            TextView tvDeviceMFRssi = view.findViewById(R.id.tvDeviceMFRssi);
            TextView tvDeviceMFKFRssi = view.findViewById(R.id.tvDeviceMFKFRssi);

            tvDeviceName.setText(devices.get(pos).getName());
            tvDeviceRSSI.setText("Raw RSSI value: " + rssis.get(pos).toString() + "dbm");
            tvDeviceMFRssi.setText("Mean filtered RSSI value: " + mfRssis.get(pos).toString() + "dbm");
            tvDeviceMFKFRssi.setText("Mean & Kalman Filtered RSSI value: " + mfkfRssis.get(pos).toString() + "dbm");

            return view;
        }

    }

    //start scanning for signals - only get the RSSI from the 3 beacons
    //resets all global variables before starting as this is the first function to be called -> if not will use previous data for next scanning activity
    public void startScan() {
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

        beaconNameList1 = new ArrayList<>();
        beaconNameList2 = new ArrayList<>();
        beaconNameList3 = new ArrayList<>();

        rssiList1 = new ArrayList<>();
        rssiList2 = new ArrayList<>();
        rssiList3 = new ArrayList<>();

        timestampList1 = new ArrayList<>();
        timestampList2 = new ArrayList<>();
        timestampList3 = new ArrayList<>();

        mfkfRssiList1 = new ArrayList<>();
        mfkfRssiList2 = new ArrayList<>();
        mfkfRssiList3 = new ArrayList<>();

        mfRssiList1 = new ArrayList<>();
        mfRssiList2 = new ArrayList<>();
        mfRssiList3 = new ArrayList<>();

        hm = new HashMap<String, Integer>();

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
