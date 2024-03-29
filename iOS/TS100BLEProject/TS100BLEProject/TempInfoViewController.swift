//
//  TempInfoViewController.swift
//  TS100BLEProject
//
//  Created by LST on 2023/08/22.
//

import UIKit
import CoreBluetooth
import RealmSwift
import DGCharts

class TempInfoViewController: UIViewController {
    
    // realm 열기
    let realm = try! Realm()
    // 온도정보 데이터베이스
    var temperatureInformationList: Results<TemperatureInformation>!
    
    var peripheral: CBPeripheral!

    let bluetoothManager = BluetoothManager.shared
    
    @IBOutlet weak var lineChartView: LineChartView!
    
    // 알림기능
    var token: NotificationToken?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.title = "Chart"
        
        setLineChart()
        
        // 연결된 peripheral 변수에 할당
        peripheral = connectedPeripheral
        
        // 델리게이트 설정
        peripheral.delegate = self
        // UUID
        peripheral.discoverServices([CBUUID(string: "1809")])
        
        // Realm Objects
        temperatureInformationList = realm.objects(TemperatureInformation.self)
        
        // 데이터베이스 정보가 변경되었을 때 호출되는 메서드
        token = temperatureInformationList.observe { change in
            switch change {
            case .update:
                // 차트 업데이트
                self.setLineChart()
                break
            default:
                break
            }
        }
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        
        bluetoothManager.disconnect(connectedPeripheral)
    }
    
    // 차트 그리기
    func setLineChart() {
        // 그래프에 보여줄 배열
        var lineChartEntry = [ChartDataEntry]()
        
        // DB내 온도 변수
        guard let tempList = temperatureInformationList else { return }
        
        // 최근 10개의 데이터 추출
        let tenBeforeList = Array(tempList).suffix(10)
        var newSuffixArray: [TemperatureInformation] = []
        for i in tenBeforeList {
            newSuffixArray.append(i)
        }
        
        var continuousValue = [TemperatureInformation]()
        // 연속적인 데이터만 배열에 담음
        if let lastValue = newSuffixArray.last {
            continuousValue.append(lastValue)
        }
        
        for i in stride(from: newSuffixArray.count - 2, through: 0, by: -1) {
            let now = newSuffixArray[i].date
            let next = newSuffixArray[i + 1].date
            
            // 만약 이전 값과 시간 차이가 1초보다 많이 난다면
            if Int(next.timeIntervalSince(now)) > 5 {
                break
            }
            
            // 배열에 추가
            continuousValue.insert(newSuffixArray[i], at: 0)
        }
        
        
        // 차트 배열에 데이터 추가
        for i in 0..<continuousValue.count {
            let temperature = round(Double(continuousValue[i].temperature) * 10) / 10
            let value = ChartDataEntry(x: Double(i), y: temperature)
            
            lineChartEntry.append(value)
        }
        
        // 라인 추가
        let line1 = LineChartDataSet(entries: lineChartEntry, label: "온도")
        line1.colors = [NSUIColor.systemYellow]
        line1.circleColors = [NSUIColor.systemYellow]
        
        let data = LineChartData(dataSet: line1)
        
        // 차트에 데이터 추가
        lineChartView.data = data
        
        // 기본 출력 텍스트
        lineChartView.noDataText = "출력 데이터가 없습니다."
        // 기본 출력 텍스트 폰트
        lineChartView.noDataFont = .systemFont(ofSize: 20)
        // 기본 출력 텍스트 색상
        lineChartView.noDataTextColor = .lightGray
        // Chart 뒷 배경 색상
        lineChartView.backgroundColor = .white
    }
    
    // 클라우드 온도정보 저장(현재는 API 호출 불가능)
    func saveTemperatureInformationToCloud(date: Date, temperature: Double) {
        // dateFormatter 사용해 날짜포맷 변경
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        let convertDate = dateFormatter.string(from: date)
        
        // 온도정보 String으로 변경
        let convertTemperature = String(temperature)
        
        // peripheral
        guard let peripheralName = peripheral.name else { return }
        
        // URL 생성
        let urlStr = "https://xyyyaewub0.execute-api.ap-northeast-2.amazonaws.com/TS100Stage/ts100_resource?name=\(peripheralName)&temperature=\(convertTemperature)&date=\(convertDate)"
        guard let encoded = urlStr.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) else { return }
        guard let myURL = URL(string: encoded) else { return }
        
        //Request 객체
        var request = URLRequest(url: myURL)
        request.httpMethod = "GET"
        
        let dataTask = URLSession.shared.dataTask(with: request, completionHandler: {data, response, error in
            guard let response = response as? HTTPURLResponse else { return }
            
            //상태 확인
            switch response.statusCode {
            case (200...299): //성공
                print("Success")
            case (400...499): //에러
                print("Error")
            default:
                print("Default Error")
            }
        })
        
        // resume() 실행하지 않으면 API호출 안됨
        dataTask.resume()
    }
}

extension TempInfoViewController: CBPeripheralDelegate {
    // 1. Service를 수신했을 때 호출되는 메서드
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        if let services = peripheral.services {
            
            for service in services {
                // Service의 characteristic 검색(nil로 설정하면 전체 characteristic 검색)
                peripheral.discoverCharacteristics([CBUUID(string: "2A1C"), CBUUID(string: "2A08")], for: service)
                
            }
        }
    }
    
    // 2. Charactistic을 수신했을 때 호출되는 메서드
    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        guard let characteristics = service.characteristics else {
            return
        }
        
        for characteristic in characteristics {
            // 온도정보일 때
            if characteristic.uuid == CBUUID(string: "2A1C") {
                // notify 설정
                peripheral.setNotifyValue(true, for: characteristic)
            }
            
            // Date 값일 때
            if characteristic.uuid == CBUUID(string: "2A08") {
                // 현재시간으로 설정(Response 수신하지 않음)
                peripheral.writeValue(BLEDataConverter.currentDateData(), for: characteristic, type: .withResponse)
            }
        }
    }
    
    // 3. characteristic에서 read, notify 설정 후 데이터가 수신되었을 때 호출되는 메서드
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        
        if characteristic.uuid == CBUUID(string: "2A1C") {
            if let value = characteristic.value {
                
                let array = [UInt8](value)
                
                // 온도정보 받아옴
                let temperature = BLEDataConverter.temperatureCalculate(array[1], array[2], array[3], array[4])
                
                // array중 5번 인덱스부터 배열 자르기, 고차함수 사용해 [UInt8] -> [Int]로 변환
                let dateArray = array[5...].map { Int($0) }
                // 날짜정보 받아옴
                let date = BLEDataConverter.dateCalculate(dateArray)
                
                let name = peripheral.name ?? ""
                
                // 온도정보 생성
                let temperatureInformation = TemperatureInformation(name: name, temperature: temperature, date: date)
                try! realm.write {
                    // 데이터베이스에 온도정보 저장
                    realm.add(temperatureInformation)
                }
                
                saveTemperatureInformationToCloud(date: date, temperature: temperature)
            }
        }
    }
}

extension TempInfoViewController: UITableViewDelegate, UITableViewDataSource {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return temperatureInformationList.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "TempInfo", for: indexPath)
        
        let row = temperatureInformationList[indexPath.row]
        cell.textLabel?.text = "\(row.date) \(row.temperature)"
        
        return cell
    }
}
