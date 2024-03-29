//
//  SingleConnectViewController.swift
//  TS100BLEProject
//
//  Created by LST on 2023/08/01.
//

import UIKit
import CoreBluetooth
import RealmSwift

var connectedPeripheral: CBPeripheral!

class SingleConnectViewController: UIViewController {
    
    // realm 열기
    let realm = try! Realm()
        
    @IBOutlet weak var tableView: UITableView!
    
    let bluetoothManager = BluetoothManager.shared
    
    var scanDevice: [CBPeripheral] = [] {
        didSet {
            tableView.reloadData()
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        bluetoothManager.didDiscover = { peripheral in
            let bleDeviceName = String(peripheral.name ?? "")
            
            // "TS100" 이름을 가진 블루투스 기기만 출력
            if bleDeviceName.contains("TS100") {
                if !self.scanDevice.contains(peripheral) {
                    self.scanDevice.append(peripheral)
                }
            }
        }
        
        bluetoothManager.didConnect = { peripheral in
            // peripheral 전역변수에 할당
            connectedPeripheral = peripheral
            
            guard let viewController = self.storyboard?.instantiateViewController(withIdentifier: "TempInfoViewController") else { return }
            
            self.navigationController?.pushViewController(viewController, animated: true)
        }
        
        bluetoothManager.updateState = { state in
            if state == .poweredOn {
                self.bluetoothManager.startScan()
            }
        }
        
        self.view.backgroundColor = .white
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        // 배열 초기화
        scanDevice = []
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        if bluetoothManager.managerState == .poweredOn {
            bluetoothManager.startScan()
        }
    }
}

extension SingleConnectViewController: UITableViewDelegate, UITableViewDataSource {
    // 리턴할 Item 개수
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return scanDevice.count
    }
    
    // 리턴할 Item 정보
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "TS100Name", for: indexPath)
        
        cell.textLabel?.text = scanDevice[indexPath.row].name
        
        return cell
    }
    
    // TableViewCell 선택했을 때 호출되는 메서드
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let device = scanDevice[indexPath.row]
        
        // Device scan 종료
        bluetoothManager.stopScan()
        
        // 블루투스 연결
        bluetoothManager.connect(device)
    }
}
