package com.lexicon.data.repository

import com.lexicon.boundary.SessionStore
import com.lexicon.model.training.Session
import com.lexicon.model.training.SessionId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val REMEMBERED_SESSIONS = 8

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
