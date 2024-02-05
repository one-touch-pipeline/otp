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
package de.dkfz.tbi.otp.parser

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SampleTypePerProject

import java.util.regex.Matcher

@Component
class SimpleProjectIndividualSampleTypeParser implements SampleIdentifierParser {

    @Override
    DefaultParsedSampleIdentifier tryParse(String sampleIdentifier) {
        Matcher matcher = sampleIdentifier =~ createRegex()
        if (!matcher) {
            return null
        }

        String sampleType = matcher.group('sampleType')

        return new DefaultParsedSampleIdentifier(
                matcher.group('project'),
                matcher.group('pid'),
                sampleType,
                matcher.group('displayedSampleIdentifier'),
                getSpecificReferenceGenomeFromSampleType(sampleType),
                matcher.group('sampleTypeCategory') as SampleTypePerProject.Category,
        )
    }

    @Override
    boolean tryParsePid(String pid) {
        return pid =~ "^" + pidRegex + /$/
    }

    @Override
    String tryParseSingleCellWellLabel(String sampleIdentifier) {
        return null
    }

    protected static String createRegex() {
        return "^" +
                "\\[(?<project>${projectRegex})\\]" +
                "\\[(?<pid>${pidRegex})\\]" +
                "\\[(?<sampleType>${sampleType})\\]" +
                "\\[(?<displayedSampleIdentifier>${displayedSampleIdentifier})\\]" +
                "(\\[(?<sampleTypeCategory>${displayedSampleTypeCategory})\\])?" +
                /$/
    }

    private static String getProjectRegex() {
        return "[+A-Za-z0-9_-]+"
    }

    private static String getPidRegex() {
        return "[+A-Za-z0-9_-]+"
    }

    private static String getSampleType() {
        return "[+A-Za-z0-9-]+"
    }

    private static String getDisplayedSampleTypeCategory() {
        return "(UNDEFINED|DISEASE|CONTROL|IGNORED)?"
    }

    private static SampleType.SpecificReferenceGenome getSpecificReferenceGenomeFromSampleType(String sampleType) {
        if (sampleType.toLowerCase().endsWith("-x")) {
            return SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
        }
        return SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
    }

    private static String getDisplayedSampleIdentifier() {
        return "[^\\[\\]]+"
    }
}
