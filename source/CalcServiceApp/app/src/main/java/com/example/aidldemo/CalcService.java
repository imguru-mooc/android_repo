package com.example.aidldemo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class CalcService extends Service {

    private static final String TAG = "CalcService";

    // AIDL 인터페이스 Stub 구현
    private final ICalcService.Stub mBinder = new ICalcService.Stub() {
        @Override
        public int add(int a, int b) throws RemoteException {
            Log.d(TAG, "add(" + a + ", " + b + ")");
            return a + b;
        }

        @Override
        public int subtract(int a, int b) throws RemoteException {
            Log.d(TAG, "subtract(" + a + ", " + b + ")");
            return a - b;
        }

        @Override
        public int multiply(int a, int b) throws RemoteException {
            Log.d(TAG, "multiply(" + a + ", " + b + ")");
            return a * b;
        }

        @Override
        public int divide(int a, int b) throws RemoteException {
            Log.d(TAG, "divide(" + a + ", " + b + ")");
            if (b == 0) {
                throw new ArithmeticException("Divide by zero");
            }
            return a / b;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service bound");
        return mBinder;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        super.onDestroy();
    }
}
