package dev.kwiksilver.mustache

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive


class Template(val fragments: List<Fragment>) {
    fun render(context: JsonElement? = null): String {
        return fragments.joinToString(separator = "") { it.render(context) }
    }
}


interface Fragment {
    fun render(context: JsonElement?) : String
}

interface CanStandAloneFragment: Fragment


abstract class EmptyFragment : Fragment {
    override fun render(context: JsonElement?): String = ""
}

class TextFragment(val text: String) : Fragment {
    override fun render(context: JsonElement?): String = text
}

class CommentFragment : EmptyFragment(), CanStandAloneFragment

class DelimiterChangeFragment(val openDelimiter: String, val closeDelimiter: String) : EmptyFragment(), CanStandAloneFragment {
    companion object {
        operator fun invoke(actionText: String): DelimiterChangeFragment {
            require(actionText.startsWith('=')) { "Delimiter set action must start with '='" }
            require(actionText.endsWith('=')) { "Delimiter set action must end with '='" }

            val newDelimiters = actionText.subSequence(1, actionText.length - 1).trim().split("\\s+".toRegex())
            require(newDelimiters.size == 2) { "Delimiter set action must specify exactly 2 delimiters" }
            return DelimiterChangeFragment(newDelimiters[0], newDelimiters[1])
        }
    }
}

class ErrorFragment(val message: String) : EmptyFragment()

class InterpolationFragment(text: String, private val escapeHtml: Boolean = false) : Fragment {
    private val valuePath = text.trim().split('.').filter { it.isNotEmpty() } + "."

    override fun render(context: JsonElement?): String = resolve(valuePath, context)

    private fun resolve(path: List<String>, context: JsonElement?): String {
        if (path[0] == ".") {
            return if (context is JsonPrimitive && context !is JsonNull) {
                    escapeValue(context.content)
            } else {
                ""
            }
        }
        if (context is JsonObject) {
            val nextContext = context[path[0]]
            return resolve(path.drop(1), nextContext)
        }
        return ""
    }

    private fun escapeValue(value: String): String = if (escapeHtml) {
        value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    } else {
        value
    }
}

fun buildActionFragment(actionText: String): Fragment {
    return when {
        actionText.startsWith('!') -> CommentFragment()
        actionText.startsWith('=') -> DelimiterChangeFragment(actionText)
        actionText.startsWith('&') -> InterpolationFragment(actionText.substring(1))
        else -> InterpolationFragment(actionText, escapeHtml = true)
    }
}

