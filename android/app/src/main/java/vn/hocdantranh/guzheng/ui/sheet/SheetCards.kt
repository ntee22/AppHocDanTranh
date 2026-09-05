package vn.hocdantranh.guzheng.ui.sheet

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import coil.compose.SubcomposeAsyncImage
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.sheet.SheetItem
import vn.hocdantranh.guzheng.data.sheet.directImageUrl
import vn.hocdantranh.guzheng.ui.theme.VintageColors

private val ThumbWidth = 64.dp
private val ThumbHeight = 80.dp

/**
 * One row in the sheet library.
 *
 * A premium sheet gets a scrim and a padlock but stays clickable — iOS shows the same
 * badge and opens the sheet anyway, and Android mirrors that until Play Billing lands.
 */
@Composable
fun SheetCard(sheet: SheetItem, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VintageColors.paperWarm.copy(alpha = 0.7f))
            .border(1.dp, VintageColors.inkMuted.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(sheet)

        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = sheet.title,
                color = VintageColors.inkDark,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.sheet_pages_count, sheet.pages.size),
                    color = VintageColors.inkMuted,
                    fontSize = 11.sp,
                )
                AccessPill(isFree = sheet.isFree)
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = VintageColors.inkMuted.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun Thumbnail(sheet: SheetItem) {
    Box(
        Modifier
            .width(ThumbWidth)
            .height(ThumbHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(VintageColors.paperDeep),
        contentAlignment = Alignment.Center,
    ) {
        val firstPage = sheet.pages.firstOrNull()
        if (firstPage != null) {
            SubcomposeAsyncImage(
                // Raw Drive links serve an HTML page, not bytes — always rewrite first.
                model = directImageUrl(firstPage),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = VintageColors.inkMuted,
                    )
                },
            )
        } else {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = VintageColors.inkMuted)
        }

        if (!sheet.isFree) {
            val locked = stringResource(R.string.sheet_locked)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .semantics { contentDescription = locked },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun AccessPill(isFree: Boolean) {
    val tint = if (isFree) VintageColors.sageDark else VintageColors.plum
    Text(
        text = stringResource(
            if (isFree) R.string.sheet_badge_free else R.string.sheet_badge_premium
        ),
        color = tint,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tint.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Placeholder row shown while the first load is in flight. */
@Composable
fun SheetSkeletonCard() {
    val transition = rememberInfiniteTransition(label = "sheet-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmer",
    )

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VintageColors.paperWarm.copy(alpha = 0.5f))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .width(ThumbWidth)
                .height(ThumbHeight)
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
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(VintageColors.woodSoft.copy(alpha = alpha))
            )
            Box(
                Modifier
                    .width(80.dp)
                    .height(11.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(VintageColors.woodSoft.copy(alpha = alpha))
            )
        }
    }
}
