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

class OE0290_EORTC_SampleIdentifierParserSpec extends AbstractHipo2SampleIdentifierParserSpec {

    OE0290_EORTC_SampleIdentifierParser parser = new OE0290_EORTC_SampleIdentifierParser()

    String validProjectPart = "M002"

    String projectName = "OE0290_EORTC"

    @Unroll
    void 'tryParse for projectPart, when identifier is #identifier, returns null'() {
        expect:
        parser.tryParse(identifier) == null

        where:
        identifier << [
                // invalid project
                'K12K-123ABC-N0-D1',
                'M02-123ABC-N0-D1',
                'M002B-123ABC-N0-D1',
        ]
    }
}
