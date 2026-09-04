package dev.kwiksilver.mustache

import dev.kwiksilver.Mustache
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private fun errorReport(templateText: String): List<String> =
    Mustache.parseWithErrorReport(templateText).second

private fun delimiterError(line: Int, column: Int): String =
    expectedError("Open delimiter without closing delimiter", line, column)

private fun expectedError(message: String, line: Int, column: Int): String =
    "line: $line, column: $column: $message"

class ParseWithErrorReportTests : FunSpec({
    test("reports an unclosed default interpolation at the start") {
        errorReport("{{name") shouldBe listOf(delimiterError(1, 1))
    }

    test("parsed template should indicate whether there are errors") {
        Mustache.parse("{{name").hasErrors() shouldBe true
        Mustache.parse("{{name}}").hasErrors() shouldBe false
    }

    test("reports an unclosed default interpolation after text") {
        errorReport("hello {{name") shouldBe listOf(delimiterError(1, 7))
    }

    test("reports an unclosed default interpolation on the second line") {
        errorReport("first\n{{name") shouldBe listOf(delimiterError(2, 1))
    }

    test("reports an unclosed default interpolation after a CRLF line break") {
        errorReport("first\r\n{{name") shouldBe listOf(delimiterError(2, 1))
    }

    test("reports an unclosed triple interpolation at the start") {
        errorReport("{{{name") shouldBe listOf(delimiterError(1, 1))
    }

    test("reports an unclosed triple interpolation after text") {
        errorReport("hello {{{name") shouldBe listOf(delimiterError(1, 7))
    }

    test("reports a triple interpolation with an incomplete closing delimiter") {
        errorReport("{{{name}}") shouldBe listOf(delimiterError(1, 1))
    }

    test("reports an unclosed custom delimiter after a delimiter change") {
        errorReport("{{=<% %>=}}<% name") shouldBe listOf(delimiterError(1, 12))
    }

    test("reports a custom delimiter when the old default closing delimiter is present") {
        errorReport("{{=<% %>=}}<% name }}") shouldBe listOf(delimiterError(1, 12))
    }

    test("reports a custom delimiter when a different closing delimiter is present") {
        errorReport("{{=<% %>=}}<% name%}") shouldBe listOf(delimiterError(1, 12))
    }

    test("reports an incomplete custom closing delimiter") {
        errorReport("{{=<% %>=}}<% name%") shouldBe listOf(delimiterError(1, 12))
    }

    test("reports an unclosed custom delimiter on the second line") {
        errorReport("{{=<% %>=}}\n<% name") shouldBe listOf(delimiterError(2, 1))
    }

    test("reports an unclosed custom delimiter using a different delimiter pair") {
        errorReport("{{=[[ ]]=}}[[ name") shouldBe listOf(delimiterError(1, 12))
    }

    test("reports an unclosed default delimiter after switching back to the default pair") {
        errorReport("{{=<% %>=}}<%={{ }}=%>{{name") shouldBe listOf(delimiterError(1, 23))
    }

    test("reports an unclosed delimiter change tag") {
        errorReport("{{=<% %>=}") shouldBe listOf(delimiterError(1, 1))
    }

    test("reports an unclosed delimiter after a closed section") {
        errorReport("{{#foo}}x{{/foo}} {{name") shouldBe listOf(delimiterError(1, 19))
    }

    test("delimiter change missing trailing equals throws") {
        errorReport("{{=<% %>}}") shouldBe listOf(
            expectedError("Delimiter set action must end with '='", 1, 1))
    }

    test("delimiter change with too many delimiters throws") {
        errorReport("{{=foo bar baz=}}") shouldBe listOf(
            expectedError("Delimiter set action must specify exactly 2 delimiters", 1, 1))
    }
})
