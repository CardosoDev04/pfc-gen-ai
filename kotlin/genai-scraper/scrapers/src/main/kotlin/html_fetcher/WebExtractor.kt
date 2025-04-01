package html_fetcher

import classes.data.Element
import com.cardoso.common.buildChromeDriver
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.time.Duration


class WebExtractor {

    private val driver: WebDriver = buildChromeDriver(true)

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

    fun getInteractiveElementsHTML(html: String): List<Element> {
        val document = Jsoup.parse(html)
        val selectors = listOf("a", "button", "input", "select", "textarea", "label", "[onclick]", "[tabindex]", "[role=button]", "[role=link]")

        return document.select(selectors.joinToString(", ")).map { element ->
            Element(
                type = element.tagName(),
                cssSelector = element.cssSelector(),
                text = element.ownText().trim()
            )
        }
    }
}

fun main() {
    val webExtractor = WebExtractor()
    val elements = webExtractor.getInteractiveElementsHTML("")

    elements.forEach { element ->
        println("Type: ${element.type}, CSS Selector: ${element.cssSelector}, Text: ${element.text}")
    }

}



