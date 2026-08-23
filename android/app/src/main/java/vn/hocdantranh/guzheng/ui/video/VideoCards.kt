package vn.hocdantranh.guzheng.ui.video

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.video.DefaultThumbnailResolver
import vn.hocdantranh.guzheng.data.video.ThumbnailResolver
import vn.hocdantranh.guzheng.data.video.VideoItem
import vn.hocdantranh.guzheng.data.video.VideoSource
import vn.hocdantranh.guzheng.ui.theme.VintageColors

/** TikTok resolution is a suspending oembed call, so the URL arrives after composition. */
@Composable
private fun rememberThumbnailUrl(
    item: VideoItem,
    resolver: ThumbnailResolver = DefaultThumbnailResolver,
): String? {
    var url by remember(item.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(item.id) { url = resolver.resolve(item) }
    return url
}

private val VideoItem.platformLabel: String
    get() = if (source == VideoSource.TIKTOK) "TikTok" else "YouTube"

private val VideoItem.platformColor: Color
    get() = if (source == VideoSource.TIKTOK) VintageColors.inkDark else VintageColors.sealRed

@Composable
private fun Badge(text: String, background: Color, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

/** The first card in the list: a large 180dp tile with the title over a gradient scrim. */
@Composable
fun FeaturedVideoCard(item: VideoItem, onClick: () -> Unit) {
    val playLabel = stringResource(R.string.video_play)

    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(VintageColors.woodSoft.copy(alpha = 0.4f))
            .border(1.dp, VintageColors.inkMuted.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$playLabel: ${item.title}" }
    ) {
        AsyncImage(
            model = rememberThumbnailUrl(item),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.5f to Color.Transparent,
                        1f to VintageColors.inkDark.copy(alpha = 0.7f),
                    )
                )
        )

        Box(
            Modifier
                .align(Alignment.Center)
                .size(52.dp)
                .clip(CircleShape)
                .background(VintageColors.paper.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = VintageColors.inkDark,
                modifier = Modifier.size(24.dp),
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.title,
                color = VintageColors.paper,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge(
                    text = stringResource(item.category.labelRes).uppercase(),
                    background = VintageColors.paper.copy(alpha = 0.2f),
                    color = VintageColors.paper.copy(alpha = 0.85f),
                )
                Badge(
                    text = item.platformLabel,
                    background = item.platformColor.copy(alpha = 0.85f),
                    color = VintageColors.paper,
                )
            }
        }
    }
}

/** Every card after the first: a 108x68 thumbnail beside the title and category pill. */
@Composable
fun VideoCard(item: VideoItem, onClick: () -> Unit) {
    val playLabel = stringResource(R.string.video_play)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(VintageColors.paperWarm.copy(alpha = 0.5f))
            .border(1.dp, VintageColors.inkMuted.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$playLabel: ${item.title}" }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .width(108.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(VintageColors.woodSoft.copy(alpha = 0.5f))
        ) {
            AsyncImage(
                model = rememberThumbnailUrl(item),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(VintageColors.inkDark.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = VintageColors.paper,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = item.platformLabel,
                color = VintageColors.paper,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(item.platformColor.copy(alpha = 0.85f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }

        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.title,
                color = VintageColors.inkDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Badge(
                text = stringResource(item.category.labelRes).uppercase(),
                background = VintageColors.sageDark.copy(alpha = 0.1f),
                color = VintageColors.sageDark,
            )
        }

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = VintageColors.inkSoft.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Shown while the first page loads, so the tab never flashes an empty state. */
@Composable
fun SkeletonCard() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmer",
    )

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(VintageColors.paperWarm.copy(alpha = 0.5f))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .width(108.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(VintageColors.woodSoft.copy(alpha = alpha))
        )
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(VintageColors.woodSoft.copy(alpha = alpha))
            )
            Box(
                Modifier
                    .width(80.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(VintageColors.woodSoft.copy(alpha = alpha))
            )
        }
    }
}

@Composable
fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (isSelected) VintageColors.paper else VintageColors.inkSoft,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (isSelected) VintageColors.sageDark
                else VintageColors.paperWarm.copy(alpha = 0.6f)
            )
            .border(
                1.dp,
                if (isSelected) Color.Transparent else VintageColors.inkMuted.copy(alpha = 0.25f),
                RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}
