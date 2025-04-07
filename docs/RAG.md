# Retrieval Augmented Generation

Large language models are trained on vast volumes of data and use billions of parameters to generate original output for tasks like answering questions, translating languages, and completing sentences. However, they are unable to access up-to-date information or domain-specific knowledge that is not part of their training data. Furthermore, these types of models are known for hallucinating ([[Hallucination]]).

Retrieval-Augmented Generation, also known as RAG, comes to solve these issues. RAG is the process of optimizing the output of a large language model, so it references an authoritative knowledge base outside of its training data sources before generating a response. This process results in a higher factual accuracy, reduced hallucination and allows for domain-specific applications without having to retrain the model which can be quite expensive.

![RAG](./images/jumpstart-fm-rag.jpg)
## Retrieval Function

In Retrieval Augmented Generation the **retrieval function** is responsible for finding the most relevant pieces of information according to the question from external knowledge sources to help the language model generate better answers.




[https://blogs.nvidia.com/blog/what-is-retrieval-augmented-generation/]
[https://aws.amazon.com/what-is/retrieval-augmented-generation/]
[https://chatgpt.com]