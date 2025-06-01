package enums

import interfaces.IScraper

sealed class ScraperCorrectionResult {
    data object Failure : ScraperCorrectionResult()
    data class PartialFix(val stepsAchieved: Int) : ScraperCorrectionResult()
    data class Success(val correctedScraper: IScraper) : ScraperCorrectionResult()
}