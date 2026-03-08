//
//  FilterCondition.swift
//  CapacitorPluginDataViewer
//
//  Created by cmd on 8/3/26.
//

import Foundation

struct FilterCondition: Identifiable {
    let id = UUID()
    var column: String
    var op: String = "="
    var value: String = ""
}
