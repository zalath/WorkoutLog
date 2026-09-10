package com.kzwdaw.kyguukk.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 扩展 SQLite 存储：
 *  - exercise_categories 锻炼分类（胸、背、肩...）
 *  - exercise_postures    每个分类下的姿势（动作名 + 示意图片路径）
 *  - exercise_records     每日锻炼记录（日期）
 *  - exercise_record_items 记录明细（分类 + 动作 + 重量 + 组数），关联 record
 */
class ExerciseDbHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "fitness_exercise.db"
        private const val DB_VERSION = 3

        private const val TABLE_CATEGORY = "exercise_categories"
        private const val TABLE_POSTURE = "exercise_postures"
        private const val TABLE_RECORD = "exercise_records"
        private const val TABLE_RECORD_ITEM = "exercise_record_items"

        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_ORDER = "_order"

        // Posture columns
        const val COL_CATEGORY_ID = "category_id"
        const val COL_POSTURE_IMAGE = "image_uri"  // 图片存储 URI，可为空

        // Record columns
        const val COL_DATE = "record_date"   // yyyy-MM-dd 字符串
        const val COL_CATEGORY_NAMES = "category_names"  // 兼容旧版，逗号分隔

        // Record item columns
        const val COL_RECORD_ID = "record_id"
        const val COL_ITEM_CATEGORY = "item_category"
        const val COL_ITEM_POSTURE = "item_posture"
        const val COL_ITEM_WEIGHT = "item_weight"
        const val COL_ITEM_SETS = "item_sets"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_CATEGORY (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT NOT NULL,
                $COL_ORDER INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_POSTURE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CATEGORY_ID INTEGER NOT NULL,
                $COL_NAME TEXT NOT NULL,
                $COL_POSTURE_IMAGE TEXT,
                $COL_ORDER INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY ($COL_CATEGORY_ID) REFERENCES $TABLE_CATEGORY($COL_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_RECORD (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_DATE TEXT NOT NULL UNIQUE,
                $COL_CATEGORY_NAMES TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_RECORD_ITEM (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_RECORD_ID INTEGER NOT NULL,
                $COL_ITEM_CATEGORY TEXT NOT NULL,
                $COL_ITEM_POSTURE TEXT NOT NULL DEFAULT '',
                $COL_ITEM_WEIGHT REAL NOT NULL DEFAULT 0,
                $COL_ITEM_SETS INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY ($COL_RECORD_ID) REFERENCES $TABLE_RECORD($COL_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_rec_date ON $TABLE_RECORD($COL_DATE)")
        db.execSQL("CREATE INDEX idx_item_rec ON $TABLE_RECORD_ITEM($COL_RECORD_ID)")

        // 初始化默认分类：胸、背、肩、臂、腿、臀
        val defaultCategories = listOf("胸", "背", "肩", "臂", "腿", "臀")
        db.beginTransaction()
        try {
            defaultCategories.forEachIndexed { idx, name ->
                val cv = ContentValues().apply {
                    put(COL_NAME, name)
                    put(COL_ORDER, idx)
                }
                db.insert(TABLE_CATEGORY, null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // v2: 新增 exercise_record_items 表
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_RECORD_ITEM (
                    $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_RECORD_ID INTEGER NOT NULL,
                    $COL_ITEM_CATEGORY TEXT NOT NULL,
                    $COL_ITEM_POSTURE TEXT NOT NULL DEFAULT '',
                    $COL_ITEM_WEIGHT REAL NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_item_rec ON $TABLE_RECORD_ITEM($COL_RECORD_ID)")
            // 迁移旧记录：把 category_names 拆成 items
            val c = db.query(
                TABLE_RECORD, arrayOf(COL_ID, COL_CATEGORY_NAMES),
                null, null, null, null, null
            )
            c.use {
                while (it.moveToNext()) {
                    val recId = it.getLong(0)
                    val namesStr = it.getString(1)
                    val names = namesStr.split(",").map(String::trim).filter { n -> n.isNotEmpty() }
                    for (name in names) {
                        val cv = ContentValues().apply {
                            put(COL_RECORD_ID, recId)
                            put(COL_ITEM_CATEGORY, name)
                            put(COL_ITEM_POSTURE, "")
                            put(COL_ITEM_WEIGHT, 0.0)
                        }
                        db.insert(TABLE_RECORD_ITEM, null, cv)
                    }
                }
            }
        }
        if (oldVersion < 3) {
            // v3: 新增组数列
            db.execSQL("ALTER TABLE $TABLE_RECORD_ITEM ADD COLUMN $COL_ITEM_SETS INTEGER NOT NULL DEFAULT 0")
        }
    }

    // --- Categories ---

    data class CategoryRow(val id: Long, val name: String, val order: Int)

    fun getAllCategories(): List<CategoryRow> {
        val c = readableDatabase.query(
            TABLE_CATEGORY,
            arrayOf(COL_ID, COL_NAME, COL_ORDER),
            null, null, null, null, "$COL_ORDER ASC, $COL_ID ASC"
        )
        return c.use {
            val list = ArrayList<CategoryRow>(it.count)
            while (it.moveToNext()) {
                list.add(
                    CategoryRow(
                        id = it.getLong(0),
                        name = it.getString(1),
                        order = it.getInt(2)
                    )
                )
            }
            list
        }
    }

    fun addCategory(name: String): Long {
        val list = getAllCategories()
        val nextOrder = (list.maxOfOrNull { it.order } ?: -1) + 1
        val cv = ContentValues().apply {
            put(COL_NAME, name)
            put(COL_ORDER, nextOrder)
        }
        return writableDatabase.insert(TABLE_CATEGORY, null, cv)
    }

    fun renameCategory(id: Long, newName: String) {
        val cv = ContentValues().apply { put(COL_NAME, newName) }
        writableDatabase.update(TABLE_CATEGORY, cv, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun deleteCategory(id: Long) {
        val db = writableDatabase
        val catName = getNameById(id) ?: return
        db.delete(TABLE_CATEGORY, "$COL_ID = ?", arrayOf(id.toString()))
        // 删除引用此分类名的记录明细
        db.delete(TABLE_RECORD_ITEM, "$COL_ITEM_CATEGORY = ?", arrayOf(catName))
    }

    private fun getNameById(id: Long): String? {
        val c = readableDatabase.query(
            TABLE_CATEGORY, arrayOf(COL_NAME),
            "$COL_ID = ?", arrayOf(id.toString()),
            null, null, null, "1"
        )
        return c.use { if (it.moveToFirst()) it.getString(0) else null }
    }

    fun reorderCategories(orderedIds: List<Long>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            orderedIds.forEachIndexed { idx, id ->
                val cv = ContentValues().apply { put(COL_ORDER, idx) }
                db.update(TABLE_CATEGORY, cv, "$COL_ID = ?", arrayOf(id.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // --- Postures ---

    data class PostureRow(
        val id: Long,
        val categoryId: Long,
        val name: String,
        val imageUri: String?,
        val order: Int
    )

    fun getPostures(categoryId: Long): List<PostureRow> {
        val c = readableDatabase.query(
            TABLE_POSTURE,
            arrayOf(COL_ID, COL_CATEGORY_ID, COL_NAME, COL_POSTURE_IMAGE, COL_ORDER),
            "$COL_CATEGORY_ID = ?", arrayOf(categoryId.toString()),
            null, null, "$COL_ORDER ASC, $COL_ID ASC"
        )
        return c.use {
            val list = ArrayList<PostureRow>(it.count)
            while (it.moveToNext()) {
                list.add(
                    PostureRow(
                        id = it.getLong(0),
                        categoryId = it.getLong(1),
                        name = it.getString(2),
                        imageUri = if (it.isNull(3)) null else it.getString(3),
                        order = it.getInt(4)
                    )
                )
            }
            list
        }
    }

    fun addPosture(categoryId: Long, name: String, imageUri: String?): Long {
        val list = getPostures(categoryId)
        val nextOrder = (list.maxOfOrNull { it.order } ?: -1) + 1
        val cv = ContentValues().apply {
            put(COL_CATEGORY_ID, categoryId)
            put(COL_NAME, name)
            if (imageUri == null) putNull(COL_POSTURE_IMAGE) else put(COL_POSTURE_IMAGE, imageUri)
            put(COL_ORDER, nextOrder)
        }
        return writableDatabase.insert(TABLE_POSTURE, null, cv)
    }

    fun updatePosture(id: Long, name: String, imageUri: String?) {
        val cv = ContentValues().apply {
            put(COL_NAME, name)
            if (imageUri == null) putNull(COL_POSTURE_IMAGE) else put(COL_POSTURE_IMAGE, imageUri)
        }
        writableDatabase.update(TABLE_POSTURE, cv, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun deletePosture(id: Long) {
        writableDatabase.delete(TABLE_POSTURE, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun reorderPostures(categoryId: Long, orderedIds: List<Long>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            orderedIds.forEachIndexed { idx, id ->
                val cv = ContentValues().apply { put(COL_ORDER, idx) }
                db.update(TABLE_POSTURE, cv, "$COL_ID = ? AND $COL_CATEGORY_ID = ?",
                    arrayOf(id.toString(), categoryId.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // --- Exercise Record Items ---

    /** 一条锻炼明细（读） */
    data class RecordItemRow(
        val id: Long,
        val recordId: Long,
        val categoryName: String,
        val postureName: String,
        val weight: Double,
        val sets: Int
    )

    /** 输入用简化结构 */
    data class ExerciseItemInput(
        val categoryName: String,
        val postureName: String,
        val weight: Double,
        val sets: Int
    )

    private fun getRecordItems(recordId: Long): List<RecordItemRow> {
        val c = readableDatabase.query(
            TABLE_RECORD_ITEM,
            arrayOf(COL_ID, COL_RECORD_ID, COL_ITEM_CATEGORY, COL_ITEM_POSTURE, COL_ITEM_WEIGHT, COL_ITEM_SETS),
            "$COL_RECORD_ID = ?", arrayOf(recordId.toString()),
            null, null, "$COL_ID ASC"
        )
        return c.use {
            val list = ArrayList<RecordItemRow>(it.count)
            while (it.moveToNext()) {
                list.add(
                    RecordItemRow(
                        id = it.getLong(0),
                        recordId = it.getLong(1),
                        categoryName = it.getString(2),
                        postureName = it.getString(3),
                        weight = it.getDouble(4),
                        sets = it.getInt(5)
                    )
                )
            }
            list
        }
    }

    // --- Exercise Records ---

    data class RecordRow(
        val id: Long,
        val date: String,  // yyyy-MM-dd
        val items: List<RecordItemRow>
    ) {
        /** 从明细中派生分类名（兼容旧调用方） */
        val categoryNames: List<String> get() = items.map { it.categoryName }.distinct()
    }

    fun getAllRecords(): List<RecordRow> {
        val c = readableDatabase.query(
            TABLE_RECORD,
            arrayOf(COL_ID, COL_DATE),
            null, null, null, null, "$COL_DATE DESC"
        )
        return c.use {
            val list = ArrayList<RecordRow>(it.count)
            while (it.moveToNext()) {
                val recId = it.getLong(0)
                list.add(RecordRow(recId, it.getString(1), getRecordItems(recId)))
            }
            list
        }
    }

    fun getRecordsByMonth(yearMonth: String): List<RecordRow> {
        val c = readableDatabase.query(
            TABLE_RECORD,
            arrayOf(COL_ID, COL_DATE),
            "substr($COL_DATE,1,7) = ?", arrayOf(yearMonth),
            null, null, "$COL_DATE ASC"
        )
        return c.use {
            val list = ArrayList<RecordRow>(it.count)
            while (it.moveToNext()) {
                val recId = it.getLong(0)
                list.add(RecordRow(recId, it.getString(1), getRecordItems(recId)))
            }
            list
        }
    }

    /** 新增或替换某一天的锻炼记录 + 明细。空列表则删除当日记录。 */
    fun upsertRecordWithItems(date: String, items: List<ExerciseItemInput>): Long {
        val db = writableDatabase
        // 查找是否已有记录
        val existing = db.query(
            TABLE_RECORD, arrayOf(COL_ID),
            "$COL_DATE = ?", arrayOf(date),
            null, null, null, "1"
        )
        val existingId = existing.use { if (it.moveToFirst()) it.getLong(0) else null }

        if (items.isEmpty()) {
            // 没有明细 → 删除记录
            if (existingId != null) {
                db.delete(TABLE_RECORD_ITEM, "$COL_RECORD_ID = ?", arrayOf(existingId.toString()))
                db.delete(TABLE_RECORD, "$COL_ID = ?", arrayOf(existingId.toString()))
            }
            return existingId ?: -1L
        }

        // 派生分类名（兼容旧 category_names 列）
        val catNames = items.map { it.categoryName }.distinct().joinToString(",")

        val recordId = if (existingId != null) {
            // 清除旧明细
            db.delete(TABLE_RECORD_ITEM, "$COL_RECORD_ID = ?", arrayOf(existingId.toString()))
            val cv = ContentValues().apply { put(COL_CATEGORY_NAMES, catNames) }
            db.update(TABLE_RECORD, cv, "$COL_ID = ?", arrayOf(existingId.toString()))
            existingId
        } else {
            val cv = ContentValues().apply {
                put(COL_DATE, date)
                put(COL_CATEGORY_NAMES, catNames)
            }
            db.insert(TABLE_RECORD, null, cv)
        }

        // 插入新明细
        for (item in items) {
            val cv = ContentValues().apply {
                put(COL_RECORD_ID, recordId)
                put(COL_ITEM_CATEGORY, item.categoryName)
                put(COL_ITEM_POSTURE, item.postureName)
                put(COL_ITEM_WEIGHT, item.weight)
                put(COL_ITEM_SETS, item.sets)
            }
            db.insert(TABLE_RECORD_ITEM, null, cv)
        }
        return recordId
    }

    fun deleteRecord(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_RECORD_ITEM, "$COL_RECORD_ID = ?", arrayOf(id.toString()))
        db.delete(TABLE_RECORD, "$COL_ID = ?", arrayOf(id.toString()))
    }
}
