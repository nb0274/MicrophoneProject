package com.example.microphoneproject;

import android.os.Bundle;
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

}