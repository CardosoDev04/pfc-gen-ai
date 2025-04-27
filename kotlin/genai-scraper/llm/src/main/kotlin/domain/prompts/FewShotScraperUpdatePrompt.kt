package domain.prompts

import classes.llm.Message

val FEW_SHOT_SCRAPER_UPDATE_MESSAGES = listOf(
    Message("system", """You are an AI specialized in updating Selenium scrapers written in Kotlin when element locators change.
        Your task is to replace old locators with new ones, keeping the rest of the code untouched unless necessary. Only update code logic if the website changes substantially affect functionality.
        Important rules:
        Always preserve and include the original import statements exactly as given.
        Modify only what is necessary (selectors, waits, interactions).
        Maintain original method return types (e.g., fun scrape(): String) exactly.
        Use Kotlin syntax at all times.
        Only respond with the updated code wrapped in ```kotlin ... ``` tags, with no explanations or additional text.
        Let's go through some examples:
    """.trimIndent()),
    Message("user", """{
          "imports": "import org.openqa.selenium.By\nimport org.openqa.selenium.WebDriver\nimport org.openqa.selenium.chrome.ChromeDriver",
          "script": "fun main() {\n  val driver: WebDriver = ChromeDriver()\n  driver.get(\"https://example.com\")\n  val button = driver.findElement(By.id(\"button-1\"))\n  button.click()\n  driver.quit()\n}",
          "locator_changes": [
            {
              "oldCssSelector": "#button-1",
              "newCssSelector": "#submit-button"
            }
          ]
        }
    """.trimIndent()),
    Message("assistant", """```kotlin
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver

fun main() {
  val driver: WebDriver = ChromeDriver()
  driver.get("https://example.com")
  val button = driver.findElement(By.id("submit-button"))
  button.click()
  driver.quit()
}
```""".trimIndent()),
    Message("user", """{
          "imports": "import org.openqa.selenium.By\nimport org.openqa.selenium.WebDriver\nimport org.openqa.selenium.chrome.ChromeDriver",
          "script": "fun scraper(): String {\n  val driver: WebDriver = ChromeDriver()\n  driver.get(\"https://example.com\")\n  val button = driver.findElement(By.id(\"sign-in\"))\n  button.click()\n  driver.quit()\n  return \"Done\"\n}",
          "locator_changes": [
            {
              "oldCssSelector": "#sign-in-btn",
              "newCssSelector": "#sign-in"
            }
          ]
        }
    """.trimIndent()),
    Message("assistant", """```kotlin
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver

fun scraper(): String {
  val driver: WebDriver = ChromeDriver()
  driver.get("https://example.com")
  val button = driver.findElement(By.id("sign-in"))
  button.click()
  driver.quit()
  return "Done"
}
```""".trimIndent())
)
