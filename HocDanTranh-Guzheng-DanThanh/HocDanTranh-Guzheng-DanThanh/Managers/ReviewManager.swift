//
//  ReviewManager.swift
//  Dan Thanh Guzheng
//
//  Throttled SKStoreReviewController prompts after positive user actions.
//

import StoreKit
import SwiftUI
import UIKit

@MainActor
final class ReviewManager {
    static let shared = ReviewManager()
    private init() {}

    private let userDefaults = UserDefaults.standard
    private let lastReviewRequestKey = "lastReviewRequestDate"
    private let appVersionAtLastRequestKey = "appVersionAtLastReviewRequest"
    private let actionCountKey = "reviewTriggerActionCount"

    private let positiveActionsThreshold = 3
    private let minDaysBetweenRequests = 60

    /// Call after a positive action (lesson completed, tuner string matched...).
    /// Automatically triggers the review prompt when thresholds are met.
    func recordPositiveAction() {
        let count = userDefaults.integer(forKey: actionCountKey) + 1
        userDefaults.set(count, forKey: actionCountKey)

        if count >= positiveActionsThreshold {
            requestReviewIfAppropriate()
        }
    }

    /// Call after a successful IAP. Skips the action counter.
    func requestReviewAfterPurchase() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            self?.requestReviewIfAppropriate()
        }
    }

    private func requestReviewIfAppropriate() {
        let currentVersion = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""
        let lastVersion = userDefaults.string(forKey: appVersionAtLastRequestKey) ?? ""

        if currentVersion == lastVersion {
            return
        }

        if let lastDate = userDefaults.object(forKey: lastReviewRequestKey) as? Date {
            let daysSince = Calendar.current.dateComponents([.day], from: lastDate, to: Date()).day ?? 0
            if daysSince < minDaysBetweenRequests {
                return
            }
        }

        guard let scene = UIApplication.shared.connectedScenes
            .first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene else {
            return
        }

        AppStore.requestReview(in: scene)

        userDefaults.set(Date(), forKey: lastReviewRequestKey)
        userDefaults.set(currentVersion, forKey: appVersionAtLastRequestKey)
        userDefaults.set(0, forKey: actionCountKey)
    }
}
