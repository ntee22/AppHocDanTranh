import UIKit
import Combine

private struct TikTokOEmbed: Codable {
    let thumbnailUrl: String
    let title: String
    enum CodingKeys: String, CodingKey {
        case thumbnailUrl = "thumbnail_url"
        case title
    }
}

class ThumbnailLoader: ObservableObject {
    @Published var image: UIImage?

    func load(for item: VideoItem) {
        switch item.source {

        case .youtube:
            guard let id = extractYouTubeID(from: item.url),
                  let url = URL(string: "https://img.youtube.com/vi/\(id)/mqdefault.jpg")
            else { return }
            fetchImage(from: url)

        case .tiktok:
            let encoded = item.url.addingPercentEncoding(
                withAllowedCharacters: .urlQueryAllowed
            ) ?? ""
            guard let oembedURL = URL(
                string: "https://www.tiktok.com/oembed?url=\(encoded)"
            ) else { return }

            URLSession.shared.dataTask(with: oembedURL) { [weak self] data, _, _ in
                guard let data,
                      let resp = try? JSONDecoder().decode(TikTokOEmbed.self, from: data),
                      let imgURL = URL(string: resp.thumbnailUrl)
                else { return }
                self?.fetchImage(from: imgURL)
            }.resume()
        }
    }

    private func fetchImage(from url: URL) {
        URLSession.shared.dataTask(with: url) { [weak self] data, _, _ in
            guard let data, let img = UIImage(data: data) else { return }
            DispatchQueue.main.async { self?.image = img }
        }.resume()
    }

    private func extractYouTubeID(from url: String) -> String? {
        // ?v=VIDEO_ID  (standard watch URL)
        if let range = url.range(of: "v=") {
            let after = url[range.upperBound...]
            let id = String(after.prefix(11))
            if id.count == 11 { return id }
        }
        // youtu.be/VIDEO_ID  (short URL, strip any query params)
        if let range = url.range(of: "youtu.be/") {
            let after = url[range.upperBound...]
            let id = String(after.prefix(11))
            if id.count == 11 { return id }
        }
        // /shorts/VIDEO_ID
        if let range = url.range(of: "/shorts/") {
            let after = url[range.upperBound...]
            let id = String(after.prefix(11))
            if id.count == 11 { return id }
        }
        return nil
    }
}
