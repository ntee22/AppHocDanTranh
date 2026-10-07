//
//  AppFonts.swift
//  Dan Thanh Guzheng
//
//  Typography system following traditional yet modern aesthetic
//

import SwiftUI

/// Centralized font system for the app
extension Font {
    
    // MARK: - Title Fonts (Serif for traditional feel)
    
    /// Large hero title
    static let heroTitle = Font.system(.largeTitle, design: .serif, weight: .bold)
    
    /// Section titles
    static let sectionTitle = Font.system(.title, design: .serif, weight: .semibold)
    
    /// Card titles
    static let cardTitle = Font.system(.title3, design: .serif, weight: .semibold)
    
    /// Subsection titles
    static let subtitle = Font.system(.headline, design: .serif, weight: .medium)
    
    // MARK: - Body Fonts (Sans-serif for readability)
    
    /// Main body text
    static let bodyText = Font.system(.body, design: .default)
    
    /// Secondary descriptive text
    static let caption = Font.system(.caption, design: .default)
    
    /// Small metadata text
    static let metadata = Font.system(.caption2, design: .default)
    
    // MARK: - Special Fonts
    
    /// Lesson numbers
    static let lessonNumber = Font.system(.title2, design: .rounded, weight: .bold)
    
    /// Button text
    static let buttonText = Font.system(.body, design: .default, weight: .semibold)
}

// MARK: - Text Styles
extension Text {
    
    /// Apply hero title style
    func heroTitleStyle() -> some View {
        self
            .font(.heroTitle)
            .foregroundColor(.secondaryBrown)
    }
    
    /// Apply section title style
    func sectionTitleStyle() -> some View {
        self
            .font(.sectionTitle)
            .foregroundColor(.primaryTeal)
    }
    
    /// Apply card title style
    func cardTitleStyle() -> some View {
        self
            .font(.cardTitle)
            .foregroundColor(.primary)
    }
    
    /// Apply subtitle style
    func subtitleStyle() -> some View {
        self
            .font(.subtitle)
            .foregroundColor(.secondary)
    }
    
    /// Apply body text style
    func bodyTextStyle() -> some View {
        self
            .font(.bodyText)
            .foregroundColor(.primary)
    }
    
    /// Apply caption style
    func captionStyle() -> some View {
        self
            .font(.caption)
            .foregroundColor(.secondary)
    }
}
