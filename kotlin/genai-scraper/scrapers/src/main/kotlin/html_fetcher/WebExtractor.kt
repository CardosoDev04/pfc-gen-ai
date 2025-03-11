package html_fetcher

import com.cardoso.common.buildChromeDriver
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.time.Duration


class WebExtractor{

    val driver : WebDriver = buildChromeDriver(true)

    fun getPageHTML(url: String): String {
        driver.get(url)
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5))
        return driver.pageSource ?: ""
    }

    fun getInteractiveElementsFromHTML(url: String): String{
        // get whole pages html
        driver.get(url)
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5))
        // parse it and get all interactive elements

        val buttons: List<WebElement> = driver.findElements(By.tagName("button"))
        val inputButtons: List<WebElement> = driver.findElements(By.xpath("//input[@type='submit' or @type='button']"))
        val links: List<WebElement> = driver.findElements(By.tagName("a"))
        val inputs: List<WebElement> = driver.findElements(By.tagName("input"))
        val textareas: List<WebElement> = driver.findElements(By.tagName("textarea"))
        val selects: List<WebElement> = driver.findElements(By.tagName("select"))
        val forms: List<WebElement> = driver.findElements(By.tagName("form"))

        //print elements
        println("Buttons:")
        buttons.forEach { println(it.text) }
        inputButtons.forEach { println(it.getDomAttribute("value")) }

        println("\nLinks:")
        links.forEach {
            var linkButton : String? = ""
            if(it.accessibleName == "") {
                linkButton = "Unknown name, "
            } else  linkButton = it.accessibleName
            val href = it.getAttribute("href")

            println("Link name: $linkButton, Href: $href " )
        }

        println("\nInput Fields:")
        inputs.forEach { println(it.getDomAttribute("name")) }

        println("\nTextAreas:")
        textareas.forEach { println(it.getDomAttribute("name")) }

        println("\nSelects:")
        selects.forEach { println(it.getDomAttribute("name")) }

        println("\nForms:")
        forms.forEach { println(it.getDomAttribute("action")) }

        return "\nPage analysis complete"



    }
}

fun main() {
    val webExtractor = WebExtractor()
    webExtractor.getInteractiveElementsFromHTML("https://parabank.parasoft.com/parabank/index.html")

}




