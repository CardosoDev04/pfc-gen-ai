package modification_detection

import classes.data.Element
import classes.llm.Message
import classes.service_model.Modification

interface IModificationDetectionService {
    /**
     * Gets the missing elements between the previous and new HTML states.
     *
     * @param previousElements The elements present in the previous HTML state.
     * @param newElements The elements present in the new HTML state.
     * @return A list of missing elements.
     */
    suspend fun getMissingElements(previousElements: List<Element>, newElements: List<Element>): List<Element>

    /**
     * Gets the modification for a modified element.
     *
     * @param modifiedElement The modified element.
     * @param newElements The new elements.
     * @return The modification for the element.
     */
    suspend fun getModification(modifiedElement: Element, newElements: List<Element>): Modification<Element>

    /**
     * Modifies the script based on a single modification using mistral.
     *
     * @param oldScript The old script.
     * @param modification The modification to apply.
     * @return The modified script.
     */
    suspend fun modifyMistralScript(oldScript: String, modification: Modification<Element>, modelName: String, systemPrompt: String): String

    /**
     * Modified the script based on a list of modifications using a chat history.
     *
     * @param oldScript The old script.
     * @param modifications The list of modifications to apply.
     * @param modelName The name of the model to use.
     * @param systemPrompt The system prompt to feed to the model.
     * @return The modified script.
     */
    suspend fun modifyScriptChatHistory(oldScript: String, modifications: List<Modification<Element>>, modelName: String, systemPrompt: String): String

    /**
     * Modified the script based on a list of modifications using a chat history.
     *
     * @param oldScript The old script.
     * @param modifications The list of modifications to apply.
     * @param modelName The name of the model to use.
     * @param messages The messages simulating a conversation.
     * @return The modified script.
     */
    suspend fun modifyScriptChatHistory(oldScript: String, modifications: List<Modification<Element>>, modelName: String, messages: List<Message>): String
}