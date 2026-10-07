//
//  HomeView.swift
//  Dan Thanh Guzheng
//
//  Vintage-style home screen with masthead and lesson list
//

import SwiftUI

struct HomeView: View {

    @EnvironmentObject var viewModel: LessonViewModel
    @StateObject private var updateChecker = UpdateChecker.shared
    @State private var selectedLesson: Lesson?
    @AppStorage("appLanguage") private var appLanguage: String = "vi"
    private var isEnglish: Bool { appLanguage == "en" }

    var body: some View {
        NavigationStack {
            ZStack {
                PaperBackground()

                ScrollView {
                    VStack(spacing: 0) {
                        if updateChecker.hasUpdate {
                            updateBanner
                                .padding(.horizontal, 18)
                                .padding(.top, 16)
                                .transition(.opacity.combined(with: .move(edge: .top)))
                        }
                        masthead
                        progressCard
                        sectionHeader
                        lessonList
                        footer
                    }
                }
            }
            .navigationBarHidden(true)
            .navigationDestination(item: $selectedLesson) { lesson in
                LessonDetailView(lesson: lesson)
                    .environmentObject(viewModel)
            }
            .onAppear {
                updateChecker.checkForUpdateIfNeeded()
            }
            // #if DEBUG
            // .overlay(alignment: .bottomTrailing) {
            //     DebugResetButton(viewModel: viewModel)
            //         .padding(.bottom, 90)
            // }
            // #endif
        }
    }

    // MARK: - Update banner

    private var updateBanner: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(Color.sageDark.opacity(0.18))
                    .frame(width: 36, height: 36)
                Image(systemName: "arrow.down.circle.fill")
                    .font(.system(size: 18))
                    .foregroundColor(Color.sageDark)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(isEnglish ? "New version available 🎉" : "Có phiên bản mới 🎉")
                    .font(.system(size: 14, weight: .semibold, design: .serif))
                    .foregroundColor(Color.inkDark)
                if let latest = updateChecker.latestVersion {
                    Text(isEnglish
                         ? "v\(latest) brings new features and fixes."
                         : "Bản v\(latest) có tính năng mới và sửa lỗi.")
                        .font(.system(size: 12, design: .serif))
                        .foregroundColor(Color.inkMuted)
                }
            }

            Spacer(minLength: 8)

            VStack(spacing: 6) {
                Button(action: { updateChecker.openAppStore() }) {
                    Text(isEnglish ? "Update" : "Cập nhật")
                        .font(.system(size: 12, weight: .semibold, design: .serif))
                        .foregroundColor(Color.paper)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 7)
                        .background(Color.sageDark)
                        .clipShape(Capsule())
                }
                .buttonStyle(PlainButtonStyle())

                Button(action: { withAnimation { updateChecker.dismissBanner() } }) {
                    Text(isEnglish ? "Later" : "Để sau")
                        .font(.system(size: 11, design: .serif))
                        .italic()
                        .foregroundColor(Color.inkMuted)
                }
                .buttonStyle(PlainButtonStyle())
            }
        }
        .padding(14)
        .background(Color.paperWarm.opacity(0.85))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.sageDark.opacity(0.25), lineWidth: 1)
        )
        .shadow(color: Color.inkDark.opacity(0.05), radius: 4, y: 2)
    }

    // MARK: - Masthead

    private var masthead: some View {
        ZStack(alignment: .topTrailing) {
            // Plum blossom corner ornament
            PlumBranch(size: 140, color: Color.inkMuted)
                .opacity(0.35)
                .padding(.top, 50)
                .padding(.trailing, -10)

            VStack(alignment: .leading, spacing: 0) {
                // Volume line
                HStack(spacing: 10) {
                    Text("卷 一")
                        .font(.system(size: 10, weight: .bold))
                        .kerning(3)
                        .foregroundColor(Color.inkMuted)
                    Rectangle()
                        .fill(Color.inkMuted.opacity(0.3))
                        .frame(height: 1)
                    Text("Volume One")
                        .font(.system(size: 10, weight: .bold))
                        .kerning(3)
                        .foregroundColor(Color.inkMuted)
                }

                // Hero title
                VStack(alignment: .leading, spacing: 4) {
                    Text("Hành trình")
                        .font(.system(size: 44, weight: .medium, design: .serif))
                        .foregroundColor(Color.inkDark)
                        .lineSpacing(-2)

                    Text("Đàn Tranh")
                        .font(.system(size: 44, weight: .light, design: .serif))
                        .italic()
                        .foregroundColor(Color.sageDark)
                }
                .padding(.top, 10)

                // Subtitle
                Text(isEnglish
                     ? "Online lessons from home by Đan Thanh Đàn Tranh 🍵 We hope everyone, near and far, can master the Guzheng. Have fun learning! 🥰"
                     : "Khóa học online tại nhà do Đan Thanh Đàn Tranh và người thầm lặng đứng sau xây dựng nên 🍵 Mong mọi người ở phương xa đều có thể chinh phục đàn Guzheng nhé. Chúc bạn học vui 🥰")
                    .font(.system(size: 13))
                    .foregroundColor(Color.inkSoft)
                    .lineSpacing(4)
                    .padding(.top, 10)
                    .frame(maxWidth: 280, alignment: .leading)
            }
            .padding(.top, 56)
            .padding(.horizontal, 24)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    // MARK: - Progress Card

    private var progressCard: some View {
        let completed = viewModel.completedFreeCount
        let total = viewModel.freeLessonsCount
        let next = viewModel.nextLesson

        return HStack(spacing: 14) {
            // Circular progress
            ZStack {
                Circle()
                    .stroke(Color.inkMuted.opacity(0.15), lineWidth: 4)
                Circle()
                    .trim(from: 0, to: CGFloat(completed) / CGFloat(max(1, total)))
                    .stroke(Color.sageDark, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                    .rotationEffect(.degrees(-90))

                VStack(spacing: 0) {
                    Text("\(completed)")
                        .font(.system(size: 16, weight: .semibold, design: .serif))
                        .foregroundColor(Color.sageDark)
                    Text("/\(total)")
                        .font(.system(size: 10))
                        .foregroundColor(Color.inkMuted)
                }
            }
            .frame(width: 44, height: 44)

            VStack(alignment: .leading, spacing: 2) {
                OverlineLabel(text: next != nil ? (isEnglish ? "📖 In progress" : "📖 Đang học") : (isEnglish ? "🎉 Completed" : "🎉 Hoàn thành"))
                Text(next?.title ?? (isEnglish ? "You finished everything! 🥳" : "Bạn đã hoàn thành tất cả! 🥳"))
                    .font(.system(size: 17, weight: .medium, design: .serif))
                    .foregroundColor(Color.inkDark)
                    .lineLimit(1)
            }

            Spacer()

            if let lesson = next {
                Button {
                    selectedLesson = lesson
                } label: {
                    Text(isEnglish ? "Next" : "Tiếp")
                        .font(.system(size: 11, weight: .bold))
                        .kerning(1)
                        .textCase(.uppercase)
                        .foregroundColor(Color.paper)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(Color.sageDark)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(16)
        .background(Color.paperWarm.opacity(0.55))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.inkMuted.opacity(0.18), lineWidth: 1)
        )
        .padding(.horizontal, 24)
        .padding(.top, 20)
    }

    // MARK: - Section Header

    private var sectionHeader: some View {
        HStack(alignment: .bottom) {
            VStack(alignment: .leading, spacing: 2) {
                OverlineLabel(text: isEnglish ? "Contents" : "Mục lục")
                Text(isEnglish ? "All Lessons 🎼" : "Tất cả bài học 🎼")
                    .font(.system(size: 22, weight: .medium, design: .serif))
                    .italic()
                    .foregroundColor(Color.inkDark)
            }
            Spacer()
            InkSeal(char: "樂", size: 32)
        }
        .padding(.horizontal, 24)
        .padding(.top, 28)
        .padding(.bottom, 12)
    }

    // MARK: - Lesson List

    private var lessonList: some View {
        LazyVStack(spacing: 0) {
            ForEach(Array(viewModel.lessons.enumerated()), id: \.element.id) { idx, lesson in
                LessonRow(
                    lesson: lesson,
                    isLast: idx == viewModel.lessons.count - 1
                )
                .onTapGesture {
                    selectedLesson = lesson
                }
            }
        }
        .padding(.horizontal, 18)
    }

    // MARK: - Footer

    private var footer: some View {
        Text("· 琴 ·")
            .font(.system(size: 24))
            .foregroundColor(Color.inkMuted.opacity(0.6))
            .padding(.vertical, 24)
    }
}

// MARK: - LessonRow

struct LessonRow: View {
    let lesson: Lesson
    let isLast: Bool

    private var accent: LessonAccent {
        let accents: [LessonAccent] = [.sage, .wood, .sage, .plum, .wood, .sage, .plum, .wood, .sage, .plum, .wood, .sage, .plum, .wood, .sage, .plum]
        let idx = (lesson.lessonNumber - 1) % accents.count
        return accents[idx]
    }

    var body: some View {
        HStack(spacing: 14) {
            // Thumbnail
            LessonThumb(
                number: String(format: "%02d", lesson.lessonNumber),
                titleEn: lesson.subtitle,
                locked: !lesson.isFree,
                accent: accent
            )
            .frame(width: 92)

            // Text info
            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 4) {
                    Text(!lesson.isFree ? "Coming Soon" : "Miễn phí")
                        .font(.system(size: 9, weight: .bold))
                        .kerning(1.5)
                        .foregroundColor(!lesson.isFree ? Color.inkMuted : Color.sageDark)
                    Text("·")
                        .font(.system(size: 9))
                        .foregroundColor(Color.inkMuted.opacity(0.6))
                    Text(lesson.duration)
                        .font(.system(size: 9, weight: .bold))
                        .kerning(1.5)
                        .foregroundColor(Color.inkMuted)
                }
                .textCase(.uppercase)

                Text(lesson.title)
                    .font(.system(size: 17, weight: .medium, design: .serif))
                    .foregroundColor(Color.inkDark)
                    .lineLimit(2)
                    .lineSpacing(2)

                Text(lesson.subtitle)
                    .font(.system(size: 11))
                    .foregroundColor(Color.inkMuted)
                    .lineLimit(1)
                    .padding(.top, 1)
            }

            Spacer(minLength: 0)

            Image(systemName: "chevron.right")
                .font(.system(size: 11, weight: .medium))
                .foregroundColor(Color.inkSoft.opacity(0.4))
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 8)
        .background(Color.clear)
        .overlay(alignment: .bottom) {
            if !isLast {
                Rectangle()
                    .fill(Color.inkMuted.opacity(0.15))
                    .frame(height: 1)
                    .padding(.leading, 106)
            }
        }
        .contentShape(Rectangle())
    }
}

#Preview {
    HomeView()
        .environmentObject(LessonViewModel())
}
