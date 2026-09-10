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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kzwdaw.kyguukk.data.BodyPartGroup
import com.kzwdaw.kyguukk.data.MetricDefinitions
import com.kzwdaw.kyguukk.data.MetricItem
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    onSave: (recordedAt: Long, values: Map<String, Double?>) -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    // 以今天 00:00 为默认日期
    val todayUtc = remember {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = todayUtc)
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    // 每个字段一个文本状态，key -> 输入字符串
    val fieldStates = MetricDefinitions.allKeys.associateWith { key ->
        rememberSaveable { mutableStateOf("") }
    }

    val recordedAtMillis = (datePickerState.selectedDateMillis ?: todayUtc)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("录入数据") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("日期：${recordedAtMillis.toDateStr()}")
            }

            BodyPartGroup.entries.forEach { group ->
                Text(
                    text = group.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                MetricDefinitions.items.filter { it.group == group }.forEach { item ->
                    MetricInputRow(item, fieldStates)
                }
            }

            Spacer(Modifier.height(8.dp))

            val hasAnyValue = fieldStates.values.any { it.value.isNotBlank() }
            Button(
                onClick = {
                    val values = MetricDefinitions.allKeys.associateWith { k ->
                        fieldStates[k]!!.value.trim().toDoubleOrNull()
                    }
                    onSave(recordedAtMillis, values)
                },
                enabled = hasAnyValue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
            Text(
                text = "提示：无需全部填写，只录入本次测量的项即可。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricInputRow(
    item: MetricItem,
    fieldStates: Map<String, androidx.compose.runtime.MutableState<String>>
) {
    if (item.isPaired) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val leftKey = item.keys[0]
            val rightKey = item.keys[1]
            OutlinedTextField(
                value = fieldStates[leftKey]!!.value,
                onValueChange = { fieldStates[leftKey]!!.value = it },
                label = { Text("左 (cm)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = fieldStates[rightKey]!!.value,
                onValueChange = { fieldStates[rightKey]!!.value = it },
                label = { Text("右 (cm)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        val key = item.keys[0]
        OutlinedTextField(
            value = fieldStates[key]!!.value,
            onValueChange = { fieldStates[key]!!.value = it },
            label = { Text("${item.name} (cm)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
