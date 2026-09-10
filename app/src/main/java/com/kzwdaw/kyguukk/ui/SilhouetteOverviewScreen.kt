package com.kzwdaw.kyguukk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kzwdaw.kyguukk.data.MetricDefinitions
import com.kzwdaw.kyguukk.data.PreferencesRepository

private const val STALE_THRESHOLD_MS = 10L * 24 * 60 * 60 * 1000L // 10天
private val StaleColor = Color(0xFFE8833A) // 橙色

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SilhouetteOverviewScreen(
    latestValues: Map<String, Pair<Double, Long>>,
    now: Long = System.currentTimeMillis(),
    onSwitchToList: () -> Unit,
    onItemClick: (String) -> Unit,
    onAddClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onPostureClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesRepository.get(context) }

    // 从偏好加载性别，变更时保存
    var genderString by remember { mutableStateOf(prefs.silhouetteGender) }
    var gender = if (genderString == "FEMALE") SilhouetteGender.FEMALE else SilhouetteGender.MALE
    val onGenderChange: (SilhouetteGender) -> Unit = { g ->
        gender = g
        genderString = if (g == SilhouetteGender.FEMALE) "FEMALE" else "MALE"
        prefs.silhouetteGender = genderString
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("剪影概览") },
                actions = {
                    IconButton(onClick = onSwitchToList) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "切换为列表")
                    }
                }
            )
        },
        floatingActionButton = {
            HomeFABs(
                onCalendarClick = onCalendarClick,
                onPostureClick = onPostureClick,
                onAddClick = onAddClick
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = gender == SilhouetteGender.MALE,
                    onClick = { onGenderChange(SilhouetteGender.MALE) },
                    label = { Text("男") }
                )
                FilterChip(
                    selected = gender == SilhouetteGender.FEMALE,
                    onClick = { onGenderChange(SilhouetteGender.FEMALE) },
                    label = { Text("女") }
                )
            }
            Spacer(Modifier.height(12.dp))

            // 构造数值标签
            val labels = SilhouetteAnchors.map { anchor ->
                val metricItem = MetricDefinitions.keyToItem[anchor.key]!!
                val entry = latestValues[anchor.key]
                if (entry == null) {
                    SilhouetteLabel(
                        text = "0",
                        color = StaleColor,
                        anchor = anchor,
                        metricItemId = metricItem.id
                    )
                } else {
                    val (value, ts) = entry
                    val isStale = (now - ts) > STALE_THRESHOLD_MS
                    SilhouetteLabel(
                        text = value.fmtCm(),
                        color = if (isStale) StaleColor
                        else MaterialTheme.colorScheme.onSurface,
                        anchor = anchor,
                        metricItemId = metricItem.id
                    )
                }
            }

            BodySilhouette(
                gender = gender,
                labels = labels,
                onLabelClick = onItemClick,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = "橙色 = 未录入 / 超过10天未录入 · 点击数值查看趋势",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(80.dp))
        }
    }
}
