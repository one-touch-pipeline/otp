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
package de.dkfz.tbi.otp.parser.itccp4

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.parser.DefaultParsedSampleIdentifier
import de.dkfz.tbi.otp.parser.SampleIdentifierParser

import java.util.regex.Matcher

/**
 * Checker for Project ITCC-4P.
 * To be more flexible only the structure but not each allowed char/number is checked.
 */
@Component('iTCC_4P_Parser')
class ITCC_4P_Parser implements SampleIdentifierParser {

    static final String CHAR = /[A-Z]/

    /**
     * Expression for the project name. Only OE0290_ITCC-P4 supported.
     */
    static final String PROJECT = /(?<project>ITCC-P4)/

    /**
     * Represent the center, value is not used in OTP.
     */
    static final String SOURCE = /(?:s\d{2})/

    /**
     * The disease of the sample. Together with project it creates the PID in OTP.
     * To be more flexible we do not check explicit for valid chars.
     */
    static final String DISEASE = /(?<disease>${CHAR}{2}\d{4})/

    /**
     * The sample type definition as defined in the project external. In OTP we use also {@link #SAMPLE_TYPE} and {@link #MATERIAL_TYPE}
     * to build the OTP sample type. If the sample type starts with P, it is a xenograph sample type und use a sample type specific reference genome.
     */
    static final String SAMPLE_TYPE = /(?<sampleType>${CHAR}{2}\d{2})/

    /**
     * Match the material type. It is used as second part of the OTP sample type.
     */
    static final String MATERIAL_TYPE = /(?<materialType>${CHAR}\d{2})/

    /**
     * Match the isolate type. It is used as third part of the OTP sample type.
     */
    static final String ISOLATE_TYPE = /(?<isolateType>${CHAR}\d{2})/

    /**
     * Match the isolate type. Since it is not used in OTP, we do not check the content at all. Also it is an obsolete component in the identifier.
     */
    static final String ANALYSE_TYPE = /(?:.+)/

    /**
     * expression for the OTP PID
     */
    static final String PID = /^${PROJECT}_${DISEASE}$/

    // The full identifier
    static final String REGEX = /^${PROJECT}_${SOURCE}_${DISEASE}_${SAMPLE_TYPE}_${MATERIAL_TYPE}_${ISOLATE_TYPE}(?:_${ANALYSE_TYPE})?$/

    @Override
    boolean tryParsePid(String pid) {
        return pid =~ PID
    }

    @Override
    DefaultParsedSampleIdentifier tryParse(String sampleIdentifier) {
        Matcher matcher = sampleIdentifier =~ REGEX
        if (matcher) {
            String project = "OE0290_${matcher.group('project')}"
            String pid = "${matcher.group('project')}_${matcher.group('disease')}"
            String sampleType = "${matcher.group('sampleType')}-${matcher.group('materialType')}-${matcher.group('isolateType')}"
            SampleType.SpecificReferenceGenome specificReferenceGenome = sampleType.startsWith("P") ?
                    SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC : SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

            return new DefaultParsedSampleIdentifier(
                    project,
                    pid,
                    sampleType,
                    sampleIdentifier,
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
}
