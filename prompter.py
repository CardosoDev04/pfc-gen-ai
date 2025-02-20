import ollama
from bs4 import BeautifulSoup
from web_fetcher import WebFetcher
from utils import Utils
import random
import string
import os

MODEL_NAME = "mistral:7b"

def extract_interactive_elements(html):
    soup = BeautifulSoup(html, "html.parser")
    elements = []

    for button in soup.find_all("button"):
        elements.append(str(button))
    for input_tag in soup.find_all("input"):
        elements.append(str(input_tag))
    for link in soup.find_all("a"):
        elements.append(str(link))
    for select in soup.find_all("select"):
        elements.append(str(select))

    return "\n".join(elements)

def chunk_elements(text, max_size=1000):
    """Split elements into chunks of maximum size while preserving complete elements"""
    chunks = []
    current_chunk = []
    current_size = 0
    
    # Split by newline to preserve complete elements
    elements = text.split('\n')
    
    for element in elements:
        element_size = len(element)
        if current_size + element_size > max_size and current_chunk:
            # Add current chunk to results and start new chunk
            chunks.append('\n'.join(current_chunk))
            current_chunk = []
            current_size = 0
        
        current_chunk.append(element)
        current_size += element_size

    # Add final chunk if not empty
    if current_chunk:
        chunks.append('\n'.join(current_chunk))
        
    return chunks

SYSTEM_PROMPT = """You are an HTML interaction analyzer. You will receive HTML code and must extract all possible interactions, following these rules:
1. Only analyze actual HTML elements provided
2. Return interactions in the format: ["Element identifier (id, arialabel, etc...)","Element Type", "Element Text (null if none)", "Action Type"]
3. Do not hallucinate or make up elements
4. Do make up element identifiers
5. Only extract from the HTML provided
6. Do not return any other text besides the requested interpretation, not even comments
7. The action of typing should be referred to as "type", Selecting should be referred to as "select", Clicking should be referred to as "click", Hover should be referred to as "hover". Element Type should the be HTML element code name, exactly like it's written, in lowercase (for example, a, div, button, form, etc...). Action types and Element Types should be lowercase.
8. Element text refers to, for example, input placeholders, labels or button text
9. Do not include any ordering or bullet points in your response, keep it to the format
10. You should only return one top level array with the interactions as it's elements. Not multiple arrays per line.
11. Here is an example of a correct complete output: [["example-btn-id","button", "Click Me", "click"], ["password-input","input", "Type here", "type"]]
12. Here is an example of a incorrect complete output:  [["example-btn-id","button", "See details", "click"]][["example-btn-2","button", "See details", "click"]][["example-btn-3","button", "See details", "click"]]
13. If you are asked for the identifier of a button given a certain target action, respond according to the following example format: ["id": button-id", "text": "button text"]
"""

def extract_interactions(html):
    """Process a chunk of HTML through the LLM with proper prompting"""
    full_prompt = f"{SYSTEM_PROMPT}\n\nHTML Input:\n{html}\n\nExtract all possible interactions in the specified format:"
    
    response = ollama.chat(
        model=MODEL_NAME,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": f"HTML Input:\n{html}"}
        ]
    )
    return response["message"]["content"]


def process_elements_with_llm(html, llm_function, max_chunk_size=1000):
    """Process HTML elements in chunks through the LLM"""
    # Extract elements
    elements = extract_interactive_elements(html)
    
    # Split into chunks
    chunks = chunk_elements(elements, max_chunk_size)
    
    # Process each chunk
    results = []
    for chunk in chunks:
        result = llm_function(chunk)
        results.append(result)
        
    return results

        
def get_results(url):
    fetcher = WebFetcher()
    fetched = fetcher.extract_html(url)
    Utils.save_to_file(fetched, "output.html")
    html_input = Utils.read_from_file("output.html")
    results = process_elements_with_llm(html_input, extract_interactions)
    
    return results

def get_element_by_action(results, target_action):
    response = ollama.chat(
        model=MODEL_NAME,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": target_action}
        ]
    )
    return response["message"]["content"]
    
              
if __name__ == "__main__":
    url = input("URL: ").strip()
    save_path = input("Path to save results (/results by default): ").strip()
    if not save_path:
        save_path = "results"
    filename = input("Results file name (random by default): ").strip()
    if not filename:
        letters = string.ascii_lowercase
        filename = ''.join(random.choice(letters) for i in range(12))
    filename += '.md'
    save_path += '/'
    results = get_results(url)
    os.makedirs(save_path, exist_ok=True)
    target_element_id = get_element_by_action(results, f"Out of the possible HTML interactions, what is the id of the HTML element that can be interacted with in order to get more information about the life of the company?\nHTML interactions available:{", ".join(results)}")
    print(target_element_id)
    Utils.save_to_file(results, save_path + filename)
