import Foundation

enum VideoSource: String, Codable {
    case youtube, tiktok
}

enum VideoCategory: String, Codable, CaseIterable {
    case intro    = "intro"
    case songs    = "songs"
    case tips     = "tips"
    case tutorial = "tutorial"

    var displayName: String {
        switch self {
        case .intro:    return "Giới thiệu"
        case .songs:    return "Bài hát"
        case .tips:     return "Tip & Trick"
        case .tutorial: return "Hướng dẫn"
        }
    }
}

struct VideoItem: Identifiable {
    let id: String
    let title: String
    let url: String
    let source: VideoSource
    let category: VideoCategory
    let order: Int
    let isVisible: Bool
}
