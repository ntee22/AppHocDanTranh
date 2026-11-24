package vn.hocdantranh.guzheng.ui.tuner

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.prefs.AppPrefs
import vn.hocdantranh.guzheng.data.review.ReviewPrompter
import vn.hocdantranh.guzheng.tuner.GUZHENG_STRINGS
import vn.hocdantranh.guzheng.tuner.GuzhengString
import vn.hocdantranh.guzheng.tuner.TunerState
import vn.hocdantranh.guzheng.tuner.guzhengString
import vn.hocdantranh.guzheng.ui.components.InkSeal
import vn.hocdantranh.guzheng.ui.components.OverlineLabel
import vn.hocdantranh.guzheng.ui.components.PaperBackground
import vn.hocdantranh.guzheng.ui.theme.VintageColors

// Off-palette colours the iOS tuner hard-codes.
private val StringCream = Color(0xFFF2E8D5)
private val StringGreen = Color(0xFF8FBF7A)
private val MeterTrack = Color(0xFFEFE0C7)
private val Backdrop = Color(0xFF2A2520)

/** Needle travel per cent; the meter shows ±40¢. Same scale as iOS `CentsMeter`. */
private const val DP_PER_CENT = 1.8f

/**
 * The Tuner tab wired to its view model: microphone permission, start/stop with the
 * screen's lifecycle, match feedback and the first-run guide. Port of iOS `TunerView`.
 */
@Composable
fun TunerRoute(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val viewModel: TunerViewModel = viewModel(factory = TunerViewModel.factory(app))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val prefs = remember { AppPrefs(app) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val reviewPrompter = remember { ReviewPrompter(app) }

    var hasMic by remember { mutableStateOf(context.hasMicPermission()) }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMic = granted
        askedOnce = true
    }

    // Listen only while the tab is on screen and the app is in the foreground — iOS
    // starts in onAppear and stops in onDisappear.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, hasMic) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    // Re-read: the user may have granted it in system settings meanwhile.
                    hasMic = context.hasMicPermission()
                    if (hasMic) viewModel.startIfPermitted(context)
                }
                Lifecycle.Event.ON_STOP -> viewModel.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stop()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasMic && !askedOnce) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }
    LaunchedEffect(viewModel) {
        viewModel.matches.collect {
            view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
                else HapticFeedbackConstants.LONG_PRESS
            )
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            context.findActivity()?.let(reviewPrompter::recordPositiveAction)
        }
    }

    // First-run guide: shown after a short beat, like iOS's 0.4 s delay.
    var showGuide by rememberSaveable { mutableStateOf(false) }
    var guideChecked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val seen = prefs.tunerGuideSeen.first()
        if (!seen && !guideChecked) {
            delay(400)
            showGuide = true
        }
        guideChecked = true
    }

    TunerScreen(
        state = state,
        hasMic = hasMic,
        showGuide = showGuide,
        onStringTap = viewModel::selectString,
        onHelp = { showGuide = true },
        onGrantMic = {
            if (askedOnce && context.findActivity()?.shouldShowRationale() == false) {
                context.openAppSettings()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onGuideStart = { showGuide = false },
        onGuideNeverShow = {
            showGuide = false
            scope.launch { prefs.setTunerGuideSeen() }
        },
        modifier = modifier,
    )
}

@Composable
fun TunerScreen(
    state: TunerState,
    hasMic: Boolean,
    showGuide: Boolean,
    onStringTap: (Int) -> Unit,
    onHelp: () -> Unit,
    onGrantMic: () -> Unit,
    onGuideStart: () -> Unit,
    onGuideNeverShow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val blur by animateFloatAsState(if (showGuide) 6f else 0f, tween(280), label = "guideBlur")

    Box(modifier.fillMaxSize()) {
        PaperBackground()

        Column(
            Modifier
                .fillMaxSize()
                .blur(blur.dp)
        ) {
            Header(isSounding = state.isSounding, onHelp = onHelp)

            if (hasMic) {
                ActiveStringCard(state)
            } else {
                MicPermissionCard(onGrantMic)
            }

            // ── String list header ──
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OverlineLabel(text = stringResource(R.string.tuner_strings_overline))
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.tuner_high_marker),
                    color = VintageColors.inkMuted.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            StringList(
                activeStringId = state.activeStringId,
                onStringTap = onStringTap,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp),
            )

            // ── Legend ──
            Row(
                Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 6.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(width = 18.dp, height = 2.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(StringGreen)
                )
                Text(
                    text = stringResource(R.string.tuner_legend),
                    color = VintageColors.inkMuted,
                    fontSize = 10.sp,
                )
            }
        }

        AnimatedVisibility(
            visible = showGuide,
            enter = fadeIn(tween(280)),
            exit = fadeOut(tween(250)),
        ) {
            TunerGuidePopup(
                onStart = onGuideStart,
                onNeverShow = onGuideNeverShow,
                onBackdrop = onGuideStart,
            )
        }
    }
}

// ── Header ──

@Composable
private fun Header(isSounding: Boolean, onHelp: () -> Unit) {
    val narrow = LocalConfiguration.current.screenWidthDp < 360
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            OverlineLabel(text = stringResource(R.string.tuner_overline))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = VintageColors.inkDark, fontWeight = FontWeight.Medium)) {
                        append(stringResource(R.string.tuner_title))
                    }
                    append(" ")
                    withStyle(
                        SpanStyle(
                            color = VintageColors.sageDark,
                            fontWeight = FontWeight.Light,
                            fontStyle = FontStyle.Italic,
                        )
                    ) {
                        // Non-breaking: on narrow screens the title wraps before
                        // "Đàn Tranh", never inside it.
                        append("Đàn\u00A0Tranh")
                    }
                },
                fontFamily = FontFamily.Serif,
                // iOS's 32pt assumes ≥ 375pt of width. Narrow or zoomed Android screens
                // (320dp on the OPPO at its display size) would split "Đàn Tranh".
                fontSize = if (narrow) 26.sp else 32.sp,
                lineHeight = if (narrow) 30.sp else 36.sp,
            )
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(VintageColors.sageDark.copy(alpha = 0.12f))
                        .border(1.dp, VintageColors.sageDark.copy(alpha = 0.3f), CircleShape)
                        .clickable(onClick = onHelp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "?",
                        color = VintageColors.sageDark,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                }
                Text(
                    text = "Tone D",
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(VintageColors.sageDark.copy(alpha = 0.12f))
                        .border(1.dp, VintageColors.sageDark.copy(alpha = 0.3f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    color = VintageColors.sageDark,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                val dot by animateColorAsState(
                    if (isSounding) VintageColors.sageDark else VintageColors.inkMuted.copy(alpha = 0.3f),
                    tween(300),
                    label = "listenDot",
                )
                Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
                Text(
                    text = stringResource(
                        if (isSounding) R.string.tuner_listening else R.string.tuner_waiting
                    ),
                    color = VintageColors.inkMuted,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

// ── Active string card ──

@Composable
private fun ActiveStringCard(state: TunerState) {
    val active = guzhengString(state.activeStringId)
    val (status, statusColor) = statusOf(state)
    val nameColor by animateColorAsState(
        if (state.isInTune && state.isSounding) VintageColors.sageDark else VintageColors.inkDark,
        tween(300),
        label = "nameColor",
    )

    Column(
        Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        VintageColors.paperWarm.copy(alpha = 0.9f),
                        VintageColors.paperDeep.copy(alpha = 0.85f),
                    )
                )
            )
            .border(1.dp, VintageColors.inkMuted.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Light)) {
                        append(stringResource(R.string.tuner_string_prefix))
                    }
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium, fontStyle = FontStyle.Italic)) {
                        append("${active.id}")
                    }
                },
                color = nameColor,
                fontFamily = FontFamily.Serif,
                fontSize = 28.sp,
            )
            Text(
                text = active.label,
                modifier = Modifier.padding(start = 6.dp),
                color = VintageColors.inkSoft,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            )
            Text(
                text = "%.1f Hz".format(active.frequency),
                modifier = Modifier.padding(start = 4.dp),
                color = VintageColors.inkMuted.copy(alpha = 0.7f),
                fontSize = 11.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = status,
                color = statusColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
            )
        }

        CentsMeter(cents = state.cents, isInTune = state.isInTune, isSounding = state.isSounding)
    }
}

/** Status line text and colour — iOS `statusText` / `statusColor`. */
@Composable
private fun statusOf(state: TunerState): Pair<String, Color> = when {
    state.isPlayingReference -> stringResource(R.string.tuner_status_reference) to VintageColors.sageDark
    state.isMatched -> stringResource(R.string.tuner_status_locked) to VintageColors.sageDark
    !state.isSounding -> "" to VintageColors.inkMuted
    state.isInTune -> stringResource(R.string.tuner_status_in_tune) to VintageColors.sageDark
    state.cents < 0 ->
        stringResource(R.string.tuner_status_tighten, kotlin.math.abs(state.cents.toInt())) to VintageColors.sealRed
    else -> stringResource(R.string.tuner_status_loosen, state.cents.toInt()) to VintageColors.sealRed
}

@Composable
private fun MicPermissionCard(onGrant: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VintageColors.paperWarm.copy(alpha = 0.9f))
            .border(1.dp, VintageColors.inkMuted.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.tuner_mic_title),
            color = VintageColors.inkDark,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
        )
        Text(
            text = stringResource(R.string.tuner_mic_body),
            color = VintageColors.inkSoft,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        Text(
            text = stringResource(R.string.tuner_mic_button),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(VintageColors.sageDark)
                .clickable(onClick = onGrant)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = VintageColors.paper,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ── Cents meter ──

@Composable
private fun CentsMeter(cents: Double, isInTune: Boolean, isSounding: Boolean) {
    val needleColor = if (isInTune) VintageColors.sageDark else VintageColors.sealRed
    val clamped = cents.coerceIn(-40.0, 40.0).toFloat()
    val offset by animateFloatAsState(
        targetValue = clamped * DP_PER_CENT,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 1700f),
        label = "needle",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .alpha(if (isSounding) 1f else 0.45f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .background(MeterTrack.copy(alpha = 0.7f))
        )
        Box(
            Modifier
                .size(width = 20.dp, height = 48.dp)
                .background(VintageColors.sageDark.copy(alpha = 0.12f))
        )
        for (v in listOf(-40, -20, 0, 20, 40)) {
            Box(
                Modifier
                    .offset(x = (v * DP_PER_CENT).dp)
                    .size(width = if (v == 0) 2.dp else 1.dp, height = 36.dp)
                    .background(
                        if (v == 0) VintageColors.sageDark else VintageColors.inkMuted.copy(alpha = 0.3f)
                    )
            )
        }
        if (isSounding) {
            Box(
                Modifier
                    .offset(x = offset.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(1.5.dp),
                        ambientColor = needleColor,
                        spotColor = needleColor,
                    )
                    .size(width = 3.dp, height = 38.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(needleColor)
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .offset(y = 14.dp),
        ) {
            Text(stringResource(R.string.tuner_flat), color = VintageColors.inkMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.tuner_sharp), color = VintageColors.inkMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── String list ──

@Composable
private fun StringList(
    activeStringId: Int,
    onStringTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Same order as iOS: GUZHENG_STRINGS.reversed().
    val strings = remember { GUZHENG_STRINGS.asReversed() }
    val listState = rememberLazyListState()

    // iOS fits all 21 rows on any iPhone. Narrow or zoomed Android screens cannot, so the
    // list scrolls there — and follows the detected string so it never sits off-screen.
    LaunchedEffect(activeStringId) {
        val index = strings.indexOfFirst { it.id == activeStringId }
        val visible = listState.layoutInfo.visibleItemsInfo
        val fullyVisible = visible.any { it.index == index } &&
            visible.first().index < index && visible.last().index > index
        if (index >= 0 && !fullyVisible) {
            val rows = visible.size.coerceAtLeast(1)
            listState.animateScrollToItem((index - rows / 2).coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(10.dp), ambientColor = VintageColors.inkDark, spotColor = VintageColors.inkDark)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(VintageColors.woodDark, VintageColors.wood.copy(alpha = 0.85f), VintageColors.woodDark)
                )
            ),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        items(strings, key = { it.id }) { string ->
            StringRow(
                string = string,
                isActive = string.id == activeStringId,
                onTap = { onStringTap(string.id) },
            )
        }
    }
}

@Composable
private fun StringRow(string: GuzhengString, isActive: Boolean, onTap: () -> Unit) {
    val breath = rememberInfiniteTransition(label = "breath")
    val breathScale by breath.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathScale",
    )
    val breathAlpha by breath.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathAlpha",
    )
    val textColor = if (isActive) StringCream else StringCream.copy(alpha = 0.5f)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) VintageColors.sageDark.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onTap)
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${string.id}",
            modifier = Modifier.width(24.dp),
            color = textColor,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        )
        Box(Modifier.weight(1f).height(8.dp), contentAlignment = Alignment.Center) {
            if (isActive) {
                // Soft glow under the active string (iOS: blurred rectangle, breathing).
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(if (string.isGreen) 4.dp else 3.dp)
                        .graphicsLayer {
                            scaleX = breathScale
                            alpha = breathAlpha
                        }
                        .blur(4.dp)
                        .background(if (string.isGreen) StringGreen else StringCream)
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(if (string.isGreen) 2.5.dp else 1.8.dp)
                    .background(
                        when {
                            string.isGreen -> StringGreen
                            isActive -> Color.White
                            else -> StringCream.copy(alpha = 0.7f)
                        }
                    )
            )
        }
        Text(
            text = string.label,
            modifier = Modifier.width(24.dp),
            color = textColor,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontSize = 12.sp,
            lineHeight = 15.sp,
        )
    }
}

// ── First-run guide ──

private data class GuideStep(val icon: String, val title: Int, val body: Int)

private val GUIDE_STEPS = listOf(
    GuideStep("🎧", R.string.tuner_guide_step1_title, R.string.tuner_guide_step1_body),
    GuideStep("↕", R.string.tuner_guide_step2_title, R.string.tuner_guide_step2_body),
    GuideStep("☝", R.string.tuner_guide_step3_title, R.string.tuner_guide_step3_body),
    GuideStep("↺", R.string.tuner_guide_step4_title, R.string.tuner_guide_step4_body),
)

/** iOS `TunerGuidePopup`: dimmed backdrop, card flips in about the X axis. */
@Composable
private fun TunerGuidePopup(onStart: () -> Unit, onNeverShow: () -> Unit, onBackdrop: () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val progress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(420, easing = androidx.compose.animation.core.CubicBezierEasing(0.22f, 0.9f, 0.35f, 1f)),
        label = "guideFlip",
    )
    val density = LocalDensity.current

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Backdrop.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBackdrop,
                )
        )
        Column(
            Modifier
                .padding(24.dp)
                .widthIn(max = 320.dp)
                .graphicsLayer {
                    rotationX = 78f * (1f - progress)
                    alpha = progress
                    cameraDistance = 8f * density.density
                }
                .shadow(20.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(VintageColors.paperWarm, VintageColors.paperDeep.copy(alpha = 0.9f))
                    )
                )
                .border(1.dp, VintageColors.inkMuted.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                // Swallow taps so they do not reach the backdrop.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                // Small or zoomed screens: the card scrolls rather than hiding its buttons.
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            InkSeal(char = "調", size = 42.dp, rotate = -3f, modifier = Modifier.padding(bottom = 10.dp))
            Text(
                text = stringResource(R.string.tuner_guide_title),
                color = VintageColors.inkDark,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
            )
            Text(
                text = "How to tune your Đàn Tranh",
                modifier = Modifier.padding(bottom = 16.dp),
                color = VintageColors.inkMuted,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
            )

            GUIDE_STEPS.forEachIndexed { index, step ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(VintageColors.sageDark.copy(alpha = 0.15f))
                            .border(1.dp, VintageColors.sageDark.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(step.icon, color = VintageColors.sageDark, fontSize = 14.sp)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = stringResource(step.title),
                            color = VintageColors.inkDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(step.body),
                            color = VintageColors.inkSoft,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                        )
                    }
                }
                if (index < GUIDE_STEPS.lastIndex) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(VintageColors.woodDark.copy(alpha = 0.12f))
                    )
                }
            }

            Text(
                text = stringResource(R.string.tuner_guide_start),
                modifier = Modifier
                    .padding(top = 22.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(VintageColors.sageDark)
                    .clickable(onClick = onStart)
                    .padding(vertical = 13.dp),
                color = VintageColors.paper,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.tuner_guide_never),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clickable(onClick = onNeverShow)
                    .padding(vertical = 8.dp),
                color = VintageColors.inkMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Helpers ──

private fun Context.hasMicPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Activity.shouldShowRationale(): Boolean =
    shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
