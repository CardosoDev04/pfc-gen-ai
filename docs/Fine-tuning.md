## What is it?

Fine-tuning a large language model(LLM) consistes in the process of adapting an existing model to perform a specific task with greater precision an efficiency.

This process increases the model's performance on specialised tasks and significantly broadens it's applicability through various subjects.

Today, most LLMs have an acceptable general-purpose performance but they often struggle with specific task-oriented problems. The process of fine-tuning, by introducing a task-specific data set into a model, allows it better understand and solve focused problems without the need to build a new model from scratch.

Essentially, fine-tuning tailors an existing LLM to increase its performance for solving specific tasks.

## Types of Fine-tuning

Fine-tuning can be achieved through multiple pathways, each suited to particular objectives and resources. Below we list some of the most common approaches:

### Supervised Fine-tuning

This approach trains a model on a labeled dataset tailored to the target task to perform, such as text classification on named entity recognition.

Example:
Providing a dataset of phrases labeled with sentiment(positive, negative or neutral) to train a model that can classify the emotional tone of a text.

### Few-shot Learning

In this case, you write well-crafted prompts that include examples, from which the model will infer the desired behaviour. This allows the model to have better context of the task without an extensive fine-tuning process.

Example:
```
Q: What’s the capital of France?
A: Paris

Q: What’s the capital of Germany?
A: Berlin

Q: What’s the capital of Portugal?
A:

```

To which the LLM will learn the pattern and answer Lisbon.
### Transfer Learning

This technique involves taking a model that was trained for one task and fine-tuning it to a different, yet related, task. It leverages the model's existing language understanding to adapt it mode efficiently to new challenges.

Example:
- Pretrained BERT:
  You start with the BERT model that was trained on a general corpus like Wikipedia and BooksCorpus. It already knows how language works - grammar, sentence structure, common knowledge, etc.

- Fine-tuned on BioASQ:  
  The BERT model is then fine-tuned using the BioASQ dataset - composed of question/answer pairs - to specialise it for biomedical question answering.

- The Goal:  
  The goal is a model better equipped at answering medical questions — like a doctor-style assistant that can understand and answer questions about diseases, drugs, symptoms, etc.

### Domain-specific fine-tuning

This method adapts the model to understand and generate text that is specific to a particular domain or industry by being fine-tuned on a dataset composed of text from the target domain in order to improve its context and knowledge of domain-specific tasks.

Example:
To generate a chatbot for a medical application, the model could be trained with medical records, clinical notes or medical literature to adapt its language understanding capabilities to the healthcare field.

### Instruction Tuning

Instruction tuning involves training a model on datasets structures as triples: instruction, input and output. This helps the model understand and complete tasks via instructions.

Example:

```
{
  "instruction": "Translate English to French",
  "input": "I love programming.",
  "output": "J'adore programmer."
}
```

## Conclusion

There are multiple approaches to fine-tuning a large language model (LLM), each tailored to different goals, resources, and use cases. The diversity of methods makes it essential to understand their characteristics before choosing one.

Selecting the most effective strategy requires both technical insight and a clear understanding of the desired application. Whether the goal is to adapt a model to a specific domain, improve its performance on a particular task, or make it more responsive to natural instructions, there is a fine-tuning method that fits.

Ultimately, fine-tuning is a powerful and reliable way to leverage the strengths of existing LLMs while adapting them to specific needs. It allows developers and researchers to save time and resources by building upon a strong foundation rather than starting from scratch, making it one of the most efficient and scalable ways to bring advanced language models into real-world applications.

## References

- https://www.datacamp.com/tutorial/fine-tuning-large-language-models?dc_referrer=https%3A%2F%2Fwww.google.com%2F
- https://www.superannotate.com/blog/llm-fine-tuning
- https://fenix.tecnico.ulisboa.pt/downloadFile/563345090419087/extended_abstract.pdf