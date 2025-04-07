# LLM Hallucinations

LLM hallucinations are the events in which ML models, particularly large language models, produce outputs that are coherent and grammatically correct but **factually incorrect or nonsensical**. “Hallucinations” in this context means the generation of false or misleading information. They are a critical obstacle in the development of LLMs, often arising from the training data's quality and the models' interpretative limits.

## Types of Hallucinations

LLM hallucinations can be categorized into many types. Some of these are:
### Fact-conflicting hallucination

Fact-conflicting hallucinations occur when large language models (LLMs) generate information that contradicts known or verified facts. These errors can occur without warning and may affect the reliability of the model’s output.
### Input-conflicting hallucination

Input-conflicting hallucinations occur when the output produced by an LLM diverges from what the user specifies. Generally, the user's input into an LLM consists of two parts: the task direction (for instance, a user prompt for summarization) and the task material (for instance, the document to be summarized). A discrepancy between the LLM's output and the task direction often indicates a misinterpretation of the user's objectives.

### Context-conflicting hallucination

In context-conflicting hallucination, LLMs may produce outputs that are inconsistent or contain self-contradictions, something particularly noticeable in longer or multipart responses. These hallucinations occur when LLMs either overlook the broader context or have difficulty maintaining uniformity throughout an exchange.

## What causes these hallucinations?

There are many factors that contribute to LLM hallucinations. Some of these are:
### Prediction-Based Nature

LLMs work by **predicting the next word** in a sequence based on patterns they've seen in training data, not by understanding truth or facts. So if the model has seen a lot of plausible-sounding but incorrect information, it may reproduce or even remix it. Also, when faced with uncertainty, they don't say "I don't know" but instead produce the most statistically plausible continuation based on similar patterns they've seen before, even if it is not factually correct.

Large language models (LLMs) generate text by **predicting the next word** in a sequence based on patterns learned from their training data, not by understanding truth or facts. As a result, if the model has been exposed to a lot of plausible-sounding but inaccurate information, it may reproduce or even remix those inaccuracies. Furthermore, when uncertain, LLMs typically don’t admit “I don’t know”. Instead, they generate the most statistically likely continuation based on similar patterns, even if the result is factually incorrect.

### Limitations of Training Data

A large language model is only as reliable as the data it was trained on. If that data is outdated, inaccurate, or factually incorrect, the model may absorb and reproduce those flaws in its responses.

### Lack of Real-Time Knowledge

LLMs are trained on static datasets collected at a fixed point in time. As a result, they can only absorb factual information that was available up to their training cutoff date. They lack access to real-time updates or ongoing events. Because of this limitation, LLMs are more prone to hallucinations or inaccuracies when generating responses about events, developments, or facts that occurred after their training period.

## How to avoid hallucinations

Some key strategies to avoid LLM hallucination are:

- Advanced Prompting Techniques like **Few Shot Prompting** or **Chain of Thought Prompting**
- Retrieval Augmented Generation ([[RAG]])
- Fine-tuning LLMs

## References

- https://www.lakera.ai/blog/guide-to-hallucinations-in-large-language-models#:~:text=Hallucinations%20in%20LLMs%20refer%20to,trust%20placed%20in%20these%20models
- https://www.turing.com/resources/minimize-llm-hallucinations-strategy
- https://www.vellum.ai/blog/how-to-reduce-llm-hallucinations
- https://ai.plainenglish.io/the-challenge-of-keeping-llms-factual-93b32468def9
- https://chatgpt.com