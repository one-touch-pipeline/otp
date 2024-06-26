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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.hibernate.annotation.ManagedEntity
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflow.WorkflowCreateState
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

/**
 * Represents the state of the import instance of the externally processed BAM files
 */
@ManagedEntity
class BamImportInstance implements Entity, ProcessParameterObject {

    WorkflowCreateState workflowCreateState = WorkflowCreateState.WAITING

    @TupleConstructor
    enum LinkOperation {
        COPY_AND_KEEP(false, false),
        COPY_AND_LINK(false, true),
        LINK_SOURCE(true, false),

        final boolean linkSource

        final boolean replaceSourceWithLink
    }

    LinkOperation linkOperation = LinkOperation.COPY_AND_KEEP

    boolean triggerAnalysis

    Ticket ticket

    Set<ExternallyProcessedBamFile> externallyProcessedBamFiles

    static hasMany = [
            externallyProcessedBamFiles: ExternallyProcessedBamFile,
    ]

    static constraints = {
        ticket nullable: true
        externallyProcessedBamFiles validator: { val, obj ->
            List<BamImportInstance> importInstances = BamImportInstance.createCriteria().listDistinct {
                externallyProcessedBamFiles {
                    'in'('id', val*.id)
                }
            }
            for (BamImportInstance importInstance : importInstances) {
                if (importInstance && importInstance.id != obj.id) {
                    return "already.imported"
                }
            }
            return true
        }
    }

    static Closure mapping = {
        state index: "bam_import_instance_state_idx"
        workflowCreateState index: "bam_import_instance_workflow_create_state_idx"
    }

    @SuppressWarnings("GetterMethodCouldBeProperty")
    // is no property
    @Override
    SeqType getSeqType() {
        return null
    }

    @SuppressWarnings("GetterMethodCouldBeProperty")
    // is no property
    @Override
    Individual getIndividual() {
        return null
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return []
    }

    @Override
    ProcessingPriority getProcessingPriority() {
        return externallyProcessedBamFiles ? externallyProcessedBamFiles*.project*.processingPriority.max {
            it.priority
        } : null
    }
}
