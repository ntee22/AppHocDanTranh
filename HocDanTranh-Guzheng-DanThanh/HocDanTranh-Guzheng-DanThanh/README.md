# Dan Thanh Guzheng - iOS Learning App

A beautiful, zen-inspired iOS app for learning the traditional Chinese instrument Guzheng (古筝).

## Overview

Dan Thanh Guzheng provides a premium learning experience with:
- 12 comprehensive lessons (5 free, 7 premium)
- High-quality video instruction
- Anti-leak video player with dynamic watermarks
- Clean, minimalist UI following Apple's Human Interface Guidelines

## Design Philosophy

### Visual Identity
- **Primary Color**: Soft Teal (#78A083) - representing tranquility and nature
- **Secondary Color**: Warm Wood Brown (#8B4513) - representing the Guzheng instrument
- **Background**: Off-white/Cream (#FDFCF0) - creating a peaceful, paper-like feel
- **Typography**: Serif fonts for titles (traditional feel), Sans-serif for body text (readability)

### User Experience
- Zen & minimalist aesthetic
- Smooth animations and transitions
- Clear visual hierarchy
- Intuitive navigation

## Architecture

### MVVM Pattern
The app follows the Model-View-ViewModel architecture:

```
├── Models/
│   └── Lesson.swift                  # Data models
├── Views/
│   ├── HomeView.swift                # Main lesson list
│   ├── LessonDetailView.swift        # Individual lesson view
│   ├── MainTabView.swift             # Tab navigation
│   └── Components/
│       ├── LessonCard.swift          # Lesson card component
│       ├── LockBadge.swift           # Lock indicator
│       ├── SecureVideoPlayer.swift   # Custom video player
│       └── PurchaseBottomSheet.swift # Purchase prompt
├── ViewModels/
│   └── LessonViewModel.swift         # Business logic
├── Managers/
│   ├── VideoManager.swift            # Video URL fetching
│   └── PaymentManager.swift          # StoreKit integration
└── Utilities/
    ├── AppColors.swift               # Color palette
    └── AppFonts.swift                # Typography system
```

## Features

### 1. Lesson Management
- 12 mock lessons with realistic metadata
- 5 free lessons (1-5)
- 7 locked premium lessons (6-12)
- Elegant lesson cards with duration, status badges

### 2. Secure Video Playback
- Custom AVPlayer wrapper
- Dynamic watermark overlay that moves periodically
- Disabled external playback (no AirPlay)
- Limited forward buffering
- Custom playback controls

### 3. In-App Purchase
- StoreKit 2 integration (stub implementation)
- Beautiful bottom sheet explaining benefits
- Purchase restoration capability
- Persistent purchase state

### 4. Additional Features (Placeholders)
- Tuner tab for instrument tuning
- Sheet Music library tab

## Technical Requirements

- **Platform**: iOS 16.0+
- **Framework**: SwiftUI
- **Architecture**: MVVM
- **Dependencies**: AVFoundation, StoreKit 2

## Setup Instructions

1. Open the project in Xcode
2. Add your App Bundle Identifier
3. Configure StoreKit products in App Store Connect
4. Update `courseProductID` in `PaymentManager.swift`
5. Implement actual Cloudflare Stream integration in `VideoManager.swift`
6. Run on device or simulator

## Cloudflare Integration (TODO)

To connect real videos:

1. Upload videos to Cloudflare Stream
2. Create a backend API to generate signed URLs
3. Update `fetchSignedVideoURL()` in `VideoManager.swift`:

```swift
func fetchSignedVideoURL(for videoID: String) async throws -> URL {
    // Call your backend API
    let response = try await yourBackendAPI.getSignedURL(videoID: videoID)
    return response.signedURL
}
```

## StoreKit Configuration (TODO)

1. Create in-app purchase product in App Store Connect
2. Configure product ID: `com.danthanh.guzheng.3month`
3. Uncomment StoreKit code in `PaymentManager.swift`
4. Test with sandbox accounts

## Code Style

- Clear, descriptive naming
- Comprehensive comments
- Separated concerns (MVVM)
- Reusable components
- Type-safe color and font systems

## Future Enhancements

- [ ] Implement real Cloudflare integration
- [ ] Complete StoreKit 2 purchase flow
- [ ] Add progress tracking
- [ ] Implement Tuner functionality
- [ ] Build Sheet Music library
- [ ] Add practice reminders
- [ ] Implement social sharing
- [ ] Add offline video downloads

## License

Copyright © 2024 Dan Thanh. All rights reserved.

---

Built with ❤️ for Guzheng students worldwide.
