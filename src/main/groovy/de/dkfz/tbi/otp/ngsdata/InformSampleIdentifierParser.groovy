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

package de.dkfz.tbi.otp.ngsdata

import org.springframework.stereotype.Component

import java.util.regex.Matcher

@Component
class InformSampleIdentifierParser implements SampleIdentifierParser {

    static final String REGEX = createRegex()

    @Override
    DefaultParsedSampleIdentifier tryParse(String sampleIdentifier) {
        Matcher matcher = sampleIdentifier =~ REGEX
        if (matcher && checkSampleIdentifierConsistency(findSampleIdentifiersByPidAndTissueTypeKey(matcher.group('pid'), matcher.group('tissueTypeKey')))) {
            return new DefaultParsedSampleIdentifier(
                    'INFORM',
                    matcher.group('pid'),
                    buildSampleTypeDbName(matcher),
                    sampleIdentifier,
            )
        }
        return null
    }

    @Override
    boolean tryParsePid(String pid) {
        return pid =~ "^" + getPidRegex() + /$/
    }

    @Override
    String tryParseCellPosition(String sampleIdentifier) {
        return null
    }

    private Collection<SampleIdentifier> findSampleIdentifiersByPidAndTissueTypeKey(String pid, String tissueTypeKey) {
        Collection<SampleIdentifier> result = SampleIdentifier.createCriteria().list {
            sample {
                individual {
                    eq("pid", pid)
                }
            }
        }.findAll {
            Matcher matcher = it.name =~ REGEX
            return matcher.matches() && (matcher.group('tissueTypeKey') == tissueTypeKey)
        }
        return result
    }

    private boolean checkSampleIdentifierConsistency(Collection<SampleIdentifier> sampleIdentifiers) {
        return sampleIdentifiers.every {
            return buildSampleTypeDbName(it.name =~ REGEX) == it.sampleType.name
        }
    }

    private String buildSampleTypeDbName(Matcher matcher) {
        assert matcher.matches()
        String tissueType = "${InformTissueType.fromKey(matcher.group('tissueTypeKey'))}"

        if (matcher.group('sampleTypeNumber') != 'X') {
            tissueType += "0${matcher.group('sampleTypeNumber')}"
        }
        return tissueType
    }

    private static getPidRegex() {
        String treatingCenterId = "([0-9]{3})"
        String patientId = "([0-9]{3})"
        return "(I${treatingCenterId}_${patientId})"
    }

    private static String createRegex() {
        String sampleTypeNumber = "(?<sampleTypeNumber>([0-9X]))"
        String tissueTypeKey = "(?<tissueTypeKey>([TMCFL]))"
        String sampleId = "(${sampleTypeNumber}${tissueTypeKey}[0-9]_[DRPI][0-9])"
        return "^" +
                "(?<pid>${getPidRegex()})_" +
                "${sampleId}" +
                /$/
    }

}
