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
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ts100.Data;
import com.example.ts100.databinding.ActivityBluetoothConnectBinding;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmConfiguration;

@SuppressLint("MissingPermission")
public class BluetoothConnectActivity extends AppCompatActivity {

    private ActivityBluetoothConnectBinding binding;

    private String deviceName;
    private String deviceAddress;

    Realm realm;

    // Service UUID
    private final UUID UUID_SERVICE_FEMON_CUSTOM_SERVICE = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");
    // 날짜 정보 관련 Characteristic UUID
    private final UUID UUID_CHARACTERISTIC_FEMON_SET_DATE = UUID.fromString("00002a08-0000-1000-8000-00805f9b34fb");
    // 온도 정보 관련 Characteristic UUID
    private final UUID UUID_CHARACTERISTIC_FEMON_TEMPERATURE_DATA = UUID.fromString("00002a1c-0000-1000-8000-00805f9b34fb");
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic setDateCharacteristic;
    private BluetoothGattCharacteristic getTempCharacteristic;

    // Chart List
    private ArrayList<String> dateList;
    private LineData lineData;
    private int tempCount;

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
    protected void onResume() {
        super.onResume();
        resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroy();
    }

    private void set() {
        setViewBinding();
        setValue();
    }

    private void start() {
        setRealm();
        setChart(binding.chartTempChart);
    }

    private void resume() {
        connectToTS100();
    }

    private void destroy() {
        exit();
    }

    private void setViewBinding() {
        binding = ActivityBluetoothConnectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    //  정보 및 변수 설정
    private void setValue() {
        // TS100 기기 정보 수신
        String deviceInfo = getIntent().getStringExtra("TS100");

        // TS100 기기가 null일 경우 이전화면으로 이동
        if (deviceInfo == null) {
            Toast.makeText(getApplicationContext(), "알 수 없는 기기", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // TS100 기기 정보 재조립
        binding.textDeviceInfo.setText(deviceInfo);
        String[] splitDeviceInfo = deviceInfo.split("\\(");
        deviceName = splitDeviceInfo[0].substring(0, splitDeviceInfo[0].length() - 1);
        deviceAddress = splitDeviceInfo[1].substring(0, splitDeviceInfo[1].length() - 1);
    }

    // 내부 DB Realm 설정
    private void setRealm() {
        // Realm 초기화
        Realm.init(this);

        RealmConfiguration config = new RealmConfiguration.Builder()
                .name("TS100")     // 저장할 파일 이름 설정
                .allowWritesOnUiThread(true)       // UI Thread 에서도 Realm 접근할 수 있도록 허용
                .build();

        Realm.setDefaultConfiguration(config);

        // Realm 생성
        realm = Realm.getDefaultInstance();
    }

    // Chart 설정
    private void setChart(LineChart chart) {
        // X축 설정
        XAxis xAxis = chart.getXAxis();
        xAxis.setEnabled(true);     // X축 사용 O
        xAxis.setTextSize(14f);     // X축 글자 사이즈 설정
        xAxis.setTextColor(Color.BLACK);    // X축 글자 컬러 설정
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);  // X축 위치 설정
        xAxis.setLabelCount(2);

        // Y축 (Left) 설정
        YAxis yAxisLeft = chart.getAxisLeft();
        yAxisLeft.setEnabled(true);     // Y축 사용 O
        yAxisLeft.setTextSize(14f);     // Y축 글자 사이즈 설정
        yAxisLeft.setTextColor(Color.BLACK);   // Y축 글자 컬러 설정

        // Y축 (Right) 설정
        YAxis yAxisRight = chart.getAxisRight();
        yAxisRight.setEnabled(false);   // Y축 사용 X

        // List 및 변수 초기화
        dateList = new ArrayList<>();       // 날짜 데이터를 저장할 리스트
        tempCount = 0;      // 온도 데이터 개수를 저장할 변수

        // 차트에 담길 Line Data Set 설정
        LineDataSet lineDataSet = new LineDataSet(null, "온도");
        lineDataSet.setLineWidth(3f);   // Chart Line 굵기 설정
        lineDataSet.setColor(Color.YELLOW);     // Chart Line 컬러 설정
        lineDataSet.setValueTextSize(14f);      // Chart Line 텍스트 사이즈 설정
        lineDataSet.setDrawCircles(true);       // Chart Line Circle 사용 O
        lineDataSet.setCircleRadius(6f);        // Chart Line Circle 반지름 설정
        lineDataSet.setCircleColor(Color.YELLOW);   // Chart Line Circle 컬러 설정

        // Line Data Set 을 담을 그릇 설정 (여러 Line Data Set 담을 수 있음)
        lineData = new LineData();
        lineData.addDataSet(lineDataSet);

        // Chart 설정
        chart.setTouchEnabled(true);        // 차트 클릭 가능하도록 설정
        chart.setScaleEnabled(true);        // 차트 Zoom 가능하도록 설정
        chart.setAutoScaleMinMaxEnabled(true);  // 차트 자동으로 Scale 조절되도록 설정
        chart.setData(lineData);
    }

    // TS100 기기 연결 함수
    @SuppressLint("MissingPermission")
    private void connectToTS100() {
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();

        // 받아온 기기 정보로 연결 시도
        BluetoothDevice device = adapter.getRemoteDevice(deviceAddress);
        gatt = device.connectGatt(getApplicationContext(), false, new BLEGattCallback());
    }

    // BLE Connect Callback 클래스 선언
    private class BLEGattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            switch (newState) {
                // 블루투스가 연결된 경우
                case BluetoothProfile.STATE_CONNECTED:
                    connectedToTS100();
                    gatt.discoverServices();
                    break;
                // 블루투스 연결중인 경우
                case BluetoothProfile.STATE_CONNECTING:
                    connectingToTS100();
                    break;
                // 블루트스 연결이 해제된 경우
                case BluetoothProfile.STATE_DISCONNECTED:
                    disconnectedToTS100();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Callback 함수가 여러번 호출되는 것을 방지하기 위한 if문
                if (getTempCharacteristic == null) {
                    discoveryCharacteristic(gatt);   // TS100 Gatt 내부의 Service 및 Characteristic 탐색
                }
            }
        }

        @SuppressLint("SimpleDateFormat")
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            // 온도 정보 수신
            double temp = getTemperatureData(characteristic);

            // 날짜 정보 수신
            String date = getDateData(characteristic.getValue());
            String dateNow = new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis()));       // 날짜 비교 변수 추가

            // 날짜 정보 비교
            if (date.contains(dateNow)) {
                addRealmDB(deviceName, date, temp);     // 날짜 정보가 오늘 일자와 일치할 경우
                drawChart(date, temp);                  // 차트로 출력
//                sendToAWSCloudServer(date, temp);
            } else {
                writeSetDateCharacteristic(gatt, setDateCharacteristic);    // 날짜 정보 일치하지 않을 경우 날짜 정보 재전송
            }
        }
    }

    // TS100 과 연결중인 경우
    private void connectingToTS100() {
        runOnUiThread(() -> binding.textDeviceConnectState.setText("Connecting ..."));
    }

    // TS100가 연결된 경우
    private void connectedToTS100() {
        runOnUiThread(() -> binding.textDeviceConnectState.setText("Connected (Discovery Characteristic ...)"));
    }

    // TS100 Gatt 내부의 Service 및 Characteristic 탐색
    private void discoveryCharacteristic(BluetoothGatt gatt) {

        // Service 조회
        for (BluetoothGattService service : gatt.getServices()) {
            // 필요한 Service 발견 시 Characteristic 조회
            if (service.getUuid().equals(UUID_SERVICE_FEMON_CUSTOM_SERVICE)) {
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {

                    // 날짜 정보와 관련된 Characteristic
                    if (characteristic.getUuid().equals(UUID_CHARACTERISTIC_FEMON_SET_DATE)) {
                        // 현재 시간 입력
                        new Handler(Looper.getMainLooper()).postDelayed(() -> writeSetDateCharacteristic(gatt, characteristic), 500);
                        setDateCharacteristic = characteristic;
                    }

                    // 온도 정보와 관련된 Characteristic
                    if (characteristic.getUuid().equals(UUID_CHARACTERISTIC_FEMON_TEMPERATURE_DATA)) {
                        getTempCharacteristic = characteristic;
                        gatt.setCharacteristicNotification(characteristic, true);

                        // 온도 정보를 수신할 수 있도록 INDICATION ENABLE
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                        gatt.writeDescriptor(descriptor);

                        discoveredCharacteristic();
                    }
                }
            }
        }
    }

    // TS100 Characteristic 에 데이터 전송
    private void writeSetDateCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        characteristic.setValue(getNowDate());
        gatt.writeCharacteristic(characteristic);
    }

    // TS100의 원하는 Characteristic을 찾은 경우
    private void discoveredCharacteristic() {
        // Text 변경
        runOnUiThread(() -> binding.textDeviceConnectState.setText("Connected"));
    }

    // 현재 시간을 byte 배열로 얻어오는 함수
    private byte[] getNowDate() {

        LocalDateTime now = LocalDateTime.now();

        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        int hour = now.getHour();
        int minute = now.getMinute();
        int second = now.getSecond();

        return new byte[]{
                (byte) (year & 0xff),
                (byte) ((year >> 8) & 0xff),
                (byte) (month & 0xff),
                (byte) (day & 0xff),
                (byte) (hour & 0xff),
                (byte) (minute & 0xff),
                (byte) (second & 0xff)
        };
    }

    // TS100으로부터 수신한 날짜 정보를 String 형태로 변환
    private String getDateData(byte[] array) {

        int year = ((array[6] & 0x0F) << 8) | (array[5] & 0x00FF);
        int month = array[7];
        int day = array[8];
        int hour = array[9];
        int minute = array[10];
        int second = array[11];

        LocalDateTime localDateTime = LocalDateTime.of(year, Month.of(month), day, hour, minute, second);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return localDateTime.format(formatter);
    }

    // TS100으로부터 수신한 온도 정보를 Double 형태로 변환
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

    // TS100과 연결이 끊어진 경우
    private void disconnectedToTS100() {
        runOnUiThread(() -> binding.textDeviceConnectState.setText("Disconnected"));
    }

    // 내부 DB인 Realm에 데이터 저장
    private void addRealmDB(String name, String date, double temp) {

        if (realm == null) {
            return;
        }

        // 저장할 데이터 설정
        Data ts100 = new Data();
        ts100.setName(name);
        ts100.setDate(date);
        ts100.setTemp(temp);

        // Realm 데이터 저장 - UI Thread 에서 저장
        new Handler(Looper.getMainLooper()).post(() -> {
            realm.executeTransaction(realm1 -> {
                realm1.copyToRealm(ts100);
            });
        });
    }

    // Chart 그리는 함수
    private void drawChart(String date, double temp) {
        tempCount++;            // 온도 데이터 갯수 추가
        dateList.add(date);     // 날짜 정보 추가
        lineData.addEntry(new Entry((float) tempCount, (float) temp), 0);   // 온도 정보 추가

        binding.chartTempChart.notifyDataSetChanged();      // Chart Data 갱신

        // X축 10개만 출력되고 10개 이상일 경우 우측으로 이동되도록 설정
        if (tempCount > 10) {
            binding.chartTempChart.getXAxis().setAxisMinimum(tempCount - 10);
            binding.chartTempChart.getXAxis().setAxisMaximum(tempCount);
        }

        binding.chartTempChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(dateList));     // X축을 TS100 측정 시간으로 설정되도록 설정
        binding.chartTempChart.invalidate();    // Chart View 갱신
    }

    // AWS Cloud Server로 데이터 전송하는 함수
    private void sendToAWSCloudServer(String date, double temp) {
        Thread thread = new Thread(() -> {

            // 온도 정보 문자열로 변경
            String convertTemperature = String.valueOf(temp);

            try {
                // URL 생성
                String baseUrl = "https://xyyyaewub0.execute-api.ap-northeast-2.amazonaws.com/TS100Stage/ts100_resource";
                String urlStr = baseUrl + "?date='" + URLEncoder.encode(date, "UTF-8") + "'&temperature=" + URLEncoder.encode(convertTemperature, "UTF-8");

                // HTTP 연결 설정
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                Log.d("TempInfoActivity", "Response code: " + responseCode);

                connection.disconnect();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // 스레드 시작
        thread.start();
    }

    private void exit() {
        if (gatt != null) {
            gatt.disconnect();
            gatt = null;
        }

        if (realm != null) {
            realm.close();
            realm = null;
        }
    }
}