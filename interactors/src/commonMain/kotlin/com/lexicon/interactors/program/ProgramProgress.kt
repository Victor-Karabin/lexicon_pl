package com.lexicon.interactors.program

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ProgramProgress(
    val programId: ProgramId,
    val metrics: ImmutableList<ProgressMetric> = persistentListOf(),
) {
    val overall: Double
        get() {
            val totalWeight = metrics.sumOf { it.weight }
            if (totalWeight == 0) return 0.0
            return metrics.sumOf { it.fraction * it.weight } / totalWeight
        }
}

data class ProgressMetric(
    val type: ProgressMetricType,
    val current: Int,
    val target: Int,
    val weight: Int,
    val isMeasured: Boolean = true,
) {
    val fraction: Double get() = if (target <= 0) 1.0 else (current.toDouble() / target).coerceIn(0.0, 1.0)
}

enum class ProgressMetricType { VOCABULARY, MILESTONES, CONSISTENCY, STUDY_TIME, ACCURACY }
