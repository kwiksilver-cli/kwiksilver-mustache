package dev.kwiksilver

import dev.kwiksilver.mustache.Context
import dev.kwiksilver.mustache.Template
import dev.kwiksilver.mustache.generateErrors
import dev.kwiksilver.mustache.parseTemplate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

/**
 * This is the main interface used to access the Mustache library.
 * Users should use the methods in this object and should not need to worry about the details
 * of other classes that implement the library internals.
 *
 * The [process] methods are concerned with processing and rendering templates fast. If errors
 * exist in the template it will still be rendered to the extent possible and no failures will
 * be reported.
 *
 * Likewise the [parse] method limits error reporting to providing a single method on the returned
 * [Template] to check whether there were any errors.
 *
 * For a full error report, use the [parseWithErrorReport] method which performs extra work to provide
 * a report with the locations and descriptions of any errors in the template.
 *
 * If you bundle templates in your app or library, it is recommended to add unit tests for parsing
 * and rendering those templates. If templates are supplied from outside, it is recommended to process
 * them with [parse] and only fall back to [parseWithErrorReport] to report errors.
 *
 * Some templates can make use of `partials` which are sub-templates that are referenced by name to
 * include them in the main template.
 */
object Mustache {
    /**
     * Processes the given template text and renders it using the optional Json [data] and [partials].
     *
     * Because both the [templateText] and [partials] are supplied as text it is simple to use but
     * requires reparsing the templates for every request.
     */
    fun process(templateText: String, data: JsonElement? = null, partials: Map<String, String> = emptyMap()): String {
        val template = parseTemplate(templateText)
        val partialTemplates = partials.mapValues { (_, partialText) -> parseTemplate(partialText) }
        return template.render(Context(data), partialTemplates)
    }

    /**
     * Processes the given template text and renders it using the optional serializable [data] object and [partials].
     * The [data] object can be any [Serializable] object.
     *
     * Because both the [templateText] and [partials] are supplied as text it is simple to use but
     * requires reparsing the templates for every request.
     */
    fun process(
        templateText: String,
        data: @Serializable Any? = null,
        partials: Map<String, String> = emptyMap()
    ): String {
        val template = parseTemplate(templateText)
        val partialTemplates = partials.mapValues { (_, partialText) -> parseTemplate(partialText) }
        val jsonData = Json.encodeToJsonElement(data)
        return template.render(Context(jsonData), partialTemplates)
    }

    /**
     * Processes the given template text and renders it using the optional Json [data] and [partials].
     */
    fun process(template: Template, data: JsonElement? = null, partials: Map<String, Template> = emptyMap()): String {
        return template.render(Context(data), partials)
    }

    /**
     * Processes the given template text and renders it using the optional serializable [data] object and [partials].
     * The [data] object can be any [Serializable] object.
     */
    fun process(
        template: Template,
        data: @Serializable Any? = null,
        partials: Map<String, Template> = emptyMap()
    ): String {
        val jsonData = Json.encodeToJsonElement(data)
        return template.render(Context(jsonData), partials)
    }

    /**
     * Parse the [templateText] into a [Template] that can be rendered quickly.
     * While the resulting [Template] can indicate whether there were any errors during the parsing,
     * for detailed error reports the [parseWithErrorReport] function should be used.
     */
    fun parse(templateText: String): Template = parseTemplate(templateText)

    /**
     * Parse the [templateText] into a [Template] and a [List] of errors. The errors will indicate the location
     * and nature of any problems that occurred during parsing.
     */
    fun parseWithErrorReport(templateText: String): Pair<Template, List<String>> {
        val template = parseTemplate(templateText)
        return Pair(template, template.generateErrors(templateText))
    }
}
