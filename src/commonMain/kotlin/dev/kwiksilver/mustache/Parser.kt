package dev.kwiksilver.mustache

fun parseTemplate(template: String): Template {
    var openDelimiter = "{{"
    var closeDelimiter = "}}"
    var position = 0

    val fragments = mutableListOf<Fragment>()

    val lineStartIndices = template.findAllLineStartIndices()

    while (position < template.length) {
        val openPos = template.indexOf(openDelimiter, position)
        val tripleOpenPos = template.indexOf("{{{", position)

        if (tripleOpenPos != -1 && tripleOpenPos <= openPos && openPos != -1) {
            if (tripleOpenPos > position) {
                val fragmentLineStarts = lineStartIndices.filter { it in position..tripleOpenPos }.map { it - position }
                fragments.add(TextFragment(template.substring(position, tripleOpenPos), fragmentLineStarts, position))
            }
            position = parseTripleMustache(template, tripleOpenPos, fragments)
            continue
        }

        if (openPos == -1) {
            val fragmentLineStarts = lineStartIndices.filter { it in position..template.length }.map { it - position }
            fragments.add(TextFragment(template.substring(position), fragmentLineStarts, position))
            break
        }

        if (openPos > position) {
            val fragmentLineStarts = lineStartIndices.filter { it in position..openPos }.map { it - position }
            fragments.add(TextFragment(template.substring(position, openPos), fragmentLineStarts, position))
        }

        val closePos = template.indexOf(closeDelimiter, openPos + openDelimiter.length)
        if (closePos == -1) {
            fragments.add(ErrorFragment("Open delimiter without closing delimiter", openPos))
            break
        }

        position = closePos + closeDelimiter.length

        val actionFragment = buildActionFragment(template.substring(openPos + openDelimiter.length, closePos), openPos)
        fragments.add(actionFragment)

        if (actionFragment is DelimiterChangeFragment) {
            openDelimiter = actionFragment.openDelimiter
            closeDelimiter = actionFragment.closeDelimiter
        }
    }

    return Template(fragments.cleanStandaloneFragmentLines().constructSections())
}

private fun buildActionFragment(actionText: String, position: Int): Fragment {
    return when {
        actionText.startsWith('!') -> CommentFragment(position)
        actionText.startsWith('=') -> DelimiterChangeFragment(actionText, position)
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
