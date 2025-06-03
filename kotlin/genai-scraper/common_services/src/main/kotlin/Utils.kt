object Utils {
    fun String.replaceFirstLine(newLine: String): String {
        val lines = this.lines()
        if (lines.isNotEmpty()) {
            return (listOf(newLine) + lines.drop(1)).joinToString("\n")
        }
        return newLine
    }
}