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

    /// Number of free lessons the user has actually opened
    var completedFreeCount: Int {
        lessons.filter { $0.isFree && openedLessonNumbers.contains($0.lessonNumber) }.count
    }

    /// Total number of free (playable) lessons
    var freeLessonsCount: Int {
        lessons.filter { $0.isFree }.count
    }

    /// Next free lesson the user hasn't opened yet; nil when all are done
    var nextLesson: Lesson? {
        lessons.first { $0.isFree && !openedLessonNumbers.contains($0.lessonNumber) }
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
        lesson.isFree || paymentManager.hasPurchasedCourse
    }

    /// Call when the user opens a free lesson — persists progress
    func markOpened(_ lesson: Lesson) {
        guard lesson.isFree else { return }
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
