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

import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.parser.ParsedSampleIdentifier

class Hipo2SamplePreparationLabSampleIdentifierParserSpec extends AbstractHipo2SampleIdentifierParserSpec {

    Hipo2SamplePreparationLabSampleIdentifierParser parser = new Hipo2SamplePreparationLabSampleIdentifierParser()

    String validProjectPart = "H021"

    String projectName = "hipo_021"

    @Override
    String transformTissueNumber(String tissueNumber) {
        return tissueNumber.padLeft(3, '0')
    }

    void setup() {
        findOrCreateProcessingOption(
                name: ProcessingOption.OptionName.HIPO2_PARSER_MAPPING,
                value: '{\n' +
                        '   "H021":"hipo_021",\n' +
                        '   "H022":"hipo_022",\n' +
                        '}',
        )
        parser.processingOptionService = new ProcessingOptionService()
    }

    @Unroll
    void 'tryParse for tissue number part, when identifier is #identifier, parses correctly'() {
        given:
        setup()

        when:
        ParsedSampleIdentifier parsed = parser.tryParse(identifier)
        boolean validPid = parser.tryParsePid(identifier.split("-")[0, 1].join("-"))

        then:
        validPid
        parsed.projectName == "hipo_${identifier.substring(1, 4)}"
        parsed.pid == identifier.split("-")[0, 1].join("-")
        parsed.sampleTypeDbName == sampleTypeDbName
        parsed.fullSampleName == identifier
        parsed.useSpecificReferenceGenome == SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        where:
        identifier             || sampleTypeDbName
        'H021-12AB-T1-D1'      || 'tumor001-01'
        'H021-12AB-T12-D1'     || 'tumor012-01'
        'H021-12AB-T123-D1'    || 'tumor123-01'
        'H021-123ABCD-T123-D1' || 'tumor123-01'
    }

    @Unroll
    void 'tryParse for tissue number part, when identifier is #identifier, returns null'() {
        given:
        setup()

        expect:
        parser.tryParse(identifier) == null

        where:
        identifier << [
                'H021-12AB-T-D1',
                'H021-12AB-T1234-D1',
        ]
    }
}
