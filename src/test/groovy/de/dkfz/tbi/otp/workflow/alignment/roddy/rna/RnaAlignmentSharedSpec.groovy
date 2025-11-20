/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.workflow.alignment.roddy.rna

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflowShared
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflowSharedSpec
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class RnaAlignmentSharedSpec extends AlignmentWorkflowSharedSpec<RnaRoddyBamFile> implements RoddyRnaFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                WorkflowRun,
                WorkflowStep,
                RnaRoddyBamFile,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                FileType,
        ]
    }

    @Override
    AlignmentWorkflowShared<RnaRoddyBamFile> createSharedInstance() {
        return Spy(RnaAlignmentSharedInstance)
    }

    @Override
    RnaRoddyBamFile createBamFile() {
        return RoddyRnaFactory.super.createBamFile([:])
    }

    @Override
    String getWorkflowName() {
        return RnaAlignmentWorkflow.WORKFLOW
    }

    @Override
    String getInputFastqConstant() {
        return RnaAlignmentWorkflow.INPUT_FASTQ
    }

    @Override
    String getOutputBamConstant() {
        return RnaAlignmentWorkflow.OUTPUT_BAM
    }

    @SuppressWarnings('EmptyClass')
    class RnaAlignmentSharedInstance implements RnaAlignmentShared { }
}
