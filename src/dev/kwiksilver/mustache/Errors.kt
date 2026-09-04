package dev.kwiksilver.mustache

/**
 * Maps the errors in a parsed [Template] to a [Position] in the [templateText] and
 * transform that into error messages that include that position indication.
 */
internal fun Template.generateErrors(templateText: String): List<String> {
    val errorFragments = this.fragments.filterIsInstance<ErrorFragment>()
    return errorFragments.map { errorFragment ->
        val position = Position.fromOffset(errorFragment.position, templateText)
        "$position: ${errorFragment.message}"
    }
}

/**
 * Indicates a positions by [line] and [column] in the template source text.
 */
internal data class Position(val line: Int, val column: Int) {
    companion object {
        /**
         * Transforms an offset [position] in the [source] text into a [line] and [column] [Position].
         */
        fun fromOffset(position: Int, source: String): Position {
            val newlinePositions = source.withIndex().filter{it.value == '\n'}.map{it.index}

            if (newlinePositions.isEmpty() || position <= newlinePositions.first()) {
                // No newlines in source or position is on the first line
                return Position(1, position + 1)
            }

            val lastPassedNewlineIndex = newlinePositions.indexOfLast{it < position}
            if (lastPassedNewlineIndex == -1) {
                // After last newline
                val line = newlinePositions.size + 1
                val column = position - newlinePositions.last()
                return Position(line, column)
            }

            val lastNewlinePosition = newlinePositions[lastPassedNewlineIndex]
            val passedNewlineCount = lastPassedNewlineIndex + 1
            return Position(passedNewlineCount + 1, position - lastNewlinePosition)
        }
    }
}