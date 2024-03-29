//
//  BLEDataConverter.swift
//  TS100BLEProject
//
//  Created by LST on 12/19/23.
//

import Foundation

class BLEDataConverter {
    // 온도 계산해주는 메서드
    static func temperatureCalculate(_ t1: UInt8, _ t2: UInt8, _ t3: UInt8, _ d: UInt8) -> Double {
        // Int로 변환, 비트 시프트 후 합산
        let intT1 = Int(t1)
        let intT2 = Int(t2) << 8
        let intT3 = Int(t3) << 16
        
        let signed = intT1 + intT2 + intT3
        
        // 자릿수 정하는 변수
        var digit = Int(d)
        if (digit > 127) {
            digit -= 256
        }
        
        // 온도
        var temperature = Double(signed) * pow(10, Double(digit))
        // 소수점 1자리만 남도록 수정
        temperature = round(temperature * 10) / 10
        
        return temperature
    }
    
    // 날짜 계산해주는 메서드
    static func dateCalculate(_ array: [Int]) -> Date {
        let year = (UInt16(array[1] & 0x0F) << 8) | UInt16(array[0] & 0x00FF)
        let month = array[2]
        let day = array[3]
        let hour = array[4]
        let minute = array[5]
        let second = array[6]
        
        let dateComponent = DateComponents(year: Int(year), month: month, day: day, hour: hour, minute: minute, second: second)
        guard let date = Calendar.current.date(from: dateComponent) else { return Date() }
        
        return date
    }
    
    // 현재 날짜를 리턴하는 메서드
    static func currentDateData() -> Data {
        // 현재 한국 시간(GMT + 9)
        let currentDate = Date()
        let calendar = Calendar(identifier: .gregorian)
        let components = calendar.dateComponents([.year, .month, .day, .hour, .minute, .second], from: currentDate)
        
        guard let year = components.year,
              let month = components.month,
              let day = components.day,
              let hour = components.hour,
              let minute = components.minute,
              let second = components.second else {
            fatalError("Failed to get date components")
        }
        
        let result: [UInt8] = [
            UInt8(year & 0xff),
            UInt8((year & 0xff00) >> 8),
            UInt8(month & 0xff),
            UInt8(day & 0xff),
            UInt8(hour & 0xff),
            UInt8(minute & 0xff),
            UInt8(second & 0xff)
        ]
        
        return Data(result)
    }
}
