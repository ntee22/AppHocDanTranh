package vn.hocdantranh.guzheng.ui.billing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.hocdantranh.guzheng.FeatureFlags
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.data.billing.BillingRepository
import vn.hocdantranh.guzheng.ui.theme.VintageColors

/**
 * Shown in place of the player on a paid lesson the learner does not own.
 *
 * Only reachable when [FeatureFlags.IAP_ENABLED] is true; it renders nothing otherwise, as
 * a second guard in case a caller forgets to check.
 */
@Composable
fun CoursePurchaseCard(modifier: Modifier = Modifier) {
    if (!FeatureFlags.IAP_ENABLED) return

    val context = LocalContext.current
    val billing = remember { BillingRepository.get(context) }
    val details by billing.courseDetails.collectAsStateWithLifecycle()
    val price = details?.oneTimePurchaseOfferDetails?.formattedPrice

    Column(
        modifier
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VintageColors.paper)
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(VintageColors.sageDark.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = VintageColors.sageDark,
                modifier = Modifier.size(30.dp),
            )
        }

        Text(
            text = stringResource(R.string.purchase_course_title),
            color = VintageColors.inkDark,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.purchase_course_body),
            color = VintageColors.inkSoft,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )

        Button(
            onClick = { context.findActivity()?.let(billing::launchCoursePurchase) },
            enabled = details != null,
            colors = ButtonDefaults.buttonColors(containerColor = VintageColors.sageDark),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (price != null) {
                    stringResource(R.string.purchase_course_button_price, price)
                } else {
                    stringResource(R.string.purchase_course_button)
                },
                fontWeight = FontWeight.Bold,
            )
        }

        TextButton(onClick = billing::restore) {
            Text(
                text = stringResource(R.string.purchase_restore),
                color = VintageColors.inkMuted,
                fontSize = 12.sp,
            )
        }
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
