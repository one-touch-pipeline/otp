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
package de.dkfz.tbi.otp.parser.hipo

import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.parser.ParsedSampleIdentifier

@SuppressWarnings('JUnitPublicProperty')
class Hipo2SampleIdentifierParserSpec extends AbstractHipo2SampleIdentifierParserSpec {

    Hipo2SampleIdentifierParser parser = new Hipo2SampleIdentifierParser()

    String validProjectPart = "K12A"

    String projectName = "hipo_K12A"

    @Unroll
    void 'tryParse for projectPart, when identifier is #identifier, parses correctly'() {
        when:
        ParsedSampleIdentifier parsed = parser.tryParse(identifier)
        boolean validPid = parser.tryParsePid(identifier.split("-")[0, 1].join("-"))

        then:
        validPid
        parsed.projectName == "hipo_${identifier.substring(0, 4)}"
        parsed.pid == identifier.split("-")[0, 1].join("-")
        parsed.sampleTypeDbName == sampleTypeDbName
        parsed.fullSampleName == identifier
        parsed.useSpecificReferenceGenome == SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        where:
        identifier          || sampleTypeDbName
        'K12A-123ABC-N0-D1' || 'CONTROL0-01'

        //different project prefixes
        'S12A-123ABC-T0-D1' || 'TUMOR0-01'
        'T12A-123ABC-T0-D1' || 'TUMOR0-01'
        'A12A-123ABC-T0-D1' || 'TUMOR0-01'
        'B12A-123ABC-T0-D1' || 'TUMOR0-01'
        'H12A-123ABC-T0-D1' || 'TUMOR0-01'
        'P12A-123ABC-T0-D1' || 'TUMOR0-01'
        'K123-123ABC-T0-D1' || 'TUMOR0-01'
    }

    @Unroll
    void 'tryParse for project part, when identifier is #identifier, returns null'() {
        expect:
        parser.tryParse(identifier) == null

        where:
        identifier << [
                'K12-123ABC-N0-D1',
                'K12AB-123ABC-N0-D1',
                '212A-123ABC-N0-D1',
                'KABC-123ABC-N0-D1',
        ]
    }
}
