package modification_detection

import classes.data.Element
import classes.llm.Message
import classes.service_model.Modification

interface IModificationService {
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

    /**
     * Modified the script based on a list of missing elements using a chat history.
     *
     * @param oldScript The old script.
     * @param missingElements The list of missing elements to apply.
     * @param modelName The name of the model to use.
     * @param messages The messages simulating a conversation.
     * @return The modified script.
     */
    suspend fun modifyScriptChatHistoryV2(oldScript: String, missingElements: List<Modification<Element>>, modelName: String, messages: List<Message>): String

    /**
     * Get elements interacted with in the scraper that are not present in the new HTML state.
     *
     * @param stableHtmlSnapshotElements The elements found in the stable snapshot
     * @param latestHtmlSnapshotElements The new elements to compare against
     * @param system The system prompt to use
     * @param prompt The prompt to use
     * @return The list of elements of the script
     */
    suspend fun getMissingElementsFromScript(stableHtmlSnapshotElements: List<Element>, latestHtmlSnapshotElements: List<Element>, exceptionMessage: String, system: String, prompt: List<Message>): List<Element>
}