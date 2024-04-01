package com.example.ts100;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.UUID;

@SuppressLint("MissingPermission")
public class DeviceConnect {

    // 온도 데이터 수신 인터페이스 변수
    private final DeviceGetDataListener listener;
    // 몇 번째 기기 인지 알기 위해 선언한 변수
    private final int position;
    private final Context context;
    private String deviceName;

    // Service UUID
    private final UUID UUID_SERVICE_FEMON_CUSTOM_SERVICE = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");
    // 온도 정보 관련 Characteristic UUID
    private final UUID UUID_CHARACTERISTIC_FEMON_TEMPERATURE_DATA = UUID.fromString("00002a1c-0000-1000-8000-00805f9b34fb");

    private BluetoothGatt gatt;

    private Handler pairingCheckHandler;

    public DeviceConnect(Context context, DeviceGetDataListener listener, int position) {
        this.context = context;
        this.listener = listener;
        this.position = position;
    }

    // interface 등록
    public interface DeviceGetDataListener {
        void onDeviceGetDataListener(double temp, int position);
    }

    // 디바이스와 블루투스 연결 함수
    public void connect(String name, String address) {
        // 블루투스 설정
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();

        // 블루투스 기기 선언
        deviceName = name;
        BluetoothDevice device = adapter.getRemoteDevice(address);

        // 블루투스 페어링 상태 확인 후 연결 시도
        boolean isPaired = device.getBondState() == BluetoothDevice.BOND_BONDED;
        gatt = device.connectGatt(context, false, new BLEGattCallback());

        // 만약 페어링 되지 않은 경우
        if (!isPaired) {
            // 1초마다 페어링 상태를 확인 후, 페어링 완료된 경우 연결 시도
            pairingCheckHandler = new Handler();
            pairingCheckHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        Log.d("jh", "페어링 완료 Service & Characteristic 체크");
                        discoveryServiceAndCharacteristic(gatt);
                    } else {
                        Log.d("jh", "페어링 미완료 (" + device.getBondState() + ")");
                        pairingCheckHandler.postDelayed(this, 1 * 1000);
                    }
                }
            }, 1 * 1000);
        }
    }

    public void disconnect() {
        if (gatt != null) {
            gatt.disconnect();
        }
    }

    private class BLEGattCallback extends BluetoothGattCallback {
        // 블루투스 연결 상태에 따라 호출되는 함수
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            switch (newState) {
                // 블루투스가 연결된 경우
                case BluetoothProfile.STATE_CONNECTED:
                    connectedToDevice();
                    gatt.discoverServices();
                    break;
                // 블루투스 연결이 해제된 경우
                case BluetoothProfile.STATE_DISCONNECTED:
                    disconnectedToDevice();
            }
        }

        // 블루투스 Service 및 Characteristic 조회 시 호출되는 함수
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                discoveryServiceAndCharacteristic(gatt);
            }
        }

        // 원하는 Characteristic 을 Notify 되도록 등록한 경우 Notify 될 때마다 호출되는 함수
        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);

            // 온도 데이터
            double temp = getTemperatureData(characteristic);

            // MultiConnectActivity 으로 온도 및 몇 번째 기기의 온도 데이터인지 전송
            listener.onDeviceGetDataListener(temp, position);
        }
    }

    // 기기와 연결이 끊어진 경우
    private void connectedToDevice() {
        Log.d("jh", String.format("%s 기기 연결 성공", deviceName));
    }

    // Service 및 Characteristic 요청 함수
    public void discoveryServiceAndCharacteristic(BluetoothGatt gatt) {
        for (BluetoothGattService service : gatt.getServices()) {
            // 필요한 Service 발견 시 Characteristic 조회
            if (service.getUuid().equals(UUID_SERVICE_FEMON_CUSTOM_SERVICE)) {
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {

                    // 온도 정보와 관련된 Characteristic
                    if (characteristic.getUuid().equals(UUID_CHARACTERISTIC_FEMON_TEMPERATURE_DATA)) {
                        gatt.setCharacteristicNotification(characteristic, false);

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            gatt.setCharacteristicNotification(characteristic, true);

                            // 온도 정보를 수신할 수 있도록 INDICATION ENABLE
                            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                            gatt.writeDescriptor(descriptor);

                            Log.d("jh", String.format("%s 기기 특성 찾기 성공", deviceName));
                        }, 300);
                    }
                }
            }
        }
    }

    // 블루투스 연결이 해제된 경우의 함수
    private void disconnectedToDevice() {
        // Pairing 상태를 확인하지 않도록 Handler 종료
        if (pairingCheckHandler != null) {
            pairingCheckHandler.removeCallbacksAndMessages(null);
        }

        Log.d("jh", String.format("%s 기기 연결 해제", deviceName));
    }

    private double getTemperatureData(BluetoothGattCharacteristic characteristic) {
        // Int로 변환, 비트 시프트 후 합산
        int intT1 = characteristic.getValue()[1] & 0xFF;
        int intT2 = (characteristic.getValue()[2] & 0xFF) << 8;
        int intT3 = (characteristic.getValue()[3] & 0xFF) << 16;

        int signed = intT1 + intT2 + intT3;

        // 자릿수 정하는 변수
        int digit = characteristic.getValue()[4];
        if (digit > 127) {
            digit -= 256;
        }

        // 온도
        double temperature = signed * Math.pow(10, digit);
        // 소수점 1자리만 남도록 수정
        temperature = Math.round(temperature * 10) / 10.0;

        return temperature;
    }
}
