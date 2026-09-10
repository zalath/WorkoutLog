package com.kzwdaw.kyguukk.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kzwdaw.kyguukk.data.ExerciseDbHelper
import com.kzwdaw.kyguukk.data.ExerciseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

/**
 * 生成 AI 姿势示意图：使用项目提供的 text_to_image 接口。
 * 图片下载后存到 app filesDir 下，文件路径作为 imageUri 持久化。
 */
private suspend fun generateAndSavePostureImage(
    filesDir: File,
    categoryName: String,
    postureName: String
): String? = withContext(Dispatchers.IO) {
    try {
        val prompt = "Anatomical fitness exercise illustration showing the correct posture and muscle group for exercise '$postureName' focusing on '$categoryName' body part, clear diagram with white background, instructional chart style"
        val size = "square"
        val url =
            "https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=${
                URLEncoder.encode(prompt, "UTF-8")
            }&image_size=$size"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 30_000
        conn.readTimeout = 60_000
        val code = conn.responseCode
        if (code != 200) return@withContext null
        val tmpDir = File(filesDir, "posture_images").apply { mkdirs() }
        val fname = "p_${System.currentTimeMillis()}_${
            (postureName.take(8) + categoryName.take(4)).replace(
                Regex("[^a-zA-Z0-9_\\-]"),
                "_"
            )
        }.png"
        val outFile = File(tmpDir, fname)
        conn.inputStream.use { ins ->
            FileOutputStream(outFile).use { fos -> ins.copyTo(fos) }
        }
        return@withContext outFile.absolutePath
    } catch (t: Throwable) {
        t.printStackTrace()
        null
    }
}

/**
 * 将相册选中的 content Uri 复制到 app 内部存储，返回本地文件路径。
 * 使用 Android Photo Picker，无需申请相册权限。
 */
private suspend fun copyUriToInternalStorage(
    context: Context,
    uri: Uri,
    filesDir: File,
    label: String
): String? = withContext(Dispatchers.IO) {
    try {
        val tmpDir = File(filesDir, "posture_images").apply { mkdirs() }
        val safeLabel = label.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(16)
        val fname = "p_${System.currentTimeMillis()}_$safeLabel.png"
        val outFile = File(tmpDir, fname)
        context.contentResolver.openInputStream(uri)?.use { ins ->
            FileOutputStream(outFile).use { fos -> ins.copyTo(fos) }
        } ?: return@withContext null
        outFile.absolutePath
    } catch (t: Throwable) {
        t.printStackTrace()
        null
    }
}

/**
 * 图片选择区域：点击调用系统 Photo Picker 选图，复制到内部存储后回调路径。
 */
@Composable
private fun ImagePickerBox(
    currentPath: String?,
    onPicked: (String?) -> Unit,
    modifier: Modifier,
    context: Context,
    filesDir: File,
    scope: kotlinx.coroutines.CoroutineScope,
    label: String
) {
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val path = copyUriToInternalStorage(context, uri, filesDir, label)
                if (path != null) onPicked(path)
            }
        }
    }

    Box(
        modifier = modifier
            .clickable {
                pickMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (currentPath != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(currentPath))
                    .crossfade(true)
                    .build(),
                contentDescription = "预览",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "点击选择相册图片",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "不选则自动生成",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 姿势库：
 *  - 分类横向 Chips（可增、删、重命名）
 *  - 选中分类 → 显示该分类姿势列表，每行名称 + 示意图；
 *    每个姿势可以增、删、改名，一键生成/刷新示意图
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostureLibraryScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val repo = remember { ExerciseRepository.get(context) }
    val scope = rememberCoroutineScope()
    val filesDir = context.filesDir

    var refreshTick by remember { mutableStateOf(0) }
    var categories by remember { mutableStateOf(listOf<ExerciseDbHelper.CategoryRow>()) }
    var currentCatId by remember { mutableStateOf<Long?>(null) }
    var postures by remember { mutableStateOf(listOf<ExerciseDbHelper.PostureRow>()) }

    LaunchedEffect(refreshTick) {
        categories = repo.getCategories()
        if (currentCatId == null || categories.none { it.id == currentCatId }) {
            currentCatId = categories.firstOrNull()?.id
        }
        postures = currentCatId?.let { repo.getPostures(it) } ?: emptyList()
    }

    LaunchedEffect(currentCatId, refreshTick) {
        postures = currentCatId?.let { repo.getPostures(it) } ?: emptyList()
    }

    // 对话框状态
    var addingCategory by remember { mutableStateOf(false) }
    var renamingCategory: ExerciseDbHelper.CategoryRow? by remember { mutableStateOf(null) }
    var addingPosture by remember { mutableStateOf(false) }
    var editingPosture: ExerciseDbHelper.PostureRow? by remember { mutableStateOf(null) }
    // 生成图片中 (postureId -> true)
    var generatingIdSet by remember { mutableStateOf(setOf<Long>()) }

    val currentCategory = categories.firstOrNull { it.id == currentCatId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("姿势库") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { addingCategory = true }) {
                        Icon(Icons.Default.Add, contentDescription = "新分类")
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentCategory != null) {
                IconButton(
                    onClick = { addingPosture = true },
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加姿势", tint = Color.White)
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(contentPadding)
        ) {
            // 分类 Chips 行（横向滑动）
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(categories, key = { it.id }) { cat ->
                    FilterChip(
                        selected = cat.id == currentCatId,
                        onClick = { currentCatId = cat.id },
                        label = { Text(cat.name) }
                    )
                }
                item {
                    IconButton(onClick = { addingCategory = true }) {
                        Icon(Icons.Default.Add, contentDescription = "新分类")
                    }
                }
            }

            val cat = currentCategory
            if (cat == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无分类，点击右上角 + 新建")
                }
            } else {
                // 当前分类的重命名/删除
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "「${cat.name}」分类",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row {
                        IconButton(onClick = { renamingCategory = cat }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "重命名",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (categories.size > 1) {
                            IconButton(onClick = {
                                repo.deleteCategory(cat.id)
                                refreshTick++
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除分类",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // 姿势列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp, 4.dp, 12.dp, 100.dp)
                ) {
                    items(postures, key = { it.id }) { p ->
                        val isGenerating = generatingIdSet.contains(p.id)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 姿势示意图
                                Box(
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (p.imageUri != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(File(p.imageUri))
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = p.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.Image,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "无图",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                Text(
                                    p.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = {
                                            generatingIdSet = generatingIdSet + p.id
                                            scope.launch {
                                                val path = generateAndSavePostureImage(
                                                    filesDir,
                                                    categories.first { it.id == currentCatId }.name,
                                                    p.name
                                                )
                                                if (path != null) {
                                                    repo.updatePosture(p.id, p.name, path)
                                                    postures = repo.getPostures(currentCatId!!)
                                                }
                                                generatingIdSet = generatingIdSet - p.id
                                            }
                                        },
                                        enabled = !isGenerating
                                    ) {
                                        Icon(
                                            if (isGenerating) Icons.Default.Refresh else Icons.Default.Image,
                                            contentDescription = "生成示意图",
                                            tint = if (isGenerating)
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            else
                                                MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { editingPosture = p }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "编辑",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = {
                                        // 同时删除可能存在的图片文件
                                        p.imageUri?.let { File(it).delete() }
                                        repo.deletePosture(p.id)
                                        postures = repo.getPostures(currentCatId!!)
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
                    if (postures.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "该分类下暂无姿势，点击右下角 + 新建。",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- 对话框 ----

    if (addingCategory) {
        val (name, setName) = remember { mutableStateOf("") to fun(v: String) {} }
        var v by remember(addingCategory) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addingCategory = false },
            title = { Text("新增分类") },
            text = {
                OutlinedTextField(
                    value = v,
                    onValueChange = { v = it },
                    label = { Text("分类名称（如：胸）") },
                    singleLine = true
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { addingCategory = false }) { Text("取消") }
                    Button(onClick = {
                        if (v.isNotBlank()) {
                            repo.addCategory(v.trim())
                            refreshTick++
                        }
                        addingCategory = false
                    }) { Text("添加") }
                }
            }
        )
    }

    val rc = renamingCategory
    if (rc != null) {
        var v by remember(rc) { mutableStateOf(rc.name) }
        AlertDialog(
            onDismissRequest = { renamingCategory = null },
            title = { Text("重命名分类") },
            text = {
                OutlinedTextField(
                    value = v,
                    onValueChange = { v = it },
                    label = { Text("新名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { renamingCategory = null }) { Text("取消") }
                    Button(onClick = {
                        if (v.isNotBlank()) {
                            repo.renameCategory(rc.id, v.trim())
                            refreshTick++
                        }
                        renamingCategory = null
                    }) { Text("保存") }
                }
            }
        )
    }

    if (addingPosture) {
        var v by remember(addingPosture) { mutableStateOf("") }
        var pickedImagePath by remember(addingPosture) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { addingPosture = false },
            title = { Text("在「${currentCategory?.name}」下新增姿势") },
            text = {
                Column {
                    ImagePickerBox(
                        currentPath = pickedImagePath,
                        onPicked = { pickedImagePath = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        context = context,
                        filesDir = filesDir,
                        scope = scope,
                        label = v.ifBlank { "posture" }
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = v,
                        onValueChange = { v = it },
                        label = { Text("动作名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { addingPosture = false }) { Text("取消") }
                    Button(onClick = {
                        if (v.isNotBlank() && currentCategory != null) {
                            val newId = repo.addPosture(currentCategory.id, v.trim(), pickedImagePath)
                            // 只有用户没有手动选图时，才自动生成 AI 示意图
                            if (pickedImagePath == null) {
                                generatingIdSet = generatingIdSet + newId
                                scope.launch {
                                    val path = generateAndSavePostureImage(
                                        filesDir,
                                        currentCategory.name,
                                        v.trim()
                                    )
                                    if (path != null) {
                                        repo.updatePosture(newId, v.trim(), path)
                                    }
                                    generatingIdSet = generatingIdSet - newId
                                    postures = repo.getPostures(currentCategory.id)
                                }
                            }
                            postures = repo.getPostures(currentCategory.id)
                        }
                        addingPosture = false
                    }) { Text("添加") }
                }
            }
        )
    }

    val ep = editingPosture
    if (ep != null) {
        var v by remember(ep) { mutableStateOf(ep.name) }
        var editImagePath by remember(ep) { mutableStateOf<String?>(ep.imageUri) }
        AlertDialog(
            onDismissRequest = { editingPosture = null },
            title = { Text("编辑姿势") },
            text = {
                Column {
                    ImagePickerBox(
                        currentPath = editImagePath,
                        onPicked = {
                            // 替换图：删旧文件
                            editImagePath?.let { File(it).delete() }
                            editImagePath = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        context = context,
                        filesDir = filesDir,
                        scope = scope,
                        label = v.ifBlank { "posture" }
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = v,
                        onValueChange = { v = it },
                        label = { Text("动作名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        // 只清图，不改名字
                        if (editImagePath != null) {
                            File(editImagePath).delete()
                            editImagePath = null
                            repo.updatePosture(ep.id, ep.name, null)
                            postures = currentCatId?.let { repo.getPostures(it) } ?: emptyList()
                        }
                        editingPosture = null
                    }) {
                        Text("清除图片", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { editingPosture = null }) { Text("取消") }
                    Button(onClick = {
                        if (v.isNotBlank()) {
                            repo.updatePosture(ep.id, v.trim(), editImagePath)
                            postures = currentCatId?.let { repo.getPostures(it) } ?: emptyList()
                        }
                        editingPosture = null
                    }) { Text("保存") }
                }
            }
        )
    }
}
