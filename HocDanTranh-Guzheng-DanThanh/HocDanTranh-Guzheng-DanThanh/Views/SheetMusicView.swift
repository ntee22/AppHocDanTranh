//
//  SheetMusicView.swift
//  Dan Thanh Guzheng
//
//  Sheet music library backed by Firestore.
//  Design (colors, fonts, components) is unchanged from the vintage system.
//

import SwiftUI
import Combine
import FirebaseFirestore

// MARK: - Google Drive URL helper

/// Converts any Google Drive sharing URL to a direct image URL.
/// Drive sharing links return HTML, not raw images — AsyncImage needs the raw bytes.
private func directImageURL(_ urlString: String) -> URL? {
    guard urlString.contains("drive.google.com") else {
        return URL(string: urlString)
    }
    // Extract file ID from:
    //   /file/d/FILE_ID/view
    //   /d/FILE_ID/
    //   ?id=FILE_ID
    //   /uc?id=FILE_ID
    var fileID: String?

    if let range = urlString.range(of: #"(?<=/d/)[^/?]+"#, options: .regularExpression) {
        fileID = String(urlString[range])
    } else if let components = URLComponents(string: urlString),
              let id = components.queryItems?.first(where: { $0.name == "id" })?.value {
        fileID = id
    }

    guard let id = fileID else { return URL(string: urlString) }
    // lh3.googleusercontent.com/d/ID serves the raw image for publicly shared files
    return URL(string: "https://lh3.googleusercontent.com/d/\(id)")
}

// MARK: - SheetRepository

class SheetRepository: ObservableObject {
    @Published var sheets: [SheetItem] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    // v1 key: Codable JSON — safe to bump if model changes
    private let cacheKey = "cached_sheets_v1"

    struct SheetItem: Identifiable, Codable {
        let id: String
        let title: String
        let pages: [String]
        let isFree: Bool
        let order: Int
    }

    func fetch() {
        loadCache()
        isLoading = true
        errorMessage = nil

        let db = Firestore.firestore()
        // Single-field order(by:) uses the auto-created index — no composite index needed.
        // isVisible is filtered client-side to avoid requiring a composite index.
        db.collection("sheets")
            .order(by: "order")
            .getDocuments { [weak self] snapshot, error in
                DispatchQueue.main.async {
                    guard let self else { return }
                    self.isLoading = false

                    if let error {
                        self.errorMessage = error.localizedDescription
                        print("❌ Firestore error: \(error.localizedDescription)")
                        return
                    }

                    guard let docs = snapshot?.documents else {
                        print("⚠️ No documents in sheets collection")
                        return
                    }

                    print("✅ Fetched \(docs.count) sheets from Firestore")

                    let fetched: [SheetItem] = docs.compactMap { doc in
                        let d = doc.data()
                        // Skip hidden sheets client-side
                        guard d["isVisible"] as? Bool == true else { return nil }
                        guard
                            let title = d["title"] as? String,
                            let pages = d["pages"] as? [String],
                            let order = d["order"] as? Int
                        else {
                            print("⚠️ Skipping malformed doc: \(doc.documentID) — \(doc.data())")
                            return nil
                        }
                        return SheetItem(
                            id: doc.documentID,
                            title: title,
                            pages: pages,
                            isFree: d["isFree"] as? Bool ?? true,
                            order: order
                        )
                    }
                    self.sheets = fetched
                    self.saveCache(fetched)
                }
            }
    }

    private func saveCache(_ items: [SheetItem]) {
        guard let encoded = try? JSONEncoder().encode(items) else { return }
        UserDefaults.standard.set(encoded, forKey: cacheKey)
    }

    private func loadCache() {
        guard
            let data = UserDefaults.standard.data(forKey: cacheKey),
            let decoded = try? JSONDecoder().decode([SheetItem].self, from: data)
        else { return }
        sheets = decoded
        print("📦 Loaded \(decoded.count) sheets from cache")
    }
}

// MARK: - SheetMusicView

struct SheetMusicView: View {

    @StateObject private var repo = SheetRepository()
    @State private var searchText = ""
    @State private var selectedSheet: SheetRepository.SheetItem?
    @FocusState private var searchFocused: Bool
    @AppStorage("appLanguage") private var appLanguage: String = "vi"
    private var isEnglish: Bool { appLanguage == "en" }

    private var filtered: [SheetRepository.SheetItem] {
        guard !searchText.isEmpty else { return repo.sheets }
        return repo.sheets.filter {
            $0.title.localizedCaseInsensitiveContains(searchText)
        }
    }

    var body: some View {
        ZStack {
            Color.paper.ignoresSafeArea()
                .onTapGesture { searchFocused = false }

            VStack(spacing: 0) {
                header
                searchBar
                content
            }
        }
        .onAppear { repo.fetch() }
        .fullScreenCover(item: $selectedSheet) { sheet in
            SheetViewer(sheet: sheet)
        }
    }

    // MARK: Header

    private var header: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 2) {
                OverlineLabel(text: "樂譜 · Sheet Nhạc")
                (
                    Text("Sheet ")
                        .font(.system(size: 32, weight: .medium, design: .serif))
                        .foregroundColor(Color.inkDark)
                    + Text("Guzheng")
                        .font(.system(size: 32, weight: .medium, design: .serif))
                        .italic()
                        .foregroundColor(Color.sageDark)
                )
                Text(isEnglish
                     ? "Hand-picked Guzheng sheet music compiled and arranged by Đan Thanh 🎶"
                     : "Những sheet Guzheng tinh tuyển được Đan Thanh biên soạn và sưu tầm cho mọi người 🎶")
                    .font(.system(size: 13))
                    .foregroundColor(Color.inkSoft)
                    .lineSpacing(4)
                    .padding(.top, 4)
            }
            Spacer()
            InkSeal(char: "樂", size: 32)
        }
        .padding(.top, 56)
        .padding(.horizontal, 24)
        .padding(.bottom, 14)
    }

    // MARK: Search Bar

    private var searchBar: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 14))
                .foregroundColor(Color.inkMuted)

            TextField(
                text: $searchText,
                prompt: Text(isEnglish ? "Enter a song name to search" : "Nhập tên bài hát bạn muốn tìm")
                    .foregroundColor(Color.inkMuted.opacity(0.7))
            ) { EmptyView() }
            .font(.system(size: 14))
            .foregroundColor(Color.inkDark)
            .autocorrectionDisabled()
            .focused($searchFocused)

            if !searchText.isEmpty {
                Button { searchText = "" } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 14))
                        .foregroundColor(Color.inkMuted)
                }
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(Color.paperWarm.opacity(0.8))
        .cornerRadius(10)
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(Color.woodDark.opacity(0.2), lineWidth: 1)
        )
        .padding(.horizontal, 18)
        .padding(.bottom, 12)
    }

    // MARK: Content

    @ViewBuilder
    private var content: some View {
        if let errorMsg = repo.errorMessage {
            Spacer()
            VStack(spacing: 12) {
                Image(systemName: "exclamationmark.triangle")
                    .font(.system(size: 32))
                    .foregroundColor(Color.sealRed)
                Text(isEnglish ? "Oops, couldn't load 😵" : "Ối, tải không được rồi 😵")
                    .font(.system(size: 18, weight: .medium, design: .serif))
                    .foregroundColor(Color.inkDark)
                Text(errorMsg)
                    .font(.system(size: 11))
                    .foregroundColor(Color.inkMuted)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                Button(isEnglish ? "🔄 Retry" : "🔄 Thử lại") { repo.fetch() }
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(Color.sageDark)
                    .padding(.top, 4)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 60)
            Spacer()
        } else if repo.isLoading && repo.sheets.isEmpty {
            ScrollView(showsIndicators: false) {
                LazyVStack(spacing: 10) {
                    ForEach(0..<6, id: \.self) { _ in SheetSkeletonCard() }
                }
                .padding(.horizontal, 18)
                .padding(.bottom, 110)
            }
        } else if filtered.isEmpty {
            Spacer()
            VStack(spacing: 12) {
                Text("樂")
                    .font(.system(size: 48))
                    .foregroundColor(Color.inkMuted.opacity(0.4))
                Text(searchText.isEmpty
                     ? (isEnglish ? "No sheets yet 📭" : "Chưa có sheet nào cả 📭")
                     : (isEnglish ? "No results for \"\(searchText)\" 🔍" : "Không tìm thấy \"\(searchText)\" 🔍"))
                    .font(.system(size: 18, weight: .medium, design: .serif))
                    .italic()
                    .foregroundColor(Color.inkMuted)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 60)
            Spacer()
        } else {
            ScrollView(showsIndicators: false) {
                LazyVStack(spacing: 10) {
                    ForEach(filtered) { sheet in
                        SheetCard(sheet: sheet) {
                            selectedSheet = sheet
                        }
                    }
                }
                .padding(.horizontal, 18)
                .padding(.bottom, 110)
            }
            .scrollDismissesKeyboard(.interactively)
        }
    }
}

// MARK: - SheetCard

private struct SheetCard: View {
    let sheet: SheetRepository.SheetItem
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 14) {
                thumbnail
                info
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 11))
                    .foregroundColor(Color.inkMuted.opacity(0.4))
            }
            .padding(14)
            .background(Color.paperWarm.opacity(0.7))
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color.inkMuted.opacity(0.15), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private var thumbnail: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.paperDeep)
                .frame(width: 64, height: 80)

            if let urlStr = sheet.pages.first, let url = directImageURL(urlStr) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let img):
                        img.resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(width: 64, height: 80)
                            .cornerRadius(8)
                            .clipped()
                    case .failure:
                        Image(systemName: "music.note.list")
                            .foregroundColor(Color.inkMuted)
                    default:
                        ProgressView().tint(Color.inkMuted)
                    }
                }
            } else {
                Image(systemName: "music.note.list")
                    .foregroundColor(Color.inkMuted)
            }

            if !sheet.isFree {
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color.black.opacity(0.5))
                    .frame(width: 64, height: 80)
                Image(systemName: "lock.fill")
                    .foregroundColor(.white)
                    .font(.system(size: 16))
            }
        }
        .frame(width: 64, height: 80)
    }

    private var info: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(sheet.title)
                .font(.system(size: 18, weight: .medium, design: .serif))
                .foregroundColor(Color.inkDark)
                .lineLimit(2)

            HStack(spacing: 8) {
                Text("\(sheet.pages.count) trang")
                    .font(.system(size: 11))
                    .foregroundColor(Color.inkMuted)

                Text(sheet.isFree ? "Miễn phí" : "Premium")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundColor(sheet.isFree ? Color.sageDark : Color.plum)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(
                        (sheet.isFree ? Color.sageDark : Color.plum).opacity(0.1)
                    )
                    .cornerRadius(999)
            }
        }
    }
}

// MARK: - SheetViewer

struct SheetViewer: View {
    let sheet: SheetRepository.SheetItem
    @Environment(\.dismiss) private var dismiss
    @State private var zoomPage: PageSelection?

    private struct PageSelection: Identifiable {
        let id: Int       // page index
        let url: URL
    }

    var body: some View {
        ZStack {
            Color.paper.ignoresSafeArea()

            VStack(spacing: 0) {
                // Fixed header — back button + title side by side
                HStack(alignment: .center, spacing: 12) {
                    Button { dismiss() } label: {
                        ZStack {
                            Circle()
                                .fill(Color.paperWarm.opacity(0.9))
                                .frame(width: 38, height: 38)
                                .shadow(color: Color.black.opacity(0.1), radius: 4)
                            Image(systemName: "chevron.left")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(Color.inkSoft)
                        }
                    }
                    .buttonStyle(.plain)
                    .fixedSize()

                    Text(sheet.title)
                        .font(.system(size: 20, weight: .medium, design: .serif))
                        .italic()
                        .foregroundColor(Color.inkDark)
                        .lineLimit(2)
                        .lineSpacing(2)
                        .minimumScaleFactor(0.85)

                    Spacer(minLength: 0)
                }
                .padding(.top, 56)
                .padding(.horizontal, 18)
                .padding(.bottom, 10)

                BrushDivider()
                    .padding(.horizontal, 40)
                    .padding(.bottom, 8)

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 8) {
                        // Hint shown once above first page
                        HStack(spacing: 6) {
                            Image(systemName: "hand.tap")
                                .font(.system(size: 11))
                            Text("👆 Nhấn vào trang để phóng to xem cho rõ nhé!")
                                .font(.system(size: 11))
                        }
                        .foregroundColor(Color.inkMuted.opacity(0.6))
                        .padding(.bottom, 4)

                        ForEach(Array(sheet.pages.enumerated()), id: \.offset) { idx, urlStr in
                            if let url = directImageURL(urlStr) {
                                AsyncImage(url: url) { phase in
                                    switch phase {
                                    case .success(let img):
                                        img.resizable()
                                            .aspectRatio(contentMode: .fit)
                                            .cornerRadius(8)
                                            .shadow(color: Color.black.opacity(0.1), radius: 8, y: 4)
                                            .padding(.horizontal, 16)
                                            .onTapGesture {
                                                zoomPage = PageSelection(id: idx, url: url)
                                            }
                                    case .failure:
                                        VStack(spacing: 8) {
                                            Image(systemName: "exclamationmark.triangle")
                                                .foregroundColor(Color.inkMuted)
                                            Text("Không thể tải trang \(idx + 1)")
                                                .font(.system(size: 13))
                                                .foregroundColor(Color.inkMuted)
                                        }
                                        .frame(height: 200)
                                    default:
                                        ZStack {
                                            RoundedRectangle(cornerRadius: 8)
                                                .fill(Color.paperDeep.opacity(0.5))
                                                .frame(height: 300)
                                            ProgressView().tint(Color.inkMuted)
                                        }
                                        .padding(.horizontal, 16)
                                    }
                                }
                            }
                        }

                        Spacer().frame(height: 40)
                    }
                }
            }
        }
        .fullScreenCover(item: $zoomPage) { page in
            PageZoomView(url: page.url, pageNumber: page.id + 1)
        }
    }
}

// MARK: - PageZoomView

private struct PageZoomView: View {
    let url: URL
    let pageNumber: Int
    @Environment(\.dismiss) private var dismiss

    @State private var scale: CGFloat = 1.0
    @State private var baseScale: CGFloat = 1.0
    @State private var offset: CGSize = .zero
    @State private var lastOffset: CGSize = .zero

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let img):
                    img.resizable()
                        .aspectRatio(contentMode: .fit)
                        .scaleEffect(scale)
                        .offset(offset)
                        .gesture(dragGesture)
                        .simultaneousGesture(pinchGesture)
                        .onTapGesture(count: 2) { resetZoom() }
                case .failure:
                    Image(systemName: "exclamationmark.triangle")
                        .foregroundColor(.white.opacity(0.5))
                        .font(.system(size: 40))
                default:
                    ProgressView().tint(.white)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            // Top bar
            VStack {
                HStack {
                    Button { dismiss() } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(width: 36, height: 36)
                            .background(Color.white.opacity(0.18))
                            .clipShape(Circle())
                    }
                    Spacer()
                    Text("Trang \(pageNumber)")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(.white.opacity(0.7))
                }
                .padding(.horizontal, 18)
                .padding(.top, 56)
                Spacer()
                Text("🤏 Chụm để phóng to  ·  👆👆 Nhấp đúp để reset")
                    .font(.system(size: 11))
                    .foregroundColor(.white.opacity(0.35))
                    .padding(.bottom, 30)
            }
        }
    }

    // MARK: Gestures

    private var pinchGesture: some Gesture {
        MagnificationGesture()
            .onChanged { value in
                let newScale = baseScale * value
                scale = min(max(newScale, 1.0), 6.0)
            }
            .onEnded { value in
                baseScale = min(max(baseScale * value, 1.0), 6.0)
                scale = baseScale
                if baseScale <= 1.0 { resetZoom() }
            }
    }

    private var dragGesture: some Gesture {
        DragGesture()
            .onChanged { value in
                guard scale > 1.0 else { return }
                offset = CGSize(
                    width: lastOffset.width + value.translation.width,
                    height: lastOffset.height + value.translation.height
                )
            }
            .onEnded { _ in
                lastOffset = offset
            }
    }

    private func resetZoom() {
        withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
            scale = 1.0
            baseScale = 1.0
            offset = .zero
            lastOffset = .zero
        }
    }
}

// MARK: - SheetSkeletonCard

private struct SheetSkeletonCard: View {
    @State private var shimmer = false

    var body: some View {
        HStack(spacing: 14) {
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.woodSoft.opacity(shimmer ? 0.3 : 0.5))
                .frame(width: 64, height: 80)
            VStack(alignment: .leading, spacing: 8) {
                RoundedRectangle(cornerRadius: 4)
                    .fill(Color.woodSoft.opacity(shimmer ? 0.3 : 0.5))
                    .frame(height: 16)
                RoundedRectangle(cornerRadius: 4)
                    .fill(Color.woodSoft.opacity(shimmer ? 0.3 : 0.5))
                    .frame(width: 80, height: 11)
            }
            Spacer()
        }
        .padding(14)
        .background(Color.paperWarm.opacity(0.5))
        .cornerRadius(12)
        .onAppear {
            withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) {
                shimmer = true
            }
        }
    }
}

#Preview {
    SheetMusicView()
}
