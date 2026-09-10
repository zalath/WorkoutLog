package com.kzwdaw.kyguukk.data

/**
 * 体型测量项目定义。
 *
 * 每个MetricItem对应"一项"（用于折线图与卡片展示），
 * 其中胳膊和腿的项目含左右两条数据（series），其余为单条。
 * 数据库列为 series.key（如 upper_arm_left）。
 */
enum class BodyPartGroup(val displayName: String) {
    SHOULDER("肩部"),
    ARM("手臂"),
    TORSO("躯干"),
    LEG("腿部")
}

enum class MetricSide(val suffix: String) {
    SINGLE(""),
    LEFT("·左"),
    RIGHT("·右")
}

data class MetricSeries(
    val key: String,            // 数据库列名，如 upper_arm_left
    val side: MetricSide
) {
    val label: String get() = side.suffix
}

data class MetricItem(
    val id: String,             // 业务标识，如 upper_arm
    val name: String,           // 中文名，如 大臂围
    val group: BodyPartGroup,
    val series: List<MetricSeries>
) {
    val keys: List<String> get() = series.map { it.key }
    val isPaired: Boolean get() = series.size > 1
}

object MetricDefinitions {

    val items: List<MetricItem> = listOf(
        MetricItem("shoulder_width", "肩宽", BodyPartGroup.SHOULDER,
            listOf(MetricSeries("shoulder_width", MetricSide.SINGLE))),
        MetricItem("shoulder_thickness", "肩厚", BodyPartGroup.SHOULDER,
            listOf(MetricSeries("shoulder_thickness", MetricSide.SINGLE))),
        MetricItem("upper_arm", "大臂围", BodyPartGroup.ARM,
            listOf(MetricSeries("upper_arm_left", MetricSide.LEFT),
                MetricSeries("upper_arm_right", MetricSide.RIGHT))),
        MetricItem("forearm", "小臂围", BodyPartGroup.ARM,
            listOf(MetricSeries("forearm_left", MetricSide.LEFT),
                MetricSeries("forearm_right", MetricSide.RIGHT))),
        MetricItem("chest", "胸围", BodyPartGroup.TORSO,
            listOf(MetricSeries("chest", MetricSide.SINGLE))),
        MetricItem("waist", "腰围", BodyPartGroup.TORSO,
            listOf(MetricSeries("waist", MetricSide.SINGLE))),
        MetricItem("hip", "臀围", BodyPartGroup.TORSO,
            listOf(MetricSeries("hip", MetricSide.SINGLE))),
        MetricItem("thigh", "大腿围", BodyPartGroup.LEG,
            listOf(MetricSeries("thigh_left", MetricSide.LEFT),
                MetricSeries("thigh_right", MetricSide.RIGHT))),
        MetricItem("calf", "小腿围", BodyPartGroup.LEG,
            listOf(MetricSeries("calf_left", MetricSide.LEFT),
                MetricSeries("calf_right", MetricSide.RIGHT))),
    )

    val allKeys: List<String> = items.flatMap { it.keys }

    val keyToSeries: Map<String, MetricSeries> =
        items.flatMap { it.series }.associateBy { it.key }

    val keyToItem: Map<String, MetricItem> =
        buildMap {
            items.forEach { item -> item.keys.forEach { put(it, item) } }
        }

    fun itemById(id: String): MetricItem = items.first { it.id == id }

    /** 所有测量列，按分组顺序排列，供数据库建表使用。 */
    val columnDefinitions: List<Pair<String, MetricSide>> =
        items.flatMap { it.series.map { s -> s.key to s.side } }
}
