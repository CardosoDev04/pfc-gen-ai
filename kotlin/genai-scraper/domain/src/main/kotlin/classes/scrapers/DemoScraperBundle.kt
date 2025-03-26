package classes.scrapers

import interfaces.IDemoScraper
import interfaces.IScraper

class DemoScraperBundle(override val code: String, override val compiledClass: IDemoScraper): IScraper