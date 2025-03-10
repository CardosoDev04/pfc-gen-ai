import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import java.time.Duration


class WebInteractor{

    val driver : WebDriver = ChromeDriver()

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

        println("Links:")
        links.forEach { println(it.getDomAttribute("href")) }

        println("Input Fields:")
        inputs.forEach { println(it.getDomAttribute("name")) }

        print("TextAreas:")
        textareas.forEach { println(it.getDomAttribute("name")) }

        println("Selects:")
        selects.forEach { println(it.getDomAttribute("name")) }

        println("Forms:")
        forms.forEach { println(it.getDomAttribute("action")) }

        return "Page analysis complete"



    }
}

fun main() {
    val webInteractor = WebInteractor()
    val html = webInteractor.getPageHTML("http://www.google.com/")
    println(html)
}




