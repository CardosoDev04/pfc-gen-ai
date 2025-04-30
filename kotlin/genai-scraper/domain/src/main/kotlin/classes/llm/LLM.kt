package classes.llm

enum class LLM(val modelName: String) {
    Mistral7B("mistral:7B"),
    Llama8B("llama3.1:8b"),
    CodeLlama7B("codellama:7b"),
    DeepSeekCoder1Point3B("deepseek-coder:1.3b"),
    Gemma3_1B("gemma3:1b"),
    Gemma3_4B("gemma3:4b"),
}