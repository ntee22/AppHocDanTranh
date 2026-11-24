package vn.hocdantranh.guzheng.ui.lessons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.lesson.Lesson
import vn.hocdantranh.guzheng.ui.components.InkSeal
import vn.hocdantranh.guzheng.ui.components.OverlineLabel
import vn.hocdantranh.guzheng.ui.components.PaperBackground
import vn.hocdantranh.guzheng.ui.components.PlumBranch
import vn.hocdantranh.guzheng.ui.components.ScrollEndMark
import vn.hocdantranh.guzheng.ui.theme.VintageColors

/** Stable handles for assertions that would otherwise be ambiguous with the lesson list. */
object LessonsTestTags {
    const val PROGRESS_NEXT_TITLE = "lessons:progressNextTitle"
}

/**
 * The Lessons tab, wired to its view model. Port of iOS `HomeView`.
 *
 * iOS's update banner is deliberately absent: it polls the App Store for a newer build, which
 * has no counterpart on Play.
 */
@Composable
fun LessonsRoute(
    onLessonClick: (Lesson) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: LessonsViewModel = viewModel(factory = LessonsViewModel.factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LessonsScreen(state = state, onLessonClick = onLessonClick, modifier = modifier)
}

@Composable
fun LessonsScreen(
    state: LessonsUiState,
    onLessonClick: (Lesson) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        PaperBackground()

        LazyColumn(Modifier.fillMaxSize()) {
            item { Masthead() }
            item { ProgressCard(state = state, onNextClick = onLessonClick) }
            item { SectionHeader() }

            itemsIndexed(
                items = state.lessons,
                key = { _, lesson -> lesson.lessonNumber },
            ) { index, lesson ->
                LessonRow(
                    lesson = lesson,
                    access = state.accessOf(lesson),
                    isLast = index == state.lessons.lastIndex,
                    onClick = { onLessonClick(lesson) },
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }

            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), Alignment.Center) {
                    ScrollEndMark()
                }
            }
        }
    }
}

// ---- Masthead ----

@Composable
private fun Masthead() {
    Box(Modifier.fillMaxWidth()) {
        PlumBranch(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 50.dp)
                .alpha(0.35f),
            size = 140.dp,
        )

        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 56.dp)) {
            VolumeLine()

            Text(
                text = stringResource(R.string.lessons_hero_line1),
                modifier = Modifier.padding(top = 10.dp),
                color = VintageColors.inkDark,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                fontSize = 44.sp,
                lineHeight = 48.sp,
            )
            Text(
                text = stringResource(R.string.lessons_hero_line2),
                modifier = Modifier.padding(top = 4.dp),
                color = VintageColors.sageDark,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Light,
                fontStyle = FontStyle.Italic,
                fontSize = 44.sp,
                lineHeight = 48.sp,
            )

            Text(
                text = stringResource(R.string.lessons_blurb),
                modifier = Modifier
                    .padding(top = 10.dp)
                    .widthIn(max = 280.dp),
                color = VintageColors.inkSoft,
                fontSize = 13.sp,
                lineHeight = 21.sp,
            )
        }
    }
}

/** `卷 一 ———— VOLUME ONE` rule above the hero title. */
@Composable
private fun VolumeLine() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.lessons_volume_mark),
            color = VintageColors.inkMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
        )
        Spacer(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(VintageColors.inkMuted.copy(alpha = 0.3f))
        )
        Text(
            text = stringResource(R.string.lessons_volume_one),
            color = VintageColors.inkMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
        )
    }
}

// ---- Progress card ----

@Composable
private fun ProgressCard(
    state: LessonsUiState,
    onNextClick: (Lesson) -> Unit,
) {
    val next = state.nextLesson
    val context = LocalContext.current
    val ratio = context.getString(
        R.string.lessons_progress_ratio,
        state.completedFreeCount,
        state.freeLessonsCount,
    )

    Row(
        Modifier
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VintageColors.paperWarm.copy(alpha = 0.55f))
            .border(
                width = 1.dp,
                color = VintageColors.inkMuted.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressRing(
            completed = state.completedFreeCount,
            total = state.freeLessonsCount,
            modifier = Modifier.semantics { contentDescription = ratio },
        )

        Column(Modifier.weight(1f)) {
            OverlineLabel(
                text = stringResource(
                    if (next != null) R.string.lessons_progress_in_progress
                    else R.string.lessons_progress_done
                )
            )
            Text(
                text = next?.title ?: stringResource(R.string.lessons_all_done),
                modifier = Modifier
                    .padding(top = 2.dp)
                    .testTag(LessonsTestTags.PROGRESS_NEXT_TITLE),
                color = VintageColors.inkDark,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (next != null) {
            Button(
                onClick = { onNextClick(next) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VintageColors.sageDark,
                    contentColor = VintageColors.paper,
                ),
                contentPadding = PaddingValues(
                    horizontal = 14.dp,
                    vertical = 8.dp,
                ),
            ) {
                Text(
                    text = stringResource(R.string.lessons_next).uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

/** 44dp ring, filled clockwise from twelve o'clock. Port of the iOS trimmed Circle. */
@Composable
private fun ProgressRing(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(44.dp), contentAlignment = Alignment.Center) {
        val fraction = completed.toFloat() / maxOf(1, total)
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = Size(
                size.width - stroke.width,
                size.height - stroke.width,
            )
            drawArc(
                color = VintageColors.inkMuted.copy(alpha = 0.15f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
            if (fraction > 0f) {
                drawArc(
                    color = VintageColors.sageDark,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = stroke,
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$completed",
                color = VintageColors.sageDark,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 16.sp,
            )
            Text(
                text = "/$total",
                color = VintageColors.inkMuted,
                fontSize = 10.sp,
                lineHeight = 10.sp,
            )
        }
    }
}

// ---- Section header ----

@Composable
private fun SectionHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 28.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            OverlineLabel(text = stringResource(R.string.lessons_contents))
            Text(
                text = stringResource(R.string.lessons_all_lessons),
                modifier = Modifier.padding(top = 2.dp),
                color = VintageColors.inkDark,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
            )
        }
        InkSeal(char = "樂", size = 32.dp)
    }
}
