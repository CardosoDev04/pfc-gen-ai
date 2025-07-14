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

    /**
     * Gets an alternative element for a missing element based on the provided alternatives and the exception message.
     *
     * @param latestSnapshotHtmlElements The HTML elements from the latest snapshot.
     * @param exceptionMessage The exception message indicating the missing element.
     * @param missingElement The element that was expected but is missing.
     * @return A modification containing the missing element and its alternative.
     */
    suspend fun getMissingElementAlternative(latestSnapshotHtmlElements: List<Element>, exceptionMessage: String, missingElement: Element): Modification<Element>
}
