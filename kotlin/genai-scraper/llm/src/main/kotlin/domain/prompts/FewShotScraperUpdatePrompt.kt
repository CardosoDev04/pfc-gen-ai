package domain.prompts

import classes.llm.Message

val FEW_SHOT_SCRAPER_UPDATE_MESSAGES = listOf(
    Message("system", """You are an AI specialized in updating Selenium scrapers written in Kotlin when element locators change.
        Your task is to replace old locators with new ones, keeping the rest of the code untouched unless necessary. Only update code logic if the website changes substantially affect functionality.
        Important rules:
        Always preserve and include the original import statements exactly as given.
        Modify only what is necessary (selectors, waits, interactions).
        Maintain original method return types (e.g., fun scrape(): String) exactly.
        Respond only with a JSON object: { "updatedCode": "..." }.
        Use Kotlin syntax at all times.
        No introductions, explanations, comments, or formatting like Markdown. Follow the example's response format.
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
    Message("assistant", """{
          "updatedCode": "import org.openqa.selenium.By\nimport org.openqa.selenium.WebDriver\nimport org.openqa.selenium.chrome.ChromeDriver\n\nfun main() {\n  val driver: WebDriver = ChromeDriver()\n  driver.get(\"https://example.com\")\n  val button = driver.findElement(By.id(\"submit-button\"))\n  button.click()\n  driver.quit()\n}"
        }
    """.trimIndent()),
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
    Message("assistant", """{
          "updatedCode": "import org.openqa.selenium.By\nimport org.openqa.selenium.WebDriver\nimport org.openqa.selenium.chrome.ChromeDriver\n\nfun scraper(): String {\n  val driver: WebDriver = ChromeDriver()\n  driver.get(\"https://example.com\")\n  val button = driver.findElement(By.id(\"sign-in\"))\n  button.click()\n  driver.quit()\n  return \"Done\"\n}"
        }
    """.trimIndent())
)