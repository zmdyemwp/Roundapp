package com.evenwell.round;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

public class RoundApplication extends Application {
    // public static final String TAG = "RoundApplication";
    public static final String TAG = "MINROUNDAPPDEBUGTAG";

    public RoundApplication() {
    }

    @Override
    public void onCreate() {
        getApplicationContext().startService(new Intent(getApplicationContext(), RoundService.class));
    }
}
