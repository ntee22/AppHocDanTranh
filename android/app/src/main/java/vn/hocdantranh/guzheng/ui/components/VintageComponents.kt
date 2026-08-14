package vn.hocdantranh.guzheng.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import vn.hocdantranh.guzheng.ui.theme.VintageColors

/** The seal's character colour — iOS `Color(hex: "F5E8D0")`, a shade lighter than [VintageColors.paper]. */
private val SealInk = Color(0xFFF5E8D0)

/**
 * Full-screen vintage paper ground with warm corner vignettes.
 * Port of iOS `PaperBackground`.
 */
@Composable
fun PaperBackground(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(VintageColors.paper)
            .background(
                Brush.radialGradient(
                    colors = listOf(VintageColors.wood.copy(alpha = 0.06f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset.Zero,
                    radius = 800f,
                )
            )
    )
}

/**
 * Small uppercase label with wide tracking, used above section titles.
 * Port of iOS `OverlineLabel` (10pt bold, 2.5 kerning).
 */
@Composable
fun OverlineLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = VintageColors.inkMuted,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.5.sp,
    )
}

/**
 * Red square seal stamp bearing a Chinese character, tilted slightly off-axis.
 * Port of iOS `InkSeal`.
 */
@Composable
fun InkSeal(
    char: String = "琴",
    size: Dp = 32.dp,
    rotate: Float = -4f,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(size)
            .rotate(rotate)
            .clip(RoundedCornerShape(size * 0.12f))
            .background(VintageColors.sealRed),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = char,
            color = SealInk,
            fontSize = (size.value * 0.60f).sp,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Soft brush-stroke divider that fades out at both ends.
 * Port of iOS `BrushDivider`.
 */
@Composable
fun BrushDivider(
    modifier: Modifier = Modifier,
    alpha: Float = 0.35f,
) {
    Box(
        modifier
            .height(1.2.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        VintageColors.inkMuted.copy(alpha = 0.5f * alpha),
                        VintageColors.inkSoft.copy(alpha = 0.6f * alpha),
                        VintageColors.inkMuted.copy(alpha = 0.5f * alpha),
                        Color.Transparent,
                    )
                )
            )
    )
}

/**
 * The `· 琴 ·` mark that closes the lessons list.
 */
@Composable
fun ScrollEndMark(modifier: Modifier = Modifier) {
    Text(
        text = "· 琴 ·",
        modifier = modifier.wrapContentSize(),
        color = VintageColors.inkMuted.copy(alpha = 0.6f),
        fontFamily = FontFamily.Serif,
        fontSize = 24.sp,
    )
}
