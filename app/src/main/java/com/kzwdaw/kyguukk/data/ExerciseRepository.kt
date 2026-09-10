package com.kzwdaw.kyguukk.data

import android.content.Context

/**
 * 锻炼相关门面（分类 / 姿势 / 日历记录 + 明细）。
 */
class ExerciseRepository private constructor(context: Context) {

    private val helper = ExerciseDbHelper(context.applicationContext)

    // Categories
    fun getCategories(): List<ExerciseDbHelper.CategoryRow> = helper.getAllCategories()
    fun addCategory(name: String) = helper.addCategory(name)
    fun renameCategory(id: Long, newName: String) = helper.renameCategory(id, newName)
    fun deleteCategory(id: Long) = helper.deleteCategory(id)
    fun reorderCategories(orderedIds: List<Long>) = helper.reorderCategories(orderedIds)

    // Postures
    fun getPostures(categoryId: Long): List<ExerciseDbHelper.PostureRow> = helper.getPostures(categoryId)
    fun addPosture(categoryId: Long, name: String, imageUri: String?) = helper.addPosture(categoryId, name, imageUri)
    fun updatePosture(id: Long, name: String, imageUri: String?) = helper.updatePosture(id, name, imageUri)
    fun deletePosture(id: Long) = helper.deletePosture(id)
    fun reorderPostures(categoryId: Long, orderedIds: List<Long>) = helper.reorderPostures(categoryId, orderedIds)

    // Exercise records (by date, with items)
    fun getAllRecords(): List<ExerciseDbHelper.RecordRow> = helper.getAllRecords()
    fun getRecordsByMonth(yearMonth: String): List<ExerciseDbHelper.RecordRow> = helper.getRecordsByMonth(yearMonth)
    fun upsertRecordWithItems(date: String, items: List<ExerciseDbHelper.ExerciseItemInput>) =
        helper.upsertRecordWithItems(date, items)
    fun deleteRecord(id: Long) = helper.deleteRecord(id)

    companion object {
        @Volatile private var instance: ExerciseRepository? = null
        fun get(context: Context): ExerciseRepository =
            instance ?: synchronized(this) {
                instance ?: ExerciseRepository(context).also { instance = it }
            }
    }
}
