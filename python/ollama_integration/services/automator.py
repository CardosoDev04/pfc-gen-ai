import ollama

SYSTEM_PROMPT = """You are a Playwright script generator. You will receive formatted input representing HTML elements and possible interactions with them, as well as an end goal.
1. The interactions will be elements in an array and will follow this format: ["Element identifier (id, arialabel, etc...)","Element Type", "Element Text (null if none)", "Action Type"]
2. You must generate a Playwright script to achieve the end goal. For example, if the end goal is fill out a user's password, you must generate a script that interacts with the password input identified in the input array and fills it out.
3. You will receive the input in this format: "[Interactions], "Action to perform""
4. Do not deviate from the provided elements, you cannot make anything up. Do not, in any circumstance, hallucinate.
5. You cannot provide comments in the generated script, nor any other text besides the code. Your output must be clean and concise.
6. You can use the element identifier to find the element in your script so you can interact with it.
7. Make your script as simple as possible without compromising the goal.
"""


def generate_script(action, interactions):
    response = ollama.chat(
        model="mistral:7b",
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": f"{interactions},{action}"}
        ]
    )
    return response["message"]["content"]


if __name__ == "__main__":
    result = generate_script("Click the login button", """[["login-btn", "button", "Log In", "click"]]""")
    print(result)