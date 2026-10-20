package com.lexicon.application.rules

import com.lexicon.boundary.ReferenceRuleRepository
import com.lexicon.interactors.rules.GetReferenceRuleUseCase
import com.lexicon.model.rules.ReferenceRule
import com.lexicon.model.rules.ReferenceRuleId

class GetReferenceRuleUseCaseImpl(
    private val repository: ReferenceRuleRepository,
) : GetReferenceRuleUseCase {
    override suspend fun invoke(id: ReferenceRuleId): ReferenceRule? = repository.rules().firstOrNull { it.id == id }
}
