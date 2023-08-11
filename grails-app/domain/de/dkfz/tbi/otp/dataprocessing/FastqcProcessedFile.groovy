/*
 * Copyright 2011-2023 The OTP authors
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

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.filestore.PathInWorkFolder
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflowExecution.Artefact

/**
 * One object of FastqcProcessedFile represents one output file
 * of "fastqc" program. It belongs to sequence file object which
 * keeps track of the status: if the file exists
 * and if it content was uploaded to database.
 */
@ManagedEntity
class FastqcProcessedFile implements Artefact, PathInWorkFolder, Entity {

    boolean fileExists = false
    boolean contentUploaded = false
    long fileSize = -1

    Date dateFromFileSystem = null

    RawSequenceFile sequenceFile

    String workDirectoryName

    /**
     * flag to indicate, that the file was copied and not processed
     */
    boolean fileCopied = false

    static belongsTo = [
            sequenceFile: RawSequenceFile,
    ]

    static Closure constraints = {
        dateFromFileSystem(nullable: true)
        sequenceFile(unique: true)
        workflowArtefact nullable: true
        pathInWorkFolder(nullable: true) // only used for wes fastqc
        workDirectoryName validator: { String value, FastqcProcessedFile obj ->
            if (value && obj.sequenceFile && FastqcProcessedFile.withCriteria {
                sequenceFile {
                    eq('seqTrack', obj.sequenceFile.seqTrack)
                }
                ne('workDirectoryName', value)
            }) {
                return "fastqcProcessedFile.workDirectoryName.differ"
            }
        }
    }

    static Closure mapping = {
        sequenceFile index: "fastqc_processed_file_sequence_file_idx"
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return [sequenceFile.seqTrack] as Set
    }

    @Override
    String toString() {
        return "Fastqc ${id} for ${sequenceFile} for ${sequenceFile.seqTrack}"
    }
}
