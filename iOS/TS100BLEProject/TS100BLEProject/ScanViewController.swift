//
//  ScanViewController.swift
//  TS100BLEProject
//
//  Created by LST on 12/14/23.
//

import UIKit
import CoreBluetooth

var selectDevice: CBPeripheral? = nil

class ScanViewController: UIViewController {
    
    @IBOutlet weak var tableView: UITableView!
    
    var bluetoothManager = BluetoothManager.shared
    
    var scanDevice: [CBPeripheral] = [] {
        didSet {
            tableView.reloadData()
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.title = "Device Scan"
        
        // selectDevice 초기화
        selectDevice = nil
        
        tableView.delegate = self
        tableView.dataSource = self
        
        bluetoothManager.didDiscover = { peripheral in
            let bleDeviceName = String(peripheral.name ?? "")
            
            // "TS100" 이름을 가진 블루투스 기기만 출력
            if bleDeviceName.contains("TS100") {
                if !self.scanDevice.contains(peripheral) {
                    self.scanDevice.append(peripheral)
                }
            }
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        // CentralManager의 상태가 poweredOn이라면 Scan 시작
        if bluetoothManager.managerState == .poweredOn {
            bluetoothManager.startScan()
        }
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        // 스캔 종료
        bluetoothManager.stopScan()
    }
}

extension ScanViewController: UITableViewDelegate, UITableViewDataSource {
    // 리턴할 Item 개수
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return scanDevice.count
    }
    
    // 리턴할 Item 정보
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "ScanDevice", for: indexPath)
        
        cell.textLabel?.text = scanDevice[indexPath.row].name
        
        return cell
    }
    
    // TableViewCell 선택했을 때 호출되는 메서드
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let device = scanDevice[indexPath.row]
        
        // 연결할 Device 전역변수에 할당
        selectDevice = device
        
        // 화면 뒤로가기
        self.navigationController?.popViewController(animated: true)
    }
}
