import ollama

MODEL_NAME = "mistral:7b"

SYSTEM_PROMPT = """You are a specialist in analyzing HTML elements and finding out their purpose. You will receive HTML elements and a prompt which identifies a target action and your job is to find the html element that better suits the given action.
1. Only consider the elements provided.
2. Return results in the following representation: {"id": "element-id", "text": "elements inner text"}
3. Do not make up elements.
4. Do not return any kind of introduction or explanatory text, just return the requested representation.
5. Do not hallucinate. 
"""

def get_element_by_action(results, target_action):
    response = ollama.chat(
        model=MODEL_NAME,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": target_action}
        ]
    )
    return response["message"]["content"]