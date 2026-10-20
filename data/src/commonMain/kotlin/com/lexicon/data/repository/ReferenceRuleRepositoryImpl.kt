package com.lexicon.data.repository

import com.lexicon.boundary.ReferenceRuleRepository
import com.lexicon.data.local.AssetReader
import com.lexicon.data.local.REFERENCE_RULES_ASSET_PATH
import com.lexicon.data.local.parseReferenceRules
import com.lexicon.model.rules.ReferenceRule

class ReferenceRuleRepositoryImpl(
    private val assets: AssetReader,
) : ReferenceRuleRepository {
    private val loaded: List<ReferenceRule> by lazy { parseReferenceRules(assets.readText(REFERENCE_RULES_ASSET_PATH)) }

    override suspend fun rules(): List<ReferenceRule> = loaded
}
