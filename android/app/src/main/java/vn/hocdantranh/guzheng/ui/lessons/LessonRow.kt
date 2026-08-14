package vn.hocdantranh.guzheng.ui.lessons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.lesson.Lesson
import vn.hocdantranh.guzheng.ui.components.LessonAccent
import vn.hocdantranh.guzheng.ui.components.LessonThumb
import vn.hocdantranh.guzheng.ui.theme.VintageColors

/** Left inset of the row divider, so it starts past the thumbnail. Matches iOS. */
private val DividerInset = 106.dp

/**
 * One entry in the lessons list: tile, metadata line, title, English subtitle, chevron.
 * Port of iOS `LessonRow`.
 */
@Composable
fun LessonRow(
    lesson: Lesson,
    isLast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.clickable(onClick = onClick)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LessonThumb(
                number = "%02d".format(lesson.lessonNumber),
                titleEn = lesson.subtitle,
                locked = !lesson.isFree,
                accent = LessonAccent.forLesson(lesson.lessonNumber),
                lockedContentDescription = stringResource(R.string.lesson_locked),
                modifier = Modifier.width(92.dp),
            )

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                MetadataLine(lesson)

                Text(
                    text = lesson.title,
                    color = VintageColors.inkDark,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = lesson.subtitle,
                    color = VintageColors.inkMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = VintageColors.inkSoft.copy(alpha = 0.4f),
            )
        }

        if (!isLast) {
            Spacer(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = DividerInset)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(VintageColors.inkMuted.copy(alpha = 0.15f))
            )
        }
    }
}

/** `MIỄN PHÍ · 8:45` — free lessons in sage, unreleased ones in muted ink. */
@Composable
private fun MetadataLine(lesson: Lesson) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                if (lesson.isFree) R.string.lesson_badge_free else R.string.lesson_badge_coming_soon
            ).uppercase(),
            color = if (lesson.isFree) VintageColors.sageDark else VintageColors.inkMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
        )
        Text(
            text = "·",
            color = VintageColors.inkMuted.copy(alpha = 0.6f),
            fontSize = 9.sp,
        )
        Text(
            text = lesson.duration,
            color = VintageColors.inkMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
        )
    }
}
