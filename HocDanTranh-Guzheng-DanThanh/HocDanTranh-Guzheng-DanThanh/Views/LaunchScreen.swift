//
//  LaunchScreen.swift
//  Dan Thanh Guzheng
//
//  Paper scroll unroll loading screen matching the vintage design
//

import SwiftUI

// MARK: - Launch Screen

struct LaunchScreen: View {
    @State private var progress: Double = 0
    @State private var dotPhase: Double = 0
    @State private var showContent = false
    @State private var showText = false

    var body: some View {
        ZStack {
            // Dark wooden background
            Color(hex: "2A2520").ignoresSafeArea()

            // Ambient warm glow from center
            RadialGradient(
                colors: [
                    Color(hex: "C9A961").opacity(0.12),
                    Color(hex: "2A2520").opacity(0)
                ],
                center: .center,
                startRadius: 0,
                endRadius: 300
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                Spacer()

                // Scroll container — rollers + paper
                ZStack {
                    // Left roller: tracks the paper's left edge outward
                    ScrollRollerView(side: .left)
                        .frame(width: 18, height: 380)
                        .offset(x: -(160 * progress + 9))

                    // Right roller: tracks the paper's right edge outward
                    ScrollRollerView(side: .right)
                        .frame(width: 18, height: 380)
                        .offset(x: 160 * progress + 9)

                    // Paper: shadow behind, content clipped inside
                    ZStack(alignment: .center) {
                        // Paper background with shadow (not clipped so shadow shows)
                        Rectangle()
                            .fill(Color(hex: "F2E8D5"))
                            .frame(width: max(1, 320 * progress), height: 380)
                            .shadow(color: Color.black.opacity(0.35), radius: 30)

                        // Clipped zone for edges + content
                        ZStack {
                            // Top/bottom paper edge shading
                            VStack(spacing: 0) {
                                LinearGradient(
                                    colors: [Color(hex: "8B6F4E").opacity(0.25), .clear],
                                    startPoint: .top, endPoint: .bottom
                                )
                                .frame(height: 4)
                                Spacer()
                                LinearGradient(
                                    colors: [.clear, Color(hex: "8B6F4E").opacity(0.25)],
                                    startPoint: .top, endPoint: .bottom
                                )
                                .frame(height: 4)
                            }

                            // Main content — fades in at 55% (1.43s), text at 75% (1.95s)
                            VStack(spacing: 10) {
                                Text("琴")
                                    .font(.system(size: 96, design: .serif))
                                    .foregroundColor(Color(hex: "3A3228"))
                                    .shadow(color: Color.white.opacity(0.3),
                                            radius: 0, x: 0, y: 1)

                                BrushDivider()
                                    .frame(width: 100)

                                Text("Đan Thanh")
                                    .font(.system(size: 28, weight: .medium, design: .serif))
                                    .italic()
                                    .foregroundColor(Color(hex: "3A3228"))
                                    .opacity(showText ? 1 : 0)
                                    .animation(.easeOut(duration: 0.42), value: showText)

                                Text("HỌC ĐÀN TRANH · GUZHENG")
                                    .font(.system(size: 10, weight: .semibold))
                                    .kerning(3)
                                    .foregroundColor(Color(hex: "8A7B66"))
                                    .opacity(showText ? 1 : 0)
                                    .animation(.easeOut(duration: 0.42), value: showText)

                                InkSeal(char: "樂", size: 24, rotate: -4)
                                    .opacity(showText ? 1 : 0)
                                    .animation(.easeOut(duration: 0.42), value: showText)
                            }
                            .opacity(showContent ? 1 : 0)
                            .offset(y: showContent ? 0 : 8)
                            .animation(.easeOut(duration: 0.42), value: showContent)
                        }
                        .frame(width: max(1, 320 * progress), height: 380)
                        .clipped()
                    }
                }
                .frame(width: 320, height: 380)

                Spacer()

                // Progress dots — breathe independently from the scroll animation
                HStack(spacing: 6) {
                    ForEach(0..<3, id: \.self) { i in
                        Circle()
                            .fill(Color(hex: "C9A961"))
                            .frame(width: 5, height: 5)
                            .opacity(0.6)
                            .scaleEffect(
                                1.0 + 0.3 * sin((dotPhase + Double(i) * 0.333) * 2 * .pi)
                            )
                    }
                }
                .padding(.bottom, 60)
            }
        }
        .onAppear {
            // Main scroll unroll: 2.6 seconds
            withAnimation(.easeInOut(duration: 2.6)) {
                progress = 1.0
            }

            // Dot breathing loop
            withAnimation(.linear(duration: 1.2).repeatForever(autoreverses: false)) {
                dotPhase = 1.0
            }

            // Logo fades in at 55% of 2.6s = 1.43s
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.43) {
                showContent = true
            }

            // Text fades in at 75% of 2.6s = 1.95s
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.95) {
                showText = true
            }
        }
    }
}

// MARK: - Wooden Roller

private struct ScrollRollerView: View {
    enum Side { case left, right }
    let side: Side

    var body: some View {
        ZStack {
            // Wood cylinder gradient (light source from left)
            LinearGradient(
                colors: [
                    Color(hex: "6B5538"),
                    Color(hex: "8B6F4E"),
                    Color(hex: "A88860"),
                    Color(hex: "8B6F4E"),
                    Color(hex: side == .left ? "5A4428" : "6B5538")
                ],
                startPoint: side == .left ? .leading : .trailing,
                endPoint: side == .left ? .trailing : .leading
            )
            .clipShape(Capsule())
            .shadow(color: Color.black.opacity(0.4), radius: 10, x: 0, y: 3)
            .overlay(Capsule().stroke(Color(hex: "C9A961").opacity(0.2), lineWidth: 1))

            // Top cap (gold trim)
            RoundedRectangle(cornerRadius: 3)
                .fill(LinearGradient(
                    colors: [Color(hex: "C9A961"), Color(hex: "8B6F4E")],
                    startPoint: .topLeading, endPoint: .bottomTrailing
                ))
                .frame(height: 10)
                .padding(.horizontal, -3)
                .offset(y: -185)
                .shadow(color: Color.black.opacity(0.4), radius: 4, y: 2)

            // Bottom cap (gold trim)
            RoundedRectangle(cornerRadius: 3)
                .fill(LinearGradient(
                    colors: [Color(hex: "C9A961"), Color(hex: "8B6F4E")],
                    startPoint: .topLeading, endPoint: .bottomTrailing
                ))
                .frame(height: 10)
                .padding(.horizontal, -3)
                .offset(y: 185)
                .shadow(color: Color.black.opacity(0.4), radius: 4, y: 2)
        }
    }
}

// MARK: - Preview

#Preview {
    LaunchScreen()
}
