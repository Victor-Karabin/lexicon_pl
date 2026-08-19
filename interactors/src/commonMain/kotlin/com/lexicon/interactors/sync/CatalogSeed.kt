package com.lexicon.interactors.sync

import kotlinx.coroutines.flow.Flow

sealed interface SeedStepStatus {
    data object Pending : SeedStepStatus

    data object InProgress : SeedStepStatus

    data class Complete(
        val total: Int,
        val added: Int,
        val updated: Int,
        val removed: Int,
    ) : SeedStepStatus

    data class Failed(
        val reason: String,
        val canContinue: Boolean,
    ) : SeedStepStatus
}

private val SeedStepStatus.isSettled: Boolean
    get() = this is SeedStepStatus.Complete || this is SeedStepStatus.Failed

data class CatalogSeedStatus(
    val vocabulary: SeedStepStatus = SeedStepStatus.Pending,
    val presets: SeedStepStatus = SeedStepStatus.Pending,
    val course: SeedStepStatus = SeedStepStatus.Pending,
    val verbs: SeedStepStatus = SeedStepStatus.Pending,
)

val CatalogSeedStatus.steps: List<SeedStepStatus>
    get() = listOf(vocabulary, presets, course, verbs)

val SeedStepStatus.Complete.wasAlreadyCurrent: Boolean
    get() = added == 0 && updated == 0 && removed == 0

val CatalogSeedStatus.isFinished: Boolean
    get() = steps.all { it.isSettled }

val CatalogSeedStatus.isBlocked: Boolean
    get() = steps.any { it is SeedStepStatus.Failed && !it.canContinue }

interface SeedCatalogsUseCase {
    operator fun invoke(): Flow<CatalogSeedStatus>
}
