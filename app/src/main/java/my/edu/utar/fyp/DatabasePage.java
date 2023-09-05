package my.edu.utar.fyp;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class DatabasePage extends AppCompatActivity {

    ListView lv;
    DeviceListAdapter adapter;
    //displays all records from the database
    //can clear database records from here
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_page);

        lv = findViewById(R.id.lvDatabaseResults);
        adapter = new DatabasePage.DeviceListAdapter(this);
        lv.setAdapter(adapter);

        Button btnClearDatabase = findViewById(R.id.btnClearDatabase);
        DatabaseHandler dbHandler = new DatabaseHandler(this);

        btnClearDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(DatabasePage.this);
                builder.setMessage("Do you want to clear the database?");
                builder.setTitle("ALERT");
                builder.setCancelable(false);

                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dbHandler.clearDatabase();
                        Toast toast = Toast.makeText(DatabasePage.this, "Database cleared", Toast.LENGTH_SHORT);
                        toast.show();
                        finish();
                    }
                });

                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });

        int dbSize = dbHandler.getRowCount();
        for(int i=1; i<=dbSize; i++) {
            String text = dbHandler.getRecordStr(new String[] {Integer.toString(i)});
            adapter.addRow(text);
            adapter.notifyDataSetChanged();
        }

    }

    public static class DeviceListAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<String> dbRecord;

        public DeviceListAdapter(Context context) {
            super();
            this.context = context;
            dbRecord = new ArrayList<>();
        }

        public void addRow(String record) {
            dbRecord.add(record);
        }

        public String getRow(int pos) {
            return dbRecord.get(pos);
        }

        public void clearList() {
            dbRecord.clear();
        }

        @Override
        public int getCount() {
            return dbRecord.size();
        }

        @Override
        public Object getItem(int pos) {
            return dbRecord.get(pos);
        }

        @Override
        public long getItemId(int pos) {
            return pos;
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup viewGroup) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.activity_list_database, viewGroup, false);

            TextView tvRow = view.findViewById(R.id.tvRow);

            tvRow.setText(dbRecord.get(pos).toString());

            return view;
        }

    }
}

