package domain.templates

val ELEMENT_RECOVERY_PROMPT_TEMPLATE = """
    The scraper encountered an error due to a missing HTML element.

    Exception:
    %s

    The scraper previously relied on the following element, which no longer exists in the page:
    %s

    Below is a list of `Element` candidates extracted from the new HTML snapshot:
    %s

    Instructions:

    You must analyze the provided elements and select the one that best matches the missing element the scraper used before the error.

    Return only one element inside:

    <BEGIN_ALTERNATIVE>
    <your_json_selection_here>
    </END_ALTERNATIVE>
""".trimIndent()
