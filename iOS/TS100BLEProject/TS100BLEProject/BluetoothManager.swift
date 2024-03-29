//
//  BluetoothManager.swift
//  TS100BLEProject
//
//  Created by LST on 12/19/23.
//

import CoreBluetooth

class BluetoothManager: NSObject {
    static let shared = BluetoothManager()

    private var centralManager: CBCentralManager!
    
    var connectedDevices: [CBPeripheral] = []
    
    var managerState: CBManagerState!
    
    // 콜백 변수
    var updateState: ((CBManagerState) -> Void)?
    var didDiscover: ((CBPeripheral) -> Void)?
    var didConnect: ((CBPeripheral) -> Void)?

    private override init() {
        super.init()
        
        centralManager = CBCentralManager(delegate: self, queue: nil)
    }
}

extension BluetoothManager {
    // 스캔 시작
    func startScan() {
        centralManager.scanForPeripherals(withServices: nil)
        
        updateConnectDevice()
    }
    
    // 스캔 종료
    func stopScan() {
        centralManager.stopScan()
    }
    
    // 연결된 디바이스 최신화
    private func updateConnectDevice() {
        connectedDevices = centralManager.retrieveConnectedPeripherals(withServices: [])
    }
    
    // 기기 연결
    func connect(_ peripheral: CBPeripheral) {
        centralManager.connect(peripheral)
    }
    
    // 기기 연결 해제
    func disconnect(_ peripheral: CBPeripheral) {
        centralManager.cancelPeripheralConnection(peripheral)
    }
}

extension BluetoothManager: CBCentralManagerDelegate {
    // 1. Central의 상태가 변경되었을 때 호출되는 메서드
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        managerState = central.state
        updateState?(central.state)
    }
    
    // 2. Device가 Scan되었을 때 호출되는 메서드
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        
        // 기존에 연결된 기기가 있다면 didDiscover 호출되지 않도록
        connectedDevices.forEach { connectedPeripheral in
            if connectedPeripheral.identifier == peripheral.identifier {
                return
            }
        }
        
        didDiscover?(peripheral)
    }
    
    // 3. Device가 connect되었을 때 호출되는 메서드
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        didConnect?(peripheral)
    }
}
