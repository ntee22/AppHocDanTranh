Thực hiện toàn bộ fix Phase 2 và improvements cho app Học Đàn Tranh, gộp vào build 3 / version 1.0.1.

# MỤC TIÊU TỔNG

8 nhóm việc, làm theo thứ tự dưới đây. Sau mỗi nhóm, verify trước khi sang nhóm tiếp theo. KHÔNG build hoặc archive — Nhut sẽ archive trong Xcode sau khi review code.

---

# NHÓM 1 — Bump Version & Build Number

Trong `project.pbxproj`:
- `CURRENT_PROJECT_VERSION` → đổi từ `2` thành `3`
- `MARKETING_VERSION` → đổi từ `1.0` thành `1.0.1`

---

# NHÓM 2 — Privacy Manifest (PrivacyInfo.xcprivacy)

Tạo file mới: `HocDanTranh-Guzheng-DanThanh/HocDanTranh-Guzheng-DanThanh/PrivacyInfo.xcprivacy`

Nội dung XML:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>NSPrivacyTracking</key>
  <false/>
  <key>NSPrivacyTrackingDomains</key>
  <array/>
  <key>NSPrivacyCollectedDataTypes</key>
  <array/>
  <key>NSPrivacyAccessedAPITypes</key>
  <array>
    <dict>
      <key>NSPrivacyAccessedAPIType</key>
      <string>NSPrivacyAccessedAPICategoryUserDefaults</string>
      <key>NSPrivacyAccessedAPITypeReasons</key>
      <array>
        <string>CA92.1</string>
      </array>
    </dict>
  </array>
</dict>
</plist>
```

Project dùng PBXFileSystemSynchronizedRootGroup → file sẽ tự include trong target. Verify bằng cách check file đã tồn tại đúng path.

---

# NHÓM 3 — Info.plist / Project Build Settings

Project có thể dùng `INFOPLIST_KEY_*` trong project.pbxproj (Xcode 13+ style) hoặc file Info.plist riêng. Check cả 2.

### 3.1. GỠ BỎ 2 keys sau (code không dùng):
- `NSPhotoLibraryUsageDescription` / `INFOPLIST_KEY_NSPhotoLibraryUsageDescription`
- `NSPhotoLibraryAddUsageDescription` / `INFOPLIST_KEY_NSPhotoLibraryAddUsageDescription`

### 3.2. ĐỔI text của NSCameraUsageDescription:

Từ: *"Ứng dụng không sử dụng camera trực tiếp. Quyền này được yêu cầu bởi thư viện bên thứ ba."*

Thành: *"Ứng dụng KHÔNG truy cập camera. Quyền này tồn tại vì thư viện phát hiện cao độ âm thanh có liên kết một API media-capture chung, nhưng tính năng camera không bao giờ được kích hoạt."*

### 3.3. Giữ nguyên `NSMicrophoneUsageDescription`:
*"Ứng dụng cần truy cập microphone để phát hiện cao độ âm thanh, phục vụ tính năng lên dây đàn tranh."*

---

# NHÓM 4 — Sửa SettingsView.swift

### 4.1. Replace placeholder

Trong `SettingsView.swift`, tìm tất cả `idYOUR_APP_ID` và thay bằng `id6767492629`.

Cụ thể line 134, 146, 147 (theo audit report). Verify các URL sau dùng đúng:
- `https://apps.apple.com/app/id6767492629` (link tới app)
- `https://apps.apple.com/app/id6767492629?action=write-review` (link viết review)

### 4.2. Verify nút "Đánh giá ứng dụng"

Đảm bảo có một row trong Settings dùng URL `https://apps.apple.com/app/id6767492629?action=write-review` để mở thẳng form viết review trên App Store. Nếu chưa có, thêm vào:

```swift
Button(action: {
    if let url = URL(string: "https://apps.apple.com/app/id6767492629?action=write-review") {
        UIApplication.shared.open(url)
    }
}) {
    HStack {
        Image(systemName: "star.fill").foregroundColor(.yellow)
        Text("Đánh giá ứng dụng")
        Spacer()
        Image(systemName: "chevron.right").foregroundColor(.secondary)
    }
}
```

---

# NHÓM 5 — DebugResetButton: Wrap #if DEBUG

### 5.1. Wrap definition

Mở file `Views/Components/DebugResetButton.swift` (hoặc path tương đương). Wrap toàn bộ struct trong `#if DEBUG ... #endif`:

```swift
#if DEBUG
struct DebugResetButton: View {
    // ... toàn bộ code hiện tại
}
#endif
```

### 5.2. Wrap call sites

Grep toàn project tìm `DebugResetButton` được sử dụng ở đâu. Mỗi call site cũng wrap `#if DEBUG`:

```swift
#if DEBUG
DebugResetButton()
#endif
```

Verify build Release không còn reference nào tới DebugResetButton.

---

# NHÓM 6 — watermarkEmail (placeholder cho tương lai)

App hiện chỉ có nội dung MIỄN PHÍ, chưa có IAP và chưa implement secure video player thật sự. `watermarkEmail` trong `SecureVideoPlayer.swift` là placeholder code cho feature tương lai (khi user mua khóa → access video Cloudflare Stream với watermark chống leak).

### Việc cần làm:
1. Verify code hiện tại chỉ là placeholder/demo, không thực sự xử lý PII của user thật.
2. Nếu giá trị watermarkEmail được hardcode (vd `"demo@..."`, empty string, hay UUID random) → KHÔNG cần đổi gì, chỉ note lại.
3. Nếu thấy có chỗ truyền email user thật vào → flag cho Nhut.
4. Thêm comment `// TODO: When IAP launches, replace with non-PII identifier (transaction ID or hashed user ID)` để nhắc cho tương lai.

KHÔNG implement secure video logic vì feature này chưa được build.

---

# NHÓM 7 — In-App Review Prompt (SKStoreReviewController)

### 7.1. Tạo ReviewManager service

Tạo file mới: `Services/ReviewManager.swift`

```swift
import StoreKit
import SwiftUI
import UIKit

@MainActor
final class ReviewManager {
    static let shared = ReviewManager()
    private init() {}
    
    private let userDefaults = UserDefaults.standard
    private let lastReviewRequestKey = "lastReviewRequestDate"
    private let appVersionAtLastRequestKey = "appVersionAtLastReviewRequest"
    private let actionCountKey = "reviewTriggerActionCount"
    
    private let positiveActionsThreshold = 3
    private let minDaysBetweenRequests = 60
    
    /// Gọi khi user vừa hoàn thành positive action (xem hết bài học, tune xong dây...).
    /// Tự động trigger review prompt khi đủ điều kiện.
    func recordPositiveAction() {
        let count = userDefaults.integer(forKey: actionCountKey) + 1
        userDefaults.set(count, forKey: actionCountKey)
        
        if count >= positiveActionsThreshold {
            requestReviewIfAppropriate()
        }
    }
    
    /// Gọi sau khi user mua IAP thành công. Trigger ngay (skip counter).
    func requestReviewAfterPurchase() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            self?.requestReviewIfAppropriate()
        }
    }
    
    private func requestReviewIfAppropriate() {
        let currentVersion = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""
        let lastVersion = userDefaults.string(forKey: appVersionAtLastRequestKey) ?? ""
        
        // Không request 2 lần cùng version
        if currentVersion == lastVersion {
            return
        }
        
        // Cách lần request gần nhất ít nhất `minDaysBetweenRequests` ngày
        if let lastDate = userDefaults.object(forKey: lastReviewRequestKey) as? Date {
            let daysSince = Calendar.current.dateComponents([.day], from: lastDate, to: Date()).day ?? 0
            if daysSince < minDaysBetweenRequests {
                return
            }
        }
        
        guard let scene = UIApplication.shared.connectedScenes
            .first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene else {
            return
        }
        
        AppStore.requestReview(in: scene)
        
        userDefaults.set(Date(), forKey: lastReviewRequestKey)
        userDefaults.set(currentVersion, forKey: appVersionAtLastRequestKey)
        userDefaults.set(0, forKey: actionCountKey)  // reset counter
    }
}
```

### 7.2. Tích hợp call sites

App hiện chưa có IAP, nên CHỈ implement 2 trigger (A và B). Trigger C (sau IAP) sẽ thêm sau khi launch IAP ở version tương lai.

**Trigger A — Khi user xem hết một bài học miễn phí:**

Tìm file VideoPlayer hoặc LessonView. Thêm vào khi video play đạt >80% hoặc kết thúc:
```swift
ReviewManager.shared.recordPositiveAction()
```

**Trigger B — Khi user tune xong một dây thành công:**

Trong TunerViewModel, khi state chuyển sang `.matched` (Nhóm 8), gọi:
```swift
ReviewManager.shared.recordPositiveAction()
```

**Trigger C — Khi user mua IAP** (sẽ thêm sau, không làm bây giờ):

Khi nào implement IAP, sẽ thêm:
```swift
ReviewManager.shared.requestReviewAfterPurchase()
```

Để sẵn method `requestReviewAfterPurchase()` trong ReviewManager (đã có trong code template) — chỉ là chưa có call site.
```

### 7.3. Debug helper (only #if DEBUG)

Thêm vào Settings (chỉ build Debug):
```swift
#if DEBUG
Button("Force Review Prompt (DEBUG)") {
    if let scene = UIApplication.shared.connectedScenes
        .first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene {
        AppStore.requestReview(in: scene)
    }
}
#endif
```

---

# NHÓM 8 — Tuner UX: State Machine + Anti-Jitter

### 8.1. Phân tích code hiện tại

Đọc và hiểu:
- `Tuner/TunerView.swift` (hoặc tương đương) — UI
- `Tuner/TunerViewModel.swift` (hoặc tương đương) — logic
- Integration với Tuna library: `InputSignalTracker.swift`

Trace luồng:
- Frequency được callback về như thế nào? (Tuna delegate hay closure?)
- Match logic hiện tại?
- Trigger "ting" sound ở đâu?
- Có sẵn RMS/amplitude data không, hay phải compute riêng?

### 8.2. Định nghĩa TunerState enum

Thêm vào TunerViewModel:

```swift
enum TunerState: Equatable {
    case listening
    case matched(stringID: Int, until: Date)
    case waitingForSilence(silenceStartedAt: Date?)
}
```

### 8.3. Thêm constants cho state machine

```swift
private struct TunerTuning {
    static let amplitudeThreshold: Float = -28.0      // dB
    static let silenceThreshold: Float = -35.0        // dB
    static let silenceDuration: TimeInterval = 0.3    // giây
    static let matchCooldown: TimeInterval = 1.5      // giây
    static let stabilityFramesRequired: Int = 5
    static let pitchToleranceCents: Double = 5.0
}
```

### 8.4. Thêm properties

```swift
@Published private(set) var tunerState: TunerState = .listening
private var consecutiveMatchCount: Int = 0
```

### 8.5. RMS calculation

Nếu Tuna chưa expose RMS, thêm:

```swift
private func calculateRMS(buffer: AVAudioPCMBuffer) -> Float {
    guard let channelData = buffer.floatChannelData?[0] else { return -.infinity }
    let frameLength = Int(buffer.frameLength)
    
    var sum: Float = 0
    for i in 0..<frameLength {
        let sample = channelData[i]
        sum += sample * sample
    }
    
    let rms = sqrt(sum / Float(frameLength))
    let rmsDB = 20 * log10(max(rms, .leastNonzeroMagnitude))
    return rmsDB
}
```

Hook vào AVAudioEngine input tap để compute RMS song song với pitch detection của Tuna.

### 8.6. State machine handler

Refactor frequency update handler:

```swift
func handleAudioFrame(frequency: Double, rms: Float) {
    switch tunerState {
    case .matched(let stringID, let until):
        if Date() >= until {
            tunerState = .waitingForSilence(silenceStartedAt: nil)
        }
        return
        
    case .waitingForSilence(let silenceStartedAt):
        if rms < TunerTuning.silenceThreshold {
            if let started = silenceStartedAt {
                if Date().timeIntervalSince(started) >= TunerTuning.silenceDuration {
                    tunerState = .listening
                    consecutiveMatchCount = 0
                }
            } else {
                tunerState = .waitingForSilence(silenceStartedAt: Date())
            }
        } else {
            // Reset silence timer nếu có noise
            if silenceStartedAt != nil {
                tunerState = .waitingForSilence(silenceStartedAt: nil)
            }
        }
        return
        
    case .listening:
        // Amplitude gate
        guard rms > TunerTuning.amplitudeThreshold else {
            consecutiveMatchCount = 0
            // Optional: clear visual indicator
            return
        }
        
        // Tìm dây gần nhất + tính cents off
        let (nearestStringID, targetFreq) = findNearestString(forFrequency: frequency)
        let centsOff = calculateCentsOff(detected: frequency, target: targetFreq)
        
        // Update UI realtime (cho user thấy thanh tuner di chuyển)
        updateUIIndicator(stringID: nearestStringID, centsOff: centsOff)
        
        // Stability check
        if abs(centsOff) <= TunerTuning.pitchToleranceCents {
            consecutiveMatchCount += 1
            
            if consecutiveMatchCount >= TunerTuning.stabilityFramesRequired {
                // MATCH CONFIRMED!
                triggerMatchFeedback(stringID: nearestStringID)
                tunerState = .matched(
                    stringID: nearestStringID,
                    until: Date().addingTimeInterval(TunerTuning.matchCooldown)
                )
                consecutiveMatchCount = 0
                
                // Record positive action cho review prompt (Nhóm 7)
                ReviewManager.shared.recordPositiveAction()
            }
        } else {
            consecutiveMatchCount = 0
        }
    }
}

private func triggerMatchFeedback(stringID: Int) {
    playTingSound()
    if #available(iOS 13.0, *) {
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.success)
    }
}
```

Lưu ý: `findNearestString`, `calculateCentsOff`, `updateUIIndicator`, `playTingSound` là các helper hiện đã có hoặc cần extract từ code cũ — KHÔNG viết lại logic match từ đầu, chỉ wrap trong state machine.

### 8.7. UI feedback theo state

Trong TunerView, observe `tunerState` và thay đổi visual feedback:

```swift
switch viewModel.tunerState {
case .listening:
    // Normal UI: thanh tuner di chuyển, hiển thị string đang detect
    
case .matched(let stringID, _):
    // Highlight dây matched với màu xanh
    // Hiển thị overlay "✓ Đã đúng!" cho dây stringID
    // Có thể có animation pulse / confetti nhẹ
    
case .waitingForSilence:
    // Faded UI, hiển thị hint "Đợi một chút rồi gảy dây tiếp theo..."
    // Hoặc đơn giản là dim thanh tuner
}
```

### 8.8. Settings — Tuner Sensitivity (optional, recommend)

Thêm slider trong SettingsView:

```swift
@AppStorage("tunerSensitivity") private var tunerSensitivity: String = "medium"

Picker("Độ nhạy Tuner", selection: $tunerSensitivity) {
    Text("Thấp (chỉ gảy mạnh)").tag("low")
    Text("Vừa").tag("medium")
    Text("Cao (gảy nhẹ cũng detect)").tag("high")
}
```

Trong TunerViewModel, map sang amplitudeThreshold:
- `low` → -32 dB
- `medium` → -28 dB
- `high` → -25 dB

---

# NHÓM 9 — Verify & Report

Sau khi hoàn tất nhóm 1-8, output report markdown ở `/tmp/v1-0-1-changes.md`:

### Sections cần có:
- **A. Version bump**: từ build 2/v1.0 → build 3/v1.0.1 ✓
- **B. PrivacyInfo.xcprivacy**: path, đã tạo, content correct
- **C. Info.plist changes**: keys đã gỡ, key đã reword (before/after text)
- **D. SettingsView changes**: URL đã thay (line numbers), nút review đã có
- **E. DebugResetButton**: file path, đã wrap, list các call sites cũng đã wrap
- **F. watermarkEmail findings**: giá trị thật là gì, đã đổi/không cần đổi, lý do
- **G. ReviewManager**: file đã tạo, list 3 call sites đã hook (file:line)
- **H. Tuner UX**: 
  - State machine đã implement
  - RMS calculation hook ở đâu
  - UI feedback changes
  - Constants có thể tune
- **I. Settings**: Sensitivity picker đã thêm chưa
- **J. Files thay đổi**: list đầy đủ
- **K. Items cần Nhut quyết định**: bất kỳ ambiguity nào

### KHÔNG làm:
- KHÔNG `xcodebuild` hoặc archive
- KHÔNG sửa Firestore rules (Nhut làm trực tiếp trên Firebase Console)
- KHÔNG sửa code logic của Tuna library (chỉ wrap trong state machine)
- KHÔNG đổi marketing copy, screenshots, hoặc App Store metadata
- KHÔNG commit Git (Nhut review code trước khi commit)