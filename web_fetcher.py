from playwright.sync_api import sync_playwright

class WebFetcher:
    def extract_html(self, url: str) -> str:
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_page()
            page.goto(url, wait_until="networkidle")
            html = page.content()
            browser.close()
        return html

    def save_to_file(self, content: str, filename: str) -> None:
        try:
            with open(filename, 'w', encoding='utf-8') as f:
                f.write(content)
        except IOError as e:
            print(f"Error writing to file: {e}")


if __name__ == "__main__":
    print("Module")
