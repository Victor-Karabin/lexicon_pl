package com.lexicon.data.repository

import com.lexicon.boundary.SessionStore
import com.lexicon.model.training.Session
import com.lexicon.model.training.SessionId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A session lives as long as the learner is answering it. Nothing has ever survived
 * process death here — the ViewModel's copy did not either — so this keeps the
 * lifetime the app already had rather than inventing persistence for it.
 */
class InMemorySessionStore : SessionStore {
    private val lock = Mutex()
    private val sessions = mutableMapOf<SessionId, Session>()

    override suspend fun save(session: Session) =
        lock.withLock {
            sessions[session.id] = session
            Unit
        }

    override suspend fun find(id: SessionId): Session? = lock.withLock { sessions[id] }

    override suspend fun remove(id: SessionId) =
        lock.withLock {
            sessions.remove(id)
            Unit
        }
}
