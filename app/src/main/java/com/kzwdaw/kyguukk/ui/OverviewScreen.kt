package com.kzwdaw.kyguukk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.TrendingFlat
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kzwdaw.kyguukk.data.BodyPartGroup
import com.kzwdaw.kyguukk.data.MetricDefinitions
import com.kzwdaw.kyguukk.data.MetricItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    latestValues: Map<String, Pair<Double, Long>>,
    recordCount: Int,
    onItemClick: (String) -> Unit,
    onAddClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onPostureClick: () -> Unit,
    onSwitchToSilhouette: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("健身数据") },
                actions = {
                    IconButton(onClick = onSwitchToSilhouette) {
                        Icon(Icons.Filled.Accessibility, contentDescription = "剪影概览")
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
        if (recordCount == 0) {
            EmptyState(Modifier.padding(innerPadding).padding(contentPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(contentPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BodyPartGroup.entries.forEach { group ->
                    val groupItems = MetricDefinitions.items.filter { it.group == group }
                    item(key = "header_${group.name}") {
                        GroupHeader(group.displayName)
                    }
                    items(groupItems, key = { it.id }) { item ->
                        MetricCard(
                            item = item,
                            latestValues = latestValues,
                            onClick = { onItemClick(item.id) }
                        )
                    }
                    item(key = "spacer_${group.name}") { Spacer(Modifier.height(4.dp)) }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun GroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun MetricCard(
    item: MetricItem,
    latestValues: Map<String, Pair<Double, Long>>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.TrendingFlat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val latestTime = item.keys.mapNotNull { latestValues[it]?.second }.maxOrNull()
                Text(
                    text = latestTime?.let { "最近：" + it.toDateStr() } ?: "暂无数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ValuesColumn(item, latestValues)
        }
    }
}

@Composable
private fun ValuesColumn(
    item: MetricItem,
    latestValues: Map<String, Pair<Double, Long>>
) {
    Column(horizontalAlignment = Alignment.End) {
        if (item.isPaired) {
            val left = latestValues[item.keys[0]]?.first
            val right = latestValues[item.keys[1]]?.first
            Text(
                text = "左 ${left.fmtCmOrDash()}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "右 ${right.fmtCmOrDash()}",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            val v = latestValues[item.keys[0]]?.first
            Text(
                text = "${v.fmtCmOrDash()}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "还没有数据",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "点击右下角「录入」开始记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
