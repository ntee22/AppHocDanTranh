package vn.hocdantranh.guzheng.ui.lessons

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.lesson.Lesson
import vn.hocdantranh.guzheng.data.lesson.LessonCatalog
import vn.hocdantranh.guzheng.ui.theme.DanThanhTheme

class LessonsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    /** Resolved from resources, never hardcoded — the device locale picks vi or en. */
    private fun label(resId: Int): String = context.getString(resId)

    private val opened = mutableListOf<Lesson>()

    private fun launch(state: LessonsUiState = LessonsUiState(LessonCatalog.lessons)) {
        composeRule.setContent {
            DanThanhTheme {
                LessonsScreen(state = state, onLessonClick = { opened += it })
            }
        }
    }

    /**
     * Each row is wrapped in a clickable, which merges its descendants — so a row is one
     * semantics node carrying its badge, duration, title and subtitle at once. Matching on
     * title AND clickability is what separates a row from the progress card, which repeats
     * the next lesson's title but is not itself clickable.
     */
    private fun row(lesson: Lesson) =
        composeRule.onNode(hasText(lesson.title) and hasClickAction())

    private fun scrollTo(lesson: Lesson) = composeRule
        .onNode(hasScrollToNodeAction())
        .performScrollToNode(hasText(lesson.title) and hasClickAction())

    @Test
    fun showsTheMastheadHeroLines() {
        launch()
        composeRule.onNodeWithText(label(R.string.lessons_hero_line1)).assertIsDisplayed()
        composeRule.onNodeWithText(label(R.string.lessons_hero_line2)).assertIsDisplayed()
    }

    @Test
    fun showsEveryLessonInTheCatalog() {
        launch()
        LessonCatalog.lessons.forEach { lesson ->
            scrollTo(lesson)
            row(lesson).assertIsDisplayed()
        }
    }

    @Test
    fun marksFreeLessonsFreeAndUnreleasedOnesComingSoon() {
        launch()

        val free = LessonCatalog.lessons.first { it.isFree }
        scrollTo(free)
        composeRule
            .onNode(hasText(free.title) and hasText(label(R.string.lesson_badge_free).uppercase()))
            .assertIsDisplayed()

        val locked = LessonCatalog.lessons.first { !it.isFree }
        scrollTo(locked)
        composeRule
            .onNode(
                hasText(locked.title) and
                    hasText(label(R.string.lesson_badge_coming_soon).uppercase())
            )
            .assertIsDisplayed()
    }

    @Test
    fun showsTheDurationOnEachRow() {
        launch()
        val lesson = LessonCatalog.lessons.first()
        scrollTo(lesson)
        composeRule
            .onNode(hasText(lesson.title) and hasText(lesson.duration))
            .assertIsDisplayed()
    }

    @Test
    fun tappingALessonOpensIt() {
        launch()
        val lesson = LessonCatalog.lessons[2]

        scrollTo(lesson)
        row(lesson).performClick()

        assertEquals(listOf(lesson), opened)
    }

    @Test
    fun tappingNextOpensTheFirstUnopenedFreeLesson() {
        launch(LessonsUiState(LessonCatalog.lessons, openedLessonNumbers = setOf(1, 2)))

        composeRule.onNodeWithText(label(R.string.lessons_next).uppercase()).performClick()

        assertEquals(listOf(3), opened.map { it.lessonNumber })
    }

    @Test
    fun showsTheNextLessonTitleOnTheProgressCard() {
        launch(LessonsUiState(LessonCatalog.lessons, openedLessonNumbers = setOf(1)))

        val expected = LessonCatalog.lessons.first { it.lessonNumber == 2 }
        composeRule.onNodeWithTag(LessonsTestTags.PROGRESS_NEXT_TITLE)
            .assertTextEquals(expected.title)
    }

    @Test
    fun celebratesWhenEveryFreeLessonHasBeenOpened() {
        launch(LessonsUiState(LessonCatalog.lessons, openedLessonNumbers = (1..6).toSet()))

        composeRule.onNodeWithTag(LessonsTestTags.PROGRESS_NEXT_TITLE)
            .assertTextEquals(label(R.string.lessons_all_done))
        composeRule.onNodeWithText(label(R.string.lessons_progress_done).uppercase())
            .assertIsDisplayed()
    }

    @Test
    fun hidesTheNextButtonOnceEverythingIsOpened() {
        launch(LessonsUiState(LessonCatalog.lessons, openedLessonNumbers = (1..6).toSet()))

        composeRule.onNodeWithText(label(R.string.lessons_next).uppercase())
            .assertDoesNotExist()
    }

    @Test
    fun opensLockedLessonsToo() {
        // The row is tappable even though the lesson is unreleased — iOS shows a
        // "coming soon" card on the detail screen rather than blocking the tap.
        launch()
        val locked = LessonCatalog.lessons.first { !it.isFree }

        scrollTo(locked)
        row(locked).performClick()

        assertEquals(listOf(locked), opened)
    }
}
