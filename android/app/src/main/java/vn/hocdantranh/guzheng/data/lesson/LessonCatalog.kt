package vn.hocdantranh.guzheng.data.lesson

/**
 * The lesson catalog, ported verbatim from iOS `Lesson.mockLessons()`.
 *
 * Content is Vietnamese in both locales, exactly as on iOS — [Lesson.subtitle] carries the
 * English title and the surrounding chrome is what gets localized. Translating the bodies
 * would mean inventing English copy that has no iOS counterpart.
 *
 * Lessons 1–6 are free and stream from YouTube. Lessons 7–16 are unreleased: they carry no
 * video ID, so they show the "coming soon" card. Lesson 7 will be free; 8–16 belong to the
 * paid course (`course_basic_1490k`) and stay "coming soon" until [FeatureFlags.IAP_ENABLED]
 * is on — see [LessonAccess].
 */
object LessonCatalog {

    val lessons: List<Lesson> = listOf(
        // ---- Free lessons (1–6) ----
        Lesson(
            lessonNumber = 1,
            title = "Giới thiệu Đàn Tranh",
            subtitle = "Introduction to Guzheng",
            thumbnailUrl = "https://img.youtube.com/vi/B2aIkMOMSYA/maxresdefault.jpg",
            videoId = "B2aIkMOMSYA",
            isFree = true,
            duration = "8:45",
            isYouTube = true,
            description = "Giới thiệu một số đặc điểm của Guzheng, chào mừng đến khóa học tại nhà, và một số bài học tiếp theo.",
            practiceTips = listOf(
                "Hãy cố gắng theo đuổi đam mê, mọi người đều tin vào bạn 💪"
            ),
            whatYoullLearn = listOf(
                "Lịch sử và nguồn gốc của đàn Guzheng",
                "Sự khác nhau giữa đàn tranh Việt và Guzheng",
                "Cấu tạo các bộ phận của đàn",
                "Lộ trình học trong khóa học này"
            ),
            funFact = "Guzheng có lịch sử hơn 2.500 năm — từ thời Chiến Quốc bên Trung Hoa. Ngày nay nhạc cụ này được yêu thích khắp châu Á và trên toàn thế giới! 🌏"
        ),
        Lesson(
            lessonNumber = 2,
            title = "Cách đeo móng, vị trí nốt",
            subtitle = "Finger Picks & Note Positions",
            thumbnailUrl = "https://img.youtube.com/vi/9Pww1rrA-e4/maxresdefault.jpg",
            videoId = "9Pww1rrA-e4",
            isFree = true,
            duration = "10:20",
            isYouTube = true,
            description = "Hướng dẫn cách đeo móng đúng cách và tìm vị trí các nốt nhạc trên đàn tranh.",
            practiceTips = listOf(
                "Ngồi thẳng, vai buông thả tự nhiên.",
                "Giữ móng gảy ở góc 45° với mặt đàn.",
                "Luyện 10 phút mỗi ngày hơn 1 giờ cuối tuần."
            ),
            whatYoullLearn = listOf(
                "Cách đeo móng gảy đúng vào từng ngón tay",
                "Vị trí 5 nốt cơ bản trong âm giai ngũ cung",
                "Cách đặt tay phải trên mặt đàn",
                "Tư thế ngồi chuẩn khi chơi đàn"
            ),
            funFact = "Móng gảy (giả) thường làm từ nhựa hoặc đồi mồi giả. Ngày xưa các cung nữ Trung Hoa để móng tay thật dài để gảy đàn — đẹp nhưng dễ gãy lắm! 💅"
        ),
        Lesson(
            lessonNumber = 3,
            title = "Nhịp phách, luyện ngón",
            subtitle = "Rhythm & Finger Training",
            thumbnailUrl = "https://img.youtube.com/vi/6ChWwXCPSIE/maxresdefault.jpg",
            videoId = "6ChWwXCPSIE",
            isFree = true,
            duration = "12:15",
            isYouTube = true,
            description = "Hãy ngồi đúng tư thế, thả lỏng tay và vai, theo dõi clip và ủng hộ mình nhé.",
            practiceTips = listOf(
                "Giữ nhịp đều, không vội vàng.",
                "Tay và vai luôn thư giãn — căng cơ sẽ làm tiếng đàn cứng.",
                "Luyện từ chậm đến nhanh, đừng bỏ qua bước chậm!"
            ),
            whatYoullLearn = listOf(
                "Hiểu nhịp 4/4 và cách đếm phách cơ bản",
                "Luyện độc lập từng ngón tay phải",
                "Kỹ thuật gảy xuống (劈 pī) và gảy lên (托 tuō)",
                "Phối hợp nhịp thở với tiếng đàn"
            ),
            funFact = "Não bộ cần khoảng 21 ngày để hình thành một thói quen mới. Luyện ngón đàn đều đặn mỗi ngày — dù chỉ 10 phút — sẽ hiệu quả hơn nhiều so với luyện dồn một lần! 🧠"
        ),
        Lesson(
            lessonNumber = 4,
            title = "Song Âm (Nhóm lớn/nhỏ)",
            subtitle = "Double Notes Technique",
            thumbnailUrl = "https://img.youtube.com/vi/HmwP2YzmeOk/maxresdefault.jpg",
            videoId = "HmwP2YzmeOk",
            isFree = true,
            duration = "9:30",
            isYouTube = true,
            description = "Hướng dẫn theo dõi kỹ trong video, luyện tập mỗi ngày sẽ đem lại kết quả vượt mong đợi.",
            practiceTips = listOf(
                "Quan sát kỹ vị trí ngón tay trong video.",
                "Luyện từng nhóm nốt riêng biệt trước khi ghép.",
                "Kiên trì sẽ thấy tiến bộ — đây là kỹ thuật cần thời gian!"
            ),
            whatYoullLearn = listOf(
                "Song âm nhóm nhỏ: ngón cái + ngón trỏ (quãng 1 - 7)",
                "Song âm nhóm lớn: ngón cái + ngón giữa (quãng 8)",
                "Cách để 2 ngón bật đều và đồng thời",
                "Ứng dụng song âm trong giai điệu thực tế"
            ),
            funFact = "Song âm quãng 5 (Do-Sol) là âm thanh đặc trưng của Guzheng trong các bản nhạc cổ điển Trung Hoa. Khi bạn nghe nhạc phim cổ trang — đó chính là kỹ thuật này đấy! 🎬"
        ),
        Lesson(
            lessonNumber = 5,
            title = "Rung Âm tay trái",
            subtitle = "Left Hand Vibrato",
            thumbnailUrl = "https://img.youtube.com/vi/dZ7hBhRQs2M/maxresdefault.jpg",
            videoId = "dZ7hBhRQs2M",
            isFree = true,
            duration = "11:00",
            isYouTube = true,
            description = "Theo dõi những ký hiệu nốt, chú ý dùng sức tay trái, tiến hành tập luyện. Nếu có nhu cầu mua đàn và các phụ kiện Guzheng hãy liên hệ mình qua các sàn thương mại điện tử và Fanpage ở các mạng xã hội.",
            practiceTips = listOf(
                "Dùng lực từ cổ tay, không từ ngón — sẽ ít mỏi hơn nhiều.",
                "Rung đều và liên tục, không giật cục.",
                "Luyện với metronome để giữ nhịp khi rung."
            ),
            whatYoullLearn = listOf(
                "Ký hiệu rung âm (揉弦 róu xián) trong bản nhạc",
                "Cách dùng sức từ cổ tay trái để tạo rung",
                "Phân biệt rung nhanh, rung chậm và rung sâu",
                "Kết hợp tay trái rung trong khi tay phải gảy"
            ),
            funFact = "Kỹ thuật rung âm chính là linh hồn của Guzheng — nó tạo nên cảm giác 'nói' và 'thở' cho từng nốt nhạc. Không có rung âm, tiếng đàn sẽ nghe cứng như đàn piano! 🎵"
        ),
        Lesson(
            lessonNumber = 6,
            title = "Lướt dây, kí hiệu",
            subtitle = "Glissando Technique",
            thumbnailUrl = "https://img.youtube.com/vi/5LOtc3xCMwk/maxresdefault.jpg",
            videoId = "5LOtc3xCMwk",
            isFree = true,
            duration = "14:30",
            isYouTube = true,
            description = "Kỹ thuật nghe rất hay, cố gắng theo dõi kỹ, cho mình like subscribe nhé.",
            practiceTips = listOf(
                "Ngón tay lướt nhẹ nhàng — đừng nhấn quá mạnh vào dây.",
                "Giữ áp lực đều từ đầu đến cuối lướt.",
                "Nghe lại video nhiều lần để cảm nhận tốc độ chuẩn."
            ),
            whatYoullLearn = listOf(
                "Kỹ thuật lướt dây (刮奏 guā zòu) từ thấp lên cao",
                "Lướt từ cao xuống thấp và lướt hai tay",
                "Đọc ký hiệu lướt dây trong bản nhạc số",
                "Ứng dụng lướt dây để tạo hiệu ứng âm thanh đặc biệt"
            ),
            funFact = "Lướt dây là kỹ thuật 'signature' của Guzheng mà không nhạc cụ nào khác tạo ra được y hệt! Trong tiếng Trung nó được gọi là 刮奏 (guā zòu) — nghe như tiếng mưa rào hay suối chảy vậy 🌊"
        ),

        // ---- Coming soon (7–16). 7 is free; 8–16 are the paid course. ----
        Lesson(
            lessonNumber = 7,
            title = "Hướng dẫn Nụ Hồng Mong Manh",
            subtitle = "Fragile Rose Bud — Song Tutorial",
            thumbnailUrl = "",
            videoId = "",
            isFree = true,
            duration = "",
        ),
        Lesson(
            lessonNumber = 8,
            title = "Bài học ngón 4 tay phải và ngón cái đánh ngược",
            subtitle = "Right-Hand 4th Finger & Reverse Thumb",
            thumbnailUrl = "",
            videoId = "",
            isFree = false,
            duration = "",
        ),
        Lesson(
            lessonNumber = 9,
            title = "Hướng dẫn Si Fa",
            subtitle = "The Si & Fa Presses",
            thumbnailUrl = "",
            videoId = "",
            isFree = false,
            duration = "",
        ),
        Lesson(
            lessonNumber = 10,
            title = "Áp dụng Si Fa: Ánh trăng nói hộ lòng tôi",
            subtitle = "Si Fa in Practice — The Moon Represents My Heart",
            thumbnailUrl = "",
            videoId = "",
            isFree = false,
            duration = "",
        ),
        Lesson(
            lessonNumber = 11,
            title = "Kỹ thuật tay trái",
            subtitle = "Left-Hand Techniques",
            thumbnailUrl = "",
            videoId = "",
            isFree = false,
            duration = "",
        ),
        Lesson(
            lessonNumber = 12,
            title = "Hoạt âm",
            subtitle = "Pitch Slides",
            thumbnailUrl = "",
            videoId = "",
            isFree = false,
            duration = "",
        ),
        Lesson(
            lessonNumber = 13,
            title = "Vê dây",
            subtitle = "Tremolo",
            thumbnailUrl = "",
            videoId = "",
            isFree = false,
            duration = "",
        ),
        Lesson(
            lessonNumber = 14,
            title = "Thực hành vê dây, bài hát",
            subtitle = "Tremolo Practice & Song",
            thumbnailUrl = "",
            videoId = "",
            isFree = false,
            duration = "",
        ),
        Lesson(
            lessonNumber = 15,
            title = "Hợp âm rải",
            subtitle = "Arpeggios",
            thumbnailUrl = "",
            videoId = "",
            isFree = false,
            duration = "",
        ),
        Lesson(
            lessonNumber = 16,
            title = "Thực hành hợp âm rải và bài hát",
            subtitle = "Arpeggio Practice & Song",
            thumbnailUrl = "",
            videoId = "",
            isFree = false,
            duration = "",
        ),
    )
}
