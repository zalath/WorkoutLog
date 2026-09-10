package com.kzwdaw.kyguukk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 右下角组合 FAB：
 *  三个按钮风格统一：统一 ExtendedFAB（图标 + 文字）、统一形状、统一颜色风格、统一高度。
 *  垂直堆叠：
 *      📅 锻炼日历    （在上）
 *      💪 姿势库      （在中）
 *      ➕ 录入        （在下，主按钮，primary色）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFABs(
    onCalendarClick: () -> Unit,
    onPostureClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val shape = ShapeDefaults.ExtraLarge
    val iconSize = 20.dp
    val spacer = 6.dp

    Box(contentAlignment = Alignment.BottomEnd) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 顶部：锻炼日历（secondary container，辅助色）
            ExtendedFloatingActionButton(
                onClick = onCalendarClick,
                shape = shape,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                elevation = FloatingActionButtonDefaults.elevation()
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize)
                )
                Spacer(Modifier.width(spacer))
                Text("锻炼记录")
            }

            // 中部：姿势库（tertiary container）
            ExtendedFloatingActionButton(
                onClick = onPostureClick,
                shape = shape,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                elevation = FloatingActionButtonDefaults.elevation()
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize)
                )
                Spacer(Modifier.width(spacer))
                Text("姿势库")
            }

            // 底部：录入（primary 主色，主按钮）
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                shape = shape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation()
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize + 2.dp)
                )
                Spacer(Modifier.width(spacer))
                Text("录入")
            }
        }
    }
    // Spacer 用于占位，避免按钮紧贴屏幕右/下边缘时的过度贴边（Scaffold 已处理 FAB 位置 padding）
    Spacer(Modifier.height(0.dp))
}
