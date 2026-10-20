package com.lexicon.application.rules

import com.lexicon.boundary.ReferenceRuleRepository
import com.lexicon.model.rules.ReferenceRule
import com.lexicon.model.rules.ReferenceRuleId
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetReferenceRuleUseCaseImplTest {
    private val cases = ReferenceRule(ReferenceRuleId("przypadki"), "Przypadki", persistentListOf())
    private val aspect = ReferenceRule(ReferenceRuleId("aspekt"), "Aspekt", persistentListOf())

    private val repository = object : ReferenceRuleRepository {
        override suspend fun rules(): List<ReferenceRule> = listOf(cases, aspect)
    }

    @Test
    fun `a rule is found by its id`() =
        runTest {
            assertEquals(aspect, GetReferenceRuleUseCaseImpl(repository)(ReferenceRuleId("aspekt")))
            assertNull(GetReferenceRuleUseCaseImpl(repository)(ReferenceRuleId("tryb")))
        }
}
