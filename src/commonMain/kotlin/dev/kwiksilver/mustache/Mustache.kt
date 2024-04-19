package dev.kwiksilver.mustache

import kotlinx.serialization.json.JsonElement

object Mustache {
    fun process(templateText: String, context: JsonElement? = null, partials: Map<String, String> = emptyMap()): String {
        val template = parseTemplate(templateText)
        val partialTemplates = partials.mapValues { (_, partialText) -> parseTemplate(partialText) }
        return template.render(Context(context), partialTemplates)
    }

}


