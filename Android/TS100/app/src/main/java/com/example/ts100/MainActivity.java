package com.example.ts100;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.ts100.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    // 권한의 허용 상태를 판단하기 위한 변수
    private boolean checkPermission;
    // 권한 요청 시 필요한 parameter
    private final int PERMISSION_REQUEST_CODE = 1203;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
        start();
    }

    private void setUp() {
        setViewBinding();
        setPermission();
    }

    private void start() {
        clickButton();
    }

    private void setViewBinding() {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    // 권한 설정 함수
    private void setPermission() {
        // 블루투스 권한 종류
        String[] Permissions;
        // 안드로이드 바전 별로 권한 분류
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            Permissions = new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        } else {
            Permissions = new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }

        // 거절된 권한을 담을 그릇 선언
        ArrayList<String> denyPermissions = new ArrayList<>();
        for (String permission : Permissions) {
            // 권한이 거절된 경우 리스트에 추가
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), permission) == PackageManager.PERMISSION_DENIED) {
                denyPermissions.add(permission);
            }
        }

        // 거절된 권한이 있는 경우 권한 요청
        if (denyPermissions.size() > 0) {
            ActivityCompat.requestPermissions(this, denyPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            // 권한이 허용되어 있는 경우 권한이 허용되었다고 판단
            checkPermission = true;
        }
    }

    // setPermission() 함수에서 Activity.requestPermissions 함수 사용 시 호출되는 함수
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // 권한 거절 유무를 확인할 변수 선언
        checkPermission = true;

        // request code 함수가 맞는지 확인
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int granResult : grantResults) {
                // 권한을 거절한 경우 거절했다고 판별 후 함수 종료
                if (granResult == PackageManager.PERMISSION_DENIED) {
                    checkPermission = false;
                    break;
                }
            }
        }
    }

    // 버튼 설정 함수
    private void clickButton() {
        // 단일 연결 버튼을 누른 경우 TS100 1대와 연결할 수 있는 화면으로 이동하는 버튼
        binding.buttonSingle.setOnClickListener(view -> {

            // 권한이 허용 여부 판단
            if (checkPermission) {
                // 허용된 경우 화면 이동
                Intent intent = new Intent(this, BluetoothScanActivity.class);
                startActivity(intent);
            } else {
                // 허용되지 않은 경우 메시지 출력 후 재요청
                Toast.makeText(this, "권한을 허용해주세요.", Toast.LENGTH_SHORT).show();
                setPermission();
            }
        });

        // 여러 대의 TS100을 연결할 수 있는 화면으로 이동하는 버튼
        binding.buttonMulti.setOnClickListener(view -> {

            // 권한이 허용 여부 판단
            if (checkPermission) {
                // 허용된 경우 화면 이동
                startActivity(new Intent(this, MultiConnectActivity.class));
            } else {
                // 허용되지 않은 경우 메시지 출력 후 재요청
                Toast.makeText(this, "권한을 허용해주세요.", Toast.LENGTH_SHORT).show();
                setPermission();
            }
        });
    }
}