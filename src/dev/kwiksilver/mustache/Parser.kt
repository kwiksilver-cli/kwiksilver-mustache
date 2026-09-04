package dev.kwiksilver.mustache

/**
 * A simple parser for the [template], which linearly scans for the current delimiter sequences as well
 * as the triple-mustache delimiter that cannot be modified.
 *
 * It internally uses a mutable list of fragments that were gathered so far, and the current position indicating
 * how far the source [template] has been processed.
 */
internal fun parseTemplate(template: String): Template {
    var openDelimiter = "{{"
    var closeDelimiter = "}}"
    var position = 0

    val fragments = mutableListOf<Fragment>()

    val lineStartIndices = template.findAllLineStartIndices()

    while (position < template.length) {
        val openPos = template.indexOf(openDelimiter, position)
        val tripleOpenPos = template.indexOf("{{{", position)

        if (tripleOpenPos != -1 && (tripleOpenPos <= openPos || openPos == -1)) {
            // Handle the triple-mustache interpolation.

            if (tripleOpenPos > position) {
                // Add any text found before the triple mustache opening
                val fragmentLineStarts = lineStartIndices.filter { it in position..tripleOpenPos }.map { it - position }
                fragments.add(TextFragment(template.substring(position, tripleOpenPos), fragmentLineStarts, position))
            }
            position = parseTripleMustache(template, tripleOpenPos, fragments)
            continue
        }

        if (openPos == -1) {
            // No more opening delimiters found. Wrap up the last bit of text and finish the parsing loop.

            val fragmentLineStarts = lineStartIndices.filter { it in position..template.length }.map { it - position }
            fragments.add(TextFragment(template.substring(position), fragmentLineStarts, position))
            break
        }

        if (openPos > position) {
            // Add the text between the old position and the next opening delimiter.

            val fragmentLineStarts = lineStartIndices.filter { it in position..openPos }.map { it - position }
            fragments.add(TextFragment(template.substring(position, openPos), fragmentLineStarts, position))
        }

        val closePos = template.indexOf(closeDelimiter, openPos + openDelimiter.length)
        if (closePos == -1) {
            // No close delimiter found for the current opening delimiter. Wrap up with an error fragment and finish.

            fragments.add(ErrorFragment("Open delimiter without closing delimiter", openPos))
            break
        }

        position = closePos + closeDelimiter.length

        // Build a new fragment based on the tag type.
        val actionFragment = buildActionFragment(template.substring(openPos + openDelimiter.length, closePos), openPos)
        fragments.add(actionFragment)

        if (actionFragment is DelimiterChangeFragment) {
            // Change the delimiters for parsing.

            openDelimiter = actionFragment.openDelimiter
            closeDelimiter = actionFragment.closeDelimiter
        }
    }

    // Clean up and restructure the simple list of fragments before wrapping it a Template instance.
    return Template(fragments.cleanStandaloneFragmentLines().constructSections())
}

/**
 * Builds different fragments based on the first character of the tag contents.
 */
private fun buildActionFragment(actionText: String, position: Int): Fragment {
    return when {
        actionText.startsWith('!') -> CommentFragment(position)
        actionText.startsWith('=') -> parseDelimiterChange(actionText, position)
        actionText.startsWith('#') -> SectionStartFragment(parseValuePath(actionText.substring(1)), position)
        actionText.startsWith('/') -> SectionEndFragment(parseValuePath(actionText.substring(1)), position)
        actionText.startsWith('^') -> InvertedSectionStartFragment(parseValuePath(actionText.substring(1)), position)
        actionText.startsWith('&') -> InterpolationFragment(parseValuePath(actionText.substring(1)), position)
        actionText.startsWith('>') -> PartialFragment(actionText.substring(1).trim(), "", position)
        else -> InterpolationFragment(parseValuePath(actionText), position, escapeHtml = true)
    }
}


private fun parseValuePath(text: String) = text.trim().split('.').filter { it.isNotEmpty() }

private fun parseTripleMustache(template: String, tripleOpenPos: Int, fragments: MutableList<Fragment>): Int {
    val tripleClosePos = template.indexOf("}}}", tripleOpenPos + 3)
    if (tripleClosePos == -1) {
        fragments.add(ErrorFragment("Open delimiter without closing delimiter", tripleOpenPos))
        return template.length
    }

    fragments.add(InterpolationFragment(parseValuePath(template.substring(tripleOpenPos + 3, tripleClosePos)), tripleOpenPos))

    return tripleClosePos + 3
}

private fun parseDelimiterChange(actionText: String, position: Int): Fragment {
    if (!actionText.endsWith('=')) {
        return ErrorFragment("Delimiter set action must end with '='", position)
    }

    val newDelimiters = actionText.subSequence(1, actionText.length - 1).trim().split("\\s+".toRegex())

    if (newDelimiters.size != 2) {
        return ErrorFragment("Delimiter set action must specify exactly 2 delimiters", position)
    }

    return DelimiterChangeFragment(newDelimiters[0], newDelimiters[1], position)
}

/**
 * Find the indices where a new line starts in the text.
 */
internal fun CharSequence.findAllLineStartIndices(): List<Int> {
    val lineStarts = mutableListOf<Int>()

    val lineBreakChars = charArrayOf('\n', '\r')
    var position = 0

    while (position < length) {
        lineStarts.add(position)
        val nextBreak = indexOfAny(lineBreakChars, position)
        if (nextBreak == -1) {
            break
        }

        position = if (this[nextBreak] == '\r' && this[nextBreak + 1] == '\n') {
            nextBreak + 2
        } else {
            nextBreak + 1
        }
    }

    return lineStarts
}
