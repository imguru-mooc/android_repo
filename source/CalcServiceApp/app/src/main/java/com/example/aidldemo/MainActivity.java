package com.example.aidldemo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class MainActivity extends Activity {
    ICalcService calcService;

    private ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            calcService = ICalcService.Stub.asInterface(service);
            try {
                int result = calcService.subtract(10, 5);
                Log.d("CalcClient", "Result from service: " + result);
            } catch (Exception e) {
                Log.e("CalcClient", "Error: ", e);
            }
        }
        public void onServiceDisconnected(ComponentName name) {
            calcService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent("com.example.aidldemo.CalcService");
        intent.setPackage("com.example.aidldemo");
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }
}
