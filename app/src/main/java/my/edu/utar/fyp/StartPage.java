package my.edu.utar.fyp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class StartPage extends AppCompatActivity {

    //main page that allows user to choose different pages -- start scan, display database records, show map...
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_page);

        Button btnStartScanActivity = findViewById(R.id.btnStartScanActivity);
        Button btnViewDatabase = findViewById(R.id.btnViewDatabase);

        btnStartScanActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StartPage.this, MainActivity.class);
                StartPage.this.startActivity(intent);
            }
        });

        btnViewDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StartPage.this, DatabasePage.class);
                StartPage.this.startActivity(intent);
            }
        });
    }
}