package dev.kwiksilver.mustache

/**
 * Handles cleanup for Standalone fragments, meaning tag-fragments that other than themselves only have whitespace
 * on the source line and are defined as being able to stand alone.
 * In this case the whitespace for this line is stripped from the preceding and following text fragments so that
 * the entire line containing this tag is stripped from the output.
 *
 * This allows whitespace on lines that only contain comments, delimiter-changes, and section start/end tags without
 * affecting the rendered output.
 *
 * For [PartialFragment]s the stripped indentation is saved back in the [PartialFragment] itself, so that is can
 * be rendered with the appropriate indentation.
 */
internal fun List<Fragment>.cleanStandaloneFragmentLines(): List<Fragment> {
    val updatedFragments = this.toMutableList()
    for (index in updatedFragments.indices) {
        val fragment = updatedFragments[index]
        if (fragment is CanStandAloneFragment) {
            val precededByWhitespace = this.endsWithWhitespaceAfterNewline(index - 1)
            val followedByWhitespace = this.startsWithWhitespaceBeforeNewline(index + 1)

            if (precededByWhitespace && followedByWhitespace) {
                val strippedIndent = updatedFragments.removeCharsAfterLastLinebreak(index - 1)
                updatedFragments.removeCharsUptoAndIncludingFirstLinebreak(index + 1)

                if (fragment is PartialFragment) {
                    updatedFragments[index] = PartialFragment(fragment.name, strippedIndent, fragment.position)
                }
            }
        }
    }

    return updatedFragments
}

/**
 * Checks whether the last line in the [TextFragment] at the given [index] only contains whitespace.
 */
private fun List<Fragment>.endsWithWhitespaceAfterNewline(index: Int): Boolean {
    if (!indices.contains(index)) {
        return true
    }
    if (this[index] !is TextFragment) {
        return false
    }
    val textContent = (this[index] as TextFragment).text
    val lastLinebreakIndex = textContent.lastIndexOfAny(charArrayOf('\r', '\n'))

    if (lastLinebreakIndex == -1 && index > 0) {
        // There are more tags before this one on the same line
        return false
    }

    return textContent.substring(lastLinebreakIndex + 1).all { it.isWhitespace() }
}

/**
 * Checks whether the first line in the [TextFragment] at the given [index] only contains whitespace.
 */
private fun List<Fragment>.startsWithWhitespaceBeforeNewline(index: Int): Boolean {
    if (!indices.contains(index)) {
        return true
    }
    if (this[index] !is TextFragment) {
        return false
    }
    val textContent = (this[index] as TextFragment).text
    var firstLinebreakIndex = textContent.indexOfAny(charArrayOf('\r', '\n'))
    if (firstLinebreakIndex == -1) {
        if (index < lastIndex) {
            // There are more tags after this one on the same line
            return false
        }
        firstLinebreakIndex = textContent.length
    }

    return textContent.substring(0, firstLinebreakIndex).all { it.isWhitespace() }
}

/**
 * Removes the characters after the last linebreak in the [TextFragment] at the given [index].
 * This is only used to remove whitespace.
 */
private fun MutableList<Fragment>.removeCharsAfterLastLinebreak(index: Int): String {
    if (!indices.contains(index)) {
        return ""
    }
    val textFragment = this[index] as TextFragment
    val textContent = textFragment.text
    val lastLinebreakIndex = textContent.lastIndexOfAny(charArrayOf('\r', '\n'))

    if (lastLinebreakIndex != -1 || index == 0) {
        this[index] = TextFragment(
            textContent.substring(0, lastLinebreakIndex + 1),
            textFragment.lineStartPositions,
            textFragment.position
        )
        return textContent.substring(lastLinebreakIndex + 1)
    }

    return ""
}

/**
 * Removes the characters up to and including the first linebreak in the [TextFragment] at the given [index].
 * This is only used to remove whitespace.
 */
private fun MutableList<Fragment>.removeCharsUptoAndIncludingFirstLinebreak(index: Int) {
    if (!indices.contains(index)) {
        return
    }
    val textFragment = this[index] as TextFragment
    val textContent = textFragment.text
    var firstLinebreakIndex = textContent.indexOfAny(charArrayOf('\r', '\n'))
    val linebreakSize: Int
    if (firstLinebreakIndex == -1) {
        firstLinebreakIndex = textContent.length
        linebreakSize = 0
    } else {
        linebreakSize = if (textContent[firstLinebreakIndex] == '\r'
            && firstLinebreakIndex + 1 < textContent.length
            && textContent[firstLinebreakIndex + 1] == '\n'
        ) {
            2
        } else {
            1
        }
    }

    val offset = firstLinebreakIndex + linebreakSize
    this[index] =
        TextFragment(textContent.substring(offset), textFragment.lineStartPositions, textFragment.position + offset)
}
