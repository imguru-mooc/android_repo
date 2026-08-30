package com.example.hvacsimulator;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private int leftTemp = 63;
    private int rightTemp = 63;
    private boolean isCelsius = false;

    private final String[] fanModes = {"OFF", "LOW", "MEDIUM", "HIGH"};
    private int fanModeIndex = 0;

    private TextView leftTempDisplay;
    private TextView rightTempDisplay;
    private TextView fanDisplay;
    private Button fanButton;
    private LinearLayout rootLayout;
    private Spinner unitSelector;
    private ImageView fanIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI 요소 초기화
        rootLayout = findViewById(R.id.root_layout);
        leftTempDisplay = findViewById(R.id.temp_display_left);
        rightTempDisplay = findViewById(R.id.temp_display_right);
        fanDisplay = findViewById(R.id.fan_display);
        fanButton = findViewById(R.id.fan_button);
        unitSelector = findViewById(R.id.unit_selector);
        fanIcon = findViewById(R.id.fan_icon);

        // 단위 선택 Spinner 설정
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.temp_units,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSelector.setAdapter(adapter);
        unitSelector.setSelection(isCelsius ? 1 : 0);

        unitSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                isCelsius = (position == 1);
                updateDisplays();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 온도 조절 버튼 이벤트
        findViewById(R.id.temp_up_left).setOnClickListener(v -> {
            leftTemp++;
            updateDisplays();
        });

        findViewById(R.id.temp_down_left).setOnClickListener(v -> {
            leftTemp--;
            updateDisplays();
        });

        findViewById(R.id.temp_up_right).setOnClickListener(v -> {
            rightTemp++;
            updateDisplays();
        });

        findViewById(R.id.temp_down_right).setOnClickListener(v -> {
            rightTemp--;
            updateDisplays();
        });

        // FAN 버튼 이벤트
        fanButton.setOnClickListener(v -> {
            fanModeIndex = (fanModeIndex + 1) % fanModes.length;
            updateDisplays();
        });

        // 초기 표시
        updateDisplays();
    }

    private void updateDisplays() {
        // 온도 표시
        leftTempDisplay.setText(formatTemp(leftTemp));
        rightTempDisplay.setText(formatTemp(rightTemp));
        fanDisplay.setText("FAN: " + fanModes[fanModeIndex]);

        // 텍스트 색상
        leftTempDisplay.setTextColor(getColorByTemp(leftTemp));
        rightTempDisplay.setTextColor(getColorByTemp(rightTemp));

        // 배경색 변경
        if (leftTemp >= 75 || rightTemp >= 75) {
            rootLayout.setBackgroundColor(Color.parseColor("#FF5722")); // 따뜻함
        } else if (leftTemp <= 60 || rightTemp <= 60) {
            rootLayout.setBackgroundColor(Color.parseColor("#2196F3")); // 시원함
        } else {
            rootLayout.setBackgroundColor(Color.parseColor("#202124")); // 중간
        }

        // FAN 모드 아이콘 및 애니메이션 설정
        switch (fanModes[fanModeIndex]) {
            case "LOW":
                fanIcon.setImageResource(R.drawable.fan_low);
                startFanAnimation("LOW");
                break;
            case "MEDIUM":
                fanIcon.setImageResource(R.drawable.fan_medium);
                startFanAnimation("MEDIUM");
                break;
            case "HIGH":
                fanIcon.setImageResource(R.drawable.fan_high);
                startFanAnimation("HIGH");
                break;
            default:
                fanIcon.setImageResource(R.drawable.fan_off);
                fanIcon.clearAnimation();
                break;
        }
    }

    private void startFanAnimation(String mode) {
        int animRes;
        switch (mode) {
            case "LOW":
                animRes = R.anim.rotate_fan_low;
                break;
            case "MEDIUM":
                animRes = R.anim.rotate_fan_medium;
                break;
            case "HIGH":
                animRes = R.anim.rotate_fan_high;
                break;
            default:
                fanIcon.clearAnimation();
                return;
        }

        Animation rotate = AnimationUtils.loadAnimation(this, animRes);
        fanIcon.startAnimation(rotate);
    }

    private int getColorByTemp(int tempF) {
        if (tempF >= 75) return Color.RED;
        if (tempF <= 60) return Color.BLUE;
        return Color.WHITE;
    }

    private String formatTemp(int f) {
        if (isCelsius) {
            int c = (int) ((f - 32) * 5.0 / 9.0);
            return c + "°C";
        } else {
            return f + "°F";
        }
    }
}
