package vn.hocdantranh.guzheng.ui.video

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.video.VideoCategory
import vn.hocdantranh.guzheng.data.video.VideoItem
import vn.hocdantranh.guzheng.data.video.VideoSource

/**
 * Asserts against string resources rather than Vietnamese literals: these tests drive
 * `VideoScreen` directly, so they never run `LocaleController` and resolve whichever
 * locale the device is set to.
 */
class VideoScreenTest {

    @get:Rule val compose = createComposeRule()

    private fun str(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun item(id: String, title: String, category: VideoCategory) = VideoItem(
        id = id,
        title = title,
        url = "https://youtu.be/VGqJwQPjA3g?si=x",
        source = VideoSource.YOUTUBE,
        category = category,
        order = id.toInt(),
    )

    private val videos = listOf(
        item("1", "Giới thiệu Đàn Tranh", VideoCategory.INTRO),
        item("2", "Gương Sáng Bồ Đề", VideoCategory.SONGS),
        item("3", "Hướng dẫn thay dây", VideoCategory.TUTORIAL),
    )

    @Test
    fun rendersEveryVideo() {
        compose.setContent {
            VideoScreen(VideoUiState(videos = videos), {}, {}, {})
        }

        compose.onNodeWithText("Gương Sáng Bồ Đề").assertIsDisplayed()
        compose.onNodeWithText("Hướng dẫn thay dây").assertIsDisplayed()
    }

    @Test
    fun showsOnlyChipsForCategoriesPresentInTheData() {
        compose.setContent {
            VideoScreen(VideoUiState(videos = videos), {}, {}, {})
        }

        compose.onNode(hasText(str(R.string.video_category_all)) and hasClickAction())
            .assertIsDisplayed()
        compose.onNode(hasText(str(R.string.video_category_intro)) and hasClickAction())
            .assertIsDisplayed()
        // No `tips` video exists, so no Tip & Trick chip may render.
        compose.onNodeWithText(str(R.string.video_category_tips)).assertDoesNotExist()
    }

    @Test
    fun tappingAChipReportsTheCategory() {
        var selected: VideoCategory? = null
        compose.setContent {
            VideoScreen(VideoUiState(videos = videos), { selected = it }, {}, {})
        }

        // The songs label is also the header title, so target the clickable chip.
        compose.onNode(hasText(str(R.string.video_category_songs)) and hasClickAction())
            .performClick()

        assertEquals(VideoCategory.SONGS, selected)
    }

    @Test
    fun tappingTheActiveChipClearsTheFilter() {
        var selected: VideoCategory? = VideoCategory.SONGS
        compose.setContent {
            VideoScreen(
                VideoUiState(videos = videos, selectedCategory = VideoCategory.SONGS),
                { selected = it },
                {},
                {},
            )
        }

        compose.onNode(hasText(str(R.string.video_category_songs)) and hasClickAction())
            .performClick()

        assertEquals("tapping the active chip clears it, matching iOS", null, selected)
    }

    @Test
    fun tappingACardReportsTheVideo() {
        var clicked: VideoItem? = null
        compose.setContent {
            VideoScreen(VideoUiState(videos = videos), {}, { clicked = it }, {})
        }

        compose.onNodeWithText("Gương Sáng Bồ Đề").performClick()

        assertEquals("2", clicked?.id)
    }

    @Test
    fun showsTheEmptyState() {
        compose.setContent {
            VideoScreen(VideoUiState(videos = emptyList(), isLoading = false), {}, {}, {})
        }

        compose.onNodeWithText(str(R.string.video_empty)).assertIsDisplayed()
    }
}
