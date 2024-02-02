/*
 * Copyright 2011-2024 The OTP authors
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

import org.junit.Test

import de.dkfz.tbi.TestCase

import static de.dkfz.tbi.otp.utils.StringUtils.commonPrefixLength
import static de.dkfz.tbi.otp.utils.StringUtils.longestCommonPrefix

class StringUtilsUnitTests {

    @Test
    void testCommonPrefixLength_sameLength_noCommonPrefix() {
        assert commonPrefixLength('ab', 'bc') == 0
    }

    @Test
    void testCommonPrefixLength_sameLength_commonPrefix() {
        assert commonPrefixLength('ab', 'ac') == 1
    }

    @Test
    void testCommonPrefixLength_sameLength_fullMatch() {
        assert commonPrefixLength('ab', 'ab') == 2
    }

    @Test
    void testCommonPrefixLength_differentLengths_noCommonPrefix() {
        assert commonPrefixLength('bc', 'abc') == 0
        assert commonPrefixLength('abc', 'bc') == 0
    }

    @Test
    void testCommonPrefixLength_differentLengths_commonPrefix() {
        assert commonPrefixLength('ac', 'abc') == 1
        assert commonPrefixLength('abc', 'ac') == 1
    }

    @Test
    void testCommonPrefixLength_differentLengths_onePrefixOfOther() {
        assert commonPrefixLength('ab', 'abc') == 2
        assert commonPrefixLength('abc', 'ab') == 2
    }

    @Test
    void testLongestCommonPrefix_FirstStringIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            longestCommonPrefix(null, "second")
        }
    }

    @Test
    void testLongestCommonPrefix_SecondStringIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            longestCommonPrefix("first", null)
        }
    }

    @Test
    void testLongestCommonPrefix_FirstStringIsEmpty_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            longestCommonPrefix("", "second")
        }
    }

    @Test
    void testLongestCommonPrefix_SecondStringIsEmpty_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            longestCommonPrefix("first", "")
        }
    }

    @Test
    void testLongestCommonPrefix_FirstStringIsEqualSecondString() {
        assert "equal" == longestCommonPrefix("equal", "equal")
    }

    @Test
    void testLongestCommonPrefix_FirstStringIsSubstring() {
        assert "String" == longestCommonPrefix("String", "StringExtended")
    }

    @Test
    void testLongestCommonPrefix_SecondStringIsSubstring() {
        assert "String" == longestCommonPrefix("StringExtended", "String")
    }

    @Test
    void testLongestCommonPrefix_DifferentStrings() {
        assert "" == longestCommonPrefix("OneString", "AnotherString")
    }
}
