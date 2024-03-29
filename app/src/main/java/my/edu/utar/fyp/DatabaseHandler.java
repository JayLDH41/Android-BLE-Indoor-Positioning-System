package my.edu.utar.fyp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static final String db_name = "offline_fingerprint.db";
    private static final int db_ver = 1;
//    private static final String key_id = "ID";
    private static final String key_beacon1 = "Beacon_1";
    private static final String key_beacon2 = "Beacon_2";
    private static final String key_beacon3 = "Beacon_3";
    private static final String key_timestamp = "Timestamp";
    private static final String key_x_coord = "X_Coordinate";
    private static final String key_y_coord = "Y_Coordinate";
    private static final String table_name = "Fingerprint_RSSI";

    //returns all rssi values in array format
    public double[][] getRssiArray() {
        SQLiteDatabase db = getWritableDatabase();
        String[] columns = new String[] {key_beacon1, key_beacon2, key_beacon3};
        Cursor cursor = db.query(table_name, columns, null, null, null, null, null);

        int rssi1Index = cursor.getColumnIndex(key_beacon1);
        int rssi2Index = cursor.getColumnIndex(key_beacon2);
        int rssi3Index = cursor.getColumnIndex(key_beacon3);

        ArrayList<Double[]> rssiArrList = new ArrayList<>();

        for (cursor.moveToFirst(); !(cursor.isAfterLast()); cursor.moveToNext()){
            double rssi1 = cursor.getInt(rssi1Index);
            double rssi2 = cursor.getInt(rssi2Index);
            double rssi3 = cursor.getInt(rssi3Index);

            rssiArrList.add(new Double[]{rssi1, rssi2, rssi3});
        }

        //convert arraylist<Integer[]> to int[][]
        double[][] arrRssi = new double[rssiArrList.size()][];
        for(int i=0; i<rssiArrList.size(); i++)
            arrRssi[i] = Arrays.stream(rssiArrList.get(i)).mapToDouble(Double::doubleValue).toArray();

        return arrRssi;
    }

//    returns coordinates given the point number
    public float[] getCoordinates(String[] rowId) {
        SQLiteDatabase db = getWritableDatabase();
        String[] columns = new String[] {"rowid", key_x_coord, key_y_coord};
        String whereClause = "rowid=?";
        Cursor cursor = db.query(table_name, columns, whereClause, rowId, null, null, null);

        int xcoorIndex = cursor.getColumnIndex(key_x_coord);
        int ycoorIndex = cursor.getColumnIndex(key_y_coord);
        float[] coordinates = new float[0];

        if(cursor.moveToFirst()) {
            float xcoor = cursor.getFloat(xcoorIndex);
            float ycoor = cursor.getFloat(ycoorIndex);
            coordinates = new float[] {xcoor, ycoor};
        }

        return coordinates;
    }
    
    //returns information of the specific row
    public String getRecordStr(String[] rowId) {
        SQLiteDatabase db = getWritableDatabase();
        String[] columns = new String[] {"rowid", key_beacon1, key_beacon2, key_beacon3, key_x_coord, key_y_coord, key_timestamp};
        String whereClause = "rowid=?";
        Cursor cursor = db.query(table_name, columns, whereClause, rowId, null, null, null);

        int rowidIndex = cursor.getColumnIndex("rowid");
        int rssi1Index = cursor.getColumnIndex(key_beacon1);
        int rssi2Index = cursor.getColumnIndex(key_beacon2);
        int rssi3Index = cursor.getColumnIndex(key_beacon3);
        int xcoorIndex = cursor.getColumnIndex(key_x_coord);
        int ycoorIndex = cursor.getColumnIndex(key_y_coord);
        int timestampIndex = cursor.getColumnIndex(key_timestamp);

        if(cursor.moveToFirst()) {
            int rowid = cursor.getInt(rowidIndex);
            int rssi1 = cursor.getInt(rssi1Index);
            int rssi2 = cursor.getInt(rssi2Index);
            int rssi3 = cursor.getInt(rssi3Index);
            float xcoor = cursor.getFloat(xcoorIndex);
            float ycoor = cursor.getFloat(ycoorIndex);
            String timestamp = cursor.getString(timestampIndex);

            String result = "Row ID: " + rowid +
                    "\nBeacon 1 RSSI: " + rssi1 +
                    "\nBeacon 2 RSSI: " + rssi2 +
                    "\nBeacon 3 RSSI: " + rssi3 +
                    "\n X: " + xcoor +
                    "\n Y: " + ycoor +
                    "\nTimestamp: " + timestamp;

            return result;
        }
        return null;
    }

    //deletes all records in database
    public void clearDatabase() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(table_name, null, null);
    }

    //deletes row with specific >=1 reference point ids
    public void deleteRows(String rowID) {
        String[] rowIDs = new String[] {rowID};
        String whereClause = "rowid=?";
        SQLiteDatabase db = getWritableDatabase();
        db.delete(table_name, whereClause, rowIDs);
    }

    //updates row with specific reference point ids
    public void updateRows(String[] rowIDs, int rssi_beacon1, int rssi_beacon2, int rssi_beacon3, String timestamp) {
        String whereClause = "rowid=?";
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(key_beacon1, rssi_beacon1);
        cv.put(key_beacon2, rssi_beacon2);
        cv.put(key_beacon3, rssi_beacon3);
        cv.put(key_timestamp, timestamp);

        db.update(table_name, cv, whereClause, rowIDs);
    }

    //adds new row to the database - each row is a reference point, with rssi values of 3 beacons and timestamp
    public void addRow(float xcoor, float ycoor, int rssi1, int rssi2, int rssi3) {

        SQLiteDatabase db = getWritableDatabase();
        Date date = new Date();
        String dateFormat = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        String dateStr = sdf.format(date);

        ContentValues cv = new ContentValues();
        cv.put(key_beacon1, rssi1);
        cv.put(key_beacon2, rssi2);
        cv.put(key_beacon3, rssi3);
        cv.put(key_x_coord, xcoor);
        cv.put(key_y_coord, ycoor);
        cv.put(key_timestamp, dateStr);

        db.insert(table_name, null, cv);
        Log.i("Adding new row for database: ", "row added");
    }

    //get number of rows
    public int getRowCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = (int) DatabaseUtils.queryNumEntries(db, table_name);
        return count;
    }

    //constructor
    public DatabaseHandler(Context context) {
        super(context, db_name, null, db_ver);
    }

    //creates database automatically if there is no existing database
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + table_name + " (rowid" + " INTEGER PRIMARY KEY NOT NULL, " +
                key_beacon1 + " INTEGER, " +
                key_beacon2 + " INTEGER, " +
                key_beacon3 + " INTEGER, " +
                key_x_coord + " FLOAT, " +
                key_y_coord + " FLOAT, " +
                key_timestamp +" TEXT);");

//        for (float x=0; x <= 210; x+=52.5) {
//            for (float y=0; y <= 210; y+=52.5) {
//                Date date = new Date();
//                String dateFormat = "yyyy-MM-dd HH:mm:ss";
//                SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
//                String dateStr = sdf.format(date);
//                addRow(x, y, db);
//                Log.i("Adding new row for database: ", "adding new row");
//            }
    }

    //called when upgrading the database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
        db.execSQL("DROP TABLE IF EXISTS " + table_name);
        onCreate(db);
    }
}
