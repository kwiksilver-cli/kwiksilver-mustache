package dev.kwiksilver.mustache.specs

import dev.kwiksilver.mustache.Mustache
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.IsStableType
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer


@Serializable
@IsStableType
data class MustacheSpec(
    val name: String,
    val desc: String,
    val data: JsonElement,
    val partials: Map<String, String> = emptyMap(),
    val template: String,
    val expected: String,
) {
    override fun toString(): String = name
}

@Serializable
@IsStableType
data class MustacheSuite(
    val name: String = "",
    val overview: String,
    val tests: List<MustacheSpec>,
) {
    override fun toString(): String = name
}

val jsonParser = Json {
    ignoreUnknownKeys = true
}

class MustacheSpecTests : FunSpec({

    context("Mustache specification tests") {
        val specsDir = FileSystem.SYSTEM.canonicalize("mustache-specs".toPath())
        val testSuites = FileSystem.SYSTEM.list(specsDir)
            .filter { it.name.endsWith(".json") && !it.name.startsWith('~') }
            .map { specPath ->
                val specJson = FileSystem.SYSTEM.source(specPath).buffer().readUtf8()
                val suite = jsonParser.decodeFromString<MustacheSuite>(specJson)
                suite.copy(name = specPath.name.removeSuffix(".json"))
            }

        withData(testSuites) { testSuite ->
            withData(testSuite.tests) { specTest ->
                withClue(specTest.desc) {
                    Mustache.process(specTest.template, specTest.data, specTest.partials) shouldBe specTest.expected
                }
            }
        }
    }

})