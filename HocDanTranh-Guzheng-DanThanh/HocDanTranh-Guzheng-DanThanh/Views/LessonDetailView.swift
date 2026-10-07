//
//  LessonDetailView.swift
//  Dan Thanh Guzheng
//
//  Vintage-styled lesson detail with video player
//

import SwiftUI

struct LessonDetailView: View {

    let lesson: Lesson
    @EnvironmentObject var viewModel: LessonViewModel
    @Environment(\.dismiss) private var dismiss
    @AppStorage("appLanguage") private var appLanguage: String = "vi"
    private var isEnglish: Bool { appLanguage == "en" }

    private var accentColors: [Color] {
        switch lesson.lessonNumber % 3 {
        case 0: return [Color.sageDark, Color.inkDark]
        case 1: return [Color.woodDark, Color.inkDark]
        default: return [Color.plum, Color.inkDark]
        }
    }

    var body: some View {
        ZStack {
            PaperBackground()

            ScrollView {
                VStack(spacing: 0) {
                    videoSection
                    detailContent
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(Color.paper.opacity(0.95), for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbar {
            ToolbarItem(placement: .principal) {
                OverlineLabel(text: "Bài \(String(format: "%02d", lesson.lessonNumber)) · \(lesson.duration)")
            }
        }
        .onAppear {
            viewModel.markOpened(lesson)
        }
    }

    // MARK: - Video Section

    @ViewBuilder
    private var videoSection: some View {
        if lesson.isFree {
            LessonVideoPlayer(videoID: lesson.videoID)
                .frame(height: 200)
                .background(Color.black)
        } else {
            comingSoonCard
        }
    }

    private var comingSoonCard: some View {
        VStack(spacing: 16) {
            Image(systemName: "lock.circle.fill")
                .font(.system(size: 64))
                .foregroundColor(Color.inkMuted)

            Text(isEnglish ? "Coming soon 🚧" : "Bài học sắp ra mắt 🚧")
                .font(.system(size: 24, weight: .medium, design: .serif))
                .foregroundColor(Color.inkDark)

            Text(isEnglish
                 ? "Finish the earlier lessons and come back!\nĐan Thanh is working hard to get this one ready for you 🎬"
                 : "Hãy hoàn thành các bài trước rồi quay lại nhé!\nĐan Thanh đang dồn sức lực để làm bài này cho bạn 🎬")
                .font(.system(size: 14))
                .foregroundColor(Color.inkSoft)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 60)
        .background(Color.paper)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal, 18)
        .padding(.top, 16)
    }

    // MARK: - Detail Content

    private var detailContent: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Status badge + duration
            HStack(spacing: 10) {
                Text(lesson.isFree ? "✦ Miễn phí" : "⏳ Coming Soon")
                    .font(.system(size: 10, weight: .bold))
                    .kerning(2)
                    .foregroundColor(lesson.isFree ? Color.sageDark : Color.inkMuted)
                    .padding(.horizontal, 9)
                    .padding(.vertical, 3)
                    .background(
                        (lesson.isFree ? Color.sageDark : Color.inkMuted).opacity(0.12)
                    )
                    .clipShape(Capsule())

                Text(lesson.duration)
                    .font(.system(size: 11))
                    .foregroundColor(Color.inkMuted)

                Spacer()
            }
            .padding(.top, 20)

            // Title
            Text(lesson.title)
                .font(.system(size: 28, weight: .medium, design: .serif))
                .foregroundColor(Color.inkDark)
                .lineSpacing(2)
                .padding(.top, 6)

            Text(lesson.subtitle)
                .font(.system(size: 16, design: .serif))
                .italic()
                .foregroundColor(Color.inkMuted)
                .padding(.top, 2)

            // Divider
            BrushDivider()
                .frame(width: 120)
                .padding(.vertical, 16)

            // Description
            Text(lessonDescription)
                .font(.system(size: 14))
                .foregroundColor(Color.inkSoft)
                .lineSpacing(6)

            // What You'll Learn card
            if !lesson.whatYoullLearn.isEmpty {
                whatYoullLearnCard
                    .padding(.top, 20)
            }

            // Practice tips card
            practiceTipsCard
                .padding(.top, 20)

            // Fun Fact card
            if let fact = lesson.funFact {
                funFactCard(fact)
                    .padding(.top, 20)
            }
        }
        .padding(.horizontal, 24)
        .padding(.bottom, 32)
    }

    // MARK: - What You'll Learn

    private var whatYoullLearnCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Text("✦")
                    .foregroundColor(Color.sageDark)
                    .font(.system(size: 12))
                OverlineLabel(text: isEnglish ? "You'll learn" : "Bạn sẽ học được")
            }

            VStack(alignment: .leading, spacing: 8) {
                ForEach(Array(lesson.whatYoullLearn.enumerated()), id: \.offset) { i, item in
                    HStack(alignment: .top, spacing: 10) {
                        Text(String(format: "%02d", i + 1))
                            .font(.system(size: 13, weight: .semibold, design: .serif))
                            .foregroundColor(Color.sageDark)
                            .frame(width: 22, alignment: .leading)
                        Text(item)
                            .font(.system(size: 14))
                            .foregroundColor(Color.inkSoft)
                            .lineSpacing(3)
                        Spacer()
                    }
                }
            }
        }
        .padding(16)
        .background(Color.paperWarm.opacity(0.6))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(Color.inkMuted.opacity(0.15), lineWidth: 1)
        )
    }

    // MARK: - Practice Tips

    private var practiceTipsCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Text("✦")
                    .foregroundColor(Color.sealRed)
                    .font(.system(size: 14))
                OverlineLabel(text: isEnglish ? "Practice tips" : "Practice tips · Lời khuyên")
            }

            VStack(alignment: .leading, spacing: 8) {
                ForEach(Array(practiceTips.enumerated()), id: \.offset) { i, tip in
                    HStack(alignment: .top, spacing: 10) {
                        Text("\(i + 1)")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(Color.sageDark)
                        Text(tip)
                            .font(.system(size: 13))
                            .foregroundColor(Color.inkSoft)
                            .lineSpacing(3)
                    }
                }
            }
        }
        .padding(16)
        .background(Color.paperWarm.opacity(0.6))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(Color.inkMuted.opacity(0.18), lineWidth: 1)
        )
    }

    // MARK: - Fun Fact

    private func funFactCard(_ fact: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Text("🎋")
                    .font(.system(size: 14))
                OverlineLabel(text: "Thú vị đó!")
            }
            Text(fact)
                .font(.system(size: 16, weight: .regular, design: .serif))
                .italic()
                .foregroundColor(Color.inkSoft)
                .lineSpacing(4)
        }
        .padding(16)
        .background(
            LinearGradient(
                colors: [
                    Color.sageDark.opacity(0.08),
                    Color.paperWarm.opacity(0.3)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(Color.sageDark.opacity(0.2), lineWidth: 1)
        )
    }

    // MARK: - Helpers

    private var lessonDescription: String {
        lesson.description.isEmpty
            ? (isEnglish
               ? "\(lesson.subtitle). Sit up straight, relax your shoulders and let your fingers glide over the strings — no stress, this isn't an exam! 😄"
               : "\(lesson.subtitle). Hãy ngồi đúng tư thế, thả lỏng vai và để những ngón tay chạm nhẹ vào dây — đừng căng thẳng như thi đại học nhé! 😄")
            : lesson.description
    }

    private var practiceTips: [String] {
        lesson.practiceTips.isEmpty
            ? (isEnglish
               ? ["🪑 Sit straight, relax your shoulders — imagine you're meditating.", "⏰ 10 minutes daily beats 1 hour on the weekend. Consistency is the secret!"]
               : ["🪑 Ngồi thẳng, vai buông thả tự nhiên — hãy tưởng tượng bạn đang ngồi thiền.", "⏰ Luyện 10 phút mỗi ngày còn hơn 1 giờ cuối tuần. Kiên nhẫn là bí kíp số 1!"])
            : lesson.practiceTips
    }
}

#Preview {
    NavigationStack {
        LessonDetailView(lesson: Lesson.mockLessons()[0])
            .environmentObject(LessonViewModel())
    }
}
