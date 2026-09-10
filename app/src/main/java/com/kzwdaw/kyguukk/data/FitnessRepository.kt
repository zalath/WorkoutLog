package com.kzwdaw.kyguukk.data

import android.content.Context

/**
 * 数据访问门面。单例，随 Application 生命周期。
 */
class FitnessRepository private constructor(context: Context) {

    private val helper = FitnessDbHelper(context.applicationContext)

    /** 新增记录，仅写入传入的非空值。 */
    fun addRecord(recordedAt: Long, values: Map<String, Double?>): Long =
        helper.insert(recordedAt, values)

    fun deleteRecord(id: Long) = helper.delete(id)

    /** 全部记录（时间倒序）。 */
    fun getAllRecords(): List<FitnessRecord> = helper.getAll()

    /** 某 series 的历史点（时间升序），供折线图。 */
    fun getSeriesHistory(key: String): List<SeriesPoint> = helper.getSeriesHistory(key)

    /**
     * 每个 series 的最近一次录入值，供整体概览展示。
     * key -> (value, recordedAt)
     */
    fun getLatestValues(): Map<String, Pair<Double, Long>> {
        val latest = HashMap<String, Pair<Double, Long>>()
        // getAll 已按时间倒序，取每个 key 第一个非空值
        for (record in getAllRecords()) {
            MetricDefinitions.allKeys.forEach { key ->
                if (key !in latest && record.values[key] != null) {
                    latest[key] = record.values[key]!! to record.recordedAt
                }
            }
            if (latest.size == MetricDefinitions.allKeys.size) return latest
        }
        return latest
    }

    companion object {
        @Volatile private var instance: FitnessRepository? = null
        fun get(context: Context): FitnessRepository =
            instance ?: synchronized(this) {
                instance ?: FitnessRepository(context).also { instance = it }
            }
    }
}
