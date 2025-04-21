package persistence

interface PersistenceService {
    /**
     * Writes the given content to a file representing the model.
     *
     * @param filePath The path for the result.
     * @param content The content to write to the file.
     */
    fun write(filePath: String, content: String)

    /**
     * Writes the given content to a file representing the model.
     *
     * @param modelName The name of the model.
     * @param fileName The name of the file.
     * @param fileContent The content to write to the file.
     */
    fun write(modelName: String, fileName: String, fileContent: String)

    /**
     * Reads the content of a file.
     *
     * @param scraperPath The path to the file.
     * @return The content of the file.
     */
    fun read(scraperPath: String): String

    /**
     * Copies the content of a file from the source path to the destination path and deletes the source file.
     *
     * @param from The path of the file to copy the contents from.
     * @param to The path of the file to paste the contents into.
     */
    fun copyAndDeleteFile(from: String, to: String)
}