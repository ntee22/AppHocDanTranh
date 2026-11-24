package vn.hocdantranh.guzheng.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.hocdantranh.guzheng.FeatureFlags

/**
 * Google Play Billing wrapper — the Android counterpart of iOS `PaymentManager`.
 *
 * Inert while [FeatureFlags.IAP_ENABLED] is false: [start] returns without creating a
 * [BillingClient], so nothing talks to Play and [ownsCourse] stays false.
 *
 * Process-wide singleton: purchases can complete while no screen is listening (pending
 * payments, purchases made on another device), so the listener must outlive any ViewModel.
 */
class BillingRepository private constructor(
    private val appContext: Context,
    private val enabled: Boolean,
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var client: BillingClient? = null

    private val _ownedProducts = MutableStateFlow<Set<String>>(emptySet())
    val ownedProducts: StateFlow<Set<String>> = _ownedProducts.asStateFlow()

    private val _courseDetails = MutableStateFlow<ProductDetails?>(null)

    /** Localised product info for the purchase sheet; null until Play answers. */
    val courseDetails: StateFlow<ProductDetails?> = _courseDetails.asStateFlow()

    val ownsCourse: Boolean
        get() = _ownedProducts.value.any { it in BillingProducts.COURSE_GRANTING }

    /** Connects and refreshes products and owned purchases. Safe to call repeatedly. */
    fun start() {
        if (!enabled) return
        val existing = client
        if (existing != null && existing.isReady) {
            scope.launch { refresh() }
            return
        }
        val newClient = existing ?: BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .enableAutoServiceReconnection()
            .build()
            .also { client = it }

        newClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch { refresh() }
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    /** Restore = re-query what Play says this account owns. */
    fun restore() {
        if (!enabled) return
        scope.launch { refreshPurchases() }
    }

    /** Opens the Play purchase sheet for the course. No-op until details have loaded. */
    fun launchCoursePurchase(activity: Activity): Boolean {
        if (!enabled) return false
        val billing = client ?: return false
        val details = _courseDetails.value ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        return billing.launchBillingFlow(activity, params).responseCode ==
            BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch { handlePurchases(purchases, replace = false) }
        }
        // USER_CANCELED and errors: nothing to record, the sheet already told the user.
    }

    private suspend fun refresh() {
        refreshProductDetails()
        refreshPurchases()
    }

    private suspend fun refreshProductDetails() {
        val billing = client ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingProducts.COURSE_BASIC)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        val result = billing.queryProductDetails(params)
        _courseDetails.value = result.productDetailsList
            ?.firstOrNull { it.productId == BillingProducts.COURSE_BASIC }
    }

    private suspend fun refreshPurchases() {
        val billing = client ?: return
        val result = billing.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            handlePurchases(result.purchasesList, replace = true)
        }
    }

    /**
     * Grants only PURCHASED (not PENDING) purchases, and acknowledges them — Play refunds
     * any purchase left unacknowledged for three days.
     */
    private suspend fun handlePurchases(purchases: List<Purchase>, replace: Boolean) {
        val owned = mutableSetOf<String>()
        for (purchase in purchases) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            owned += purchase.products
            if (!purchase.isAcknowledged) {
                client?.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                )
            }
        }
        _ownedProducts.value = if (replace) owned else _ownedProducts.value + owned
    }

    companion object {
        private const val TAG = "BillingRepository"

        @Volatile
        private var instance: BillingRepository? = null

        fun get(context: Context): BillingRepository =
            instance ?: synchronized(this) {
                instance ?: BillingRepository(
                    context.applicationContext,
                    FeatureFlags.IAP_ENABLED,
                ).also { instance = it }
            }
    }
}
