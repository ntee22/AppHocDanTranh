import FirebaseFirestore
import Foundation
import Combine

private let pageSize = 10

class VideoRepository: ObservableObject {
    @Published var videos: [VideoItem] = []
    @Published var isLoading = false
    @Published var isLoadingMore = false
    @Published var hasMore = true

    private let db = Firestore.firestore()
    private var lastDocument: DocumentSnapshot?

    // MARK: - Initial fetch / pull-to-refresh

    func fetch() {
        guard !isLoading else { return }
        isLoading = true
        lastDocument = nil
        hasMore = true

        query(after: nil) { [weak self] result in
            guard let self else { return }
            self.isLoading = false
            switch result {
            case .success(let (items, last)):
                self.videos = items
                self.lastDocument = last
                self.hasMore = items.count == pageSize
            case .failure(let error):
                print("Firestore error: \(error)")
            }
        }
    }

    // MARK: - Load next page

    func fetchMore() {
        guard !isLoadingMore, hasMore, let cursor = lastDocument else { return }
        isLoadingMore = true

        query(after: cursor) { [weak self] result in
            guard let self else { return }
            self.isLoadingMore = false
            switch result {
            case .success(let (items, last)):
                self.videos.append(contentsOf: items)
                self.lastDocument = last
                self.hasMore = items.count == pageSize
            case .failure(let error):
                print("Firestore pagination error: \(error)")
            }
        }
    }

    // MARK: - Shared query builder

    private func query(
        after cursor: DocumentSnapshot?,
        completion: @escaping (Result<([VideoItem], DocumentSnapshot?), Error>) -> Void
    ) {
        var q: Query = db.collection("videos")
            .order(by: "order")
            .limit(to: pageSize)

        if let cursor { q = q.start(afterDocument: cursor) }

        q.getDocuments { snapshot, error in
            if let error {
                completion(.failure(error))
                return
            }
            guard let docs = snapshot?.documents else {
                completion(.success(([], nil)))
                return
            }

            let items = docs.compactMap { doc -> VideoItem? in
                let d = doc.data()

                // Skip hidden videos client-side; default to visible if field absent
                let isVisible = d["isVisible"] as? Bool ?? true
                guard isVisible else { return nil }

                guard
                    let title     = d["title"]    as? String,
                    let url       = d["url"]       as? String,
                    let sourceRaw = d["source"]    as? String,
                    let catRaw    = d["category"]  as? String,
                    let source    = VideoSource(rawValue: sourceRaw),
                    let category  = VideoCategory(rawValue: catRaw)
                else { return nil }

                let order: Int
                if let i = d["order"] as? Int { order = i }
                else if let f = d["order"] as? Double { order = Int(f) }
                else { return nil }

                return VideoItem(
                    id: doc.documentID,
                    title: title,
                    url: url,
                    source: source,
                    category: category,
                    order: order,
                    isVisible: true
                )
            }

            // docs.last is the raw page cursor regardless of client-side filtering
            completion(.success((items, docs.last)))
        }
    }
}
