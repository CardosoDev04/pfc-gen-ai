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

if __name__ == "__main__":
    print("Module")
