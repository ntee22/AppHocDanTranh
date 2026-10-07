//
//  AppColors.swift
//  Dan Thanh Guzheng
//
//  Vintage paper + sage + wood + plum palette
//

import SwiftUI

// MARK: - Vintage Color Palette

extension Color {

    // Paper tones
    static let paper        = Color(hex: "F2E8D5")
    static let paperWarm    = Color(hex: "FFF8EB")
    static let paperDeep    = Color(hex: "E8DCC0")

    // Ink (text)
    static let inkDark      = Color(hex: "3A3228")
    static let inkSoft      = Color(hex: "5C4E3D")
    static let inkMuted     = Color(hex: "8A7B66")

    // Sage green
    static let sage         = Color(hex: "8A9A7B")
    static let sageDark     = Color(hex: "6B7C5E")
    static let sageSoft     = Color(hex: "B5C2A6")

    // Wood
    static let wood         = Color(hex: "B8956A")
    static let woodDark     = Color(hex: "8B6F4E")
    static let woodSoft     = Color(hex: "D4B896")

    // Plum / accent
    static let plum         = Color(hex: "A65F5F")
    static let plumSoft     = Color(hex: "C48888")

    // Special
    static let vintagGold   = Color(hex: "C9A961")
    static let sealRed      = Color(hex: "B84A3E")

    // Legacy aliases (keep existing code compiling)
    static let primaryTeal      = Color.sageDark
    static let secondaryBrown   = Color.inkDark
    static let backgroundCream  = Color.paper
    static let accentTeal       = Color.sage
    static let lightTeal        = Color.sageSoft
    static let cardBackground   = Color.paperWarm
    static let lockedGray       = Color.plum
    static let successGreen     = Color.sageDark
    static let premiumGold      = Color.vintagGold
}

// MARK: - Hex Initializer

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let r = (int >> 16) & 0xFF
        let g = (int >> 8)  & 0xFF
        let b = int         & 0xFF
        self.init(.sRGB,
                  red:   Double(r) / 255,
                  green: Double(g) / 255,
                  blue:  Double(b) / 255,
                  opacity: 1)
    }
}
