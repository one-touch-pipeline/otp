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
package de.dkfz.tbi.otp.workflow.alignment.rna

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflow
import de.dkfz.tbi.otp.workflow.alignment.RoddyAlignmentConditionalFailJob
import de.dkfz.tbi.otp.workflow.alignment.RoddyAlignmentPrepareJob
import de.dkfz.tbi.otp.workflow.jobs.SetCorrectPermissionJob
import de.dkfz.tbi.otp.workflowExecution.Artefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact

@Component
@Slf4j
class RnaAlignmentWorkflow extends AlignmentWorkflow {

    public static final String WORKFLOW = "RNA alignment"

    final String userDocumentation = null

    @Override
    List<String> getJobBeanNames() {
        return [
                SetCorrectPermissionJob.simpleName.uncapitalize(),
                RnaAlignmentLinkJob.simpleName.uncapitalize(),
                RoddyAlignmentPrepareJob.simpleName.uncapitalize(),
                RnaAlignmentValidationJob.simpleName.uncapitalize(),
                RnaAlignmentCleanUpJob.simpleName.uncapitalize(),
                RoddyAlignmentConditionalFailJob.simpleName.uncapitalize(),
        ]
    }

    @Override
    Artefact createCopyOfArtefact(Artefact artefact) {
        RnaRoddyBamFile rnaRoddyBamFile = artefact as RnaRoddyBamFile

        MergingWorkPackage mergingWorkPackage = rnaRoddyBamFile.mergingWorkPackage
        int identifier = RnaRoddyBamFile .nextIdentifier(mergingWorkPackage)

        RnaRoddyBamFile outputRnaRoddyBamFile = new RnaRoddyBamFile([
                workPackage        : mergingWorkPackage,
                identifier         : identifier,
                workDirectoryName  : "${RnaRoddyBamFile.WORK_DIR_PREFIX}_${identifier}",
                baseBamFile        : rnaRoddyBamFile.baseBamFile,
                seqTracks          : rnaRoddyBamFile.seqTracks.collect() as Set,
                config             : rnaRoddyBamFile.config,
                numberOfMergedLanes: rnaRoddyBamFile.containedSeqTracks.size(),
        ]).save(flush: true)

        return outputRnaRoddyBamFile
    }

    @Override
    void reconnectDependencies(Artefact artefact, WorkflowArtefact newWorkflowArtefact) {
    }
}
