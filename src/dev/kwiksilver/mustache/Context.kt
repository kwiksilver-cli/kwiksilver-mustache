package dev.kwiksilver.mustache

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * The [Context] contains the [current] data for rendering templates as a json structure that can be indexed into
 * by a [ValuePath].
 *
 * Contexts can be nested with an optional [parent] context. If a path cannot be resolved in the [current] context
 * then resolution will fall back to the [parent] if present. Sub-contexts with a parent are created when rendering
 * a [SectionFragment].
 *
 * Additional information carried is any additional indentation that is applied when rendering a [PartialFragment].
 */
internal class Context private constructor(
    private val parent: Context?,
    private val current: JsonElement,
    val indentation: String,
) {
    constructor(contextValue: JsonElement?, parent: Context? = null) : this(
        parent,
        contextValue ?: JsonNull,
        parent?.indentation ?: "",
    )

    /**
     * Resolves the [path] in the [current] [Context] data, with fallback to the [parent] [Context] if it isn't
     * found in the current [Context].
     */
    fun resolvePath(path: ValuePath): JsonElement {
        if (path.isEmpty()) {
            return current
        }

        return current.resolvePath(path) ?: parent?.resolvePath(path) ?: JsonNull
    }

    /**
     * Resolve the [path] in the [JsonElement] with an optional default [notFoundValue].
     */
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

    /**
     * Copies the current context with increased [indentation].
     */
    fun withAddedIndentation(indentation: String): Context {
        return Context(parent, current, this.indentation + indentation)
    }

}

/**
 * A path in the Json data structure represented as a list of [String]s.
 */
internal typealias ValuePath = List<String>

