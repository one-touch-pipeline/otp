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
package de.dkfz.tbi.otp.parser.hipo

import de.dkfz.tbi.otp.parser.DefaultParsedSampleIdentifier
import de.dkfz.tbi.otp.parser.SampleIdentifierParser

import java.text.NumberFormat
import java.util.regex.Matcher

abstract class AbstractHipo2SampleIdentifierParser implements SampleIdentifierParser {

    private final static String PID = "(?<pid>(?<project>[A-Z0-9]{4})-[A-Z0-9]{4}([A-Z0-9]{2}([A-Z0-9]{2})?)?)"

    private final static String TISSUE = "(?<tissueType>[${HipoTissueType.values()*.key.join('')}])(?<tissueNumber>[0-9]{1,3})"

    // single cell
    private final static String ANALYTE_CHARS_SINGLE_CELL = 'GHJS'

    // chip seq
    private final static String ANALYTE_CHAR_CHIP_SEQ = 'C'

    // use only the digits, formatting to two letters
    private final static String ANALYTE_CHARS_OTHER = 'ABDELMPRTWY'

    private final static String ANALYTE_CHARS_NO_DIGIT_BEFORE = ANALYTE_CHARS_OTHER + ANALYTE_CHARS_SINGLE_CELL

    // no chip seq or single cell demultiplex
    // adapt HIPO2 Parser to parse char 'P' in SampleID and append '-p' suffix to the SampleType
    private final static String ANALYTE_PATTERN_NO_DIGIT_BEFORE =
            "(?<analyteChar>P?)(?<analyteCharOnlyNumber>[${ANALYTE_CHARS_NO_DIGIT_BEFORE}])(?<analyteDigit>[0-9]{1,2})"

    private final static String ANALYTE_PATTERN_SKIP_ANALYTE =
            "[0-9]*(?<analyteSkip>[${ANALYTE_CHAR_CHIP_SEQ}])[0-9]{1,2}"

    private final static String ANALYTE_PATTERN_SINGLE_CELL_DEMULTIPLEX =
            "[0-9]+(?<analyteSingleCellDemultiplex>[${ANALYTE_CHARS_SINGLE_CELL}])[0-9]{1,2}"

    // Info if the Library Prep Kit Version 8 is used
    private final static String V8 = "(-(?<v8>V8))?"

    private final static String ANALYTE_PATTERN =
            "(?<analyte>${ANALYTE_PATTERN_NO_DIGIT_BEFORE}|${ANALYTE_PATTERN_SKIP_ANALYTE}|${ANALYTE_PATTERN_SINGLE_CELL_DEMULTIPLEX})"

    static final String REGEX = /^${PID}-${TISSUE}-${ANALYTE_PATTERN}${V8}$/

    @Override
    boolean tryParsePid(String pid) {
        return pid =~ "^" + PID + /$/
    }

    @SuppressWarnings('ConstantAssertExpression')
    @Override
    DefaultParsedSampleIdentifier tryParse(String sampleIdentifier) {
        Matcher matcher = sampleIdentifier =~ REGEX
        if (matcher.matches()) {
            String projectName = createProjectName(matcher.group('project'))
            String tissueNumber = createTissueNumber(matcher.group('tissueNumber'))
            if (!projectName || !tissueNumber) {
                return null
            }

            HipoTissueType hipoTissueType = HipoTissueType.fromKey(matcher.group('tissueType'))
            String baseSampleTypeName = "${hipoTissueType}${tissueNumber}"
            String analyteCharOnlyNumber = matcher.group('analyteCharOnlyNumber')
            String analyteSkip = matcher.group('analyteSkip')
            String analyteSingleCellDemultiplex = matcher.group('analyteSingleCellDemultiplex')
            String analyteDigit = matcher.group('analyteDigit')
            String analyteChar = matcher.group('analyteChar')

            String realSampleTypeName
            if (analyteCharOnlyNumber && ANALYTE_CHARS_NO_DIGIT_BEFORE.contains(analyteCharOnlyNumber)) {
                NumberFormat numberFormat = NumberFormat.integerInstance
                numberFormat.minimumIntegerDigits = 2
                realSampleTypeName = "${baseSampleTypeName}-${numberFormat.format(analyteDigit.toLong())}"
                if (analyteChar && analyteCharOnlyNumber ==~ /[RD]/) {
                    realSampleTypeName += "-p"
                } else if (analyteChar) {
                    return null
                }
            } else if (analyteSkip && ANALYTE_CHAR_CHIP_SEQ.contains(analyteSkip) ||
                    analyteSingleCellDemultiplex && ANALYTE_CHARS_SINGLE_CELL.contains(analyteSingleCellDemultiplex)) {
                realSampleTypeName = baseSampleTypeName
            } else {
                assert false: 'may not occur'
            }
            if (matcher.group('v8')) {
                realSampleTypeName += '-v8'
            }
            return new DefaultParsedSampleIdentifier(
                    projectName,
                    matcher.group('pid'),
                    realSampleTypeName,
                    sampleIdentifier,
                    hipoTissueType.specificReferenceGenome,
                    null,
            )
        }
        return null
    }

    abstract String createProjectName(String projectNumber)

    String createTissueNumber(String tissueNumber) {
        return (tissueNumber ==~ /[0-9]{1,2}/) ? tissueNumber : null
    }

    @Override
    String tryParseSingleCellWellLabel(String sampleIdentifier) {
        Matcher matcher = sampleIdentifier =~ REGEX
        if (matcher.matches() && matcher.group('analyteSingleCellDemultiplex')) {
            return matcher.group('analyte')
        }
        return null
    }
}
