package domain.prompts

const val CODE_LLAMA_SCRAPER_UPDATE_PROMPT_USER = """
## Original Import Statements
{imports}

## Original Selenium Script
{code}

## Locator Changes
{locator_changes}
"""