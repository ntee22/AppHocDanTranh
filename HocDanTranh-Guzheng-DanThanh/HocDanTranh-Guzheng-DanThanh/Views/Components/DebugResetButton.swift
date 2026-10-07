//
//  DebugResetButton.swift
//  Dan Thanh Guzheng
//
//  Debug helper to reset purchase state for testing
//  ONLY FOR DEVELOPMENT - Removed in Release builds via #if DEBUG
//

#if DEBUG

import SwiftUI

/// Hidden debug view to reset purchase state
/// Usage: Long-press the small debug indicator in bottom-right corner
struct DebugResetButton: View {
    @ObservedObject var viewModel: LessonViewModel
    @State private var showResetButton = false
    @State private var showConfirmation = false

    var body: some View {
        VStack {
            Spacer()
            HStack {
                Spacer()

                // Small debug indicator button
                Button(action: {
                    showConfirmation = true
                }) {
                    HStack(spacing: 6) {
                        Image(systemName: "arrow.counterclockwise.circle.fill")
                        Text("Reset")
                    }
                    .font(.caption2)
                    .foregroundColor(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color.red.opacity(0.8))
                    .cornerRadius(15)
                    .shadow(color: .black.opacity(0.2), radius: 3, x: 0, y: 2)
                }
                .padding(.trailing, 16)
                .padding(.bottom, 16)
            }
        }
        .allowsHitTesting(true)
        .alert("Reset Purchase State?", isPresented: $showConfirmation) {
            Button("Cancel", role: .cancel) { }
            Button("Reset", role: .destructive) {
                resetPurchase()
            }
        } message: {
            Text("This will reset your purchase state to unpurchased. You can test the buy flow again.")
        }
    }

    private func resetPurchase() {
        UserDefaults.standard.removeObject(forKey: "hasPurchasedCourse")
        viewModel.resetProgress()

        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.success)

        print("🔄 Purchase + lesson progress reset")

        withAnimation {
            showResetButton = false
        }
    }
}

// MARK: - Alternative: Shake to Reset
struct ShakeToResetModifier: ViewModifier {
    @ObservedObject var viewModel: LessonViewModel
    @State private var showAlert = false

    func body(content: Content) -> some View {
        content
            .onReceive(NotificationCenter.default.publisher(for: UIDevice.deviceDidShakeNotification)) { _ in
                showAlert = true
            }
            .alert("Reset Purchase? (Debug)", isPresented: $showAlert) {
                Button("Cancel", role: .cancel) { }
                Button("Reset", role: .destructive) {
                    UserDefaults.standard.removeObject(forKey: "hasPurchasedCourse")
                    viewModel.resetProgress()
                    print("🔄 Purchase + lesson progress reset via shake")
                }
            } message: {
                Text("Shake detected! Reset to test purchase flow again?")
            }
    }
}

extension View {
    func shakeToReset(viewModel: LessonViewModel) -> some View {
        modifier(ShakeToResetModifier(viewModel: viewModel))
    }
}

// MARK: - Device Shake Detection
extension UIDevice {
    static let deviceDidShakeNotification = Notification.Name(rawValue: "deviceDidShakeNotification")
}

extension UIWindow {
    open override func motionEnded(_ motion: UIEvent.EventSubtype, with event: UIEvent?) {
        if motion == .motionShake {
            NotificationCenter.default.post(name: UIDevice.deviceDidShakeNotification, object: nil)
        }
    }
}

#endif
