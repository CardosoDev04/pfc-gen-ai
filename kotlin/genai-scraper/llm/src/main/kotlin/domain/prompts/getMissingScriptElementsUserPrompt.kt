package domain.prompts

import classes.llm.Message

val GET_MISSING_ELEMENTS_MESSAGES = listOf(
    Message(role = "user", content = """
        You are a helpful assistant that helps users identify missing elements by comparing two HTML documents.
        The HTML pages will be provided in the following format:
        ```
        <first-html>
            // Script content here
        </first-html>
        
        <second-html>
            // Script content here
        </second-html>
        ```

        Your output, which will contain the missing elements will be formatted as a JSON array of objects, each representing an element with its properties in the following format:
        ```json
        [
            {
                "type": "string",
                "cssSelector": "string",
                "text": "string" (optional)
                "id": "string" (optional),
                "label": "string" (optional)
            },
            ...
        ]
        ```
        
        If you cannot infer a property, you should not include it in the output.

        Please ensure that your response is clear and concise, listing only the missing elements.
    """.trimIndent()),

    Message(role = "assistant", content = """
        Okay, I understand that you want me to identify the missing elements in the new HTML page based on the previous one.
    """.trimIndent()),

    Message(role = "user", content = """
        <first-html>
            driver.findElement(By.cssSelector("button.login")).click()
            driver.findElement(By.id("username")).sendKeys("user")
            driver.findElement(By.id("password")).sendKeys("pass")
        </first-html>

        ```json
        [
            {
                "type": "input",
                "cssSelector": "input#username",
                "id": "username"
            },
            {
                "type": "input",
                "cssSelector": "input#password",
                "id": "password"
            },
            {
                "type": "button",
                "cssSelector": "button.submit",
                "text": "Submit"
            }
        ]
        ```
    """.trimIndent()),

    Message(role = "assistant", content = """
        [
            {
                "type": "button",
                "cssSelector": "button.submit",
                "text": "Submit"
            }
        ]
    """.trimIndent()),

    Message(role = "user", content = """
        <script>
            driver.findElement(By.cssSelector("input#search")).sendKeys("Kotlin")
            driver.findElement(By.cssSelector("button.search-btn")).click()
        </script>

        ```json
        [
            {
                "type": "input",
                "cssSelector": "input#search",
                "label": "Search"
            },
            {
                "type": "button",
                "cssSelector": "button.search-btn",
                "text": "Search"
            }
        ]
        ```
    """.trimIndent()),

    Message(role = "assistant", content = "[]"),

    Message(role = "user", content = """
        <script>

        </script>

        ```json
        [
            {
                "type": "input",
                "cssSelector": "input#email",
                "id": "email"
            },
            {
                "type": "input",
                "cssSelector": "input#password",
                "id": "password"
            },
            {
                "type": "button",
                "cssSelector": "button.login",
                "text": "Log In"
            }
        ]
        ```
    """.trimIndent()),

    Message(role = "assistant", content = """
        [
            {
                "type": "input",
                "cssSelector": "input#email",
                "id": "email"
            },
            {
                "type": "input",
                "cssSelector": "input#password",
                "id": "password"
            },
            {
                "type": "button",
                "cssSelector": "button.login",
                "text": "Log In"
            }
        ]
    """.trimIndent()),

    Message(role = "user", content = """
        <script>
            driver.findElement(By.name("q")).sendKeys("Selenium")
            driver.findElement(By.cssSelector("button[type='submit']")).click()
        </script>

        ```json
        [
            {
                "type": "input",
                "cssSelector": "input[name='q']",
                "label": "Search"
            },
            {
                "type": "button",
                "cssSelector": "button.search-button",
                "text": "Go"
            }
        ]
        ```
    """.trimIndent()),

    Message(role = "assistant", content = """
        [
            {
                "type": "button",
                "cssSelector": "button.search-button",
                "text": "Go"
            }
        ]
    """.trimIndent()),

    Message(
        role = "user", content = """
            <script>
            class ExampleScraper: IScraper {
                override suspend fun scrape(): List<BookingOption> {
                    driver.findElement(By.id("search")).click()
                    driver.findElement(By.id("action-title")).text
                }
            }
            </script>
            ```json
            []
            ```
            """.trimIndent()
    ),
    Message(
        role = "assistant", content = """
            [
                {
                    "cssSelector": "#search",
                    "id": "search"
                },
                {
                    "cssSelector": "#action-title",
                    "text": ""
                }
            ]
        """.trimIndent()
    )
)
