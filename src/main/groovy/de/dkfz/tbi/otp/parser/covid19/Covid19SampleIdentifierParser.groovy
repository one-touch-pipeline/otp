/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.parser.covid19

import groovy.transform.CompileDynamic
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.parser.DefaultParsedSampleIdentifier
import de.dkfz.tbi.otp.parser.SampleIdentifierParser

import java.util.regex.Matcher

@CompileDynamic
@Component
class Covid19SampleIdentifierParser implements SampleIdentifierParser {

    static final String REGEX = createRegex()

    @Override
    DefaultParsedSampleIdentifier tryParse(String sampleIdentifier) {
        Matcher matcher = sampleIdentifier =~ REGEX
        if (matcher) {
            return new DefaultParsedSampleIdentifier(
                    Covid19SeqTypeProjectMapper."${matcher.group('seqType')}".projectName,
                    matcher.group('projectAndCenter') + matcher.group('lifeStatus'),
                    buildSampleTypeDbName(matcher),
                    sampleIdentifier,
                    SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT,
            )
        }
        return null
    }

    @Override
    boolean tryParsePid(String pid) {
        return pid =~ "^" + pidRegex + /$/
    }

    @Override
    String tryParseSingleCellWellLabel(String sampleIdentifier) {
        return null
    }

    private static String buildSampleTypeDbName(Matcher matcher) {
        assert matcher.matches()

        return "${Covid19TissueType.fromKey(matcher.group('tissueType'))}" +
                "${matcher.group('orderNumber').length() == 2 ? '0' : ''}" +
                "${matcher.group('orderNumber')}"
    }

    private static String getPidRegex() {
        String project = "SC2"
        String center = "([1-2])"
        String child = "([0-4])"
        String adult = "([5-9])"
        String id = "[0-9]{3}"
        String seqType = "(?<seqType>((${Covid19SeqTypeProjectMapper.values().join(')|(')})))"
        return "(?<projectAndCenter>${project}-${center})-{0,1}(?<lifeStatus>(${child}|${adult})${id}${seqType})"
    }

    private static String createRegex() {
        String sampleType = "(?<tissueType>((${Covid19TissueType.values()*.key.join(')|(')})))-?(?<orderNumber>([0-9]{2,3}))"
        return "^${pidRegex}-${sampleType}\$"
    }

}
