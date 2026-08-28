package dev.kwiksilver.mustache

import dev.kwiksilver.Mustache
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val OPEN_DELIMITER_WITHOUT_CLOSE = "Open delimiter without closing delimiter"

private fun errorReport(templateText: String): List<String> =
    Mustache.parseWithErrorReport(templateText).second

private fun expectedError(line: Int, column: Int): String =
    "Position(line=$line, column=$column): $OPEN_DELIMITER_WITHOUT_CLOSE"

class ParseWithErrorReportTests : FunSpec({
    test("reports an unclosed default interpolation at the start") {
        errorReport("{{name") shouldBe listOf(expectedError(1, 1))
    }

    test("parsed template should indicate whether there are errors") {
        Mustache.parse("{{name").hasErrors() shouldBe true
        Mustache.parse("{{name}}").hasErrors() shouldBe false
    }

    test("reports an unclosed default interpolation after text") {
        errorReport("hello {{name") shouldBe listOf(expectedError(1, 7))
    }

    test("reports an unclosed default interpolation on the second line") {
        errorReport("first\n{{name") shouldBe listOf(expectedError(2, 1))
    }

    test("reports an unclosed default interpolation after a CRLF line break") {
        errorReport("first\r\n{{name") shouldBe listOf(expectedError(2, 1))
    }

    test("reports an unclosed triple interpolation at the start") {
        errorReport("{{{name") shouldBe listOf(expectedError(1, 1))
    }

    test("reports an unclosed triple interpolation after text") {
        errorReport("hello {{{name") shouldBe listOf(expectedError(1, 7))
    }

    test("reports a triple interpolation with an incomplete closing delimiter") {
        errorReport("{{{name}}") shouldBe listOf(expectedError(1, 1))
    }

    test("reports an unclosed custom delimiter after a delimiter change") {
        errorReport("{{=<% %>=}}<% name") shouldBe listOf(expectedError(1, 12))
    }

    test("reports a custom delimiter when the old default closing delimiter is present") {
        errorReport("{{=<% %>=}}<% name }}") shouldBe listOf(expectedError(1, 12))
    }

    test("reports a custom delimiter when a different closing delimiter is present") {
        errorReport("{{=<% %>=}}<% name%}") shouldBe listOf(expectedError(1, 12))
    }

    test("reports an incomplete custom closing delimiter") {
        errorReport("{{=<% %>=}}<% name%") shouldBe listOf(expectedError(1, 12))
    }

    test("reports an unclosed custom delimiter on the second line") {
        errorReport("{{=<% %>=}}\n<% name") shouldBe listOf(expectedError(2, 1))
    }

    test("reports an unclosed custom delimiter using a different delimiter pair") {
        errorReport("{{=[[ ]]=}}[[ name") shouldBe listOf(expectedError(1, 12))
    }

    test("reports an unclosed default delimiter after switching back to the default pair") {
        errorReport("{{=<% %>=}}<%={{ }}=%>{{name") shouldBe listOf(expectedError(1, 23))
    }

    test("reports an unclosed delimiter change tag") {
        errorReport("{{=<% %>=}") shouldBe listOf(expectedError(1, 1))
    }

    test("reports an unclosed delimiter after a closed section") {
        errorReport("{{#foo}}x{{/foo}} {{name") shouldBe listOf(expectedError(1, 19))
    }
})

class MalformedDelimiterChangeTests : FunSpec({
    // TODO consider whether throwing an exception is the right way to handle this problem.
    test("delimiter change missing trailing equals throws") {
        val exception = shouldThrow<IllegalArgumentException> {
            Mustache.parseWithErrorReport("{{=<% %>}}")
        }

        exception.message shouldBe "Delimiter set action must end with '='"
    }

    test("delimiter change with too many delimiters throws") {
        val exception = shouldThrow<IllegalArgumentException> {
            Mustache.parseWithErrorReport("{{=foo bar baz=}}")
        }

        exception.message shouldBe "Delimiter set action must specify exactly 2 delimiters"
    }
})