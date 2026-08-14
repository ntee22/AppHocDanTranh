package vn.hocdantranh.guzheng.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vn.hocdantranh.guzheng.ui.theme.VintageColors

/** Blossom placement as (x fraction, y fraction, radius in px). Radii are absolute, as on iOS. */
private val Blossoms = listOf(
    Triple(0.175f, 0.80f, 2.4f),
    Triple(0.375f, 0.625f, 2.1f),
    Triple(0.525f, 0.525f, 2.6f),
    Triple(0.675f, 0.40f, 2.2f),
    Triple(0.825f, 0.25f, 2.5f),
    Triple(0.90f, 0.125f, 1.8f),
)

private val BlossomCentre = Color(0xFFF5E8D0)

/**
 * Decorative plum-blossom branch that sits in the masthead's top-right corner.
 * Port of iOS `PlumBranch` — a single stroked branch rising left-to-right with six
 * two-tone blossoms along it.
 */
@Composable
fun PlumBranch(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    color: Color = VintageColors.inkMuted,
) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val branch = Path().apply {
            moveTo(w * 0.075f, h * 0.90f)
            quadraticTo(w * 0.25f, h * 0.69f, w * 0.40f, h * 0.60f)
            quadraticTo(w * 0.575f, h * 0.50f, w * 0.725f, h * 0.35f)
            quadraticTo(w * 0.85f, h * 0.225f, w * 0.925f, h * 0.10f)
        }
        drawPath(
            path = branch,
            color = color.copy(alpha = 0.45f),
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round),
        )

        Blossoms.forEach { (xf, yf, r) ->
            val centre = Offset(w * xf, h * yf)
            // Radii are points on iOS, so they must convert through density here — as raw
            // pixels they render sub-dp and vanish on a high-density screen.
            val radius = r.dp.toPx()
            drawCircle(
                color = VintageColors.sealRed.copy(alpha = 0.75f),
                radius = radius,
                center = centre,
            )
            drawCircle(
                color = BlossomCentre.copy(alpha = 0.70f),
                radius = radius * 0.4f,
                center = centre,
            )
        }
    }
}
