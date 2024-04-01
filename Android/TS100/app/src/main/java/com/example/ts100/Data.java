package com.example.ts100;

import io.realm.RealmObject;

// Realm 데이터 모델
public class Data extends RealmObject {
    private String name;
    private String date;
    private double temp;

    // 데이터 초기화
    public Data() {
        name = null;
        date = null;
        temp = 0.0f;
    }

    public String getName() {
        return name;
    }

    // 저정할 기기 이름 정보 설정
    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    // 저장할 날짜 정보 설정
    public void setDate(String date) {
        this.date = date;
    }

    public double getTemp() {
        return temp;
    }

    // 저장할 온도 정보 설정
    public void setTemp(double temp) {
        this.temp = temp;
    }
}