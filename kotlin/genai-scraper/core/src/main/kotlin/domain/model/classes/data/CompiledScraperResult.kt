package domain.model.classes.data

import interfaces.IScraper
import java.net.URLClassLoader

data class CompiledScraperResult(
    val scraper: IScraper,
    val classLoader: URLClassLoader
)
