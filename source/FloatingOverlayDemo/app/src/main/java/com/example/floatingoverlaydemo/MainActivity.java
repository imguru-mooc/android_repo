package com.example.floatingoverlaydemo;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY_PERMISSION = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 오버레이 권한 확인
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        } else {
            showFloatingWindow();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                showFloatingWindow();
            } else {
                Toast.makeText(this, "오버레이 권한이 필요합니다.", Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showFloatingWindow() {
        Intent intent = new Intent(this, FloatingService.class);
        startService(intent);
    }
}

// 에뮬레이터에서 수동으로 shell 명령으로 오버레이 허용
// adb shell appops set com.example.floatingoverlaydemo SYSTEM_ALERT_WINDOW allow
