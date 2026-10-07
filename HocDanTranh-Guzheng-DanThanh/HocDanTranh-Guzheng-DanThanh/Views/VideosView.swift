import SwiftUI

// MARK: - VideoScreen

struct VideoScreen: View {
    @StateObject private var repo = VideoRepository()
    @State private var selectedCategory: VideoCategory? = nil
    @AppStorage("appLanguage") private var appLanguage: String = "vi"
    private var isEnglish: Bool { appLanguage == "en" }

    var filtered: [VideoItem] {
        guard let cat = selectedCategory else { return repo.videos }
        return repo.videos.filter { $0.category == cat }
    }

    var availableCategories: [VideoCategory] {
        let existing = Set(repo.videos.map { $0.category })
        return VideoCategory.allCases.filter { existing.contains($0) }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                PaperBackground()

                VStack(spacing: 0) {
                    header
                        .readableContentWidth()
                    categoryChips
                        .readableContentWidth()

                    ScrollView(showsIndicators: false) {
                        VStack(spacing: 0) {
                            content
                            loadMoreTrigger
                            Spacer(minLength: 20)
                        }
                        .readableContentWidth()
                    }
                    .refreshable { repo.fetch() }
                }
            }
            .navigationBarHidden(true)
            .onAppear { if repo.videos.isEmpty { repo.fetch() } }
        }
    }

    // MARK: - Load More Trigger

    @ViewBuilder
    private var loadMoreTrigger: some View {
        if repo.isLoadingMore {
            HStack {
                Spacer()
                ProgressView().tint(Color.inkMuted)
                Spacer()
            }
            .padding(.vertical, 16)
        } else if repo.hasMore && !repo.videos.isEmpty {
            Color.clear
                .frame(height: 1)
                .onAppear { repo.fetchMore() }
        }
    }

    // MARK: - Header

    private var header: some View {
        VStack(alignment: .leading, spacing: 2) {
            OverlineLabel(text: "影片 · Video")
            Text(isEnglish ? "Songs " : "Bài hát ")
                .font(.system(size: 32, weight: .medium, design: .serif))
                .foregroundColor(Color.inkDark)
            + Text("Guzheng")
                .font(.system(size: 32, weight: .light, design: .serif))
                .italic()
                .foregroundColor(Color.sageDark)
            Text(isEnglish ? "💡 Tips & Tricks" : "💡 Tip & Trick hữu ích")
                .font(.system(size: 15, design: .serif))
                .italic()
                .foregroundColor(Color.inkMuted)
                .padding(.top, 2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 24)
        .padding(.top, 56)
        .padding(.bottom, 14)
    }

    // MARK: - Category Chips

    @ViewBuilder
    private var categoryChips: some View {
        if !availableCategories.isEmpty {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    CategoryChip(label: isEnglish ? "All" : "Tất cả", isSelected: selectedCategory == nil) {
                        withAnimation(.easeInOut(duration: 0.18)) {
                            selectedCategory = nil
                        }
                    }

                    ForEach(availableCategories, id: \.self) { cat in
                        CategoryChip(label: cat.displayName, isSelected: selectedCategory == cat) {
                            withAnimation(.easeInOut(duration: 0.18)) {
                                selectedCategory = selectedCategory == cat ? nil : cat
                            }
                        }
                    }
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 2)
            }
            .padding(.bottom, 12)
        }
    }

    // MARK: - Content

    @ViewBuilder
    private var content: some View {
        if repo.isLoading && repo.videos.isEmpty {
            VStack(spacing: 10) {
                ForEach(0..<3, id: \.self) { _ in SkeletonCard() }
            }
            .padding(.horizontal, 18)

        } else if filtered.isEmpty {
            VStack(spacing: 12) {
                InkSeal(char: "琴", size: 48)
                    .opacity(0.4)
                Text(isEnglish ? "No videos yet 🎬" : "Chưa có video nào 🎬")
                    .font(.system(size: 18, design: .serif))
                    .italic()
                    .foregroundColor(Color.inkMuted)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 60)

        } else {
            LazyVStack(spacing: 10) {
                ForEach(Array(filtered.enumerated()), id: \.element.id) { index, item in
                    if index == 0 {
                        FeaturedVideoCard(item: item)
                    } else {
                        VideoCard(item: item)
                    }
                }
            }
            .padding(.horizontal, 18)
        }
    }
}

// MARK: - FeaturedVideoCard

struct FeaturedVideoCard: View {
    let item: VideoItem
    @StateObject private var loader = ThumbnailLoader()

    private var platformColor: Color {
        item.source == .tiktok ? Color.inkDark : Color.sealRed
    }

    var body: some View {
        Button(action: { openVideo(item) }) {
            ZStack {
                Color.woodSoft.opacity(0.4)

                if let img = loader.image {
                    Image(uiImage: img)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .clipped()
                } else {
                    ProgressView().tint(Color.inkMuted)
                }
            }
            .frame(maxWidth: .infinity)
            // Scale with the available width instead of a fixed height so
            // 16:9 thumbnails aren't heavily cropped on wide displays.
            .aspectRatio(16 / 9, contentMode: .fit)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            // Gradient
            .overlay {
                LinearGradient(
                    colors: [Color.inkDark.opacity(0.7), .clear],
                    startPoint: .bottom,
                    endPoint: .center
                )
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            // Centered play button
            .overlay {
                Circle()
                    .fill(Color.paper.opacity(0.92))
                    .frame(width: 52, height: 52)
                    .overlay(
                        Image(systemName: "play.fill")
                            .font(.system(size: 18))
                            .foregroundColor(Color.inkDark)
                            .offset(x: 2)
                    )
                    .shadow(color: Color.inkDark.opacity(0.25), radius: 8)
            }
            // Title + meta pinned to bottom-left
            .overlay(alignment: .bottomLeading) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.title)
                        .font(.system(size: 18, weight: .medium, design: .serif))
                        .foregroundColor(Color.paper)
                        .lineLimit(2)
                        .lineSpacing(2)

                    HStack(spacing: 6) {
                        Text(item.category.displayName)
                            .font(.system(size: 9, weight: .bold))
                            .kerning(1.2)
                            .foregroundColor(Color.paper.opacity(0.85))
                            .padding(.horizontal, 7)
                            .padding(.vertical, 2)
                            .background(Color.paper.opacity(0.2))
                            .clipShape(Capsule())
                            .textCase(.uppercase)

                        Text(item.source == .tiktok ? "TikTok" : "YouTube")
                            .font(.system(size: 9, weight: .bold))
                            .foregroundColor(Color.paper)
                            .padding(.horizontal, 7)
                            .padding(.vertical, 2)
                            .background(platformColor.opacity(0.85))
                            .clipShape(Capsule())
                    }
                }
                .padding(14)
            }
            .overlay {
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color.inkMuted.opacity(0.15), lineWidth: 1)
            }
            .shadow(color: Color.inkDark.opacity(0.12), radius: 6, x: 0, y: 3)
        }
        .buttonStyle(PlainButtonStyle())
        .onAppear { loader.load(for: item) }
    }
}

// MARK: - VideoCard

struct VideoCard: View {
    let item: VideoItem
    @StateObject private var loader = ThumbnailLoader()

    private var platformColor: Color {
        item.source == .tiktok ? Color.inkDark : Color.sealRed
    }

    var body: some View {
        Button(action: { openVideo(item) }) {
            HStack(spacing: 14) {

                // Thumbnail
                ZStack {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color.woodSoft.opacity(0.5))
                        .frame(width: 108, height: 68)

                    if let img = loader.image {
                        Image(uiImage: img)
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(width: 108, height: 68)
                            .cornerRadius(8)
                            .clipped()
                    } else {
                        ProgressView().tint(Color.inkMuted)
                    }

                    Circle()
                        .fill(Color.inkDark.opacity(0.45))
                        .frame(width: 28, height: 28)
                        .overlay(
                            Image(systemName: "play.fill")
                                .font(.system(size: 10))
                                .foregroundColor(Color.paper)
                                .offset(x: 1)
                        )

                    VStack {
                        Spacer()
                        HStack {
                            Text(item.source == .tiktok ? "TikTok" : "YouTube")
                                .font(.system(size: 8, weight: .bold))
                                .foregroundColor(Color.paper)
                                .padding(.horizontal, 5)
                                .padding(.vertical, 2)
                                .background(platformColor.opacity(0.85))
                                .cornerRadius(3)
                            Spacer()
                        }
                        .padding(5)
                    }
                }
                .frame(width: 108, height: 68)

                // Text info
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.title)
                        .font(.system(size: 16, weight: .medium, design: .serif))
                        .foregroundColor(Color.inkDark)
                        .lineLimit(2)
                        .lineSpacing(2)

                    Text(item.category.displayName)
                        .font(.system(size: 9, weight: .bold))
                        .kerning(1.2)
                        .foregroundColor(Color.sageDark)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(Color.sageDark.opacity(0.1))
                        .clipShape(Capsule())
                        .textCase(.uppercase)
                }

                Spacer(minLength: 0)

                Image(systemName: "chevron.right")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(Color.inkSoft.opacity(0.4))
            }
            .padding(10)
            .background(Color.paperWarm.opacity(0.5))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(Color.inkMuted.opacity(0.15), lineWidth: 1)
            )
        }
        .buttonStyle(PlainButtonStyle())
        .onAppear { loader.load(for: item) }
    }
}

// MARK: - SkeletonCard

struct SkeletonCard: View {
    @State private var shimmer = false

    var body: some View {
        HStack(spacing: 14) {
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.woodSoft.opacity(shimmer ? 0.3 : 0.5))
                .frame(width: 108, height: 68)
            VStack(alignment: .leading, spacing: 8) {
                RoundedRectangle(cornerRadius: 4)
                    .fill(Color.woodSoft.opacity(shimmer ? 0.3 : 0.5))
                    .frame(height: 14)
                RoundedRectangle(cornerRadius: 4)
                    .fill(Color.woodSoft.opacity(shimmer ? 0.3 : 0.5))
                    .frame(width: 80, height: 10)
            }
            Spacer()
        }
        .padding(10)
        .background(Color.paperWarm.opacity(0.5))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .onAppear {
            withAnimation(.easeInOut(duration: 0.9).repeatForever()) { shimmer = true }
        }
    }
}

// MARK: - CategoryChip

struct CategoryChip: View {
    let label: String
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Text(label)
            .font(.system(size: 12, weight: .semibold))
            .foregroundColor(isSelected ? Color.paper : Color.inkSoft)
            .padding(.horizontal, 14)
            .padding(.vertical, 7)
            .background(isSelected ? Color.sageDark : Color.paperWarm.opacity(0.6))
            .clipShape(Capsule())
            .overlay(
                Capsule().stroke(
                    isSelected ? Color.clear : Color.inkMuted.opacity(0.25),
                    lineWidth: 1
                )
            )
            .contentShape(Capsule())
            .highPriorityGesture(TapGesture().onEnded { onTap() })
    }
}

// MARK: - Helper

private func openVideo(_ item: VideoItem) {
    guard let url = URL(string: item.url) else { return }
    UIApplication.shared.open(url)
}

#Preview {
    VideoScreen()
}
