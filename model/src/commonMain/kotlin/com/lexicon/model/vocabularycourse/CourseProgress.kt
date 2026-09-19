package com.lexicon.model.vocabularycourse

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class ProgressMetricType { VOCABULARY, ACCURACY }

data class ProgressMetric(
    val type: ProgressMetricType,
    val current: Int,
    val target: Int,
    val isMeasured: Boolean = true,
) {
    val fraction: Double get() = if (target <= 0) 0.0 else (current.toDouble() / target).coerceIn(0.0, 1.0)
}

data class CourseProgress(
    val metrics: ImmutableList<ProgressMetric> = persistentListOf(),
) {
    fun metric(type: ProgressMetricType): ProgressMetric? = metrics.firstOrNull { it.type == type }
}
