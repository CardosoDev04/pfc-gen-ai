import sys
import os

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

class Utils:
    def read_from_file(filename: str) -> str:
        try:
            with open(filename, 'r', encoding='utf-8') as f:
                return f.read()
        except IOError as e:
            print(f"Error reading file: {e}")
            return ""
        
    def save_to_file(content: str | list, filename: str) -> None:
        try:
            with open(filename, 'w', encoding='utf-8') as f:
                if isinstance(content, list):
                    content = '\n'.join(str(item) for item in content)
                f.write(str(content))
        except IOError as e:
            print(f"Error writing to file: {e}")
        except Exception as e:
            print(f"Unexpected error while writing to file: {e}")