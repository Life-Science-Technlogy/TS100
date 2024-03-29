//
//  RootViewController.swift
//  TS100BLEProject
//
//  Created by LST on 12/13/23.
//

import UIKit

class RootViewController: UIViewController {

    @IBOutlet weak var singleConnect: UIButton!
    @IBOutlet weak var multiConnect: UIButton!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // 타이틀 설정
        self.title = "TS100 Project"
        
        setButton()
    }
    
    // 버튼 설정
    private func setButton() {
        singleConnect.setTitle("Single Connect", for: .normal)
        singleConnect.tintColor = .white
        singleConnect.layer.cornerRadius = 10
        singleConnect.backgroundColor = .systemBlue
        
        multiConnect.setTitle("Multi Connect", for: .normal)
        multiConnect.tintColor = .white
        multiConnect.layer.cornerRadius = 10
        multiConnect.backgroundColor = .systemBlue
    }
}

