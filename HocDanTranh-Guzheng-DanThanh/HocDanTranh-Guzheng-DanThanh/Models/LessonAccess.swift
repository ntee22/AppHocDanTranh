//
//  LessonAccess.swift
//  Dan Thanh Guzheng
//
//  What the learner sees for one lesson. Must stay identical to Android LessonAccess.
//

import Foundation

enum LessonAccess: Equatable {
    /// No video yet, or a paid lesson while selling is switched off.
    case comingSoon
    /// Released and free.
    case free
    /// Released, paid and owned.
    case purchased
    /// Released and paid but not owned — the only state that may show a purchase entry.
    case locked

    var isPlayable: Bool { self == .free || self == .purchased }

    /// The single source of truth for lesson gating.
    static func resolve(_ lesson: Lesson, iapEnabled: Bool, ownsCourse: Bool) -> LessonAccess {
        if !lesson.isReleased { return .comingSoon }
        if lesson.isFree { return .free }
        if !iapEnabled { return .comingSoon }
        return ownsCourse ? .purchased : .locked
    }

    /// Short list badge. Only `.locked` mentions payment.
    func badge(isEnglish: Bool) -> String {
        switch self {
        case .free:       return isEnglish ? "Free" : "Miễn phí"
        case .purchased:  return isEnglish ? "Unlocked" : "Đã mở khóa"
        case .locked:     return isEnglish ? "Course" : "Khóa học"
        case .comingSoon: return "Coming Soon"
        }
    }

    /// Badge with its leading mark, for the detail screen.
    func markedBadge(isEnglish: Bool) -> String {
        switch self {
        case .free:       return isEnglish ? "✦ Free" : "✦ Miễn phí"
        case .purchased:  return isEnglish ? "✦ Unlocked" : "✦ Đã mở khóa"
        case .locked:     return isEnglish ? "🔒 Course" : "🔒 Khóa học"
        case .comingSoon: return "⏳ Coming Soon"
        }
    }
}
