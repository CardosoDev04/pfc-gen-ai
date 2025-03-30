# Projects Logs

## Sunday 30th March

We encountered a problem where the LLM was ignoring the return of the scraper function, even when it was being told not to in the system prompt. We have found that if we included the scrapers interface, commented out in the beginning of the source file, it would include the return as expected and would not delete it.

First successful run E2E, using our own scraper and demo website.

