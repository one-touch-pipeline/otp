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
package de.dkfz.tbi.otp.parser.inform

import de.dkfz.tbi.otp.ngsdata.SampleType.SpecificReferenceGenome

import static de.dkfz.tbi.otp.ngsdata.SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
import static de.dkfz.tbi.otp.ngsdata.SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC

enum InformTissueType {
    TUMOR     ('T', USE_PROJECT_DEFAULT),
    METASTASIS('M', USE_PROJECT_DEFAULT),
    CONTROL   ('C', USE_PROJECT_DEFAULT),
    FFPE      ('F', USE_PROJECT_DEFAULT),
    PDX       ('P', USE_SAMPLE_TYPE_SPECIFIC),
    PLASMA    ('L', USE_PROJECT_DEFAULT),
    OTHER     ('X', USE_PROJECT_DEFAULT),

    final char key
    final SpecificReferenceGenome specificReferenceGenome

    private InformTissueType(String key, SpecificReferenceGenome specificReferenceGenome) {
        this.key = key as char
        this.specificReferenceGenome = specificReferenceGenome
    }

    static InformTissueType fromKey(String key) {
        InformTissueType informTissueType = values().find { it.key == key as char }
        if (informTissueType == null) {
            throw new IllegalArgumentException()
        }
        return informTissueType
    }

    @Override
    String toString() {
        return this.name().toLowerCase()
    }
}
