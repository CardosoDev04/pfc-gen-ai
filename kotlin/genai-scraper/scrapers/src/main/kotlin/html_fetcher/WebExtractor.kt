package html_fetcher

import classes.data.Element
import org.jsoup.Jsoup
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement


class WebExtractor {
    fun getCssSelector(driver: WebDriver, element: WebElement): String {
        val jsScript = """
        function getCssSelector(el) {
            if (!(el instanceof Element)) return;
            const path = [];
            while (el.nodeType === Node.ELEMENT_NODE) {
                let selector = el.nodeName.toLowerCase();
                if (el.id) {
                    selector += '#' + el.id;
                    path.unshift(selector);
                    break;
                } else {
                    let sib = el, nth = 1;
                    while (sib = sib.previousElementSibling) {
                        if (sib.nodeName.toLowerCase() == selector) nth++;
                    }
                    if (nth != 1) selector += ':nth-of-type(' + nth + ')';
                }
                path.unshift(selector);
                el = el.parentNode;
            }
            return path.join(' > ');
        }
        return getCssSelector(arguments[0]);
    """.trimIndent()

        return (driver as JavascriptExecutor).executeScript(jsScript, element) as String
    }

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
