package vn.hocdantranh.guzheng.ui.lessons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.lesson.Lesson
import vn.hocdantranh.guzheng.data.lesson.LessonAccess
import vn.hocdantranh.guzheng.ui.billing.CoursePurchaseCard
import vn.hocdantranh.guzheng.ui.components.BrushDivider
import vn.hocdantranh.guzheng.ui.components.OverlineLabel
import vn.hocdantranh.guzheng.ui.components.PaperBackground
import vn.hocdantranh.guzheng.ui.theme.VintageColors

/** Fixed player height, as on iOS — the recorded orientation does not change it. */
private val PlayerHeight = 200.dp

/**
 * Lesson detail wired to the view model. Records the lesson as opened on first composition,
 * matching the iOS `.onAppear`.
 */
@Composable
fun LessonDetailRoute(
    lessonNumber: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: LessonsViewModel = viewModel(factory = LessonsViewModel.factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lesson = state.lessons.firstOrNull { it.lessonNumber == lessonNumber }

    LaunchedEffect(lesson) {
        lesson?.let(viewModel::markOpened)
    }

    if (lesson != null) {
        LessonDetailScreen(
            lesson = lesson,
            access = state.accessOf(lesson),
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@Composable
fun LessonDetailScreen(
    lesson: Lesson,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    access: LessonAccess = LessonAccess.resolve(lesson, iapEnabled = false, ownsCourse = false),
    videoContent: @Composable (Lesson) -> Unit = { YouTubeLessonPlayer(it.videoId) },
) {
    Box(modifier.fillMaxSize()) {
        PaperBackground()

        Column(Modifier.fillMaxSize()) {
            DetailTopBar(lesson = lesson, onBack = onBack)

            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    if (access.isPlayable) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(PlayerHeight)
                                .background(Color.Black)
                        ) {
                            videoContent(lesson)
                        }
                    } else if (access == LessonAccess.LOCKED) {
                        // Unreachable while FeatureFlags.IAP_ENABLED is false — resolve()
                        // never returns LOCKED then.
                        CoursePurchaseCard()
                    } else {
                        ComingSoonCard()
                    }
                }
                item { DetailBody(lesson, access) }
            }
        }
    }
}

// ---- Top bar ----

@Composable
private fun DetailTopBar(lesson: Lesson, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(VintageColors.paper.copy(alpha = 0.95f))
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.lesson_back),
                tint = VintageColors.inkDark,
            )
        }

        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            val number = "%02d".format(lesson.lessonNumber)
            OverlineLabel(
                text = if (lesson.duration.isBlank()) {
                    stringResource(R.string.lesson_detail_prefix_no_duration, number)
                } else {
                    stringResource(R.string.lesson_detail_prefix, number, lesson.duration)
                }
            )
        }

        // Balances the back button so the overline stays optically centred.
        Box(Modifier.size(48.dp))
    }
}

// ---- Coming soon ----

@Composable
private fun ComingSoonCard() {
    Column(
        Modifier
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VintageColors.paper)
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(VintageColors.inkMuted.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = VintageColors.inkMuted,
                modifier = Modifier.size(32.dp),
            )
        }

        Text(
            text = stringResource(R.string.lesson_coming_soon_title),
            color = VintageColors.inkDark,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(R.string.lesson_coming_soon_body),
            modifier = Modifier.padding(horizontal = 32.dp),
            color = VintageColors.inkSoft,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )
    }
}

// ---- Body ----

@Composable
private fun DetailBody(lesson: Lesson, access: LessonAccess) {
    val context = LocalContext.current

    val description = lesson.description.ifBlank {
        context.getString(R.string.lesson_default_description, lesson.subtitle)
    }
    val tips = lesson.practiceTips.ifEmpty {
        listOf(
            context.getString(R.string.lesson_default_tip_1),
            context.getString(R.string.lesson_default_tip_2),
        )
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        StatusLine(lesson, access)

        Text(
            text = lesson.title,
            modifier = Modifier.padding(top = 6.dp),
            color = VintageColors.inkDark,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            fontSize = 28.sp,
            lineHeight = 34.sp,
        )
        Text(
            text = lesson.subtitle,
            modifier = Modifier.padding(top = 2.dp),
            color = VintageColors.inkMuted,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontSize = 16.sp,
        )

        BrushDivider(Modifier.padding(vertical = 16.dp).width(120.dp))

        Text(
            text = description,
            color = VintageColors.inkSoft,
            fontSize = 14.sp,
            lineHeight = 22.sp,
        )

        if (lesson.whatYoullLearn.isNotEmpty()) {
            NumberedCard(
                marker = "✦",
                markerColor = VintageColors.sageDark,
                markerSize = 12.sp,
                title = stringResource(R.string.lesson_youll_learn),
                items = lesson.whatYoullLearn,
                serifNumbers = true,
                modifier = Modifier.padding(top = 20.dp),
            )
        }

        NumberedCard(
            marker = "✦",
            markerColor = VintageColors.sealRed,
            markerSize = 14.sp,
            title = stringResource(R.string.lesson_practice_tips),
            items = tips,
            serifNumbers = false,
            modifier = Modifier.padding(top = 20.dp),
        )

        lesson.funFact?.let { fact ->
            FunFactCard(fact, Modifier.padding(top = 20.dp))
        }
    }
}

@Composable
private fun StatusLine(lesson: Lesson, access: LessonAccess) {
    Row(
        Modifier.padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val accent = if (access.isPlayable) VintageColors.sageDark else VintageColors.inkMuted
        Text(
            text = stringResource(access.markedBadgeRes).uppercase(),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(accent.copy(alpha = 0.12f))
                .padding(horizontal = 9.dp, vertical = 3.dp),
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )
        Text(
            text = lesson.duration,
            color = VintageColors.inkMuted,
            fontSize = 11.sp,
        )
    }
}

/** Paper card holding a numbered list — used for both "you'll learn" and practice tips. */
@Composable
private fun NumberedCard(
    marker: String,
    markerColor: Color,
    markerSize: androidx.compose.ui.unit.TextUnit,
    title: String,
    items: List<String>,
    serifNumbers: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(VintageColors.paperWarm.copy(alpha = 0.6f))
            .border(
                width = 1.dp,
                color = VintageColors.inkMuted.copy(alpha = 0.16f),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = marker, color = markerColor, fontSize = markerSize)
            OverlineLabel(text = title)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEachIndexed { index, item ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (serifNumbers) "%02d".format(index + 1) else "${index + 1}",
                        modifier = if (serifNumbers) Modifier.width(22.dp) else Modifier,
                        color = VintageColors.sageDark,
                        fontFamily = if (serifNumbers) FontFamily.Serif else FontFamily.SansSerif,
                        fontWeight = if (serifNumbers) FontWeight.SemiBold else FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = item,
                        color = VintageColors.inkSoft,
                        fontSize = if (serifNumbers) 14.sp else 13.sp,
                        lineHeight = if (serifNumbers) 20.sp else 19.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun FunFactCard(fact: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        VintageColors.sageDark.copy(alpha = 0.08f),
                        VintageColors.paperWarm.copy(alpha = 0.3f),
                    )
                )
            )
            .border(
                width = 1.dp,
                color = VintageColors.sageDark.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "🎋", fontSize = 14.sp)
            OverlineLabel(text = stringResource(R.string.lesson_fun_fact))
        }
        Text(
            text = fact,
            color = VintageColors.inkSoft,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        )
    }
}
