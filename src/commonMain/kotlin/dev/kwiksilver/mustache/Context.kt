package dev.kwiksilver.mustache

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

class Context private constructor(
    private val parent: Context?,
    private val current: JsonElement,
) {
    constructor(contextValue: JsonElement?, parent: Context? = null) : this(
        parent,
        contextValue ?: JsonNull,
    )

    fun resolvePath(path: ValuePath): JsonElement {
        if (path.isEmpty()) {
            return current
        }

        return current.resolvePath(path) ?: parent?.resolvePath(path) ?: JsonNull
    }

    private fun JsonElement.resolvePath(path: ValuePath, notFoundValue: JsonElement? = null): JsonElement? {
        if (path.isEmpty()) {
            return this
        }
        if (this is JsonObject) {
            val nextObject = this[path[0]]
            return nextObject?.resolvePath(path.drop(1), JsonNull) ?: notFoundValue
        }
        return notFoundValue
    }

}

typealias ValuePath = List<String>

