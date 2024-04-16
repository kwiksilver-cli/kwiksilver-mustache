package dev.kwiksilver.mustache

import kotlinx.serialization.json.JsonElement

object Mustache {
    fun process(templateText: String, context: JsonElement? = null): String {
        val template = parse(templateText)
        return template.render(context)
    }

    private fun parse(template: String): Template {
        var openDelimiter = "{{"
        var closeDelimiter = "}}"
        var position = 0

        val fragments = mutableListOf<Fragment>()

        while (position < template.length) {
            val openPos = template.indexOf(openDelimiter, position)
            val tripleOpenPos = template.indexOf("{{{", position)

            if (tripleOpenPos != -1 && tripleOpenPos <= openPos) {
                if (tripleOpenPos > position) {
                    fragments.add(TextFragment(template.substring(position, tripleOpenPos)))
                }
                position = parseTripleMustache(template, tripleOpenPos, fragments)
                continue
            }

            if (openPos == -1) {
                fragments.add(TextFragment(template.substring(position)))
                break
            }

            if (openPos > position) {
                fragments.add(TextFragment(template.substring(position, openPos)))
            }

            val closePos = template.indexOf(closeDelimiter, openPos + openDelimiter.length)
            if (closePos == -1) {
                fragments.add(ErrorFragment("Open delimiter without closing delimiter found at $openPos"))
                break
            }

            position = closePos + closeDelimiter.length

            val actionFragment = buildActionFragment(template.substring(openPos + openDelimiter.length, closePos))
            fragments.add(actionFragment)

            if (actionFragment is DelimiterChangeFragment) {
                openDelimiter = actionFragment.openDelimiter
                closeDelimiter = actionFragment.closeDelimiter
            }
        }

        return Template(fragments.cleanStandaloneFragmentLines())
    }

    private fun parseTripleMustache(template: String, tripleOpenPos: Int, fragments: MutableList<Fragment>): Int {
        val tripleClosePos = template.indexOf("}}}", tripleOpenPos + 3)
        if (tripleClosePos == -1) {
            fragments.add(ErrorFragment("Open delimiter without closing delimiter found at $tripleOpenPos"))
            return template.length
        }

        fragments.add(InterpolationFragment(template.substring(tripleOpenPos + 3, tripleClosePos)))

        return tripleClosePos + 3
    }
}

