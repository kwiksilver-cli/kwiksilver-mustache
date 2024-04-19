package dev.kwiksilver.mustache

fun parseTemplate(template: String): Template {
    var openDelimiter = "{{"
    var closeDelimiter = "}}"
    var position = 0

    val fragments = mutableListOf<Fragment>()

    while (position < template.length) {
        val openPos = template.indexOf(openDelimiter, position)
        val tripleOpenPos = template.indexOf("{{{", position)

        if (tripleOpenPos != -1 && tripleOpenPos <= openPos) {
            if (tripleOpenPos > position) {
                fragments.add(TextFragment(template.substring(position, tripleOpenPos), position))
            }
            position = parseTripleMustache(template, tripleOpenPos, fragments)
            continue
        }

        if (openPos == -1) {
            fragments.add(TextFragment(template.substring(position), position))
            break
        }

        if (openPos > position) {
            fragments.add(TextFragment(template.substring(position, openPos), position))
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

fun buildActionFragment(actionText: String, position: Int): Fragment {
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
