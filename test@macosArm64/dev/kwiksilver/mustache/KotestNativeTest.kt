package dev.kwiksilver.mustache

import dev.kwiksilver.mustache.specs.MustacheSpecTests
import io.kotest.common.KotestInternal
import io.kotest.core.spec.SpecRef
import io.kotest.engine.launcher.invokeTestEngine
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

// TODO see if this workaround becomes superfluous with newer versions of Kotlin Toolchain or Kotest.
@OptIn(KotestInternal::class)
class KotestNativeTest {
    @Test
    fun runKotestSpecs() = runBlocking {
        invokeTestEngine(
            listOf(
                SpecRef.Function(::ParserTests, ParserTests::class),
                SpecRef.Function(::MustacheSpecTests, MustacheSpecTests::class),
            ),
            null,
        )
    }
}