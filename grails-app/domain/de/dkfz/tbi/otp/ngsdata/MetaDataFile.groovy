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

import de.dkfz.tbi.otp.utils.Entity

class MetaDataFile implements Entity {

    String fileName
    String filePath

    /**
     * Will only be filled by metadata import 2.0.
     * For metadata import 1.0 this field will stay null.
     */
    String md5sum

    FastqImportInstance fastqImportInstance

    static belongsTo = [
        fastqImportInstance: FastqImportInstance,
    ]

    static constraints = {
        fileName(blank: false, shared: "pathComponent")
        filePath(blank: false, shared: "absolutePath")
        md5sum(nullable: true, matches: /^[0-9a-f]{32}$/)
    }

    static mapping = {
        fastqImportInstance index: "meta_data_file_fastq_import_instance_idx"
    }
}
