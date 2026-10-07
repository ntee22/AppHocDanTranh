//
//  VideoManager.swift
//  Dan Thanh Guzheng
//
//  Manages video playback and Cloudflare integration
//

import Foundation
import AVFoundation

/// Manager responsible for fetching and preparing video content
class VideoManager {
    
    static let shared = VideoManager()
    
    private init() {}
    
    // MARK: - Public Methods
    
    /// Fetch a signed Cloudflare Stream URL for a video
    /// - Parameter videoID: The unique identifier for the video
    /// - Returns: A signed URL that can be used with AVPlayer
    func fetchSignedVideoURL(for videoID: String) async throws -> URL {
        // TODO: Implement actual Cloudflare API integration
        // For now, return a placeholder URL
        
        // In production, this would:
        // 1. Call your backend API
        // 2. Backend generates a signed URL from Cloudflare Stream
        // 3. Return the signed URL with expiration
        
        // Simulate network call
        try await Task.sleep(nanoseconds: 500_000_000) // 0.5 seconds
        
        // Placeholder - Replace with actual Cloudflare signed URLs
        guard let url = URL(string: "https://customer-\(videoID).cloudflarestream.com/\(videoID)/manifest/video.m3u8") else {
            throw VideoError.invalidURL
        }
        
        return url
    }
    
    /// Create an AVPlayer configured for secure playback
    /// - Parameter url: The video URL
    /// - Returns: Configured AVPlayer instance
    func createSecurePlayer(with url: URL) -> AVPlayer {
        let playerItem = AVPlayerItem(url: url)
        
        // Configure for security
        playerItem.preferredForwardBufferDuration = 2.0 // Limit buffer
        
        let player = AVPlayer(playerItem: playerItem)
        player.allowsExternalPlayback = false // Disable AirPlay
        
        return player
    }
    
    // MARK: - Error Handling
    
    enum VideoError: LocalizedError {
        case invalidURL
        case networkError
        case unauthorized
        
        var errorDescription: String? {
            switch self {
            case .invalidURL:
                return "Invalid video URL"
            case .networkError:
                return "Network connection error"
            case .unauthorized:
                return "You don't have access to this video"
            }
        }
    }
}
