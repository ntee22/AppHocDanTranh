package vn.hocdantranh.guzheng.ui.launch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.hocdantranh.guzheng.ui.components.BrushDivider
import vn.hocdantranh.guzheng.ui.components.InkSeal
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

// Hex values copied from iOS LaunchScreen.swift.
private val WoodNight = Color(0xFF2A2520)
private val Gold = Color(0xFFC9A961)
private val Paper = Color(0xFFF2E8D5)
private val Ink = Color(0xFF3A3228)
private val InkMuted = Color(0xFF8A7B66)
private val WoodEdge = Color(0xFF8B6F4E)

/** Timings from iOS: 2.6 s unroll, logo at 55 %, text at 75 %. */
const val LAUNCH_UNROLL_MS = 2_600
private const val LOGO_AT_MS = 1_430L
private const val TEXT_AT_MS = 1_950L

private const val PAPER_W = 320f
private const val PAPER_H = 380f
private const val ROLLER_W = 18f

/**
 * Paper-scroll unroll shown at launch. Port of iOS `LaunchScreen`: two wooden rollers move
 * apart revealing the paper, then 琴, the brush divider, "Đan Thanh", the tagline and the
 * 樂 seal fade in, while three gold dots breathe below.
 */
@Composable
fun LaunchScreen(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    var showContent by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch { progress.animateTo(1f, tween(LAUNCH_UNROLL_MS, easing = FastOutSlowInEasing)) }
        launch { delay(LOGO_AT_MS); showContent = true }
        launch { delay(TEXT_AT_MS); showText = true }
    }

    val contentAlpha by animateFloatAsState(if (showContent) 1f else 0f, tween(420), label = "logo")
    val contentShift by animateFloatAsState(if (showContent) 0f else 8f, tween(420), label = "logoShift")
    val textAlpha by animateFloatAsState(if (showText) 1f else 0f, tween(420), label = "text")

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(WoodNight)
            .background(
                Brush.radialGradient(
                    listOf(Gold.copy(alpha = 0.12f), WoodNight.copy(alpha = 0f)),
                    radius = 300f * 3,
                )
            ),
    ) {
        // iOS assumes ≥ 375pt. Fully open, the rollers span 356dp, so shrink on narrower
        // screens instead of clipping the rollers.
        val fit = min(1f, (maxWidth.value - 16f) / (PAPER_W + ROLLER_W * 2))

        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))

            Box(
                Modifier
                    .requiredSize(PAPER_W.dp, PAPER_H.dp)
                    .graphicsLayer { scaleX = fit; scaleY = fit },
                contentAlignment = Alignment.Center,
            ) {
                val p = progress.value
                val paperW = maxOf(1f, PAPER_W * p)

                // Paper with its shadow.
                Box(
                    Modifier
                        .size(paperW.dp, PAPER_H.dp)
                        .shadow(30.dp, ambientColor = Color.Black, spotColor = Color.Black)
                        .background(Paper)
                        .clipToBounds(),
                    contentAlignment = Alignment.Center,
                ) {
                    // Top/bottom edge shading.
                    Column(Modifier.fillMaxSize()) {
                        Box(
                            Modifier.fillMaxWidth().height(4.dp)
                                .background(Brush.verticalGradient(listOf(WoodEdge.copy(alpha = 0.25f), Color.Transparent)))
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier.fillMaxWidth().height(4.dp)
                                .background(Brush.verticalGradient(listOf(Color.Transparent, WoodEdge.copy(alpha = 0.25f))))
                        )
                    }

                    // Content is laid out at full paper width and revealed by the clip,
                    // as iOS's .clipped() does.
                    Column(
                        Modifier
                            .requiredSize(PAPER_W.dp, PAPER_H.dp)
                            .graphicsLayer {
                                alpha = contentAlpha
                                translationY = contentShift * density
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                    ) {
                        Text("琴", color = Ink, fontFamily = FontFamily.Serif, fontSize = 96.sp, lineHeight = 110.sp)
                        BrushDivider(Modifier.width(100.dp))
                        Text(
                            "Đan Thanh",
                            modifier = Modifier.graphicsLayer { alpha = textAlpha },
                            color = Ink,
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            fontSize = 28.sp,
                        )
                        Text(
                            "HỌC ĐÀN TRANH · GUZHENG",
                            modifier = Modifier.graphicsLayer { alpha = textAlpha },
                            color = InkMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 3.sp,
                        )
                        InkSeal(char = "樂", size = 24.dp, rotate = -4f, modifier = Modifier.graphicsLayer { alpha = textAlpha })
                    }
                }

                // Rollers track the paper's edges outward.
                ScrollRoller(left = true, modifier = Modifier.offset(x = (-(PAPER_W / 2 * p + ROLLER_W / 2)).dp))
                ScrollRoller(left = false, modifier = Modifier.offset(x = (PAPER_W / 2 * p + ROLLER_W / 2).dp))
            }

            Spacer(Modifier.weight(1f))
            BreathingDots(Modifier.padding(bottom = 60.dp))
        }
    }
}

@Composable
private fun ScrollRoller(left: Boolean, modifier: Modifier = Modifier) {
    val wood = listOf(
        Color(0xFF6B5538), Color(0xFF8B6F4E), Color(0xFFA88860), Color(0xFF8B6F4E),
        if (left) Color(0xFF5A4428) else Color(0xFF6B5538),
    )
    Box(modifier.requiredSize(ROLLER_W.dp, PAPER_H.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .shadow(10.dp, CircleShape)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(if (left) wood else wood.reversed()))
                .border(1.dp, Gold.copy(alpha = 0.2f), RoundedCornerShape(50))
        )
        for (y in listOf(-185f, 185f)) {
            Box(
                Modifier
                    .offset(y = y.dp)
                    .requiredSize((ROLLER_W + 6).dp, 10.dp)
                    .shadow(4.dp, RoundedCornerShape(3.dp))
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.linearGradient(listOf(Gold, WoodEdge)))
            )
        }
    }
}

@Composable
private fun BreathingDots(modifier: Modifier = Modifier) {
    val phase by rememberInfiniteTransition(label = "dots").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "dotPhase",
    )
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            val scale = 1f + 0.3f * sin(((phase + i * 0.333f) * 2 * PI).toFloat())
            Box(
                Modifier
                    .size(5.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; alpha = 0.6f }
                    .clip(CircleShape)
                    .background(Gold)
            )
        }
    }
}
