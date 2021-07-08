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
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflowExecution.Artefact

/**
 * One object of FastqcProcessedFile represents one output file
 * of "fastqc" program. It belongs to dataFile object which represents
 * original sequence file and keep track of the status: if the file exists
 * and if it content was uploaded to data base.
 */

class FastqcProcessedFile implements Artefact, Entity {

    boolean fileExists = false
    boolean contentUploaded = false
    long fileSize = -1

    Date dateFromFileSystem = null

    DataFile dataFile

    static belongsTo = [
        dataFile: DataFile,
    ]

    static constraints = {
        dateFromFileSystem(nullable: true)
        dataFile(unique: true)
        workflowArtefact nullable: true
    }

    static mapping = {
        dataFile index: "fastqc_processed_file_data_file_idx"
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return [dataFile.seqTrack] as Set
    }
}
