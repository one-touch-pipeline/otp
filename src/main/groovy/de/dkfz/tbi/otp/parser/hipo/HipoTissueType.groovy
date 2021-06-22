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

import de.dkfz.tbi.otp.ngsdata.SampleType

/**
 * Tissue types as defined by the HIPO project.
 */
enum HipoTissueType {
    TUMOR                               ('T'),
    METASTASIS                          ('M'),
    SPHERE                              ('S'),
    XENOGRAFT                           ('X', SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC),
    BLOOD                               ('B'),
    CONTROL                             ('N'),
    CELL                                ('C'),
    INVASIVE_MARGINS                    ('I'),
    PATIENT_DERIVED_CULTURE             ('P', SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC),
    CULTURE_DERIVED_XENOGRAFT           ('Q'),
    PLASMA                              ('L'),
    BUFFY_COAT                          ('F'),
    NORMAL_SORTED_CELLS                 ('Z'),
    TUMOR_INTERVAL_DEBULKING_SURGERY    ('E'),
    EXTERNAL_CONTROL                    ('K'),
    LYMPH_NODES                         ('A'),
    UNDEFINED_NEOPLASIA                 ('U'),

    final char key

    final SampleType.SpecificReferenceGenome specificReferenceGenome

    private HipoTissueType(String key, SampleType.SpecificReferenceGenome specificReferenceGenome = SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT) {
        this.key = key as char
        this.specificReferenceGenome = specificReferenceGenome
    }

    /**
     * Returns the corresponding {@link HipoTissueType} for a key or <code>null</code> if no
     * {@link HipoTissueType} with that key exists.
     */
    static HipoTissueType fromKey(String key) {
        char keyChar = key as char
        return values().find {
            it.key == keyChar
        }
    }

    @Override
    String toString() {
        return this.name().toLowerCase()
    }
}
