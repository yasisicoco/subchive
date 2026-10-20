package com.example.submate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import com.example.submate.integration.AdsConsent
import com.example.submate.integration.PurchaseManager
import com.example.submate.ui.SubMateApp
import com.example.submate.ui.SubMateViewModel

class MainActivity : ComponentActivity() {
    private var selectedId by mutableLongStateOf(0)
    private lateinit var purchases: PurchaseManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        selectedId = intent.getLongExtra(SUBSCRIPTION_ID, 0)
        val model = ViewModelProvider(this)[SubMateViewModel::class.java]
        val consent = AdsConsent(this)
        purchases = PurchaseManager(this, (application as SubMateApplication).settings)
        if (BuildConfig.BILLING_ENABLED) purchases.connect()
        setContent { SubMateApp(model, selectedId, { selectedId = 0 }, consent, purchases) }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        selectedId = intent.getLongExtra(SUBSCRIPTION_ID, 0)
    }
    override fun onDestroy() {
        purchases.close()
        super.onDestroy()
    }
    companion object { const val SUBSCRIPTION_ID = "subscription_id" }
}
