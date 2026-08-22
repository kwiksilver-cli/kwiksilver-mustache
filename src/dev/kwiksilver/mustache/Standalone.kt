package dev.kwiksilver.mustache

fun List<Fragment>.cleanStandaloneFragmentLines(): List<Fragment> {
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
