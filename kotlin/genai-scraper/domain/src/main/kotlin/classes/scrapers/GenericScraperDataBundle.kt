package classes.scrapers

import interfaces.IScraper
import interfaces.IScraperData

data class GenericScraperDataBundle(override val path: String, override val compiledClass: IScraper): IScraperData
