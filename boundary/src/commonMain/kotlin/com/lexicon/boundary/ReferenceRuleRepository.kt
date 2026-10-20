package com.lexicon.boundary

import com.lexicon.model.rules.ReferenceRule

interface ReferenceRuleRepository {
    suspend fun rules(): List<ReferenceRule>
}
