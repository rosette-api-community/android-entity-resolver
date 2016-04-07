package com.basistech.EntityExtractionApp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class EntryActivity extends AppCompatActivity {
    public static final String TEXT = "com.basistech.EntityExtractorApp.text";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_entry, menu);
        return true;
    }

    public void startExtraction(View view) {
        Intent intent = new Intent(this, ExtractionActivity.class);
        EditText textBox = (EditText) findViewById(R.id.textEntry);
        String input = textBox.getText().toString();
        intent.putExtra(TEXT, input);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivityForResult(i, 1);
                break;
        }
        return true;
    }
}
