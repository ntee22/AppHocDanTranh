package vn.hocdantranh.guzheng.ui.lessons

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.lesson.Lesson
import vn.hocdantranh.guzheng.data.lesson.LessonCatalog
import vn.hocdantranh.guzheng.ui.theme.DanThanhTheme

class LessonDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun label(resId: Int, vararg args: Any): String = context.getString(resId, *args)

    private var backPresses = 0

    private val freeLesson = LessonCatalog.lessons.first { it.isFree }
    private val lockedLesson = LessonCatalog.lessons.first { !it.isFree }

    /**
     * The real player is a WebView that reaches YouTube, so tests substitute a stand-in
     * through the screen's video slot. What matters here is which branch the screen picks —
     * player versus coming-soon card — not what the player draws.
     */
    private fun launch(lesson: Lesson) {
        composeRule.setContent {
            DanThanhTheme {
                LessonDetailScreen(
                    lesson = lesson,
                    onBack = { backPresses++ },
                    videoContent = {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .testTag(STUB_PLAYER)
                        )
                    },
                )
            }
        }
    }

    private fun scrollTo(text: String) =
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))

    @Test
    fun showsTheLessonTitleAndSubtitle() {
        launch(freeLesson)
        composeRule.onNodeWithText(freeLesson.title).assertIsDisplayed()
        composeRule.onNodeWithText(freeLesson.subtitle).assertIsDisplayed()
    }

    @Test
    fun showsTheLessonNumberAndDurationInTheTopBar() {
        launch(freeLesson)
        val expected = label(R.string.lesson_detail_prefix, "01", freeLesson.duration)
        composeRule.onNodeWithText(expected.uppercase()).assertIsDisplayed()
    }

    @Test
    fun playsTheVideoForAFreeLesson() {
        launch(freeLesson)
        composeRule.onNodeWithTag(STUB_PLAYER).assertIsDisplayed()
        composeRule.onNodeWithText(label(R.string.lesson_coming_soon_title)).assertDoesNotExist()
    }

    @Test
    fun showsTheComingSoonCardForAnUnreleasedLesson() {
        launch(lockedLesson)
        composeRule.onNodeWithText(label(R.string.lesson_coming_soon_title)).assertIsDisplayed()
        composeRule.onNodeWithTag(STUB_PLAYER).assertDoesNotExist()
    }

    @Test
    fun showsTheFreeBadgeOnAFreeLesson() {
        launch(freeLesson)
        composeRule.onNodeWithText(label(R.string.lesson_badge_free_marked).uppercase())
            .assertIsDisplayed()
    }

    @Test
    fun showsTheComingSoonBadgeOnAnUnreleasedLesson() {
        launch(lockedLesson)
        composeRule.onNodeWithText(label(R.string.lesson_badge_coming_soon_marked).uppercase())
            .assertIsDisplayed()
    }

    @Test
    fun listsEverythingTheLessonTeaches() {
        launch(freeLesson)
        freeLesson.whatYoullLearn.forEach { item ->
            scrollTo(item)
            composeRule.onNodeWithText(item).assertIsDisplayed()
        }
    }

    @Test
    fun showsThePracticeTips() {
        launch(freeLesson)
        freeLesson.practiceTips.forEach { tip ->
            scrollTo(tip)
            composeRule.onNodeWithText(tip).assertIsDisplayed()
        }
    }

    @Test
    fun showsTheFunFactWhenTheLessonHasOne() {
        val lesson = LessonCatalog.lessons.first { it.funFact != null }
        launch(lesson)

        scrollTo(lesson.funFact!!)
        composeRule.onNodeWithText(lesson.funFact!!).assertIsDisplayed()
    }

    @Test
    fun hidesTheFunFactCardWhenTheLessonHasNone() {
        launch(lockedLesson)
        composeRule.onNodeWithText(label(R.string.lesson_fun_fact).uppercase())
            .assertDoesNotExist()
    }

    @Test
    fun fallsBackToDefaultPracticeTipsWhenTheLessonHasNone() {
        // Unreleased lessons carry no tips; iOS shows two generic ones rather than an empty card.
        launch(lockedLesson)

        val fallback = label(R.string.lesson_default_tip_1)
        scrollTo(fallback)
        composeRule.onNodeWithText(fallback).assertIsDisplayed()
    }

    @Test
    fun fallsBackToADescriptionBuiltFromTheSubtitleWhenTheLessonHasNone() {
        val noDescription = lockedLesson.copy(description = "")
        launch(noDescription)

        val fallback = label(R.string.lesson_default_description, noDescription.subtitle)
        scrollTo(fallback)
        composeRule.onNodeWithText(fallback).assertIsDisplayed()
    }

    @Test
    fun goingBackNotifiesTheCaller() {
        launch(freeLesson)
        composeRule.onNodeWithContentDescription(label(R.string.lesson_back)).performClick()
        assertEquals(1, backPresses)
    }

    private companion object {
        const val STUB_PLAYER = "stub:player"
    }
}
