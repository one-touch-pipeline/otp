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

package de.dkfz.tbi.otp.hipo

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.DefaultParsedSampleIdentifier
import de.dkfz.tbi.otp.ngsdata.SampleIdentifierParser

import java.util.regex.Matcher

@Component
class Hipo2SampleIdentifierParser implements SampleIdentifierParser {

    private final static String PID = "(?<pid>(?<project>[A-Z][0-9]{2}[A-Z0-9])-[A-Z0-9]{4}([A-Z0-9]{2})?)"
    private final static String TISSUE = "(?<tissueType>[${HipoTissueType.values()*.key.join('')}])(?<tissueNumber>[0-9]{1,2})"
    private final static String ANALYTE = "(?<analyte>[DRPAWYTBMSE][0-9]|[0-9]*[CGHLJ][0-9]{1,2})"

    static String REGEX = /^${PID}-${TISSUE}-${ANALYTE}$/

    @Override
    boolean tryParsePid(String pid) {
        return pid =~ "^" + PID + /$/
    }

    @Override
    DefaultParsedSampleIdentifier tryParse(String sampleIdentifier) {
        Matcher matcher = sampleIdentifier =~ REGEX
        if (matcher.matches()) {
            String projectNumber = matcher.group('project')

            String sampleTypeDbName = "${HipoTissueType.fromKey(matcher.group('tissueType'))}${matcher.group('tissueNumber')}"
            String analyte = matcher.group('analyte')
            if (!'DRPAWYEMT'.toCharArray().contains(analyte.charAt(0))) {
                sampleTypeDbName += "-${analyte}"
            }

            return new DefaultParsedSampleIdentifier(
                    "hipo_${projectNumber}",
                    matcher.group('pid'),
                    sampleTypeDbName,
                    sampleIdentifier,
            )
        }
        return null
    }

    @Override
    String tryParseCellPosition(String sampleIdentifier) {
        Matcher matcher = sampleIdentifier =~ /^.*-${ANALYTE}$/
        if (matcher.matches()) {
            return matcher.group('analyte')
        }
        return null
    }
}
