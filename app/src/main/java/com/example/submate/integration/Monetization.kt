package com.example.submate.integration

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.submate.BuildConfig
import com.example.submate.data.SettingsStore
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdsConsent(private val activity: Activity) {
    private val info = UserMessagingPlatform.getConsentInformation(activity)
    private val mutable = MutableStateFlow(false)
    val canRequest = mutable.asStateFlow()
    private val privacyMutable = MutableStateFlow(false)
    val privacyRequired = privacyMutable.asStateFlow()
    private var initialized = false
    fun request() {
        if (!BuildConfig.ADS_ENABLED) return
        info.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { refresh() }
        }, { refresh() })
    }
    fun privacy() = UserMessagingPlatform.showPrivacyOptionsForm(activity) { refresh() }
    private fun refresh() {
        privacyMutable.value = info.privacyOptionsRequirementStatus == com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (info.canRequestAds() && !initialized) {
            initialized = true
            MobileAds.initialize(activity) { mutable.value = true }
        } else if (!info.canRequestAds()) mutable.value = false
    }
}

class PurchaseManager(context: Context, private val settings: SettingsStore) {
    private val statusMutable = MutableStateFlow("Google Play 상품을 불러오는 중")
    val status = statusMutable.asStateFlow()
    private var details: ProductDetails? = null
    private val client = BillingClient.newBuilder(context)
        .enableAutoServiceReconnection()
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .setListener { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) reconcile(purchases)
            else statusMutable.value = if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) "결제를 취소했습니다." else "결제를 완료하지 못했습니다. (${result.responseCode})"
        }.build()
    fun connect() {
        if (!BuildConfig.BILLING_ENABLED) { statusMutable.value = "이 빌드에서는 결제가 꺼져 있습니다."; return }
        if (client.isReady) { restore(); return }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() { statusMutable.value = "Google Play에 다시 연결해 주세요." }
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) { statusMutable.value = "Google Play 결제 연결 실패 (${result.responseCode})"; return }
                restore()
                val product = QueryProductDetailsParams.Product.newBuilder().setProductId(BuildConfig.REMOVE_ADS_PRODUCT).setProductType(BillingClient.ProductType.INAPP).build()
                client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()) { response, products ->
                    details = products.productDetailsList.firstOrNull()
                    statusMutable.value = if (response.responseCode == BillingClient.BillingResponseCode.OK && details != null) "광고 제거 · ${details?.oneTimePurchaseOfferDetails?.formattedPrice.orEmpty()}" else "Play Console 상품 등록과 테스트 배포가 필요합니다."
                }
            }
        })
    }
    fun buy(activity: Activity) {
        val product = details ?: run { connect(); return }
        val builder = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product)
        product.oneTimePurchaseOfferDetails?.offerToken?.let(builder::setOfferToken)
        val params = builder.build()
        val result = client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build())
        if (result.responseCode != BillingClient.BillingResponseCode.OK) statusMutable.value = "결제 화면을 열 수 없습니다. (${result.responseCode})"
    }
    fun restore() {
        if (!client.isReady) return
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) reconcile(purchases)
            else statusMutable.value = "구매 내역을 복원하지 못했습니다."
        }
    }
    private fun reconcile(purchases: List<Purchase>) {
        val owned = purchases.filter { BuildConfig.REMOVE_ADS_PRODUCT in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (owned.isEmpty()) { settings.update { it.copy(purchased = false) }; return }
        owned.forEach { purchase ->
            if (purchase.isAcknowledged) settings.update { it.copy(purchased = true) }
            else client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) settings.update { it.copy(purchased = true) }
                else statusMutable.value = "구매 승인 대기 중입니다. 구매 복원을 다시 실행해 주세요."
            }
        }
    }
    fun close() = client.endConnection()
}
