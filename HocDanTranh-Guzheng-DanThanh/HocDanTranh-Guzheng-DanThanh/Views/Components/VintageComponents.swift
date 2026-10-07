//
//  VintageComponents.swift
//  Dan Thanh Guzheng
//
//  Reusable vintage-style UI components matching the design handoff
//

import SwiftUI

// MARK: - InkSeal

/// Red square seal stamp with a Chinese character
struct InkSeal: View {
    var char: String = "琴"
    var size: CGFloat = 32
    var rotate: Double = -4

    var body: some View {
        Text(char)
            .font(.system(size: size * 0.60))
            .foregroundColor(Color(hex: "F5E8D0"))
            .frame(width: size, height: size)
            .background(Color.sealRed)
            .clipShape(RoundedRectangle(cornerRadius: size * 0.12))
            .overlay(
                RoundedRectangle(cornerRadius: size * 0.12)
                    .stroke(Color(hex: "F5E8D0").opacity(0.4), lineWidth: 1.5)
            )
            .rotationEffect(.degrees(rotate))
    }
}

// MARK: - BrushDivider

/// Soft brush-stroke style horizontal divider
struct BrushDivider: View {
    var opacity: Double = 0.35

    var body: some View {
        Rectangle()
            .fill(
                LinearGradient(
                    colors: [
                        .clear,
                        Color.inkMuted.opacity(0.5),
                        Color.inkSoft.opacity(0.6),
                        Color.inkMuted.opacity(0.5),
                        .clear
                    ],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .frame(height: 1.2)
            .opacity(opacity)
    }
}

// MARK: - PlumBranch

/// Decorative plum blossom corner ornament drawn with SwiftUI shapes
struct PlumBranch: View {
    var size: CGFloat = 120
    var color: Color = Color.inkMuted

    private let blossomPositions: [(CGFloat, CGFloat, CGFloat)] = [
        (0.175, 0.80, 2.4),
        (0.375, 0.625, 2.1),
        (0.525, 0.525, 2.6),
        (0.675, 0.40, 2.2),
        (0.825, 0.25, 2.5),
        (0.90, 0.125, 1.8),
    ]

    var body: some View {
        Canvas { ctx, sz in
            let w = sz.width
            let h = sz.height

            // Main branch
            var branch = Path()
            branch.move(to: CGPoint(x: w * 0.075, y: h * 0.90))
            branch.addQuadCurve(
                to: CGPoint(x: w * 0.40, y: h * 0.60),
                control: CGPoint(x: w * 0.25, y: h * 0.69)
            )
            branch.addQuadCurve(
                to: CGPoint(x: w * 0.725, y: h * 0.35),
                control: CGPoint(x: w * 0.575, y: h * 0.50)
            )
            branch.addQuadCurve(
                to: CGPoint(x: w * 0.925, y: h * 0.10),
                control: CGPoint(x: w * 0.85, y: h * 0.225)
            )

            ctx.stroke(branch,
                       with: .color(color.opacity(0.45)),
                       style: StrokeStyle(lineWidth: 1.2, lineCap: .round))

            // Blossoms
            for (xf, yf, r) in blossomPositions {
                let center = CGPoint(x: w * xf, y: h * yf)
                let outer = Path(ellipseIn: CGRect(
                    x: center.x - r, y: center.y - r,
                    width: r * 2, height: r * 2
                ))
                ctx.fill(outer, with: .color(Color.sealRed.opacity(0.75)))
                let inner = Path(ellipseIn: CGRect(
                    x: center.x - r * 0.4, y: center.y - r * 0.4,
                    width: r * 0.8, height: r * 0.8
                ))
                ctx.fill(inner, with: .color(Color(hex: "F5E8D0").opacity(0.70)))
            }
        }
        .frame(width: size, height: size)
    }
}

// MARK: - LessonThumb

enum LessonAccent {
    case sage, wood, plum

    var gradientColors: [Color] {
        switch self {
        case .sage:  return [Color.sageSoft, Color.sageDark]
        case .wood:  return [Color.woodSoft, Color.wood]
        case .plum:  return [Color.plumSoft, Color.plum]
        }
    }
}

// MARK: - Readable width

extension View {
    /// Caps a page at a comfortable reading width and centers it, so rows and
    /// cards don't stretch edge to edge on wide displays (iPhone Duo inner
    /// screen, iPad). No effect on regular iPhones, which are narrower.
    func readableContentWidth(_ maxWidth: CGFloat = 700) -> some View {
        frame(maxWidth: maxWidth)
            .frame(maxWidth: .infinity)
    }
}

/// Magazine-style lesson thumbnail with number and optional lock overlay
struct LessonThumb: View {
    var number: String
    var titleEn: String
    var locked: Bool = false
    var accent: LessonAccent = .sage

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height

            ZStack(alignment: .topLeading) {
                // Background gradient
                LinearGradient(
                    colors: accent.gradientColors,
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )

                // Wood-grain strokes
                Canvas { ctx, sz in
                    for i in 0..<6 {
                        var path = Path()
                        let y = sz.height * (0.20 + Double(i) * 0.14)
                        path.move(to: CGPoint(x: 0, y: y))
                        path.addQuadCurve(
                            to: CGPoint(x: sz.width, y: y + sz.height * 0.02),
                            control: CGPoint(x: sz.width * 0.5, y: y - sz.height * 0.01)
                        )
                        ctx.stroke(path,
                                   with: .color(.white.opacity(0.4)),
                                   style: StrokeStyle(lineWidth: 0.8))
                    }
                }
                .opacity(0.25)

                // Lesson number
                if !number.isEmpty {
                    Text(number)
                        .font(.system(size: w * 0.36, weight: .medium, design: .serif))
                        .italic()
                        .foregroundColor(.white.opacity(0.85))
                        .padding(.top, h * 0.10)
                        .padding(.leading, w * 0.10)
                }

                // Title at bottom
                if !titleEn.isEmpty {
                    VStack {
                        Spacer()
                        Text(titleEn.uppercased())
                            .font(.system(size: max(8, w * 0.10), weight: .semibold))
                            .foregroundColor(.white.opacity(0.90))
                            .lineLimit(2)
                            .padding(.horizontal, w * 0.10)
                            .padding(.bottom, h * 0.12)
                    }
                }

                // Lock overlay
                if locked {
                    VStack {
                        HStack {
                            Spacer()
                            ZStack {
                                Circle()
                                    .fill(Color.inkDark.opacity(0.55))
                                    .frame(width: 26, height: 26)
                                Image(systemName: "lock.fill")
                                    .font(.system(size: 11, weight: .semibold))
                                    .foregroundColor(.white)
                            }
                            .padding(6)
                        }
                        Spacer()
                    }
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(Color.white.opacity(0.15), lineWidth: 1)
            )
            .shadow(color: Color.inkDark.opacity(0.15), radius: 4, x: 0, y: 2)
        }
        .aspectRatio(16 / 10, contentMode: .fit)
    }
}

// MARK: - VintageButton

struct VintageButton: View {
    enum Style { case primary, wood, ghost }

    var title: String
    var style: Style = .primary
    var action: () -> Void

    private var bg: Color {
        switch style {
        case .primary: return Color.sageDark
        case .wood:    return Color.woodDark
        case .ghost:   return .clear
        }
    }
    private var fg: Color {
        style == .ghost ? Color.inkSoft : Color.paper
    }
    private var border: Color {
        switch style {
        case .primary: return Color.sage
        case .wood:    return Color.wood
        case .ghost:   return Color.inkMuted
        }
    }

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .kerning(0.8)
                .textCase(.uppercase)
                .foregroundColor(fg)
                .padding(.horizontal, 22)
                .padding(.vertical, 11)
                .background(bg)
                .clipShape(Capsule())
                .overlay(Capsule().stroke(border, lineWidth: 1))
                .shadow(color: style == .ghost ? .clear : Color.inkDark.opacity(0.15),
                        radius: 4, x: 0, y: 2)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - PaperBackground

/// Full-screen vintage paper background
struct PaperBackground: View {
    var body: some View {
        ZStack {
            Color.paper

            // Subtle warm vignette corners
            RadialGradient(
                colors: [Color.wood.opacity(0.06), .clear],
                center: .topLeading, startRadius: 0, endRadius: 300
            )
            RadialGradient(
                colors: [Color.sage.opacity(0.05), .clear],
                center: .bottomTrailing, startRadius: 0, endRadius: 300
            )
        }
        .ignoresSafeArea()
    }
}

// MARK: - Overline Label

struct OverlineLabel: View {
    let text: String
    var color: Color = Color.inkMuted

    var body: some View {
        Text(text.uppercased())
            .font(.system(size: 10, weight: .bold))
            .kerning(2.5)
            .foregroundColor(color)
    }
}
