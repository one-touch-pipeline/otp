/*
 * Copyright 2011-2021 The OTP authors
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

import java.util.regex.Pattern

class OtpPathValidator {
    static final String PATH_COMPONENT_REGEX = /[a-zA-Z0-9_\-\+\.]+/
    static final String PATH_CHARACTERS_REGEX = /[a-zA-Z0-9_\-\+\.\/]+/
    static final Pattern PATH_COMPONENT_PATTERN = Pattern.compile(/^${PATH_COMPONENT_REGEX}$/)
    static final Pattern RELATIVE_PATH_PATTERN = Pattern.compile(/^${PATH_COMPONENT_REGEX}(?:\/${PATH_COMPONENT_REGEX})*$/)
    static final Pattern ABSOLUTE_PATH_PATTERN = Pattern.compile(/^(?:\/${PATH_COMPONENT_REGEX})+$/)
    static final Pattern ILLEGAL_IN_NORMALIZED_PATH = Pattern.compile(/(?:^|\/)\.{1,2}(?:\/|$)/)

    static boolean isValidPathComponent(String string) {
        return PATH_COMPONENT_PATTERN.matcher(string).matches() && !ILLEGAL_IN_NORMALIZED_PATH.matcher(string).find()
    }

    static boolean isValidRelativePath(String string) {
        return RELATIVE_PATH_PATTERN.matcher(string).matches() && !ILLEGAL_IN_NORMALIZED_PATH.matcher(string).find()
    }

    static boolean isValidAbsolutePath(String string) {
        return ABSOLUTE_PATH_PATTERN.matcher(string).matches() && !ILLEGAL_IN_NORMALIZED_PATH.matcher(string).find()
    }

    static boolean isValidAbsolutePathContainingVariable(String string) {
        String pathComponentRegex = /[a-zA-Z0-9_\-\+\.\$\{\}]+/
        return Pattern.compile(/^(?:\/${pathComponentRegex})+$/).matcher(string).matches() &&
                !OtpPathValidator.ILLEGAL_IN_NORMALIZED_PATH.matcher(string).find()
    }
}
