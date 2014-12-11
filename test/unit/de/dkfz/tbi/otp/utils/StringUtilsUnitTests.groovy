package de.dkfz.tbi.otp.utils

import static de.dkfz.tbi.otp.utils.StringUtils.commonPrefixLength

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
}
