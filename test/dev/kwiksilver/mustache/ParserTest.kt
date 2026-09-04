package dev.kwiksilver.mustache

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ParserTests : FunSpec({
    test("Find correct line start indices") {
        "bare line".findAllLineStartIndices() shouldBe listOf(0)
        "12345{{!\n  This is a\n  multi-line comment...\n}}67890\n".findAllLineStartIndices() shouldBe listOf(0, 9, 21, 45)
        "12345{{!\r  This is a\n  multi-line comment...\r\n}}67890\r\n".findAllLineStartIndices() shouldBe listOf(0, 9, 21, 46)
    }

    test("Convert offset to line and column") {
        Position.fromOffset(0, "first\nsecond") shouldBe Position(1, 1)
        Position.fromOffset(6, "first\nsecond") shouldBe Position(2, 1)
        Position.fromOffset(12, "first\nsecond") shouldBe Position(2, 7)
        Position.fromOffset(6, "first\r\nsecond") shouldBe Position(1, 7)
        Position.fromOffset(8, "first\r\nsecond") shouldBe Position(2, 2)
        Position.fromOffset(6, "first\n") shouldBe Position(2, 1)
    }

    test("Change delimiter followed by triple") {
        val fragments = parseTemplate("""Hello{{=<% %>=}}, (<%text%>){{{point}}}""").fragments
        fragments shouldBe arrayListOf(
            TextFragment("Hello", listOf(0), 0),
            DelimiterChangeFragment("<%", "%>", 5),
            TextFragment(", (", listOf(), 16),
            InterpolationFragment(listOf("text"), 19, true),
            TextFragment(")", listOf(), 27),
            InterpolationFragment(listOf("point"), 28, false)
        )

        val fragments2 = parseTemplate("""Hello{{=<% %>=}},{{{space}}}(<%text%>)!""").fragments
        fragments2 shouldBe arrayListOf(
            TextFragment("Hello", listOf(0), 0),
            DelimiterChangeFragment("<%", "%>", 5),
            TextFragment(",", listOf(), 16),
            InterpolationFragment(listOf("space"), 17, false),
            TextFragment("(", listOf(), 28),
            InterpolationFragment(listOf("text"), 29, true),
            TextFragment(")!", listOf(), 37)
        )
    }
})
