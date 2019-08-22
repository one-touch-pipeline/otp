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

import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.Entity

/**
 * Represents the state of the import process of the externally processed merged BAM files
 */
class ImportProcess implements Entity, ProcessParameterObject {

    enum State {
        NOT_STARTED,
        STARTED,
        FINISHED
    }

    State state = State.NOT_STARTED

    boolean replaceSourceWithLink

    boolean triggerAnalysis

    Set<ExternallyProcessedMergedBamFile> externallyProcessedMergedBamFiles

    static hasMany = [
            externallyProcessedMergedBamFiles: ExternallyProcessedMergedBamFile,
    ]

    static constraints = {
        externallyProcessedMergedBamFiles validator: { val, obj ->
            List<ImportProcess> importProcesses = ImportProcess.createCriteria().listDistinct {
                externallyProcessedMergedBamFiles {
                    'in'('id', val*.id)
                }
            }
            for (ImportProcess importProcess : importProcesses) {
                if (importProcess && importProcess.id != obj.id) {
                    return "This set of bam files was already imported"
                }
            }
            return true
        }
    }

    @Override
    SeqType getSeqType() {
        return null
    }

    @Override
    Individual getIndividual() {
        return null
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return []
    }

    @Override
    short getProcessingPriority() {
        return ProcessingPriority.NORMAL.priority
    }
}
