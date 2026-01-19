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
package de.dkfz.tbi.otp.utils.validation

import grails.util.Environment

import java.util.regex.Pattern

class OtpPathValidator {

    static boolean isWindows() {
        if (Environment.current == Environment.PRODUCTION) {
            return false
        }

        return System.getProperty("os.name").toLowerCase().contains("windows")
    }

    static final String PATH_COMPONENT_REGEX = /[a-zA-Z0-9_\-+.]+/
    static final String UNIX_SEPARATOR_REGEX = "/"
    static final String WINDOWS_SEPARATOR_REGEX = "\\\\"
    static final String UNIX_PATH_CHARACTERS_REGEX = /[a-zA-Z0-9_\-\+\.\/]+/
    static final String WINDOWS_PATH_CHARACTERS_REGEX = /[a-zA-Z0-9_\-\+\.\\:]+/
    static final String WINDOWS_DRIVE_REGEX = /[a-zA-Z]:/

    static final Pattern PATH_COMPONENT_PATTERN = Pattern.compile(/^${PATH_COMPONENT_REGEX}$/)

    private static String getSeparatorRegex() {
        return isWindows() ? WINDOWS_SEPARATOR_REGEX : UNIX_SEPARATOR_REGEX
    }

    static String getPathCharactersRegex() {
        return isWindows() ? WINDOWS_PATH_CHARACTERS_REGEX : UNIX_PATH_CHARACTERS_REGEX
    }

    private static boolean isValidPath(String string, String pathRegex) {
        String sepRegex = separatorRegex
        Pattern pattern = Pattern.compile(pathRegex)
        Pattern illegalPattern = Pattern.compile(/(?:^|${sepRegex})\.{1,2}(?:${sepRegex}|$)/)
        return pattern.matcher(string).matches() && !illegalPattern.matcher(string).find()
    }

    static boolean isValidPathComponent(String string) {
        String sepRegex = separatorRegex
        Pattern illegalPattern = Pattern.compile(/(?:^|${sepRegex})\.{1,2}(?:${sepRegex}|$)/)
        return PATH_COMPONENT_PATTERN.matcher(string).matches() && !illegalPattern.matcher(string).find()
    }

    static boolean isValidRelativePath(String string) {
        String sepRegex = separatorRegex
        return isValidPath(string, /^${PATH_COMPONENT_REGEX}(?:${sepRegex}${PATH_COMPONENT_REGEX})*$/)
    }

    static boolean isValidAbsolutePath(String string) {
        String sepRegex = separatorRegex
        if (isWindows()) {
            // Support both UNC paths (\path\to\file) and drive letter paths (C:\path\to\file)
            String uncPathRegex = /^${sepRegex}${PATH_COMPONENT_REGEX}(?:${sepRegex}${PATH_COMPONENT_REGEX})*$/
            String drivePathRegex = /^${WINDOWS_DRIVE_REGEX}${sepRegex}${PATH_COMPONENT_REGEX}(?:${sepRegex}${PATH_COMPONENT_REGEX})*$/
            String driveOnlyRegex = /^${WINDOWS_DRIVE_REGEX}${sepRegex}?$/
            return isValidPath(string, uncPathRegex) || isValidPath(string, drivePathRegex) || isValidPath(string, driveOnlyRegex)
        }
        return isValidPath(string, /^${sepRegex}${PATH_COMPONENT_REGEX}(?:${sepRegex}${PATH_COMPONENT_REGEX})*$/)
    }

    static boolean isValidAbsolutePathContainingVariable(String string) {
        String pathComponentRegex = /[a-zA-Z0-9_\-\+\.\$\{\}]+/
        String sepRegex = separatorRegex
        if (isWindows()) {
            // Support both UNC paths (\path\to\file) and drive letter paths (C:\path\to\file) with variables
            String uncPathRegex = /^${sepRegex}${pathComponentRegex}(?:${sepRegex}${pathComponentRegex})*$/
            String drivePathRegex = /^${WINDOWS_DRIVE_REGEX}${sepRegex}${pathComponentRegex}(?:${sepRegex}${pathComponentRegex})*$/
            String driveOnlyRegex = /^${WINDOWS_DRIVE_REGEX}${sepRegex}?$/
            return isValidPath(string, uncPathRegex) || isValidPath(string, drivePathRegex) || isValidPath(string, driveOnlyRegex)
        }
        return isValidPath(string, /^${sepRegex}${pathComponentRegex}(?:${sepRegex}${pathComponentRegex})*$/)
    }
}
