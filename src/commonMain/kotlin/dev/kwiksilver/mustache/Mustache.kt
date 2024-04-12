package dev.kwiksilver.mustache

import kotlinx.serialization.json.JsonElement

object Mustache {
    fun process(templateText: String, data: JsonElement? = null): String {
        val template = parse(templateText)
        return template.render(data)
    }

    private fun parse(template: String): Template {
        var openDelimiter = "{{"
        var closeDelimiter = "}}"
        var position = 0

        val fragments = mutableListOf<Fragment>()

        while (position < template.length) {
            val openPos = template.indexOf(openDelimiter, position)
            if (openPos == -1) {
                fragments.add(TextFragment(template.substring(position)))
                break
            }

            if (openPos > position) {
                fragments.add(TextFragment(template.substring(position, openPos)))
            }

            val closePos = template.indexOf(closeDelimiter, openPos + openDelimiter.length)
            if (closePos == -1) {
                fragments.add(ErrorFragment(template.substring(position),
                    "Open delimiter without closing delimiter found at $openPos"))
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
}

interface Fragment {
    fun render() : String
}
interface CanStandAloneFragment: Fragment
abstract class EmptyFragment : Fragment {
    override fun render(): String = ""
}

class TextFragment(val text: String) : Fragment {
    override fun render(): String = text
}
class CommentFragment(val text: String) : EmptyFragment(), CanStandAloneFragment
class DelimiterChangeFragment(val openDelimiter: String, val closeDelimiter: String) : EmptyFragment(), CanStandAloneFragment {
    companion object {
        operator fun invoke(actionText: String): DelimiterChangeFragment {
            require(actionText.startsWith("=")) { "Delimiter set action must start with '='" }
            require(actionText.endsWith("=")) { "Delimiter set action must end with '='" }

            val newDelimiters = actionText.subSequence(1, actionText.length - 1).trim().split("\\s+".toRegex())
            require(newDelimiters.size == 2) { "Delimiter set action must specify exactly 2 delimiters" }
            return DelimiterChangeFragment(newDelimiters[0], newDelimiters[1])
        }
    }
}
class ErrorFragment(val text: String, val message: String) : EmptyFragment()
class ActionFragment : Fragment {
    override fun render(): String = ""
}

fun buildActionFragment(actionText: String): Fragment {
    return when {
        actionText.startsWith("!") -> CommentFragment(actionText)
        actionText.startsWith("=") -> DelimiterChangeFragment(actionText)
        else -> ActionFragment()
    }
}

class Template(val fragments: List<Fragment>) {
    fun render(data: Any? = null): String {
        return fragments.joinToString(separator = "") { it.render() }
    }
}

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

fun List<Fragment>.endsWithWhitespaceAfterNewline(index: Int): Boolean {
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
fun List<Fragment>.startsWithWhitespaceBeforeNewline(index: Int): Boolean {
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
fun MutableList<Fragment>.removeCharsAfterLastLinebreak(index: Int) {
    if (!indices.contains(index)) {
        return
    }
    val textContent = (this[index] as TextFragment).text
    val lastLinebreakIndex = textContent.lastIndexOfAny(charArrayOf('\r', '\n'))

    this[index] = TextFragment(textContent.substring(0, lastLinebreakIndex + 1))
}
fun MutableList<Fragment>.removeCharsUptoAndIncludingFirstLinebreak(index: Int) {
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
