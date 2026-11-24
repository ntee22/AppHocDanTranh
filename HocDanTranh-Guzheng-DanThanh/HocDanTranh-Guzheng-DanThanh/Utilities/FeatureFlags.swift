//
//  FeatureFlags.swift
//  Dan Thanh Guzheng
//
//  Compile-time feature switches.
//

import Foundation

enum FeatureFlags {
    /// Paid lessons 8–16 (product `course_basic_1490k`).
    ///
    /// While false: StoreKit is never queried, paid lessons read "Coming soon", and no
    /// price, buy button, purchase sheet or restore row is reachable — so this build can be
    /// reviewed without any in-app purchase attached.
    ///
    /// Deliberately compile-time, not remote: guideline 2.3.1 forbids enabling hidden
    /// features after review. To sell the course: create the product in App Store Connect,
    /// set this to true, and submit that build together with the IAP for review.
    /// Mirror of Android `FeatureFlags.IAP_ENABLED`.
    static let iapEnabled = false
}
