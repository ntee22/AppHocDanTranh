//
//  SettingsView.swift
//  Dan Thanh Guzheng
//
//  Vintage-styled settings screen — Đan Thanh edition
//

import SwiftUI
import UIKit

struct SettingsView: View {

    @AppStorage("appLanguage") private var appLanguage: String = "vi"
    @AppStorage("tunerSensitivity") private var tunerSensitivity: String = "medium"

    var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
    }

    var isEnglish: Bool { appLanguage == "en" }

    private var sensitivityHint: String {
        switch tunerSensitivity {
        case "low":
            return isEnglish
                ? "Locks slowly — most stable, avoids false matches."
                : "Khóa chậm — ổn định nhất, ít báo nhầm."
        case "high":
            return isEnglish
                ? "Locks quickly — very responsive but can match briefly."
                : "Khóa nhanh — phản hồi nhanh nhưng có thể báo sớm."
        default:
            return isEnglish
                ? "Balanced lock speed — recommended."
                : "Tốc độ khóa cân bằng — khuyến nghị."
        }
    }

    @ViewBuilder
    private func sensitivityButton(label: String, tag: String) -> some View {
        let isSelected = tunerSensitivity == tag
        Button {
            withAnimation(.easeInOut(duration: 0.15)) {
                tunerSensitivity = tag
            }
        } label: {
            Text(label)
                .font(.system(size: 14, weight: isSelected ? .semibold : .regular, design: .serif))
                .foregroundColor(isSelected ? Color.paper : Color.inkSoft)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(isSelected ? Color.sageDark : Color.clear)
        }
        .buttonStyle(PlainButtonStyle())
    }

    var body: some View {
        ZStack {
            PaperBackground()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {

                    // ── Header ──
                    VStack(alignment: .leading, spacing: 2) {
                        OverlineLabel(text: "設定 · Cài đặt")
                        Text(isEnglish ? "Settings" : "Cài đặt")
                            .font(.system(size: 32, weight: .medium, design: .serif))
                            .foregroundColor(Color.inkDark)
                    }
                    .padding(.top, 56)
                    .padding(.horizontal, 24)
                    .padding(.bottom, 20)

                    // ── Language Selector ──
                    languageSelector
                        .padding(.horizontal, 18)
                        .padding(.bottom, 24)

                    // ── Section 1: Về Đan Thanh ──
                    SettingsSectionView(title: isEnglish ? "About Đan Thanh" : "Về Đan Thanh") {

                        // Bio card
                        VStack(alignment: .leading, spacing: 10) {
                            HStack(spacing: 12) {
                                InkSeal(char: "琴", size: 32, rotate: -3)
                                VStack(alignment: .leading, spacing: 3) {
                                    Text("Đan Thanh Đàn Tranh")
                                        .font(.system(size: 19, weight: .medium, design: .serif))
                                        .foregroundColor(Color.inkDark)
                                    Text("Guzheng · Đàn Tranh")
                                        .font(.system(size: 13, design: .serif))
                                        .foregroundColor(Color.inkMuted)
                                }
                            }
                            .padding(.bottom, 4)

                            Text(isEnglish
                                 ? "Đan Thanh offers offline classes in Tân Bình and Gò Vấp, as well as online group classes and private 1-on-1 lessons. Feel free to reach out with any questions or feedback — thank you so much for your support!"
                                 : "Đan Thanh có các khóa offline tại Tân Bình và Gò Vấp, có khóa học online tập thể và cả dạy kèm 1-1. Mọi người hãy liên hệ nếu có thắc mắc và góp ý nhé, cảm ơn đã ủng hộ!")
                                .font(.system(size: 15, design: .serif))
                                .foregroundColor(Color.inkSoft)
                                .lineSpacing(6)
                        }
                        .padding(18)
                        .background(Color.paperWarm.opacity(0.6))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(Color.woodDark.opacity(0.15), lineWidth: 1)
                        )
                        .padding(.horizontal, 18)
                        .padding(.bottom, 6)

                        SettingsRow(
                            icon: "hand.thumbsup.fill",
                            iconColor: Color(hex: "1877F2"),
                            title: "Facebook Fanpage",
                            subtitle: isEnglish ? "Follow & contact us here" : "Theo dõi & liên hệ tại đây"
                        ) {
                            UIApplication.shared.open(
                                URL(string: "https://www.facebook.com/dantranhhcm")!
                            )
                        }

                        SettingsRow(
                            icon: "music.note",
                            iconColor: Color.inkDark,
                            title: "TikTok",
                            subtitle: "@danthanhdantranh"
                        ) {
                            UIApplication.shared.open(
                                URL(string: "https://www.tiktok.com/@danthanhdantranh")!
                            )
                        }

                        SettingsRow(
                            icon: "play.rectangle.fill",
                            iconColor: Color(hex: "FF0000"),
                            title: "YouTube",
                            subtitle: "@danthanhdantranh"
                        ) {
                            UIApplication.shared.open(
                                URL(string: "https://www.youtube.com/@danthanhdantranh")!
                            )
                        }

                        SettingsRow(
                            icon: "cart.fill",
                            iconColor: Color(hex: "EE4D2D"),
                            title: isEnglish ? "Shopee — Buy instruments & accessories" : "Shopee — Mua đàn & phụ kiện",
                            subtitle: isEnglish ? "Guzheng, picks, accessories — always on sale! 🛍️" : "Guzheng, móng gảy, phụ kiện — sale liên tục! 🛍️"
                        ) {
                            UIApplication.shared.open(
                                URL(string: "https://shopee.vn/danthanhdantranh?categoryId=100630&entryPoint=ShopByPDP&itemId=26115698334")!
                            )
                        }
                    }

                    // ── Section 2: Ứng dụng ──
                    SettingsSectionView(title: isEnglish ? "App" : "Ứng dụng") {

                        SettingsRow(
                            icon: "star.fill",
                            iconColor: Color.vintagGold,
                            title: isEnglish ? "Rate 5 stars ⭐⭐⭐⭐⭐" : "Đánh giá 5 sao ⭐⭐⭐⭐⭐",
                            subtitle: isEnglish ? "Built with passion and lots of coffee ☕" : "App được tạo ra bởi rất nhiều đam mê và cà phê ☕"
                        ) {
                            if let url = URL(string: "https://apps.apple.com/app/id6767492629?action=write-review") {
                                UIApplication.shared.open(url)
                            }
                        }

                        SettingsRow(
                            icon: "square.and.arrow.up",
                            iconColor: Color.sageDark,
                            title: isEnglish ? "Share App" : "Chia sẻ ứng dụng",
                            subtitle: isEnglish ? "Tell your friends to learn Guzheng too" : "Giới thiệu bạn bè cùng học Guzheng"
                        ) {
                            let text = isEnglish
                                ? "I'm learning Guzheng with the Học Đàn Tranh app — it's great! Check it out: https://apps.apple.com/app/id6767492629"
                                : "Mình đang học Guzheng với app Học Đàn Tranh, rất hay! Bạn thử xem: https://apps.apple.com/app/id6767492629"
                            let av = UIActivityViewController(
                                activityItems: [text],
                                applicationActivities: nil
                            )
                            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                               let vc = windowScene.windows.first?.rootViewController {
                                vc.present(av, animated: true)
                            }
                        }

                        SettingsRow(
                            icon: "envelope.fill",
                            iconColor: Color.inkMuted,
                            title: isEnglish ? "Feedback & Contact" : "Góp ý & Liên hệ",
                            subtitle: isEnglish ? "Send us an email" : "Gửi email cho chúng mình"
                        ) {
                            let subject = "Góp ý app Học Đàn Tranh".addingPercentEncoding(
                                withAllowedCharacters: .urlQueryAllowed
                            ) ?? ""
                            if let url = URL(string: "mailto:danthanhdantranh@gmail.com?subject=\(subject)") {
                                UIApplication.shared.open(url)
                            }
                        }

                        // Required by App Review once the course is sold; hidden until then.
                        if FeatureFlags.iapEnabled {
                            SettingsRow(
                                icon: "arrow.clockwise.circle.fill",
                                iconColor: Color.sageDark,
                                title: isEnglish ? "Restore purchases" : "Khôi phục giao dịch",
                                subtitle: isEnglish ? "Bought on this Apple ID" : "Đã mua trên Apple ID này"
                            ) {
                                Task { try? await PaymentManager.shared.restorePurchases() }
                            }
                        }
                    }

                    // ── Section 3: Tuner ──
                    SettingsSectionView(title: isEnglish ? "Tuner" : "Lên dây") {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack(spacing: 14) {
                                ZStack {
                                    RoundedRectangle(cornerRadius: 8)
                                        .fill(Color.sageDark.opacity(0.15))
                                        .frame(width: 36, height: 36)
                                    Image(systemName: "tuningfork")
                                        .font(.system(size: 17))
                                        .foregroundColor(Color.sageDark)
                                }
                                Text(isEnglish ? "Tuner sensitivity" : "Độ nhạy tuner")
                                    .font(.system(size: 17, design: .serif))
                                    .foregroundColor(Color.inkDark)
                                Spacer()
                            }

                            HStack(spacing: 0) {
                                sensitivityButton(
                                    label: isEnglish ? "Low" : "Thấp",
                                    tag: "low"
                                )
                                Rectangle()
                                    .fill(Color.inkMuted.opacity(0.2))
                                    .frame(width: 1)
                                sensitivityButton(
                                    label: isEnglish ? "Medium" : "Vừa",
                                    tag: "medium"
                                )
                                Rectangle()
                                    .fill(Color.inkMuted.opacity(0.2))
                                    .frame(width: 1)
                                sensitivityButton(
                                    label: isEnglish ? "High" : "Cao",
                                    tag: "high"
                                )
                            }
                            .background(Color.paperWarm.opacity(0.55))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(Color.inkMuted.opacity(0.2), lineWidth: 1)
                            )

                            Text(sensitivityHint)
                                .font(.system(size: 12, design: .serif))
                                .italic()
                                .foregroundColor(Color.inkMuted)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 14)
                    }

                    // ── Section 4: Thông tin ──
                    SettingsSectionView(title: isEnglish ? "Info" : "Thông tin") {

                        HStack(spacing: 14) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(Color.inkMuted.opacity(0.12))
                                    .frame(width: 36, height: 36)
                                Image(systemName: "info.circle.fill")
                                    .font(.system(size: 17))
                                    .foregroundColor(Color.inkMuted)
                            }
                            Text(isEnglish ? "Version" : "Phiên bản")
                                .font(.system(size: 17, design: .serif))
                                .foregroundColor(Color.inkDark)
                            Spacer()
                            Text(appVersion)
                                .font(.system(size: 15, design: .serif))
                                .italic()
                                .foregroundColor(Color.inkMuted)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 14)
                    }

                    // ── Footer ──
                    VStack(spacing: 6) {
                        Text("· 琴 ·")
                            .font(.system(size: 24))
                            .foregroundColor(Color.inkMuted.opacity(0.5))
                        Text("Made with 💚 & ☕ in Việt Nam")
                            .font(.system(size: 13, design: .serif))
                            .italic()
                            .foregroundColor(Color.inkMuted.opacity(0.6))
                        Text("© 2025 Đan Thanh Đàn Tranh")
                            .font(.system(size: 12, design: .serif))
                            .foregroundColor(Color.inkMuted.opacity(0.4))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.top, 24)
                }
                .readableContentWidth()
            }
        }
    }

    // MARK: - Language Selector

    private var languageSelector: some View {
        VStack(alignment: .leading, spacing: 10) {
            OverlineLabel(text: isEnglish ? "Language · Ngôn ngữ" : "Ngôn ngữ · Language")
                .padding(.horizontal, 8)

            HStack(spacing: 0) {
                langButton(flag: "🇻🇳", label: "Tiếng Việt", tag: "vi")

                Rectangle()
                    .fill(Color.inkMuted.opacity(0.2))
                    .frame(width: 1)

                langButton(flag: "🇬🇧", label: "English", tag: "en")
            }
            .background(Color.paperWarm.opacity(0.55))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color.inkMuted.opacity(0.18), lineWidth: 1)
            )
            .shadow(color: Color.inkDark.opacity(0.06), radius: 4, y: 2)
        }
    }

    @ViewBuilder
    private func langButton(flag: String, label: String, tag: String) -> some View {
        let isSelected = appLanguage == tag
        Button {
            withAnimation(.easeInOut(duration: 0.2)) {
                appLanguage = tag
            }
        } label: {
            VStack(spacing: 6) {
                Text(flag)
                    .font(.system(size: 28))
                Text(label)
                    .font(.system(size: 15, weight: isSelected ? .semibold : .regular, design: .serif))
                    .foregroundColor(isSelected ? Color.paper : Color.inkSoft)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(
                isSelected
                    ? Color.sageDark
                    : Color.clear
            )
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// ── Reusable Section Container ──
struct SettingsSectionView<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            OverlineLabel(text: title)
                .padding(.horizontal, 26)
                .padding(.bottom, 8)

            VStack(spacing: 0) {
                content
            }
            .background(Color.paperWarm.opacity(0.55))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color.inkMuted.opacity(0.15), lineWidth: 1)
            )
            .padding(.horizontal, 18)
        }
        .padding(.bottom, 24)
    }
}

// ── Reusable Row ──
struct SettingsRow: View {
    let icon: String
    let iconColor: Color
    let title: String
    let subtitle: String?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(iconColor.opacity(0.15))
                        .frame(width: 36, height: 36)
                    Image(systemName: icon)
                        .font(.system(size: 17))
                        .foregroundColor(iconColor)
                }

                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(.system(size: 17, design: .serif))
                        .foregroundColor(Color.inkDark)
                    if let sub = subtitle {
                        Text(sub)
                            .font(.system(size: 13, design: .serif))
                            .foregroundColor(Color.inkMuted)
                    }
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(Color.inkMuted.opacity(0.4))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
        }
        .buttonStyle(PlainButtonStyle())
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(Color.inkMuted.opacity(0.10))
                .frame(height: 1)
                .padding(.leading, 64)
        }
    }
}

// TODO: IAP - Uncomment when ready to implement
// struct PaywallSheet: View { ... }

#Preview {
    SettingsView()
}
