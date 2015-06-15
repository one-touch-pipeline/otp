package de.dkfz.tbi.otp.utils

class StringUtils {

    /**
     * @return The length of the longest prefix that two strings have in common.
     */
    static int commonPrefixLength(final String s1, final String s2) {
        final int minLength = Math.min(s1.length(), s2.length());
        for (int i = 0; i < minLength; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return i
            }
        }
        return minLength
    }

     static String longestCommonPrefix(String a, String b) {
        assert a : "input String a is not allowed to be null or empty"
        assert b : "input String b is not allowed to be null or empty"
        return a.substring(0, commonPrefixLength(a, b));
    }
}
