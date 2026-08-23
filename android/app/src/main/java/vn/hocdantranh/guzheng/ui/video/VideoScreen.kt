package vn.hocdantranh.guzheng.ui.video

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.video.VideoCategory
import vn.hocdantranh.guzheng.data.video.VideoItem
import vn.hocdantranh.guzheng.ui.components.InkSeal
import vn.hocdantranh.guzheng.ui.components.OverlineLabel
import vn.hocdantranh.guzheng.ui.components.PaperBackground
import vn.hocdantranh.guzheng.ui.theme.VintageColors

/** Binds the ViewModel and hands a tapped video to whichever app owns it. */
@Composable
fun VideoRoute() {
    val context = LocalContext.current
    val viewModel: VideoViewModel = viewModel(factory = VideoViewModel.factory())
    val state by viewModel.uiState.collectAsState()
    val openFailed = stringResource(R.string.video_open_failed)

    VideoScreen(
        state = state,
        onCategorySelected = viewModel::selectCategory,
        onVideoClick = { item ->
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
            } catch (e: ActivityNotFoundException) {
                // No browser and no platform app — tell the user rather than dying silently.
                Toast.makeText(context, openFailed, Toast.LENGTH_SHORT).show()
            }
        },
        onLoadMore = viewModel::loadMore,
    )
}

@Composable
fun VideoScreen(
    state: VideoUiState,
    onCategorySelected: (VideoCategory?) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onLoadMore: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        PaperBackground()

        Column(Modifier.fillMaxSize()) {
            Header()

            if (state.availableCategories.isNotEmpty()) {
                Chips(state, onCategorySelected)
            }

            Content(state, onVideoClick, onLoadMore)
        }
    }
}

@Composable
private fun Header() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        OverlineLabel(text = stringResource(R.string.video_overline))
        Row {
            Text(
                text = stringResource(R.string.video_header_title),
                color = VintageColors.inkDark,
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
            )
            Text(
                text = stringResource(R.string.video_header_accent),
                color = VintageColors.sageDark,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Serif,
            )
        }
        Text(
            text = stringResource(R.string.video_header_tagline),
            color = VintageColors.inkMuted,
            fontSize = 15.sp,
            fontStyle = FontStyle.Italic,
            fontFamily = FontFamily.Serif,
        )
    }
}

@Composable
private fun Chips(state: VideoUiState, onCategorySelected: (VideoCategory?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 12.dp),
    ) {
        item {
            CategoryChip(
                label = stringResource(R.string.video_category_all),
                isSelected = state.selectedCategory == null,
                onClick = { onCategorySelected(null) },
            )
        }
        items(state.availableCategories) { category ->
            CategoryChip(
                label = stringResource(category.labelRes),
                isSelected = state.selectedCategory == category,
                // Tapping the active chip clears the filter, matching iOS.
                onClick = {
                    onCategorySelected(if (state.selectedCategory == category) null else category)
                },
            )
        }
    }
}

@Composable
private fun Content(
    state: VideoUiState,
    onVideoClick: (VideoItem) -> Unit,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()

    // Page in as the last card comes into view, replacing the iOS 1px trigger spacer.
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: return@derivedStateOf false
            last >= state.filtered.size - 1
        }
    }
    LaunchedEffect(listState, state.hasMore) {
        snapshotFlow { shouldLoadMore }.collect { if (it && state.hasMore) onLoadMore() }
    }

    when {
        state.isLoading && state.videos.isEmpty() -> {
            Column(
                Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                repeat(3) { SkeletonCard() }
            }
        }

        state.filtered.isEmpty() -> {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InkSeal(char = "琴", size = 48.dp)
                Text(
                    text = stringResource(R.string.video_empty),
                    color = VintageColors.inkMuted,
                    fontSize = 18.sp,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                )
            }
        }

        else -> {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(state.filtered, key = { _, item -> item.id }) { index, item ->
                    if (index == 0) {
                        FeaturedVideoCard(item) { onVideoClick(item) }
                    } else {
                        VideoCard(item) { onVideoClick(item) }
                    }
                }

                if (state.isLoadingMore) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = VintageColors.inkMuted)
                        }
                    }
                }
            }
        }
    }
}
