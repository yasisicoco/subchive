package com.example.submate.ui

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.submate.BuildConfig
import com.example.submate.integration.AdsConsent
import com.example.submate.integration.PurchaseManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun SubMateApp(model: SubMateViewModel, externalId: Long, consumeId: () -> Unit, consent: AdsConsent, purchases: PurchaseManager) {
    val subscriptions by model.subscriptions.collectAsStateWithLifecycle()
    val settings by model.settings.collectAsStateWithLifecycle()
    val today by model.today.collectAsStateWithLifecycle()
    val loaded by model.loaded.collectAsStateWithLifecycle()
    val adsReady by consent.canRequest.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "home"
    val snackbar = remember { SnackbarHostState() }
    val roots = listOf("home", "calendar", "stats", "settings")
    val labels = listOf("내 구독", "캘린더", "리포트", "설정")
    val icons = listOf(Icons.Default.CreditCard, Icons.Default.CalendarMonth, Icons.Default.DonutLarge, Icons.Default.Settings)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.refreshDate() }
    LaunchedEffect(Unit) { model.events.collect { snackbar.showSnackbar(it, actionLabel = "확인", duration = SnackbarDuration.Short) } }
    LaunchedEffect(externalId) {
        if (externalId > 0) { nav.navigate("detail/$externalId") { launchSingleTop = true }; consumeId() }
    }
    LaunchedEffect(settings.purchased) { if (!settings.purchased && BuildConfig.ADS_ENABLED) consent.request() }
    SubMateTheme(settings.darkMode) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
            if (route in roots) Column {
                if (BuildConfig.ADS_ENABLED && adsReady && !settings.purchased) AdBanner()
                NavigationBar(modifier = Modifier.background(MaterialTheme.colorScheme.surface).navigationBarsPadding().height(64.dp), windowInsets = WindowInsets(0, 0, 0, 0), containerColor = MaterialTheme.colorScheme.surface) {
                    roots.forEachIndexed { index, destination ->
                        NavigationBarItem(selected = route == destination, onClick = {
                            nav.navigate(destination) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true }
                        }, icon = { Icon(icons[index], labels[index], Modifier.size(20.dp)) }, label = { Text(labels[index], fontSize = 11.sp, fontWeight = if (route == destination) FontWeight.Medium else FontWeight.Normal) })
                    }
                }
            }
        }) { padding ->
            NavHost(nav, "home", modifier = Modifier.fillMaxSize().padding(padding)) {
                composable("home") { HomeScreen(subscriptions, today, loaded, { nav.navigate("edit/0") }, { nav.navigate("detail/$it") }, model) }
                composable("calendar") { CalendarScreen(subscriptions, today, { nav.navigate("detail/$it") }) }
                composable("stats") { StatsScreen(subscriptions, today) }
                composable("settings") { SettingsScreen(model, consent, purchases) }
                composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    val id = entry.arguments?.getLong("id") ?: 0
                    DetailScreen(subscriptions.firstOrNull { it.id == id }, today, loaded, model, { nav.popBackStack() }, { nav.navigate("edit/$id") })
                }
                composable("edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    val id = entry.arguments?.getLong("id") ?: 0
                    if (id == 0L || subscriptions.any { it.id == id }) SubscriptionEditor(subscriptions.firstOrNull { it.id == id }, model, { nav.popBackStack() })
                    else EmptyPage(if (loaded) "구독을 찾을 수 없습니다." else "구독을 불러오는 중입니다.", { nav.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun AdBanner() {
    val context = LocalContext.current
    val ad = remember { AdView(context).apply { setAdSize(AdSize.BANNER); adUnitId = BuildConfig.AD_BANNER_ID; loadAd(AdRequest.Builder().build()) } }
    DisposableEffect(ad) { onDispose { ad.destroy() } }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { ad.pause() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { ad.resume() }
    AndroidView(factory = { ad }, modifier = Modifier.fillMaxWidth())
}
