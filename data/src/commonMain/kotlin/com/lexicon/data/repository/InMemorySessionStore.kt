package com.lexicon.data.repository

import com.lexicon.boundary.SessionStore
import com.lexicon.model.training.Session
import com.lexicon.model.training.SessionId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A learner can only be answering one session at a time, and a session that is finished
 * or abandoned is never asked for again. Holding more than a handful would keep every
 * session's words alive for as long as the app runs, so the oldest is dropped once the
 * store is full — abandoned sessions age out the same way finished ones do.
 */
private const val REMEMBERED_SESSIONS = 8

/**
 * A session lives as long as the learner is answering it. Nothing has ever survived
 * process death here — the ViewModel's copy did not either — so this keeps the lifetime
 * the app already had rather than inventing persistence for it.
 */
class InMemorySessionStore : SessionStore {
    private val lock = Mutex()
    private val sessions = LinkedHashMap<SessionId, Session>()

    override suspend fun save(session: Session) =
        lock.withLock {
            sessions.remove(session.id)
            sessions[session.id] = session
            while (sessions.size > REMEMBERED_SESSIONS) {
                sessions.remove(sessions.keys.first())
            }
        }

    override suspend fun find(id: SessionId): Session? = lock.withLock { sessions[id] }

    override suspend fun remove(id: SessionId) {
        lock.withLock { sessions.remove(id) }
    }
}
