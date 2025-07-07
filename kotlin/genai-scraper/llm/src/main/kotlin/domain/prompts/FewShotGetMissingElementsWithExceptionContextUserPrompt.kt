package domain.prompts

import classes.llm.Message

val FEW_SHOT_GET_MISSING_ELEMENTS_WITH_EXCEPTION = listOf(
    Message(
        role = "user",
        content = """
            The scraper encountered an error due to a missing HTML element.

            Exception:
            org.openqa.selenium.NoSuchElementException: Unable to locate element using selector "#login-btn"

            The scraper previously relied on a specific HTML element that no longer exists in the page. Below is a list of `Element` candidates extracted from the new HTML snapshot:

            [
              {
                "type": "button",
                "cssSelector": "#sign-in",
                "text": "Sign In",
                "id": "sign-in",
                "label": "Sign In"
              },
              {
                "type": "a",
                "cssSelector": ".nav-login a",
                "text": "Login",
                "id": "",
                "label": ""
              },
              {
                "type": "button",
                "cssSelector": "#submit",
                "text": "Submit",
                "id": "submit",
                "label": "Submit"
              }
            ]

            Instructions:

            You must analyze the provided elements and select the one that best matches the missing element the scraper used before the error.

            Return only one element inside:

            <BEGIN_ALTERNATIVE>
            <your_json_selection_here>
            </END_ALTERNATIVE>
        """.trimIndent()
    ),
    Message(
        role = "assistant",
        content = """
            <BEGIN_ALTERNATIVE>
            {
              "type": "a",
              "cssSelector": ".nav-login a",
              "text": "Login",
              "id": "",
              "label": ""
            }
            </END_ALTERNATIVE>
        """.trimIndent()
    ),
    Message(
        role = "user",
        content = """
            The scraper encountered an error due to a missing HTML element.

            Exception:
            org.openqa.selenium.NoSuchElementException: Element not found with selector "input[name='email']"

            Elements:
            [
              {
                "type": "input",
                "cssSelector": "input#email-field",
                "text": "",
                "id": "email-field",
                "label": "Email Address"
              },
              {
                "type": "input",
                "cssSelector": "input[name='username']",
                "text": "",
                "id": "",
                "label": "Username"
              },
              {
                "type": "input",
                "cssSelector": "input[type='text']",
                "text": "",
                "id": "",
                "label": "Full Name"
              }
            ]

            Instructions:

            You must analyze the provided elements and select the one that best matches the missing element the scraper used before the error.

            Return only one element inside:

            <BEGIN_ALTERNATIVE>
            <your_json_selection_here>
            </END_ALTERNATIVE>
        """.trimIndent()
    ),
    Message(
        role = "assistant",
        content = """
            <BEGIN_ALTERNATIVE>
            {
              "type": "input",
              "cssSelector": "input#email-field",
              "text": "",
              "id": "email-field",
              "label": "Email Address"
            }
            </END_ALTERNATIVE>
        """.trimIndent()
    ),
    Message(
        role = "user",
        content = """
            The scraper encountered an error due to a missing HTML element.

            Exception:
            org.openqa.selenium.NoSuchElementException: No element found matching "#checkout-button"

            Elements:
            [
              {
                "type": "button",
                "cssSelector": ".btn-primary.checkout",
                "text": "Proceed to Checkout",
                "id": "",
                "label": ""
              },
              {
                "type": "button",
                "cssSelector": "#continue-payment",
                "text": "Continue",
                "id": "continue-payment",
                "label": "Payment"
              },
              {
                "type": "a",
                "cssSelector": "a[href='/checkout']",
                "text": "Go to Checkout",
                "id": "",
                "label": ""
              }
            ]

            Instructions:

            You must analyze the provided elements and select the one that best matches the missing element the scraper used before the error.

            Return only one element inside:

            <BEGIN_ALTERNATIVE>
            <your_json_selection_here>
            </END_ALTERNATIVE>
        """.trimIndent()
    ),
    Message(
        role = "assistant",
        content = """
            <BEGIN_ALTERNATIVE>
            {
              "type": "button",
              "cssSelector": ".btn-primary.checkout",
              "text": "Proceed to Checkout",
              "id": "",
              "label": ""
            }
            </END_ALTERNATIVE>
        """.trimIndent()
    ),
    Message(
        role = "user",
        content = """
            The scraper encountered an error due to a missing HTML element.

            Exception:
            org.openqa.selenium.NoSuchElementException: Unable to locate element using selector "button#apply-discount"

            The scraper previously relied on a specific HTML element that no longer exists in the page. Below is a list of `Element` candidates extracted from the new HTML snapshot:

            [
              {
                "type": "button",
                "cssSelector": ".coupon-apply",
                "text": "Apply Coupon",
                "id": "",
                "label": ""
              },
              {
                "type": "button",
                "cssSelector": "#redeem-btn",
                "text": "Redeem",
                "id": "redeem-btn",
                "label": "Redeem Discount"
              },
              {
                "type": "a",
                "cssSelector": ".apply-link",
                "text": "Apply",
                "id": "",
                "label": "Discount Code"
              }
            ]

            Instructions:

            You must analyze the provided elements and select the one that best matches the missing element the scraper used before the error.

            Return only one element inside:

            <BEGIN_ALTERNATIVE>
            <your_json_selection_here>
            </END_ALTERNATIVE>
        """.trimIndent()
    )
)
