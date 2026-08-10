package com.lexicon.interactors.sync

import kotlinx.coroutines.flow.Flow

sealed interface SyncStepStatus {
    data object Pending : SyncStepStatus

    data object InProgress : SyncStepStatus

    data class Complete(
        val total: Int,
        val added: Int,
        val updated: Int,
        val removed: Int,
    ) : SyncStepStatus

    data class Failed(
        val reason: String,
        val canContinue: Boolean,
    ) : SyncStepStatus
}

private val SyncStepStatus.isSettled: Boolean
    get() = this is SyncStepStatus.Complete || this is SyncStepStatus.Failed

data class CatalogSyncStatus(
    val vocabulary: SyncStepStatus = SyncStepStatus.Pending,
    val presets: SyncStepStatus = SyncStepStatus.Pending,
    val course: SyncStepStatus = SyncStepStatus.Pending,
)

val CatalogSyncStatus.steps: List<SyncStepStatus>
    get() = listOf(vocabulary, presets, course)

val SyncStepStatus.Complete.wasAlreadyCurrent: Boolean
    get() = added == 0 && updated == 0 && removed == 0

val CatalogSyncStatus.isFinished: Boolean
    get() = steps.all { it.isSettled }

val CatalogSyncStatus.isBlocked: Boolean
    get() = steps.any { it is SyncStepStatus.Failed && !it.canContinue }

interface SyncCatalogUseCase {
    operator fun invoke(): Flow<CatalogSyncStatus>
}
