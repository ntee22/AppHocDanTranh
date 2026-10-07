//
//  PurchaseBottomSheet.swift
//  Dan Thanh Guzheng
//
//  Bottom sheet showing course purchase options
//

import SwiftUI
import StoreKit

/// Elegant bottom sheet prompting course purchase with full pricing details
struct PurchaseBottomSheet: View {

    let lesson: Lesson
    @Binding var isPresented: Bool
    @EnvironmentObject var viewModel: LessonViewModel

    @State private var isPurchasing = false
    @State private var selectedOption: PurchaseOption = .course
    @State private var showingError = false
    @State private var errorMessage = ""

    private let paymentManager = PaymentManager.shared

    enum PurchaseOption: String, CaseIterable, Identifiable {
        case course   = "course"
        case lifetime = "lifetime"
        var id: String { rawValue }
    }

    var body: some View {
        ZStack {
            Color.backgroundCream.ignoresSafeArea()

            VStack(spacing: 20) {
                // Handle
                RoundedRectangle(cornerRadius: 3)
                    .fill(Color.secondary.opacity(0.3))
                    .frame(width: 40, height: 5)
                    .padding(.top, 8)

                // Icon
                ZStack {
                    Circle()
                        .fill(LinearGradient(
                            colors: [.premiumGold.opacity(0.3), .premiumGold.opacity(0.1)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ))
                        .frame(width: 72, height: 72)
                    Image(systemName: "sparkles")
                        .font(.system(size: 30))
                        .foregroundColor(.premiumGold)
                }

                // Title
                VStack(spacing: 6) {
                    Text("Mở Khóa Nội Dung")
                        .font(.heroTitle)
                        .foregroundColor(.secondaryBrown)
                    Text("Sở hữu mãi mãi, xem lại vô hạn")
                        .font(.subtitle)
                        .foregroundColor(.secondary)
                }

                // Purchase Options
                VStack(spacing: 12) {
                    purchaseOptionCard(
                        option: .course,
                        title: "📚 Khóa Học Cơ Bản",
                        subtitle: "16 video kỹ thuật nâng cao",
                        price: "1,490,000đ",
                        badge: nil
                    )

                    purchaseOptionCard(
                        option: .lifetime,
                        title: "🌟 Trọn Bộ Lifetime",
                        subtitle: "Tất cả khóa học + 20 bài hát + cập nhật",
                        price: "2,990,000đ",
                        badge: "BEST VALUE"
                    )
                }
                .padding(.horizontal)

                // Benefits
                VStack(alignment: .leading, spacing: 10) {
                    benefitRow(icon: "checkmark.seal.fill",   text: "Xem lại vô hạn lần")
                    benefitRow(icon: "arrow.clockwise",        text: "Sở hữu mãi mãi, không hết hạn")
                    benefitRow(icon: "video.fill",             text: "Video chất lượng cao")
                    benefitRow(icon: "person.fill.checkmark",  text: "Giảng viên chuyên nghiệp")
                }
                .padding(.horizontal)

                Spacer()

                // Purchase Button
                Button {
                    handlePurchase()
                } label: {
                    HStack {
                        if isPurchasing {
                            ProgressView().tint(.white)
                        } else {
                            Image(systemName: "lock.open.fill")
                            Text(purchaseButtonTitle)
                                .font(.buttonText)
                        }
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(
                        RoundedRectangle(cornerRadius: 16)
                            .fill(LinearGradient(
                                colors: [.primaryTeal, .accentTeal],
                                startPoint: .leading,
                                endPoint: .trailing
                            ))
                    )
                }
                .disabled(isPurchasing)
                .padding(.horizontal)

                Button {
                    handleRestore()
                } label: {
                    Text("Khôi phục giao dịch")
                        .font(.caption)
                        .foregroundColor(.primaryTeal)
                }
                .padding(.bottom, 8)
            }
        }
        .alert("Giao dịch thất bại", isPresented: $showingError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
    }

    // MARK: - Option Card

    private func purchaseOptionCard(
        option: PurchaseOption,
        title: String,
        subtitle: String,
        price: String,
        badge: String?
    ) -> some View {
        Button {
            selectedOption = option
        } label: {
            HStack(spacing: 12) {
                Image(systemName: selectedOption == option
                      ? "largecircle.fill.circle"
                      : "circle")
                    .foregroundColor(.primaryTeal)
                    .font(.title3)

                VStack(alignment: .leading, spacing: 2) {
                    HStack {
                        Text(title)
                            .font(.buttonText)
                            .foregroundColor(.primary)
                        if let badge {
                            Text(badge)
                                .font(.system(size: 10, weight: .bold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color.premiumGold)
                                .clipShape(Capsule())
                        }
                    }
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer()

                Text(price)
                    .font(.buttonText)
                    .foregroundColor(.primaryTeal)
            }
            .padding(12)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(selectedOption == option
                          ? Color.primaryTeal.opacity(0.08)
                          : Color(.systemBackground))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(selectedOption == option
                            ? Color.primaryTeal
                            : Color.gray.opacity(0.2),
                            lineWidth: 1.5)
            )
        }
        .buttonStyle(.plain)
    }

    private func benefitRow(icon: String, text: String) -> some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .foregroundColor(.primaryTeal)
                .frame(width: 22)
            Text(text)
                .font(.bodyText)
                .foregroundColor(.primary)
        }
    }

    // MARK: - Computed

    private var purchaseButtonTitle: String {
        switch selectedOption {
        case .course:   return "Mua Khóa Cơ Bản — 1,490,000đ"
        case .lifetime: return "Mua Trọn Bộ — 2,990,000đ"
        }
    }

    private var selectedProductID: String {
        switch selectedOption {
        case .course:   return IAPProductID.courseBasic
        case .lifetime: return IAPProductID.lifetimeAll
        }
    }

    // MARK: - Actions

    private func handlePurchase() {
        isPurchasing = true
        Task {
            do {
                try await paymentManager.purchase(selectedProductID)
                await MainActor.run {
                    isPurchasing = false
                    isPresented = false
                }
            } catch {
                await MainActor.run {
                    isPurchasing = false
                    errorMessage = error.localizedDescription
                    showingError = true
                }
            }
        }
    }

    private func handleRestore() {
        isPurchasing = true
        Task {
            do {
                try await viewModel.restorePurchases()
                await MainActor.run {
                    isPurchasing = false
                    isPresented = false
                }
            } catch {
                await MainActor.run {
                    isPurchasing = false
                    errorMessage = "Không tìm thấy giao dịch trước đó"
                    showingError = true
                }
            }
        }
    }
}

#Preview {
    PurchaseBottomSheet(
        lesson: Lesson.mockLessons()[7],
        isPresented: .constant(true)
    )
    .environmentObject(LessonViewModel())
}
