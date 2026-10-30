package com.lexicon.application

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.TestLogWriter
import co.touchlab.kermit.platformLogWriter
import org.junit.rules.ExternalResource

class LoggedWarnings : ExternalResource() {
    private val writer = TestLogWriter(Severity.Warn)

    val entries: List<TestLogWriter.LogEntry> get() = writer.logs

    override fun before() = Logger.setLogWriters(writer)

    override fun after() = Logger.setLogWriters(platformLogWriter())
}
