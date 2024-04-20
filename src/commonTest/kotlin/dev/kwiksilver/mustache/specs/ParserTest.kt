package dev.kwiksilver.mustache.specs

import dev.kwiksilver.mustache.findAllLineStartIndices
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ParserTests : FunSpec({
    test("Find correct line start indices") {
        "bare line".findAllLineStartIndices() shouldBe listOf(0)
        "12345{{!\n  This is a\n  multi-line comment...\n}}67890\n".findAllLineStartIndices() shouldBe listOf(0, 9, 21, 45)
        "12345{{!\r  This is a\n  multi-line comment...\r\n}}67890\r\n".findAllLineStartIndices() shouldBe listOf(0, 9, 21, 46)
    }
})