package vn.hocdantranh.guzheng.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.ui.theme.VintageColors
import vn.hocdantranh.guzheng.data.sheet.directImageUrl

/**
 * One page, full-screen on black.
 *
 * Telephoto supplies the pinch, pan-with-edge-clamping, fling and double-tap-to-reset that
 * iOS hand-rolls with gesture recognisers; [ZoomSpec] caps the zoom at the same 6x.
 */
@Composable
fun SheetPageZoomScreen(url: String, pageNumber: Int, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 6f))

        ZoomableAsyncImage(
            // Raw Drive links serve an HTML page, not bytes — always rewrite first.
            model = directImageUrl(url),
            contentDescription = stringResource(R.string.sheet_page_number, pageNumber),
            contentScale = ContentScale.Fit,
            state = rememberZoomableImageState(zoomableState),
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 56.dp, start = 18.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    // Solid app sage, not translucent white: the button often sits over
                    // the white sheet page, where white-on-white disappeared.
                    .background(VintageColors.sageDark)
                    .border(1.dp, VintageColors.paper.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.sheet_zoom_close),
                    tint = VintageColors.paper,
                    modifier = Modifier.size(18.dp),
                )
            }

            Box(Modifier.weight(1f))

            Text(
                text = stringResource(R.string.sheet_page_number, pageNumber),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.sheet_zoom_hint),
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 11.sp,
            )
        }
    }
}
