package com.siui.round;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

public class RoundApplication extends Application {
     public static final String TAG = "RoundApplication";

     public RoundApplication() {
     }

     @Override
     public void onCreate() {
         getApplicationContext().startService(new Intent(getApplicationContext(), RoundService.class));
     }
}
