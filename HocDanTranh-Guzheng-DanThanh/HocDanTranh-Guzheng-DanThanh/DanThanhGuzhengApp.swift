//
//  DanThanhGuzhengApp.swift
//  Dan Thanh Guzheng
//
//  Main entry point for the Dan Thanh Guzheng learning app
//

import SwiftUI
import FirebaseCore

@main
struct DanThanhGuzhengApp: App {

    init() {
        FirebaseApp.configure()
    }
    @State private var showLaunchScreen = true

    var body: some Scene {
        WindowGroup {
            ZStack {
                // Always rendered so the tab bar layout is pre-calculated
                // before the launch screen fades — prevents the jump
                MainTabView()
                    .opacity(showLaunchScreen ? 0 : 1)
                    .zIndex(0)

                // Loading screen fades out when done
                if showLaunchScreen {
                    LaunchScreen()
                        .transition(.opacity)
                        .zIndex(1)
                }
            }
            .animation(.easeOut(duration: 0.5), value: showLaunchScreen)
            .onAppear {
                // Scroll animation takes 2.6s; dismiss after it completes
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.8) {
                    showLaunchScreen = false
                }
            }
        }
    }
}
