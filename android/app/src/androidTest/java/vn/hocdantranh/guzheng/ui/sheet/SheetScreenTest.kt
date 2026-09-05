package vn.hocdantranh.guzheng.ui.sheet

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.sheet.SheetItem

/**
 * Asserts against string resources rather than Vietnamese literals: these tests drive
 * `SheetScreen` directly, so they never run `LocaleController` and resolve whichever
 * locale the device is set to. Same reasoning as `VideoScreenTest`.
 */
class SheetScreenTest {

    @get:Rule val compose = createComposeRule()

    private fun str(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun str(@StringRes id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private val sheets = listOf(
        SheetItem("1", "Nhỏ ơi", listOf("a"), isFree = true, order = 11),
        SheetItem("2", "Hồn quê", listOf("b"), isFree = true, order = 13),
    )

    private fun show(
        state: SheetUiState,
        onSheetClick: (SheetItem) -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        compose.setContent {
            var query by remember { mutableStateOf(state.query) }
            SheetScreen(
                state = state.copy(query = query),
                onQueryChange = { query = it },
                onSheetClick = onSheetClick,
                onRetry = onRetry,
            )
        }
    }

    @Test
    fun rendersEverySheetTitle() {
        show(SheetUiState(sheets = sheets))

        compose.onNodeWithText("Nhỏ ơi").assertIsDisplayed()
        compose.onNodeWithText("Hồn quê").assertIsDisplayed()
    }

    @Test
    fun typingInSearchFiltersTheList() {
        show(SheetUiState(sheets = sheets))

        compose.onNodeWithText(str(R.string.sheet_search_hint)).performTextInput("nhỏ")

        compose.onNodeWithText("Nhỏ ơi").assertIsDisplayed()
        compose.onNodeWithText("Hồn quê").assertDoesNotExist()
    }

    @Test
    fun clearButtonRestoresTheFullList() {
        show(SheetUiState(sheets = sheets))

        compose.onNodeWithText(str(R.string.sheet_search_hint)).performTextInput("nhỏ")
        compose.onNodeWithContentDescription(str(R.string.sheet_search_clear)).performClick()

        compose.onNodeWithText("Hồn quê").assertIsDisplayed()
    }

    @Test
    fun showsTheNoResultsEmptyStateForAQueryThatMatchesNothing() {
        show(SheetUiState(sheets = sheets, query = "zzz"))

        compose.onNodeWithText(str(R.string.sheet_empty_search, "zzz")).assertIsDisplayed()
    }

    @Test
    fun showsTheEmptyCatalogStateWhenThereAreNoSheets() {
        show(SheetUiState(sheets = emptyList()))

        compose.onNodeWithText(str(R.string.sheet_empty)).assertIsDisplayed()
    }

    @Test
    fun showsTheErrorStateAndRetries() {
        var retries = 0
        show(SheetUiState(errorMessage = "offline"), onRetry = { retries++ })

        compose.onNodeWithText(str(R.string.sheet_error_title)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.sheet_error_retry)).performClick()

        assertEquals(1, retries)
    }

    @Test
    fun tappingASheetReportsIt() {
        var tapped: SheetItem? = null
        show(SheetUiState(sheets = sheets), onSheetClick = { tapped = it })

        compose.onNodeWithText("Hồn quê").performClick()

        assertEquals("2", tapped?.id)
    }

    /**
     * The only coverage of the premium branch: no live sheet is premium, so this synthetic
     * item is the sole thing exercising the lock overlay. It must still be clickable.
     */
    @Test
    fun premiumSheetShowsTheLockAndStaysClickable() {
        var tapped: SheetItem? = null
        val premium = SheetItem("9", "Sheet trả phí", listOf("x"), isFree = false, order = 1)
        show(SheetUiState(sheets = listOf(premium)), onSheetClick = { tapped = it })

        compose.onNodeWithText(str(R.string.sheet_badge_premium)).assertIsDisplayed()
        compose.onNodeWithContentDescription(str(R.string.sheet_locked)).assertIsDisplayed()

        compose.onNodeWithText("Sheet trả phí").performClick()
        assertEquals("9", tapped?.id)
    }
}
