package com.kzwdaw.kyguukk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kzwdaw.kyguukk.data.FitnessRepository
import com.kzwdaw.kyguukk.data.MetricDefinitions
import com.kzwdaw.kyguukk.data.PreferencesRepository
import com.kzwdaw.kyguukk.ui.CalendarScreen
import com.kzwdaw.kyguukk.ui.ChartScreen
import com.kzwdaw.kyguukk.ui.OverviewScreen
import com.kzwdaw.kyguukk.ui.PostureLibraryScreen
import com.kzwdaw.kyguukk.ui.RecordScreen
import com.kzwdaw.kyguukk.ui.SilhouetteOverviewScreen
import com.kzwdaw.kyguukk.ui.theme.And03Theme

private sealed class Screen {
    data object Overview : Screen()
    data object SilhouetteOverview : Screen()
    data object Record : Screen()
    data class Chart(val itemId: String) : Screen()
    data object Calendar : Screen()
    data object PostureLibrary : Screen()
}

private const val ANIM_DURATION = 300

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() // Activity 1.12+ 签名变化，不使用系统边到边，由 Scaffold handle insets
        setContent {
            And03Theme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val repository = remember { FitnessRepository.get(context) }
    val prefs = remember { PreferencesRepository.get(context) }

    val rootScreen: Screen = if (prefs.overviewMode == PreferencesRepository.MODE_SILHOUETTE) {
        Screen.SilhouetteOverview
    } else {
        Screen.Overview
    }

    val backStack = remember { mutableStateListOf<Screen>(rootScreen) }
    val currentScreen: Screen = backStack.last()

    var goingForward by remember { mutableStateOf(true) }
    var refreshTick by remember { mutableIntStateOf(0) }

    var latestValues by remember { mutableStateOf<Map<String, Pair<Double, Long>>>(emptyMap()) }
    var recordCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTick) {
        latestValues = repository.getLatestValues()
        recordCount = repository.getAllRecords().size
    }

    val push: (Screen) -> Unit = { target ->
        if (backStack.lastOrNull() != target) {
            goingForward = true
            backStack.add(target)
        }
    }

    val pop: () -> Unit = {
        if (backStack.size > 1) {
            goingForward = false
            backStack.removeLastOrNull()
        }
    }

    /** 切换根概览模式（列表/剪影）——替换整个栈。 */
    val switchOverviewMode: (Screen) -> Unit = { target ->
        goingForward = false
        backStack.clear()
        backStack.add(target)
    }

    BackHandler(enabled = backStack.size > 1) {
        pop()
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (goingForward) {
                (slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(ANIM_DURATION)
                ) togetherWith slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(ANIM_DURATION)
                ))
            } else {
                (slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(ANIM_DURATION)
                ) togetherWith slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(ANIM_DURATION)
                ))
            }
        },
        label = "screen"
    ) { current ->
        when (current) {
            Screen.Overview -> OverviewScreen(
                latestValues = latestValues,
                recordCount = recordCount,
                onItemClick = { itemId -> push(Screen.Chart(itemId)) },
                onAddClick = { push(Screen.Record) },
                onCalendarClick = { push(Screen.Calendar) },
                onPostureClick = { push(Screen.PostureLibrary) },
                onSwitchToSilhouette = {
                    prefs.overviewMode = PreferencesRepository.MODE_SILHOUETTE
                    switchOverviewMode(Screen.SilhouetteOverview)
                }
            )

            Screen.SilhouetteOverview -> SilhouetteOverviewScreen(
                latestValues = latestValues,
                onSwitchToList = {
                    prefs.overviewMode = PreferencesRepository.MODE_LIST
                    switchOverviewMode(Screen.Overview)
                },
                onItemClick = { itemId -> push(Screen.Chart(itemId)) },
                onAddClick = { push(Screen.Record) },
                onCalendarClick = { push(Screen.Calendar) },
                onPostureClick = { push(Screen.PostureLibrary) }
            )

            Screen.Record -> RecordScreen(
                onSave = { recordedAt, values ->
                    repository.addRecord(recordedAt, values)
                    refreshTick++
                    goingForward = false
                    backStack.removeLastOrNull()
                    Unit
                },
                onBack = { pop() }
            )

            is Screen.Chart -> {
                val item = remember(current.itemId) {
                    MetricDefinitions.itemById(current.itemId)
                }
                ChartScreen(
                    item = item,
                    loadHistory = { key -> repository.getSeriesHistory(key) },
                    onBack = { pop() }
                )
            }

            Screen.Calendar -> CalendarScreen(
                onBack = { pop() }
            )

            Screen.PostureLibrary -> PostureLibraryScreen(
                onBack = { pop() }
            )
        }
    }
}
