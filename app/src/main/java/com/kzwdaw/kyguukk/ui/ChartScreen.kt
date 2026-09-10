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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kzwdaw.kyguukk.data.MetricItem
import com.kzwdaw.kyguukk.data.SeriesPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    item: MetricItem,
    loadHistory: (key: String) -> List<SeriesPoint>,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    // 每个 series 的历史
    var histories by remember { mutableStateOf<Map<String, List<SeriesPoint>>>(emptyMap()) }
    LaunchedEffect(item.id) {
        histories = item.keys.associateWith { loadHistory(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${item.name}·趋势") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(contentPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val primary = MaterialTheme.colorScheme.primary
                val tertiary = MaterialTheme.colorScheme.tertiary
                val chartSeries = item.series.mapIndexed { i, s ->
                    val color = if (item.isPaired && i == 1) tertiary else primary
                    val name = if (item.isPaired) s.label.trimStart('·') else item.name
                    ChartSeries(
                        name = name,
                        color = color,
                        points = (histories[s.key] ?: emptyList()).map { it.recordedAt to it.value }
                    )
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        if (item.isPaired) {
                            ChartLegend(chartSeries)
                            Spacer(Modifier.height(8.dp))
                        }
                        LineChart(chartSeries)
                    }
                }
            }

            item {
                Text(
                    text = "历史记录",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 合并各 series 历史，按时间倒序展示
            val rows = histories.flatMap { (key, pts) ->
                pts.map { key to it }
            }.sortedByDescending { it.second.recordedAt }

            if (rows.isEmpty()) {
                item {
                    Text(
                        text = "暂无数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(rows, key = { it.second.recordId.toString() + it.first }) { (key, pt) ->
                    val seriesDef = item.series.first { it.key == key }
                    HistoryRow(item, seriesDef.label, pt)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun HistoryRow(
    item: MetricItem,
    sideLabel: String,
    point: SeriesPoint
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = point.recordedAt.toDateTimeStr(),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (item.isPaired) "${item.name}$sideLabel" else item.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${point.value.fmtCm()} cm",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
