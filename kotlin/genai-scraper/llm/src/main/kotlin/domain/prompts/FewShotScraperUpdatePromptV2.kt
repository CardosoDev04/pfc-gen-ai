package domain.prompts

import classes.llm.Message

val FEW_SHOT_SCRAPER_REPLACEMENT_MESSAGES_V2 = listOf(
    Message("system", """You are an AI specialized in maintaining Selenium scrapers written in Kotlin.
Your task is to update the scraper code when it appears that existing elements have been replaced in the latest version of the website.

Inputs:
- The original code and its import statements.
- A list of new elements found in the updated webpage that were not present in the original script.

Instructions:
- If any of the new elements clearly **replace** elements currently used in the original script (e.g., based on ID/class similarities or structural position), update the original code to use the new element instead.
- Leave all other parts of the code untouched unless absolutely necessary.
- Preserve the original import block exactly.
- Use Kotlin syntax at all times.
- Respond only with the updated Kotlin code, wrapped in ```kotlin ... ``` tags, with no extra commentary or text.

Other rules:
 - When using Selenium's By.id() do not use # before the ID, as it is not valid in Kotlin.
- When using Selenium's By.cssSelector() use the full CSS selector as provided, without any modifications.
Let’s look at some examples:
""".trimIndent()),

    Message("user", """{
  "imports": "import org.openqa.selenium.By\nimport org.openqa.selenium.WebDriver\nimport org.openqa.selenium.chrome.ChromeDriver",
  "script": "fun main() {\n  val driver: WebDriver = ChromeDriver()\n  driver.get(\"https://example.com\")\n  val button = driver.findElement(By.id(\"old-button\"))\n  button.click()\n  driver.quit()\n}",
  "new_elements": [
    {
      "type": "button",
      "cssSelector": "#submit-button",
      "text": "Submit",
      "id": "submit-button",
      "label": ""
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
  "script": "fun login(): String {\n  val driver: WebDriver = ChromeDriver()\n  driver.get(\"https://example.com/login\")\n  val usernameInput = driver.findElement(By.id(\"user-input\"))\n  usernameInput.sendKeys(\"admin\")\n  val loginButton = driver.findElement(By.id(\"login-btn\"))\n  loginButton.click()\n  driver.quit()\n  return \"Done\"\n}",
  "new_elements": [
    {
      "type": "input",
      "cssSelector": "input[name='username']",
      "text": "",
      "id": "",
      "label": "Username"
    },
    {
      "type": "button",
      "cssSelector": ".btn.btn-primary",
      "text": "Log In",
      "id": "login-button",
      "label": ""
    }
  ]
}
""".trimIndent()),

    Message("assistant", """```kotlin
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver

fun login(): String {
  val driver: WebDriver = ChromeDriver()
  driver.get("https://example.com/login")
  val usernameInput = driver.findElement(By.name("username"))
  usernameInput.sendKeys("admin")
  val loginButton = driver.findElement(By.id("login-button"))
  loginButton.click()
  driver.quit()
  return "Done"
}
```""".trimIndent())
)
