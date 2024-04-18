package dev.kwiksilver.mustache

import kotlinx.serialization.json.JsonElement

object Mustache {
    fun process(templateText: String, context: JsonElement? = null): String {
        val template = parseTemplate(templateText)
        return template.render(Context(context))
    }

}


