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
