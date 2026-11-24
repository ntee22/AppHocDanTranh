//
//  PaymentManager.swift
//  Dan Thanh Guzheng
//
//  Manages in-app purchases using StoreKit 2
//

import Foundation
import StoreKit
import Combine

// MARK: - Product IDs

enum IAPProductID {
    // Khóa học cơ bản
    static let courseBasic         = "course_basic_1490k"

    // Bài hát - Combo (Video + Sheet)
    static let songDiVangCombo     = "song_divang_combo_150k"
    static let songLyNguaCombo     = "song_lynguao_combo_150k"

    // Bài hát - Sheet only
    static let songDiVangSheet     = "song_divang_sheet_100k"
    static let songLyNguaSheet     = "song_lynguao_sheet_100k"

    // Bài hát - Video only
    static let songDiVangVideo     = "song_divang_video_80k"
    static let songLyNguaVideo     = "song_lynguao_video_80k"

    // Bundles
    static let bundle5Songs        = "bundle_5songs_600k"
    static let bundle10Songs       = "bundle_10songs_1000k"
    static let bundle20Songs       = "bundle_20songs_1800k"

    // Lifetime
    static let lifetimeAll         = "lifetime_all_2990k"

    /// All product IDs to fetch from App Store
    static let all: [String] = [
        courseBasic,
        songDiVangCombo, songLyNguaCombo,
        songDiVangSheet, songLyNguaSheet,
        songDiVangVideo, songLyNguaVideo,
        bundle5Songs, bundle10Songs, bundle20Songs,
        lifetimeAll
    ]
}

// MARK: - IAPManager

/// Manager responsible for handling all in-app purchases (StoreKit 2)
@MainActor
class PaymentManager: ObservableObject {

    static let shared = PaymentManager()

    // MARK: - Published Properties

    @Published var products: [Product] = []
    @Published var purchasedProductIDs = Set<String>()
    @Published var isProcessing = false

    // Convenience accessors
    var hasPurchasedCourse: Bool {
        guard FeatureFlags.iapEnabled else { return false }
        return purchasedProductIDs.contains(IAPProductID.courseBasic) ||
        purchasedProductIDs.contains(IAPProductID.lifetimeAll)
    }

    var hasPurchasedLifetime: Bool {
        purchasedProductIDs.contains(IAPProductID.lifetimeAll)
    }

    // MARK: - Initialization

    private init() {
        // Selling is off: never touch StoreKit, so this build has no IAP surface at all.
        guard FeatureFlags.iapEnabled else { return }
        Task {
            await loadProducts()
            await updatePurchasedProducts()
            await observeTransactions()
        }
    }

    // MARK: - Load Products

    func loadProducts() async {
        do {
            products = try await Product.products(for: IAPProductID.all)
        } catch {
            print("PaymentManager: Failed to load products — \(error)")
        }
    }

    func product(for id: String) -> Product? {
        products.first { $0.id == id }
    }

    // MARK: - Purchase

    func purchase(_ productID: String) async throws {
        guard FeatureFlags.iapEnabled else { throw PaymentError.productNotFound }
        guard let product = product(for: productID) else {
            throw PaymentError.productNotFound
        }
        try await purchase(product)
    }

    func purchase(_ product: Product) async throws {
        isProcessing = true
        defer { isProcessing = false }

        let result = try await product.purchase()

        switch result {
        case .success(let verification):
            let transaction = try checkVerified(verification)
            await updatePurchasedProducts()
            await transaction.finish()

        case .userCancelled:
            break   // user tapped cancel — not an error

        case .pending:
            break   // Ask to Buy / deferred

        @unknown default:
            break
        }
    }

    // MARK: - Restore

    func restorePurchases() async throws {
        guard FeatureFlags.iapEnabled else { return }
        isProcessing = true
        defer { isProcessing = false }
        try await AppStore.sync()
        await updatePurchasedProducts()
    }

    // MARK: - Check Access

    func hasPurchased(_ productID: String) -> Bool {
        purchasedProductIDs.contains(productID)
    }

    // MARK: - Private

    private func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .unverified:
            throw PaymentError.verificationFailed
        case .verified(let safe):
            return safe
        }
    }

    private func updatePurchasedProducts() async {
        var ids = Set<String>()
        for await result in Transaction.currentEntitlements {
            guard case .verified(let transaction) = result,
                  transaction.revocationDate == nil else { continue }
            ids.insert(transaction.productID)
        }
        purchasedProductIDs = ids
    }

    private func observeTransactions() async {
        for await result in Transaction.updates {
            guard case .verified(let transaction) = result else { continue }
            await transaction.finish()
            await updatePurchasedProducts()
        }
    }

    // MARK: - Errors

    enum PaymentError: LocalizedError {
        case productNotFound
        case verificationFailed
        case purchaseFailed

        var errorDescription: String? {
            switch self {
            case .productNotFound:      return "Sản phẩm không tồn tại"
            case .verificationFailed:   return "Xác minh giao dịch thất bại"
            case .purchaseFailed:       return "Không thể hoàn tất giao dịch"
            }
        }
    }
}
