package vn.hocdantranh.guzheng.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.ui.theme.DanThanhTheme

class MainScaffoldTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    /**
     * Labels are resolved from resources, never hardcoded. The emulator's system
     * locale decides whether values/ (vi) or values-en/ wins, and a US-locale
     * emulator would fail hardcoded Vietnamese assertions.
     */
    private fun label(resId: Int): String = context.getString(resId)

    /**
     * PlaceholderScreen(title = ...) reuses the same string resource as the
     * currently selected tab's label, so on a real device the semantics tree
     * always has two nodes containing that text at once: the plain Text title
     * in the screen body, and the clickable NavigationBar tab itself. Plain
     * onNodeWithText(label) is therefore ambiguous whenever that tab is
     * selected — confirmed via the instrumented test report node dump (one
     * node with no Role/Actions, one with Role='Tab' and OnClick). Scope the
     * query to the clickable node so the test targets the tab bar, not the
     * placeholder body.
     */
    private fun tabNode(resId: Int) =
        composeRule.onNode(hasText(label(resId)) and hasClickAction())

    private fun launch() {
        composeRule.setContent {
            DanThanhTheme { MainScaffold() }
        }
    }

    @Test
    fun showsAllFiveTabsInIosOrder() {
        launch()
        tabNode(R.string.tab_lessons).assertIsDisplayed()
        tabNode(R.string.tab_video).assertIsDisplayed()
        tabNode(R.string.tab_tuner).assertIsDisplayed()
        tabNode(R.string.tab_sheet).assertIsDisplayed()
        tabNode(R.string.tab_settings).assertIsDisplayed()

        // Displayed independently isn't ordered. Read each tab's left edge in the
        // Scaffold's root coordinate space and require strictly increasing x, so a
        // reorder of Destination.entries (e.g. swapping SHEET and TUNER) fails this
        // test even though every individual assertIsDisplayed() above still passes.
        val leftEdges = listOf(
            R.string.tab_lessons,
            R.string.tab_video,
            R.string.tab_tuner,
            R.string.tab_sheet,
            R.string.tab_settings
        ).map { resId -> tabNode(resId).fetchSemanticsNode().boundsInRoot.left }

        leftEdges.zipWithNext().forEach { (left, right) ->
            assert(left < right) {
                "Expected strictly increasing left-to-right tab order, got $leftEdges"
            }
        }
    }

    @Test
    fun lessonsTabIsSelectedOnLaunch() {
        launch()
        tabNode(R.string.tab_lessons).assertIsSelected()
    }

    @Test
    fun tappingTunerSelectsTunerTab() {
        launch()
        tabNode(R.string.tab_tuner).performClick()
        tabNode(R.string.tab_tuner).assertIsSelected()
    }

    @Test
    fun vietnameseResourcesCarryTheIosWording() {
        val vi = context.createConfigurationContext(
            android.content.res.Configuration(context.resources.configuration).apply {
                setLocale(java.util.Locale("vi"))
            }
        )
        org.junit.Assert.assertEquals("Khóa học", vi.getString(R.string.tab_lessons))
        org.junit.Assert.assertEquals("Lên dây", vi.getString(R.string.tab_tuner))
        org.junit.Assert.assertEquals("Cài đặt", vi.getString(R.string.tab_settings))
    }
}
