package com.example.ts100;

import static android.content.Context.BLUETOOTH_SERVICE;

import androidx.annotation.NonNull;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.ts100.databinding.DialogDeviceScanBinding;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@SuppressLint("MissingPermission")
public class DeviceScanDialog extends Dialog {

    private DialogDeviceScanBinding binding;
    private DeviceSelectListener listener;
    private Context context;

    private ArrayList<String> deviceList;
    private ArrayList<String> deviceCheckList;
    private ArrayAdapter<String> adapter;

    private BluetoothLeScanner scanner;
    private BLEScanCallback scanCallback;
    private Handler scanHandler;

    public DeviceScanDialog(@NonNull Context context, DeviceSelectListener listener, ArrayList<String> list) {
        super(context);
        this.context = context;
        this.listener = listener;
        this.deviceCheckList = list;
    }

    // interface 생성
    public interface DeviceSelectListener {
        void onDeviceSelected(String deviceInfo, String deviceName, String deviceAddress);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
        start();
        setButton();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        exit();
    }

    private void setUp() {
        setViewBinding();
        setListViewAdapter();
        setDialogSize();
        setBluetooth();
    }

    private void setViewBinding() {
        binding = DialogDeviceScanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    // ListView 설정 함수
    private void setListViewAdapter() {
        deviceList = new ArrayList<>();
        adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, deviceList);
        binding.listviewDeviceDialog.setAdapter(adapter);
    }

    // 다이얼로그 사이즈 설정
    private void setDialogSize() {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();

        // 다이얼로그의 크기를 가로 세로 80% 크기로 조절
        int width = (int) (dm.widthPixels * 0.8);
        int height = (int) (dm.heightPixels * 0.8);

        Objects.requireNonNull(getWindow()).getAttributes().width = width;
        getWindow().getAttributes().height = height;
    }

    // 블루투스 설정
    private void setBluetooth() {
        BluetoothManager manager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();

        // Bluetooth Adapter 여부 확인 (Bluetooth 사용 가능 여부 확인)
        if (adapter == null) {
            Toast.makeText(context, "해당 기기에서 블루투스 기능을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        scanner = adapter.getBluetoothLeScanner();

        // BLE Scanner 존재 여부 확인
        if (scanner == null) {
            Toast.makeText(context, "해당 기기에서 BLE 검색 기능을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }

    private void start() {
        startScan();
        setButton();
    }

    // 블루투스 스캔 시작 함수
    @SuppressLint("MissingPermission")
    private void startScan() {
        Toast.makeText(context, "30초 간 기기를 탐색합니다.", Toast.LENGTH_SHORT).show();
        binding.buttonDeviceScanDialog.setVisibility(View.INVISIBLE);

        // BLE 콜백 함수 등록 및 스캔 시작
        scanCallback = new BLEScanCallback();
        scanner.startScan(scanCallback);

        // 30초 후 스캔이 중지되도록 Handler 설정
        scanHandler = new Handler();
        scanHandler.postDelayed(this::stopScan, 30 * 1000);
    }

    private void stopScan() {
        if (scanCallback == null) {
            return;
        }

        Toast.makeText(context, "탐색 종료", Toast.LENGTH_SHORT).show();

        scanner.stopScan(scanCallback);
        binding.buttonDeviceScanDialog.setVisibility(View.VISIBLE);

        scanCallback = null;
        scanHandler = null;
    }

    // BLE Callback 클래스 등록
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
            Toast.makeText(context, "스캔에 실패하였습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // 스캔된 장치 추가 함수
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
                if (!deviceList.contains(deviceItem) && !deviceCheckList.contains(deviceItem)) {
                    deviceList.add(deviceItem);

                    // ★ ListView Update
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }

    // 버튼 설정 함수
    private void setButton() {
        // 블루투스 검색 버튼 클릭 시 스캔이 시작되도록 설정
        binding.buttonDeviceScanDialog.setOnClickListener(view -> startScan());

        // 스캔된 기기 클릭 시 리스트에 등록되도록 설정
        binding.listviewDeviceDialog.setOnItemClickListener((adapterView, view, position, id) -> {
            String deviceItem = deviceList.get(position);
            String deviceName = deviceItem.substring(0, deviceItem.indexOf("(")).trim();
            String deviceAddress = deviceItem.substring(deviceItem.indexOf("(") + 1, deviceItem.indexOf(")")).trim();

            // 연결할 장치 선택 시 이전 화면으로 선택한 기기의 정보를 전송
            if (listener != null) {
                listener.onDeviceSelected(deviceItem, deviceName, deviceAddress);
            }

            // 다이얼로그 종료
            dismiss();
        });
    }

    private void exit() {
        if (scanCallback != null) {
            stopScan();
        }

        if (scanHandler != null) {
            scanHandler.removeCallbacksAndMessages(null);
            scanHandler = null;
        }
    }
}