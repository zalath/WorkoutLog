package com.kzwdaw.kyguukk.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite 存储。数据库文件位于 app 的 databases 目录，
 * 清理"缓存"不会被清除（清理"数据"才会）。
 */
class FitnessDbHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "fitness.db"
        private const val DB_VERSION = 1
        private const val TABLE = "records"
        private const val COL_ID = "id"
        private const val COL_RECORDED_AT = "recorded_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val columns = MetricDefinitions.columnDefinitions.joinToString(", ") { (key, _) ->
            "$key REAL"
        }
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_RECORDED_AT INTEGER NOT NULL,
                $columns
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX idx_${TABLE}_time ON $TABLE($COL_RECORDED_AT)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    fun insert(recordedAt: Long, values: Map<String, Double?>): Long {
        val cv = ContentValues().apply {
            put(COL_RECORDED_AT, recordedAt)
            values.forEach { (key, v) -> if (v != null) put(key, v) }
        }
        val db = writableDatabase
        return db.insert(TABLE, null, cv)
    }

    fun delete(id: Long) {
        writableDatabase.delete(TABLE, "$COL_ID = ?", arrayOf(id.toString()))
    }

    /** 全部记录，按时间倒序。 */
    fun getAll(): List<FitnessRecord> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE,
            arrayOf(COL_ID, COL_RECORDED_AT) + MetricDefinitions.allKeys.toTypedArray(),
            null, null, null, null, "$COL_RECORDED_AT DESC"
        )
        return cursor.use { c ->
            val result = ArrayList<FitnessRecord>(c.count)
            val keyIndex = MetricDefinitions.allKeys.associateWith { c.getColumnIndex(it) }
            while (c.moveToNext()) {
                val values = MetricDefinitions.allKeys.associateWith { k ->
                    val idx = keyIndex[k]!!
                    if (c.isNull(idx)) null else c.getDouble(idx)
                }
                result.add(
                    FitnessRecord(
                        id = c.getLong(c.getColumnIndexOrThrow(COL_ID)),
                        recordedAt = c.getLong(c.getColumnIndexOrThrow(COL_RECORDED_AT)),
                        values = values
                    )
                )
            }
            result
        }
    }

    /** 某一列（series key）的历史值，按时间升序，供折线图使用。 */
    fun getSeriesHistory(key: String): List<SeriesPoint> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE,
            arrayOf(COL_ID, COL_RECORDED_AT, key),
            "$key IS NOT NULL",
            null, null, null, "$COL_RECORDED_AT ASC"
        )
        return cursor.use { c ->
            val list = ArrayList<SeriesPoint>(c.count)
            while (c.moveToNext()) {
                list.add(
                    SeriesPoint(
                        recordId = c.getLong(c.getColumnIndexOrThrow(COL_ID)),
                        recordedAt = c.getLong(c.getColumnIndexOrThrow(COL_RECORDED_AT)),
                        value = c.getDouble(c.getColumnIndexOrThrow(key))
                    )
                )
            }
            list
        }
    }
}

data class SeriesPoint(
    val recordId: Long,
    val recordedAt: Long,
    val value: Double
)
