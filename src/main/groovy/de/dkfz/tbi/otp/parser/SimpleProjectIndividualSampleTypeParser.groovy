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

package de.dkfz.tbi.otp.parser

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.DefaultParsedSampleIdentifier
import de.dkfz.tbi.otp.ngsdata.SampleIdentifierParser

import java.util.regex.Matcher

@Component
class SimpleProjectIndividualSampleTypeParser implements SampleIdentifierParser {

    @Override
    DefaultParsedSampleIdentifier tryParse(String sampleIdentifier) {
        Matcher matcher = sampleIdentifier =~ createRegex()
        if (!matcher) {
            return null
        }

        return new DefaultParsedSampleIdentifier(
                matcher.group('project'),
                matcher.group('pid'),
                matcher.group('sampleType'),
                sampleIdentifier,
        )
    }

    @Override
    boolean tryParsePid(String pid) {
        return pid =~ "^" + getPidRegex() + /$/
    }

    @Override
    String tryParseCellPosition(String sampleIdentifier) {
        return null
    }

    protected static String createRegex() {
        return "^" +
                "\\((?<project>${getProjectRegex()})\\)" +
                "\\((?<pid>${getPidRegex()})\\)" +
                "\\((?<sampleType>${getSampleType()})\\)" +
                /$/
    }

    private static String getProjectRegex() {
        return "[A-Za-z0-9_-]+"
    }

    private static String getPidRegex() {
        return "[A-Za-z0-9_-]+"
    }

    private static String getSampleType() {
        return "[A-Za-z0-9-]+"
    }
}
