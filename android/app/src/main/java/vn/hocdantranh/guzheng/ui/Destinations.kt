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
