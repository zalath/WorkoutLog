package com.kzwdaw.kyguukk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kzwdaw.kyguukk.data.ExerciseDbHelper
import com.kzwdaw.kyguukk.data.ExerciseRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private fun Calendar.toYearMonth(): String {
    val fmt = SimpleDateFormat("yyyy-MM", Locale.US)
    return fmt.format(time)
}

private fun Calendar.toDayString(day: Int): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val c = this.clone() as Calendar
    c.set(Calendar.DAY_OF_MONTH, day)
    return fmt.format(c.time)
}

/** 格式化重量：整数省略小数点 */
private fun formatWeight(w: Double): String =
    if (w % 1.0 == 0.0) "${w.toInt()}kg" else "${w}kg"

/** 格式化明细：动作 · 重量 · 组数 */
private fun formatItem(item: ExerciseDbHelper.RecordItemRow): String = buildString {
    if (item.postureName.isNotEmpty()) {
        append(item.postureName)
    } else {
        append(item.categoryName)
    }
    append(" · ").append(formatWeight(item.weight))
    if (item.sets > 0) append(" · ").append(item.sets).append("组")
}

/**
 * 锻炼日历页：
 *  - 月历视图，有记录的日期用彩色圆点 + 分类名缩写
 *  - 点击某日期 → 弹出编辑（选择分类 → 选择动作 → 输入重量，可添加多条）
 *  - 下方列出当月所有记录列表，也可删除
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val repo = remember { ExerciseRepository.get(context) }

    var refreshTick by remember { mutableStateOf(0) }
    var categories by remember { mutableStateOf(listOf<ExerciseDbHelper.CategoryRow>()) }

    // 当前展示月份
    val viewMonth = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    var yearMonth by remember { mutableStateOf(viewMonth.toYearMonth()) }
    var records by remember { mutableStateOf(listOf<ExerciseDbHelper.RecordRow>()) }

    var editingDate by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshTick, yearMonth) {
        categories = repo.getCategories()
        records = repo.getRecordsByMonth(yearMonth)
    }

    val recordByDate: Map<String, ExerciseDbHelper.RecordRow> = records.associateBy { it.date }
    val todayStr: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("锻炼日历") },
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
        ) {
            // 月份切换
            val ym = yearMonth.split("-")
            val y = ym[0].toInt()
            val m = ym[1].toInt()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    val c = Calendar.getInstance().apply {
                        set(y, m - 1, 1)
                        add(Calendar.MONTH, -1)
                    }
                    yearMonth = c.toYearMonth()
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "上个月")
                }
                Text(
                    "${y}年${m}月",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = {
                    val c = Calendar.getInstance().apply {
                        set(y, m - 1, 1)
                        add(Calendar.MONTH, 1)
                    }
                    yearMonth = c.toYearMonth()
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "下个月")
                }
            }

            // 星期标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 日历网格
            val cal = Calendar.getInstance().apply {
                set(y, m - 1, 1)
            }
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            var firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
            // 转为周一起：Sun(1)->6, Mon(2)->0, .., Sat(7)->5
            firstDayOfWeek = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

            val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                var cellIdx = 0
                while (cellIdx < totalCells) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        repeat(7) { colIdx ->
                            val globalIdx = cellIdx + colIdx
                            val dayNum = globalIdx - firstDayOfWeek + 1
                            val isEmpty = dayNum < 1 || dayNum > daysInMonth
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .padding(2.dp)
                            ) {
                                if (!isEmpty) {
                                    val dateStr = cal.toDayString(dayNum)
                                    val record = recordByDate[dateStr]
                                    val isToday = (dateStr == todayStr)

                                    Card(
                                        onClick = { editingDate = dateStr },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isToday)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                                MaterialTheme.colorScheme.surfaceContainerLow
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                dayNum.toString(),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isToday)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (record != null) {
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    record.categoryNames.joinToString("").take(4),
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier
                                                        .background(
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    cellIdx += 7
                }
            }

            Spacer(Modifier.height(12.dp))

            // 当月记录列表
            Text(
                "当月记录（${records.size}）",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(records, key = { it.id }) { rec ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    rec.date,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    rec.items.forEach { item ->
                                        AssistChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    formatItem(item),
                                                    fontSize = 13.sp
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                            Row {
                                IconButton(onClick = { editingDate = rec.date }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "编辑",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = {
                                    repo.deleteRecord(rec.id)
                                    refreshTick++
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
                if (records.isEmpty()) {
                    item {
                        Text(
                            "当月还没有锻炼记录，点击日历上的任意日期添加。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }

    // 编辑对话框
    val editD = editingDate
    if (editD != null) {
        val existing = recordByDate[editD]

        // 当前编辑的明细列表
        val editItems = remember(editD) {
            mutableStateListOf<ExerciseDbHelper.ExerciseItemInput>().apply {
                existing?.items?.forEach {
                    add(ExerciseDbHelper.ExerciseItemInput(it.categoryName, it.postureName, it.weight, it.sets))
                }
            }
        }

        var selectedCatName by remember(editD) { mutableStateOf("") }
        var postureName by remember(editD) { mutableStateOf("") }
        // 重量（0~200 整数）、组数（1~10 整数），滑动选择
        var weightVal by remember(editD) { mutableStateOf(20) }
        var setsVal by remember(editD) { mutableStateOf(3) }

        // 选中分类下的姿势列表
        var posturesForCat by remember(editD) {
            mutableStateOf(listOf<ExerciseDbHelper.PostureRow>())
        }
        LaunchedEffect(selectedCatName, categories) {
            val cat = categories.find { it.name == selectedCatName }
            posturesForCat = if (cat != null) repo.getPostures(cat.id) else emptyList()
        }

        AlertDialog(
            onDismissRequest = { editingDate = null },
            title = { Text("${editD} 锻炼记录") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(max = 400.dp)
                ) {
                    // 已添加的明细列表
                    if (editItems.isNotEmpty()) {
                        Text(
                            "已添加（${editItems.size}）：",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        editItems.forEachIndexed { idx, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    buildString {
                                        append(item.categoryName)
                                        if (item.postureName.isNotEmpty()) {
                                            append(" · ").append(item.postureName)
                                        }
                                        append(" · ").append(formatWeight(item.weight))
                                        if (item.sets > 0) append(" · ").append(item.sets).append("组")
                                    },
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { editItems.removeAt(idx) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // 分类选择
                    Text(
                        "选择分类：",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = cat.name == selectedCatName,
                                onClick = {
                                    selectedCatName = if (cat.name == selectedCatName) "" else cat.name
                                    postureName = ""
                                },
                                label = { Text(cat.name) }
                            )
                        }
                    }

                    // 动作选择（从该分类的姿势库中选择）——单独一行
                    Text(
                        "选择动作：",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedCatName.isEmpty()) {
                        Text(
                            "请先选择上方分类",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else if (posturesForCat.isEmpty()) {
                        Text(
                            "该分类还没有动作，请到姿势库添加",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(posturesForCat) { p ->
                                FilterChip(
                                    selected = p.name == postureName,
                                    onClick = {
                                        postureName = if (p.name == postureName) "" else p.name
                                    },
                                    label = { Text(p.name) }
                                )
                            }
                        }
                    }

                    // 重量与组数——滑动选择，同一行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "重量 ${weightVal}kg",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Slider(
                                value = weightVal.toFloat(),
                                onValueChange = { weightVal = it.roundToInt() },
                                valueRange = 0f..200f,
                                steps = 199  // 0~200 共 201 个整数点
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "组数 ${setsVal}组",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Slider(
                                value = setsVal.toFloat(),
                                onValueChange = { setsVal = it.roundToInt() },
                                valueRange = 1f..10f,
                                steps = 8  // 1~10 共 10 个整数点
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (selectedCatName.isNotEmpty() && postureName.isNotEmpty()) {
                                editItems.add(
                                    ExerciseDbHelper.ExerciseItemInput(
                                        categoryName = selectedCatName,
                                        postureName = postureName,
                                        weight = weightVal.toDouble(),
                                        sets = setsVal
                                    )
                                )
                                postureName = ""
                            }
                        },
                        enabled = selectedCatName.isNotEmpty() && postureName.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("添加")
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (existing != null) {
                        TextButton(onClick = {
                            repo.deleteRecord(existing.id)
                            refreshTick++
                            editingDate = null
                        }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    Row {
                        TextButton(onClick = { editingDate = null }) {
                            Text("取消")
                        }
                        Button(onClick = {
                            repo.upsertRecordWithItems(editD, editItems.toList())
                            refreshTick++
                            editingDate = null
                        }) {
                            Text(if (existing == null) "添加" else "保存")
                        }
                    }
                }
            }
        )
    }
}
