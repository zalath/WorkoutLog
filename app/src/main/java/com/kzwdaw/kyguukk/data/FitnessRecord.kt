package com.kzwdaw.kyguukk.data

/**
 * 一次测量记录。每次录入不要求填写全部字段，未填写为 null。
 * 存储在 SQLite（databases 目录），清理缓存不会清除。
 */
data class FitnessRecord(
    val id: Long,
    val recordedAt: Long,            // 毫秒时间戳
    val values: Map<String, Double?> // key -> 厘米值
) {
    companion object {
        val EMPTY_VALUES: Map<String, Double?> =
            MetricDefinitions.allKeys.associateWith { null }
    }
}
