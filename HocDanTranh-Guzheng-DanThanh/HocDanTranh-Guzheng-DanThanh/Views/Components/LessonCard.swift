//
//  LessonCard.swift
//  Dan Thanh Guzheng
//
//  Beautiful card component for displaying lessons
//

import SwiftUI

/// Elegant card displaying lesson information
struct LessonCard: View {
    
    let lesson: Lesson
    @EnvironmentObject var viewModel: LessonViewModel
    
    private var isAccessible: Bool {
        viewModel.isLessonAccessible(lesson)
    }
    
    var body: some View {
        HStack(spacing: 16) {
            // Lesson Number Badge
            lessonNumberBadge
            
            // Content
            VStack(alignment: .leading, spacing: 6) {
                Text(lesson.title)
                    .font(.cardTitle)
                    .foregroundColor(isAccessible ? .primary : .lockedGray)
                    .lineLimit(2)
                
                Text(lesson.subtitle)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
                
                HStack(spacing: 12) {
                    // Duration
                    HStack(spacing: 4) {
                        Image(systemName: "clock")
                            .font(.caption2)
                        Text(lesson.duration)
                            .font(.metadata)
                    }
                    .foregroundColor(.secondary)
                    
                    // Status Badge
                    statusBadge
                }
                .padding(.top, 4)
            }
            
            Spacer()
            
            // Lock or Arrow
            accessoryView
        }
        .padding()
        .background(cardBackground)
        .overlay(cardBorder)
        .shadow(color: shadowColor, radius: 8, x: 0, y: 4)
    }
    
    // MARK: - Lesson Number Badge
    
    private var lessonNumberBadge: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 12)
                .fill(isAccessible ? 
                      LinearGradient(
                        colors: [.primaryTeal, .accentTeal],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                      ) :
                      LinearGradient(
                        colors: [.lockedGray, .lockedGray.opacity(0.7)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                      )
                )
                .frame(width: 60, height: 60)
            
            Text("\(lesson.lessonNumber)")
                .font(.lessonNumber)
                .foregroundColor(.white)
        }
    }
    
    // MARK: - Status Badge
    
    @ViewBuilder
    private var statusBadge: some View {
        if lesson.isFree {
            HStack(spacing: 4) {
                Image(systemName: "gift.fill")
                    .font(.caption2)
                Text(L10n.free)
                    .font(.metadata)
                    .fontWeight(.semibold)
            }
            .foregroundColor(.successGreen)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(
                Capsule()
                    .fill(Color.successGreen.opacity(0.15))
            )
        } else if viewModel.hasPurchasedCourse {
            HStack(spacing: 4) {
                Image(systemName: "checkmark.seal.fill")
                    .font(.caption2)
                Text(L10n.unlocked)
                    .font(.metadata)
                    .fontWeight(.semibold)
            }
            .foregroundColor(.premiumGold)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(
                Capsule()
                    .fill(Color.premiumGold.opacity(0.15))
            )
        }
    }
    
    // MARK: - Accessory View
    
    @ViewBuilder
    private var accessoryView: some View {
        if isAccessible {
            Image(systemName: "chevron.right")
                .font(.body)
                .foregroundColor(.primaryTeal)
        } else {
            LockBadge()
        }
    }
    
    // MARK: - Card Styling
    
    private var cardBackground: some View {
        RoundedRectangle(cornerRadius: 20)
            .fill(Color.cardBackground)
    }
    
    private var cardBorder: some View {
        RoundedRectangle(cornerRadius: 20)
            .stroke(
                isAccessible ? 
                    Color.primaryTeal.opacity(0.2) : 
                    Color.lockedGray.opacity(0.2),
                lineWidth: 1
            )
    }
    
    private var shadowColor: Color {
        isAccessible ? 
            Color.primaryTeal.opacity(0.1) : 
            Color.black.opacity(0.05)
    }
}

#Preview {
    VStack(spacing: 16) {
        LessonCard(lesson: Lesson.mockLessons()[0])
            .environmentObject(LessonViewModel())
        
        LessonCard(lesson: Lesson.mockLessons()[5])
            .environmentObject(LessonViewModel())
    }
    .padding()
    .background(Color.backgroundCream)
}
