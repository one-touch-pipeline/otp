/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflow.alignment.cellRanger

import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflowShared
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflowSharedSpec
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class CellRangerSharedSpec extends AlignmentWorkflowSharedSpec<SingleCellBamFile> implements CellRangerFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                WorkflowRun,
                WorkflowStep,
                SingleCellBamFile,
                CellRangerMergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                FileType,
                FastqImportInstance,
        ]
    }

    @Override
    AlignmentWorkflowShared<SingleCellBamFile> createSharedInstance() {
        return Spy(CellRangerSharedInstance)
    }

    @Override
    SingleCellBamFile createBamFile() {
        return CellRangerFactory.super.createBamFile()
    }

    @Override
    String getWorkflowName() {
        return CellRangerWorkflow.WORKFLOW
    }

    @Override
    String getInputFastqConstant() {
        return CellRangerWorkflow.INPUT_FASTQ
    }

    @Override
    String getOutputBamConstant() {
        return CellRangerWorkflow.OUTPUT_BAM
    }

    @SuppressWarnings('EmptyClass')
    class CellRangerSharedInstance implements CellRangerShared { }
}
