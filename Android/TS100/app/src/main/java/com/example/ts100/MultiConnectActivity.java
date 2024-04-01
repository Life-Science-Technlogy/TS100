package com.example.ts100;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.ts100.databinding.ActivityMultiConnectBinding;

import java.util.ArrayList;
import java.util.Locale;

public class MultiConnectActivity extends AppCompatActivity implements DeviceScanDialog.DeviceSelectListener, DeviceConnect.DeviceGetDataListener {

    private ActivityMultiConnectBinding binding;
    private ArrayList<String> deviceList;       // List View 에서 보여줄 List 선언
    private ArrayList<String> deviceNameList;       // device 이름 List 선언
    private ArrayList<DeviceConnect> multiConnectList;      // 연결한 블루투스 정보를 보관할 List 선언
    private ArrayAdapter<String> adapter;       // List View 와 연결할 ArrayAdapter 설정

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exit();
    }

    // 화면을 위한 기초 셋팅을 진행할 함수
    private void setUp() {
        setViewBinding();
        setListViewAdapter();
        setValue();
    }

    // View Binding 진행
    private void setViewBinding() {
        binding = ActivityMultiConnectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    // ListView 설정
    private void setListViewAdapter() {
        deviceList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        binding.listviewDeviceList.setAdapter(adapter);
    }

    // 변수 선언
    private void setValue() {
        deviceNameList = new ArrayList<>();
        multiConnectList = new ArrayList<>();
    }

    private void start() {
        setButton();
    }

    // 버튼 설정 (기기 추가 버튼)
    private void setButton() {
        // 기기 추가 버튼 클릭 시 기기를 검색할 수 있는 다이얼로그 이동
        binding.buttonDeviceScan.setOnClickListener(view -> {
            DeviceScanDialog dialog = new DeviceScanDialog(this, this, deviceList);
            dialog.show();
        });
    }

    // 연결하고자 하는 기기 선택 시 (다이얼로그에서 기기 선택 시 기기의 정보 수신)
    @Override
    public void onDeviceSelected(String deviceInfo, String deviceName, String deviceAddress) {
        Log.d("jh", "디바이스 이름: " + deviceName + ", 디바이스 주소: " + deviceAddress);
        // 기기 정보 수신 및 List View Update
        deviceList.add(deviceName);
        deviceNameList.add(deviceName);
        adapter.notifyDataSetChanged();

        // 연결 시도
        tryConnectToDevice(deviceName, deviceAddress, adapter.getCount());
    }

    // 기기와의 연결을 시도하는 함수
    private void tryConnectToDevice(String name, String address, int position) {
        DeviceConnect deviceConnect = new DeviceConnect(this, this, position);
        deviceConnect.connect(name, address);

        // 각 기기 별 연결 상태를 저장할 수 있도록 배열에 추가
        multiConnectList.add(deviceConnect);
    }

    // 연결된 기기로부터 온도 데이터 수신 시 List View 수정
    @Override
    public void onDeviceGetDataListener(double temp, int position) {
        Log.d("jh", String.format("[MultiConnectActivity] %d 기기의 온도: %.2f℃", position, temp));

        // UI 변경이므로, UI Thread에서 수행되도록 설정
        runOnUiThread(() -> {
            // List View에 온도 데이터 추가되도록 설정
            deviceList.set(position - 1, String.format(Locale.getDefault(), "%s, %.1f℃", deviceNameList.get(position - 1), temp));
            adapter.notifyDataSetChanged();
        });
    }

    // 화면 종료 시 호출되는 함수
    private void exit() {
        // 연결된 기기의 상태를 저장한 모든 변수에 대해 블루투스 연결 해제
        for (DeviceConnect connect : multiConnectList) {
            connect.disconnect();
        }
    }
}