package com.example.ts100;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ts100.databinding.ActivityBluetoothScanBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressLint("MissingPermission")
public class BluetoothScanActivity extends AppCompatActivity {

    private ActivityBluetoothScanBinding binding;
    private BluetoothLeScanner scanner;
    private BLEScanCallback callback;
    private Handler bleScanHandler;

    // 검색된 디바이스를 담을 그릇
    private List<String> deviceList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        set();
    }

    @Override
    protected void onStart() {
        super.onStart();
        start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exit();
    }

    private void set() {
        setViewBinding();
        setBluetooth();
        setListViewAdapter();
    }

    private void start() {
        startBLEScan();
        setButton();
    }

    private void exit() {
        if (callback != null) {
            scanner.stopScan(callback);
            callback = null;
        }

        if (bleScanHandler != null) {
            bleScanHandler.removeCallbacksAndMessages(null);
            bleScanHandler = null;
        }
    }

    private void setViewBinding() {
        binding = ActivityBluetoothScanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    // Bluetooth 설정 함수
    private void setBluetooth() {
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();

        // Bluetooth Adapter 여부 확인 (Bluetooth 사용 가능 여부 확인)
        if (adapter == null) {
            Toast.makeText(getApplicationContext(), "해당 기기에서 블루투스 기능을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        scanner = adapter.getBluetoothLeScanner();

        // BLE Scanner 존재 여부 확인
        if (scanner == null) {
            Toast.makeText(getApplicationContext(), "해당 기기에서 BLE 검색 기능을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // ListView 설정 함수
    private void setListViewAdapter() {
        deviceList = new ArrayList<>();
        adapter = new ArrayAdapter<> (this, android.R.layout.simple_list_item_1, deviceList);
        binding.listviewDevice.setAdapter(adapter);
    }

    // BLE 스캔 시작 함수
    private void startBLEScan() {
        // BluetoothLeScanner 가 없는 경우 Toast 메시지 출력 및 함수 종료
        if (scanner == null) {
            Toast.makeText(getApplicationContext(), "해당 기기에서 BLE 검색 기능을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.buttonScan.setVisibility(View.INVISIBLE);       // Scan 버튼이 보이지 않도록 설정
        callback = new BLEScanCallback();       // BluetoothScanCallback 함수 선언
        scanner.startScan(callback);        // BLE Scan 시작

        // 30초 후 Scan을 중지하도록 설정
        bleScanHandler = new Handler(Looper.getMainLooper());
        bleScanHandler.postDelayed(this::stopBLEScan, 30 * 1000);

        // 30초 간 스캔을 시작한다는 Toast 메시지 출력
        Toast.makeText(getApplicationContext(), "30초간 주변기기를 검색합니다.", Toast.LENGTH_SHORT).show();
    }

    // 스캔 버튼 및 목록 버튼 설정 함수
    private void setButton() {
        binding.buttonScan.setOnClickListener(view -> startBLEScan());      // Scan 시작 버튼 클릭 시 이벤트
        binding.listviewDevice.setOnItemClickListener((adapterView, view, position, id) -> {    // TS100 리스트 클릭 시 이벤트

            // 30초 후 탐색을 멈추도록 하는 Handler 정지
            if (bleScanHandler != null) {
                bleScanHandler.removeCallbacksAndMessages(null);
                bleScanHandler = null;
            }

            // 기기 탐색 중지
            if(callback != null) {
                scanner.stopScan(callback);
            }

            // 블루투스 연결화면으로 이동 & 기기 정보 전송
            String deviceInfo = (String) binding.listviewDevice.getItemAtPosition(position);
            Intent intent = new Intent(this, BluetoothConnectActivity.class);
            intent.putExtra("TS100", deviceInfo);   // deviceInfo = 기기정보
            startActivity(intent);
        });
    }

    // 블루투스 스캔 중지하는 함수
    private void stopBLEScan() {
        if (callback == null) {
            return;
        }

        scanner.stopScan(callback);
        binding.buttonScan.setVisibility(View.VISIBLE);

        Toast.makeText(getApplicationContext(), "탐색 종료", Toast.LENGTH_SHORT).show();
    }

    // 검색된 기기를 추가하는 함수
    private void addDevice(BluetoothDevice device) {
        if (deviceList == null || adapter == null) {
            return;
        }

        // 기기의 이름이 유효할 경우
        if (device.getName() != null) {
            String deviceName = device.getName();

            // 기기의 이름에 "TS100"이 포함된 경우
            if (deviceName.contains("TS100")) {
                String deviceItem = String.format(Locale.getDefault(), "%s (%s)", deviceName, device.getAddress());

                // 같은 기기 추가 중복 방지
                if (!deviceList.contains(deviceItem)) {
                    deviceList.add(deviceItem);

                    // ★ ListView Update
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }

    // BLE 스캔 시 콜백 class 별도 선언
    private class BLEScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            if (result != null) {
                if (result.getDevice() != null) {
                    // 검색된 기기 추가
                    addDevice(result.getDevice());
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            for (ScanResult result : results) {
                if (result != null) {
                    if (result.getDevice() != null) {
                        // 검색된 기기 추가
                        addDevice(result.getDevice());
                    }
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(getApplicationContext(), "스캔에 실패하였습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}