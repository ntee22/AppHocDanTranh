package vn.hocdantranh.guzheng.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import vn.hocdantranh.guzheng.ui.theme.VintageColors

/** Accent ramp for a lesson tile. Port of iOS `LessonAccent`. */
enum class LessonAccent(val colors: List<Color>) {
    SAGE(listOf(VintageColors.sageSoft, VintageColors.sageDark)),
    WOOD(listOf(VintageColors.woodSoft, VintageColors.wood)),
    PLUM(listOf(VintageColors.plumSoft, VintageColors.plum));

    companion object {
        /**
         * The exact 16-entry cycle iOS uses, indexed by `(lessonNumber - 1) % 16`. It is
         * deliberately irregular — a plain 3-colour rotation would put the same accent beside
         * itself at different points in the list.
         */
        private val CYCLE = listOf(
            SAGE, WOOD, SAGE, PLUM, WOOD, SAGE, PLUM, WOOD,
            SAGE, PLUM, WOOD, SAGE, PLUM, WOOD, SAGE, PLUM,
        )

        fun forLesson(lessonNumber: Int): LessonAccent =
            CYCLE[Math.floorMod(lessonNumber - 1, CYCLE.size)]
    }
}

/**
 * Number and title sizes as a fraction of tile width. iOS uses 0.36/0.10, but the tile is only
 * 16:10 — at those ratios a wrapped two-line title collides with the number. Trimmed until both
 * fit inside the tile at every lesson number.
 */
private const val NUMBER_SIZE_RATIO = 0.28f
private const val TITLE_SIZE_RATIO = 0.095f

/**
 * Text sized in dp rather than sp. The tile is a fixed-size graphic, so its labels must not
 * grow with the system font-size setting — at large scales they would overflow the panel.
 */
@Composable
private fun fixedSp(dp: Float) = with(LocalDensity.current) { dp.dp.toSp() }

/**
 * Magazine-style lesson tile: a 16:10 gradient panel with wood-grain strokes, the lesson
 * number in italic serif, the English title along the bottom, and a lock badge when the
 * lesson has not been released. Port of iOS `LessonThumb`.
 */
@Composable
fun LessonThumb(
    number: String,
    titleEn: String,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    accent: LessonAccent = LessonAccent.SAGE,
    lockedContentDescription: String? = null,
) {
    val density = LocalDensity.current
    var widthDp by remember { mutableStateOf(0f) }

    Box(
        modifier
            .aspectRatio(16f / 10f)
            .onSizeChanged { widthDp = with(density) { it.width.toDp().value } }
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(accent.colors))
    ) {
        WoodGrain()

        if (number.isNotEmpty() && widthDp > 0f) {
            Text(
                text = number,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = (widthDp * 0.10f).dp, top = (widthDp * 0.04f).dp),
                color = Color.White.copy(alpha = 0.85f),
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                fontSize = fixedSp(widthDp * NUMBER_SIZE_RATIO),
                lineHeight = fixedSp(widthDp * NUMBER_SIZE_RATIO * 1.1f),
            )
        }

        if (titleEn.isNotEmpty() && widthDp > 0f) {
            Text(
                text = titleEn.uppercase(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = (widthDp * 0.10f).dp,
                        end = (widthDp * 0.10f).dp,
                        bottom = (widthDp * 0.05f).dp,
                    ),
                color = Color.White.copy(alpha = 0.90f),
                fontWeight = FontWeight.SemiBold,
                fontSize = fixedSp(maxOf(7.5f, widthDp * TITLE_SIZE_RATIO)),
                lineHeight = fixedSp(maxOf(9f, widthDp * TITLE_SIZE_RATIO * 1.15f)),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (locked) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(VintageColors.inkDark.copy(alpha = 0.55f))
                    .then(
                        if (lockedContentDescription != null) {
                            Modifier.semantics { contentDescription = lockedContentDescription }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

/** Six faint curved strokes suggesting wood grain across the tile. */
@Composable
private fun WoodGrain() {
    Canvas(Modifier.fillMaxSize()) {
        repeat(6) { i ->
            val y = size.height * (0.20f + i * 0.14f)
            val path = Path().apply {
                moveTo(0f, y)
                quadraticTo(
                    size.width * 0.5f, y - size.height * 0.01f,
                    size.width, y + size.height * 0.02f,
                )
            }
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.10f),
                style = Stroke(width = 0.8.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}
