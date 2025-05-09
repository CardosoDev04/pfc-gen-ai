package domain.prompts

import classes.llm.Message

val FEW_SHOT_GET_MODIFICATION_PROMPT = listOf(
    Message(
        "system", """
        You are an automation assistant tasked with selecting the best alternative HTML element from a list of alternatives based on similarity to a modified element.

        ## GUIDELINES
        1. You will receive a **target element** (modifiedElement) and a **list of alternatives** (newElements) in JSON format.
        2. You must assign each alternative a score from 1 to 5 in two categories:
           - Unique identifier, label, or class similarity
           - Inner text similarity
        3. Sum both scores for each alternative.
        4. Select the element with the highest **total score**. For example if the Total Score of element A is 7 and the Total Score of element B is 9, you should choose element B
        5. Your output must include:
           - The classification table for **all elements**
           - The selected element object enclosed in:
             <BEGIN_ALTERNATIVE>
             <JSON element>
             <END_ALTERNATIVE>
        6. Do not include the score of modifiedElement, score all elements in the newElements list.
        7. Keep the format for classifications as such:
            Element order number. <cssSelector>
            → id or cssSelector property similarity: <1-5>  
            → text property similarity: <1-5>
            → Total: <1-10>
        8. Return only the highest scoring alternative.
    """.trimIndent()
    ),

    // Example 1
    Message(
        "user", """
        {
          "modifiedElement": {
            "type": "INPUT",
            "cssSelector": "#username-field",
            "text": "Username",
            "id": "user-input"
          },
          "newElements": [
            {
              "type": "INPUT",
              "cssSelector": "#user-input",
              "text": "Enter username",
              "id": "user-input"
            },
            {
              "type": "INPUT",
              "cssSelector": "#email-field",
              "text": "Email",
              "id": "email-input"
            },
            {
              "type": "TEXTAREA",
              "cssSelector": "#bio-field",
              "text": "Short bio",
              "id": "bio"
            }
          ]
        }
    """.trimIndent()
    ),
    Message("assistant", """
        Scoring all alternatives:

        1. #user-input
        → id or cssSelector property similarity: 5
        → text property similarity: 4
        → Total: 9

        2. #email-field
        → id or cssSelector property similarity: 2
        → text property similarity: 1
        → Total: 3

        3. #bio-field
        → id or cssSelector property similarity: 1
        → text property similarity: 1
        → Total: 2

        <BEGIN_ALTERNATIVE>
        {
          "type": "INPUT",
          "cssSelector": "#user-input",
          "text": "Enter username",
          "id": "user-input"
        }
        <END_ALTERNATIVE>
    """.trimIndent()),

    // Example 2
    Message(
        "user", """
        {
          "modifiedElement": {
            "type": "BUTTON",
            "cssSelector": ".actions > button.confirm",
            "text": "Confirm",
            "id": "confirm-btn"
          },
          "newElements": [
            {
              "type": "BUTTON",
              "cssSelector": "#proceed",
              "text": "Continue",
              "id": "continue-btn"
            },
            {
              "type": "BUTTON",
              "cssSelector": "#confirm-now",
              "text": "Confirm",
              "id": "confirmation"
            },
            {
              "type": "SPAN",
              "cssSelector": "#label-confirm",
              "text": "Are you sure?"
            }
          ]
        }
    """.trimIndent()
    ),
    Message("assistant", """
        Scoring all alternatives:

        1. #proceed
        → id or cssSelector property similarity: 2
        → text property similarity: 1
        → Total: 3

        2. #confirm-now
        → id or cssSelector property similarity: 3
        → text property similarity: 5
        → Total: 8

        3. #label-confirm
        → id or cssSelector property similarity: 1
        → text property similarity: 2
        → Total: 3

        <BEGIN_ALTERNATIVE>
        {
          "type": "BUTTON",
          "cssSelector": "#confirm-now",
          "text": "Confirm",
          "id": "confirmation"
        }
        <END_ALTERNATIVE>
    """.trimIndent()),

    // Example 3
    Message(
        "user", """
        {
          "modifiedElement": {
            "type": "BUTTON",
            "cssSelector": "#login-button",
            "text": "Log in",
            "id": "login-btn"
          },
          "newElements": [
            {
              "type": "LINK",
              "cssSelector": "#sign-in-link",
              "text": "Sign in",
              "id": "signin-link"
            },
            {
              "type": "BUTTON",
              "cssSelector": "#register-button",
              "text": "Register",
              "id": "register-btn"
            },
            {
              "type": "BUTTON",
              "cssSelector": "#logout-button",
              "text": "Log out",
              "id": "logout-btn"
            }
          ]
        }
    """.trimIndent()
    ),
    Message("assistant", """
        Scoring all alternatives:

        1. #sign-in-link
        → id or cssSelector property similarity: 4
        → text property similarity: 4
        → Total: 8

        2. #register-button
        → id or cssSelector property similarity: 2
        → text property similarity: 1
        → Total: 3

        3. #logout-button
        → id or cssSelector property similarity: 2
        → text property similarity: 1
        → Total: 3

        <BEGIN_ALTERNATIVE>
        {
          "type": "LINK",
          "cssSelector": "#sign-in-link",
          "text": "Sign in",
          "id": "signin-link"
        }
        <END_ALTERNATIVE>
    """.trimIndent()),

    // Example 4
    Message(
        "user", """
        {
          "modifiedElement": {
            "type": "BUTTON",
            "cssSelector": "#reset-password",
            "text": "Reset Password",
            "id": "reset-pass"
          },
          "newElements": [
            {
              "type": "BUTTON",
              "cssSelector": "#recover-pass",
              "text": "Recover Password",
              "id": "recover-pass"
            },
            {
              "type": "LINK",
              "cssSelector": "#forgot-password",
              "text": "Forgot your password?"
            },
            {
              "type": "BUTTON",
              "cssSelector": "#change-email",
              "text": "Change Email",
              "id": "change-email"
            }
          ]
        }
    """.trimIndent()
    ),
    Message("assistant", """
        Scoring all alternatives:

        1. #recover-pass
        → id or cssSelector property similarity: 4
        → text property similarity: 4
        → Total: 8

        2. #forgot-password
        → id or cssSelector property similarity: 2
        → text property similarity: 2
        → Total: 4

        3. #change-email
        → id or cssSelector property similarity: 2
        → text property similarity: 1
        → Total: 3

        <BEGIN_ALTERNATIVE>
        {
          "type": "BUTTON",
          "cssSelector": "#recover-pass",
          "text": "Recover Password",
          "id": "recover-pass"
        }
        <END_ALTERNATIVE>
    """.trimIndent()),

    // Example 5
    Message(
        "user", """
        {
          "modifiedElement": {
            "type": "BUTTON",
            "cssSelector": "#submit-form-btn",
            "text": "Submit",
            "id": "submit-btn"
          },
          "newElements": [
            {
              "type": "BUTTON",
              "cssSelector": "#save-form-btn",
              "text": "Save",
              "id": "save-btn"
            },
            {
              "type": "LINK",
              "cssSelector": "#cancel-link",
              "text": "Cancel",
              "id": "cancel-link"
            },
            {
              "type": "BUTTON",
              "cssSelector": "#submit-form-alt",
              "text": "Submit",
              "id": "submit-alt"
            },
            {
              "type": "INPUT",
              "cssSelector": "#email-input",
              "id": "email-field"
            }
          ]
        }
    """.trimIndent()
    ),
    Message("assistant", """
        Scoring all alternatives:

        1. #save-form-btn
        → id or cssSelector property similarity: 3
        → text property similarity: 2
        → Total: 5

        2. #cancel-link
        → id or cssSelector property similarity: 2
        → text property similarity: 1
        → Total: 3

        3. #submit-form-alt
        → id or cssSelector property similarity: 4
        → text property similarity: 5
        → Total: 9

        4. #email-input
        → id or cssSelector property similarity: 1
        → text property similarity: 1
        → Total: 2

        <BEGIN_ALTERNATIVE>
        {
          "type": "BUTTON",
          "cssSelector": "#submit-form-alt",
          "text": "Submit",
          "id": "submit-alt"
        }
        <END_ALTERNATIVE>
    """.trimIndent())
)
