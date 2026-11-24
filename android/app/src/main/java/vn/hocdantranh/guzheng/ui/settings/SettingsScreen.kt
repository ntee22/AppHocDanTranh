package vn.hocdantranh.guzheng.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import vn.hocdantranh.guzheng.BuildConfig
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.prefs.AppPrefs
import vn.hocdantranh.guzheng.ui.components.InkSeal
import vn.hocdantranh.guzheng.ui.components.OverlineLabel
import vn.hocdantranh.guzheng.ui.components.PaperBackground
import vn.hocdantranh.guzheng.ui.theme.VintageColors

/** Brand colours for the social rows — deliberately off-palette, as on iOS. */
private val FacebookBlue = Color(0xFF1877F2)
private val YouTubeRed = Color(0xFFFF0000)
private val ShopeeOrange = Color(0xFFEE4D2D)

@Composable
fun SettingsRoute(
    language: String,
    onLanguageChange: (String) -> Unit,
    tunerSensitivity: String = AppPrefs.SENSITIVITY_MEDIUM,
    onTunerSensitivityChange: (String) -> Unit = {},
    onRestorePurchases: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val openFailed = stringResource(R.string.settings_open_failed)
    val shareText = stringResource(
        R.string.settings_share_text,
        SettingsLinks.playStoreWebUrl(),
    )
    val shareChooser = stringResource(R.string.settings_share_chooser)
    val emailSubject = stringResource(R.string.settings_feedback_subject)

    SettingsScreen(
        language = language,
        versionName = BuildConfig.VERSION_NAME,
        onLanguageChange = onLanguageChange,
        tunerSensitivity = tunerSensitivity,
        onTunerSensitivityChange = onTunerSensitivityChange,
        onRestorePurchases = onRestorePurchases,
        onOpenUrl = { url -> context.openUrl(url, openFailed) },
        onRate = { context.openPlayStore(openFailed) },
        onShare = { context.shareText(shareText, shareChooser, openFailed) },
        onEmail = { context.sendEmail(SettingsLinks.EMAIL, emailSubject, openFailed) },
    )
}

@Composable
fun SettingsScreen(
    language: String,
    versionName: String,
    onLanguageChange: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onRate: () -> Unit,
    onShare: () -> Unit,
    onEmail: () -> Unit,
    tunerSensitivity: String = AppPrefs.SENSITIVITY_MEDIUM,
    onTunerSensitivityChange: (String) -> Unit = {},
    onRestorePurchases: (() -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize()) {
        PaperBackground()

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp)
        ) {
            Header()

            LanguageSelector(
                language = language,
                onLanguageChange = onLanguageChange,
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 24.dp),
            )

            SettingsSection(title = stringResource(R.string.settings_section_about)) {
                BioCard()
                SettingsRow(
                    icon = Icons.Filled.ThumbUp,
                    iconColor = FacebookBlue,
                    title = stringResource(R.string.settings_facebook_title),
                    subtitle = stringResource(R.string.settings_facebook_subtitle),
                    onClick = { onOpenUrl(SettingsLinks.FACEBOOK) },
                )
                SettingsRow(
                    icon = Icons.Filled.MusicNote,
                    iconColor = VintageColors.inkDark,
                    title = stringResource(R.string.settings_tiktok_title),
                    subtitle = stringResource(R.string.settings_tiktok_subtitle),
                    onClick = { onOpenUrl(SettingsLinks.TIKTOK) },
                )
                SettingsRow(
                    icon = Icons.Filled.PlayCircle,
                    iconColor = YouTubeRed,
                    title = stringResource(R.string.settings_youtube_title),
                    subtitle = stringResource(R.string.settings_youtube_subtitle),
                    onClick = { onOpenUrl(SettingsLinks.YOUTUBE) },
                )
                SettingsRow(
                    icon = Icons.Filled.ShoppingCart,
                    iconColor = ShopeeOrange,
                    title = stringResource(R.string.settings_shopee_title),
                    subtitle = stringResource(R.string.settings_shopee_subtitle),
                    onClick = { onOpenUrl(SettingsLinks.SHOPEE) },
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_app)) {
                SettingsRow(
                    icon = Icons.Filled.Star,
                    iconColor = VintageColors.vintageGold,
                    title = stringResource(R.string.settings_rate_title),
                    subtitle = stringResource(R.string.settings_rate_subtitle),
                    onClick = onRate,
                )
                SettingsRow(
                    icon = Icons.Filled.Share,
                    iconColor = VintageColors.sageDark,
                    title = stringResource(R.string.settings_share_title),
                    subtitle = stringResource(R.string.settings_share_subtitle),
                    onClick = onShare,
                )
                SettingsRow(
                    icon = Icons.Filled.Email,
                    iconColor = VintageColors.inkMuted,
                    title = stringResource(R.string.settings_feedback_title),
                    subtitle = stringResource(R.string.settings_feedback_subtitle),
                    onClick = onEmail,
                )
                if (onRestorePurchases != null) {
                    SettingsRow(
                        icon = Icons.Filled.Restore,
                        iconColor = VintageColors.sageDark,
                        title = stringResource(R.string.purchase_restore),
                        subtitle = stringResource(R.string.settings_restore_subtitle),
                        onClick = onRestorePurchases,
                    )
                }
            }

            // Same position as iOS: between "App" and "Info".
            SettingsSection(title = stringResource(R.string.settings_section_tuner)) {
                TunerSensitivitySelector(
                    selected = tunerSensitivity,
                    onSelect = onTunerSensitivityChange,
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_info)) {
                VersionRow(versionName = versionName)
            }

            Footer()
        }
    }
}

@Composable
private fun Header() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            OverlineLabel(text = stringResource(R.string.settings_overline))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = VintageColors.inkDark)) {
                        append(stringResource(R.string.settings_header_title))
                    }
                    withStyle(
                        SpanStyle(
                            color = VintageColors.sageDark,
                            fontStyle = FontStyle.Italic,
                        )
                    ) {
                        append(stringResource(R.string.settings_header_accent))
                    }
                },
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
            )
        }
        InkSeal(char = "設", size = 32.dp)
    }
}

@Composable
private fun LanguageSelector(
    language: String,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OverlineLabel(
            text = stringResource(R.string.settings_language_overline),
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(VintageColors.paperWarm.copy(alpha = 0.55f))
                .border(
                    1.dp,
                    VintageColors.inkMuted.copy(alpha = 0.18f),
                    RoundedCornerShape(12.dp),
                ),
        ) {
            LanguageButton(
                flag = "🇻🇳",
                label = stringResource(R.string.settings_language_vi),
                selected = language == "vi",
                onClick = { onLanguageChange("vi") },
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .width(1.dp)
                    .height(72.dp)
                    .background(VintageColors.inkMuted.copy(alpha = 0.2f))
            )
            LanguageButton(
                flag = "🇬🇧",
                label = stringResource(R.string.settings_language_en),
                selected = language == "en",
                onClick = { onLanguageChange("en") },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LanguageButton(
    flag: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        targetValue = if (selected) VintageColors.sageDark else Color.Transparent,
        label = "lang-bg",
    )
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = flag, fontSize = 28.sp)
        Text(
            text = label,
            color = if (selected) VintageColors.paper else VintageColors.inkSoft,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontFamily = FontFamily.Serif,
        )
    }
}

/** iOS "Độ nhạy tuner": three-way segmented control plus a hint for the chosen tier. */
@Composable
private fun TunerSensitivitySelector(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        AppPrefs.SENSITIVITY_LOW to R.string.settings_sensitivity_low,
        AppPrefs.SENSITIVITY_MEDIUM to R.string.settings_sensitivity_medium,
        AppPrefs.SENSITIVITY_HIGH to R.string.settings_sensitivity_high,
    )
    val hint = when (selected) {
        AppPrefs.SENSITIVITY_LOW -> R.string.settings_sensitivity_hint_low
        AppPrefs.SENSITIVITY_HIGH -> R.string.settings_sensitivity_hint_high
        else -> R.string.settings_sensitivity_hint_medium
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VintageColors.sageDark.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Tune, contentDescription = null, tint = VintageColors.sageDark, modifier = Modifier.size(19.dp))
            }
            Text(
                text = stringResource(R.string.settings_tuner_sensitivity),
                color = VintageColors.inkDark,
                fontSize = 17.sp,
                fontFamily = FontFamily.Serif,
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(10.dp))
                .background(VintageColors.paperWarm.copy(alpha = 0.55f))
                .border(1.dp, VintageColors.inkMuted.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
        ) {
            options.forEachIndexed { index, (value, label) ->
                if (index > 0) {
                    Box(
                        Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(VintageColors.inkMuted.copy(alpha = 0.2f))
                    )
                }
                val isSelected = value == selected
                val bg by animateColorAsState(
                    if (isSelected) VintageColors.sageDark else Color.Transparent,
                    label = "sensitivityBg",
                )
                Text(
                    text = stringResource(label),
                    modifier = Modifier
                        .weight(1f)
                        .background(bg)
                        .clickable { onSelect(value) }
                        .padding(vertical = 10.dp),
                    color = if (isSelected) VintageColors.paper else VintageColors.inkSoft,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Text(
            text = stringResource(hint),
            color = VintageColors.inkMuted,
            fontSize = 12.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = 24.dp)) {
        OverlineLabel(
            text = title,
            modifier = Modifier.padding(start = 26.dp, bottom = 10.dp),
        )
        Column(
            Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(VintageColors.paperWarm.copy(alpha = 0.6f))
                .border(
                    1.dp,
                    VintageColors.woodDark.copy(alpha = 0.15f),
                    RoundedCornerShape(12.dp),
                ),
            content = { content() },
        )
    }
}

@Composable
private fun BioCard() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InkSeal(char = "琴", size = 32.dp, rotate = -3f)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.settings_bio_name),
                    color = VintageColors.inkDark,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Serif,
                )
                Text(
                    text = stringResource(R.string.settings_bio_subtitle),
                    color = VintageColors.inkMuted,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Serif,
                )
            }
        }
        Text(
            text = stringResource(R.string.settings_bio_body),
            color = VintageColors.inkSoft,
            fontSize = 15.sp,
            lineHeight = 25.sp,
            fontFamily = FontFamily.Serif,
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = VintageColors.inkDark,
                fontSize = 17.sp,
                fontFamily = FontFamily.Serif,
            )
            Text(
                text = subtitle,
                color = VintageColors.inkMuted,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun VersionRow(versionName: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(VintageColors.inkMuted.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = VintageColors.inkMuted,
                modifier = Modifier.size(19.dp),
            )
        }
        Text(
            text = stringResource(R.string.settings_version),
            color = VintageColors.inkDark,
            fontSize = 17.sp,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = versionName,
            color = VintageColors.inkMuted,
            fontSize = 15.sp,
            fontStyle = FontStyle.Italic,
            fontFamily = FontFamily.Serif,
        )
    }
}

@Composable
private fun Footer() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "· 琴 ·",
            color = VintageColors.inkMuted.copy(alpha = 0.5f),
            fontSize = 24.sp,
            fontFamily = FontFamily.Serif,
        )
        Text(
            text = stringResource(R.string.settings_footer_tagline),
            color = VintageColors.inkMuted.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontStyle = FontStyle.Italic,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.settings_footer_copyright),
            color = VintageColors.inkMuted.copy(alpha = 0.4f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center,
        )
    }
}

// MARK: - Intent helpers
//
// Every one of these can fail on a phone with no browser, mail client or Play Store, so
// they all funnel through the same toast rather than letting ActivityNotFoundException
// crash the app.

private fun Context.openUrl(url: String, failureMessage: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, failureMessage, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Prefers the Play Store app, falling back to the browser.
 *
 * Both legs fail today because the listing does not exist yet; once it is published this
 * starts working with no code change.
 */
private fun Context.openPlayStore(failureMessage: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SettingsLinks.playStoreAppUrl())))
    } catch (e: ActivityNotFoundException) {
        openUrl(SettingsLinks.playStoreWebUrl(), failureMessage)
    }
}

private fun Context.shareText(text: String, chooserTitle: String, failureMessage: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        startActivity(Intent.createChooser(intent, chooserTitle))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, failureMessage, Toast.LENGTH_SHORT).show()
    }
}

private fun Context.sendEmail(address: String, subject: String, failureMessage: String) {
    // ACTION_SENDTO with a mailto: URI reaches only mail apps, unlike ACTION_SEND which
    // would offer every share target on the phone.
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")).apply {
        putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, failureMessage, Toast.LENGTH_SHORT).show()
    }
}
