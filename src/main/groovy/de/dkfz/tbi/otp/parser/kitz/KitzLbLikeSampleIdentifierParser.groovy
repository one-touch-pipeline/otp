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
package de.dkfz.tbi.otp.parser.kitz

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.parser.*

import java.util.regex.Matcher

@Component
class KitzLbLikeSampleIdentifierParser implements SampleIdentifierParser, ProjectMappingParser {

    static final String PROJECT_PREFIX = /(?<projectPrefix>[A-Z]{4})/
    static final String INTERMEDIATE = /[0-9A-Z]{3,6}/
    static final String DIGITS = /[0-9]{3,4}/
    static final String PID = /(?<pid>${PROJECT_PREFIX}-${INTERMEDIATE}-${DIGITS})/
    static final String TISSUE_TYPE = /(?<tissueType>([PTLASBU]|DBS|TD))/
    static final String SAMPLE_NUMBER = /(?<sampleNumber>[0-9]{1,2})/
    static final String ALIQUOT_NUMBER = /(?<aliquotNumber>[0-9]{1,2})/
    static final String REGEX = "^${PID}-${TISSUE_TYPE}${SAMPLE_NUMBER}\\.${ALIQUOT_NUMBER}\$"

    final ProcessingOptionService processingOptionService

    KitzLbLikeSampleIdentifierParser(final ProcessingOptionService processingOptionService) {
        this.processingOptionService = processingOptionService
    }

    @Override
    DefaultParsedSampleIdentifier tryParse(String sampleIdentifier) {
        if (!sampleIdentifier) {
            return null
        }
        Matcher matcher = sampleIdentifier =~ REGEX
        if (matcher) {
            assert matcher.matches()

            String projectName = mapProjectName(ProcessingOption.OptionName.KITZ_LB_LIKE_PARSER_MAPPING, matcher.group('projectPrefix'))

            if (!projectName) {
                return null
            }

            String tissueAbbreviation = matcher.group('tissueType')
            KitzLbLikeTissueType kitzLbLikeTissueType = KitzLbLikeTissueType.fromKey(tissueAbbreviation)
            if (!kitzLbLikeTissueType) {
                return null
            }

            String sampleNumber = matcher.group('sampleNumber').padLeft(2, '0')
            String aliquotNumber = matcher.group('aliquotNumber').padLeft(2, '0')

            String sampleType = "${kitzLbLikeTissueType}-${sampleNumber}-${aliquotNumber}"

            return new DefaultParsedSampleIdentifier(
                    projectName,
                    matcher.group('pid'),
                    sampleType,
                    sampleIdentifier,
                    null,
            )
        }
        return null
    }

    @Override
    String tryParseSingleCellWellLabel(String sampleIdentifier) {
        return null
    }
}
