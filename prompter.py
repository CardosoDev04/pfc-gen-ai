import ollama
from bs4 import BeautifulSoup

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

def read_from_file(filename: str) -> str:
    try:
        with open(filename, 'r', encoding='utf-8') as f:
            return f.read()
    except IOError as e:
        print(f"Error reading file: {e}")
        return ""

SYSTEM_PROMPT = """You are an HTML interaction analyzer. You will receive HTML code and must extract all possible interactions, following these rules:
1. Only analyze actual HTML elements provided
2. Return interactions in the format: ["Element Type", "Element Text (null if none)", "Action Type"]
3. Do not hallucinate or make up elements
4. Only extract from the HTML provided"""

def extract_interactions(html):
    """Process a chunk of HTML through the LLM with proper prompting"""
    full_prompt = f"{SYSTEM_PROMPT}\n\nHTML Input:\n{html}\n\nExtract all possible interactions in the specified format:"
    
    response = ollama.chat(
        model="mistral:7b",
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

html_input = read_from_file("output.html")
results = process_elements_with_llm(html_input, extract_interactions)
print(results)
