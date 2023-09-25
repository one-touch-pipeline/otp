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
package de.dkfz.tbi.otp.workflow.alignment.panCancer

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.workflow.alignment.*
import de.dkfz.tbi.otp.workflow.jobs.SetCorrectPermissionJob
import de.dkfz.tbi.otp.workflowExecution.Artefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact

/**
 * represents the PanCancer Workflow
 */
@Component
@Slf4j
class PanCancerWorkflow extends AlignmentWorkflow {

    static final String WORKFLOW = "PanCancer alignment"

    @Override
    List<String> getJobBeanNames() {
        return [
                RoddyAlignmentFragmentJob.simpleName.uncapitalize(),
                PanCancerConditionalFailJob.simpleName.uncapitalize(),
                RoddyAlignmentPrepareJob.simpleName.uncapitalize(),
                PanCancerExecuteJob.simpleName.uncapitalize(),
                PanCancerValidationJob.simpleName.uncapitalize(),
                PanCancerParseJob.simpleName.uncapitalize(),
                RoddyAlignmentCheckQcJob.simpleName.uncapitalize(),
                PanCancerCleanUpJob.simpleName.uncapitalize(),
                SetCorrectPermissionJob.simpleName.uncapitalize(),
                PanCancerLinkJob.simpleName.uncapitalize(),
                RoddyAlignmentFinishJob.simpleName.uncapitalize(),
        ]
    }

    /**
     * Since it is designed for repeated run, it creates and returns a new artefact
     */
    @Override
    Artefact createCopyOfArtefact(Artefact artefact) {
        RoddyBamFile roddyBamFile = artefact as RoddyBamFile

        MergingWorkPackage mergingWorkPackage = roddyBamFile.mergingWorkPackage
        int identifier = RoddyBamFile.nextIdentifier(mergingWorkPackage)

        RoddyBamFile outputRoddyBamFile = new RoddyBamFile([
                workPackage        : mergingWorkPackage,
                identifier         : identifier,
                workDirectoryName  : "${RoddyBamFile.WORK_DIR_PREFIX}_${identifier}",
                baseBamFile        : roddyBamFile.baseBamFile,
                seqTracks          : roddyBamFile.seqTracks.collect() as Set,
                config             : roddyBamFile.config,
                numberOfMergedLanes: roddyBamFile.containedSeqTracks.size(),
        ]).save(flush: true)

        return outputRoddyBamFile
    }

    /**
     * There is nothing to do in a single run workflow.
     */
    @Override
    void reconnectDependencies(Artefact artefact, WorkflowArtefact newWorkflowArtefact) {
    }

    final String userDocumentation = "notification.template.references.alignment.pancancer"
}
