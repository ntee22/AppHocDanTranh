# Project Setup Guide

## Creating the Xcode Project

Since this is a SwiftUI-only app, follow these steps to set up the Xcode project:

### 1. Create New Xcode Project

1. Open Xcode
2. Select **File > New > Project**
3. Choose **iOS > App**
4. Configure:
   - **Product Name**: Dan Thanh Guzheng
   - **Team**: Your development team
   - **Organization Identifier**: com.danthanh (or your identifier)
   - **Bundle Identifier**: com.danthanh.guzheng
   - **Interface**: SwiftUI
   - **Language**: Swift
   - **Storage**: None (we're not using Core Data)
   - **Include Tests**: Optional

### 2. Add Files to Project

After creating the project, you'll need to organize the existing Swift files:

1. In Finder, navigate to your project folder
2. In Xcode, right-click on the project root
3. Select **Add Files to "Dan Thanh Guzheng"**
4. Add all the folders we created:
   - Models/
   - Views/
   - ViewModels/
   - Managers/
   - Utilities/

### 3. Configure Info.plist

Add these keys to your Info.plist (or in Target > Info):

```xml
<!-- Prevent screen recording (basic protection) -->
<key>UIApplicationSupportsIndirectInputEvents</key>
<true/>

<!-- Disable AirPlay mirroring -->
<key>UIApplicationExitsOnSuspend</key>
<false/>

<!-- Supported orientations (portrait only for better UX) -->
<key>UISupportedInterfaceOrientations</key>
<array>
    <string>UIInterfaceOrientationPortrait</string>
</array>
```

### 4. Configure Capabilities

In Xcode:
1. Select your project target
2. Go to **Signing & Capabilities**
3. Add **In-App Purchase** capability

### 5. Configure App Store Connect

#### Create In-App Purchase Product:
1. Go to [App Store Connect](https://appstoreconnect.apple.com)
2. Select your app
3. Go to **Features > In-App Purchases**
4. Click **+** to create new product
5. Select **Non-Consumable**
6. Configure:
   - **Product ID**: `com.danthanh.guzheng.3month`
   - **Reference Name**: "3-Month Guzheng Course"
   - **Price**: Your desired price (e.g., $49.99)
   - Add localizations with:
     - **Display Name**: "3-Month Guzheng Course"
     - **Description**: "Full access to all 12 comprehensive Guzheng lessons with lifetime access"

### 6. Configure StoreKit Configuration File (for Testing)

1. In Xcode: **File > New > File**
2. Select **StoreKit Configuration File**
3. Name it `Products.storekit`
4. Add a product:
   - **Type**: Non-Consumable
   - **Reference Name**: 3-Month Guzheng Course
   - **Product ID**: com.danthanh.guzheng.3month
   - **Price**: Your price

5. In scheme settings, select this StoreKit configuration file

### 7. Cloudflare Stream Setup (Backend Required)

You'll need a backend service to generate signed URLs:

#### Backend Endpoint Example (Node.js):
```javascript
// Example using Cloudflare Stream API
app.get('/api/video/:videoId/signed-url', async (req, res) => {
  const { videoId } = req.params;
  
  // Verify user has access (check purchase status)
  
  // Generate signed URL from Cloudflare
  const signedUrl = await generateCloudflareSignedUrl(videoId);
  
  res.json({ signedUrl, expiresAt: Date.now() + 3600000 }); // 1 hour expiry
});
```

Update `VideoManager.swift` to call your backend:
```swift
func fetchSignedVideoURL(for videoID: String) async throws -> URL {
    let endpoint = "https://yourapi.com/api/video/\(videoID)/signed-url"
    let (data, _) = try await URLSession.shared.data(from: URL(string: endpoint)!)
    let response = try JSONDecoder().decode(SignedURLResponse.self, from: data)
    return response.signedUrl
}
```

### 8. Build Settings

Recommended settings:
- **Minimum iOS Version**: iOS 16.0
- **Swift Language Version**: Swift 5
- **Optimization Level (Debug)**: No Optimization
- **Optimization Level (Release)**: Optimize for Speed

### 9. App Icon & Launch Screen

1. Add app icon to Assets.xcassets:
   - Create an icon with Guzheng imagery
   - Use colors from our palette (Teal/Brown/Cream)
   - Add all required sizes

2. Create Launch Screen:
   - Simple logo centered on cream background
   - Add "Dan Thanh Guzheng" text below

### 10. Testing Checklist

- [ ] Free lessons (1-5) are accessible without purchase
- [ ] Locked lessons (6-12) show purchase sheet
- [ ] Video player displays watermark
- [ ] Watermark moves periodically
- [ ] Purchase flow completes successfully
- [ ] Purchase state persists after app restart
- [ ] Restore purchase works correctly
- [ ] All UI elements match design system
- [ ] App handles network errors gracefully
- [ ] Video player handles loading states

### 11. Project Structure in Xcode

Your project navigator should look like:

```
Dan Thanh Guzheng/
├── DanThanhGuzhengApp.swift
├── Models/
│   └── Lesson.swift
├── Views/
│   ├── MainTabView.swift
│   ├── HomeView.swift
│   ├── LessonDetailView.swift
│   └── Components/
│       ├── LessonCard.swift
│       ├── LockBadge.swift
│       ├── SecureVideoPlayer.swift
│       └── PurchaseBottomSheet.swift
├── ViewModels/
│   └── LessonViewModel.swift
├── Managers/
│   ├── VideoManager.swift
│   └── PaymentManager.swift
├── Utilities/
│   ├── AppColors.swift
│   └── AppFonts.swift
└── Assets.xcassets
```

## Running the App

1. Select a simulator or connected device
2. Press **Cmd + R** to build and run
3. You should see the beautiful home screen with 12 lessons
4. Free lessons should be accessible
5. Tapping locked lessons shows the purchase sheet

## Next Steps

1. ✅ Add real video content to Cloudflare Stream
2. ✅ Implement backend API for signed URLs
3. ✅ Configure App Store Connect
4. ✅ Test purchase flow with sandbox accounts
5. ✅ Prepare App Store screenshots
6. ✅ Write App Store description
7. ✅ Submit for review

## Support

For issues or questions, refer to:
- [Apple Developer Documentation](https://developer.apple.com/documentation/)
- [StoreKit 2 Guide](https://developer.apple.com/documentation/storekit)
- [Cloudflare Stream Docs](https://developers.cloudflare.com/stream/)
