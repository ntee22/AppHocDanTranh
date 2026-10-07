//
//  LockBadge.swift
//  Dan Thanh Guzheng
//
//  Visual indicator for locked content
//

import SwiftUI

/// Badge showing locked status with elegant animation
struct LockBadge: View {
    
    @State private var isAnimating = false
    
    var body: some View {
        ZStack {
            Circle()
                .fill(
                    LinearGradient(
                        colors: [.lockedGray.opacity(0.3), .lockedGray.opacity(0.1)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 44, height: 44)
            
            Image(systemName: "lock.fill")
                .font(.system(size: 18))
                .foregroundColor(.lockedGray)
                .scaleEffect(isAnimating ? 1.1 : 1.0)
        }
        .onAppear {
            withAnimation(
                .easeInOut(duration: 1.5)
                .repeatForever(autoreverses: true)
            ) {
                isAnimating = true
            }
        }
    }
}

#Preview {
    LockBadge()
        .padding()
        .background(Color.backgroundCream)
}
