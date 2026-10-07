//
//  Localization.swift
//  Dan Thanh Guzheng
//
//  Localization helper for Vietnamese/English support
//

import Foundation

/// Localized string keys for the app
enum L10n {
    
    // MARK: - Welcome & Hero
    static let welcomeTo = NSLocalizedString("welcome_to", comment: "Welcome to")
    static let appName = NSLocalizedString("app_name", comment: "Dan Thanh Guzheng")
    static let heroSubtitle = NSLocalizedString("hero_subtitle", comment: "Begin your journey into the ancient art of Guzheng")
    
    // MARK: - Tab Bar
    static let tabLessons = NSLocalizedString("tab_lessons", comment: "Lessons")
    static let tabTuner = NSLocalizedString("tab_tuner", comment: "Tuner")
    static let tabSheetMusic = NSLocalizedString("tab_sheet_music", comment: "Sheet Music")
    
    // MARK: - Lessons
    static let courseLessons = NSLocalizedString("course_lessons", comment: "Course Lessons")
    static let lesson = NSLocalizedString("lesson", comment: "Lesson")
    static let free = NSLocalizedString("free", comment: "Free")
    static let unlocked = NSLocalizedString("unlocked", comment: "Unlocked")
    
    // MARK: - Premium Status
    static let premiumStudent = NSLocalizedString("premium_student", comment: "Premium Student")
    static let fullAccessMessage = NSLocalizedString("full_access_message", comment: "You have full access to all 12 lessons")
    
    // MARK: - Lesson Detail
    static let aboutThisLesson = NSLocalizedString("about_this_lesson", comment: "About This Lesson")
    static let whatYouWillLearn = NSLocalizedString("what_you_will_learn", comment: "What You'll Learn")
    static let practiceTips = NSLocalizedString("practice_tips", comment: "Practice Tips")
    
    // MARK: - Practice Tips Content
    static let practiceTipsContent = NSLocalizedString("practice_tips_content", comment: "Practice tips bullet points")
    
    // MARK: - Purchase Sheet
    static let unlockFullCourse = NSLocalizedString("unlock_full_course", comment: "Unlock Full Course")
    static let lifetimeAccess = NSLocalizedString("lifetime_access_subtitle", comment: "Get lifetime access to all 12 lessons")
    static let buyButton = NSLocalizedString("buy_button", comment: "Buy 3-Month Course")
    static let restorePurchase = NSLocalizedString("restore_purchase", comment: "Restore Purchase")
    
    // MARK: - Benefits
    static let benefit12Lessons = NSLocalizedString("benefit_12_lessons", comment: "12 Comprehensive Lessons")
    static let benefit12LessonsDesc = NSLocalizedString("benefit_12_lessons_desc", comment: "From beginner to advanced techniques")
    static let benefitHighQuality = NSLocalizedString("benefit_high_quality", comment: "High-Quality Videos")
    static let benefitHighQualityDesc = NSLocalizedString("benefit_high_quality_desc", comment: "Crystal clear demonstrations")
    static let benefitLifetime = NSLocalizedString("benefit_lifetime", comment: "Lifetime Access")
    static let benefitLifetimeDesc = NSLocalizedString("benefit_lifetime_desc", comment: "Learn at your own pace, forever")
    static let benefitExpert = NSLocalizedString("benefit_expert", comment: "Expert Instruction")
    static let benefitExpertDesc = NSLocalizedString("benefit_expert_desc", comment: "Learn from master Guzheng teachers")
    
    // MARK: - Placeholders
    static let comingSoon = NSLocalizedString("coming_soon", comment: "Coming Soon")
    static let tunerDescription = NSLocalizedString("tuner_description", comment: "A professional tuner to help you keep your Guzheng in perfect pitch")
    static let sheetMusicLibrary = NSLocalizedString("sheet_music_library", comment: "Sheet Music Library")
    static let sheetMusicDescription = NSLocalizedString("sheet_music_description", comment: "Access a curated collection of traditional and modern Guzheng sheet music")
    static let guzhengTuner = NSLocalizedString("guzheng_tuner", comment: "Guzheng Tuner")
    
    // MARK: - Video Player
    static let loadingLesson = NSLocalizedString("loading_lesson", comment: "Loading lesson...")
    static let retry = NSLocalizedString("retry", comment: "Retry")
    static let failedToLoadVideo = NSLocalizedString("failed_to_load_video", comment: "Failed to load video")
    
    // MARK: - Errors
    static let purchaseFailed = NSLocalizedString("purchase_failed", comment: "Purchase Failed")
    static let ok = NSLocalizedString("ok", comment: "OK")
    static let noPreviousPurchases = NSLocalizedString("no_previous_purchases", comment: "No previous purchases found")
}

// MARK: - Lesson Localization
extension L10n {
    /// Get localized lesson title
    static func lessonTitle(_ number: Int) -> String {
        return NSLocalizedString("lesson_\(number)_title", comment: "Lesson \(number) title")
    }
    
    /// Get localized lesson subtitle
    static func lessonSubtitle(_ number: Int) -> String {
        return NSLocalizedString("lesson_\(number)_subtitle", comment: "Lesson \(number) subtitle")
    }
}
