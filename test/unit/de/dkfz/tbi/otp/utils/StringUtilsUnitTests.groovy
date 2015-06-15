package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.TestCase

import static de.dkfz.tbi.otp.utils.StringUtils.commonPrefixLength
import static de.dkfz.tbi.otp.utils.StringUtils.longestCommonPrefix

import org.junit.Test

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



    void testLongestCommonPrefix_FirstStringIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            longestCommonPrefix(null, "second")
        }
    }

    void testLongestCommonPrefix_SecondStringIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            longestCommonPrefix("first", null)
        }
    }

    void testLongestCommonPrefix_FirstStringIsEmpty_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            longestCommonPrefix("", "second")
        }
    }

    void testLongestCommonPrefix_SecondStringIsEmpty_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            longestCommonPrefix("first", "")
        }
    }

    void testLongestCommonPrefix_FirstStringIsEqualSecondString() {
        assert "equal" == longestCommonPrefix("equal", "equal")
    }

    void testLongestCommonPrefix_FirstStringIsSubstring() {
        assert "String" == longestCommonPrefix("String", "StringExtended")
    }

    void testLongestCommonPrefix_SecondStringIsSubstring() {
        assert "String" == longestCommonPrefix("StringExtended", "String")
    }

    void testLongestCommonPrefix_DifferentStrings() {
        assert "" == longestCommonPrefix("OneString", "AnotherString")
    }

}
