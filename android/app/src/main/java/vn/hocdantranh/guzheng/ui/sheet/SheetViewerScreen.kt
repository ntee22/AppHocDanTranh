package vn.hocdantranh.guzheng.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.sheet.SheetItem
import vn.hocdantranh.guzheng.data.sheet.directImageUrl
import vn.hocdantranh.guzheng.ui.components.BrushDivider
import vn.hocdantranh.guzheng.ui.components.PaperBackground
import vn.hocdantranh.guzheng.ui.theme.VintageColors

/** All pages of one sheet, stacked vertically. Tapping a page opens the zoom screen. */
@Composable
fun SheetViewerScreen(
    sheet: SheetItem,
    onBack: () -> Unit,
    onPageClick: (Int) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        PaperBackground()

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp, start = 18.dp, end = 18.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(VintageColors.paperWarm.copy(alpha = 0.9f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.sheet_back),
                        tint = VintageColors.inkSoft,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Text(
                    text = sheet.title,
                    color = VintageColors.inkDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            BrushDivider(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
                    .padding(bottom = 8.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.sheet_viewer_hint),
                        color = VintageColors.inkMuted.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                    )
                }

                itemsIndexed(sheet.pages) { index, page ->
                    SheetPage(
                        url = page,
                        pageNumber = index + 1,
                        onClick = { onPageClick(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetPage(url: String, pageNumber: Int, onClick: () -> Unit) {
    SubcomposeAsyncImage(
        // Raw Drive links serve an HTML page, not bytes — always rewrite first.
        model = directImageUrl(url),
        contentDescription = stringResource(R.string.sheet_page_number, pageNumber),
        contentScale = ContentScale.FillWidth,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        loading = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(VintageColors.paperDeep.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = VintageColors.inkMuted)
            }
        },
        error = {
            // A fixed height, so a failed page stays visible instead of collapsing away.
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = VintageColors.inkMuted,
                )
                Text(
                    text = stringResource(R.string.sheet_page_load_failed, pageNumber),
                    color = VintageColors.inkMuted,
                    fontSize = 13.sp,
                )
            }
        },
    )
}
