//
//  SecureVideoPlayer.swift
//  Dan Thanh Guzheng
//
//  Secure AVPlayer-based video player with watermark and portrait orientation support
//

import SwiftUI
import AVKit
import Combine

/// Secure video player (Cloudflare/AVPlayer) with dynamic watermark overlay.
/// For YouTube free videos use `YouTubePlayerView` instead.
struct SecureVideoPlayer: View {

    let videoID: String
    // TODO: When IAP launches, replace with non-PII identifier (transaction ID or hashed user ID)
    let watermarkEmail: String
    /// `true` if the video was recorded in portrait (9:16). Determines aspect ratio.
    var isPortrait: Bool = true

    @StateObject private var playerManager = SecurePlayerManager()
    @State private var watermarkPosition: CGPoint = .zero

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                // Video Player
                if let player = playerManager.player {
                    VideoPlayer(player: player)
                        .onAppear {
                            initializeWatermarkPosition(in: geometry.size)
                            startWatermarkAnimation()
                        }
                } else if playerManager.isLoading {
                    loadingView
                } else if playerManager.errorMessage != nil {
                    errorView
                }

                // Dynamic Watermark Overlay
                if playerManager.player != nil {
                    watermarkOverlay
                        .position(watermarkPosition)
                        .allowsHitTesting(false)
                }

                // Custom Controls
                if playerManager.player != nil {
                    customControls
                }
            }
            .background(Color.black)
        }
        .task {
            await playerManager.loadVideo(videoID: videoID)
        }
    }

    // MARK: - Watermark Overlay

    private var watermarkOverlay: some View {
        Text(watermarkEmail)
            .font(.system(size: 12, weight: .medium, design: .monospaced))
            .foregroundColor(.white.opacity(0.4))
            .padding(8)
            .background(
                RoundedRectangle(cornerRadius: 6)
                    .fill(Color.black.opacity(0.3))
            )
            .shadow(color: .black.opacity(0.5), radius: 2)
    }

    // MARK: - Custom Controls

    private var customControls: some View {
        VStack {
            Spacer()

            HStack(spacing: 24) {
                Button {
                    playerManager.togglePlayPause()
                } label: {
                    Image(systemName: playerManager.isPlaying ? "pause.fill" : "play.fill")
                        .font(.title2)
                        .foregroundColor(.white)
                        .frame(width: 44, height: 44)
                        .background(Circle().fill(Color.primaryTeal))
                }

                Spacer()
            }
            .padding()
        }
    }

    // MARK: - Loading View

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .tint(.primaryTeal)
                .scaleEffect(1.5)

            Text(L10n.loadingLesson)
                .font(.caption)
                .foregroundColor(.white)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.black)
    }

    // MARK: - Error View

    private var errorView: some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 40))
                .foregroundColor(.red.opacity(0.8))

            Text(playerManager.errorMessage ?? L10n.failedToLoadVideo)
                .font(.caption)
                .foregroundColor(.white)
                .multilineTextAlignment(.center)

            Button(L10n.retry) {
                Task {
                    await playerManager.loadVideo(videoID: videoID)
                }
            }
            .font(.buttonText)
            .foregroundColor(.white)
            .padding(.horizontal, 20)
            .padding(.vertical, 10)
            .background(Capsule().fill(Color.primaryTeal))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.black)
    }

    // MARK: - Watermark Animation

    private func initializeWatermarkPosition(in size: CGSize) {
        watermarkPosition = CGPoint(x: size.width - 100, y: 40)
    }

    private func startWatermarkAnimation() {
        Timer.scheduledTimer(withTimeInterval: 8.0, repeats: true) { _ in
            guard let player = playerManager.player,
                  player.timeControlStatus == .playing else { return }

            withAnimation(.easeInOut(duration: 3.0)) {
                let w = UIScreen.main.bounds.width
                let h = UIScreen.main.bounds.height
                let positions: [CGPoint] = [
                    CGPoint(x: 100, y: 40),
                    CGPoint(x: w - 100, y: 40),
                    CGPoint(x: 100, y: h * 0.4),
                    CGPoint(x: w - 100, y: h * 0.4),
                ]
                watermarkPosition = positions.randomElement() ?? watermarkPosition
            }
        }
    }
}

// MARK: - Secure Player Manager

@MainActor
class SecurePlayerManager: ObservableObject {
    @Published var player: AVPlayer?
    @Published var isLoading = false
    @Published var isPlaying = false
    @Published var errorMessage: String?

    func loadVideo(videoID: String) async {
        isLoading = true
        errorMessage = nil

        do {
            let url = try await VideoManager.shared.fetchSignedVideoURL(for: videoID)
            let newPlayer = VideoManager.shared.createSecurePlayer(with: url)
            setupPlayerObserver(newPlayer)
            player = newPlayer
            isLoading = false
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
        }
    }

    func togglePlayPause() {
        guard let player = player else { return }

        if player.timeControlStatus == .playing {
            player.pause()
            isPlaying = false
        } else {
            player.play()
            isPlaying = true
        }
    }

    private func setupPlayerObserver(_ player: AVPlayer) {
        player.addPeriodicTimeObserver(
            forInterval: CMTime(seconds: 0.5, preferredTimescale: 600),
            queue: .main
        ) { [weak self] _ in
            self?.isPlaying = player.timeControlStatus == .playing
        }
    }
}

#Preview {
    SecureVideoPlayer(videoID: "video_001", watermarkEmail: "student@danthanh.com", isPortrait: true)
        .frame(height: 693)
}
