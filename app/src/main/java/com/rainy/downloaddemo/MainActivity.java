package com.rainy.downloaddemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.rainy.yupdate.YUpdate;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new YUpdate.Builder(this).url("https://erpcapp.91jikang.com/app/update.json").create().checkNewApp();
    }
}
