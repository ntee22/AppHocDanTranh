//
//  LessonViewModel.swift
//  Dan Thanh Guzheng
//
//  Manages lesson data and purchase state
//

import Foundation
import Combine

/// ViewModel responsible for managing lessons and user's purchase state
@MainActor
class LessonViewModel: ObservableObject {
    // MARK: - Published Properties

    @Published var lessons: [Lesson] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    @Published var openedLessonNumbers: Set<Int> = []

    private let paymentManager = PaymentManager.shared
    private var cancellables = Set<AnyCancellable>()
    private let openedKey = "openedLessonNumbers"

    // MARK: - Derived State

    var hasPurchasedCourse: Bool {
        paymentManager.hasPurchasedCourse
    }

    func access(for lesson: Lesson) -> LessonAccess {
        LessonAccess.resolve(lesson, iapEnabled: FeatureFlags.iapEnabled, ownsCourse: hasPurchasedCourse)
    }

    private var playableLessons: [Lesson] {
        lessons.filter { access(for: $0).isPlayable }
    }

    /// Number of playable lessons the user has actually opened
    var completedFreeCount: Int {
        playableLessons.filter { openedLessonNumbers.contains($0.lessonNumber) }.count
    }

    /// Total number of playable lessons — the progress ring's denominator
    var freeLessonsCount: Int {
        playableLessons.count
    }

    /// Next playable lesson the user hasn't opened yet; nil when all are done
    var nextLesson: Lesson? {
        playableLessons.first { !openedLessonNumbers.contains($0.lessonNumber) }
    }

    // MARK: - Initialization

    init() {
        loadOpenedLessons()
        loadLessons()

        paymentManager.objectWillChange
            .receive(on: RunLoop.main)
            .sink { [weak self] _ in self?.objectWillChange.send() }
            .store(in: &cancellables)
    }

    // MARK: - Public Methods

    func loadLessons() {
        isLoading = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
            self?.lessons = Lesson.mockLessons()
            self?.isLoading = false
        }
    }

    func isLessonAccessible(_ lesson: Lesson) -> Bool {
        access(for: lesson).isPlayable
    }

    /// Call when the user opens a lesson — persists progress for playable ones only
    func markOpened(_ lesson: Lesson) {
        guard access(for: lesson).isPlayable else { return }
        openedLessonNumbers.insert(lesson.lessonNumber)
        UserDefaults.standard.set(Array(openedLessonNumbers), forKey: openedKey)
    }

    /// Clears all lesson progress (for debug reset)
    func resetProgress() {
        openedLessonNumbers = []
        UserDefaults.standard.removeObject(forKey: openedKey)
    }

    func purchaseCourse() async throws {
        try await paymentManager.purchase(IAPProductID.courseBasic)
    }

    func restorePurchases() async throws {
        try await paymentManager.restorePurchases()
    }

    // MARK: - Private

    private func loadOpenedLessons() {
        let numbers = UserDefaults.standard.array(forKey: openedKey) as? [Int] ?? []
        openedLessonNumbers = Set(numbers)
    }
}
