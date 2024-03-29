//
//  MultiConnectViewController.swift
//  TS100BLEProject
//
//  Created by LST on 12/13/23.
//

import UIKit
import CoreBluetooth

struct TS100Device {
    let peripheral: CBPeripheral
    var temperature: Double = 0.0
}

class MultiConnectViewController: UIViewController {
    
    @IBOutlet weak var tableView: UITableView!
    
    var connectDevices: [TS100Device] = [] {
        didSet {
            tableView.reloadData()
        }
    }
    
    var bluetoothManager = BluetoothManager.shared
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.title = "Multi Device Connect"
        
        self.navigationItem.rightBarButtonItem?.title = "Scan"
        
        tableView.delegate = self
        tableView.dataSource = self
        
        bluetoothManager.didConnect = { [weak self] peripheral in
            self?.connectDevices.append(TS100Device(peripheral: peripheral))
            
            peripheral.delegate = self
            // Service 검색(Health Thermometer 관련된 Service만 수신할 수 있도록 변경)
            peripheral.discoverServices([CBUUID(string: "1809")])
        }
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        // 만약 선택한 기기가 있다면 연결
        if let device = selectDevice {
            bluetoothManager.connect(device)
        }
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        
        selectDevice = nil
    }
    
    deinit {
        // 메모리 해제 시 전체 기기 disconnect
        connectDevices.forEach { peripheral in
            bluetoothManager.disconnect(peripheral.peripheral)
        }
    }
}

extension MultiConnectViewController: UITableViewDelegate, UITableViewDataSource {
    // 리턴할 Item 개수
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return connectDevices.count
    }
    
    // 리턴할 Item 정보
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "TemperatureInformation", for: indexPath)
        
        let device = connectDevices[indexPath.row]
        let text = "\(device.peripheral.name ?? ""), \(device.temperature)°C"
        
        cell.textLabel?.text = text
        
        return cell
    }
}

extension MultiConnectViewController: CBPeripheralDelegate {

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
        }
    }
    
    // 3. characteristic에서 read, notify 설정 후 데이터가 수신되었을 때 호출되는 메서드
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        if characteristic.uuid == CBUUID(string: "2A1C") {
            if let value = characteristic.value {
                let array = [UInt8](value)
                
                // 온도정보 받아옴
                let temperature = BLEDataConverter.temperatureCalculate(array[1], array[2], array[3], array[4])
                
                let name = peripheral.name ?? ""
                
                // connectDevices중 notify를 받은 기기가 있다면 온도정보 업데이트
                if let index = connectDevices.firstIndex(where: { $0.peripheral == peripheral }) {
                    connectDevices[index].temperature = temperature
                }
            }
        }
    }
}
