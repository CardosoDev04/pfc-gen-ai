package html_fetcher

import classes.data.Element
import org.jsoup.Jsoup


class WebExtractor {
    fun getRelevantHTMLElements(html: String): List<Element> {
        val document = Jsoup.parse(html)

        // Retrieve all elements that are likely to be relevant from the received HTML
        val relevantElements = document.select(
            """
            a,
            button,
            input,
            select,
            textarea,
            label,
            h1,
            [onclick],
            [tabindex],
            [role=button],
            [role=link]
        """.trimIndent())

        // Retrieve elements that contain text. Likely containing important information that should be scraped
        val elementsWithText = document.allElements.filter { elem -> elem.text().isNotBlank() &&
                    elem.tagName() != "script" &&
                    elem.tagName() != "head" &&
                    elem.tagName() != "title" &&
                    elem.tagName() != "#root" &&
                    elem.tagName() != "html" &&
                    elem.tagName() != "body"
        }

        val combinedElements = (relevantElements + elementsWithText).distinctBy { it.cssSelector() }

        // Filter out elements whose ancestors are already in the list
        val filteredElements = combinedElements.filter { element ->
            combinedElements.none { other -> other != element && other.parents().contains(element) }
        }

        return filteredElements.map { element ->
            Element(
                type = element.tagName(),
                cssSelector = element.cssSelector(),
                text = element.ownText().trim(),
                id = element.id() ?: "",
                label = element.attr("label").trim()
            )
        }
    }
}
