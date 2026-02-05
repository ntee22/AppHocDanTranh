# 🎵 Dan Thanh Guzheng - iOS Learning App

<p align="center">
  <img src="https://img.shields.io/badge/Platform-iOS%2016.0%2B-blue.svg" alt="Platform">
  <img src="https://img.shields.io/badge/Swift-5.0-orange.svg" alt="Swift">
  <img src="https://img.shields.io/badge/SwiftUI-✓-green.svg" alt="SwiftUI">
  <img src="https://img.shields.io/badge/Languages-EN%20%7C%20VI-red.svg" alt="Languages">
  <img src="https://img.shields.io/badge/License-All%20Rights%20Reserved-lightgrey.svg" alt="License">
</p>

A beautiful, zen-inspired iOS app for learning the traditional Chinese instrument Guzheng (古筝 / Đàn Tranh). Built with SwiftUI and featuring a premium, minimalist design.

---

## ✨ Features

### 📚 **Comprehensive Learning System**
- **12 professionally structured lessons** (5 free, 7 premium)
- From beginner basics to advanced techniques
- High-quality video instruction with custom player
- Progress tracking and practice tips

### 🔒 **Secure Video Playback**
- Custom AVPlayer with anti-leak protection
- **Dynamic watermark** that moves periodically
- Disabled AirPlay and external playback
- Limited buffering for content protection
- Custom playback controls

### 🌐 **Bilingual Support**
- **Full Vietnamese (Tiếng Việt) localization** 🇻🇳
- English language support 🇺🇸
- Seamless language switching
- Localized lesson content

### 💰 **In-App Purchase**
- StoreKit 2 integration ready
- Beautiful purchase flow with benefits display
- Restore purchases capability
- Lifetime access model

### 🎨 **Premium UI/UX**
- **Zen-inspired** minimalist design
- Smooth animations and transitions
- Follows Apple's Human Interface Guidelines
- Accessible and responsive layout

---

## 📱 Screenshots

*Coming soon - Screenshots of the app in both English and Vietnamese*

---

## 🎨 Design Philosophy

### Visual Identity
- **Primary Color**: Soft Teal (#78A083) - tranquility and nature
- **Secondary Color**: Warm Wood Brown (#8B4513) - the Guzheng instrument
- **Background**: Off-white/Cream (#FDFCF0) - peaceful, paper-like feel
- **Typography**: Serif for titles (traditional), Sans-serif for body (modern)

### User Experience
- Zen & minimalist aesthetic
- Clear visual hierarchy
- Intuitive navigation
- Premium feel throughout

---

## 🏗️ Architecture

### MVVM Pattern (Clean Architecture)

```
HocDanTranh-Guzheng-DanThanh/
│
├── DanThanhGuzhengApp.swift          # App entry point
│
├── Models/
│   └── Lesson.swift                   # Lesson data model + 12 mock lessons
│
├── Views/
│   ├── MainTabView.swift              # Tab navigation (Lessons/Tuner/Sheet Music)
│   ├── HomeView.swift                 # Lesson list with hero section
│   ├── LessonDetailView.swift         # Video player + lesson description
│   └── Components/
│       ├── LessonCard.swift           # Beautiful lesson card
│       ├── LockBadge.swift            # Animated lock indicator
│       ├── SecureVideoPlayer.swift    # Custom secure video player
│       └── PurchaseBottomSheet.swift  # Purchase prompt sheet
│
├── ViewModels/
│   └── LessonViewModel.swift          # Business logic + state management
│
├── Managers/
│   ├── VideoManager.swift             # Cloudflare Stream integration
│   └── PaymentManager.swift           # StoreKit 2 purchases
│
├── Utilities/
│   ├── AppColors.swift                # Color system
│   ├── AppFonts.swift                 # Typography system
│   └── Localization.swift             # L10n helper
│
└── Resources/
    ├── en.lproj/
    │   └── Localizable.strings        # English translations
    └── vi.lproj/
        └── Localizable.strings        # Vietnamese translations
```

---

## 🚀 Getting Started

### Prerequisites

- **Xcode 15.0+**
- **iOS 16.0+**
- **macOS Monterey or later**
- Apple Developer account (for device testing)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/ntee22/AppHocDanTranh.git
   cd AppHocDanTranh
   ```

2. **Open in Xcode**
   ```bash
   open HocDanTranh-Guzheng-DanThanh/HocDanTranh-Guzheng-DanThanh.xcodeproj
   ```

3. **Configure Signing**
   - Select your target
   - Go to "Signing & Capabilities"
   - Select your Team / Apple ID
   - Xcode will auto-generate signing certificate

4. **Build and Run**
   - Select your device or simulator
   - Press `Cmd + R`
   - App launches! 🎉

---

## 🌐 Language Support

The app supports both English and Vietnamese:

| Language | Code | Status |
|----------|------|--------|
| English | `en` | ✅ Complete |
| Vietnamese (Tiếng Việt) | `vi` | ✅ Complete |

### Testing Different Languages

**Method 1: Change Device Language**
1. Settings → General → Language & Region
2. iPhone Language → Select "Tiếng Việt"
3. Restart app

**Method 2: Xcode Scheme**
1. Edit Scheme → Run → Options
2. App Language → Select "Vietnamese"
3. Run app

See `LOCALIZATION_GUIDE.md` for detailed instructions.

---

## 📚 Documentation

- **[README.md](README.md)** - Project overview (this file)
- **[PROJECT_SETUP.md](HocDanTranh-Guzheng-DanThanh/HocDanTranh-Guzheng-DanThanh/PROJECT_SETUP.md)** - Complete Xcode setup guide
- **[DESIGN_SYSTEM.md](HocDanTranh-Guzheng-DanThanh/HocDanTranh-Guzheng-DanThanh/DESIGN_SYSTEM.md)** - UI/UX design specifications
- **[APP_OVERVIEW.md](HocDanTranh-Guzheng-DanThanh/HocDanTranh-Guzheng-DanThanh/APP_OVERVIEW.md)** - Feature breakdown & user flows
- **[LOCALIZATION_GUIDE.md](HocDanTranh-Guzheng-DanThanh/HocDanTranh-Guzheng-DanThanh/LOCALIZATION_GUIDE.md)** - Vietnamese localization setup

---

## 🎯 Lesson Content

### Free Lessons (1-5) 🎁
1. **Introduction to Guzheng** - History and parts of the instrument (8:45)
2. **Proper Hand Position** - Foundation of beautiful sound (10:20)
3. **Basic Plucking Techniques** - 托 (Tuō) and 劈 (Pī) techniques (12:15)
4. **Reading Guzheng Notation** - Traditional numbered notation (9:30)
5. **Your First Melody** - Play "Little Star" (小星星) (11:00)

### Premium Lessons (6-12) 🔒
6. **Tremolo Technique (摇指)** - Flowing tremolo sound (14:30)
7. **Glissando & Slides** - Expression with sliding techniques (13:45)
8. **Playing Traditional Melodies** - "Fisherman's Song at Dusk" (16:20)
9. **Advanced Right Hand Techniques** - Combinations and fast passages (15:10)
10. **Left Hand Expression** - Vibrato (揉弦) and bending (14:00)
11. **Performance Practice** - Stage presence and musicality (12:30)
12. **Final Recital Piece** - "High Mountains, Flowing Water" (18:45)

---

## 🔧 Configuration

### For Production Use

#### 1. Cloudflare Stream Integration

```swift
// In VideoManager.swift
func fetchSignedVideoURL(for videoID: String) async throws -> URL {
    // Replace with your backend API
    let endpoint = "https://your-api.com/video/\(videoID)/signed-url"
    let (data, _) = try await URLSession.shared.data(from: URL(string: endpoint)!)
    let response = try JSONDecoder().decode(SignedURLResponse.self, from: data)
    return response.signedUrl
}
```

#### 2. StoreKit Configuration

1. Create in-app purchase in App Store Connect
2. Product ID: `com.danthanh.guzheng.3month`
3. Uncomment StoreKit code in `PaymentManager.swift`
4. Test with sandbox accounts

#### 3. App Bundle Identifier

Update in Xcode:
- Target → General → Bundle Identifier
- Use your own identifier (e.g., `com.yourcompany.danthanh`)

---

## 🛠️ Tech Stack

- **Language**: Swift 5.0
- **UI Framework**: SwiftUI
- **Architecture**: MVVM (Model-View-ViewModel)
- **Video**: AVFoundation
- **Purchases**: StoreKit 2
- **Localization**: NSLocalizedString
- **Minimum iOS**: 16.0+

---

## 🎨 Key Technologies

- **SwiftUI** - Modern declarative UI
- **Combine** - Reactive programming for ViewModels
- **AVPlayer** - Custom video playback
- **StoreKit 2** - Modern in-app purchases
- **NavigationStack** - iOS 16+ navigation
- **@StateObject** - State management
- **async/await** - Modern concurrency

---

## 📦 Current Status

### ✅ Complete
- [x] Full UI/UX implementation
- [x] MVVM architecture
- [x] 12 mock lessons with metadata
- [x] Lesson browsing and navigation
- [x] Lock/unlock logic
- [x] Purchase sheet UI
- [x] Custom video player structure
- [x] Dynamic watermark animation
- [x] Color and font systems
- [x] English localization
- [x] Vietnamese localization

### 🚧 To Complete
- [ ] Cloudflare Stream integration (needs backend)
- [ ] StoreKit 2 purchase flow (needs App Store Connect)
- [ ] Real video content (needs video uploads)
- [ ] Tuner functionality
- [ ] Sheet Music library
- [ ] App icon and launch screen

---

## 🚀 Future Enhancements

- [ ] Progress tracking dashboard
- [ ] Offline video downloads
- [ ] Practice reminders/notifications
- [ ] Certificate of completion
- [ ] Social sharing features
- [ ] Live Q&A sessions
- [ ] Advanced course tiers
- [ ] Chinese (Simplified/Traditional) localization
- [ ] Dark mode support
- [ ] iPad optimization

---

## 🤝 Contributing

This is a private project. If you have suggestions or find bugs, please contact the project owner.

---

## 📄 License

Copyright © 2024-2026 Dan Thanh. All rights reserved.

This is proprietary software. Unauthorized copying, modification, distribution, or use of this software is strictly prohibited.

---

## 👤 Author

**Dan Thanh**
- GitHub: [@ntee22](https://github.com/ntee22)
- Repository: [AppHocDanTranh](https://github.com/ntee22/AppHocDanTranh)

---

## 🙏 Acknowledgments

- Traditional Guzheng music heritage
- Apple's Human Interface Guidelines
- SwiftUI community
- Vietnamese Guzheng learning community

---

## 📞 Support

For questions or support:
- Open an issue on GitHub
- Contact the development team

---

<p align="center">
  Built with ❤️ for Guzheng students worldwide 🎋
</p>

<p align="center">
  <strong>Được xây dựng với ❤️ dành cho học viên Đàn Tranh trên toàn thế giới 🎋</strong>
</p>
