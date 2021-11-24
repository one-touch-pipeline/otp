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
package de.dkfz.tbi.otp.ngsdata

import groovy.transform.TupleConstructor

/**
 * Defines the columns of the FASTQ metadata file
 */
@TupleConstructor
enum MetaDataColumn {
    FASTQ_FILE,
    READ(["MATE"]),
    MD5,
    CENTER_NAME,
    RUN_ID,
    RUN_DATE,
    LANE_NO,
    SAMPLE_NAME(["SAMPLE_ID"]),
    INDEX(["BARCODE"]),
    SEQUENCING_TYPE,
    INSTRUMENT_MODEL,
    INSTRUMENT_PLATFORM,
    FASTQ_GENERATOR(["BCL2FASTQ", "BCL2FASTQ_VERSION"]),
    SEQUENCING_READ_TYPE(["LIBRARY_LAYOUT"]),
    ILSE_NO,
    LIB_PREP_KIT,
    ANTIBODY_TARGET,
    ANTIBODY,
    SEQUENCING_KIT,
    PROJECT,
    SAMPLE_TYPE(["BIOMATERIAL_ID"]),
    TAGMENTATION_LIBRARY(["CUSTOMER_LIBRARY"]),
    PATIENT_ID,
    SPECIES,
    BASE_MATERIAL,
    ALIGN_TOOL,
    COMMENT,
    FILE_EXISTS, //For export, indicates, if a physically file is available or if it is a dead link
    FRAGMENT_SIZE(["INSERT_SIZE"]),
    SAMPLE_SUBMISSION_TYPE,
    SINGLE_CELL_WELL_LABEL,
    SWAPPED,
    WITHDRAWN,
    WITHDRAWN_DATE,
    WITHDRAWN_COMMENT,

    final List<String> importAliases = []

    private static Map<String, MetaDataColumn> mapping
    static {
        mapping = values().collectEntries { column ->
            Map<String, MetaDataColumn> map = [(column.name()): column]
            column.importAliases.each { alias ->
                map.put(alias, column)
            }
            return map
        }
    }

    static MetaDataColumn getColumnForName(String name) {
        return mapping.get(name)
    }
}
