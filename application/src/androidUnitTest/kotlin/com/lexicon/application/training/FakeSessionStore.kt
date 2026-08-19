package com.lexicon.application.training

import com.lexicon.boundary.SessionStore
import com.lexicon.model.training.Session
import com.lexicon.model.training.SessionId

class FakeSessionStore : SessionStore {
    private val sessions = mutableMapOf<SessionId, Session>()

    override suspend fun save(session: Session) {
        sessions[session.id] = session
    }

    override suspend fun find(id: SessionId): Session? = sessions[id]

    override suspend fun remove(id: SessionId) {
        sessions.remove(id)
    }
}
