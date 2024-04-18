package dev.kwiksilver.mustache

import kotlinx.serialization.json.*


class Template(val fragments: List<Fragment>) {
    fun render(context: Context): String {
        return fragments.joinToString(separator = "") { it.render(context) }
    }
}


sealed interface Fragment {
    fun render(context: Context) : String
    val position: Int
}

interface CanStandAloneFragment: Fragment


abstract class EmptyFragment(override val position: Int) : Fragment {
    override fun render(context: Context): String = ""
}

class TextFragment(val text: String, override val position: Int) : Fragment {
    override fun render(context: Context): String = text
}

class CommentFragment(position: Int) : EmptyFragment(position), CanStandAloneFragment

class DelimiterChangeFragment(val openDelimiter: String, val closeDelimiter: String, position: Int) : EmptyFragment(position), CanStandAloneFragment {
    companion object {
        operator fun invoke(actionText: String, position: Int): DelimiterChangeFragment {
            require(actionText.startsWith('=')) { "Delimiter set action must start with '='" }
            require(actionText.endsWith('=')) { "Delimiter set action must end with '='" }

            val newDelimiters = actionText.subSequence(1, actionText.length - 1).trim().split("\\s+".toRegex())
            require(newDelimiters.size == 2) { "Delimiter set action must specify exactly 2 delimiters" }
            return DelimiterChangeFragment(newDelimiters[0], newDelimiters[1], position)
        }
    }
}

class ErrorFragment(val message: String, position: Int) : EmptyFragment(position)

class InterpolationFragment(private val valuePath: ValuePath, override val position: Int, private val escapeHtml: Boolean = false) : Fragment {
    override fun render(context: Context): String = context.resolvePath(valuePath).renderValue()

    private fun JsonElement?.renderValue(): String = when (this) {
        is JsonNull -> ""
        is JsonPrimitive -> content.escapeValue()
        else -> ""
    }

    private fun String.escapeValue(): String = if (escapeHtml) {
        this.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    } else {
        this
    }
}

class SectionStartFragment(val valuePath: ValuePath, position: Int) : EmptyFragment(position), CanStandAloneFragment
class SectionEndFragment(val valuePath: ValuePath, position: Int) : EmptyFragment(position), CanStandAloneFragment
class SectionFragment(private val valuePath: ValuePath, private val contents: List<Fragment>, override val position: Int) : CanStandAloneFragment {
    override fun render(context: Context): String {
        val targetValue = context.resolvePath(valuePath)

        if (!isTruthy(targetValue)) {
            return ""
        }

        val targetArray = if (targetValue is JsonArray) {
            targetValue
        } else {
            JsonArray(listOf(targetValue))
        }

        return buildString {
            for (targetElement in targetArray) {
                val nestedContext = Context(targetElement, context)
                append(contents.joinToString(separator = "") { it.render(nestedContext) })
            }
        }
    }
}

class InvertedSectionStartFragment(val valuePath: ValuePath, position: Int) : EmptyFragment(position), CanStandAloneFragment
class InvertedSectionFragment(private val valuePath: ValuePath, private val contents: List<Fragment>, override val position: Int) : CanStandAloneFragment {
    override fun render(context: Context): String {
        val targetValue = context.resolvePath(valuePath)

        if (isTruthy(targetValue)) {
            return ""
        }

        return contents.joinToString(separator = "") { it.render(context) }
    }
}

private fun isTruthy(targetValue: JsonElement) = !(
        targetValue == JsonPrimitive(false) ||
        targetValue == JsonNull ||
        targetValue is JsonArray && targetValue.size == 0)
