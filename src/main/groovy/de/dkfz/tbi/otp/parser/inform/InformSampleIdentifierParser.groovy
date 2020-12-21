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
package de.dkfz.tbi.otp.parser.inform

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.parser.DefaultParsedSampleIdentifier
import de.dkfz.tbi.otp.parser.SampleIdentifierParser

import java.util.regex.Matcher

@Component
class InformSampleIdentifierParser implements SampleIdentifierParser {

    static final String REGEX = createRegex()

    static final int PAD_LEFT = 2

    @Override
    DefaultParsedSampleIdentifier tryParse(String sampleIdentifier) {
        Matcher matcher = sampleIdentifier =~ REGEX
        if (matcher) {
            assert matcher.matches()
            return new DefaultParsedSampleIdentifier(
                    'INFORM1',
                    matcher.group('pid'),
                    buildSampleTypeDbName(matcher),
                    sampleIdentifier,
                    InformTissueType.fromKey(matcher.group('tissueTypeKey')).specificReferenceGenome,
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

    private String buildSampleTypeDbName(Matcher matcher) {
        List<String> tissueType = []
        tissueType << InformTissueType.fromKey(matcher.group('tissueTypeKey'))

        if (matcher.group('sampleTypeNumber').toLowerCase() == 'x') {
            tissueType << "0X"
        } else {
            tissueType << "${matcher.group('sampleTypeNumber')}".padLeft(PAD_LEFT, '0')
        }
        tissueType << matcher.group('sampleTypeOrderNumber')
        tissueType << '-'
        tissueType << matcher.group('orderNumber').padLeft(PAD_LEFT, '0')

        return tissueType.join('')
    }

    private static String getPidRegex() {
        String treatingCenterId = "([0-9]{3})"
        String patientId = "([0-9]{3})"
        return "(I${treatingCenterId}_${patientId})"
    }

    private static String createRegex() {
        String sampleTypeNumber = "(?<sampleTypeNumber>(([0-9]{1,2})|X))"
        String tissueTypeKey = "(?<tissueTypeKey>([${InformTissueType.values()*.key.join('')}]))"
        String sampleTypeOrderNumber = "(?<sampleTypeOrderNumber>([0-9]))"
        String orderNumber = "(?<orderNumber>([0-9]{1,2}))"

        String sampleName = "(${sampleTypeNumber}${tissueTypeKey}${sampleTypeOrderNumber}_[DRPI]${orderNumber})"
        return "^" +
                "(?<pid>${pidRegex})_" +
                "${sampleName}" +
                /$/
    }
}
