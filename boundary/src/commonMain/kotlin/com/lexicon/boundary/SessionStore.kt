package com.lexicon.boundary

import com.lexicon.model.training.Session
import com.lexicon.model.training.SessionId

interface SessionStore {
    suspend fun save(session: Session)

    suspend fun find(id: SessionId): Session?

    suspend fun remove(id: SessionId)
}
