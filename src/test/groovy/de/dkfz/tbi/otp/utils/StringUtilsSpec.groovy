/*
 * Copyright 2011-2026 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.utils

import spock.lang.Specification
import spock.lang.Unroll

import static de.dkfz.tbi.otp.utils.StringUtils.commonPrefixLength
import static de.dkfz.tbi.otp.utils.StringUtils.longestCommonPrefix

class StringUtilsSpec extends Specification {

    void "encodeForUrl, encodes special characters for use in a URL query parameter"() {
        expect:
        StringUtils.encodeForUrl(input) == expected

        where:
        input            || expected
        "simple"         || "simple"
        "with space"     || "with+space"
        "with&ampersand" || "with%26ampersand"
        "with=equals"    || "with%3Dequals"
        "a/b?c#d"        || "a%2Fb%3Fc%23d"
        "100%"           || "100%25"
        "a+b"            || "a%2Bb"
        "<html>@dkfz"    || "%3Chtml%3E%40dkfz"
        'quote"\'apos'   || "quote%22%27apos"
        "tab\tnewline\n" || "tab%09newline%0A"
        "Müller"         || "M%C3%BCller"
        "smörgåsbord"    || "sm%C3%B6rg%C3%A5sbord"
        "emoji🧬seq"      || "emoji%F0%9F%A7%ACseq"
        "safe-_.*"       || "safe-_.*"
        "~tilde"         || "%7Etilde"
    }

    void "test commonPrefixLength with same length and no common prefix"() {
        expect:
        commonPrefixLength('ab', 'bc') == 0
    }

    void "test commonPrefixLength with same length and common prefix"() {
        expect:
        commonPrefixLength('ab', 'ac') == 1
    }

    void "test commonPrefixLength with same length and full match"() {
        expect:
        commonPrefixLength('ab', 'ab') == 2
    }

    void "test commonPrefixLength with different lengths and no common prefix"() {
        expect:
        commonPrefixLength('bc', 'abc') == 0
        commonPrefixLength('abc', 'bc') == 0
    }

    void "test commonPrefixLength with different lengths and common prefix"() {
        expect:
        commonPrefixLength('ac', 'abc') == 1
        commonPrefixLength('abc', 'ac') == 1
    }

    void "test commonPrefixLength with different lengths where one is prefix of the other"() {
        expect:
        commonPrefixLength('ab', 'abc') == 2
        commonPrefixLength('abc', 'ab') == 2
    }

    void "test longestCommonPrefix with first string null should fail"() {
        when:
        longestCommonPrefix(null, "second")

        then:
        thrown(AssertionError)
    }

    void "test longestCommonPrefix with second string null should fail"() {
        when:
        longestCommonPrefix("first", null)

        then:
        thrown(AssertionError)
    }

    void "test longestCommonPrefix with first string empty should fail"() {
        when:
        longestCommonPrefix("", "second")

        then:
        thrown(AssertionError)
    }

    void "test longestCommonPrefix with second string empty should fail"() {
        when:
        longestCommonPrefix("first", "")

        then:
        thrown(AssertionError)
    }

    void "test longestCommonPrefix when first string equals second string"() {
        expect:
        "equal" == longestCommonPrefix("equal", "equal")
    }

    void "test longestCommonPrefix when first string is substring"() {
        expect:
        "String" == longestCommonPrefix("String", "StringExtended")
    }

    void "test longestCommonPrefix when second string is substring"() {
        expect:
        "String" == longestCommonPrefix("StringExtended", "String")
    }

    void "test longestCommonPrefix with different strings"() {
        expect:
        "" == longestCommonPrefix("OneString", "AnotherString")
    }

    void 'extractDistinguishingCharacter, when only one string is provided, throws IllegalArgumentException'() {
        when:
        StringUtils.extractDistinguishingCharacter(['abc'])

        then:
        thrown(IllegalArgumentException)
    }

    void 'extractDistinguishingCharacter, when strings have different lengths, returns null'() {
        expect:
        StringUtils.extractDistinguishingCharacter(['abc', 'abbc']) == [:]
    }

    void 'extractDistinguishingCharacter, when strings are empty, returns null'() {
        expect:
        StringUtils.extractDistinguishingCharacter(['', '']) == [:]
    }

    void 'extractDistinguishingCharacter, when more characters are different, returns null'(List<String> strings) {
        expect:
        StringUtils.extractDistinguishingCharacter(strings) == [:]

        where:
        strings               | _
        ['abc', 'ade']        | _
        ['abd', 'acd', 'ace'] | _
    }

    void 'extractDistinguishingCharacter, when strings are equal, returns null'(List<String> strings) {
        expect:
        StringUtils.extractDistinguishingCharacter(strings) == [:]

        where:
        strings               | _
        ['abc', 'abc']        | _
        ['abd', 'acd', 'acd'] | _
        ['abd', 'acd', 'abd'] | _
    }

    void 'extractDistinguishingCharacter, when strings differ in exactly one character, returns correct mapping'(Map<String, Character> expectedMapping) {
        given:
        List<String> strings = expectedMapping.keySet().toList()

        expect:
        StringUtils.extractDistinguishingCharacter(strings) == expectedMapping

        where:
        expectedMapping                | _
        [abc: 'a', xbc: 'x']           | _
        [abc: 'b', ayc: 'y']           | _
        [abc: 'c', abz: 'z']           | _
        [abc: 'a', ubc: 'u', xbc: 'x'] | _
        [abc: 'b', avc: 'v', ayc: 'y'] | _
        [abc: 'c', abw: 'w', abz: 'z'] | _
    }

    void 'blankToNull, blanks ONLY empty string to null, otherwise keeps as-is'() {
        expect:
        StringUtils.blankToNull(s) == expected

        where:
        s                                              || expected
        ''                                             || null
        null                                           || null
        'foo'                                          || 'foo'
        ' '                                            || ' '
        '\t'                                           || '\t'
        '\n'                                           || '\n'
        'OTP is awesome, even with trailing spaces   ' || 'OTP is awesome, even with trailing spaces   '
    }

    void 'trimAndShortenWhitespace, trims leading and trailing whitespace, shortens whitespace in the middle'() {
        expect:
        StringUtils.trimAndShortenWhitespace(given) == expected

        where:
        given  || expected
        "x"    || "x"
        " x"   || "x"
        "x "   || "x"
        ""     || ""
        "  "   || ""
        "x  x" || "x x"
    }

    @Unroll
    void 'toCamelCase, basic test cases: #given'() {
        given:
        String result

        when: "not capitalized"
        result = StringUtils.toCamelCase(given)

        then:
        result == expected

        when: "capitalized"
        result = StringUtils.toCamelCase(given, true)

        then:
        result == expected.capitalize()

        where:
        given         || expected
        "nochange"    || "nochange"
        "ALLCAPS"     || "allcaps"
        "snake_case"  || "snakeCase"
        "CAPS_SNAKE"  || "capsSnake"
        "camelCase"   || "camelcase"
        "white space" || "white space"
    }

    @Unroll
    void 'toSnakeCase, basic test cases: #given'() {
        expect:
        expected == StringUtils.toSnakeCase(given)

        where:
        given         || expected
        "nochange"    || "nochange"
        "ALLCAPS"     || "a_l_l_c_a_p_s"
        "snake_case"  || "snake_case"
        "camelCase"   || "camel_case"
        "white space" || "white space"
    }

    @Unroll
    void 'joinAsSemanticString, test case: #input'() {
        expect:
        expected == StringUtils.joinAsSemanticString(input, ", ", " and ")

        where:
        input                   || expected
        []                      || ""
        [""]                    || ""
        ["one"]                 || "one"
        ["one", "two"]          || "one and two"
        ["one", "two", "three"] || "one, two and three"
    }

    void 'generateMultiLineDisplayName, test cases: #given'() {
        expect:
        expected == StringUtils.generateMultiLineDisplayName(given)

        where:
        given                                   || expected
        []                                      || ""
        ["one"]                                 || "one"
        ["one", "two"]                          || "one\ntwo"
        ["one", "two", "three", "four", "five"] || "one\ntwo\nthree\nfour\nfive"
    }
}
