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
package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.utils.Entity

class ProcessedSaiFile implements Entity {

    boolean fileExists
    Date dateFromFileSystem
    long fileSize = -1

    /** Time stamp of deletion */
    Date deletionDate

    AlignmentPass alignmentPass
    DataFile dataFile

    static belongsTo = [
        alignmentPass: AlignmentPass,
        dataFile: DataFile,
    ]

    static constraints = {
        dateFromFileSystem(nullable: true)
        deletionDate(nullable: true)
    }

    Project getProject() {
        return alignmentPass.project
    }

    static mapping = {
        alignmentPass index: "processed_sai_file_alignemt_pass_idx"
        dataFile index: "processed_sai_file_data_file_idx"
    }
}
