package classes.service_model

import java.time.LocalDateTime

data class TestReport(
    val html: String,
    val generatedAt: LocalDateTime
)
