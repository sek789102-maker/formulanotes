package com.sek789102.changkhamnuan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sek789102.changkhamnuan.ui.AppViewModel
import com.sek789102.changkhamnuan.ui.components.TabHeader
import com.sek789102.changkhamnuan.ui.screens.CalcHomeScreen
import com.sek789102.changkhamnuan.ui.screens.CalcScreen
import com.sek789102.changkhamnuan.ui.screens.ConverterScreen
import com.sek789102.changkhamnuan.ui.screens.MoreScreen
import com.sek789102.changkhamnuan.ui.screens.PricesScreen
import com.sek789102.changkhamnuan.ui.screens.ProfileScreen
import com.sek789102.changkhamnuan.ui.screens.ProjectScreen
import com.sek789102.changkhamnuan.ui.screens.ProjectsScreen
import com.sek789102.changkhamnuan.ui.screens.RatesScreen
import com.sek789102.changkhamnuan.ui.theme.AppTheme

/** Screens, stored as strings so the back stack survives rotation. "tab:<n>" are the four main tabs. */
private object Routes {
    fun calc(id: String) = "calc:$id"
    fun project(id: Long) = "project:$id"
    const val PROFILE = "profile"
    const val RATES = "rates"
    const val CONVERTER = "converter"
}

@OptIn(ExperimentalComposeUiApi::class)
class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Headers are yellow in both themes, so status-bar icons are always dark.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val s = settings ?: return@setContent
            AppTheme(s.theme) {
                Box(Modifier.semantics { testTagsAsResourceId = true }) { Root() }
            }
        }
    }

    @Composable
    private fun Root() {
        val snackbar = remember { SnackbarHostState() }
        val stack: SnapshotStateList<String> = rememberSaveable(
            saver = Saver<SnapshotStateList<String>, ArrayList<String>>(
                save = { ArrayList(it) },
                restore = { mutableStateListOf(*it.toTypedArray()) }
            )
        ) { mutableStateListOf("tab:0") }
        fun push(r: String) { stack.add(r) }
        fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
        fun openProject(id: Long) {
            // Open a project on top of the projects tab, not on top of a calculator.
            stack.clear(); stack.add("tab:1"); stack.add(Routes.project(id))
        }

        LaunchedEffect(Unit) {
            vm.messages.collect { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(it, duration = SnackbarDuration.Short) }
        }
        BackHandler(enabled = stack.size > 1) { pop() }

        val current = stack.last()
        when {
            current.startsWith("tab:") -> {
                val tab = current.removePrefix("tab:").toIntOrNull() ?: 0
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbar) },
                    topBar = {
                        when (tab) {
                            0 -> TabHeader("ช่างคำนวณ", "เลือกงาน ใส่ขนาด ได้จำนวนที่ต้องซื้อทันที", Icons.Outlined.Engineering)
                            1 -> TabHeader("งาน / ใบเสนอราคา", "รวมวัสดุ ค่าแรง แล้วส่งให้ลูกค้าทาง LINE")
                            2 -> TabHeader("ราคาวัสดุ", "ราคาจากร้านของคุณ ใช้กับทุกงาน")
                            else -> TabHeader("เพิ่มเติม", "ข้อมูลร้าน · สูตร · สำรองข้อมูล")
                        }
                    },
                    bottomBar = {
                        NavigationBar {
                            listOf(
                                Triple("คำนวณ", Icons.Outlined.Calculate, "tab_calc"),
                                Triple("งาน", Icons.Outlined.WorkOutline, "tab_projects"),
                                Triple("ราคาวัสดุ", Icons.Outlined.Sell, "tab_prices"),
                                Triple("เพิ่มเติม", Icons.Outlined.MoreHoriz, "tab_more")
                            ).forEachIndexed { i, (label, icon, tag) ->
                                NavigationBarItem(
                                    selected = tab == i,
                                    onClick = { stack.clear(); stack.add("tab:$i") },
                                    icon = { Icon(icon, null) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag(tag)
                                )
                            }
                        }
                    }
                ) { padding ->
                    when (tab) {
                        0 -> CalcHomeScreen(padding, onOpen = { push(Routes.calc(it)) }, onConverter = { push(Routes.CONVERTER) })
                        1 -> ProjectsScreen(padding, vm, onOpen = { push(Routes.project(it)) })
                        2 -> PricesScreen(padding, vm)
                        else -> MoreScreen(padding, vm, onProfile = { push(Routes.PROFILE) }, onRates = { push(Routes.RATES) }, onConverter = { push(Routes.CONVERTER) })
                    }
                }
            }
            current.startsWith("calc:") -> CalcScreen(current.removePrefix("calc:"), vm, snackbar, onBack = { pop() }, onOpenProject = { openProject(it) })
            current.startsWith("project:") -> ProjectScreen(
                current.removePrefix("project:").toLongOrNull() ?: 0L, vm, snackbar,
                onBack = { pop() }, onOpenProject = { openProject(it) }, onOpenProfile = { push(Routes.PROFILE) }
            )
            current == Routes.PROFILE -> ProfileScreen(vm, snackbar, onBack = { pop() })
            current == Routes.RATES -> RatesScreen(vm, snackbar, onBack = { pop() })
            current == Routes.CONVERTER -> ConverterScreen(onBack = { pop() })
            else -> { stack.clear(); stack.add("tab:0") }
        }
    }
}
