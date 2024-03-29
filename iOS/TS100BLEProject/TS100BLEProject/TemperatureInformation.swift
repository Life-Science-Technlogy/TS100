//
//  TemperatureInformation.swift
//  TS100BLEProject
//
//  Created by LST on 2023/08/19.
//

import Foundation
import RealmSwift  // 패키지 Import

class TemperatureInformation: Object {
    // 기본키
    @Persisted(primaryKey: true) var _id: ObjectId
    
    // 이름
    @Persisted var name: String
    // 날짜
    @Persisted var date: Date
    // 온도
    @Persisted var temperature: Double
    
    convenience init(name: String, temperature: Double, date: Date) {
        self.init()
        self.name = name
        self.temperature = temperature
        self.date = date
    }
}
