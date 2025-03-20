import kotlinx.coroutines.runBlocking
import modification_detection.ModificationDetectionService
import okhttp3.OkHttpClient
import ollama.OllamaClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModificationDetectionServiceTests {
    private val mds = ModificationDetectionService(ollamaClient)

    @Test
    fun `getMissingElements of html form with missing submit button`(): Unit = runBlocking {
            // Given: Two HTML document and the second one not having a submit button
            val previousHtml = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Contact Form</title>
            </head>
            <body>
                <h2>Contact Us</h2>
                <form action="/submit-form" method="post">
                    <div class="form-group">
                        <label for="name">Name:</label>
                        <input type="text" id="name" name="name" required>
                    </div>
                    <div class="form-group">
                        <label for="email">Email:</label>
                        <input type="email" id="email" name="email" required>
                    </div>
                    <div class="form-group">
                        <label for="message">Message:</label>
                        <textarea id="message" name="message" rows="4" required></textarea>
                    </div>
                    <button type="submit" id="submit-button">Submit</button>
                </form>
            </body>
            </html>
        """.trimIndent()

        val newHtml = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Contact Form</title>
            </head>
            <body>
                <h2>Contact Us</h2>
                <form action="/submit-form" method="post">
                    <div class="form-group">
                        <label for="name">Name:</label>
                        <input type="text" id="name" name="name" required>
                    </div>
                    <div class="form-group">
                        <label for="email">Email:</label>
                        <input type="email" id="email" name="email" required>
                    </div>
                    <div class="form-group">
                        <label for="message">Message:</label>
                        <textarea id="message" name="message" rows="4" required></textarea>
                    </div>
                </form>
            </body>
            </html>
        """.trimIndent()

            // When: calling getMissingElements
            val missing = mds.getMissingElements(previousHtml, newHtml)

            // Then: The missing element is correctly returned
            assertEquals(1, missing.size)
            assertEquals("BUTTON", missing[0].type)
            assertEquals("#submit-button", missing[0].cssSelector)
            assertEquals("Submit", missing[0].text)
        }

    @Test
    fun `getMissingElements of html with missing input element`(): Unit = runBlocking {
        // Given: Two HTML document and the second one not having a submit button
        val previousHtml = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Contact Form</title>
            </head>
            <body>
                <h2>Contact Us</h2>
                <form action="/submit-form" method="post">
                    <div class="form-group">
                        <label for="name">Name:</label>
                        <input type="text" id="name" name="name" required>
                    </div>
                    <div class="form-group">
                        <label for="email">Email:</label>
                        <input type="email" id="email" name="email" required>
                    </div>
                    <div class="form-group">
                        <label for="message">Message:</label>
                        <textarea id="message" name="message" rows="4" required></textarea>
                    </div>
                    <button type="submit" id="submit-button">Submit</button>
                </form>
            </body>
            </html>
        """.trimIndent()

        val newHtml = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Contact Form</title>
            </head>
            <body>
                <h2>Contact Us</h2>
                <form action="/submit-form" method="post">
                    <div class="form-group">
                        <label for="name">Name:</label>
                        <input type="text" id="name" name="name" required>
                    </div>
                    <div class="form-group">
                        <label for="email">Email:</label>
                    </div>
                    <div class="form-group">
                        <label for="message">Message:</label>
                        <textarea id="message" name="message" rows="4" required></textarea>
                    </div>
                    <button type="submit" id="submit-button">Submit</button>
                </form>
            </body>
            </html>
        """.trimIndent()

        // When: Calling getMissingElements
        val missing = mds.getMissingElements(previousHtml, newHtml)

        // Then: The missing elements are correctly returned
        assertEquals(1, missing.size)
        assertEquals("INPUT", missing[0].type)
        assertEquals("#email", missing[0].cssSelector)
        assertEquals("", missing[0].text)
    }

    @Test
    fun `getMissingElements of html with missing textarea element`(): Unit = runBlocking {
        // Given: Two HTML document and the second one not having a message textarea
        val previousHtml = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Contact Form</title>
            </head>
            <body>
                <h2>Contact Us</h2>
                <form action="/submit-form" method="post">
                    <div class="form-group">
                        <label for="name">Name:</label>
                        <input type="text" id="name" name="name" required>
                    </div>
                    <div class="form-group">
                        <label for="email">Email:</label>
                        <input type="email" id="email" name="email" required>
                    </div>
                    <div class="form-group">
                        <label for="message">Message:</label>
                        <textarea id="message" name="message" rows="4" required></textarea>
                    </div>
                    <button type="submit" id="submit-button">Submit</button>
                </form>
            </body>
            </html>
        """.trimIndent()

        val newHtml = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Contact Form</title>
            </head>
            <body>
                <h2>Contact Us</h2>
                <form action="/submit-form" method="post">
                    <div class="form-group">
                        <label for="name">Name:</label>
                        <input type="text" id="name" name="name" required>
                    </div>
                    <div class="form-group">
                        <label for="email">Email:</label>
                        <input type="email" id="email" name="email" required>
                    </div>
                    <div class="form-group">
                        <label for="message">Message:</label>
                    </div>
                    <button type="submit" id="submit-button">Submit</button>
                </form>
            </body>
            </html>
        """.trimIndent()

        // When: Calling getMissingElements
        val missing = mds.getMissingElements(previousHtml, newHtml)

        // Then: The missing element is correctly returned
        assertEquals(1, missing.size)
        assertEquals("TEXTAREA", missing[0].type)
        assertEquals("#message", missing[0].cssSelector)
        assertEquals("", missing[0].text)
    }

    @Test
    fun `getMissingElements of html with 2 missing buttons`(): Unit = runBlocking {
        // Given: Two HTML document and the second one missing a signup and a search button
        val previousHtml = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Travel Search</title>
            </head>
            <body>
                <div>
                    <button type="button" id="login-button">Login</button>
                    <button type="button" id="signup-button">Sign Up</button>
                </div>
                
                <h2>Find Your Trip</h2>
                <form action="/search" method="get">
                    <div>
                        <label for="departure">Departure:</label>
                        <input type="text" id="departure" name="departure" placeholder="Enter departure city" required>
                    </div>
                    <div>
                        <label for="destination">Destination:</label>
                        <input type="text" id="destination" name="destination" placeholder="Enter destination city" required>
                    </div>
                    <div>
                        <button type="submit" id="search-button">Search</button>
                    </div>
                </form>
            </body>
            </html>
        """.trimIndent()

        val newHtml = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Travel Search</title>
            </head>
            <body>
                <div>
                    <button type="button" id="login-button">Login</button>
                </div>
                
                <h2>Find Your Trip</h2>
                <form action="/search" method="get">
                    <div>
                        <label for="departure">Departure:</label>
                        <input type="text" id="departure" name="departure" placeholder="Enter departure city" required>
                    </div>
                    <div>
                        <label for="destination">Destination:</label>
                        <input type="text" id="destination" name="destination" placeholder="Enter destination city" required>
                    </div>
                </form>
            </body>
            </html>
        """.trimIndent()

        // When: Calling getMissingElements
        val missing = mds.getMissingElements(previousHtml, newHtml)

        missing.forEach { println(it) }

        // Then: The missing elements are correctly returned
        assertEquals(3, missing.size)
        assertEquals(2, missing.filter { elem -> elem.type == "BUTTON" }.size)
        assertTrue(missing.any { elem -> elem.type == "DIV" })
        assertTrue(missing.any { elem -> elem.cssSelector == "signup-button" })
        assertTrue(missing.any { elem -> elem.cssSelector == "search-button" })
        assertTrue(missing.any { elem -> elem.text == "Sign Up" })
        assertTrue(missing.any { elem -> elem.text == "Search" })
    }

    companion object {
        private val httpClient = OkHttpClient.Builder()
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        private val ollamaClient = OllamaClient(httpClient)
    }
}