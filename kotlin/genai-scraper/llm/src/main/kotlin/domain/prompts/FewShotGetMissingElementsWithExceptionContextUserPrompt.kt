package domain.prompts

import classes.llm.Message

val FEW_SHOT_GET_MISSING_ELEMENTS_WITH_REASONING_AND_EXCEPTION = listOf(
    Message(
        role = "user",
        content = """
            The scraper encountered an error due to a missing HTML element.

            Exception:
            org.openqa.selenium.NoSuchElementException: Unable to locate element using selector "#login-btn"

            The scraper previously relied on the following element, which no longer exists in the page:
            {
              "type": "button",
              "cssSelector": "#login-btn",
              "text": "Login",
              "id": "login-btn",
              "label": "Login"
            }

            Below is a list of `Element` candidates extracted from the new HTML snapshot:

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
            Reasoning: The original element was a button with ID "login-btn" and the text strongly suggests a login action. Among the candidates, the anchor tag with the text "Login" most closely matches in purpose and label.
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

            The scraper previously relied on the following element, which no longer exists in the page:
            {
              "type": "input",
              "cssSelector": "input[name='email']",
              "text": "",
              "id": "",
              "label": "Email"
            }

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
            Reasoning: The original selector targeted an input named 'email'. The first candidate has ID "email-field" and a label of "Email Address", which directly suggests it replaced the original.
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
            org.openqa.selenium.NoSuchElementException: Unable to locate element using selector "button#apply-discount"

            The scraper previously relied on the following element, which no longer exists in the page:
            {
              "type": "button",
              "cssSelector": "button#apply-discount",
              "text": "Apply Discount",
              "id": "apply-discount",
              "label": "Apply Discount"
            }

            Below is a list of `Element` candidates extracted from the new HTML snapshot:

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
