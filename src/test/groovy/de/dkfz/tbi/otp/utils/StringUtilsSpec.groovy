/*
 * Copyright 2011-2019 The OTP authors
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

class StringUtilsSpec extends Specification {

    void 'extractDistinguishingCharacter, when only one string is provided, throws IllegalArgumentException'() {
        when:
        StringUtils.extractDistinguishingCharacter(['abc'])

        then:
        thrown(IllegalArgumentException)
    }

    void 'extractDistinguishingCharacter, when strings have different lengths, returns null'() {
        expect:
        StringUtils.extractDistinguishingCharacter(['abc', 'abbc']) == null
    }

    void 'extractDistinguishingCharacter, when strings are empty, returns null'() {
        expect:
        StringUtils.extractDistinguishingCharacter(['', '']) == null
    }

    void 'extractDistinguishingCharacter, when more characters are different, returns null'(List<String> strings) {
        expect:
        StringUtils.extractDistinguishingCharacter(strings) == null

        where:
        strings | _
        ['abc', 'ade'] | _
        ['abd', 'acd', 'ace'] | _
    }

    void 'extractDistinguishingCharacter, when strings are equal, returns null'(List<String> strings) {
        expect:
        StringUtils.extractDistinguishingCharacter(strings) == null

        where:
        strings | _
        ['abc', 'abc'] | _
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
        s     || expected
        ''    || null
        null  || null
        'foo' || 'foo'
        ' '   || ' '
        '\t'  || '\t'
        '\n'  || '\n'
        'OTP is awesome, even with trailing spaces   ' ||  'OTP is awesome, even with trailing spaces   '
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
}
