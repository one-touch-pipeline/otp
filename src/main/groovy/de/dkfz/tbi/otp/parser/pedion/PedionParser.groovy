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
package de.dkfz.tbi.otp.parser.pedion

import groovy.transform.CompileDynamic
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.parser.DefaultParsedSampleIdentifier
import de.dkfz.tbi.otp.parser.SampleIdentifierParser

import java.util.regex.Matcher

/**
 * Checker for Project PeDiOn (Berlin).
 */
@CompileDynamic
@Component
class PedionParser implements SampleIdentifierParser {

    static final String PROJECT_KEY = "project"
    static final String PID_KEY = "pid"
    static final String CATEGORY_KEY = "category"
    static final String TISSUE_TYPE_KEY = "tissueType"
    static final String BIO_REPLICATE_KEY = "bioReplicate"
    static final String SAMPLE_IDENTIFIER_KEY = "sampleIdentifier"

    static final String PROJECT_FUNDING_PERIOD = /^[A-Z]/
    static final String PROJECT_NUMBER = /[0-9]{2}/
    static final String PROJECT_SUFFIX = /[PR]/  // P=prospective, R=Retrospective

    static final String PATIENT_PSEUDONYM = /[A-Z]{6}/

    static final String CATEGORY = "(?<${CATEGORY_KEY}>[${PedionCategory.values()*.letter.join()}])"
    static final String TISSUE_TYPE = "(?<${TISSUE_TYPE_KEY}>(${PedionTissue.values()*.code.join(")|(")}))"

    /**
     * Char as Biological replicate, represent different biopsy timepoints.
     *
     * OTP uses two digits instead
     * <ul>
     * <li> A --> 01 </li>
     * <li> b --> 02 </li>
     * <li> ... </li>
     * </ul>
     */
    static final String BIOLOGICAL_REPLICATE = /(?<${BIO_REPLICATE_KEY}>[A-Z])/

    /**
     * BIH Genomics Facility:
     *
     * <ul>
     * <li> analyte </li>
     * <li> technical replicate of analyte: Different extraction time points of the same sample </li>
     * <li> sequencing assay </li>
     * <li> technical replicate of sequencing assay: Different seq reruns from the same analyte sample  </li>
     * </ul>
     */
    static final String BIH_GEN_FACILITY = /[A-Z]{4}$/

    static final String PROJECT = "(?<${PROJECT_KEY}>${PROJECT_FUNDING_PERIOD}${PROJECT_NUMBER}${PROJECT_SUFFIX})"

    static final String PID = "(?<${PID_KEY}>${PROJECT}-${PATIENT_PSEUDONYM})"

    static final String SAMPLE_TYPE = "${CATEGORY}${TISSUE_TYPE}${BIOLOGICAL_REPLICATE}"

    static final String SAMPLE = "${PID}-${SAMPLE_TYPE}"

    static final String SAMPLE_IDENTIFIER = "(?<${SAMPLE_IDENTIFIER_KEY}>${SAMPLE}${BIH_GEN_FACILITY})"

    @Override
    boolean tryParsePid(String pid) {
        return pid =~ PID
    }

    @Override
    DefaultParsedSampleIdentifier tryParse(String sampleIdentifier) {
        Matcher matcher = sampleIdentifier =~ SAMPLE_IDENTIFIER
        if (matcher) {
            String project = matcher.group(PROJECT_KEY)
            String pid = matcher.group(PID_KEY)
            String sampleType = convertToOtpSampleType(matcher)
            String fullIdentifier = matcher.group(SAMPLE_IDENTIFIER_KEY)
            SampleType.SpecificReferenceGenome specificReferenceGenome = SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

            return new DefaultParsedSampleIdentifier(
                    project,
                    pid,
                    sampleType,
                    fullIdentifier,
                    specificReferenceGenome,
                    null,
            )
        }
        return null
    }

    @Override
    String tryParseSingleCellWellLabel(String sampleIdentifier) {
        return null
    }

    private String convertToOtpSampleType(Matcher matcher) {
        String category = matcher.group(CATEGORY_KEY)
        String tissueType = matcher.group(TISSUE_TYPE_KEY)
        String bioReplicate = matcher.group(BIO_REPLICATE_KEY)

        return [
                PedionCategory.LETTER_TO_NAME_MAP[category],
                PedionTissue.LETTER_TO_NAME_MAP[tissueType],
                charToDigit(bioReplicate),
        ].join('-')
    }

    private String charToDigit(String letter) {
        char c = letter as char
        char a = 'A' as char
        int i = (c - a) + 1
        return i.toString().padLeft(2, '0')
    }
}
