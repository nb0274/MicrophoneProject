package com.example.microphoneproject;
//l
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RecordsList extends AppCompatActivity implements AdapterView.OnItemClickListener {

    ListView recordList;
    TextView tV;
    String [] town={"record 1","record 2","record 3","record 4"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records_list);
/**
 * Init parameters
 */
        recordList = (ListView) findViewById(R.id.recordList);
        tV = (TextView) findViewById(R.id.tV);

        recordList.setOnItemClickListener(this);
        recordList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        ArrayAdapter<String> adp = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,town);

        recordList.setAdapter(adp);
    }

    /**
     * onItemSelected method: Display on the TextView the position in the adapter,
     * the row id of the data that was pressed & the data.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        tV.setText(""+position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu); // Replace with your menu file name if different
        return true;
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//
//        if (id == R.id.menuLogIn) {
//            startActivity(new Intent(RecordsList.this, MainActivity.class));
//            return true;
//        } else if (id == R.id.menuSignUp) {
//            startActivity(new Intent(RecordsList.this, SignupPage.class));
//            return true;
//        } else if (id == R.id.menuRecordPage) {
//            startActivity(new Intent(RecordsList.this, RecordPage.class));
//            return true;
//        } else if (id == R.id.menuRecordList) {
//            // Already in Records List activity; no need for action here
//            return true;
//        } else if (id == R.id.menuAlphaBtnRecord) {
//            startActivity(new Intent(RecordsList.this, Alpha_BtnRecord.class));
//            return true;
//        } else if (id == R.id.menuAlphaChooseFile) {
//            startActivity(new Intent(RecordsList.this, Alpha_ChooseFile.class));
//            return true;
//        } else if (id == R.id.menuStorageImport) {
//            startActivity(new Intent(RecordsList.this, Alpha_StorageImport.class));
//            return true;
//        } else {
//            return super.onOptionsItemSelected(item);
//        }
//    }

}