package com.lexicon.interactors.sync

import kotlinx.coroutines.flow.Flow

/** One line of the startup report. */
sealed interface SyncStepStatus {
    data object Pending : SyncStepStatus

    data object InProgress : SyncStepStatus

    data class Complete(
        val total: Int,
        val added: Int,
        val updated: Int,
        val removed: Int,
    ) : SyncStepStatus {
        val wasAlreadyCurrent: Boolean get() = added == 0 && updated == 0 && removed == 0
    }

    /**
     * Carries whether the app can carry on regardless. A failed sync over a store that already
     * holds data is a stale catalogue, not a broken app, and blocking on it would strand the
     * user over something they can do nothing about.
     */
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
) {
    val isFinished: Boolean get() = vocabulary.isSettled && presets.isSettled

    /** Blocked only when something failed and left the app with nothing usable. */
    val isBlocked: Boolean
        get() = listOf(vocabulary, presets).any { it is SyncStepStatus.Failed && !it.canContinue }
}

/**
 * Imports the bundled catalogue into the store at startup, reporting each step as it goes.
 *
 * A flow rather than a suspend call because the point is the reporting: on a first launch this
 * writes thousands of rows, and a splash screen that says nothing for that long is
 * indistinguishable from one that has hung.
 */
interface SyncCatalogUseCase {
    operator fun invoke(): Flow<CatalogSyncStatus>
}
