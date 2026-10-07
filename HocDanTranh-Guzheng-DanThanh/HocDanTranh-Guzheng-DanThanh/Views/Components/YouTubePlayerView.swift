//
//  YouTubePlayerView.swift
//  Dan Thanh Guzheng
//
//  Wraps YouTubePlayerKit (SvenTiigi/YouTubePlayerKit 2.0.5).
//  The library fixes YouTube's 2025 web_embedded enforcement (errors 150–153)
//  by setting the correct origin and referrer inside its HTML builder.
//

import SwiftUI
import YouTubePlayerKit

struct LessonVideoPlayer: View {

    let videoID: String

    @StateObject private var player: YouTubePlayer
    @State private var hasTriggeredReview = false
    @State private var videoDuration: Double = 0

    init(videoID: String) {
        self.videoID = videoID
        _player = StateObject(wrappedValue: YouTubePlayer(
            source: .video(id: videoID),
            parameters: .init(showControls: true),
            configuration: .init(allowsInlineMediaPlayback: true)
        ))
    }

    var body: some View {
        YouTubePlayerView(player) { state in
            if case .error = state {
                errorOverlay
            }
        }
        .background(Color.black)
        .onReceive(player.playbackStatePublisher) { state in
            if state == .ended {
                triggerReviewOnce()
            }
        }
        .onReceive(player.durationPublisher) { duration in
            videoDuration = duration.value
        }
        .onReceive(player.currentTimePublisher) { current in
            guard videoDuration > 0 else { return }
            if current.value / videoDuration >= 0.8 {
                triggerReviewOnce()
            }
        }
    }

    private func triggerReviewOnce() {
        guard !hasTriggeredReview else { return }
        hasTriggeredReview = true
        ReviewManager.shared.recordPositiveAction()
    }

    // MARK: - Error overlay

    private var errorOverlay: some View {
        ZStack {
            Color.black
            VStack(spacing: 14) {
                Text("Video không thể phát trong ứng dụng")
                    .font(.system(size: 13))
                    .foregroundColor(Color(white: 0.75))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)

                Button {
                    guard let url = URL(string: "https://www.youtube.com/watch?v=\(videoID)") else { return }
                    UIApplication.shared.open(url)
                } label: {
                    Text("Xem trên YouTube ↗")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 28)
                        .padding(.vertical, 12)
                        .background(Color.red)
                        .clipShape(Capsule())
                }
            }
        }
    }
}

#Preview {
    LessonVideoPlayer(videoID: "B2aIkMOMSYA")
        .frame(width: 390, height: 220)
}
