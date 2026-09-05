package vn.hocdantranh.guzheng.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.sheet.SheetItem
import vn.hocdantranh.guzheng.ui.components.InkSeal
import vn.hocdantranh.guzheng.ui.components.OverlineLabel
import vn.hocdantranh.guzheng.ui.components.PaperBackground
import vn.hocdantranh.guzheng.ui.theme.VintageColors

/** Binds the ViewModel and reports a tapped sheet to the navigation host. */
@Composable
fun SheetRoute(onSheetClick: (SheetItem) -> Unit) {
    val context = LocalContext.current
    val viewModel: SheetViewModel = viewModel(factory = SheetViewModel.factory(context))
    val state by viewModel.uiState.collectAsState()

    SheetScreen(
        state = state,
        onQueryChange = viewModel::search,
        onSheetClick = onSheetClick,
        onRetry = viewModel::refresh,
    )
}

@Composable
fun SheetScreen(
    state: SheetUiState,
    onQueryChange: (String) -> Unit,
    onSheetClick: (SheetItem) -> Unit,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        PaperBackground()

        Column(Modifier.fillMaxSize()) {
            Header()
            SearchBar(query = state.query, onQueryChange = onQueryChange)
            Content(state = state, onSheetClick = onSheetClick, onRetry = onRetry)
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
            OverlineLabel(text = stringResource(R.string.sheet_overline))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = VintageColors.inkDark)) {
                        append(stringResource(R.string.sheet_header_title))
                    }
                    withStyle(
                        SpanStyle(
                            color = VintageColors.sageDark,
                            fontStyle = FontStyle.Italic,
                        )
                    ) {
                        append(stringResource(R.string.sheet_header_accent))
                    }
                },
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
            )
            Text(
                text = stringResource(R.string.sheet_header_tagline),
                color = VintageColors.inkSoft,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        InkSeal(char = "樂", size = 32.dp)
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 18.dp)
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(VintageColors.paperWarm.copy(alpha = 0.8f))
            .border(1.dp, VintageColors.woodDark.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = VintageColors.inkMuted,
            modifier = Modifier.size(16.dp),
        )

        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.sheet_search_hint),
                    color = VintageColors.inkMuted.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = VintageColors.inkDark,
                    fontSize = 14.sp,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (query.isNotEmpty()) {
            Icon(
                Icons.Filled.Cancel,
                contentDescription = stringResource(R.string.sheet_search_clear),
                tint = VintageColors.inkMuted,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onQueryChange("") },
            )
        }
    }
}

@Composable
private fun Content(
    state: SheetUiState,
    onSheetClick: (SheetItem) -> Unit,
    onRetry: () -> Unit,
) {
    val listPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 110.dp)

    when {
        state.errorMessage != null -> ErrorState(message = state.errorMessage, onRetry = onRetry)

        state.isLoading && state.sheets.isEmpty() ->
            LazyColumn(
                contentPadding = listPadding,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(6) { SheetSkeletonCard() }
            }

        state.filtered.isEmpty() -> EmptyState(query = state.query)

        else -> LazyColumn(
            contentPadding = listPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.filtered, key = { it.id }) { sheet ->
                SheetCard(sheet = sheet) { onSheetClick(sheet) }
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 60.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = VintageColors.sealRed,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = stringResource(R.string.sheet_error_title),
            color = VintageColors.inkDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Serif,
        )
        Text(
            text = message,
            color = VintageColors.inkMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.sheet_error_retry),
            color = VintageColors.sageDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onRetry),
        )
    }
}

@Composable
private fun EmptyState(query: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "樂",
            color = VintageColors.inkMuted.copy(alpha = 0.4f),
            fontSize = 48.sp,
            fontFamily = FontFamily.Serif,
        )
        Text(
            text = if (query.isBlank()) {
                stringResource(R.string.sheet_empty)
            } else {
                stringResource(R.string.sheet_empty_search, query)
            },
            color = VintageColors.inkMuted,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            fontStyle = FontStyle.Italic,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center,
        )
    }
}
