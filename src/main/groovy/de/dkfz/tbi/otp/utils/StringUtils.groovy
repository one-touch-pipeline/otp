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

class StringUtils {

    static String escapeForSqlLike(String string) {
        return string.replaceAll(/([\\_%])/, /\\$1/)
    }

    /**
     * @return The length of the longest prefix that two strings have in common.
     */
    static int commonPrefixLength(final String s1, final String s2) {
        final int minLength = Math.min(s1.length(), s2.length())
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
        return a.substring(0, commonPrefixLength(a, b))
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

    /**
     * Downcast blank string to null for simpler handling of empty fields in the GUI
     *
     * @param string to downcast
     * @return the casted String
     */
    @SuppressWarnings("TernaryCouldBeElvis")
    static String blankToNull(String string) {
        return string ? string : null
    }

    /**
     * Trims leading and trailing whitespace and shortens contained whitespace to a single whitespace.
     *
     * @param string to transform
     * @return return the transformed string
     */
    static String trimAndShortenWhitespace(String string) {
        return string?.trim()?.replaceAll(" +", " ")
    }

    /**
     * Converts snake case String to camel case String
     *
     * Credit goes to 'pascal' who published this here: http://www.groovyconsole.appspot.com/script/337001
     * Code has been fine tuned.
     */
    static String toCamelCase(String text, boolean capitalized = false) {
        String camelCasedText = text.toLowerCase().replaceAll("(_)([A-Za-z0-9])", { List<String> it -> it[2].toUpperCase() })
        return capitalized ? camelCasedText.capitalize() : camelCasedText
    }

    /**
     * Converts camel case String to snake case String
     *
     * Credit goes to 'pascal' who published this here: http://www.groovyconsole.appspot.com/script/337001
     */
    static String toSnakeCase(String text) {
        return text.replaceAll(/([A-Z])/, /_$1/ ).toLowerCase().replaceAll( /^_/, '')
    }
}
