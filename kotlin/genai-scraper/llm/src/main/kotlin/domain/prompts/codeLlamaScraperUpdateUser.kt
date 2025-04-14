package domain.prompts

const val CODE_LLAMA_SCRAPER_UPDATE_PROMPT_SYSTEM = """
    You are a Kotlin Selenium script modifier. When given a script and locator changes, you must return ONLY the modified Kotlin code, without additional commentary, with all locators updated
"""