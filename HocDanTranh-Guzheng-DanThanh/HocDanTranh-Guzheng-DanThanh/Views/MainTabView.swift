//
//  MainTabView.swift
//  Dan Thanh Guzheng
//

import SwiftUI

struct MainTabView: View {

    @StateObject private var lessonVM = LessonViewModel()
    @AppStorage("appLanguage") private var appLanguage: String = "vi"

    private var isEnglish: Bool { appLanguage == "en" }

    var body: some View {
        // Stock iOS 26 Liquid Glass tab bar — no custom appearance overrides,
        // so it behaves like the system default in every other app.
        TabView {
            Tab(isEnglish ? "Lessons" : "Khóa học", systemImage: "book.pages") {
                HomeView()
                    .environmentObject(lessonVM)
            }
            Tab("Video", systemImage: "play.rectangle") {
                VideoScreen()
            }
            Tab(isEnglish ? "Tuner" : "Lên dây", systemImage: "tuningfork") {
                TunerView()
            }
            Tab("Sheet", systemImage: "doc.text") {
                SheetMusicView()
            }
            Tab(isEnglish ? "Settings" : "Cài đặt", systemImage: "gearshape") {
                SettingsView()
                    .environmentObject(lessonVM)
            }
        }
        .tint(Color.sageDark)
    }
}

#Preview {
    MainTabView()
}
