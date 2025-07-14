package enums

sealed class ScraperCorrectionResult {
    data object Failure : ScraperCorrectionResult()
    data class PartialFix(val stepsAchieved: Int, val exceptionMessage: String) : ScraperCorrectionResult()
    data class Success(val result: Any) : ScraperCorrectionResult()
}