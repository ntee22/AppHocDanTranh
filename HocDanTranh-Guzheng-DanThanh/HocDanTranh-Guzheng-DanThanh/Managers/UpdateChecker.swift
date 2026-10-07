//
//  UpdateChecker.swift
//  Dan Thanh Guzheng
//
//  Soft-remind users when a newer version is on the App Store.
//  Uses Apple's iTunes Lookup API — no backend infrastructure needed.
//

import Foundation
import Combine
import SwiftUI
import UIKit

@MainActor
final class UpdateChecker: ObservableObject {
    static let shared = UpdateChecker()

    /// True when a newer version is live AND the user hasn't recently dismissed the banner.
    @Published private(set) var hasUpdate: Bool = false
    /// The version available on the App Store (set after a successful check).
    @Published private(set) var latestVersion: String?

    private let appStoreAppID = "6767492629"
    private let lookupURL = URL(string: "https://itunes.apple.com/lookup?id=6767492629")!

    private let userDefaults = UserDefaults.standard
    private let dismissedAtKey = "updatePromptDismissedAt"
    private let dismissedVersionKey = "updatePromptDismissedForVersion"
    private let lastCheckKey = "updateLastCheckedAt"

    private let dismissCooldownDays = 3
    private let recheckIntervalHours = 24

    private init() {}

    var currentVersion: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""
    }

    var appStoreURL: URL? {
        URL(string: "https://apps.apple.com/app/id\(appStoreAppID)")
    }

    // MARK: - Public API

    /// Call this on app launch. Network check runs at most once per `recheckIntervalHours`.
    func checkForUpdateIfNeeded() {
        if let lastCheck = userDefaults.object(forKey: lastCheckKey) as? Date {
            let hoursSince = Date().timeIntervalSince(lastCheck) / 3600
            if hoursSince < Double(recheckIntervalHours) {
                // Still re-evaluate banner visibility from cached data
                evaluateBannerVisibility()
                return
            }
        }
        Task {
            await fetchLatestVersion()
        }
    }

    /// User tapped "Later" — hide the banner for the cooldown period.
    func dismissBanner() {
        userDefaults.set(Date(), forKey: dismissedAtKey)
        userDefaults.set(latestVersion ?? "", forKey: dismissedVersionKey)
        hasUpdate = false
    }

    /// User tapped "Update" — open the App Store and hide the banner.
    func openAppStore() {
        if let url = appStoreURL {
            UIApplication.shared.open(url)
        }
        // Hide the banner immediately so the user isn't pestered when they return.
        hasUpdate = false
    }

    // MARK: - Internal

    private func fetchLatestVersion() async {
        do {
            let (data, _) = try await URLSession.shared.data(from: lookupURL)
            let decoded = try JSONDecoder().decode(LookupResponse.self, from: data)
            guard let result = decoded.results.first else { return }

            userDefaults.set(Date(), forKey: lastCheckKey)
            latestVersion = result.version
            evaluateBannerVisibility()
        } catch {
            // Network failure / parsing error — silently ignore. The banner stays hidden.
            print("UpdateChecker: lookup failed: \(error)")
        }
    }

    private func evaluateBannerVisibility() {
        guard let latest = latestVersion ?? userDefaults.string(forKey: "updateLatestVersionCache") else {
            hasUpdate = false
            return
        }
        // Cache the latest version so re-evaluation works offline within the recheck window.
        userDefaults.set(latest, forKey: "updateLatestVersionCache")

        // Numeric string compare handles "1.0.10" > "1.0.9" correctly.
        guard currentVersion.compare(latest, options: .numeric) == .orderedAscending else {
            hasUpdate = false
            return
        }

        // Respect dismissal cooldown — but only for the version that was dismissed.
        // A newer release supersedes the prior dismissal.
        if let dismissedAt = userDefaults.object(forKey: dismissedAtKey) as? Date,
           let dismissedForVersion = userDefaults.string(forKey: dismissedVersionKey),
           dismissedForVersion == latest {
            let daysSince = Date().timeIntervalSince(dismissedAt) / 86400
            if daysSince < Double(dismissCooldownDays) {
                hasUpdate = false
                return
            }
        }

        latestVersion = latest
        hasUpdate = true
    }

    // MARK: - iTunes Lookup response

    private struct LookupResponse: Decodable {
        let results: [LookupResult]
    }

    private struct LookupResult: Decodable {
        let version: String
    }
}
