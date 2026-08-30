package dev.kwiksilver.mustache

import kotlinx.serialization.json.*


/**
 * The parsed [Template] contains of a list of [fragments] that can be rendered in sequence
 * to produce the output.
 */
class Template internal constructor(internal val fragments: List<Fragment>) {
    // TODO Consider removing known non-rendering fragments (like comment) from the list
    //      for further optimization, and keeping errors in a separate internal list.

    /**
     * Renders the [Template], using the data contained in the [context] to fill in the variable
     * parts of the template. If any partials are used in the template, those are taken by name
     * from the Map of [partials].
     */
    internal fun render(context: Context, partials: Map<String, Template>): String {
        return fragments.joinToString(separator = "") { it.render(context, partials) }
    }

    /**
     * Indicates whether there were any errors while building the [Template].
     * Templates with errors can still be rendered but are unlikely to produce the intended result.
     */
    fun hasErrors(): Boolean =
        fragments.find { it is ErrorFragment } != null
}


/**
 * A [Fragment] represents a specific part of a [Template]. It can be rendered.
 * There are both leaf variants that are rendered directly, as wel as composite fragments that
 * contain (multiple) sub-fragments.
 */
internal sealed interface Fragment {
    fun render(context: Context, partials: Map<String, Template>) : String
    val position: Int
}

/**
 * Implementing this interface indicates that a fragment type can be 'standalone'.
 * A fragment is considered standalone when the tag is the only thing on the line in the template source
 * is the mustache tag that defines the fragment.
 *
 * Standalone fragments should omit the entire line in the output, including any indenting whitespace and
 * the newline terminator if they don't produce output of themselves.
 */
internal interface CanStandAloneFragment: Fragment


/**
 * An empty fragment never produces any output.
 */
internal abstract class EmptyFragment(override val position: Int) : Fragment {
    override fun render(context: Context, partials: Map<String, Template>): String = ""
}

/**
 * A [TextFragment] represents a block of literal text from the template.
 *
 * When rendering, additional indentation may be added based on the context passed in. This is used
 * when rendering partials to indent the partial into the calling template based on the indentation
 * of the partial tag in the calling template.
 */
internal class TextFragment(val text: String, val lineStartPositions: List<Int>, override val position: Int) : Fragment {
    override fun render(context: Context, partials: Map<String, Template>): String {
        if (context.indentation.isEmpty() || lineStartPositions.isEmpty()) {
            return text
        }

        return buildString {
            var textPosition = 0
            lineStartPositions.forEach { lineStartPosition ->
                append(text.substring(textPosition, lineStartPosition))
                append(context.indentation)
                textPosition = lineStartPosition
            }
            append(text.substring(textPosition))
        }
    }
}

/**
 * A [CommentFragment] represents a comment section in the source template. It is not rendered.
 */
internal class CommentFragment(position: Int) : EmptyFragment(position), CanStandAloneFragment

/**
 * A [DelimiterChangeFragment] changes the delimiters used for parsing the remainder of the source template.
 */
internal class DelimiterChangeFragment(val openDelimiter: String, val closeDelimiter: String, position: Int) : EmptyFragment(position), CanStandAloneFragment {
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

/**
 * An [ErrorFragment] is generated when there is a parsing failure.
 * It carries an error message and an offset [position] in the template source where the error occurred.
 */
internal class ErrorFragment(val message: String, position: Int) : EmptyFragment(position)

/**
 * An [InterpolationFragment] represents a variable tag in the template. When rendering that tag is replaced
 * by the value in the supplied data pointed to by the [valuePath]. The [escapeHtml] flag tracks whether
 * html-relevant characters in the inserted value should be escaped during rendering.
 */
internal class InterpolationFragment(private val valuePath: ValuePath, override val position: Int, private val escapeHtml: Boolean = false) : Fragment {
    override fun render(context: Context, partials: Map<String, Template>): String = context.resolvePath(valuePath).renderValue()

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

/**
 * Indicates the start of a section.
 * The [valuePath] represents the key path in the context-data for rendering the section.
 */
internal class SectionStartFragment(val valuePath: ValuePath, position: Int) : EmptyFragment(position), CanStandAloneFragment

/**
 * Indicates the end of a section.
 * The [valuePath] represents the key path in the context-data for rendering the section.
 */
internal class SectionEndFragment(val valuePath: ValuePath, position: Int) : EmptyFragment(position), CanStandAloneFragment

/**
 * Represents the [contents] of a section. The [contents] of the section are representented as a list of [Fragment]s.
 * The [valuePath] represents the key path in the context-data for rendering the section.
 */
internal class SectionFragment internal constructor(private val valuePath: ValuePath, private val contents: List<Fragment>, override val position: Int) : Fragment {
    override fun render(context: Context, partials: Map<String, Template>): String {
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
                append(contents.joinToString(separator = "") { it.render(nestedContext, partials) })
            }
        }
    }
}

/**
 * Indicates the start of a section.
 * The [valuePath] represents the key path in the context-data for rendering the section.
 */
internal class InvertedSectionStartFragment(val valuePath: ValuePath, position: Int) : EmptyFragment(position), CanStandAloneFragment

/**
 * Represents the [contents] of an inverted section. The [contents] of the section are representented as a list of [Fragment]s.
 * The [valuePath] represents the key path in the context-data for rendering the inverted section.
 */
internal class InvertedSectionFragment internal constructor(private val valuePath: ValuePath, private val contents: List<Fragment>, override val position: Int) : Fragment {
    override fun render(context: Context, partials: Map<String, Template>): String {
        val targetValue = context.resolvePath(valuePath)

        if (isTruthy(targetValue)) {
            return ""
        }

        return contents.joinToString(separator = "") { it.render(context, partials) }
    }
}

/**
 *  Represents a Partial, basically an include of another template. The included template is specified by [name] and
 *  when rendered the included template will be additionally indented by the [indentation] of this tag in the source
 *  template.
 */
internal class PartialFragment(val name: String, private val indentation: String, override val position: Int) : CanStandAloneFragment {
    override fun render(context: Context, partials: Map<String, Template>): String =
        partials[name]?.render(context.withAddedIndentation(indentation), partials) ?: ""
}

/**
 * Decides whether a context [targetValue] evaluates to true for the purposes of including or omitting a [SectionFragment]
 * or an [InvertedSectionFragment] in the rendered output.
 */
private fun isTruthy(targetValue: JsonElement) = !(
        targetValue == JsonPrimitive(false) ||
        targetValue == JsonNull ||
        targetValue is JsonArray && targetValue.size == 0)
