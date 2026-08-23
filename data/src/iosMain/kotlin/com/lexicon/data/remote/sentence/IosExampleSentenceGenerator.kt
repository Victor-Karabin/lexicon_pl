package com.lexicon.data.remote.sentence

import com.lexicon.boundary.ExampleRequestBoundary
import com.lexicon.boundary.ExampleSentenceGenerator
import com.lexicon.boundary.SentenceResultBoundary

class IosExampleSentenceGenerator : ExampleSentenceGenerator {
    override suspend fun generate(request: ExampleRequestBoundary): SentenceResultBoundary = SentenceResultBoundary.Offline
}
