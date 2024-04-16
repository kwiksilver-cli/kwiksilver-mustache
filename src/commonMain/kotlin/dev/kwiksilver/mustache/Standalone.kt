package dev.kwiksilver.mustache

fun List<Fragment>.cleanStandaloneFragmentLines(): List<Fragment> {
    val updatedFragments = this.toMutableList()
    for (index in updatedFragments.indices) {
        val fragment = updatedFragments[index]
        if (fragment is CanStandAloneFragment) {
            val precededByWhitespace = updatedFragments.endsWithWhitespaceAfterNewline(index - 1)
            val followedByWhitespace = updatedFragments.startsWithWhitespaceBeforeNewline(index + 1)

            if (precededByWhitespace && followedByWhitespace) {
                updatedFragments.removeCharsAfterLastLinebreak(index - 1)
                updatedFragments.removeCharsUptoAndIncludingFirstLinebreak(index + 1)
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
        firstLinebreakIndex = textContent.length
    }

    return textContent.substring(0, firstLinebreakIndex).all { it.isWhitespace() }
}

private fun MutableList<Fragment>.removeCharsAfterLastLinebreak(index: Int) {
    if (!indices.contains(index)) {
        return
    }
    val textContent = (this[index] as TextFragment).text
    val lastLinebreakIndex = textContent.lastIndexOfAny(charArrayOf('\r', '\n'))

    this[index] = TextFragment(textContent.substring(0, lastLinebreakIndex + 1))
}

private fun MutableList<Fragment>.removeCharsUptoAndIncludingFirstLinebreak(index: Int) {
    if (!indices.contains(index)) {
        return
    }
    val textContent = (this[index] as TextFragment).text
    var firstLinebreakIndex = textContent.indexOfAny(charArrayOf('\r', '\n'))
    val linebreakSize: Int
    if (firstLinebreakIndex == -1) {
        firstLinebreakIndex = textContent.length
        linebreakSize = 0
    } else {
        linebreakSize = if (textContent[firstLinebreakIndex] == '\r'
            && firstLinebreakIndex + 1 < textContent.length
            && textContent[firstLinebreakIndex + 1] == '\n') {
            2
        } else {
            1
        }
    }

    this[index] = TextFragment(textContent.substring(firstLinebreakIndex + linebreakSize))
}
