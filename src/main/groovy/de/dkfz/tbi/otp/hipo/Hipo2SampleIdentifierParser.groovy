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

import java.text.NumberFormat
import java.util.regex.Matcher

@Component
class Hipo2SampleIdentifierParser implements SampleIdentifierParser {

    private final static String PID = "(?<pid>(?<project>[A-Z][0-9]{2}[A-Z0-9])-[A-Z0-9]{4}([A-Z0-9]{2})?)"
    private final static String TISSUE = "(?<tissueType>[${HipoTissueType.values()*.key.join('')}])(?<tissueNumber>[0-9]{1,2})"

    //ignore analyte part
    private final static String ANALYTE_CHARS_SINGLE_CELL_IGNORE = 'GHJS'

    //ignore analyte part
    private final static String ANALYTE_CHAR_CHIP_SEQ = 'C'

    //use only the digits, formatting to two letters
    private final static String ANALYTE_CHARS_USE_ONLY_NUMBER = 'ABDELMPRTWY'


    private final static String ANALYTE_CHARS_ALLOW_DIGIT_BEFORE_CHAR =
            ANALYTE_CHARS_SINGLE_CELL_IGNORE + ANALYTE_CHAR_CHIP_SEQ

    private final static String ANALYTE_PATTERN_NO_DIGIT_BEFORE =
            "(?<analyteCharOnlyNumber>[${ANALYTE_CHARS_USE_ONLY_NUMBER}])(?<analyteDigit>[0-9]{1,2})"

    private final static String ANALYTE_PATTERN_ALLOW_DIGIT_BEFORE =
            "[0-9]*(?<analyteSkip>[${ANALYTE_CHARS_ALLOW_DIGIT_BEFORE_CHAR}])[0-9]{1,2}"

    private final static String ANALYTE_PATTERN =
            "(?<analyte>${ANALYTE_PATTERN_NO_DIGIT_BEFORE}|${ANALYTE_PATTERN_ALLOW_DIGIT_BEFORE})"

    static final String REGEX = /^${PID}-${TISSUE}-${ANALYTE_PATTERN}$/

    @Override
    boolean tryParsePid(String pid) {
        return pid =~ "^" + PID + /$/
    }

    @Override
    DefaultParsedSampleIdentifier tryParse(String sampleIdentifier) {
        Matcher matcher = sampleIdentifier =~ REGEX
        if (matcher.matches()) {
            String projectNumber = matcher.group('project')

            String baseSampleTypeName = "${HipoTissueType.fromKey(matcher.group('tissueType'))}${matcher.group('tissueNumber')}"
            String analyteCharOnlyNumber = matcher.group('analyteCharOnlyNumber')
            String analyteSkip = matcher.group('analyteSkip')
            String analyteDigit = matcher.group('analyteDigit')

            String realSampleTypeName = null
            if (analyteCharOnlyNumber && ANALYTE_CHARS_USE_ONLY_NUMBER.contains(analyteCharOnlyNumber)) {
                NumberFormat numberFormat = NumberFormat.getIntegerInstance()
                numberFormat.setMinimumIntegerDigits(2)
                realSampleTypeName = "${baseSampleTypeName}-${numberFormat.format(analyteDigit.toLong())}"
            } else if (analyteSkip && ANALYTE_CHARS_ALLOW_DIGIT_BEFORE_CHAR.contains(analyteSkip)) {
                realSampleTypeName = baseSampleTypeName
            } else {
                assert false: 'may not occur'
            }

            return new DefaultParsedSampleIdentifier(
                    "hipo_${projectNumber}",
                    matcher.group('pid'),
                    realSampleTypeName,
                    sampleIdentifier,
            )
        }
        return null
    }

    @Override
    String tryParseCellPosition(String sampleIdentifier) {
        Matcher matcher = sampleIdentifier =~ /^.*-${ANALYTE_PATTERN}$/
        if (matcher.matches()) {
            return matcher.group('analyte')
        }
        return null
    }
}
