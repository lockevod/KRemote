package com.enderthor.kremote;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class KRemoteActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_comp);
        String text = "V. ";
        if (BuildConfig.DEBUG) {
            text += BuildConfig.BUILD_TYPE + " " + BuildConfig.VERSION_NAME;
        } else {
            text += BuildConfig.VERSION_NAME;
        }
        TextView textViewVersion = findViewById(R.id.bar);
        textViewVersion.setText(text);
    }


}
