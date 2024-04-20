package dev.kwiksilver.mustache

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

object Mustache {
    fun process(templateText: String, data: JsonElement? = null, partials: Map<String, String> = emptyMap()): String {
        val template = parseTemplate(templateText)
        val partialTemplates = partials.mapValues { (_, partialText) -> parseTemplate(partialText) }
        return template.render(Context(data), partialTemplates)
    }

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

    fun process(template: Template, data: JsonElement? = null, partials: Map<String, Template> = emptyMap()): String {
        return template.render(Context(data), partials)
    }

    fun process(
        template: Template,
        data: @Serializable Any? = null,
        partials: Map<String, Template> = emptyMap()
    ): String {
        val jsonData = Json.encodeToJsonElement(data)
        return template.render(Context(jsonData), partials)
    }

    fun parse(templateText: String): Template = parseTemplate(templateText)
}


