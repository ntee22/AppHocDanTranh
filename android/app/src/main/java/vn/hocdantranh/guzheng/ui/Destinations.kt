package vn.hocdantranh.guzheng.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import vn.hocdantranh.guzheng.R

/**
 * Tab order mirrors iOS MainTabView exactly:
 * Lessons -> Video -> Tuner -> Sheet -> Settings.
 * Do not reorder — users switching platforms rely on muscle memory.
 */
enum class Destination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    LESSONS("lessons", R.string.tab_lessons, Icons.AutoMirrored.Filled.MenuBook),
    VIDEO("video", R.string.tab_video, Icons.Filled.PlayCircle),
    TUNER("tuner", R.string.tab_tuner, Icons.Filled.GraphicEq),
    SHEET("sheet", R.string.tab_sheet, Icons.Filled.Description),
    SETTINGS("settings", R.string.tab_settings, Icons.Filled.Settings)
}

/** Argument name for the lesson the detail screen shows. */
const val ARG_LESSON_NUMBER = "lessonNumber"

/**
 * Lesson detail lives in the same NavHost as the tabs rather than a nested graph, so the
 * bottom bar stays visible while reading a lesson — matching iOS, where the detail view is
 * pushed inside the tab's own NavigationStack.
 */
const val LESSON_DETAIL_ROUTE = "lessons/{$ARG_LESSON_NUMBER}"

fun lessonDetailRoute(lessonNumber: Int): String = "lessons/$lessonNumber"

/** Argument names for the sheet viewer and its page zoom. */
const val ARG_SHEET_ID = "sheetId"
const val ARG_PAGE_INDEX = "pageIndex"

/**
 * The sheet viewer and page zoom are presented full-screen, hiding the bottom bar —
 * iOS shows them in a `fullScreenCover`, which covers the tab bar. This is the opposite
 * of [LESSON_DETAIL_ROUTE], which keeps the bar because iOS pushes it inside the tab's
 * own NavigationStack.
 */
const val SHEET_VIEWER_ROUTE = "sheet/{$ARG_SHEET_ID}"
const val SHEET_PAGE_ROUTE = "sheet/{$ARG_SHEET_ID}/page/{$ARG_PAGE_INDEX}"

fun sheetViewerRoute(sheetId: String): String = "sheet/$sheetId"

fun sheetPageRoute(sheetId: String, pageIndex: Int): String = "sheet/$sheetId/page/$pageIndex"

/**
 * Whether [route] hides the bottom navigation bar.
 *
 * Compares whole route patterns rather than prefixes: `sheet` (the tab) is a prefix of
 * `sheet/{sheetId}` (the viewer), so a `startsWith` test would wrongly hide the bar on the
 * library list itself. Add future full-screen destinations here.
 */
fun isFullScreenRoute(route: String?): Boolean =
    route == SHEET_VIEWER_ROUTE || route == SHEET_PAGE_ROUTE
