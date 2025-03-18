package html_fetcher

import classes.data.Element
import com.cardoso.common.buildChromeDriver
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.time.Duration


class WebExtractor {

    private val driver: WebDriver = buildChromeDriver(true)

    fun getPageHTML(url: String): String {
        driver.get(url)
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5))
        return driver.pageSource ?: ""
    }

    fun WebElement.getCssSelector(driver: WebDriver, element: WebElement): String {
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

    fun getInteractiveElementsHTML(url: String): List<Element> {
        driver.get(url)
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5))

        val elements = mutableListOf<Element>()

        val buttons: List<WebElement> = driver.findElements(By.tagName("button"))
        val inputButtons: List<WebElement> = driver.findElements(By.xpath("//input[@type='submit' or @type='button']"))
        val links: List<WebElement> = driver.findElements(By.tagName("a"))
        val inputs: List<WebElement> = driver.findElements(By.tagName("input"))
        val textAreas: List<WebElement> = driver.findElements(By.tagName("textarea"))
        val selects: List<WebElement> = driver.findElements(By.tagName("select"))
        val forms: List<WebElement> = driver.findElements(By.tagName("form"))

        buttons.forEach {
            elements.add(
                Element(
                    "button",
                    it.getCssSelector(driver, it),
                    it.text
                )
            )
        }
        inputButtons.forEach {
            elements.add(
                Element(
                    "inputButton",
                    it.getCssSelector(driver, it),
                    it.getDomAttribute("value") ?: ""
                )
            )
        }
        links.forEach {
            elements.add(
                Element(
                    "link", it.getCssSelector(driver, it),
                    (it.accessibleName ?: { "Unknown name" }).toString()
                )
            )
        }
        inputs.forEach {
            elements.add(
                Element(
                    "input",
                    it.getCssSelector(driver, it),
                    it.getDomAttribute("name") ?: ""
                )
            )
        }
        textAreas.forEach {
            elements.add(
                Element(
                    "textarea",
                    it.getCssSelector(driver, it),
                    it.getDomAttribute("name") ?: ""
                )
            )
        }
        selects.forEach {
            elements.add(
                Element(
                    "select",
                    it.getCssSelector(driver, it),
                    it.getDomAttribute("name") ?: ""
                )
            )
        }
        forms.forEach {
            elements.add(
                Element(
                    "form",
                    it.getCssSelector(driver, it),
                    it.getDomAttribute("action") ?: ""
                )
            )
        }

        return elements
    }
}

fun main() {
    val webExtractor = WebExtractor()
    val elements = webExtractor.getInteractiveElementsHTML("https://www.google.pt")

    elements.forEach { element ->
        println("Type: ${element.type}, CSS Selector: ${element.cssSelector}, Text: ${element.text}")
    }

}




