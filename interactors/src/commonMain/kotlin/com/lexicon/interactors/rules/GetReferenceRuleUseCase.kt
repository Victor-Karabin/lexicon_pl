package com.lexicon.interactors.rules

import com.lexicon.model.rules.ReferenceRule
import com.lexicon.model.rules.ReferenceRuleId

fun interface GetReferenceRuleUseCase {
    suspend operator fun invoke(id: ReferenceRuleId): ReferenceRule?
}
