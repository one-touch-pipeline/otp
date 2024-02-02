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
package de.dkfz.tbi.otp.ngsdata

import groovy.transform.TupleConstructor

/**
 * Defines the columns of the BAM metadata file
 */
@TupleConstructor
enum BamMetadataColumn {
    BAM_FILE_PATH,
    COVERAGE,
    INDIVIDUAL,
    SEQUENCING_READ_TYPE(["LIBRARY_LAYOUT"]),
    MD5,
    PROJECT,
    REFERENCE_GENOME,
    SAMPLE_TYPE,
    SEQUENCING_TYPE,
    LIBRARY_PREPARATION_KIT,
    INSERT_SIZE_FILE,
    QUALITY_CONTROL_FILE,
    MAXIMAL_READ_LENGTH,

    final List<String> importAliases = []

    private static Map<String, BamMetadataColumn> mapping
    static {
        mapping = values().collectEntries { column ->
            Map<String, BamMetadataColumn> map = [(column.name()): column]
            column.importAliases.each { alias ->
                map.put(alias, column)
            }
            return map
        }
    }

    static BamMetadataColumn getColumnForName(String name) {
        return mapping.get(name)
    }
}
