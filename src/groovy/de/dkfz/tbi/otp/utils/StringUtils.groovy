package de.dkfz.tbi.otp.utils

class StringUtils {

    static String escapeForSqlLike(String string) {
        return string.replaceAll(/([\\_%])/, /\\$1/)
    }

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

    /**
     * From a list of strings which differ in exactly one character, returns a mapping of each of these strings to the
     * distinguishing character
     *
     * <p>
     * Example: <code>extractDistinguishingCharacter(['abcef', 'abdef']) == [abced: 'c', abdef: 'd']</code>
     */
    static Map<String, Character> extractDistinguishingCharacter(List<String> strings) {
        if (strings.size() < 2) {
            throw new IllegalArgumentException('At least two strings must be provided')
        }
        if (strings*.length().unique().size() != 1) {
            return null
        }
        int characterIndex = commonPrefixLength(strings[0], strings[1])
        if (characterIndex == strings[0].length()) {
            return null
        }
        String prefix = strings[0].substring(0, characterIndex)
        String suffix = strings[0].substring(characterIndex + 1)
        Map<String, Character> result = [:]
        for (String it : strings) {
            if (!it.startsWith(prefix) || !it.endsWith(suffix)) {
                return null
            }
            if (result.put(it, it.charAt(characterIndex))) {
                // At least two of the strings are equal
                return null
            }
        }
        return result
    }
}
